import { setupServer } from 'msw/node'
import { authHandlers } from './authHandlers'
import { clinicHandlers } from './clinicHandlers'

export const server = setupServer(...authHandlers, ...clinicHandlers)
