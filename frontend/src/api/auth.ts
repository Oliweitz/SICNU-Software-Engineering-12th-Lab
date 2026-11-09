import http, { type ApiResult } from './http'

export interface CaptchaVO {
  captchaId: string
  image: string
  expireSeconds: number
  debugCode?: string | null
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  realName: string
  phoneMasked?: string | null
  emailMasked?: string | null
  roles: string[]
  permissions: string[]
}

export interface UserAuthorityVO {
  userId: number
  username: string
  realName: string
  roles: string[]
  permissions: string[]
}

export interface SendCodeResponse {
  message: string
  resendSeconds: number
  debugCode?: string | null
}

export type CodeScene = 'LOGIN' | 'REGISTER' | 'RESET_PASSWORD'

export interface SendCodePayload {
  target: string
  scene: CodeScene
  captchaId: string
  captchaCode: string
}

export interface RegisterPayload {
  username: string
  realName: string
  password: string
  confirmPassword: string
  phone: string
  smsCode: string
  email?: string
  emailCode?: string
  major?: string
  grade?: string
  captchaId: string
  captchaCode: string
}

export interface ResetPasswordPayload {
  target: string
  code: string
  newPassword: string
  confirmPassword: string
}

async function post<T>(url: string, payload?: unknown): Promise<T> {
  const response = await http.post<ApiResult<T>>(url, payload)
  return response.data.data
}

export async function getCaptcha(): Promise<CaptchaVO> {
  const response = await http.get<ApiResult<CaptchaVO>>('/auth/captcha')
  return response.data.data
}

export function login(payload: {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}): Promise<LoginResponse> {
  return post<LoginResponse>('/auth/login', payload)
}

export function loginByCode(payload: { target: string; code: string }): Promise<LoginResponse> {
  return post<LoginResponse>('/auth/login/code', payload)
}

export function sendCode(payload: SendCodePayload): Promise<SendCodeResponse> {
  return post<SendCodeResponse>('/auth/code/send', payload)
}

export async function register(payload: RegisterPayload): Promise<void> {
  await post<null>('/auth/register', payload)
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  await post<null>('/auth/password/reset', payload)
}

export function exchangeCasTicket(ticket: string): Promise<LoginResponse> {
  return post<LoginResponse>('/auth/cas/exchange', { ticket })
}

export async function getMe(): Promise<UserAuthorityVO> {
  const response = await http.get<ApiResult<UserAuthorityVO>>('/rbac/me')
  return response.data.data
}
