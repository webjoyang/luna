<template>
  <!-- 吸顶毛玻璃导航：左 logo（无 logo 时显示英文名）、中间锚点菜单、右员工登录 -->
  <header
    class="fixed inset-x-0 top-0 z-[200] h-12 bg-[rgba(251,251,253,0.8)] backdrop-blur-xl backdrop-saturate-[180%] transition-shadow duration-300"
    :class="scrolled ? 'shadow-[0_1px_0_rgba(0,0,0,0.06)]' : ''"
  >
    <div class="mx-auto flex h-full max-w-[1200px] items-center justify-between px-[22px]">
      <RouterLink to="/" class="flex items-center" :aria-label="brand.name">
        <img
          v-if="brand.logoUrl && !logoError"
          :src="brand.logoUrl"
          :alt="brand.name"
          class="h-[17px] w-auto object-contain"
          @error="logoError = true"
        />
        <span v-else class="text-sm font-semibold tracking-[0.2em] text-[#1d1d1f]">
          {{ brand.nameEn || brand.name }}
        </span>
      </RouterLink>

      <nav class="hidden items-center gap-11 min-[961px]:flex">
        <a
          v-for="item in MENU_ITEMS"
          :key="item.href"
          :href="item.href"
          class="text-xs text-[rgba(29,29,31,0.82)] transition-colors hover:text-black"
        >
          {{ item.label }}
        </a>
      </nav>

      <RouterLink
        to="/luna-login"
        class="text-xs text-[rgba(29,29,31,0.55)] transition-colors hover:text-black"
      >
        员工登录
      </RouterLink>
    </div>
  </header>
</template>

<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import type { BrandConfig } from '@/views/PublicHome/config/brand'

defineProps<{ brand: BrandConfig }>()

const MENU_ITEMS = [
  { label: '首页', href: '#top' },
  { label: '产品系列', href: '#library' },
  { label: '联系', href: '#contact' },
]

const scrolled = ref(false)
const logoError = ref(false)

const onScroll = () => {
  scrolled.value = window.scrollY > 8
}

onMounted(() => {
  // 锚点平滑滚动（离开页面时清除，避免污染后台页面的滚动行为）
  document.documentElement.style.scrollBehavior = 'smooth'
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  document.documentElement.style.scrollBehavior = ''
  window.removeEventListener('scroll', onScroll)
})
</script>
