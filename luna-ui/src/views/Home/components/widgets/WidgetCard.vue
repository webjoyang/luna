<template>
  <div :class="prefixCls">
    <header v-if="title || eyebrow || $slots.extra" :class="`${prefixCls}__head`">
      <div :class="`${prefixCls}__titles`">
        <div v-if="eyebrow" :class="`${prefixCls}__eyebrow`">{{ eyebrow }}</div>
        <h3 v-if="title" :class="`${prefixCls}__title`">{{ title }}</h3>
        <div v-if="subtitle" :class="`${prefixCls}__subtitle`">{{ subtitle }}</div>
      </div>
      <div :class="`${prefixCls}__extra`">
        <slot name="extra"></slot>
      </div>
    </header>

    <div :class="`${prefixCls}__body`">
      <slot></slot>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'

defineOptions({ name: 'WidgetCard' })

defineProps<{
  title?: string
  eyebrow?: string
  subtitle?: string
}>()

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-widget')
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-widget;

.#{$prefix-cls} {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 22px;
  background: rgb(255 255 255 / 70%);
  border: 1px solid rgb(255 255 255 / 90%);
  border-radius: var(--border-radius-card, 24px);
  box-shadow: 0 8px 32px -6px rgb(0 0 0 / 5%);
  backdrop-filter: blur(24px);

  &__head {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  &__titles {
    min-width: 0;
  }

  &__eyebrow {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: #aeaeb2;
    text-transform: uppercase;
  }

  &__title {
    margin: 4px 0 0;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #1d1d1f;
  }

  &__subtitle {
    margin-top: 5px;
    font-size: 12.5px;
    color: #86868b;
  }

  &__extra {
    display: flex;
    flex: none;
    gap: 8px;
    align-items: center;
  }

  &__body {
    flex: 1;
    min-height: 0;
  }
}
</style>
