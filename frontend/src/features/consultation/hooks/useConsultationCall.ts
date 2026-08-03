import { useCallback, useEffect, useRef, useState } from 'react'
import { apiClient } from '@/shared/api/client'

export type CallPhase =
  | 'idle'
  | 'requesting-media'
  | 'connecting'
  | 'connected'
  | 'ended'
  | 'permission-denied'
  | 'error'

type SignalingMessage =
  | { type: 'peer-joined' }
  | { type: 'peer-left' }
  | { type: 'offer'; sdp: string }
  | { type: 'answer'; sdp: string }
  | { type: 'ice-candidate'; candidate: RTCIceCandidateInit }

const CONNECT_TIMEOUT_MS = 10_000
const QUALITY_POLL_INTERVAL_MS = 4_000
// Heuristics for Story 6.2's "connection quality drops below the defined
// threshold" — no vendor quality API exists (mocked TURN provider), so this
// reads RTCPeerConnection's own getStats() directly.
const PACKET_LOSS_RATIO_THRESHOLD = 0.08
const ROUND_TRIP_TIME_MS_THRESHOLD = 400

function wsUrlFor(): string {
  const base = import.meta.env.VITE_API_BASE_URL as string
  return base.replace(/^http/, 'ws') + '/ws/consultations'
}

/**
 * Story 6.1 (browser WebRTC join, no app install) + Story 6.2 (audio-only fallback).
 * Signaling protocol matches the backend's ConsultationSignalingHandler exactly: whichever side
 * receives "peer-joined" is the one that creates the offer (it's the side that was already
 * connected when the second participant showed up), so there is no glare to handle with only
 * 2 participants per room.
 */
