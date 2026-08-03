import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders, screen } from '@/test/renderWithProviders'
import type { useConsultationCall } from '../hooks/useConsultationCall'
import { VideoConsultationRoom } from './VideoConsultationRoom'

type Call = ReturnType<typeof useConsultationCall>

// The component only ever reads from `call` and calls its action functions —
// it doesn't invoke the real hook, so every phase/flag combination can be
// constructed directly without touching WebRTC/getUserMedia at all.
function makeCall(overrides: Partial<Call> = {}): Call {
  return {
    phase: 'connected',
    localStream: null,
    remoteStream: null,
    isMuted: false,
    isAudioOnly: false,
    poorConnection: false,
    connectTimeoutHit: false,
    peerLeft: false,
    errorMessage: null,
    join: vi.fn(),
    endCall: vi.fn(),
    toggleMute: vi.fn(),
    switchToAudioOnly: vi.fn(),
    dismissPoorConnection: vi.fn(),
    dismissConnectTimeout: vi.fn(),
    ...overrides,
  }
}

describe('VideoConsultationRoom', () => {
  it('shows the permission-denied state with a retry action', async () => {
    const call = makeCall({ phase: 'permission-denied' })
    renderWithProviders(<VideoConsultationRoom call={call} />)
    const user = userEvent.setup()

    expect(screen.getByText(/accès à la caméra\/micro refusé/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /^réessayer$/i }))
    expect(call.join).toHaveBeenCalled()
  })

  it('shows a generic error state', () => {
    renderWithProviders(<VideoConsultationRoom call={makeCall({ phase: 'error' })} />)
    expect(screen.getByText(/une erreur est survenue pendant la consultation/i)).toBeInTheDocument()
  })

  it('shows the ended state', () => {
    renderWithProviders(<VideoConsultationRoom call={makeCall({ phase: 'ended' })} />)
    expect(screen.getByText(/appel terminé/i)).toBeInTheDocument()
  })

  it('shows a waiting-for-peer hint while connecting with no remote stream yet', () => {
    renderWithProviders(<VideoConsultationRoom call={makeCall({ phase: 'connecting' })} />)
    expect(screen.getByText(/en attente que l'autre participant rejoigne/i)).toBeInTheDocument()
  })

  it('shows the poor-connection banner with accept/dismiss actions', async () => {
    const call = makeCall({ poorConnection: true })
    renderWithProviders(<VideoConsultationRoom call={call} />)
    const user = userEvent.setup()

    expect(screen.getByText(/connexion instable détectée/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /^passer en audio$/i }))
    expect(call.switchToAudioOnly).toHaveBeenCalled()
    await user.click(screen.getByRole('button', { name: /continuer en vidéo/i }))
    expect(call.dismissPoorConnection).toHaveBeenCalled()
  })

  it('shows the connect-timeout banner with a switch-to-audio suggestion', async () => {
    const call = makeCall({ connectTimeoutHit: true })
    renderWithProviders(<VideoConsultationRoom call={call} />)
    const user = userEvent.setup()

    expect(screen.getByText(/la connexion prend plus de temps que prévu/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /essayer l'audio seul/i }))
    expect(call.switchToAudioOnly).toHaveBeenCalled()
  })

  it('hides the video tiles and the switch-to-audio control in audio-only mode', () => {
    renderWithProviders(<VideoConsultationRoom call={makeCall({ isAudioOnly: true })} />)
    expect(screen.getByText(/mode audio seul activé/i)).toBeInTheDocument()
    expect(screen.queryByLabelText(/passer en audio seul/i)).not.toBeInTheDocument()
  })

  it('shows a peer-left notice', () => {
    renderWithProviders(<VideoConsultationRoom call={makeCall({ peerLeft: true })} />)
    expect(screen.getByText(/l'autre participant a quitté l'appel/i)).toBeInTheDocument()
  })

  it('toggles mute and ends the call', async () => {
    const call = makeCall()
    const onEndCall = vi.fn()
    renderWithProviders(<VideoConsultationRoom call={call} onEndCall={onEndCall} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /couper le micro/i }))
    expect(call.toggleMute).toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: /terminer l'appel/i }))
    expect(call.endCall).toHaveBeenCalled()
    expect(onEndCall).toHaveBeenCalled()
  })
})
