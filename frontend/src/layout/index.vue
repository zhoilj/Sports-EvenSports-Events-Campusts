<template>
  <el-container class="layout-container" :class="{ 'is-collapsed': isCollapse }">
    <el-aside :width="isCollapse ? '76px' : '252px'" class="layout-aside">
      <div class="brand" @click="router.push('/dashboard')">
        <div class="brand-mark"><el-icon><Trophy /></el-icon></div>
        <div v-if="!isCollapse" class="brand-copy">
          <div class="brand-name">相思湖校运会</div>
          <div class="brand-sub">CAMPUS SPORTS · 2026</div>
        </div>
      </div>

      <button v-if="!isCollapse" class="event-switch" @click="router.push('/events')">
        <span class="event-switch-label">当前赛事 · CURRENT MEET</span>
        <strong><i class="event-dot" />{{ activeEventName }}<el-icon><ArrowDown /></el-icon></strong>
        <span class="event-switch-hint">{{ activeEventId ? '点击管理赛事信息' : '请先创建一届运动会' }}</span>
      </button>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="sidebar-menu"
        background-color="transparent"
        text-color="#b7cbc2"
        active-text-color="#18392e"
      >
        <el-menu-item-group
          v-for="group in menuGroups"
          :key="group.title"
          :title="isCollapse ? '' : group.title"
        >
          <el-menu-item
            v-for="item in group.routes"
            :key="item.path"
            :index="menuPath(item.path)"
          >
            <el-icon><component :is="item.meta?.icon" /></el-icon>
            <template #title>{{ item.meta?.title }}</template>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>

      <div v-if="!isCollapse" class="sidebar-bottom">
        <div class="help-card">
          <div class="help-title"><el-icon><Flag /></el-icon> 赛前工作提醒</div>
          <p>先完成报名审核，再核对赛次、场地和时间，最后生成正式秩序册。</p>
          <el-button size="small" plain @click="router.push('/orderbook')">进入编排向导 <el-icon><Right /></el-icon></el-button>
        </div>
        <div class="sidebar-footer">校园体育赛事管理平台 · 管理端</div>
      </div>
    </el-aside>

    <el-container class="main-container">
      <el-header class="layout-header">
        <div class="header-left">
          <el-button class="collapse-btn" text circle @click="toggleCollapse" aria-label="折叠导航">
            <el-icon><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <div class="breadcrumb-wrap">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentRoute?.meta?.title">
                {{ currentRoute.meta.title }}
              </el-breadcrumb-item>
            </el-breadcrumb>
            <div class="header-subtitle">让报名、编排、检录与成绩管理形成完整闭环</div>
          </div>
        </div>
        <div class="header-right">
          <el-input
            v-model="globalSearch"
            class="global-search"
            placeholder="搜索功能，回车进入"
            clearable
            @keyup.enter="navigateSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-tooltip content="返回赛事总览" placement="bottom">
            <el-button circle plain @click="router.push('/dashboard')"><el-icon><House /></el-icon></el-button>
          </el-tooltip>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.realName?.charAt(0) || '用' }}
              </el-avatar>
              <span class="user-copy">
                <strong>{{ userStore.userInfo?.realName || userStore.userInfo?.username || '当前用户' }}</strong>
                <small>{{ roleName }}</small>
              </span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { get } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const globalSearch = ref('')
const eventOptions = ref<any[]>([])
const activeEventId = ref<number | null>(null)

const activeEventName = computed(() => {
  const selected = eventOptions.value.find(item => item.id === activeEventId.value)
  return selected?.name || '暂无赛事'
})
const roleName = computed(() => {
  const role = userStore.userInfo?.role
  return ({ ADMIN: '赛事管理员', TEACHER: '教师 / 裁判', STUDENT: '学生运动员' } as Record<string, string>)[role || ''] || '系统用户'
})
const activeMenu = computed(() => {
  if (route.path.startsWith('/events/')) return '/events'
  if (route.path === '/') return '/dashboard'
  return route.path
})

const menuRoutes = computed(() => {
  const routes = router.options.routes.find(item => item.path === '/')?.children || []
  const userRole = userStore.userInfo?.role
  return routes.filter(item => {
    if (item.meta?.hidden) return false
    const roles = item.meta?.roles as string[] | undefined
    return !roles?.length || (!!userRole && roles.includes(userRole))
  })
})

const menuGroups = computed(() => {
  const groups = new Map<string, typeof menuRoutes.value>()
  menuRoutes.value.forEach(item => {
    const title = String(item.meta?.group || '其他')
    if (!groups.has(title)) groups.set(title, [])
    groups.get(title)!.push(item)
  })
  return Array.from(groups.entries()).map(([title, routes]) => ({ title, routes }))
})

onMounted(loadEvents)

async function loadEvents() {
  try {
    const res = await get('/events', { current: 1, size: 100 })
    if (res.code === 200) {
      eventOptions.value = res.data?.records || []
      const active = eventOptions.value.find(item =>
        item.status === 1 || item.status === 'PUBLISHED' || item.statusName === '进行中'
      ) || eventOptions.value[0]
      activeEventId.value = active?.id ?? null
    }
  } catch (error) {
    console.warn('读取当前赛事失败', error)
  }
}

