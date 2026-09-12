<template>
  <WidgetCard>
    <template #extra>
      <span :class="[`${prefixCls}__bell`, { 'is-urgent': data.urgent }]">
        <Icon icon="ep:bell" :size="18" color="#fff" />
      </span>
    </template>

    <div :class="`${prefixCls}__label`">{{ data.label }}</div>
    <div :class="`${prefixCls}__value`">{{ data.count }}<small>MSG</small></div>
    <div :class="`${prefixCls}__title`">{{ data.title }}</div>
    <div :class="`${prefixCls}__detail`">{{ data.detail }}</div>
  </WidgetCard>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'
import { Icon } from '@/components/Icon'
import WidgetCard from './WidgetCard.vue'
import { aiReminder as data } from '../../data/mockPanels'

defineOptions({ name: 'AiReminderWidget' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-ai-reminder')
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-ai-reminder;

.#{$prefix-cls} {
  &__bell {
    position: relative;
    display: grid;
    width: 38px;
    height: 38px;
    background: linear-gradient(150deg, #f87171, #dc2626);
    border-radius: 13px;
    place-items: center;

    &.is-urgent::after {
      position: absolute;
      border: 2px solid rgb(220 38 38 / 60%);
      border-radius: 13px;
      content: '';
      animation: wb-ai-ping 1.8s cubic-bezier(0, 0, 0.2, 1) infinite;
      inset: 0;
    }
  }

  &__label {
    font-size: 12px;
    font-weight: 700;
    color: #d97706;
  }

  &__value {
    margin-top: 6px;
    font-size: 46px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.04em;
    font-variant-numeric: tabular-nums;
    color: #1d1d1f;

    small {
      margin-left: 6px;
      font-size: 13px;
      font-weight: 700;
      letter-spacing: 0.06em;
      color: #aeaeb2;
    }
  }

  &__title {
    margin-top: 12px;
    font-size: 13.5px;
    font-weight: 600;
    line-height: 1.5;
    color: #1d1d1f;
  }

  &__detail {
    margin-top: 6px;
    font-size: 12.5px;
    line-height: 1.65;
    color: #86868b;
  }
}

@media (prefers-reduced-motion: reduce) {
  .#{$prefix-cls} {
    &__bell.is-urgent::after {
      animation: none;
    }
  }
}

@keyframes wb-ai-ping {
  0% {
    opacity: 0.9;
    transform: scale(1);
  }

  75%,
  100% {
    opacity: 0;
    transform: scale(1.6);
  }
}
</style>
