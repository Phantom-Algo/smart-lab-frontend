<template>
  <div class="login-container">
    <div class="bg-layer"></div>

    <div class="glass-panel">
      <!-- 左侧 -->
      <div class="login-left">
        <div class="left-image"></div>
        <div class="left-glass"></div>
        <div class="platform-brand">
          <div class="brand-icon">
            <svg viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="#444" stroke-width="1.2">
              <path d="M9 3h6M10 3v4.5c0 1.5-2 3-2 5.5v6a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2v-6c0-2.5-2-4-2-5.5V3"/>
              <circle cx="12" cy="8" r="1.5" fill="#666"/>
            </svg>
          </div>
          <h1 class="brand-title">智慧实验室</h1>
          <p class="brand-subtitle">运行及管理平台</p>
        </div>
      </div>

      <!-- 右侧 -->
      <div class="login-right">
        <!-- ===== 登录 ===== -->
        <div v-if="currentView === 'login'" class="form-card">
          <div class="form-header">
            <div class="header-icon-circle">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#fff" stroke-width="1.5"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M13 12H3"/></svg>
            </div>
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-desc">登录您的账号以继续</p>
          </div>

          <div class="input-group">
            <div class="input-box">
              <svg class="input-ico" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input v-model="loginForm.username" type="text" placeholder="学号 / 工号" />
            </div>
            <div class="input-box">
              <svg class="input-ico" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="loginForm.password" :type="showPwd ? 'text' : 'password'" placeholder="密码" />
              <span class="pwd-toggle" @click="showPwd = !showPwd">
                <svg v-if="showPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </span>
            </div>
            <div class="captcha-row">
              <div class="input-box captcha-box">
                <input v-model="loginForm.captcha" type="text" placeholder="验证码" maxlength="4" />
              </div>
              <div class="captcha-img" @click="refreshCaptcha">{{ captchaCode }}</div>
            </div>
          </div>

          <div v-if="loginError" class="error-msg">{{ loginError }}</div>

          <button class="submit-btn" @click="handleLogin" :disabled="loginLoading">
            {{ loginLoading ? '登录中...' : '登 录' }}
          </button>

          <div class="form-links">
            <span @click="currentView = 'forgot'">忘记密码</span>
            <span class="link-divider">|</span>
            <span @click="quickMock">离线 Demo</span>
          </div>
        </div>

        <!-- ===== 忘记密码 ===== -->
        <div v-if="currentView === 'forgot'" class="form-card">
          <div class="form-header">
            <div class="header-icon-circle">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#fff" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/><circle cx="12" cy="16" r="1"/></svg>
            </div>
            <h2 class="form-title">重置密码</h2>
            <p class="form-desc">验证身份后设置新密码</p>
          </div>

          <div class="input-group">
            <div class="input-box">
              <svg class="input-ico" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input v-model="forgotForm.username" type="text" placeholder="学号 / 工号" />
            </div>
            <div class="input-box">
              <svg class="input-ico" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
              <input v-model="forgotForm.phone" type="text" placeholder="绑定的手机号" />
            </div>
            <div class="input-box">
              <svg class="input-ico" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="forgotForm.newPwd" :type="showForgotPwd ? 'text' : 'password'" placeholder="新密码" />
              <span class="pwd-toggle" @click="showForgotPwd = !showForgotPwd">
                <svg v-if="showForgotPwd" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
              </span>
            </div>
            <div class="input-box">
              <svg class="input-ico" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="forgotForm.confirmPwd" :type="showForgotPwd ? 'text' : 'password'" placeholder="确认新密码" />
            </div>
            <div class="captcha-row">
              <div class="input-box captcha-box">
                <input v-model="forgotForm.captcha" type="text" placeholder="验证码" maxlength="4" />
              </div>
              <div class="captcha-img" @click="refreshForgotCaptcha">{{ forgotCaptchaCode }}</div>
            </div>
          </div>

          <button class="submit-btn" @click="handleForgotSubmit">重置密码</button>

          <div class="form-links">
            <span @click="currentView = 'login'">想起密码？返回登录</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

const API_BASE = 'http://localhost:8080/api'

const currentView = ref<'login' | 'forgot'>('login')
const showPwd = ref(false)
const showForgotPwd = ref(false)
const loginLoading = ref(false)
const loginError = ref('')

