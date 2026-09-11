<template>
  <!-- LoginLogo — LUNA 平台官方字标（白色版 logo_white.svg），略放大，带柔和发光脉动。纯展示。 -->
  <span class="login-logo inline-flex items-center">
    <img :src="src" alt="lunaagi" class="login-logo__img" />
  </span>
</template>

<script lang="ts" setup>
/**
 * 迁移自 oldweb/components/auth/LoginLogo.tsx
 * 纯展示组件：白色字标 + logo-glow 关键帧脉动 + prefers-reduced-motion 降级。
 */
withDefaults(defineProps<{ src?: string }>(), {
  src: '/logo_white.svg'
})
</script>

<style lang="scss" scoped>
.login-logo__img {
  height: 2.75rem;
  width: auto;
  object-fit: contain;
  animation: login-logo-glow 3.2s ease-in-out infinite;
}

@media (min-width: 640px) {
  .login-logo__img {
    height: 3.1rem;
  }
}

@keyframes login-logo-glow {
  0%,
  100% {
    filter: drop-shadow(0 2px 10px rgba(90, 110, 200, 0.3));
  }
  50% {
    filter: drop-shadow(0 2px 20px rgba(140, 130, 245, 0.65));
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-logo__img {
    animation: none !important;
    filter: drop-shadow(0 2px 10px rgba(0, 0, 0, 0.25));
  }
}
</style>
