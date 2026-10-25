<template>
  <!-- 单个系列屏：左文右图（奇数屏翻转），滚动淡入 + 图片轻微视差 -->
  <section
    :id="`series-${index}`"
    ref="panelRef"
    class="relative flex min-h-screen flex-col items-center justify-center gap-10 overflow-hidden px-6 py-24 min-[961px]:flex-row min-[961px]:gap-[6vw] min-[961px]:px-[6vw]"
    :class="[flip ? 'min-[961px]:flex-row-reverse' : '', index % 2 === 0 ? 'bg-[#f5f5f7]' : 'bg-white']"
  >
    <span
      class="pointer-events-none absolute top-[8vh] select-none text-[clamp(120px,18vw,260px)] font-bold leading-none tracking-[-0.04em] text-[rgba(29,29,31,0.045)] max-[960px]:hidden"
      :style="flip ? { left: '2vw' } : { right: '2vw' }"
    >
      {{ no }}
    </span>

    <motion.div
      :initial="{ opacity: 0, y: 44 }"
      :whileInView="{ opacity: 1, y: 0 }"
      :inViewOptions="{ once: true, amount: 0.3 }"
      :transition="{ duration: 0.9, ease: EASE }"
      class="relative z-[2] w-full max-w-[460px] min-[961px]:flex-[0_0_40%]"
    >
      <p class="mb-3.5 text-sm font-semibold tracking-[0.18em] text-[#6e6e73]">
        {{ no }} / {{ product.type || '面料系列' }}{{ product.code ? ` · ${product.code}` : '' }}
      </p>
      <h2 class="text-[clamp(40px,5.6vw,80px)] font-bold leading-[1.07] tracking-[-0.023em]">
        {{ product.name }}
      </h2>
      <p
        v-if="specLine"
        class="mt-4 max-w-[400px] text-[clamp(16px,1.5vw,19px)] text-[#6e6e73]"
      >
        {{ specLine }}
      </p>
      <!-- 静态示例数据无产品详情路由（oldweb 的 /p/:id 不在迁移范围），保留视觉不导航 -->
      <a
        class="mt-[22px] inline-block cursor-pointer text-[clamp(17px,1.5vw,21px)] hover:underline"
        :style="{ color: accent }"
      >
        查看详情 ›
      </a>
    </motion.div>

    <motion.div
      :initial="{ opacity: 0, y: 44, scale: 0.96 }"
      :whileInView="{ opacity: 1, y: 0, scale: 1 }"
      :inViewOptions="{ once: true, amount: 0.3 }"
      :transition="{ duration: 0.9, delay: 0.15, ease: EASE }"
      class="relative h-[52vh] w-full overflow-hidden rounded-[28px] min-[961px]:h-[74vh] min-[961px]:flex-1"
    >
      <motion.img
        v-if="image"
        :src="image"
        :alt="`${product.name}面料特写`"
        class="h-[114%] w-full object-cover will-change-transform"
        :style="{ y: imgY }"
      />
      <div
        v-else
        class="h-full w-full"
        :style="{ background: `linear-gradient(135deg, ${accent} 0%, #1d1d1f 100%)` }"
      ></div>
      <span
        v-if="tag"
        class="absolute bottom-[22px] left-6 z-[3] rounded-full bg-white/85 px-4 py-[7px] text-[12.5px] font-semibold text-[#1d1d1f] backdrop-blur"
      >
        {{ tag }}
      </span>
    </motion.div>
  </section>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { motion, useScroll, useTransform } from 'motion-v'
import { resolvePublicAssetUrl, type PublicProduct } from '@/views/PublicHome/data/products'

const EASE: [number, number, number, number] = [0.28, 0.11, 0.32, 1]

const props = defineProps<{
  product: PublicProduct
  index: number
  accent: string
}>()

const panelRef = ref<HTMLElement | null>(null)

// 图片轻微视差：滚动区间内 -7% → 7%
const { scrollYProgress } = useScroll({
  target: panelRef,
  offset: ['start end', 'end start'],
})
const imgY = useTransform(scrollYProgress, [0, 1], ['-7%', '7%'])

const flip = computed(() => props.index % 2 === 1)
const no = computed(() => String(props.index + 1).padStart(2, '0'))
const image = computed(() => resolvePublicAssetUrl(props.product.coverImageUrl))

const specLine = computed(() =>
  [
    props.product.composition,
    props.product.weightGsm ? `${props.product.weightGsm} GSM` : '',
    props.product.widthCm ? `${props.product.widthCm} CM` : '',
  ]
    .filter(Boolean)
    .join(' · ')
)

const tag = computed(() =>
  [
    props.product.weightGsm ? `${props.product.weightGsm} GSM` : '',
    props.product.widthCm ? `${props.product.widthCm} CM` : '',
  ]
    .filter(Boolean)
    .join(' · ')
)
</script>