const loginForm = reactive({ username: '', password: '', captcha: '' })

const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
const gen = (): string => { let c=''; for(let i=0;i<4;i++) c+=chars[Math.floor(Math.random()*chars.length)]; return c }
const captchaCode = ref(gen())
function refreshCaptcha(): void { captchaCode.value = gen() }

const forgotForm = reactive({ username: '', phone: '', newPwd: '', confirmPwd: '', captcha: '' })
const forgotCaptchaCode = ref(gen())
function refreshForgotCaptcha(): void { forgotCaptchaCode.value = gen() }

async function handleLogin() {
  loginError.value = ''
  if (!loginForm.username) { loginError.value = '请输入用户名'; return }
  if (!loginForm.password) { loginError.value = '请输入密码'; return }
  if (!loginForm.captcha) { loginError.value = '请输入验证码'; return }
  if (loginForm.captcha.toUpperCase() !== captchaCode.value) {
    refreshCaptcha(); loginForm.captcha = ''
    loginError.value = '验证码错误'; return
  }

  loginLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: loginForm.username, password: loginForm.password })
    })
    const data = await res.json()
    if (data.code !== 0) {
      loginError.value = data.message || '登录失败，请检查用户名和密码'
      return
    }
    // 登录成功：保存 token 并跳转到主界面
    const loginResult = data.data
    localStorage.setItem('accessToken', loginResult.accessToken)
    localStorage.setItem('userId', String(loginResult.userId))
    localStorage.removeItem('userRole')
    localStorage.setItem('useMock', '0')
    window.location.href = 'http://localhost:5173'
  } catch {
    loginError.value = '无法连接服务器，请确认后端已启动（localhost:8080）'
  } finally {
    loginLoading.value = false
  }
}

function quickMock(): void {
  localStorage.setItem('useMock', '1')
  window.location.href = 'http://localhost:5173'
}

function handleForgotSubmit() {
  if (!forgotForm.username) return alert('请输入用户名')
  if (!forgotForm.phone) return alert('请输入绑定的手机号')
  if (!forgotForm.newPwd) return alert('请输入新密码')
  if (forgotForm.newPwd !== forgotForm.confirmPwd) return alert('两次密码不一致')
  if (!forgotForm.captcha) return alert('请输入验证码')
  if (forgotForm.captcha.toUpperCase() !== forgotCaptchaCode.value) { refreshForgotCaptcha(); forgotForm.captcha=''; return alert('验证码错误') }
  alert('密码重置功能暂未对接后端，请联系管理员')
  currentView.value = 'login'
}
</script>

<style scoped>
.login-container { width:100%; height:100vh; position:relative; overflow:hidden; }
.bg-layer { position:fixed; inset:0; width:100vw; height:100vh; background:url('./assets/background.webp') center/cover no-repeat; opacity:.25; }
.glass-panel { position:absolute; inset:0; display:flex; }

