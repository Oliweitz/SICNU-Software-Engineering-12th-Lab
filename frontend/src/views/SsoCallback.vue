<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { exchangeCasTicket } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const status = ref<'loading' | 'success' | 'error'>('loading')
const message = ref('正在完成统一身份认证…')

onMounted(async () => {
  const error = route.query.error
  const ticket = route.query.ticket

  if (typeof error === 'string' && error) {
    status.value = 'error'
    message.value = error
    return
  }
  if (typeof ticket !== 'string' || !ticket) {
    status.value = 'error'
    message.value = '缺少认证票据，请重新发起登录'
    return
  }

  try {
    const session = await exchangeCasTicket(ticket)
    auth.setSession(session)
    status.value = 'success'
    message.value = `认证成功，欢迎 ${session.realName}`
    window.setTimeout(() => router.replace('/'), 600)
  } catch {
    status.value = 'error'
    message.value = '统一身份认证失败，请重新登录'
  }
})
</script>

<template>
  <div class="callback-page">
    <div class="callback-card">
      <div v-if="status === 'loading'" class="icon loading-icon">
        <span class="spinner"></span>
      </div>
      <div v-else-if="status === 'success'" class="icon success-icon">✓</div>
      <div v-else class="icon error-icon">!</div>

      <h1>统一身份认证</h1>
      <p class="message">{{ message }}</p>

      <RouterLink v-if="status === 'error'" to="/login">
        <el-button type="primary" class="back-button">返回登录</el-button>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.callback-page {
  display: grid;
  min-height: 100vh;
  padding: 24px;
  color: #eaf6ff;
  background:
    radial-gradient(circle at 20% 10%, rgba(47, 165, 165, 0.32), transparent 42%),
    linear-gradient(155deg, #0d2b43 0%, #14506f 52%, #1b6b93 100%);
  place-items: center;
}

.callback-card {
  width: 100%;
  max-width: 420px;
  padding: 42px 36px;
  text-align: center;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 18px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
}

.icon {
  display: grid;
  width: 58px;
  height: 58px;
  margin: 0 auto 20px;
  font-size: 24px;
  font-weight: 700;
  border-radius: 50%;
  place-items: center;
}

.loading-icon {
  background: #eaf1f5;
}

.success-icon {
  color: #fff;
  background: linear-gradient(135deg, #2fa5a5, #1b6b93);
}

.error-icon {
  color: #fff;
  background: #e25656;
}

.spinner {
  width: 26px;
  height: 26px;
  border: 3px solid #cfe0ea;
  border-top-color: var(--brand-500);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

h1 {
  margin: 0;
  font-size: 20px;
  color: #17324d;
}

.message {
  margin: 12px 0 24px;
  font-size: 14px;
  line-height: 1.7;
  color: #7b8a9a;
}

.back-button {
  width: 100%;
  height: 42px;
}
</style>
