<template>
  <section :class="prefixCls">
    <header :class="`${prefixCls}__head`">
      <div :class="`${prefixCls}__eyebrow`">今天 · 共 {{ totalPending }} 项待处理</div>
      <h2 :class="`${prefixCls}__title`">要处理的三件事</h2>
      <p :class="`${prefixCls}__desc`">点开就能办。同一个数字只在一个地方出现，不重复计。</p>
    </header>

    <el-row :gutter="22">
      <el-col v-for="card in cards" :key="card.key" :xs="24" :sm="24" :md="12" :lg="8">
        <article :class="[`${prefixCls}__card`, `is-${card.tone}`]">
          <span :class="`${prefixCls}__glow`" aria-hidden="true"></span>
          <span :class="`${prefixCls}__sheen`" aria-hidden="true"></span>

          <header :class="`${prefixCls}__card-head`">
            <span :class="`${prefixCls}__badge`">
              <Icon :icon="card.icon" :size="22" color="#fff" />
            </span>
            <span :class="`${prefixCls}__card-label`">{{ card.label }}</span>
          </header>

          <div :class="`${prefixCls}__value`"
            >{{ card.value }}<small>{{ card.unit }}</small></div
          >
          <div :class="`${prefixCls}__brk`">{{ card.brk }}</div>

          <div :class="`${prefixCls}__warn`">
            <i :class="`${prefixCls}__warn-dot`"></i>
            {{ card.warning }}
          </div>

          <ul :class="`${prefixCls}__rows`">
            <li v-for="(row, index) in card.previewRows" :key="index" :class="`${prefixCls}__row`">
              <span :class="`${prefixCls}__row-main`">
                <span :class="`${prefixCls}__row-title`">{{ row.title }}</span>
                <span :class="`${prefixCls}__row-sub`">{{ row.sub }}</span>
              </span>
              <span :class="`${prefixCls}__row-value`">{{ row.value }}</span>
              <Icon :class="`${prefixCls}__row-arrow`" icon="ep:arrow-right" :size="14" />
            </li>
          </ul>

          <div v-if="card.secondary" :class="`${prefixCls}__secondary`">{{ card.secondary }}</div>

          <div :class="`${prefixCls}__more`">{{ card.moreText }} →</div>

          <button
            type="button"
            :class="`${prefixCls}__overlay`"
            :aria-label="`${card.label}：${card.moreText}`"
            @click="emit('card-click', card.key)"
          ></button>
        </article>
      </el-col>
    </el-row>
  </section>
</template>

<script lang="ts" setup>
import { useDesign } from '@/hooks/web/useDesign'
import { Icon } from '@/components/Icon'
import type { ActionCardData } from '../data/types'

defineOptions({ name: 'ActionCards' })

const props = defineProps<{ cards: ActionCardData[] }>()

const emit = defineEmits<{ 'card-click': [key: 'approve' | 'follow' | 'read'] }>()

const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('workbench-cards')

const totalPending = computed(() => props.cards.reduce((acc, card) => acc + card.value, 0))
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-workbench-cards;