/* ===== 左侧 ===== */
.login-left { flex:1; position:relative; overflow:hidden; }
.left-image { position:absolute; width:100vw; height:100vh; left:0; top:0; background:url('./assets/LoginUIBG.webp') center/cover no-repeat; }
.left-glass { position:absolute; inset:0; background:rgba(255,255,255,.08); backdrop-filter:blur(1px); -webkit-backdrop-filter:blur(1px); }
.platform-brand { position:absolute; top:20%; left:50%; transform:translateX(-50%); text-align:center; z-index:3; user-select:none; }
.brand-icon { margin-bottom:6px; }
.brand-title {
  font-size:38px; font-weight:800; margin:0 0 6px; letter-spacing:5px; white-space:nowrap;
  background:linear-gradient(135deg, #2c2c2c 20%, #555 45%, #1a1a1a 70%, #3a3a3a 85%);
  background-size:200% auto;
  -webkit-background-clip:text; -webkit-text-fill-color:transparent;
  background-clip:text;
  animation:shimmer 3s ease-in-out infinite;
}
.brand-subtitle {
  font-size:16px; font-weight:400; color:#555; letter-spacing:8px; margin:0;
}
@keyframes shimmer {
  0%, 100% { background-position:0% center; }
  50% { background-position:200% center; }
}

/* ===== 右侧 ===== */
.login-right { flex:1; display:flex; align-items:center; justify-content:center; padding:50px 40px; background:rgba(255,255,255,.04); backdrop-filter:blur(1px); -webkit-backdrop-filter:blur(1px); }

.form-card { width:100%; max-width:380px; }
.register-card { max-width:460px; max-height:90vh; overflow-y:auto; padding-right:6px; }

/* 表单头部 */
.form-header { text-align:center; margin-bottom:28px; }
.header-icon-circle { width:52px; height:52px; border-radius:50%; background:linear-gradient(135deg,#1a73e8,#4a90d9); display:inline-flex; align-items:center; justify-content:center; margin-bottom:14px; box-shadow:0 4px 16px rgba(26,115,232,.25); }
.form-title { font-size:22px; font-weight:700; color:#1a1a2e; margin:0 0 4px; }
.form-desc { font-size:13px; color:#999; margin:0; }

/* 输入组 */
.input-group { display:flex; flex-direction:column; gap:10px; margin-bottom:20px; }

.section-title { font-size:12px; font-weight:600; color:#aaa; text-transform:uppercase; letter-spacing:1px; padding:4px 0 0; }

.row { display:flex; gap:10px; }

.input-box { flex:1; display:flex; align-items:center; gap:8px; padding:0 13px; background:rgba(255,255,255,.55); border:1px solid rgba(0,0,0,.06); border-radius:10px; transition:all .2s; }
.input-box:focus-within { border-color:#1a73e8; background:rgba(255,255,255,.85); box-shadow:0 0 0 3px rgba(26,115,232,.08); }
.input-box.half { flex:1; min-width:0; }
.input-box input { flex:1; padding:11px 0; border:none; background:transparent; font-size:14px; color:#333; outline:none; }
.input-box input::placeholder { color:#bbb; }
.input-box input:disabled { color:#ccc; }
.input-ico { color:#bbb; flex-shrink:0; }
.pwd-toggle { display:flex; align-items:center; color:#aaa; cursor:pointer; flex-shrink:0; padding:2px; border-radius:4px; transition:all .15s; }
.pwd-toggle:hover { color:#1a73e8; background:rgba(26,115,232,.06); }

/* 下拉框 */
select.half { flex:1; min-width:0; padding:0 13px; background:rgba(255,255,255,.55); border:1px solid rgba(0,0,0,.06); border-radius:10px; font-size:14px; color:#333; outline:none; cursor:pointer; transition:all .2s; appearance:none; -webkit-appearance:none; background-image:url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E"); background-repeat:no-repeat; background-position:right 10px center; height:43px; line-height:43px; }
select.half:focus { border-color:#1a73e8; box-shadow:0 0 0 3px rgba(26,115,232,.08); }

/* 验证码 */
.captcha-row { display:flex; gap:10px; }
.captcha-box { flex:1; }
.captcha-img { width:100px; height:43px; border-radius:10px; background:linear-gradient(135deg,#e8f0fe,#d4e4fc); display:flex; align-items:center; justify-content:center; cursor:pointer; user-select:none; border:1px solid rgba(26,115,232,.12); font-size:21px; font-weight:700; letter-spacing:3px; color:#1a73e8; font-family:'Courier New',monospace; transition:all .15s; }
.captcha-img:hover { border-color:#1a73e8; }

/* 按钮 */
.submit-btn { width:100%; padding:12px 0; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#1557b0); color:#fff; font-size:15px; font-weight:600; letter-spacing:8px; cursor:pointer; transition:all .2s; box-shadow:0 4px 14px rgba(26,115,232,.3); }
.submit-btn:hover { transform:translateY(-1px); box-shadow:0 6px 20px rgba(26,115,232,.4); }
.submit-btn:disabled { opacity:0.6; cursor:not-allowed; transform:none; }

/* 错误提示 */
.error-msg { font-size:13px; color:#e74c3c; margin-bottom:14px; text-align:center; padding:8px 12px; background:rgba(231,76,60,.06); border-radius:8px; }

/* 底部链接 */
.form-links { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px; }
.form-links span { font-size:13px; color:#1a73e8; cursor:pointer; }
.form-links span:hover { text-decoration:underline; }
.link-divider { color:#ddd !important; cursor:default !important; }
.link-divider:hover { text-decoration:none !important; }

/* 滚动条 */
.register-card::-webkit-scrollbar { width:4px; }
.register-card::-webkit-scrollbar-thumb { background:#ddd; border-radius:2px; }
</style>