function menuPath(path: string) {
  return path.startsWith('/') ? path : '/' + path
}
function toggleCollapse() {
  isCollapse.value = !isCollapse.value
}
function navigateSearch() {
  const keyword = globalSearch.value.trim().toLowerCase()
  if (!keyword) return
  const target = menuRoutes.value.find(item => String(item.meta?.title || '').toLowerCase().includes(keyword))
  if (target) {
    router.push(menuPath(target.path))
    globalSearch.value = ''
  } else {
    ElMessage.info('没有找到匹配的菜单，请尝试“报名”“赛程”或“成绩”。')
  }
}
function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'password') {
    router.push('/profile')
    ElMessage.info('请在个人中心查看可用的账户设置。')
    return
  }
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => undefined)
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  min-height: 100vh;
  background: #f4f7f6;
}
.layout-aside {
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow: hidden auto;
  flex-shrink: 0;
  background: #102b25;
  transition: width .22s ease;
  border-right: 1px solid #183b32;
}
.brand {
  min-height: 82px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 18px 18px 15px;
  cursor: pointer;
}
.brand-mark {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 13px;
  background: #d8f36a;
  color: #17392e;
  font-size: 22px;
}
.brand-name { color: #f4faf6; font-size: 15px; font-weight: 800; letter-spacing: .2px; white-space: nowrap; }
.brand-sub { color: #80a297; font-size: 9px; letter-spacing: 1.25px; margin-top: 5px; white-space: nowrap; }
.event-switch {
  margin: 0 13px 14px;
  padding: 12px;
  border: 1px solid #35574b;
  border-radius: 12px;
  text-align: left;
  background: #19382f;
  color: #f0f7f3;
  cursor: pointer;
}
.event-switch-label { display: block; margin-bottom: 8px; color: #8eada0; font-size: 9px; letter-spacing: 1.1px; }
.event-switch strong { display: flex; align-items: center; gap: 6px; font-size: 12px; line-height: 1.5; }
.event-switch strong .el-icon { margin-left: auto; flex-shrink: 0; }
.event-switch-hint { display: block; margin: 7px 0 0 13px; color: #8da99d; font-size: 10px; }
.event-dot { width: 7px; height: 7px; flex: 0 0 auto; border-radius: 50%; background: #d8f36a; }
.sidebar-menu { width: 100%; border-right: 0; padding: 0 9px 14px; flex: 1; }
.sidebar-menu :deep(.el-menu-item-group__title) {
  padding: 17px 12px 7px;
  color: #72958a;
  font-size: 10px;
  line-height: 1.4;
  letter-spacing: 1.2px;
}
.sidebar-menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  margin: 2px 0;
  border-radius: 9px;
  color: #b7cbc2;
  font-size: 12px;
}
.sidebar-menu :deep(.el-menu-item .el-icon) { font-size: 16px; }
.sidebar-menu :deep(.el-menu-item:hover) { background: #1c4035; color: #fff; }
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #d8f36a;
  color: #18392e;
  font-weight: 750;
}
.sidebar-bottom { padding: 8px 13px 18px; }
.help-card { padding: 13px; border: 1px solid #35574b; border-radius: 12px; background: #17372d; }
.help-title { display: flex; align-items: center; gap: 7px; color: #f0f7f3; font-size: 11px; font-weight: 750; }
.help-title .el-icon { color: #d8f36a; }
.help-card p { margin: 8px 0 11px; color: #9db5aa; font-size: 10px; line-height: 1.7; }
.help-card .el-button { width: 100%; border-color: #55766a; color: #e4f2e9; background: #21473a; }
.sidebar-footer { padding: 14px 4px 0; color: #68877b; font-size: 9px; text-align: center; }
.main-container { min-width: 0; }
.layout-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  height: 74px;
  padding: 0 28px;
  background: rgba(255,255,255,.94);
  border-bottom: 1px solid #e5ece8;
  backdrop-filter: blur(12px);
}
.header-left, .header-right, .user-info { display: flex; align-items: center; }
.header-left { min-width: 0; gap: 13px; }
.collapse-btn { color: #49655a; font-size: 18px; }
.breadcrumb-wrap { min-width: 0; }
.header-subtitle { margin-top: 5px; color: #89968f; font-size: 10px; }
.header-right { gap: 11px; }
.global-search { width: 205px; }
.global-search :deep(.el-input__wrapper) { border-radius: 10px; box-shadow: 0 0 0 1px #e2eae5 inset; background: #f8faf9; }
.user-info { gap: 9px; padding: 4px 0 4px 7px; cursor: pointer; }
.user-copy { display: flex; flex-direction: column; gap: 3px; }
.user-copy strong { color: #24382f; font-size: 11px; font-weight: 750; }
.user-copy small { color: #84938b; font-size: 10px; }
.dropdown-arrow { color: #829188; margin-left: 1px; }
.layout-main { padding: 24px; min-width: 0; background: #f4f7f6; }
.fade-enter-active, .fade-leave-active { transition: opacity .18s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
@media (max-width: 980px) {
  .layout-header { padding: 0 16px; }
  .header-subtitle { display: none; }
  .global-search { width: 160px; }
  .layout-main { padding: 16px; }
}
@media (max-width: 680px) {
  .user-copy, .dropdown-arrow, .global-search { display: none; }
  .layout-header { gap: 8px; }
  .layout-main { padding: 12px; }
  .breadcrumb-wrap :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { font-size: 12px; }
}
</style>
