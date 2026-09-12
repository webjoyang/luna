<template>
  <WidgetCard title="实时汇率">
    <template #extra>
      <el-tag v-if="data.stale" type="warning" size="small" effect="light">数据延迟</el-tag>
      <span v-else :class="`${prefixCls}__live`">LIVE</span>
    </template>

    <div :class="`${prefixCls}__value`"> {{ rateText }}<small>USD/CNY</small> </div>

    <div :class="`${prefixCls}__foot`">更新 {{ data.updatedLabel }}</div>
  </WidgetCard>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'
import WidgetCard from './WidgetCard.vue'
import { exchangeRate as data } from '../../data/mockPanels'

defineOptions({ name: 'ExchangeRateWidget' })

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-exchange')

const rateText = computed(() => data.rate.toFixed(2))
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-exchange;

.#{$prefix-cls} {
  &__live {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.1em;
    color: #059669;
  }

  &__value {
    font-size: 46px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.04em;
    font-variant-numeric: tabular-nums;
    color: #1d1d1f;

    small {
      margin-left: 8px;
      font-size: 13px;
      font-weight: 600;
      color: #86868b;
    }
  }

  &__foot {
    margin-top: 14px;
    font-size: 12.5px;
    color: #86868b;
  }
}
</style>
