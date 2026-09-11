<script lang="ts" setup>
import { inject } from 'vue'
import { useAppStore } from '@/store/modules/app'
import { useDesign } from '@/hooks/web/useDesign'
import { Icon } from '@/components/Icon'
import {
  LUNA_AUTO_COLLAPSE_KEY,
  type LunaAutoCollapseApi
} from './useLunaAutoCollapse'

defineOptions({ name: 'LunaMenuReopen' })

const { getPrefixCls } = useDesign()

const prefixCls = getPrefixCls('luna-menu-reopen')

const appStore = useAppStore()

const autoCollapse = inject<LunaAutoCollapseApi | null>(LUNA_AUTO_COLLAPSE_KEY, null)

const expand = () => {
  if (autoCollapse) {
    autoCollapse.expand()
  } else {
    appStore.setCollapse(false)
  }
}

const expandOnHover = () => {
  autoCollapse?.expandOnHover()
}
</script>

<template>
  <div
    :class="prefixCls"
    class="fixed left-0 top-1/2 z-30 flex h-84px w-22px -translate-y-1/2 cursor-pointer items-center justify-center rounded-r-4px bg-[var(--left-menu-bg-color)] text-[var(--left-menu-text-color)] shadow-lg"
    title="展开菜单"
    @click="expand"
    @mouseenter="expandOnHover"
  >
    <Icon icon="ep:expand" :size="16" />
  </div>
</template>
