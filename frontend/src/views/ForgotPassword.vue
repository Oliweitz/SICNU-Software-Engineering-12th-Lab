<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '../components/AuthShell.vue'
import CaptchaField from '../components/CaptchaField.vue'
import { useCaptcha } from '../composables/useCaptcha'
import { resetPassword, sendCode } from '../api/auth'

const router = useRouter()
const step = ref(0)
const sending = ref(false)
const submitting = ref(false)
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

const form = reactive({
  target: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
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

async function requestCode() {
  if (!form.target.trim()) {
    ElMessage.warning('请输入注册时绑定的手机号或邮箱')
    return
  }
  if (!captchaCode.value.trim()) {
    ElMessage.warning('请输入图形验证码')
    return
  }
  sending.value = true
  try {
    const res = await sendCode({
      target: form.target.trim(),
      scene: 'RESET_PASSWORD',
      captchaId: captchaId.value,
      captchaCode: captchaCode.value.trim(),
    })
    startCountdown(res.resendSeconds)
    step.value = 1
    ElMessage.success(
      res.debugCode ? `验证码已发送（开发模式：${res.debugCode}）` : res.message,
    )
    await refreshCaptcha()
  } catch {
    await refreshCaptcha()
  } finally {
    sending.value = false
  }
}

async function submitReset() {
  if (!form.code.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (form.newPassword.length < 6 || form.newPassword.length > 32) {
    ElMessage.warning('新密码长度须为 6-32 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await resetPassword({
      target: form.target.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    await router.replace('/login')
  } finally {
    submitting.value = false
  }
}

function changeTarget() {
  step.value = 0
  form.code = ''
  refreshCaptcha()
}

onMounted(() => {
  refreshCaptcha()
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
})
</script>

<template>
  <AuthShell
    title="找回密码"
    subtitle="通过注册时绑定的手机号或邮箱验证身份，重置登录密码"
  >
    <el-steps :active="step" simple class="reset-steps">
      <el-step title="验证身份" />
      <el-step title="设置新密码" />
    </el-steps>

    <template v-if="step === 0">
      <el-form label-position="top" size="large" hide-required-asterisk @submit.prevent>
        <el-form-item label="手机号 / 邮箱">
          <el-input
            v-model="form.target"
            placeholder="请输入注册时绑定的手机号或邮箱"
            @keyup.enter="requestCode"
          />
        </el-form-item>
        <el-form-item label="图形验证码">
          <CaptchaField
            v-model="captchaCode"
            :image="image"
            :debug-code="debugCode"
            :loading="captchaLoading"
            @refresh="refreshCaptcha"
            @submit="requestCode"
          />
        </el-form-item>
        <el-button
          type="primary"
          class="auth-submit"
          :loading="sending"
          @click="requestCode"
        >
          发送验证码
        </el-button>
      </el-form>
      <p class="reset-hint">
        如果该账号已绑定手机号或邮箱，验证码将发送至对应联系方式；未绑定账号不会收到验证码。
      </p>
    </template>

    <template v-else>
      <el-form label-position="top" size="large" hide-required-asterisk @submit.prevent>
        <el-form-item label="验证码">
          <el-input
            v-model="form.code"
            placeholder="请输入 6 位验证码"
            maxlength="6"
            autocomplete="one-time-code"
          >
            <template #append>
              <el-button :disabled="countdown > 0" @click="changeTarget">
                {{ countdown > 0 ? `${countdown}s 后重发` : '重新获取' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <div class="target-line">
          正在为 <b>{{ form.target }}</b> 重置密码
          <el-button link type="primary" @click="changeTarget">换一个</el-button>
        </div>
        <el-form-item label="新密码">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="6-32 位"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入新密码"
            autocomplete="new-password"
            @keyup.enter="submitReset"
          />
        </el-form-item>
        <el-button
          type="primary"
          class="auth-submit"
          :loading="submitting"
          @click="submitReset"
        >
          重置密码
        </el-button>
      </el-form>
    </template>

    <div class="auth-links">
      <RouterLink to="/login">返回登录</RouterLink>
      <span>还没有账号？<RouterLink to="/register">立即注册</RouterLink></span>
    </div>
  </AuthShell>
</template>

<style scoped>
.reset-steps {
  margin-bottom: 26px;
}

:deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-size: 13px;
  color: #5c6b7a;
}

.reset-hint {
  margin: 16px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #9aa7b4;
}

.target-line {
  display: flex;
  gap: 6px;
  align-items: center;
  margin: -6px 0 16px;
  font-size: 12px;
  color: #7b8a9a;
}

.target-line b {
  color: var(--brand-500);
}
</style>
