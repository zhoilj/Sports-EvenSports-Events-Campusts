import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo, register } from '@/api/auth'
import type { LoginDTO, RegisterDTO, LoginVO } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<LoginVO | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isTeacher = computed(() => userInfo.value?.role === 'TEACHER')
  const isStudent = computed(() => userInfo.value?.role === 'STUDENT')

  // 登录
  async function doLogin(loginDTO: LoginDTO) {
    const res = await login(loginDTO)
    if (res.code === 200 && res.data) {
      token.value = res.data.token
      userInfo.value = res.data
      localStorage.setItem('token', res.data.token)
      return res.data
    }
    throw new Error(res.message || '登录失败')
  }

  // 注册
  async function doRegister(registerDTO: RegisterDTO) {
    const res = await register(registerDTO)
    if (res.code === 200) {
      return res.data
    }
    throw new Error(res.message || '注册失败')
  }

  // 获取用户信息
  async function fetchUserInfo() {
    if (!token.value) return null
    const res = await getUserInfo()
    if (res.code === 200 && res.data) {
      userInfo.value = res.data
      return res.data
    }
    return null
  }

  // 设置用户信息
  function setUserInfo(info: LoginVO | null) {
    userInfo.value = info
  }

  // 登出
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    isTeacher,
    isStudent,
    doLogin,
    doRegister,
    fetchUserInfo,
    setUserInfo,
    logout
  }
})