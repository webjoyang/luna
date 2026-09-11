<template>
  <div class="bg-white text-[#1d1d1f]">
    <!-- 右侧进度点 -->
    <nav
      class="fixed right-[26px] top-1/2 z-[150] hidden -translate-y-1/2 flex-col gap-3.5 min-[961px]:flex"
      aria-label="页面进度"
    >
      <a
        v-for="id in dotTargets"
        :key="id"
        :href="`#${id}`"
        :aria-label="id"
        class="h-2 w-2 rounded-full transition-all duration-300"
        :class="activeSection === id
          ? 'scale-150 bg-[#1d1d1f]'
          : 'bg-[rgba(29,29,31,0.25)] hover:bg-[#1d1d1f]'"
      />
    </nav>

    <!-- 首屏 -->
    <section
      id="top"
      class="relative flex min-h-screen flex-col items-center justify-center bg-white px-6 text-center"
    >
      <motion.p
        v-bind="riseBlur(0.15)"
        class="mb-3 text-[clamp(17px,1.8vw,21px)] font-semibold tracking-[-0.01em]"
      >
        {{ brand.name }} · 针织面料
      </motion.p>
      <h1 class="text-[clamp(56px,8.6vw,120px)] font-bold leading-[1.05] tracking-[-0.024em]">
        <motion.span
          v-if="brand.heroTitle"
          v-bind="riseBlur(0.35)"
          class="inline-block whitespace-pre-line"
        >
          {{ brand.heroTitle }}
        </motion.span>
        <template v-else>
          <motion.span v-bind="riseBlur(0.35)" class="inline-block">一屏，</motion.span>
          <br />
          <motion.span v-bind="riseBlur(0.55)" class="inline-block">一种质感。</motion.span>
        </template>
      </h1>
      <motion.p
        v-bind="riseBlur(0.8)"
        class="mt-[18px] max-w-[560px] text-[clamp(17px,1.6vw,21px)] text-[#6e6e73]"
      >
        {{ heroSubtitle }}
      </motion.p>
      <motion.div
        :initial="{ opacity: 0 }"
        :animate="{ opacity: 1 }"
        :transition="{ duration: 1, delay: 1.2 }"
        class="absolute bottom-[34px] left-1/2 -translate-x-1/2"
      >
        <span class="text-xs tracking-[0.28em] text-[#6e6e73]">SCROLL</span>
        <motion.div
          class="mx-auto mt-3 h-10 w-px origin-top bg-[#c7c7cc]"
          :animate="{ scaleY: [0.6, 1, 0.6], opacity: [0.3, 1, 0.3] }"
          :transition="{ duration: 2, repeat: Infinity, ease: 'easeInOut' }"
        />
      </motion.div>
    </section>

    <!-- 大图随滚动放大铺满 -->
    <div ref="zoomRef" class="relative h-[120vh] bg-white min-[961px]:h-[160vh]">
      <div class="sticky top-0 flex h-screen items-center justify-center overflow-hidden">
        <motion.div
          class="relative overflow-hidden will-change-[width,height]"
          :style="{ width: zoomWidth, height: zoomHeight, borderRadius: zoomRadius }"
        >
          <img
            :src="heroVisual"
            :alt="heroImage ? '面料肌理特写' : '品牌针织肌理视觉'"
            class="h-full w-full object-cover"
          />
          <div
            v-if="!heroImage"
            class="absolute inset-0 z-[1] mix-blend-color"
            :style="{ backgroundColor: accent, opacity: 0.2 }"
          />
          <motion.div
            class="absolute inset-0 z-[2] bg-[rgba(0,0,0,0.28)]"
            :style="{ opacity: capOpacity }"
          />
          <motion.div
            class="absolute inset-0 z-[3] flex flex-col items-center justify-center px-6 text-center text-white"
            :style="{ opacity: capOpacity, y: capY }"
          >
            <h2
              class="text-[clamp(40px,5.4vw,76px)] font-bold tracking-[-0.023em]"
              style="text-shadow: 0 2px 24px rgba(0, 0, 0, 0.25)"
            >
              {{ brand.zoomTitle || '肌理，是最好的语言。' }}
            </h2>
            <p class="mt-3 text-[clamp(16px,1.5vw,20px)] text-white/85">
              {{ brand.zoomSubtitle || '从一根纱开始，到一米布结束，全程可溯。' }}
            </p>
          </motion.div>
        </motion.div>
      </div>
    </div>

    <!-- 系列屏：首页推荐产品；无产品时降级为品牌介绍 -->
    <template v-if="products.length > 0">
      <SeriesPanel
        v-for="(product, index) in products"
        :key="product.id"
        :product="product"
        :index="index"
        :accent="accent"
      />
    </template>
    <section
      v-else
      class="flex min-h-[70vh] items-center bg-[#f5f5f7] px-6 py-24 min-[961px]:px-[6vw]"
    >
      <motion.div
        :initial="{ opacity: 0, y: 44 }"
        :whileInView="{ opacity: 1, y: 0 }"
        :inViewOptions="{ once: true, amount: 0.3 }"
        :transition="{ duration: 0.9, ease: EASE }"
        class="max-w-[560px]"
      >
        <p class="mb-3.5 text-sm font-semibold tracking-[0.18em] text-[#6e6e73]">品牌介绍</p>
        <h2 class="text-[clamp(40px,5.6vw,80px)] font-bold leading-[1.07] tracking-[-0.023em]">
          {{ brand.name }}
        </h2>
        <p class="mt-4 text-[clamp(16px,1.5vw,19px)] leading-relaxed text-[#6e6e73]">
          {{ brand.description }}
        </p>
      </motion.div>
    </section>

    <!-- 黑底收尾 CTA -->
    <section
      id="cta"
      class="relative flex min-h-screen flex-col items-center justify-center overflow-hidden bg-black px-6 py-24 text-center text-[#f5f5f7]"
    >
      <div class="absolute inset-0 z-0">
        <img
          :src="heroVisual"
          alt=""
          class="h-[110%] w-full object-cover brightness-[0.45] grayscale"
        />
        <div class="absolute inset-0 bg-black/55" />
      </div>
      <motion.h2
        :initial="{ opacity: 0, y: 44 }"
        :whileInView="{ opacity: 1, y: 0 }"
        :inViewOptions="{ once: true, amount: 0.4 }"
        :transition="{ duration: 0.9, ease: EASE }"
        class="relative z-[2] text-[clamp(44px,6.4vw,88px)] font-bold leading-[1.07] tracking-[-0.023em]"
      >
        <span v-if="brand.ctaTitle" class="whitespace-pre-line">{{ brand.ctaTitle }}</span>
        <template v-else>
          看千遍图，<br />不如摸一次。
        </template>
      </motion.h2>
      <motion.p
        :initial="{ opacity: 0, y: 30 }"
        :whileInView="{ opacity: 1, y: 0 }"
        :inViewOptions="{ once: true, amount: 0.4 }"
        :transition="{ duration: 0.9, delay: 0.1, ease: EASE }"
        class="relative z-[2] mt-4 text-[clamp(17px,1.6vw,21px)] text-[#86868b]"
      >
        {{ brand.ctaSubtitle || '色卡与剪版免费寄送，工作日当日发出。' }}
      </motion.p>
      <motion.div
        :initial="{ opacity: 0, y: 30 }"
        :whileInView="{ opacity: 1, y: 0 }"
        :inViewOptions="{ once: true, amount: 0.4 }"
        :transition="{ duration: 0.9, delay: 0.2, ease: EASE }"
        class="relative z-[2] mt-[30px] flex items-center gap-[26px]"
      >
        <a
          :href="mailto"
          class="rounded-full bg-white px-[30px] py-3.5 text-[17px] font-semibold text-black transition hover:bg-[#e8e8ed]"
        >
          预约看样
        </a>
        <a
          href="#library"
          class="text-[clamp(19px,1.8vw,24px)]"
          :style="{ color: accent }"
        >
          探索全部面料 ›
        </a>
      </motion.div>
    </section>
  </div>
