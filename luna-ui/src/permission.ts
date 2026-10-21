import router from './router'
import type { RouteRecordRaw } from 'vue-router'
import { isRelogin } from '@/config/axios/service'
import { getAccessToken } from '@/utils/auth'
import { useTitle } from '@/hooks/web/useTitle'
import { useNProgress } from '@/hooks/web/useNProgress'
import { usePageLoading } from '@/hooks/web/usePageLoading'
import { useDictStoreWithOut } from '@/store/modules/dict'
import { useUserStoreWithOut } from '@/store/modules/user'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { parseRouteLocation } from '@/utils/routeParams'
import { isUnactivatedModuleRoute } from '@/utils/moduleGateway'
import { handlePortalTokenGuide, isPortalPath, isPortalInvalidPath } from '@/utils/portalAuth'
import { canAccessH5Path, getDefaultH5Path, hasAnyWorkshopPermission } from '@/utils/h5Auth'

const { start, done } = useNProgress()

const { loadStart, loadDone } = usePageLoading()

// 路由不重定向白名单
const whiteList = [
  '/',
  '/login',
  '/luna-login',
  '/social-login',
  '/auth-redirect',
  '/bind',
  '/register',
  '/oauthLogin/gitee',
  '/pms/kb/document/share',
  '/portal',
  '/h5/login'
]

// 路由加载前
router.beforeEach(async (to, from, next) => {
  start()
  loadStart()

  // 客户门户免登路由与 token 引导
  if (isPortalPath(to.path)) {
    const queryToken = (to.query.t as string) || null
    const { isValid } = handlePortalTokenGuide(queryToken)
    if (!isPortalInvalidPath(to.path) && !isValid) {
      next({ path: '/portal/invalid', replace: true })
      return
    }
    next()
    return
  }
  if (getAccessToken()) {
    if (to.path === '/login' || to.path === '/luna-login') {
      next({ path: '/index' })
    } else if (to.path === '/h5/login') {
      const redirect = (to.query.redirect as string) || '/h5'
      next({ path: redirect })
    } else {
      const dictStore = useDictStoreWithOut()
      const userStore = useUserStoreWithOut()
      const permissionStore = usePermissionStoreWithOut()
      // 异步加载字典
      // 另外，间接 issue：https://gitee.com/yudaocode/yudao-ui-admin-vue3/issues/ID9FLI
      if (!dictStore.getIsSetDict) {
        dictStore.setDictMap().then()
      }
      if (!userStore.getIsSetUser) {
        isRelogin.show = true
        await userStore.setUserInfoAction()
        isRelogin.show = false
        // 后端过滤菜单
        await permissionStore.generateRoutes()
        permissionStore.getAddRouters.forEach((route) => {
          router.addRoute(route as unknown as RouteRecordRaw) // 动态添加可访问路由表
        })
        const redirectPath = from.query.redirect
        // 修复跳转时不带参数的问题
        const redirect = typeof redirectPath === 'string' ? redirectPath : to.fullPath
        const redirectLocation = parseRouteLocation(redirect)
        const targetPath = redirectLocation.path || to.path
        if (targetPath.startsWith('/h5')) {
          if (!hasAnyWorkshopPermission(userStore.permissions)) {
            next({ path: '/h5/403', replace: true })
            return
          }
          if (targetPath === '/h5' || targetPath === '/h5/') {
            next({ path: getDefaultH5Path(userStore.permissions), replace: true })
            return
          }
          if (!canAccessH5Path(targetPath, userStore.permissions)) {
            next({ path: getDefaultH5Path(userStore.permissions), replace: true })
            return
          }
          const nextData =
            to.fullPath === redirect
              ? { ...to, replace: true }
              : { ...redirectLocation, replace: true }
          next(nextData)
          return
        }
        if (isUnactivatedModuleRoute(targetPath, permissionStore.getAddRouters)) {
          next({ path: '/index', replace: true })
          return
        }
        const nextData =
          to.fullPath === redirect
            ? { ...to, replace: true }
            : { ...redirectLocation, replace: true }
        next(nextData)
      } else {
        if (to.path.startsWith('/h5')) {
          if (!hasAnyWorkshopPermission(userStore.permissions)) {
            if (to.path === '/h5/403') {
              next()
            } else {
              next({ path: '/h5/403', replace: true })
            }
            return
          }
          if (to.path === '/h5' || to.path === '/h5/') {
            next({ path: getDefaultH5Path(userStore.permissions), replace: true })
            return
          }
          if (!canAccessH5Path(to.path, userStore.permissions)) {
            next({ path: getDefaultH5Path(userStore.permissions), replace: true })
            return
          }
          next()
          return
        }
        if (isUnactivatedModuleRoute(to.path, permissionStore.getAddRouters)) {
          next({ path: '/index', replace: true })
        } else {
          next()
        }
      }
    }
  } else {
    if (whiteList.some((path) => to.path === path || to.path.startsWith(`${path}/`))) {
      next()
    } else if (to.path.startsWith('/h5')) {
      next(`/h5/login?redirect=${encodeURIComponent(to.fullPath)}`)
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`) // 否则全部重定向到登录页
    }
  }
})

router.afterEach((to) => {
  useTitle(to?.meta?.title as string)
  done() // 结束Progress
  loadDone()
})
