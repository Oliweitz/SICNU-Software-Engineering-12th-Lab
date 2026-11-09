<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthShell from '../components/AuthShell.vue'
import CaptchaField from '../components/CaptchaField.vue'
import { useCaptcha } from '../composables/useCaptcha'
import { login, loginByCode, sendCode } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const activeTab = ref<'password' | 'code'>('password')
const passwordRef = ref<FormInstance>()
const codeRef = ref<FormInstance>()
const loading = ref(false)
const codeLoading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer: number | undefined

const {
  captchaId,
  captchaCode,
  image,
  debugCode,
  loading: captchaLoading,
  refresh: refreshCaptcha,
} = useCaptcha()

const passwordForm = reactive({ username: '', password: '' })
const codeForm = reactive({ target: '', code: '' })

const passwordRules: FormRules = {
  username: [{ required: true, message: '请输入学号 / 工号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const codeRules: FormRules = {
  target: [{ required: true, message: '请输入手机号或邮箱', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const redirectTarget = computed(() => {
  const target = route.query.redirect
  return typeof target === 'string' && target.startsWith('/') ? target : '/'
})

function startCountdown(seconds: number) {
  countdown.value = seconds
  window.clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
    }
  }, 1000)
}

async function submitPassword() {
  const valid = await passwordRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!captchaCode.value.trim()) {
    ElMessage.warning('请输入图形验证码')
    return
  }
  loading.value = true
  try {
    const session = await login({
      username: passwordForm.username.trim(),
      password: passwordForm.password,
      captchaId: captchaId.value,
      captchaCode: captchaCode.value.trim(),
    })
    auth.setSession(session)
    ElMessage.success(`欢迎回来，${session.realName}`)
    await router.replace(redirectTarget.value)
  } catch {
    // 图形验证码一次性使用，失败后换一张
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

async function submitCode() {
  const valid = await codeRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  codeLoading.value = true
  try {
    const session = await loginByCode({
      target: codeForm.target.trim(),
      code: codeForm.code.trim(),
    })
    auth.setSession(session)
    ElMessage.success(`欢迎回来，${session.realName}`)
    await router.replace(redirectTarget.value)
  } finally {
    codeLoading.value = false
  }
}

async function requestCode() {
  if (!codeForm.target.trim()) {
    ElMessage.warning('请先输入手机号或邮箱')
    return
  }
  if (!captchaCode.value.trim()) {
    ElMessage.warning('请输入图形验证码')
    return
  }
  sending.value = true
  try {
    const res = await sendCode({
      target: codeForm.target.trim(),
      scene: 'LOGIN',
      captchaId: captchaId.value,
      captchaCode: captchaCode.value.trim(),
    })
    startCountdown(res.resendSeconds)
    ElMessage.success(
      res.debugCode
        ? `验证码已发送（开发模式：${res.debugCode}）`
        : res.message || '验证码已发送',
    )
    await refreshCaptcha()
  } catch {
    await refreshCaptcha()
  } finally {
    sending.value = false
  }
}

function ssoLogin() {
  // 经 Vite 代理同源跳转；生产环境由网关转发 /api
  window.location.href = '/api/v1/auth/cas/login'
}

onMounted(() => {
  refreshCaptcha()
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
})
</script>

<template>
  <AuthShell title="欢迎回来" subtitle="登录后继续你的 AI 试讲训练与考核">
    <el-tabs v-model="activeTab" class="login-tabs">
      <el-tab-pane label="账号密码" name="password">
        <el-form
          ref="passwordRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          size="large"
          hide-required-asterisk
          @keyup.enter="submitPassword"
        >
          <el-form-item label="学号 / 工号" prop="username">
            <el-input
              v-model="passwordForm.username"
              placeholder="请输入学号或工号"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="passwordForm.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>
          <el-form-item label="图形验证码">
            <CaptchaField
              v-model="captchaCode"
              :image="image"
              :debug-code="debugCode"
              :loading="captchaLoading"
              @refresh="refreshCaptcha"
              @submit="submitPassword"
            />
          </el-form-item>
          <el-button
            type="primary"
            class="auth-submit"
            :loading="loading"
            @click="submitPassword"
          >
            登 录
          </el-button>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="验证码登录" name="code">
        <el-form
          ref="codeRef"
          :model="codeForm"
          :rules="codeRules"
          label-position="top"
          size="large"
          hide-required-asterisk
        >
          <el-form-item label="手机号 / 邮箱" prop="target">
            <el-input
              v-model="codeForm.target"
              placeholder="请输入注册时绑定的手机号或邮箱"
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="图形验证码">
            <CaptchaField
              v-model="captchaCode"
              :image="image"
              :debug-code="debugCode"
              :loading="captchaLoading"
              @refresh="refreshCaptcha"
            />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <el-input
              v-model="codeForm.code"
              placeholder="6 位验证码"
              maxlength="6"
              autocomplete="one-time-code"
              @keyup.enter="submitCode"
            >
              <template #append>
                <el-button
                  :disabled="countdown > 0 || sending"
                  :loading="sending"
                  @click="requestCode"
                >
                  {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <div class="code-tip">未注册的手机号 / 邮箱不会收到验证码，请先注册。</div>
          <el-button
            type="primary"
            class="auth-submit"
            :loading="codeLoading"
            @click="submitCode"
          >
            登 录
          </el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <div class="sso-divider"><span>或</span></div>
    <el-button class="sso-button" @click="ssoLogin">使用学校统一身份认证登录</el-button>

    <div class="auth-links">
      <RouterLink to="/forgot-password">忘记密码</RouterLink>
      <span>
        还没有账号？
        <RouterLink to="/register">立即注册</RouterLink>
      </span>
    </div>
  </AuthShell>
</template>

<style scoped>
.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 14px;
}

.login-tabs :deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-size: 13px;
  color: #5c6b7a;
}

.code-tip {
  margin: -6px 0 14px;
  font-size: 12px;
  color: #9aa7b4;
}

.sso-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 22px 0 14px;
  font-size: 12px;
  color: #b3bfca;
}

.sso-divider::before,
.sso-divider::after {
  flex: 1;
  height: 1px;
  content: '';
  background: #edf1f5;
}

.sso-button {
  width: 100%;
  height: 42px;
  color: var(--brand-500);
  background: #f4f9fc;
  border-color: #d7e6ef;
}

.sso-button:hover {
  color: var(--brand-400);
  background: #eaf4fa;
  border-color: var(--brand-400);
}
</style>
