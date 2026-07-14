import api from './api'
import type { ApiResult } from './api'

// ==================== Auth API ====================

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  userId: number
  username: string
  userType: number       // 1=学生 2=教师 3=管理员
  userTypeName: string
  accessToken: string
  tokenType: string
  expireAt: string       // ISO datetime
  passwordResetRequired: boolean
}

export function login(data: LoginRequest): Promise<LoginResponse> {
  return api.post<ApiResult<LoginResponse>>('/auth/login', data)
    .then(res => res.data.data)
}

export function logout(): Promise<void> {
  return api.post('/auth/logout').then(() => {})
}

export function changePassword(data: { oldPassword: string; newPassword: string; confirmPassword: string }): Promise<void> {
  return api.post('/auth/change-password', data).then(() => {})
}

// ==================== Profile API ====================

export interface MyProfileResponse {
  userId: number
  profileId: number
  personId: number
  username: string
  userType: number
  userTypeName: string
  realName: string
  genderType: number
  phone: string
  email: string
  avatarUrl: string
  studentNo: string
  employeeNo: string
  departmentName: string
  className: string
  titleName: string
  accountStatus: number
  profileStatus: number
}

export function getMyProfile(): Promise<MyProfileResponse> {
  return api.get<ApiResult<MyProfileResponse>>('/profile/me')
    .then(res => res.data.data)
}
