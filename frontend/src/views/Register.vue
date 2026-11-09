<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AuthShell from '../components/AuthShell.vue'
import CaptchaField from '../components/CaptchaField.vue'
import { useCaptcha } from '../composables/useCaptcha'
import { register, sendCode } from '../api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const smsSending = ref(false)
const emailSending = ref(false)
const smsCountdown = ref(0)
const emailCountdown = ref(0)
let smsTimer: number | undefined
let emailTimer: number | undefined

const {
  captchaId,
  captchaCode,
  image,
  debugCode,
  loading: captchaLoading,
  refresh: refreshCaptcha,
} = useCaptcha()

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  smsCode: '',
  email: '',
  emailCode: '',
  major: '',
  grade: '',
  password: '',
  confirmPassword: '',
})

const majors = [
  { value: 'CHINESE_EDU', label: '汉语言文学' },
  { value: 'MATH_EDU', label: '数学与应用数学' },
  { value: 'ENGLISH_EDU', label: '英语' },
  { value: 'POLITICS_EDU', label: '思想政治教育' },
  { value: 'PRESCHOOL_EDU', label: '学前教育' },
  { value: 'PRIMARY_EDU', label: '小学教育' },
]

const grades = ['2022', '2023', '2024', '2025', '2026']

const rules: FormRules = {
  username: [
    { required: true, message: '请输入学号 / 工号', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{4,50}$/,
      message: '只能包含字母、数字、下划线，长度 4-50',
      trigger: 'blur',
    },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  smsCode: [{ required: true, message: '请输入短信验证码', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度须为 6-32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function startCountdown(kind: 'sms' | 'email', seconds: number) {
  if (kind === 'sms') {
    smsCountdown.value = seconds
    window.clearInterval(smsTimer)
    smsTimer = window.setInterval(() => {
      smsCountdown.value -= 1
      if (smsCountdown.value <= 0) {
        window.clearInterval(smsTimer)
      }
    }, 1000)
  } else {
    emailCountdown.value = seconds
    window.clearInterval(emailTimer)
    emailTimer = window.setInterval(() => {
      emailCountdown.value -= 1
      if (emailCountdown.value <= 0) {
        window.clearInterval(emailTimer)
      }
    }, 1000)
  }
}

async function sendSmsCode() {
  if (!form.phone.trim()) {
    ElMessage.warning('请先输入手机号')
    return
  }
  if (!captchaCode.value.trim()) {
    ElMessage.warning('请先输入图形验证码')
    return
  }
  smsSending.value = true
  try {
    const res = await sendCode({
      target: form.phone.trim(),
      scene: 'REGISTER',
      captchaId: captchaId.value,
      captchaCode: captchaCode.value.trim(),
    })
    startCountdown('sms', res.resendSeconds)
    ElMessage.success(
      res.debugCode ? `短信验证码已发送（开发模式：${res.debugCode}）` : res.message,
    )
    await refreshCaptcha()
  } catch {
    await refreshCaptcha()
  } finally {
    smsSending.value = false
  }
}

async function sendEmailCode() {
  if (!form.email.trim()) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (!captchaCode.value.trim()) {
    ElMessage.warning('请先输入图形验证码')
    return
  }
  emailSending.value = true
  try {
    const res = await sendCode({
      target: form.email.trim(),
      scene: 'REGISTER',
      captchaId: captchaId.value,
      captchaCode: captchaCode.value.trim(),
    })
    startCountdown('email', res.resendSeconds)
    ElMessage.success(
      res.debugCode ? `邮箱验证码已发送（开发模式：${res.debugCode}）` : res.message,
    )
    await refreshCaptcha()
  } catch {
    await refreshCaptcha()
  } finally {
    emailSending.value = false
  }
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!captchaCode.value.trim()) {
    ElMessage.warning('请输入图形验证码')
    return
  }
  if (form.email.trim() && !form.emailCode.trim()) {
    ElMessage.warning('填写邮箱后需要完成邮箱验证')
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username.trim(),
      realName: form.realName.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      phone: form.phone.trim(),
      smsCode: form.smsCode.trim(),
      email: form.email.trim() || undefined,
      emailCode: form.emailCode.trim() || undefined,
      major: form.major || undefined,
      grade: form.grade || undefined,
      captchaId: captchaId.value,
      captchaCode: captchaCode.value.trim(),
    })
    ElMessage.success('注册成功，请使用新账号登录')
    await router.replace('/login')
  } catch {
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})

onBeforeUnmount(() => {
  window.clearInterval(smsTimer)
  window.clearInterval(emailTimer)
})
</script>

<template>
  <AuthShell
    title="创建账号"
    subtitle="注册后即可加入班级、领取试讲任务并查看 AI 评测报告"
    wide
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      hide-required-asterisk
    >
      <div class="grid-2">
        <el-form-item label="学号 / 工号" prop="username">
          <el-input v-model="form.username" placeholder="4-50 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="用于证书与评测报告" />
        </el-form-item>
      </div>

      <div class="grid-2">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="用于短信登录与找回密码" maxlength="11" />
        </el-form-item>
        <el-form-item label="短信验证码" prop="smsCode">
          <el-input v-model="form.smsCode" placeholder="6 位验证码" maxlength="6">
            <template #append>
              <el-button
                :disabled="smsCountdown > 0 || smsSending"
                :loading="smsSending"
                @click="sendSmsCode"
              >
                {{ smsCountdown > 0 ? `${smsCountdown}s` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </div>

      <div class="grid-2">
        <el-form-item label="邮箱（选填）" prop="email">
          <el-input v-model="form.email" placeholder="填写后可使用邮箱验证码登录" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <el-input
            v-model="form.emailCode"
            placeholder="填写邮箱后必填"
            maxlength="6"
            :disabled="!form.email"
          >
            <template #append>
              <el-button
                :disabled="!form.email || emailCountdown > 0 || emailSending"
                :loading="emailSending"
                @click="sendEmailCode"
              >
                {{ emailCountdown > 0 ? `${emailCountdown}s` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </div>

      <div class="grid-2">
        <el-form-item label="专业（选填）">
          <el-select v-model="form.major" placeholder="请选择专业" clearable>
            <el-option
              v-for="item in majors"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级（选填）">
          <el-select v-model="form.grade" placeholder="请选择入学年份" clearable>
            <el-option v-for="item in grades" :key="item" :label="`${item} 级`" :value="item" />
          </el-select>
        </el-form-item>
      </div>

      <div class="grid-2">
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6-32 位"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入密码"
            autocomplete="new-password"
          />
        </el-form-item>
      </div>

      <el-form-item label="图形验证码">
        <CaptchaField
          v-model="captchaCode"
          :image="image"
          :debug-code="debugCode"
          :loading="captchaLoading"
          @refresh="refreshCaptcha"
          @submit="submit"
        />
      </el-form-item>

      <div class="register-tip">
        每次发送短信 / 邮箱验证码都会消耗一张图形验证码，发送后请填写新图片中的字符。
      </div>

      <el-button type="primary" class="auth-submit" :loading="loading" @click="submit">
        注 册
      </el-button>
    </el-form>

    <div class="auth-links">
      <span>已有账号？<RouterLink to="/login">返回登录</RouterLink></span>
      <RouterLink to="/forgot-password">忘记密码</RouterLink>
    </div>
  </AuthShell>
</template>

<style scoped>
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

:deep(.el-form-item__label) {
  padding-bottom: 4px;
  font-size: 13px;
  color: #5c6b7a;
}

:deep(.el-select) {
  width: 100%;
}

.register-tip {
  margin: -4px 0 14px;
  font-size: 12px;
  line-height: 1.7;
  color: #9aa7b4;
}

@media (max-width: 620px) {
  .grid-2 {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
