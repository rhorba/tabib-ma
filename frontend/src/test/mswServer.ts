import { setupServer } from 'msw/node'
import { authHandlers } from './authHandlers'
import { clinicHandlers } from './clinicHandlers'
import { bookingHandlers } from './bookingHandlers'

export const server = setupServer(...authHandlers, ...clinicHandlers, ...bookingHandlers)
