import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { MicIcon, MicOffIcon, PhoneOffIcon, VideoOffIcon, AlertTriangleIcon } from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import type { useConsultationCall } from '../hooks/useConsultationCall'

type Call = ReturnType<typeof useConsultationCall>

function VideoTile({ stream, muted, label }: { stream: MediaStream | null; muted: boolean; label: string }) {
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.srcObject = stream
    }
  }, [stream])

  return (
    <video
      ref={videoRef}
      autoPlay
      playsInline
      muted={muted}
      aria-label={label}
      className="aspect-video w-full rounded-md bg-slate-900 object-cover"
    />
  )
}

/** Story 6.1's join/connect states + Story 6.2's poor-connection/audio-only fallback UI. */
export function VideoConsultationRoom({ call, onEndCall }: { call: Call; onEndCall?: () => void }) {
  const { t } = useTranslation()

  if (call.phase === 'permission-denied') {
    return (
      <Alert variant="destructive" role="alert">
        <AlertTriangleIcon />
        <AlertDescription>
          <p className="font-medium">{t('consultation.room.permissionDenied.title')}</p>
          <p>{t('consultation.room.permissionDenied.message')}</p>
          <Button type="button" size="sm" onClick={() => void call.join()} className="mt-2">
            {t('consultation.room.permissionDenied.retry')}
          </Button>
        </AlertDescription>
      </Alert>
    )
  }

  if (call.phase === 'error') {
    return (
      <Alert variant="destructive" role="alert">
        <AlertTriangleIcon />
        <AlertDescription>{t('consultation.room.error.generic')}</AlertDescription>
      </Alert>
    )
  }

  if (call.phase === 'ended') {
    return <p className="text-sm text-muted-foreground">{t('consultation.room.ended')}</p>
  }

  return (
    <div className="flex flex-col gap-4">
      {call.connectTimeoutHit && !call.isAudioOnly && (
        <Alert role="status">
          <AlertTriangleIcon />
          <AlertDescription>
            <p>{t('consultation.room.connectTimeout.message')}</p>
            <div className="mt-2 flex gap-2">
              <Button type="button" size="sm" variant="outline" onClick={call.switchToAudioOnly}>
                {t('consultation.room.connectTimeout.suggestion')}
              </Button>
              <Button type="button" size="sm" variant="ghost" onClick={call.dismissConnectTimeout}>
                {t('consultation.room.poorConnection.dismiss')}
              </Button>
            </div>
          </AlertDescription>
        </Alert>
      )}

      {call.poorConnection && !call.isAudioOnly && (
        <Alert role="status">
          <AlertTriangleIcon />
          <AlertDescription>
            <p>{t('consultation.room.poorConnection.message')}</p>
            <p>{t('consultation.room.poorConnection.suggestion')}</p>
            <div className="mt-2 flex gap-2">
              <Button type="button" size="sm" variant="outline" onClick={call.switchToAudioOnly}>
                {t('consultation.room.poorConnection.accept')}
              </Button>
              <Button type="button" size="sm" variant="ghost" onClick={call.dismissPoorConnection}>
                {t('consultation.room.poorConnection.dismiss')}
              </Button>
            </div>
          </AlertDescription>
        </Alert>
      )}

      {call.peerLeft && (
        <Alert role="status">
          <AlertDescription>{t('consultation.room.peerLeft')}</AlertDescription>
        </Alert>
      )}

      {call.isAudioOnly && (
        <Alert role="status">
          <AlertDescription>{t('consultation.room.audioOnlyActive')}</AlertDescription>
        </Alert>
      )}

      {(call.phase === 'requesting-media' || call.phase === 'connecting') && (
        <div role="status" className="grid gap-1 rounded-md border border-border p-4 text-sm">
          <span>{t('consultation.room.connecting')}</span>
          <span className="text-muted-foreground">{t('consultation.room.connectingHint')}</span>
          {!call.remoteStream && call.phase === 'connecting' && (
            <span className="text-muted-foreground">{t('consultation.room.waitingForPeer')}</span>
          )}
        </div>
      )}

      {!call.isAudioOnly && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <VideoTile stream={call.remoteStream} muted={false} label={t('consultation.room.remoteVideoLabel')} />
          <VideoTile stream={call.localStream} muted label={t('consultation.room.localVideoLabel')} />
        </div>
      )}

      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label={t(call.isMuted ? 'consultation.room.unmute' : 'consultation.room.mute')}
          aria-pressed={call.isMuted}
          onClick={call.toggleMute}
        >
          {call.isMuted ? <MicOffIcon /> : <MicIcon />}
        </Button>
        {!call.isAudioOnly && (
          <Button
            type="button"
            variant="outline"
            size="icon"
            aria-label={t('consultation.room.switchToAudio')}
            onClick={call.switchToAudioOnly}
          >
            <VideoOffIcon />
          </Button>
        )}
        <Button
          type="button"
          variant="destructive"
          size="icon"
          aria-label={t('consultation.room.endCall')}
          onClick={() => {
            call.endCall()
            onEndCall?.()
          }}
        >
          <PhoneOffIcon />
        </Button>
      </div>
    </div>
  )
}
