<template>
  <div class="login-container">
    <div class="login-left">
      <div class="left-overlay"></div>
    </div>

    <div class="login-right">
      <div class="right-glow right-glow-top"></div>
      <div class="right-glow right-glow-bottom"></div>
      <div class="form-card">
        <div class="form-icon">
          <img src="../assets/logo.png" alt="logo" />
        </div>

        <h2 class="form-title">欢迎回来</h2>
        <p class="form-desc">登录智慧实验室管理平台</p>

        <div class="input-group">
          <div class="input-box">
            <svg class="input-ico" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <input v-model="loginForm.username" type="text" placeholder="用户名" autocomplete="username" />
          </div>
          <div class="input-box">
            <svg class="input-ico" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <input v-model="loginForm.password" :type="showPassword ? 'text' : 'password'" placeholder="密码" autocomplete="current-password" />
            <button class="field-action" type="button" @click="showPassword = !showPassword" :aria-label="showPassword ? '隐藏密码' : '显示密码'">
              <svg v-if="!showPassword" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12Z"/><circle cx="12" cy="12" r="3"/></svg>
              <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M3 3l18 18"/><path d="M10.6 10.7a3 3 0 0 0 4 4"/><path d="M9.4 5.1A10.7 10.7 0 0 1 12 5c7 0 11 7 11 7a21.8 21.8 0 0 1-4.3 5.1"/><path d="M6.7 6.7C3.9 8.2 2 12 2 12a21.8 21.8 0 0 0 8.9 6.1"/></svg>
            </button>
          </div>
          <div class="captcha-row">
            <div class="input-box captcha-box">
              <svg class="input-ico" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M12 3l7 4v5c0 4.4-2.7 7.4-7 9-4.3-1.6-7-4.6-7-9V7l7-4Z"/><path d="M9 12l2 2 4-4"/></svg>
              <input v-model="loginForm.captcha" type="text" placeholder="验证码" maxlength="4" />
            </div>
            <div class="captcha-img" @click="refreshCaptcha">{{ captchaCode }}</div>
          </div>
        </div>

        <div v-if="loginError" class="error-msg">{{ loginError }}</div>

        <div class="form-options">
          <label class="remember-me">
            <input type="checkbox" v-model="rememberMe" />
            <span>记住我</span>
          </label>
          <span class="forgot-link" @click="showForgot = true">忘记密码？</span>
        </div>

        <button class="submit-btn" @click="handleLogin" :disabled="loginLoading">
          {{ loginLoading ? '登录中...' : '登 录' }}
        </button>
      </div>

      <p class="form-footer">© 2024 智慧实验室管理平台 · 保留所有权利</p>
    </div>

    <!-- 忘记密码弹窗 -->
    <div v-if="showForgot" class="modal-overlay" @click.self="showForgot = false">
      <div class="modal-card">
        <h3 class="modal-title">重置密码</h3>
        <p class="modal-desc">验证身份后设置新密码</p>
        <div class="input-group">
          <div class="input-box">
            <input v-model="forgotForm.username" type="text" placeholder="用户名" />
          </div>
          <div class="input-box">
            <input v-model="forgotForm.phone" type="text" placeholder="绑定的手机号" />
          </div>
          <div class="input-box">
            <input v-model="forgotForm.newPwd" type="password" placeholder="新密码" />
          </div>
          <div class="input-box">
            <input v-model="forgotForm.confirmPwd" type="password" placeholder="确认新密码" />
          </div>
          <div class="captcha-row" style="padding-top:14px">
            <div class="input-box captcha-box">
              <input v-model="forgotForm.captcha" type="text" placeholder="验证码" maxlength="4" />
            </div>
            <div class="captcha-img" @click="refreshForgotCaptcha">{{ forgotCaptchaCode }}</div>
          </div>
        </div>
        <div v-if="forgotError" class="error-msg">{{ forgotError }}</div>
        <button class="submit-btn" @click="handleForgotSubmit" style="margin-top:20px">重置密码</button>
        <p class="modal-close" @click="showForgot = false">返回登录</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { doLogin } from '../stores/authStore'

const loginLoading = ref(false)
const loginError = ref('')
const rememberMe = ref(false)
const showForgot = ref(false)
const forgotError = ref('')
const showPassword = ref(false)

