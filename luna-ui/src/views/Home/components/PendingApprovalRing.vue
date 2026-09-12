<template>
  <div :class="prefixCls">
    <div :class="`${prefixCls}__halo`" aria-hidden="true"></div>

    <button
      type="button"
      :class="`${prefixCls}__disc`"
      :aria-label="`打开要你批列表，共 ${count} 件`"
      @click="emit('click')"
    >
      <svg :class="`${prefixCls}__svg`" viewBox="0 0 200 200">
        <defs>
          <linearGradient id="wb-ring-grad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="var(--primary-color)" />
            <stop offset="100%" stop-color="var(--primary-color-hover)" />
          </linearGradient>
        </defs>
        <circle cx="100" cy="100" :r="R" fill="none" stroke="#ececf0" stroke-width="13" />
        <circle
          :class="`${prefixCls}__arc`"
          cx="100"
          cy="100"
          :r="R"
          fill="none"
          stroke="url(#wb-ring-grad)"
          stroke-width="13"
          stroke-linecap="round"
          :stroke-dasharray="C"
          :stroke-dashoffset="dashOffset"
        />
      </svg>

      <div :class="`${prefixCls}__center`">
        <div :class="`${prefixCls}__label`">与我相关的待审批</div>
        <div :class="`${prefixCls}__value`">{{ count }}<small>件</small></div>
        <div :class="`${prefixCls}__hint`">
          {{ count > 0 ? '所有尚未完成的审批' : '暂无待我审批的事项' }}
        </div>
      </div>
    </button>
  </div>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'

defineOptions({ name: 'PendingApprovalRing' })

const props = defineProps<{ count: number }>()

const emit = defineEmits<{ click: [] }>()

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-ring')

const R = 85
const C = 2 * Math.PI * R

/** 入场时让进度环从 0 走到满，模拟源站的一次性过渡 */
const ready = ref(false)

onMounted(() => {
  requestAnimationFrame(() => {
    ready.value = true
  })
})

const dashOffset = computed(() => (ready.value && props.count > 0 ? 0 : C))
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-ring;

.#{$prefix-cls} {
  position: relative;
  display: flex;
  justify-content: center;
  margin-top: 36px;

  &__halo {
    position: absolute;
    top: -14px;
    width: min(320px, 78vw);
    pointer-events: none;
    background: conic-gradient(
      from 0deg,
      var(--primary-color),
      var(--primary-color-light),
      var(--primary-color-hover),
      var(--primary-color)
    );
    border-radius: 50%;
    opacity: 0.4;
    filter: blur(34px);
    animation: wb-ring-spin 24s linear infinite;
    aspect-ratio: 1;
  }

  &__disc {
    position: relative;
    display: grid;
    width: min(288px, 72vw);
    padding: 0;
    cursor: pointer;
    background: #fff;
    border: none;
    border-radius: 50%;
    box-shadow:
      0 40px 80px -30px rgb(0 0 0 / 28%),
      inset 0 0 0 1px rgb(0 0 0 / 4%);
    animation: wb-ring-breathe 8s ease-in-out infinite;
    transition: box-shadow 0.35s ease;
    place-items: center;
    aspect-ratio: 1;

    &:hover {
      box-shadow:
        0 44px 88px -28px rgb(0 0 0 / 34%),
        inset 0 0 0 1px rgb(0 0 0 / 6%);
    }

    &:focus-visible {
      outline: 4px solid var(--primary-color-light);
      outline-offset: 3px;
    }
  }

  &__svg {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    transform: rotate(-90deg);
  }

  &__arc {
    transition: stroke-dashoffset 1.4s cubic-bezier(0.22, 1, 0.36, 1);
  }

  &__center {
    position: relative;
    max-width: 76%;
    text-align: center;
  }

  &__label {
    font-size: 12.5px;
    font-weight: 600;
    letter-spacing: 0.02em;
    color: #86868b;
  }

  &__value {
    margin-top: 8px;
    font-size: clamp(34px, 7.6vw, 46px);
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.03em;
    font-variant-numeric: tabular-nums;
    color: #1d1d1f;

    small {
      margin-left: 4px;
      font-size: 0.42em;
      font-weight: 600;
      color: #86868b;
    }
  }

  &__hint {
    margin-top: 10px;
    font-size: 12.5px;
    font-weight: 600;
    color: #86868b;
  }
}

@media (prefers-reduced-motion: reduce) {
  .#{$prefix-cls} {
    &__halo,
    &__disc {
      animation: none;
    }

    &__arc {
      transition: none;
    }
  }
}

@keyframes wb-ring-spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@keyframes wb-ring-breathe {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }

  50% {
    transform: translateY(-8px) scale(1.014);
  }
}
</style>
