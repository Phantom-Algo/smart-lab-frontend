export type AiStreamEventName = 'meta' | 'status' | 'delta' | 'done' | 'error'

export interface AiStreamEvent {
  id?: string
  event: AiStreamEventName
  data: Record<string, unknown>
}

export interface StreamAiMessageOptions {
  sessionNo: string
  clientMessageId: string
  content: string
  modelCode?: string
  signal: AbortSignal
  onEvent: (event: AiStreamEvent) => void
}

export async function streamAiMessage(options: StreamAiMessageOptions): Promise<void> {
  const token = localStorage.getItem('accessToken')
  const response = await fetch(
    `/api/admin/ai-chat/sessions/${encodeURIComponent(options.sessionNo)}/messages:stream`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({
        clientMessageId: options.clientMessageId,
        content: options.content,
        modelCode: options.modelCode,
        includeSql: false,
      }),
      signal: options.signal,
    },
  )
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string } | null
    throw new Error(body?.message || `请求失败（HTTP ${response.status}）`)
  }
  if (!response.body) throw new Error('浏览器未提供流式响应体')

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, '\n')
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const frame = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      dispatchFrame(frame, options.onEvent)
      boundary = buffer.indexOf('\n\n')
    }
    if (done) break
  }
  if (buffer.trim()) dispatchFrame(buffer, options.onEvent)
}

function dispatchFrame(frame: string, callback: (event: AiStreamEvent) => void): void {
  if (!frame.trim() || frame.trimStart().startsWith(':')) return
  let id: string | undefined
  let event: AiStreamEventName = 'delta'
  const data: string[] = []
  for (const line of frame.split('\n')) {
    if (line.startsWith('id:')) id = line.slice(3).trim()
    else if (line.startsWith('event:')) event = line.slice(6).trim() as AiStreamEventName
    else if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
  }
  if (!data.length) return
  callback({ id, event, data: JSON.parse(data.join('\n')) as Record<string, unknown> })
}
