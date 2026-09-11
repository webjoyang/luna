import { onMounted, onBeforeUnmount, watch, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/modules/app'

export const LUNA_AUTO_COLLAPSE_KEY = 'lunaAutoCollapse'

const COLLAPSE_DELAY_MS = 280

export type LunaAutoCollapseApi = {
  expandOnHover: () => void
  expand: () => void
  scheduleCollapse: () => void
  cancelCollapse: () => void
  setMorePanelOpen: (open: boolean) => void
}

export const useLunaAutoCollapse = (): LunaAutoCollapseApi => {
  const appStore = useAppStore()
  const route = useRoute()

  let collapseTimer: ReturnType<typeof setTimeout> | null = null
  const morePanelOpen = ref(false)
  let lastExpandAt = 0

  const isMobile = () => appStore.getMobile

  const cancelCollapse = () => {
    if (collapseTimer) {
      clearTimeout(collapseTimer)
      collapseTimer = null
    }
  }

  const collapseNow = () => {
    cancelCollapse()
    appStore.setCollapse(true)
  }

  const expand = () => {
    cancelCollapse()
    lastExpandAt = Date.now()
    appStore.setCollapse(false)
  }

  /** 桌面悬停展开；移动端 no-op（靠点击 expand） */
  const expandOnHover = () => {
    if (isMobile()) return
    expand()
  }

  /** 桌面延时收起；更多面板打开中或移动端 no-op */
  const scheduleCollapse = () => {
    if (isMobile() || morePanelOpen.value) return
    cancelCollapse()
    // 刚从展开条切到侧栏时给一点宽限，避免 DOM 替换瞬间误收起
    const justExpanded = Date.now() - lastExpandAt < 120
    const delay = justExpanded ? 420 : COLLAPSE_DELAY_MS
    collapseTimer = setTimeout(() => {
      collapseTimer = null
      if (!morePanelOpen.value && !isMobile()) {
        appStore.setCollapse(true)
      }
    }, delay)
  }

  const setMorePanelOpen = (open: boolean) => {
    morePanelOpen.value = open
    if (open) {
      cancelCollapse()
    }
  }

  onMounted(() => {
    // 进入后台默认完全隐藏侧栏
    appStore.setCollapse(true)
  })

  // 路由变化（点菜单 / TagsView 等）后收起
  watch(
    () => route.fullPath,
    () => {
      collapseNow()
    },
    { flush: 'post' }
  )

  onBeforeUnmount(() => {
    cancelCollapse()
  })

  return {
    expandOnHover,
    expand,
    scheduleCollapse,
    cancelCollapse,
    setMorePanelOpen
  }
}
