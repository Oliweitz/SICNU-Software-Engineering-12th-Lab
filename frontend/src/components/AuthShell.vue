<script setup lang="ts">
defineProps<{
  title: string
  subtitle: string
  wide?: boolean
}>()
</script>

<template>
  <div class="auth-page">
    <aside class="brand-panel">
      <div class="grid-overlay"></div>
      <div class="orb orb-one"></div>
      <div class="orb orb-two"></div>

      <div class="brand">
        <div class="logo">AI</div>
        <div class="brand-text">
          <h1>师范专业 AI 试讲台</h1>
          <p>虚拟课堂 · 智能评测 · 达标认证</p>
        </div>
      </div>

      <ul class="feature-list">
        <li>
          <span class="dot"></span>
          <div>
            <strong>虚拟学生陪练</strong>
            <p>随时开讲，反复磨课，不再等真实课堂</p>
          </div>
        </li>
        <li>
          <span class="dot"></span>
          <div>
            <strong>多维 AI 评测</strong>
            <p>表达、互动、控场、时间管理逐项打分</p>
          </div>
        </li>
        <li>
          <span class="dot"></span>
          <div>
            <strong>成长轨迹留痕</strong>
            <p>每次试讲都有报告，进步看得见</p>
          </div>
        </li>
      </ul>

      <div class="brand-foot">
        <span>软件工程课程设计 · 2024 级</span>
        <span class="version">v0.1 · 迭代 1</span>
      </div>
    </aside>

    <main class="form-panel">
      <div class="form-card" :class="{ wide }">
        <h2>{{ title }}</h2>
        <p class="subtitle">{{ subtitle }}</p>
        <slot />
      </div>
      <p class="copyright">© 2026 师范专业 AI 试讲台系统</p>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
  background: #f5f8fb;
}

.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 46%;
  min-height: 100vh;
  padding: 54px 58px;
  overflow: hidden;
  color: #eaf6ff;
  background:
    radial-gradient(circle at 18% 12%, rgba(47, 165, 165, 0.32), transparent 42%),
    radial-gradient(circle at 88% 86%, rgba(47, 143, 181, 0.28), transparent 46%),
    linear-gradient(155deg, #0d2b43 0%, #14506f 52%, #1b6b93 100%);
}

.grid-overlay {
  position: absolute;
  inset: 0;
  opacity: 0.16;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.22) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.22) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: linear-gradient(to bottom, black, transparent 78%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(6px);
  pointer-events: none;
}

.orb-one {
  right: -90px;
  top: 16%;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(47, 165, 165, 0.38), transparent 68%);
}

.orb-two {
  left: 12%;
  bottom: -80px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(88, 176, 211, 0.26), transparent 70%);
}

.brand {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 16px;
  align-items: center;
}

.logo {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #0d2b43;
  background: linear-gradient(135deg, #d7f2ef, #9fded9);
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
}

.brand-text h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.brand-text p {
  margin: 6px 0 0;
  font-size: 13px;
  color: rgba(234, 246, 255, 0.72);
  letter-spacing: 1px;
}

.feature-list {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 26px;
  padding: 0;
  margin: 70px 0 0;
  list-style: none;
}

.feature-list li {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.feature-list .dot {
  flex: none;
  width: 9px;
  height: 9px;
  margin-top: 6px;
  background: var(--accent);
  border: 2px solid rgba(255, 255, 255, 0.75);
  border-radius: 50%;
  box-shadow: 0 0 0 5px rgba(47, 165, 165, 0.14);
}

.feature-list strong {
  display: block;
  font-size: 15px;
  font-weight: 600;
}

.feature-list p {
  margin: 5px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(234, 246, 255, 0.68);
}

.brand-foot {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  margin-top: auto;
  font-size: 12px;
  color: rgba(234, 246, 255, 0.5);
}

.version {
  letter-spacing: 0.5px;
}

.form-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 28px 24px;
}

.form-card {
  width: 100%;
  max-width: 440px;
  padding: 38px 40px 34px;
  background: #fff;
  border: 1px solid #eef2f6;
  border-radius: 18px;
  box-shadow: 0 18px 46px rgba(20, 55, 80, 0.08);
}

.form-card.wide {
  max-width: 620px;
}

.form-card h2 {
  margin: 0;
  font-size: 25px;
  font-weight: 600;
  color: #17324d;
}

.subtitle {
  margin: 9px 0 26px;
  font-size: 13px;
  color: #8a99a8;
}

.copyright {
  margin: 22px 0 0;
  font-size: 12px;
  color: #a9b5c1;
}

@media (max-width: 980px) {
  .auth-page {
    flex-direction: column;
  }

  .brand-panel {
    width: 100%;
    min-height: auto;
    padding: 30px 26px 26px;
  }

  .feature-list {
    display: none;
  }

  .brand-foot {
    margin-top: 22px;
  }

  .form-panel {
    padding: 30px 18px;
  }

  .form-card {
    margin-top: -38px;
    padding: 30px 24px;
  }
}

@media (max-width: 560px) {
  .brand-text h1 {
    font-size: 18px;
  }

  .form-card {
    padding: 26px 18px;
  }
}
</style>