</template>

<script lang="ts" setup>
/**
 * 首页滚动叙事区：首屏大字模糊浮现 → 面料大图滚动放大铺满 →
 * 推荐产品逐屏展示（无产品时降级为品牌介绍）→ 黑底收尾 CTA。
 *
 * TODO: 接入真实数据后恢复 /api/public/products?featured=1&pageSize=4 拉取
 * （0 款回退公开产品前 4 款），当前使用内置静态示例数据。
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { motion, useScroll, useTransform } from 'motion-v'
import type { BrandConfig } from '@/views/PublicHome/config/brand'
import { FEATURED_PRODUCTS, resolvePublicAssetUrl, type PublicProduct } from '@/views/PublicHome/data/products'
import SeriesPanel from './SeriesPanel.vue'

const EASE: [number, number, number, number] = [0.28, 0.11, 0.32, 1]
const FALLBACK_FABRIC_IMAGE = '/presentation/assets/fabric.png'

/** 入场模糊浮现动画的 props 集合（模板中 v-bind 展开） */
function riseBlur(delay: number) {
  return {
    initial: { opacity: 0, y: 34, filter: 'blur(10px)' },
    animate: { opacity: 1, y: 0, filter: 'blur(0px)' },
    transition: { duration: 0.9, delay, ease: EASE },
  }
}

