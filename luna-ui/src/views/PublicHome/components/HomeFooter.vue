<template>
  <!-- 官网页脚：品牌全称 + 版权 + 备案占位（id="contact" 供导航锚点跳转） -->
  <footer id="contact" class="bg-[#f5f5f7] px-[22px] pb-6 pt-8 text-xs text-[#6e6e73]">
    <div class="mx-auto flex max-w-[1024px] flex-wrap items-center justify-between gap-2.5">
      <span>
        {{ brand.fullName || brand.name }}{{ brand.footer ? ` · ${brand.footer}` : '' }}
      </span>
      <span class="flex flex-wrap items-center gap-4">
        <span class="cursor-pointer transition hover:text-[#1d1d1f]">隐私政策</span>
        <span class="cursor-pointer transition hover:text-[#1d1d1f]">使用条款</span>
        <a
          href="https://beian.miit.gov.cn/"
          target="_blank"
          rel="noreferrer"
          class="transition hover:text-[#1d1d1f]"
        >
          浙ICP备XXXXXXXX号
        </a>
      </span>
    </div>
  </footer>
</template>

<script lang="ts" setup>
import type { BrandConfig } from '@/views/PublicHome/config/brand'

defineProps<{ brand: BrandConfig }>()
</script>
