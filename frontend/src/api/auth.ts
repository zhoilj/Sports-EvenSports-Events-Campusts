import { get, post } from '@/utils/request'
import type { LoginDTO, RegisterDTO, LoginVO, Result } from '@/types/auth'

// 登录
export function login(data: LoginDTO): Promise<Result<LoginVO>> {
  return post<LoginVO>('/auth/login', data)
}

// 注册
export function register(data: RegisterDTO): Promise<Result<number>> {
  return post<number>('/auth/register', data)
}

// 获取当前用户信息
export function getUserInfo(): Promise<Result<LoginVO>> {
  return get<LoginVO>('/auth/info')
}