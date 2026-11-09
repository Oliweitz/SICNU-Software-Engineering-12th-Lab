<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const roleLabels: Record<string, string> = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '师范生',
}

const permissionLabels: Record<string, string> = {
  'user:read': '查看用户',
  'user:manage': '管理用户',
  'clazz:read': '查看班级',
  'clazz:manage': '管理班级',
  'task:read': '查看任务',
  'task:manage': '发布任务',
  'trial:submit': '提交试讲',
  'trial:read': '查看试讲',
  'report:read': '查看报告',
  'review:manage': '批阅点评',
  'resource:read': '查看资源',
  'resource:manage': '维护资源',
  'dict:read': '查看字典',
  'dict:manage': '维护字典',
  'stats:self': '个人统计',
  'stats:class': '班级统计',
  'stats:global': '全局统计',
  'cert:read': '查看证书',
  'permission:manage': '权限管理',
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const roleTags = computed(() =>
  (auth.user?.roles ?? []).map((role) => roleLabels[role] ?? role),
)

const permissionTags = computed(() =>
  (auth.user?.permissions ?? []).map((code) => ({
    code,
    label: permissionLabels[code] ?? code,
  })),
)

async function refreshPermissions() {
  await auth.refreshMe()
  ElMessage.success('权限已刷新，授权调整即时生效')
}

function logout() {
  auth.logout()
  router.replace('/login')
}

onMounted(async () => {
  try {
    await auth.refreshMe()
  } catch {
    // 401 由 axios 拦截器统一跳转登录
  }
})
</script>

<template>
  <div class="home-page">
    <header class="app-header">
      <div class="brand-mini">
        <div class="logo">AI</div>
        <div class="brand-copy">
          <strong>师范专业 AI 试讲台</strong>
          <span>虚拟课堂 · 智能评测 · 达标认证</span>
        </div>
      </div>

      <div class="user-menu">
        <el-tag v-for="role in roleTags" :key="role" effect="plain" round>
          {{ role }}
        </el-tag>
        <span class="user-name">{{ auth.user?.realName }}</span>
        <el-button text type="primary" @click="logout">退出登录</el-button>
      </div>
    </header>

    <main class="home-main">
      <section class="hero">
        <div>
          <p class="eyebrow">工作台</p>
          <h1>{{ greeting }}，{{ auth.user?.realName }}</h1>
          <p class="hero-desc">
            这是项目的登录后首页。身份认证来自 JWT，菜单与按钮权限来自 RBAC
            权限码，可在这里验证登录态与授权结果。
          </p>
        </div>
        <div class="hero-badge">
          <span class="badge-label">当前身份</span>
          <span class="badge-value">{{ roleTags.join(' / ') || '未分配角色' }}</span>
        </div>
      </section>

      <div class="content-grid">
        <section class="panel account-panel">
          <div class="panel-head">
            <h2>账号信息</h2>
            <span class="panel-sub">sys_user</span>
          </div>
          <dl class="info-list">
            <div>
              <dt>登录名</dt>
              <dd>{{ auth.user?.username }}</dd>
            </div>
            <div>
              <dt>真实姓名</dt>
              <dd>{{ auth.user?.realName }}</dd>
            </div>
            <div>
              <dt>手机号</dt>
              <dd>{{ auth.user?.phoneMasked || '未绑定' }}</dd>
            </div>
            <div>
              <dt>邮箱</dt>
              <dd>{{ auth.user?.emailMasked || '未绑定' }}</dd>
            </div>
            <div>
              <dt>用户 ID</dt>
              <dd>#{{ auth.user?.userId }}</dd>
            </div>
            <div>
              <dt>会话凭证</dt>
              <dd>JWT · {{ auth.user?.expiresIn ? `${auth.user.expiresIn / 3600} 小时` : '—' }}</dd>
            </div>
          </dl>
        </section>

        <section class="panel permission-panel">
          <div class="panel-head">
            <h2>我的权限</h2>
            <el-button size="small" :loading="false" @click="refreshPermissions">
              刷新权限
            </el-button>
          </div>
          <p class="panel-sub permission-sub">
            共 {{ permissionTags.length }} 个权限点，数据源为
            <code>GET /api/v1/rbac/me</code>
          </p>
          <div class="permission-tags">
            <el-tag
              v-for="item in permissionTags"
              :key="item.code"
              :title="item.code"
              effect="plain"
              class="permission-tag"
            >
              {{ item.label }}
            </el-tag>
            <el-empty v-if="permissionTags.length === 0" description="暂无权限点" :image-size="60" />
          </div>
        </section>
      </div>

      <section class="panel auth-panel">
        <div class="panel-head">
          <h2>本次迭代的认证能力</h2>
          <span class="panel-sub">认证与授权已就绪</span>
        </div>
        <div class="ability-grid">
          <div class="ability">
            <span class="ability-index">01</span>
            <strong>账号密码登录</strong>
            <p>BCrypt 慢哈希 + 图形验证码 + 失败锁定</p>
          </div>
          <div class="ability">
            <span class="ability-index">02</span>
            <strong>手机号 / 邮箱验证码</strong>
            <p>短信与邮件通道可插拔，开发环境模拟发送</p>
          </div>
          <div class="ability">
            <span class="ability-index">03</span>
            <strong>统一身份认证</strong>
            <p>模拟 CAS 单点登录，按真实 CAS 协议可切换</p>
          </div>
          <div class="ability">
            <span class="ability-index">04</span>
            <strong>RBAC 授权</strong>
            <p>角色 → 权限码 → 接口与前端按钮双重校验</p>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 8% 0%, rgba(47, 165, 165, 0.12), transparent 34%),
    #f4f7fa;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  padding: 0 42px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e8eef4;
  backdrop-filter: blur(10px);
}

