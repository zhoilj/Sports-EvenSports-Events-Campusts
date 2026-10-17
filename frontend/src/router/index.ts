import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', group: '工作台' }
      },
      // 赛事管理
      {
        path: 'events',
        name: 'Events',
        component: () => import('@/views/event/list.vue'),
        meta: { title: '赛事管理', icon: 'Trophy', roles: ['ADMIN', 'TEACHER'], group: '赛前准备' }
      },
      {
        path: 'events/:id',
        name: 'EventDetail',
        component: () => import('@/views/event/detail.vue'),
        meta: { title: '赛事详情', hidden: true }
      },
      // 运动项目
      {
        path: 'sport-items',
        name: 'SportItems',
        component: () => import('@/views/sportitem/list.vue'),
        meta: { title: '运动项目', icon: 'Football', roles: ['ADMIN', 'TEACHER'], group: '赛前准备' }
      },
      // 报名管理
      {
        path: 'registrations',
        name: 'Registrations',
        component: () => import('@/views/registration/list.vue'),
        meta: { title: '报名管理', icon: 'Document', roles: ['ADMIN', 'TEACHER'], group: '赛前准备' }
      },
      {
        path: 'my-registrations',
        name: 'MyRegistrations',
        component: () => import('@/views/registration/my.vue'),
        meta: { title: '我的报名', icon: 'Tickets', roles: ['STUDENT'], group: '我的参赛' }
      },
      {
        path: 'relay-teams',
        name: 'RelayTeams',
        component: () => import('@/views/registration/relay-teams.vue'),
        meta: { title: '接力组队', icon: 'UserFilled', roles: ['ADMIN', 'TEACHER', 'STUDENT'], group: '赛前准备' }
      },
      // 赛程管理
      {
        path: 'schedules',
        name: 'Schedules',
        component: () => import('@/views/schedule/list.vue'),
        meta: { title: '赛程安排', icon: 'Calendar', group: '赛程与检录' }
      },
      // 竞赛执行
      {
        path: 'schedule-execution',
        name: 'ScheduleExecution',
        component: () => import('@/views/schedule/execution.vue'),
        meta: { title: '编排与现场检录', icon: 'Timer', roles: ['ADMIN', 'TEACHER'], group: '赛程与检录' }
      },
      {
        path: 'field-attempts',
        name: 'FieldAttempts',
        component: () => import('@/views/schedule/field-attempts.vue'),
        meta: { title: '田赛试次录入', icon: 'Aim', roles: ['ADMIN', 'TEACHER'], group: '赛程与检录' }
      },
      {
        path: 'my-schedule-entries',
        name: 'MyScheduleEntries',
        component: () => import('@/views/schedule/my-entries.vue'),
        meta: { title: '我的赛程与检录号', icon: 'Tickets', roles: ['STUDENT'], group: '我的参赛' }
      },
      // 成绩管理
      {
        path: 'results',
        name: 'Results',
        component: () => import('@/views/result/list.vue'),
        meta: { title: '成绩录入', icon: 'Medal', roles: ['ADMIN', 'TEACHER'], group: '成绩管理' }
      },
      {
        path: 'result-review',
        name: 'ResultReview',
        component: () => import('@/views/result/review.vue'),
        meta: { title: '成绩复核与发布', icon: 'Finished', roles: ['ADMIN', 'TEACHER'], group: '成绩管理' }
      },
      {
        path: 'scoring-rules',
        name: 'ScoringRules',
        component: () => import('@/views/result/scoring-rules.vue'),
        meta: { title: '团体积分规则', icon: 'SetUp', roles: ['ADMIN', 'TEACHER'], group: '成绩管理' }
      },
      {
        path: 'my-results',
        name: 'MyResults',
        component: () => import('@/views/result/my.vue'),
        meta: { title: '我的成绩', icon: 'DataLine', roles: ['STUDENT'], group: '我的参赛' }
      },
      // 新闻公告
      {
        path: 'news',
        name: 'News',
        component: () => import('@/views/news/list.vue'),
        meta: { title: '新闻公告', icon: 'Bell', group: '赛事信息' }
      },
      {
        path: 'reports',
        name: 'CompetitionReports',
        component: () => import('@/views/statistics/reports.vue'),
        meta: { title: '秩序册与团体排名', icon: 'Files', roles: ['ADMIN', 'TEACHER'], group: '统计报表' }
      },
      // 用户管理
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/list.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'], group: '系统管理' }
      },
      // 统计分析
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/index.vue'),
        meta: { title: '统计分析', icon: 'DataAnalysis', roles: ['ADMIN', 'TEACHER'], group: '统计报表' }
      },
      // 个人中心
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'UserFilled', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '校园体育赛事管理系统'} - 校园体育赛事管理系统`

  const userStore = useUserStore()
  const token = userStore.token

  // 公开页面直接放行
  if (to.meta.public) {
    next()
    return
  }

  // 未登录跳转登录页
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 检查角色权限
  const roles = to.meta.roles as string[] | undefined
  if (roles && roles.length > 0) {
    const userRole = userStore.userInfo?.role
    if (!userRole || !roles.includes(userRole)) {
      next({ path: '/dashboard' })
      return
    }
  }

  next()
})

export default router