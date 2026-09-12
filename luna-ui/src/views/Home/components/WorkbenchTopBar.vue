<template>
  <header :class="prefixCls">
    <div :class="`${prefixCls}__brand`">
      <span :class="`${prefixCls}__mark`" aria-hidden="true"></span>
      <span :class="`${prefixCls}__name`">工作台</span>
      <span :class="`${prefixCls}__crumb`">/ {{ activeLabel }}</span>
    </div>

    <div :class="`${prefixCls}__spacer`"></div>

    <el-segmented
      :class="`${prefixCls}__seg`"
      :model-value="active"
      :options="options"
      @change="handleChange"
    />

    <span :class="`${prefixCls}__clock`">{{ clock }}</span>
    <span :class="`${prefixCls}__avatar`" :title="userName">{{ avatarChar }}</span>
  </header>
</template>

<script lang="ts" setup>
import { useNow } from '@vueuse/core'
import { useDesign } from '@/hooks/web/useDesign'
import type { HomeScope, ScopeTab } from '../data/types'

defineOptions({ name: 'WorkbenchTopBar' })

const props = defineProps<{
  active: HomeScope
  tabs: ScopeTab[]
  userName: string
}>()

const emit = defineEmits<{ change: [value: HomeScope] }>()

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-topbar')

const options = computed(() => props.tabs.map((tab) => ({ label: tab.label, value: tab.key })))

const activeLabel = computed(() => props.tabs.find((tab) => tab.key === props.active)?.label ?? '')

const avatarChar = computed(() => (props.userName || '我').slice(0, 1))

const now = useNow({ interval: 1000 })

const clock = computed(() => {
  const date = now.value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
})

const handleChange = (value: string | number | boolean) => {
  emit('change', value as HomeScope)
}
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-topbar;

.#{$prefix-cls} {
  position: sticky;
  top: 8px;
  z-index: 30;
  display: flex;
  padding: 10px 16px;
  background: rgb(255 255 255 / 60%);
  border: 1px solid rgb(255 255 255 / 85%);
  border-radius: 24px;
  box-shadow: 0 18px 44px -28px rgb(30 30 70 / 45%);
  animation: wb-topbar-in 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  backdrop-filter: blur(24px);

  &__brand {
    display: flex;
    gap: 10px;
    align-items: center;
    min-width: 0;
  }

  &__mark {
    width: 20px;
    height: 20px;
    background: linear-gradient(140deg, var(--primary-color), var(--primary-color-hover));
    border-radius: 7px;
    box-shadow: 0 2px 6px -1px var(--primary-color-light);
    flex: none;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    letter-spacing: -0.01em;
    color: #1d1d1f;
  }

  &__crumb {
    display: none;
    font-size: 13.5px;
    color: #aeaeb2;
  }

  &__spacer {
    flex: 1;
  }

  &__seg {
    flex: none;
  }

  &__clock {
    display: none;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 13.5px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: #6e6e73;
  }

  &__avatar {
    display: grid;
    width: 28px;
    height: 28px;
    font-size: 12px;
    font-weight: 700;
    color: #fff;
    background: linear-gradient(140deg, var(--primary-color), var(--primary-color-hover));
    border-radius: 9px;
    flex: none;
    place-items: center;
  }

  /* 把 el-segmented 覆盖成源站的胶囊页签外观 */
  :deep(.el-segmented) {
    --el-segmented-bg-color: rgb(0 0 0 / 4.5%);
    --el-segmented-color: #6e6e73;
    --el-segmented-item-selected-color: #fff;
    --el-segmented-item-selected-bg-color: var(--primary-color);
    --el-segmented-item-hover-color: #1d1d1f;
    --el-segmented-item-hover-bg-color: rgb(0 0 0 / 4%);
    --el-segmented-item-active-bg-color: rgb(0 0 0 / 6%);

    padding: 3px;
    border-radius: 999px;
  }

  :deep(.el-segmented__item) {
    padding: 6px 14px;
    font-size: 12.5px;
    font-weight: 600;
    border-radius: 999px;
    transition: color 0.3s ease;
  }

  :deep(.el-segmented__item-label) {
    padding: 0;
  }

  :deep(.el-segmented__item-selected) {
    box-shadow: 0 4px 10px -4px rgb(15 23 42 / 45%);
  }
}

@media (width >= 768px) {
  .#{$prefix-cls} {
    padding: 10px 24px;

    &__crumb,
    &__clock {
      display: inline;
    }

    :deep(.el-segmented__item) {
      padding: 6px 16px;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .#{$prefix-cls} {
    animation: none;

    :deep(.el-segmented__item) {
      transition: none;
    }
  }
}

@keyframes wb-topbar-in {
  from {
    opacity: 0;
    transform: translateY(-14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
