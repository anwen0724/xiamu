import type { BaseResponse, UserVO } from './types'
import { apiClient } from './client'

type LoginPayload = {
  userAccount: string
  userPassword: string
}

type RegisterPayload = {
  userAccount: string
  userPassword: string
  checkPassword: string
  userName: string
}

function unwrapResponse<T>(response: BaseResponse<T>): T {
  if (response.code !== 0) {
    throw new Error(response.message || '请求失败')
  }

  return response.data
}

export async function loginUser(payload: LoginPayload) {
  const { data } = await apiClient.post<BaseResponse<UserVO>>('/user/login', payload)
  return unwrapResponse(data)
}

export async function registerUser(payload: RegisterPayload) {
  const { data } = await apiClient.post<BaseResponse<number>>('/user/register', payload)
  return unwrapResponse(data)
}

export async function logoutUser() {
  const { data } = await apiClient.post<BaseResponse<boolean>>('/user/logout')
  return unwrapResponse(data)
}

export async function getLoginUser() {
  const { data } = await apiClient.get<BaseResponse<UserVO>>('/user/get/login')
  return unwrapResponse(data)
}
