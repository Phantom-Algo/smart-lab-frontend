import api from './api'
import type { ApiResult, PageResult } from './api'

// ==================== 收件箱消息 ====================

export interface InboxMessageListItem {
  inboxMessageId: number
  messageId: number
  title: string
  priority: number         // 1=预警 2=系统 3=普通
  priorityDescription: string
  messageType: string
  sentAt: string
  receivedAt: string
  read: boolean
  readAt: string | null
}

export interface InboxMessageDetail extends InboxMessageListItem {
  content: string
  contentFormat: 'TEXT' | 'MARKDOWN' | 'HTML'
  actionUrl: string | null
}

export interface InboxPageQuery {
  pageNum?: number
  pageSize?: number
  priority?: number
  messageType?: string
  isRead?: number          // 0=未读 1=已读
}

// 分页查询收件箱
export function getInboxMessages(query: InboxPageQuery): Promise<PageResult<InboxMessageListItem>> {
  return api.get<ApiResult<PageResult<InboxMessageListItem>>>('/inbox/messages', { params: query })
    .then(res => res.data.data)
}

// 查看消息详情（自动标记已读）
export function getInboxMessageDetail(inboxMessageId: number): Promise<InboxMessageDetail> {
  return api.get<ApiResult<InboxMessageDetail>>(`/inbox/messages/${inboxMessageId}`)
    .then(res => res.data.data)
}

// 删除单条消息
export function deleteInboxMessage(inboxMessageId: number): Promise<void> {
  return api.delete(`/inbox/messages/${inboxMessageId}`).then(() => {})
}

// 批量删除消息
export function batchDeleteInboxMessages(inboxMessageIds: number[]): Promise<void> {
  return api.delete('/inbox/messages/batch', { data: { inboxMessageIds } }).then(() => {})
}

// 获取新消息 ID 列表（用于未读数角标）
export function getNewMessageIds(): Promise<number[]> {
  return api.get<ApiResult<number[]>>('/inbox/messages/new-ids')
    .then(res => res.data.data)
}

// 标记收件箱已查看
export function markInboxViewed(): Promise<void> {
  return api.post('/inbox/messages/viewed').then(() => {})
}

// ==================== 管理员：查看其他用户收件箱 ====================

// 分页查询指定用户的收件箱
export function getAdminUserInbox(userId: number, query: InboxPageQuery): Promise<PageResult<InboxMessageListItem>> {
  return api.get<ApiResult<PageResult<InboxMessageListItem>>>(`/admin/inboxes/${userId}/messages`, { params: query })
    .then(res => res.data.data)
}

// 查看指定用户的消息详情
export function getAdminUserMessageDetail(userId: number, inboxMessageId: number): Promise<InboxMessageDetail> {
  return api.get<ApiResult<InboxMessageDetail>>(`/admin/inboxes/${userId}/messages/${inboxMessageId}`)
    .then(res => res.data.data)
}

// 删除指定用户的消息
export function deleteAdminUserMessage(userId: number, inboxMessageId: number): Promise<void> {
  return api.delete(`/admin/inboxes/${userId}/messages/${inboxMessageId}`).then(() => {})
}

// 批量删除指定用户的消息
export function batchDeleteAdminUserMessages(userId: number, inboxMessageIds: number[]): Promise<void> {
  return api.delete(`/admin/inboxes/${userId}/messages/batch`, { data: inboxMessageIds }).then(() => {})
}

// ==================== 管理员：发布消息 ====================

export interface PublishMessageRequest {
  messageType: string
  targetType: string
  targetUserIds?: number[]
  templateCode?: string
  params?: Record<string, unknown>
  title?: string
  content?: string
  contentFormat?: 'TEXT' | 'MARKDOWN' | 'HTML'
  actionUrl?: string
}

export interface PublishMessageResponse {
  eventNo: string
  streamRecordId: string
}

export function publishMessage(data: PublishMessageRequest): Promise<PublishMessageResponse> {
  return api.post<ApiResult<PublishMessageResponse>>('/messages', data)
    .then(res => res.data.data)
}

// ==================== 管理员：消息模板管理 ====================

export interface MessageTemplateItem {
  id: number
  templateCode: string
  templateName: string
  messageType: string
  titleTemplate: string
  contentTemplate: string
  contentFormat: 'TEXT' | 'MARKDOWN' | 'HTML'
  paramSchema: Record<string, unknown> | null
  status: number
  createdBy: number
  updatedBy: number
  createdAt: string
  updatedAt: string
}

export interface TemplatePageQuery {
  pageNum?: number
  pageSize?: number
  templateCode?: string
  templateName?: string
  messageType?: string
  status?: number
}

export function getMessageTemplates(query: TemplatePageQuery): Promise<PageResult<MessageTemplateItem>> {
  return api.get<ApiResult<PageResult<MessageTemplateItem>>>('/message-templates', { params: query })
    .then(res => res.data.data)
}
