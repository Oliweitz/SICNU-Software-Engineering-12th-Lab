import { ref } from 'vue'
import { getCaptcha } from '../api/auth'

/** 图形验证码状态：每次使用后调用 refresh() 换新 */
export function useCaptcha() {
  const captchaId = ref('')
  const captchaCode = ref('')
  const image = ref('')
  const debugCode = ref<string | null>(null)
  const loading = ref(false)

  async function refresh() {
    loading.value = true
    try {
      const data = await getCaptcha()
      captchaId.value = data.captchaId
      captchaCode.value = ''
      image.value = data.image
      debugCode.value = data.debugCode ?? null
    } finally {
      loading.value = false
    }
  }

  return { captchaId, captchaCode, image, debugCode, loading, refresh }
}