export function useConsultationCall(consultationId: string) {
  const [phase, setPhase] = useState<CallPhase>('idle')
  const [localStream, setLocalStream] = useState<MediaStream | null>(null)
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null)
  const [isMuted, setIsMuted] = useState(false)
  const [isAudioOnly, setIsAudioOnly] = useState(false)
  const [poorConnection, setPoorConnection] = useState(false)
  const [connectTimeoutHit, setConnectTimeoutHit] = useState(false)
  const [peerLeft, setPeerLeft] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const pcRef = useRef<RTCPeerConnection | null>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const connectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const qualityIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  // A ref, not the localStream state, so `cleanup` (stable identity, called from the
  // unmount-only effect below) always tears down the *current* stream rather than a stale
  // closure over whatever localStream was when cleanup was first created.
  const localStreamRef = useRef<MediaStream | null>(null)

  const cleanup = useCallback(() => {
    if (connectTimeoutRef.current) {
      clearTimeout(connectTimeoutRef.current)
      connectTimeoutRef.current = null
    }
    if (qualityIntervalRef.current) {
      clearInterval(qualityIntervalRef.current)
      qualityIntervalRef.current = null
    }
    wsRef.current?.close()
    wsRef.current = null
    pcRef.current?.close()
    pcRef.current = null
    localStreamRef.current?.getTracks().forEach((track) => track.stop())
  }, [])

  useEffect(() => cleanup, [cleanup])

  const sendSignal = useCallback((message: SignalingMessage) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message))
    }
  }, [])

  const startQualityPolling = useCallback((pc: RTCPeerConnection) => {
    qualityIntervalRef.current = setInterval(() => {
      pc.getStats(null).then((stats) => {
        let packetsLost = 0
        let packetsReceived = 0
        let rttMs = 0
        stats.forEach((report) => {
          const r = report as RTCStats & Record<string, number | boolean | string | undefined>
          if (r.type === 'inbound-rtp' && !r.isRemote) {
            packetsLost += (r.packetsLost as number) ?? 0
            packetsReceived += (r.packetsReceived as number) ?? 0
          }
          if (r.type === 'candidate-pair' && r.state === 'succeeded') {
            rttMs = ((r.currentRoundTripTime as number) ?? 0) * 1000
          }
        })
        const total = packetsLost + packetsReceived
        const lossRatio = total > 0 ? packetsLost / total : 0
        setPoorConnection(lossRatio > PACKET_LOSS_RATIO_THRESHOLD || rttMs > ROUND_TRIP_TIME_MS_THRESHOLD)
      })
    }, QUALITY_POLL_INTERVAL_MS)
  }, [])

  const createPeerConnection = useCallback(
    (stream: MediaStream, iceServers: RTCIceServer[]) => {
      const pc = new RTCPeerConnection({ iceServers })
      stream.getTracks().forEach((track) => pc.addTrack(track, stream))

      pc.onicecandidate = (event) => {
        if (event.candidate) {
          sendSignal({ type: 'ice-candidate', candidate: event.candidate.toJSON() })
        }
      }
      pc.ontrack = (event) => {
        setRemoteStream(event.streams[0] ?? null)
        setPhase('connected')
        if (connectTimeoutRef.current) {
          clearTimeout(connectTimeoutRef.current)
          connectTimeoutRef.current = null
        }
        setConnectTimeoutHit(false)
        setPeerLeft(false)
        startQualityPolling(pc)
      }
      pc.oniceconnectionstatechange = () => {
        if (pc.iceConnectionState === 'failed' || pc.iceConnectionState === 'disconnected') {
          setPoorConnection(true)
        }
      }
      return pc
    },
    [sendSignal, startQualityPolling],
  )

  const handleSignal = useCallback(
    async (message: SignalingMessage) => {
      const pc = pcRef.current
      if (!pc) {
        return
      }
      switch (message.type) {
        case 'peer-joined': {
          const offer = await pc.createOffer()
          await pc.setLocalDescription(offer)
          sendSignal({ type: 'offer', sdp: offer.sdp ?? '' })
          break
        }
        case 'offer': {
          await pc.setRemoteDescription({ type: 'offer', sdp: message.sdp })
          const answer = await pc.createAnswer()
          await pc.setLocalDescription(answer)
          sendSignal({ type: 'answer', sdp: answer.sdp ?? '' })
          break
        }
        case 'answer':
          await pc.setRemoteDescription({ type: 'answer', sdp: message.sdp })
          break
        case 'ice-candidate':
          await pc.addIceCandidate(message.candidate)
          break
        case 'peer-left':
          setPeerLeft(true)
          break
      }
    },
    [sendSignal],
  )

  const join = useCallback(async () => {
    setErrorMessage(null)
    setPhase('requesting-media')
    try {
      const { data, error } = await apiClient.POST('/api/v1/consultations/{consultationId}/join', {
        params: { path: { consultationId } },
      })
      if (error || !data) {
        setPhase('error')
        setErrorMessage('join-failed')
        return
      }

      let stream: MediaStream
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
      } catch {
        setPhase('permission-denied')
        return
      }
      localStreamRef.current = stream
      setLocalStream(stream)
      setPhase('connecting')

      const iceServers: RTCIceServer[] = (data.iceServers ?? []).map((s) => ({
        urls: s.urls ?? '',
        username: s.username,
        credential: s.credential,
      }))
      const pc = createPeerConnection(stream, iceServers)
      pcRef.current = pc

      const ws = new WebSocket(`${wsUrlFor()}?token=${encodeURIComponent(data.signalingToken ?? '')}`)
      wsRef.current = ws
      ws.onmessage = (event) => {
        void handleSignal(JSON.parse(event.data as string) as SignalingMessage)
      }
      ws.onerror = () => {
        setPhase('error')
        setErrorMessage('signaling-failed')
      }

      connectTimeoutRef.current = setTimeout(() => {
        setConnectTimeoutHit(true)
      }, CONNECT_TIMEOUT_MS)
    } catch {
      setPhase('error')
      setErrorMessage('join-failed')
    }
  }, [consultationId, createPeerConnection, handleSignal])

  const endCall = useCallback(() => {
    cleanup()
    localStreamRef.current = null
    setLocalStream(null)
    setRemoteStream(null)
    setPhase('ended')
  }, [cleanup])

  const toggleMute = useCallback(() => {
    setIsMuted((prev) => {
      const next = !prev
      localStream?.getAudioTracks().forEach((track) => {
        track.enabled = !next
      })
      return next
    })
  }, [localStream])

  const switchToAudioOnly = useCallback(() => {
    const pc = pcRef.current
    if (pc) {
      const videoSender = pc.getSenders().find((s) => s.track?.kind === 'video')
      void videoSender?.replaceTrack(null)
    }
    localStream?.getVideoTracks().forEach((track) => track.stop())
    setIsAudioOnly(true)
    setPoorConnection(false)
    setConnectTimeoutHit(false)
  }, [localStream])

  const dismissPoorConnection = useCallback(() => setPoorConnection(false), [])
  const dismissConnectTimeout = useCallback(() => setConnectTimeoutHit(false), [])

  return {
    phase,
    localStream,
    remoteStream,
    isMuted,
    isAudioOnly,
    poorConnection,
    connectTimeoutHit,
    peerLeft,
    errorMessage,
    join,
    endCall,
    toggleMute,
    switchToAudioOnly,
    dismissPoorConnection,
    dismissConnectTimeout,
  }
}
