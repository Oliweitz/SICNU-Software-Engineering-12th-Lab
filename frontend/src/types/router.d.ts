import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** 是否需要登录 */
    requiresAuth?: boolean
    /** 浏览器标题 */
    title?: string
  }
}
