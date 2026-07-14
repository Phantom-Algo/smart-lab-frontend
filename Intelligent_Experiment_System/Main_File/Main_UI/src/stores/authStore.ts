import { reactive, computed } from 'vue'
import { login as apiLogin, getMyProfile, type LoginResponse, type MyProfileResponse } from '../utils/authApi'

type UserRole = 1 | 2 | 3

const state = reactive({
  userRole: 3 as UserRole,
  userName: '管理员',
  userId: 0,
  loggedIn: false,       // 是否已通过 API 登录
  mockMode: false,       // 已永久关闭 Mock 模式，所有数据来自数据库
})

export const userRole = computed(() => state.userRole)
export const userName = computed(() => state.userName)
export const userId = computed(() => state.userId)
export const isLoggedIn = computed(() => state.loggedIn || state.mockMode)
export const isMockMode = computed(() => state.mockMode)

export const roleLabel = computed(() => {
  const map: Record<number, string> = { 1: '学生', 2: '教师', 3: '管理员' }
  return map[state.userRole] || '未知'
})

export const isAdmin = computed(() => state.userRole === 3)
export const isTeacher = computed(() => state.userRole === 2)
export const isStudent = computed(() => state.userRole === 1)

// === 不再支持 Mock 模式，必须通过 API 登录 ===
export function setRole(role: UserRole): void {
  // Mock 模式已永久关闭，请使用真实登录
  console.warn('Mock 模式已关闭，请通过 API 登录')
}

export function loadRole(): void {
  // 尝试从 token 恢复真实登录
  const token = localStorage.getItem('accessToken')
  if (token) {
    const storedUserId = Number(localStorage.getItem('userId') || '0')
    const storedUserName = localStorage.getItem('userName') || ''
    if (storedUserId > 0) state.userId = storedUserId
    if (storedUserName) state.userName = storedUserName
    state.loggedIn = true
    state.mockMode = false
    loadProfile()
    return
  }
  // 无 token：显示登录页面
  state.loggedIn = false
  state.mockMode = false
  localStorage.removeItem('userName')
}

// === 真实登录 ===
export async function doLogin(username: string, password: string): Promise<LoginResponse> {
  const res = await apiLogin({ username, password })
  state.userId = res.userId
  state.userRole = res.userType as UserRole
  state.userName = res.username
  state.loggedIn = true
  state.mockMode = false
  localStorage.setItem('accessToken', res.accessToken)
  localStorage.setItem('userId', String(res.userId))
  localStorage.setItem('userName', res.username)
  localStorage.removeItem('userRole')
  // Mock 模式已永久关闭
  // 登录后加载完整 profile
  try {
    await loadProfile()
  } catch { /* profile 加载失败不阻塞登录 */ }
  return res
}

// === 加载用户资料 ===
async function loadProfile(): Promise<void> {
  try {
    const profile: MyProfileResponse = await getMyProfile()
    state.userId = profile.userId
    state.userRole = profile.userType as UserRole
    state.userName = profile.realName || profile.username
    state.loggedIn = true
    state.mockMode = false
    localStorage.setItem('userId', String(profile.userId))
    localStorage.setItem('userName', state.userName)
  } catch {
    // profile 接口失败时保持现有状态
  }
}

// === 登出 ===
export async function doLogout(): Promise<void> {
  try {
    const { logout } = await import('../utils/authApi')
    await logout()
  } catch { /* 忽略登出 API 错误 */ }
  state.userId = 0
  state.userName = ''
  state.loggedIn = false
  localStorage.removeItem('accessToken')
  localStorage.removeItem('userId')
  localStorage.removeItem('userName')
}