const props = defineProps<{ brand: BrandConfig }>()

const accent = computed(() => props.brand.primaryColor || '#0066cc')
const products = ref<PublicProduct[]>(FEATURED_PRODUCTS)

const heroSubtitle = computed(
  () =>
    props.brand.heroSubtitle ||
    `${props.brand.slogan ? `${props.brand.slogan}。` : ''}向下滚动，用手指的阅读速度，看完每一款面料。`
)

const dotTargets = computed(() => ['top', ...products.value.map((_, i) => `series-${i}`), 'cta'])

// 右侧进度点：跟踪当前可视 section
const activeSection = ref('top')
let observer: IntersectionObserver | null = null

const observeSections = () => {
  observer?.disconnect()
  observer = new IntersectionObserver(
    (entries) =>
      entries.forEach((entry) => {
        if (entry.isIntersecting) activeSection.value = entry.target.id
      }),
    { threshold: 0.5 }
  )
  dotTargets.value.forEach((id) => {
    const el = document.getElementById(id)
    if (el) observer!.observe(el)
  })
}

onMounted(observeSections)
watch(products, observeSections)
onBeforeUnmount(() => observer?.disconnect())

// 大图滚动放大铺满
const zoomRef = ref<HTMLDivElement | null>(null)
const { scrollYProgress } = useScroll({ target: zoomRef, offset: ['start start', 'end end'] })
const zoomWidth = useTransform(scrollYProgress, [0, 0.72], ['58%', '100%'])
const zoomHeight = useTransform(scrollYProgress, [0, 0.72], ['74vh', '100vh'])
const zoomRadius = useTransform(scrollYProgress, [0, 0.72], [40, 0])
const capOpacity = useTransform(scrollYProgress, [0.68, 0.9], [0, 1])
const capY = useTransform(scrollYProgress, [0.68, 0.9], [26, 0])

const heroImage = computed(() => resolvePublicAssetUrl(products.value[0]?.coverImageUrl))
const heroVisual = computed(() => heroImage.value || FALLBACK_FABRIC_IMAGE)
const mailto = `mailto:?subject=${encodeURIComponent(`${props.brand.name} · 预约看样`)}`
</script>
