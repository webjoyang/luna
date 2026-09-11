<template>
  <!--
    LoginHero — 首屏主视觉：LUNA 流体光球悬浮于大号磨砂白标语「让智能 接管工作」上方。
    光球下方眉标显示客户企业官方 LOGO；若客户未上传自有 LOGO，则回退为公司英文简称。
    纯展示组件，对应 oldweb/components/auth/LoginHero.tsx。
  -->
  <div class="relative mb-9 flex flex-col items-center text-center sm:mb-11">
    <!-- LUNAAGI 行为光球 —— hero 视觉核心，负 margin 让标语微微钻进光晕 -->
    <div class="relative -mb-4 sm:-mb-6" aria-hidden="true">
      <ThinkingOrb state="breathing" :size="150" theme="dark" />
    </div>

    <!-- 品牌眉标：客户 LOGO 或公司英文简称 -->
    <span
      class="login-pill mb-6 inline-flex items-center gap-1.5 rounded-full border border-white/30 bg-white/15 px-4 py-1.5 shadow-sm backdrop-blur drop-shadow-[0_1px_6px_rgba(0,0,0,0.2)]"
    >
      <img
        v-if="showCustomerLogo && logoUrl"
        :src="logoUrl"
        :alt="brandNameEn"
        class="h-5 w-auto object-contain sm:h-6"
      />
      <span v-else class="text-[11px] font-semibold uppercase tracking-[0.3em] text-white/90">
        {{ brandNameEn }}
      </span>
    </span>

    <!-- 标题：上层实色 + 下层模糊呼吸光（双层叠加） -->
    <div class="relative">
      <h1 aria-hidden="true" class="login-title login-title--glow absolute inset-0 select-none">
        <span class="block text-[3rem] font-semibold leading-[1.05] tracking-tight sm:text-[5.5rem]">
          让智能 接管工作
        </span>
      </h1>
      <h1 class="login-title login-title--above relative">
        <span class="block text-[3rem] font-semibold leading-[1.05] tracking-tight sm:text-[5.5rem]">
          让智能 接管工作
        </span>
      </h1>
    </div>

    <!-- 副标题 -->
    <p
      class="mt-6 max-w-md text-sm font-medium leading-relaxed text-white/80 sm:text-base drop-shadow-[0_1px_6px_rgba(80,90,160,0.35)]"
    >
      让 AI 驱动每一天的业务协作
    </p>
  </div>
</template>

<script lang="ts" setup>
/**
 * 迁移自 oldweb/components/auth/LoginHero.tsx
 * 纯展示组件：光球 + 品牌眉标 + 双层标题（呼吸光晕）+ 副标题。
 * 光球改用 thinking-orb-vue（已 vendored 到 src/components/thinking-orb）。
 */
import { computed } from 'vue'
import { ThinkingOrb } from '@/components/thinking-orb'

const props = defineProps<{
  logoUrl?: string
  brandNameEn: string
}>()

// LUNA 默认字标视为「未上传客户 LOGO」→ 用英文简称
const isGenericLuna = computed(() => {
  const url = props.logoUrl || ''
  return (
    !url ||
    /\/logo(_white)?\.svg(\?|$)/.test(url) ||
    /custom\/lunaagi\/logo/.test(url)
  )
})
const showCustomerLogo = computed(() => !isGenericLuna.value)
</script>

<style lang="scss" scoped>
.login-title {
  margin: 0;
}

.login-title--above {
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 2px 22px rgba(90, 110, 180, 0.4);
}

.login-title--glow {
  color: #ffffff;
  filter: blur(13px);
  animation: login-title-breathe 2.6s ease-in-out infinite;
  will-change: opacity, transform;
}

@keyframes login-title-breathe {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(1);
  }
  50% {
    opacity: 0.85;
    transform: scale(1.025);
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-title--glow {
    animation: none !important;
    opacity: 0.6;
  }
}
</style>
