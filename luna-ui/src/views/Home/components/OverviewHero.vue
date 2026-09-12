<template>
  <section :class="prefixCls">
    <div :class="`${prefixCls}__eyebrow`">
      工作台 · {{ now.getFullYear() }} 年 {{ now.getMonth() + 1 }} 月 {{ now.getDate() }} 日 ·
      星期{{ WEEKDAYS[now.getDay()] }}
    </div>

    <h1 :class="`${prefixCls}__title`">
      {{ greeting }}，<span :class="`${prefixCls}__accent`">{{ nickname }}</span>
    </h1>

    <p :class="`${prefixCls}__lead`">
      今天有 <b>{{ summary.approvals }} 件</b>等你批、<b>{{ summary.riskTasks }} 项</b>要你跟、<b
        >{{ summary.focusUnread }} 条</b
      >要你看；在产 <b>{{ summary.inProduction }} 单</b>机器替你盯着。
    </p>

    <PendingApprovalRing :count="summary.approvals" @click="emit('stat-click', 'approve')" />

    <div :class="`${prefixCls}__stats`">
      <button
        v-for="item in stats"
        :key="item.key"
        type="button"
        :class="[`${prefixCls}__stat`, `${prefixCls}__stat--${item.tone}`]"
        @click="emit('stat-click', item.key)"
      >
        <span :class="`${prefixCls}__stat-label`">{{ item.label }}</span>
        <span :class="`${prefixCls}__stat-value`">
          {{ item.value }}<small>{{ item.unit }}</small>
        </span>
      </button>
    </div>
  </section>
</template>

<script lang="ts" setup>
import { useNow } from '@vueuse/core'
import { useDesign } from '@/hooks/web/useDesign'
import { useUserStore } from '@/store/modules/user'
import PendingApprovalRing from './PendingApprovalRing.vue'
import { workbenchGreeting } from '../data/mockWorkbench'
import type { StatItem, StatKey, WorkbenchSummary } from '../data/types'

defineOptions({ name: 'OverviewHero' })

defineProps<{ summary: WorkbenchSummary; stats: StatItem[] }>()

const emit = defineEmits<{ 'stat-click': [key: StatKey] }>()

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-hero')

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

const now = useNow({ interval: 60_000 })

const userStore = useUserStore()
const nickname = computed(() => userStore.getUser?.nickname || '主理人')

const greeting = computed(() => workbenchGreeting(now.value.getHours()))
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-hero;

.#{$prefix-cls} {
  padding: 48px 16px 40px;
  text-align: center;
  animation: wb-hero-in 0.75s cubic-bezier(0.22, 1, 0.36, 1) both;

  &__eyebrow {
    font-size: 12.5px;
    font-weight: 600;
    color: #86868b;
  }

  &__title {
    margin: 12px 0 16px;
    font-size: clamp(30px, 4.6vw, 54px);
    font-weight: 700;
    line-height: 1.06;
    letter-spacing: -0.03em;
    color: #1d1d1f;
  }

  &__accent {
    color: transparent;
    background: linear-gradient(90deg, var(--primary-color), var(--primary-color-hover));
    background-clip: text;
  }

  &__lead {
    max-width: 44em;
    margin: 0 auto;
    font-size: 15px;
    line-height: 1.7;
    color: #6e6e73;

    b {
      font-weight: 600;
      color: #1d1d1f;
    }
  }

  &__stats {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    max-width: 980px;
    margin: 36px auto 0;
  }

  &__stat {
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
    padding: 8px 24px;
    margin: 8px 0;
    cursor: pointer;
    background: transparent;
    border: none;
    border-left: 1px solid rgb(0 0 0 / 10%);
    border-radius: 12px;
    transition: all 0.3s ease;

    &:first-child {
      border-left: none;
    }

    &:hover {
      background: rgb(0 0 0 / 3.5%);
      transform: translateY(-2px);
    }

    &:focus-visible {
      outline: 3px solid var(--primary-color-light);
    }
  }

  &__stat-label {
    font-size: 12px;
    font-weight: 600;
    color: #86868b;
  }

  &__stat-value {
    font-size: 29px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.02em;
    font-variant-numeric: tabular-nums;
    color: #1d1d1f;

    small {
      margin-left: 2px;
      font-size: 14px;
      font-weight: 600;
      color: #86868b;
    }
  }

  &__stat--ok &__stat-value {
    color: #059669;
  }

  &__stat--bad &__stat-value {
    color: #dc2626;
  }
}

/* 窄屏：四格统计折成 2×2，用中线分隔代替竖排分隔线 */
@media (width <= 767px) {
  .#{$prefix-cls} {
    &__stats {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 4px;
    }

    &__stat {
      align-items: center;
      padding: 10px 8px;
      margin: 0;
      border-left: none;

      &:nth-child(odd) {
        border-right: 1px solid rgb(0 0 0 / 10%);
      }

      &:first-child {
        border-left: none;
      }
    }
  }
}

@media (width >= 768px) {
  .#{$prefix-cls} {
    padding: 48px 24px 44px;

    &__stat {
      padding: 8px 32px;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .#{$prefix-cls} {
    animation: none;

    &__stat {
      transition: none;

      &:hover {
        transform: none;
      }
    }
  }
}

@keyframes wb-hero-in {
  from {
    opacity: 0;
    transform: translateY(24px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