.#{$prefix-cls} {
  margin-top: 8px;

  &__head {
    margin-bottom: 20px;
  }

  &__eyebrow {
    font-size: 12.5px;
    font-weight: 600;
    color: #86868b;
  }

  &__title {
    margin: 6px 0;
    font-size: 24px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #1d1d1f;
  }

  &__desc {
    font-size: 13.5px;
    color: #6e6e73;
  }

  :deep(.el-col) {
    margin-bottom: 22px;
  }

  &__card {
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 32px 32px 24px;
    overflow: hidden;
    background: rgb(255 255 255 / 70%);
    border: 1px solid rgb(255 255 255 / 85%);
    border-radius: 30px;
    box-shadow:
      inset 0 1px 1px rgb(255 255 255 / 70%),
      0 18px 44px -28px rgb(30 30 70 / 45%);
    backdrop-filter: blur(24px);
    transition:
      transform 0.4s cubic-bezier(0.22, 1, 0.36, 1),
      box-shadow 0.4s ease;

    &:hover {
      transform: translateY(-9px);
      box-shadow:
        inset 0 1px 1px rgb(255 255 255 / 70%),
        0 30px 60px -30px rgb(30 30 70 / 55%);
    }

    &:hover .#{$prefix-cls}__sheen {
      transform: translateX(220%) skewX(-18deg);
    }
  }

  &__glow {
    position: absolute;
    top: -120px;
    left: -70px;
    width: 230px;
    height: 230px;
    pointer-events: none;
    border-radius: 50%;
    opacity: 0.5;
    filter: blur(50px);
  }

  &__sheen {
    position: absolute;
    top: 0;
    left: 0;
    width: 60px;
    height: 100%;
    pointer-events: none;
    background: linear-gradient(90deg, transparent, rgb(255 255 255 / 55%), transparent);
    transform: translateX(-140%) skewX(-18deg);
    transition: transform 0.9s cubic-bezier(0.22, 1, 0.36, 1);
  }

  &__card-head {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  &__badge {
    display: grid;
    place-items: center;
    width: 46px;
    height: 46px;
    border-radius: 15px;
  }

  &__card-label {
    font-size: 14px;
    font-weight: 600;
    color: #6e6e73;
  }

  &__value {
    margin-top: 18px;
    font-size: 64px;
    font-weight: 700;
    line-height: 1;
    letter-spacing: -0.04em;
    font-variant-numeric: tabular-nums;
    color: #1d1d1f;

    small {
      margin-left: 6px;
      font-size: 16px;
      font-weight: 600;
      color: #86868b;
    }
  }

  &__brk {
    margin-top: 10px;
    font-size: 12.5px;
    color: #86868b;
  }

  &__warn {
    display: flex;
    gap: 8px;
    align-items: center;
    margin-top: 14px;
    font-size: 12.5px;
    font-weight: 600;
  }

  &__warn-dot {
    position: relative;
    flex: none;
    width: 7px;
    height: 7px;
    color: inherit;
    background: currentcolor;
    border-radius: 50%;

    &::after {
      position: absolute;
      background: currentcolor;
      border-radius: 50%;
      content: '';
      animation: wb-cards-ping 1.8s cubic-bezier(0, 0, 0.2, 1) infinite;
      inset: 0;
    }
  }

  &__rows {
    display: flex;
    padding: 0;
    margin: 18px 0 0;
    list-style: none;
    flex-direction: column;
    gap: 4px;
  }

  &__row {
    display: flex;
    gap: 10px;
    align-items: center;
    padding: 10px 12px;
    margin: 0 -12px;
    border-radius: 14px;
    transition: background 0.3s ease;

    &:hover {
      background: rgb(0 0 0 / 3.5%);
    }
  }

  &__row-main {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 3px;
    min-width: 0;
  }

  &__row-title {
    overflow: hidden;
    font-size: 13px;
    font-weight: 600;
    color: #1d1d1f;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__row-sub {
    overflow: hidden;
    font-size: 12px;
    color: #86868b;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__row-value {
    flex: none;
    font-size: 12.5px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    color: #6e6e73;
  }

  &__row-arrow {
    flex: none;
    color: #aeaeb2;
  }

  &__secondary {
    margin-top: 14px;
    font-size: 12px;
    color: #86868b;
  }

  &__more {
    padding-top: 18px;
    margin-top: auto;
    font-size: 13.5px;
    font-weight: 600;
    color: var(--primary-color);
  }

  &__overlay {
    position: absolute;
    inset: 0;
    z-index: 2;
    padding: 0;
    cursor: pointer;
    background: transparent;
    border: none;
    border-radius: 30px;

    /* 卡片 overflow: hidden 会裁剪外扩焦点环，改为内缩保证键盘可见 */
    &:focus-visible {
      outline: 3px solid var(--primary-color-light);
      outline-offset: -3px;
    }
  }

  /* 三种语气色：徽章、光斑、警示行 */
  &__card.is-danger {
    .#{$prefix-cls}__badge {
      background: linear-gradient(150deg, #f87171, #dc2626);
    }

    .#{$prefix-cls}__glow {
      background: radial-gradient(circle, rgb(248 113 113 / 60%), transparent 70%);
    }

    .#{$prefix-cls}__warn {
      color: #dc2626;
    }
  }

  &__card.is-warning {
    .#{$prefix-cls}__badge {
      background: linear-gradient(150deg, #fcd34d, #d97706);
    }

    .#{$prefix-cls}__glow {
      background: radial-gradient(circle, rgb(252 211 77 / 60%), transparent 70%);
    }

    .#{$prefix-cls}__warn {
      color: #d97706;
    }
  }

  &__card.is-primary {
    .#{$prefix-cls}__badge {
      background: linear-gradient(150deg, var(--primary-color), var(--primary-color-hover));
    }

    .#{$prefix-cls}__glow {
      background: radial-gradient(circle, rgb(99 102 241 / 45%), transparent 70%);
    }

    .#{$prefix-cls}__warn {
      color: #d97706;
    }
  }
}

/* 窄屏：卡片内边距与大数字收敛，避免一行塞不下 */
@media (width <= 767px) {
  .#{$prefix-cls} {
    :deep(.el-col) {
      margin-bottom: 16px;
    }

    &__card {
      padding: 24px 20px 20px;
      border-radius: 24px;
    }

    &__overlay {
      border-radius: 24px;
    }

    &__value {
      margin-top: 14px;
      font-size: 52px;
    }
  }
}

@media (width >= 768px) {
  .#{$prefix-cls} {
    &__title {
      font-size: 32px;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .#{$prefix-cls} {
    &__card,
    &__sheen {
      transition: none;
    }

    &__card:hover {
      transform: none;
    }

    &__warn-dot::after {
      animation: none;
    }
  }
}

@keyframes wb-cards-ping {
  0% {
    opacity: 0.85;
    transform: scale(1);
  }

  75%,
  100% {
    opacity: 0;
    transform: scale(2.6);
  }
}
</style>
