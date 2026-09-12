<template>
  <WidgetCard title="液态时钟">
    <div :class="`${prefixCls}__clock`">
      <template v-for="(char, index) in chars" :key="index">
        <span v-if="char === ':'" :class="`${prefixCls}__sep`">:</span>
        <span v-else :class="`${prefixCls}__slot`">
          <Transition name="wb-roll">
            <span :key="char" :class="`${prefixCls}__digit`">{{ char }}</span>
          </Transition>
        </span>
      </template>
    </div>

    <div :class="`${prefixCls}__foot`">{{ dateText }}</div>
  </WidgetCard>
</template>

<script lang="ts" setup>
import { useNow } from '@vueuse/core'
import { useDesign } from '@/hooks/web/useDesign'
import WidgetCard from './WidgetCard.vue'

defineOptions({ name: 'LiquidClockWidget' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-liquid-clock')

const now = useNow({ interval: 1000 })

const pad = (n: number) => String(n).padStart(2, '0')

const chars = computed(() => {
  const date = now.value
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`.split('')
})

const dateText = computed(() => {
  const date = now.value
  return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月 ${date.getDate()} 日`
})
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-liquid-clock;

.#{$prefix-cls} {
  &__clock {
    display: flex;
    align-items: center;
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 42px;
    font-weight: 900;
    font-variant-numeric: tabular-nums;
    line-height: 1.1;
    color: #1d1d1f;
  }

  &__slot {
    position: relative;
    display: inline-block;
    width: 0.62em;
    height: 1.1em;
    overflow: hidden;
  }

  &__digit {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
  }

  &__sep {
    padding: 0 2px;
    color: rgb(99 102 241 / 30%);
  }

  &__foot {
    margin-top: 14px;
    font-size: 12.5px;
    color: #86868b;
  }

  :deep(.wb-roll-enter-active),
  :deep(.wb-roll-leave-active) {
    transition:
      transform 0.36s cubic-bezier(0.22, 1, 0.36, 1),
      opacity 0.36s ease;
  }

  :deep(.wb-roll-enter-from) {
    opacity: 0;
    transform: translateY(-70%);
  }

  :deep(.wb-roll-leave-to) {
    opacity: 0;
    transform: translateY(70%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .#{$prefix-cls} {
    :deep(.wb-roll-enter-active),
    :deep(.wb-roll-leave-active) {
      transition: none;
    }
  }
}
</style>
