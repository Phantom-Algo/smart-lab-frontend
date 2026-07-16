import api, { type ApiResult, type PageResult } from './api'

export interface AiModel {
  modelCode: string
  displayName: string
  providerCode: string
  defaultModel: boolean
  capabilities: string
}

export interface AiChatSession {
  sessionNo: string
  title: string
  modelCode: string
  modelDisplayName: string
  sessionStatus: number
  lastMessageAt: string | null
  createdAt: string
  updatedAt: string
}

export interface AiChatMessage {
  messageNo: string
  sequenceNo: number
  messageType: 'HUMAN' | 'AI'
  content: string
  contentFormat: 'TEXT' | 'MARKDOWN'
  messageStatus: string
  modelCode: string | null
  finishReason: string | null
  errorCode: string | null
  createdAt: string
  completedAt: string | null
}

export async function getAiModels(): Promise<AiModel[]> {
  const response = await api.get<ApiResult<AiModel[]>>('/admin/ai-chat/models')
  return response.data.data
}

export async function getAiSessions(pageNum = 1, pageSize = 50): Promise<PageResult<AiChatSession>> {
  const response = await api.get<ApiResult<PageResult<AiChatSession>>>('/admin/ai-chat/sessions', {
    params: { pageNum, pageSize },
  })
  return response.data.data
}

export async function createAiSession(modelCode?: string): Promise<AiChatSession> {
  const response = await api.post<ApiResult<AiChatSession>>('/admin/ai-chat/sessions', { modelCode })
  return response.data.data
}

export async function updateAiSession(
  sessionNo: string,
  update: { title?: string; modelCode?: string },
): Promise<AiChatSession> {
  const response = await api.patch<ApiResult<AiChatSession>>(
    `/admin/ai-chat/sessions/${sessionNo}`,
    update,
  )
  return response.data.data
}

export async function deleteAiSession(sessionNo: string): Promise<void> {
  await api.delete(`/admin/ai-chat/sessions/${sessionNo}`)
}

export async function getAiMessages(sessionNo: string): Promise<AiChatMessage[]> {
  const response = await api.get<ApiResult<PageResult<AiChatMessage>>>(
    `/admin/ai-chat/sessions/${sessionNo}/messages`,
    { params: { pageNum: 1, pageSize: 200 } },
  )
  return response.data.data.records
}

export async function cancelAiRun(runNo: string): Promise<void> {
  await api.post(`/admin/ai-chat/runs/${runNo}/cancel`)
}

