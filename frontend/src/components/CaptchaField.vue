<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: string
  image: string
  debugCode?: string | null
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  refresh: []
  submit: []
}>()

const code = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
})
</script>

<template>
  <div class="captcha-field">
    <div class="captcha-row">
      <el-input
        v-model="code"
        placeholder="图形验证码"
        maxlength="6"
        autocomplete="off"
        @keyup.enter="$emit('submit')"
      />
      <button
        type="button"
        class="captcha-image"
        :class="{ loading }"
        title="点击刷新"
        @click="$emit('refresh')"
      >
        <img v-if="image" :src="image" alt="图形验证码" />
        <span v-else>加载中</span>
      </button>
    </div>
    <div v-if="debugCode" class="dev-hint">
      开发模式图形验证码：<b>{{ debugCode }}</b>（生产环境不会返回）
    </div>
  </div>
</template>

<style scoped>
.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-image {
  display: grid;
  flex: none;
  width: 122px;
  height: 42px;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  background: #f6f9fc;
  border: 1px solid #dbe3eb;
  border-radius: 9px;
  place-items: center;
}

.captcha-image:hover {
  border-color: var(--brand-400);
}

.captcha-image.loading {
  opacity: 0.6;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-image span {
  font-size: 12px;
  color: #9aa7b4;
}
</style>
