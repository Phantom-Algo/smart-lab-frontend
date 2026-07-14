export interface RealtimeEnvelope<T = Record<string, unknown>> {
  event: string
  source: string
  occurredAt: string
  data: T
}

export const REALTIME_EVENT_NAME = 'realtime:event'
export const REALTIME_MESSAGE_NEW_EVENT = 'realtime:message.new'

const HEARTBEAT_INTERVAL_MS = 25000
const RECONNECT_DELAY_MS = 3000

let socket: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let shouldReconnect = false

function buildRealtimeUrl(token: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/events?token=${encodeURIComponent(token)}`
}

function clearReconnectTimer(): void {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function clearHeartbeatTimer(): void {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function startHeartbeat(): void {
  clearHeartbeatTimer()
  heartbeatTimer = setInterval(() => {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send('PING')
    }
  }, HEARTBEAT_INTERVAL_MS)
}

function scheduleReconnect(): void {
  if (!shouldReconnect || reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connectRealtime()
  }, RECONNECT_DELAY_MS)
}

function dispatchEnvelope(envelope: RealtimeEnvelope): void {
  window.dispatchEvent(new CustomEvent<RealtimeEnvelope>(REALTIME_EVENT_NAME, { detail: envelope }))
  window.dispatchEvent(new CustomEvent<RealtimeEnvelope>(`realtime:${envelope.event}`, { detail: envelope }))
}

function handleSocketMessage(rawData: string): void {
  if (rawData === 'PONG') return
  try {
    const parsed = JSON.parse(rawData) as RealtimeEnvelope
    if (!parsed || typeof parsed.event !== 'string') return
    dispatchEnvelope(parsed)
  } catch {
    // Ignore non-JSON or malformed realtime payloads.
  }
}

export function connectRealtime(): void {
  const token = localStorage.getItem('accessToken')
  if (!token) return

  shouldReconnect = true
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return
  }

  clearReconnectTimer()
  socket = new WebSocket(buildRealtimeUrl(token))

  socket.onopen = () => {
    startHeartbeat()
  }

  socket.onmessage = (event) => {
    if (typeof event.data === 'string') {
      handleSocketMessage(event.data)
    }
  }

  socket.onerror = () => {
    // Let the close callback drive reconnect behavior.
  }

  socket.onclose = () => {
    clearHeartbeatTimer()
    socket = null
    if (shouldReconnect && localStorage.getItem('accessToken')) {
      scheduleReconnect()
    }
  }
}

export function disconnectRealtime(): void {
  shouldReconnect = false
  clearReconnectTimer()
  clearHeartbeatTimer()
  if (!socket) return
  const currentSocket = socket
  socket = null
  currentSocket.close()
}