.brand-mini {
  display: flex;
  gap: 12px;
  align-items: center;
}

.brand-mini .logo {
  display: grid;
  width: 38px;
  height: 38px;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #14506f, #2fa5a5);
  border-radius: 10px;
  place-items: center;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 15px;
  color: #17324d;
}

.brand-copy span {
  font-size: 11px;
  color: #9aa7b4;
  letter-spacing: 0.5px;
}

.user-menu {
  display: flex;
  gap: 10px;
  align-items: center;
}

.user-name {
  margin-left: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #33475b;
}

.home-main {
  width: 100%;
  max-width: 1120px;
  padding: 34px 24px 60px;
  margin: 0 auto;
}

.hero {
  display: flex;
  gap: 30px;
  align-items: center;
  justify-content: space-between;
  padding: 34px 38px;
  margin-bottom: 22px;
  color: #eaf6ff;
  background:
    radial-gradient(circle at 92% 20%, rgba(47, 165, 165, 0.32), transparent 40%),
    linear-gradient(140deg, #0d2b43, #14506f 58%, #1b6b93);
  border-radius: 18px;
  box-shadow: 0 16px 36px rgba(20, 55, 80, 0.16);
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 3px;
  color: rgba(234, 246, 255, 0.55);
}

.hero h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
}

.hero-desc {
  max-width: 580px;
  margin: 12px 0 0;
  font-size: 13px;
  line-height: 1.8;
  color: rgba(234, 246, 255, 0.72);
}

.hero-badge {
  display: flex;
  flex: none;
  flex-direction: column;
  gap: 6px;
  padding: 16px 22px;
  text-align: right;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 14px;
}

.badge-label {
  font-size: 11px;
  letter-spacing: 1px;
  color: rgba(234, 246, 255, 0.55);
}

.badge-value {
  font-size: 16px;
  font-weight: 600;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1.15fr;
  gap: 22px;
  margin-bottom: 22px;
}

.panel {
  padding: 26px 28px;
  background: #fff;
  border: 1px solid #e8eef4;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(20, 55, 80, 0.04);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #17324d;
}

.panel-sub {
  font-size: 12px;
  color: #a1adba;
}

.info-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 24px;
  margin: 22px 0 0;
}

.info-list dt {
  margin-bottom: 5px;
  font-size: 12px;
  color: #9aa7b4;
}

.info-list dd {
  margin: 0;
  font-size: 14px;
  color: #33475b;
}

.permission-sub {
  margin: 10px 0 0;
}

.permission-sub code {
  padding: 1px 5px;
  font-size: 11px;
  color: var(--brand-500);
  background: #eef6fa;
  border-radius: 4px;
}

.permission-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.permission-tag {
  margin: 0;
}

.ability-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-top: 22px;
}

.ability {
  padding: 18px;
  background: #f8fbfd;
  border: 1px solid #edf2f6;
  border-radius: 12px;
}

.ability-index {
  display: block;
  margin-bottom: 12px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--accent);
}

.ability strong {
  display: block;
  font-size: 14px;
  color: #17324d;
}

.ability p {
  margin: 7px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #8a99a8;
}

@media (max-width: 900px) {
  .app-header {
    padding: 0 20px;
  }

  .brand-copy span {
    display: none;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 28px 24px;
  }

  .hero-badge {
    text-align: left;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .ability-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 620px) {
  .user-menu .el-tag {
    display: none;
  }

  .info-list {
    grid-template-columns: 1fr;
  }

  .ability-grid {
    grid-template-columns: 1fr;
  }

  .hero h1 {
    font-size: 23px;
  }
}
</style>
