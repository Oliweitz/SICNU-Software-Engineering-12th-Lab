import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMe, type LoginResponse, type UserAuthorityVO } from '../api/auth'

const TOKEN_KEY = 'auth_token'
const USER_KEY = 'auth_user'

function readUser(): LoginResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as LoginResponse
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<LoginResponse | null>(readUser())
  const isLoggedIn = computed(() => Boolean(token.value))
  const permissions = computed(() => user.value?.permissions ?? [])
  const roles = computed(() => user.value?.roles ?? [])

  function setSession(session: LoginResponse) {
    token.value = session.token
    user.value = session
    localStorage.setItem(TOKEN_KEY, session.token)
    localStorage.setItem(USER_KEY, JSON.stringify(session))
  }

  /** 从 /rbac/me 拉取最新授权，验证「权限调整即时生效」 */
  async function refreshMe(): Promise<UserAuthorityVO> {
    const me = await getMe()
    const current: LoginResponse = user.value ?? {
      token: token.value,
      tokenType: 'Bearer',
      expiresIn: 0,
      userId: me.userId,
      username: me.username,
      realName: me.realName,
      roles: me.roles,
      permissions: me.permissions,
    }
    user.value = {
      ...current,
      userId: me.userId,
      username: me.username,
      realName: me.realName,
      roles: me.roles,
      permissions: me.permissions,
    }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    return me
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function hasPermission(code: string): boolean {
    return permissions.value.includes(code)
  }

  return {
    token,
    user,
    isLoggedIn,
    permissions,
    roles,
    setSession,
    refreshMe,
    logout,
    hasPermission,
  }
})
