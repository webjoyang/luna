<template>
  <!--
    LoginAura — LUNA 登录页签名背景：固定竖向光条，色波横向流动
    （蓝 → 青 → 亮薄荷 → 蓝 → 靛 → 紫 → 品红 循环）；亮薄荷横向扫过。
    叠加极光柔光 + Canvas 鼠标青白光晕。尊重 prefers-reduced-motion；纯装饰。
  -->
  <div
    ref="rootRef"
    class="login-aura pointer-events-none absolute inset-0 overflow-hidden bg-[#2f63cf]"
    :data-reduce="dataReduce ? 'true' : null"
    aria-hidden="true"
  >
    <div class="absolute inset-0 flex">
      <div
        v-for="(col, idx) in auraCols"
        :key="idx"
        class="aura-col h-full flex-1"
        :style="{ animationDelay: col.delay }"
      ></div>
    </div>

    <!-- 极光柔光：缓慢漂移的青/紫/薄荷光幕 -->
    <div class="aura-glow aura-glow--cyan absolute inset-0 mix-blend-screen"></div>
    <div class="aura-glow aura-glow--violet absolute inset-0 mix-blend-screen"></div>
    <div class="aura-glow aura-glow--mint absolute inset-0 mix-blend-screen"></div>

    <!-- 底部渐隐为暖白/粉橙 -->
    <div
      class="absolute inset-x-0 bottom-0 h-[58%]"
      style="background: linear-gradient(180deg, transparent 0%, rgba(255, 231, 224, 0.42) 55%, rgba(255, 236, 228, 0.66) 100%)"
    ></div>
    <!-- 顶部轻微压暗，承托白色字标 -->
    <div class="absolute inset-x-0 top-0 h-28 bg-gradient-to-b from-black/15 to-transparent"></div>

    <!-- 鼠标青白光晕 + 拖尾 -->
    <LoginCursorField />
  </div>
</template>

<script lang="ts" setup>
/**
 * 迁移自 oldweb/components/auth/LoginAura.tsx
 * 纯 CSS 程序化生成，不复制 QQ 图片/视频/源码。
 */
import { computed, onMounted, ref } from 'vue'
import LoginCursorField from './LoginCursorField.vue'

const N = 46
const DURATION = 10 // 一整轮色波循环秒数

const dataReduce = ref(false)

// 预生成每列的负延时（让色波按列展开横向平移）
const auraCols = computed(() =>
  Array.from({ length: N }, (_, i) => ({
    delay: `-${((i / N) * DURATION).toFixed(2)}s`,
  })),
)

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    dataReduce.value = true
  }
})
</script>

<style lang="scss" scoped>
.aura-col {
  box-shadow: inset 1px 0 0 rgba(255, 255, 255, 0.06), inset -1px 0 0 rgba(8, 16, 50, 0.07);
  background-color: #2f63cf;
  animation-name: aura-flow;
  animation-duration: 10s;
  animation-timing-function: linear;
  animation-iteration-count: infinite;
  will-change: background-color;
}

@keyframes aura-flow {
  0% {
    background-color: #2f63cf;
  }
  9% {
    background-color: #2e87d4;
  }
  18% {
    background-color: #1eb6cf;
  }
  26% {
    background-color: #5fd0d8;
  }
  32% {
    background-color: #aeece2;
  }
  39% {
    background-color: #4fc2cf;
  }
  50% {
    background-color: #3a64d8;
  }
  60% {
    background-color: #5147c8;
  }
  70% {
    background-color: #6f4ad2;
  }
  80% {
    background-color: #9150d4;
  }
  88% {
    background-color: #b265cf;
  }
  94% {
    background-color: #7a52cc;
  }
  100% {
    background-color: #2f63cf;
  }
}

.aura-glow {
  filter: blur(48px);
  will-change: transform, opacity;
}

.aura-glow--cyan {
  background: radial-gradient(42% 72% at 34% 28%, rgba(120, 240, 225, 0.5), transparent 70%);
  animation: aurora-a 15s ease-in-out infinite;
}

.aura-glow--violet {
  background: radial-gradient(40% 66% at 72% 46%, rgba(168, 122, 240, 0.46), transparent 72%);
  animation: aurora-b 19s ease-in-out infinite;
}

.aura-glow--mint {
  background: radial-gradient(34% 60% at 54% 60%, rgba(150, 235, 220, 0.4), transparent 74%);
  animation: aurora-c 23s ease-in-out infinite;
}

@keyframes aurora-a {
  0%,
  100% {
    transform: translate(-5%, -3%) scale(1);
    opacity: 0.85;
  }
  50% {
    transform: translate(7%, 4%) scale(1.12);
    opacity: 1;
  }
}

@keyframes aurora-b {
  0%,
  100% {
    transform: translate(5%, 2%) scale(1.06);
    opacity: 0.8;
  }
  50% {
    transform: translate(-6%, -4%) scale(1);
    opacity: 1;
  }
}

@keyframes aurora-c {
  0%,
  100% {
    transform: translate(0, 5%) scale(1);
    opacity: 0.65;
  }
  50% {
    transform: translate(-4%, -5%) scale(1.1);
    opacity: 0.95;
  }
}

.login-aura[data-reduce='true'] .aura-col,
.login-aura[data-reduce='true'] .aura-glow {
  animation: none !important;
}

@media (prefers-reduced-motion: reduce) {
  .aura-col,
  .aura-glow {
    animation: none !important;
  }
}
</style>
