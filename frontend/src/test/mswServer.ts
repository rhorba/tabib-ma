import { setupServer } from 'msw/node'
import { authHandlers } from './authHandlers'

export const server = setupServer(...authHandlers)
