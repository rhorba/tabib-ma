import { setupServer } from 'msw/node'
import { authHandlers } from './authHandlers'
import { clinicHandlers } from './clinicHandlers'
import { bookingHandlers } from './bookingHandlers'
import { consultationHandlers } from './consultationHandlers'
import { prescriptionHandlers } from './prescriptionHandlers'
import { reviewHandlers } from './reviewHandlers'

export const server = setupServer(
  ...authHandlers,
  ...clinicHandlers,
  ...bookingHandlers,
  ...consultationHandlers,
  ...prescriptionHandlers,
  ...reviewHandlers
)