const loginForm = reactive({ username: '', password: '', captcha: '' })
const forgotForm = reactive({ username: '', phone: '', newPwd: '', confirmPwd: '', captcha: '' })

const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
const gen = (): string => { let c = ''; for (let i = 0; i < 4; i++) c += chars[Math.floor(Math.random() * chars.length)]; return c }
const captchaCode = ref(gen())
function refreshCaptcha(): void { captchaCode.value = gen() }

const forgotCaptchaCode = ref(gen())
function refreshForgotCaptcha(): void { forgotCaptchaCode.value = gen() }

async function handleLogin(): Promise<void> {
  loginError.value = ''
  if (!loginForm.username) { loginError.value = '请输入用户名'; return }
  if (!loginForm.password) { loginError.value = '请输入密码'; return }
  if (!loginForm.captcha) { loginError.value = '请输入验证码'; return }
  if (loginForm.captcha.toUpperCase() !== captchaCode.value) { refreshCaptcha(); loginForm.captcha = ''; loginError.value = '验证码错误'; return }
  loginLoading.value = true
  try {
    await doLogin(loginForm.username, loginForm.password)
    // 登录成功后，authStore 的响应式状态会自动切换界面，无需手动刷新页面
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败'
    if (msg.includes('401') || msg.includes('用户名或密码')) loginError.value = '用户名或密码错误'
    else if (msg.includes('Network') || msg.includes('connect')) loginError.value = '无法连接服务器，请确认后端已启动'
    else loginError.value = msg
  } finally { loginLoading.value = false }
}

function handleForgotSubmit(): void {
  forgotError.value = ''
  if (!forgotForm.username) { forgotError.value = '请输入用户名'; return }
  if (!forgotForm.phone) { forgotError.value = '请输入绑定的手机号'; return }
  if (!forgotForm.newPwd) { forgotError.value = '请输入新密码'; return }
  if (forgotForm.newPwd !== forgotForm.confirmPwd) { forgotError.value = '两次密码不一致'; return }
  if (!forgotForm.captcha) { forgotError.value = '请输入验证码'; return }
  if (forgotForm.captcha.toUpperCase() !== forgotCaptchaCode.value) { refreshForgotCaptcha(); forgotForm.captcha = ''; forgotError.value = '验证码错误'; return }
  alert('密码重置功能暂未对接后端，请联系管理员')
  showForgot.value = false
}
</script>

<style scoped>
.login-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(144, 188, 255, 0.22), transparent 30%),
    linear-gradient(135deg, #f5f9ff 0%, #edf4ff 100%);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.login-left {
  flex: 1.22;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  background: url('../assets/LoginUIBG.png') center center / cover no-repeat;
}
.left-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.06) 0%, rgba(255, 255, 255, 0) 28%);
}

.login-right {
  width: min(44vw, 620px);
  min-width: 420px;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 52px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(247, 250, 255, 0.98) 100%),
    radial-gradient(circle at left center, rgba(92, 156, 255, 0.12), transparent 38%);
}

.right-glow {
  position: absolute;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  pointer-events: none;
  background: radial-gradient(circle, rgba(122, 178, 255, 0.18) 0%, rgba(122, 178, 255, 0.05) 48%, transparent 70%);
  filter: blur(2px);
}

.right-glow-top {
  top: -110px;
  right: -80px;
}

.right-glow-bottom {
  bottom: -110px;
  left: -90px;
}

.form-card {
  position: relative;
  z-index: 1;
  width: min(100%, 446px);
  padding: 42px 42px 34px;
  border-radius: 24px;
  text-align: center;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(225, 234, 249, 0.92);
  box-shadow: 0 18px 55px rgba(126, 155, 205, 0.18);
}

