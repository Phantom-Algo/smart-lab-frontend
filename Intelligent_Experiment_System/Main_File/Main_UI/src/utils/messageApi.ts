import api from './api'
import type { ApiResult, PageResult } from './api'

// ==================== 消息模板 ====================
export interface MessageTemplateItem {
  id: number; 
  templateCode: string; 
  templateName: string; 
  messageType: string;
  titleTemplate: string; 
  contentTemplate: string; 
  contentFormat: 'TEXT' | 'MARKDOWN' | 'HTML';
  status: number; 
  createdAt: string;
}

export interface MessageTemplatePageQuery {
  pageNum?: number; pageSize?: number; templateCode?: string; messageType?: string
}

export function getTemplatePage(query: MessageTemplatePageQuery): Promise<PageResult<MessageTemplateItem>> {
  return api.get<ApiResult<PageResult<MessageTemplateItem>>>('/message-templates', { params: query }).then(r => r.data.data)
}
export function getTemplateDetail(id: number): Promise<MessageTemplateItem> {
  return api.get<ApiResult<MessageTemplateItem>>(`/message-templates/${id}`).then(r => r.data.data)
}
export function createTemplate(data: Partial<MessageTemplateItem>): Promise<MessageTemplateItem> {
  return api.post<ApiResult<MessageTemplateItem>>('/message-templates', data).then(r => r.data.data)
}
export function updateTemplate(id: number, data: Partial<MessageTemplateItem>): Promise<MessageTemplateItem> {
  return api.put<ApiResult<MessageTemplateItem>>(`/message-templates/${id}`, data).then(r => r.data.data)
}
export function deleteTemplate(id: number): Promise<void> {
  return api.delete(`/message-templates/${id}`).then(() => {})
}

// ==================== 消息发布 ====================
export interface PublishMessageRequest {
  messageType: string; targetType: string; targetUserIds?: number[]
  templateCode?: string; title?: string; content?: string; contentFormat?: string
  actionUrl?: string
  params?: Record<string, string>
}

export interface PublishMessageResult {
  eventNo: string; dispatchedCount: number
}

export function publishMessage(data: PublishMessageRequest): Promise<PublishMessageResult> {
  return api.post<ApiResult<PublishMessageResult>>('/messages', data).then(r => r.data.data)
}

// ==================== 收件人搜索 ====================
export interface RecipientSearchResult {
  userId: number; personNo: string; personName: string; username: string; userType: number
}

export function searchRecipients(keyword: string, limit?: number): Promise<RecipientSearchResult[]> {
  return api.get<ApiResult<RecipientSearchResult[]>>('/messages/recipients/search',
    { params: { keyword, limit: limit || 20 } }).then(r => r.data.data)
}