.form-icon {
  width: 120px;
  height: 120px;
  margin: 0 auto 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.form-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.form-title {
  margin: 0 0 10px;
  font-size: 48px;
  line-height: 1.15;
  font-weight: 700;
  color: #2d4f85;
}

.form-desc {
  margin: 0 0 30px;
  font-size: 18px;
  color: #9aa6bb;
}

.input-group {
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 8px;
}

.input-box {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  padding: 0 16px;
  border-radius: 12px;
  border: 1px solid #dfe7f4;
  background: #fff;
  color: #9eacc2;
  transition: border-color .2s, box-shadow .2s, color .2s;
}
.input-box:focus-within {
  color: #5a8ff0;
  border-color: #94b8ff;
  box-shadow: 0 0 0 4px rgba(112, 156, 255, 0.12);
}

.input-box input {
  flex: 1;
  height: 54px;
  border: none;
  background: transparent;
  font-size: 17px;
  color: #44556e;
  outline: none;
}

.input-box input::placeholder {
  color: #aeb8ca;
}

.input-ico {
  flex-shrink: 0;
}

.field-action {
  width: 24px;
  height: 24px;
  border: none;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #9eacc2;
  background: transparent;
  cursor: pointer;
}

.captcha-row {
  display: flex;
  gap: 14px;
  align-items: stretch;
}

.captcha-box {
  flex: 1;
  min-width: 0;
}
.captcha-img {
  width: 118px;
  flex-shrink: 0;
  border-radius: 12px;
  border: 1px solid #d7e4ff;
  background: linear-gradient(180deg, #fbfdff 0%, #eff5ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  font-family: 'Courier New', monospace;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 6px;
  color: #356fe5;
}

.captcha-img:hover {
  border-color: #94b8ff;
}

.error-msg {
  margin: 14px 0 0;
  font-size: 14px;
  color: #de4f56;
  text-align: center;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 22px 0 24px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #9aa6bb;
  cursor: pointer;
}

.remember-me input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #4f89ef;
  cursor: pointer;
}

.forgot-link {
  font-size: 15px;
  color: #5a8ff0;
  cursor: pointer;
}

.forgot-link:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  height: 58px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(90deg, #2f79ee 0%, #478fff 100%);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 14px;
  cursor: pointer;
  transition: transform .2s, box-shadow .2s, opacity .2s;
  box-shadow: 0 16px 30px rgba(74, 132, 240, 0.24);
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 34px rgba(74, 132, 240, 0.3);
}

.submit-btn:disabled {
  opacity: .72;
  cursor: not-allowed;
  transform: none;
}

.form-footer {
  position: relative;
  z-index: 1;
  margin: 24px 0 0;
  font-size: 14px;
  line-height: 1.8;
  color: #b1bccf;
  text-align: center;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(32, 50, 88, 0.28);
  backdrop-filter: blur(6px);
}

.modal-card {
  width: min(92vw, 430px);
  padding: 34px 34px 28px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid rgba(228, 236, 250, 0.95);
  box-shadow: 0 24px 60px rgba(78, 107, 158, 0.22);
  text-align: center;
}

.modal-title {
  margin: 0 0 8px;
  font-size: 28px;
  color: #2d4f85;
}

.modal-desc {
  margin: 0 0 22px;
  font-size: 15px;
  color: #9aa6bb;
}

.modal-close {
  margin-top: 18px;
  font-size: 15px;
  color: #5a8ff0;
  cursor: pointer;
}

.modal-close:hover {
  text-decoration: underline;
}

@media (max-width: 1180px) {
  .login-left {
    flex: 1;
  }

  .login-right {
    width: 46vw;
    min-width: 400px;
    padding: 40px 32px;
  }

  .form-title {
    font-size: 40px;
  }
}

@media (max-width: 760px) {
  .login-container {
    flex-direction: column;
  }

  .login-left {
    min-height: 34vh;
  }

  .login-right {
    width: 100%;
    min-width: 0;
    padding: 28px 20px 32px;
  }

  .form-card {
    width: min(100%, 520px);
  }

  .form-title {
    font-size: 36px;
  }

  .form-desc,
  .form-footer {
    font-size: 16px;
  }
}

@media (max-width: 640px) {
  .form-card {
    padding: 30px 22px 24px;
    border-radius: 20px;
  }

  .form-icon {
    width: 64px;
    height: 64px;
  }

  .form-title {
    font-size: 30px;
  }

  .form-desc {
    font-size: 15px;
  }

  .input-box input {
    font-size: 15px;
  }

  .captcha-row {
    gap: 10px;
  }

  .captcha-img {
    width: 104px;
    font-size: 20px;
    letter-spacing: 4px;
  }

  .submit-btn {
    height: 54px;
    font-size: 19px;
    letter-spacing: 10px;
  }

  .form-footer {
    margin-top: 28px;
    font-size: 13px;
  }
}
</style>
