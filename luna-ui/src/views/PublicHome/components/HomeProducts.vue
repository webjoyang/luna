<template>
  <!-- 面料库区（id="library"）：标题 + 总数副标题 + 防抖搜索 + 分类 chips + 产品网格 + 加载更多 -->
  <section id="library" class="bg-white pb-24 pt-20 min-[961px]:pt-28">
    <div class="mx-auto max-w-[1200px] px-[22px]">
      <header class="text-center">
        <h2 class="text-[clamp(36px,5vw,64px)] font-bold tracking-[-0.023em] text-[#1d1d1f]">
          面料库
        </h2>
        <p class="mt-3 text-[clamp(15px,1.5vw,19px)] text-[#6e6e73]">
          {{ loading && products.length === 0
            ? '正在整理面料档案…'
            : `共 ${total} 款面料，每一款都有高清实拍` }}
        </p>
        <form class="relative mx-auto mt-8 max-w-[620px]" @submit.prevent="onSubmitSearch">
          <svg
            class="absolute left-4 top-1/2 h-[17px] w-[17px] -translate-y-1/2 text-[#6e6e73]"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.3-4.3" />
          </svg>
          <input
            v-model="searchInput"
            type="text"
            placeholder="搜索编号、名称或成分"
            autocomplete="off"
            class="w-full rounded-xl bg-white py-3.5 pl-11 pr-4 text-base text-[#1d1d1f] outline-none transition placeholder:text-[#86868b]"
            :style="{ boxShadow: searchFocused ? `0 0 0 2px ${accent}` : '0 0 0 1px #d2d2d7' }"
            @focus="searchFocused = true"
            @blur="searchFocused = false"
          />
        </form>
      </header>

      <div v-if="categories.length > 0" class="chips-scroll mt-8 flex items-center gap-2 overflow-x-auto pb-1">
        <button :class="chipClass(activeCategory === '')" @click="activeCategory = ''">全部</button>
        <button
          v-for="c in categories"
          :key="c"
          :class="chipClass(activeCategory === c)"
          @click="activeCategory = c"
        >
          {{ c }}
        </button>
      </div>

      <!-- 加载骨架屏 -->
      <div
        v-if="loading"
        class="mt-10 grid grid-cols-2 gap-3 min-[721px]:grid-cols-3 min-[961px]:grid-cols-4 min-[961px]:gap-[18px]"
      >
        <div
          v-for="i in 8"
          :key="i"
          class="animate-pulse rounded-[20px] bg-[#f5f5f7] p-[14px] pb-[18px]"
        >
          <div class="aspect-square rounded-xl bg-[#e8e8ed]"></div>
          <div class="mx-1.5 mt-3.5 h-4 w-2/3 rounded bg-[#e8e8ed]"></div>
          <div class="mx-1.5 mt-2 h-3 w-1/2 rounded bg-[#e8e8ed]"></div>
        </div>
      </div>

      <!-- 搜索/筛选无结果 -->
      <div v-else-if="products.length === 0 && (searchQuery || activeCategory)" class="py-20 text-center">
        <p class="text-[15px] text-[#6e6e73]">没有找到匹配的面料</p>
        <button
          class="mt-4 text-[15px] font-semibold hover:underline"
          :style="{ color: accent }"
          @click="resetFilters"
        >
          重置筛选条件
        </button>
      </div>

      <!-- 空态品牌视觉展示 -->
      <div v-else-if="products.length === 0" class="mt-10">
        <div class="grid gap-4 min-[721px]:grid-cols-3">
          <article
            v-for="(item, index) in EMPTY_SHOWCASE"
            :key="item.title"
            class="group relative aspect-[4/5] overflow-hidden rounded-[24px] bg-[#1d1d1f]"
          >
            <img
              :src="FALLBACK_FABRIC_IMAGE"
              alt=""
              class="h-full w-full scale-110 object-cover grayscale transition duration-700 group-hover:scale-[1.16]"
              :style="{
                objectPosition: item.position,
                filter: `grayscale(1) contrast(${1.02 + index * 0.08}) brightness(${0.78 + index * 0.08})`,
              }"
            />
            <div
              class="absolute inset-0"
              :style="{
                background: `linear-gradient(180deg, transparent 32%, ${accent}22 68%, rgba(0,0,0,0.78) 100%)`,
              }"
            ></div>
            <div class="absolute inset-x-0 bottom-0 p-6 text-white">
              <p class="text-[11px] font-semibold tracking-[0.2em] text-white/65">
                BRAND STUDY 0{{ index + 1 }}
              </p>
              <h3 class="mt-2 text-[clamp(24px,2.7vw,36px)] font-semibold tracking-[-0.02em]">
                {{ item.title }}
              </h3>
              <p class="mt-1 text-sm text-white/70">{{ item.subtitle }}</p>
            </div>
          </article>
        </div>
        <div class="mx-auto mt-8 max-w-[620px] rounded-2xl bg-[#f5f5f7] px-6 py-5 text-center">
          <p class="text-[15px] font-semibold text-[#1d1d1f]">产品影像正在整理中</p>
          <p class="mt-1 text-[13px] leading-relaxed text-[#6e6e73]">
            这里只展示品牌视觉，不计入产品数量。真实产品上传照片并开启“上架官网”后将自动替换。
          </p>
        </div>
      </div>

      <!-- 产品网格：桌面 4 列/平板 3/手机 2 -->
      <div
        v-else
        class="mt-10 grid grid-cols-2 gap-3 min-[721px]:grid-cols-3 min-[961px]:grid-cols-4 min-[961px]:gap-[18px]"
      >
        <RouterLink
          v-for="p in products"
          :key="p.id"
          to="/"
          class="group block rounded-[20px] bg-[#f5f5f7] p-[14px] pb-[18px] transition duration-300 hover:-translate-y-1.5 hover:shadow-[0_18px_40px_rgba(0,0,0,0.12)]"
          @click.prevent
        >
          <div class="relative aspect-square overflow-hidden rounded-xl bg-[#e8e8ed]">
            <img
              v-if="p.coverImageUrl"
              :src="resolvePublicAssetUrl(p.coverImageUrl)"
              :alt="p.name"
              loading="lazy"
              class="h-full w-full object-cover transition duration-500 group-hover:scale-[1.07]"
            />
            <div
              v-else
              class="h-full w-full"
              :style="{ background: `linear-gradient(135deg, ${accent} 0%, #1d1d1f 100%)` }"
            ></div>
          </div>
          <div class="px-1.5 pt-3.5">
            <p class="flex items-baseline justify-between gap-2 text-[15.5px] font-semibold tracking-[-0.01em] text-[#1d1d1f]">
              <span class="line-clamp-1">{{ p.name }}</span>
              <span class="flex-none text-[11.5px] font-normal tracking-[0.04em] text-[#6e6e73]">
                {{ p.code }}
              </span>
            </p>
            <p class="mt-1 line-clamp-1 text-[12.5px] text-[#6e6e73]">
              {{ specText(p) }}
            </p>
          </div>
        </RouterLink>
      </div>

      <!-- 加载更多 -->
      <div v-if="!loading && products.length > 0" class="mt-10 text-center">
        <button
          v-if="hasMore"
          :disabled="loadingMore"
          class="rounded-full border-[1.5px] border-[#1d1d1f] bg-white px-8 py-[11px] text-[15px] font-semibold text-[#1d1d1f] transition hover:bg-[#1d1d1f] hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
          @click="fetchProducts(page + 1, searchQuery, activeCategory)"
        >
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
        <p class="mt-3.5 text-[12.5px] text-[#6e6e73]">已显示 {{ products.length }} / {{ total }}</p>
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
/**
 * 面料库区：搜索 + 分类 + 产品网格 + 加载更多。
 *
 * TODO: 当前对内置静态数据做内存过滤 + 本地分页（结构与原 HTTP 版一致），
 * 后续接入 /api/public/products、/api/public/categories 时仅替换 fetchProducts
 * 与 categories 加载逻辑即可。
 */
import { computed, ref, watch } from 'vue'
import type { BrandConfig } from '@/views/PublicHome/config/brand'
import { ALL_PRODUCTS, CATEGORIES, resolvePublicAssetUrl, type PublicProduct } from '@/views/PublicHome/data/products'

const PAGE_SIZE = 30
const FALLBACK_FABRIC_IMAGE = '/presentation/assets/fabric.png'
const EMPTY_SHOWCASE = [
  { title: '触感档案', subtitle: '针织肌理 · 品牌视觉', position: 'center 28%' },
  { title: '纱线语言', subtitle: '组织结构 · 细节表达', position: 'center 55%' },
  { title: '数字面料', subtitle: '真实产品影像即将发布', position: 'center 82%' },
]

const props = defineProps<{ brand: BrandConfig }>()

const accent = computed(() => props.brand.primaryColor || '#0066cc')

const products = ref<PublicProduct[]>([])
const categories = ref<string[]>(CATEGORIES)
const activeCategory = ref('')
const searchInput = ref('')
const searchQuery = ref('')
const searchFocused = ref(false)
const page = ref(1)
const total = ref(0)
const loading = ref(true)
const loadingMore = ref(false)

// 搜索防抖（回车可立即触发，见表单 onSubmit）
watch(searchInput, (val, _old, onCleanup) => {
  const timer = setTimeout(() => {
    searchQuery.value = val.trim()
  }, 350)
  onCleanup(() => clearTimeout(timer))
})

/** 内存过滤 + 本地分页（替代原 HTTP 请求，结构对齐以便后续无缝替换） */
const fetchProducts = async (pageNum: number, query: string, category: string) => {
  if (pageNum === 1) loading.value = true
  else loadingMore.value = true
  try {
    let result = ALL_PRODUCTS
    if (query) {
      const q = query.toLowerCase()
      result = result.filter(
        (p) =>
          p.code.toLowerCase().includes(q) ||
          p.name.toLowerCase().includes(q) ||
          p.composition.toLowerCase().includes(q)
      )
    }
    if (category) {
      result = result.filter((p) => p.type === category)
    }
    const items = result.slice((pageNum - 1) * PAGE_SIZE, pageNum * PAGE_SIZE)
    products.value = pageNum === 1 ? items : [...products.value, ...items]
    total.value = result.length
    page.value = pageNum
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

fetchProducts(1, searchQuery.value, activeCategory.value)

watch([searchQuery, activeCategory], ([query, category]) => {
  fetchProducts(1, query, category)
})

const onSubmitSearch = () => {
  searchQuery.value = searchInput.value.trim()
}

const hasMore = computed(() => products.value.length < total.value)
const resetFilters = () => {
  searchInput.value = ''
  searchQuery.value = ''
  activeCategory.value = ''
}

const chipClass = (active: boolean) =>
  `flex-none rounded-full px-3.5 py-1.5 text-[13px] transition ${
    active ? 'bg-[#1d1d1f] text-white' : 'bg-[#f5f5f7] text-[#1d1d1f] hover:bg-[#e8e8ed]'
  }`

const specText = (p: PublicProduct) =>
  [
    p.composition,
    p.weightGsm ? `${p.weightGsm} GSM` : '',
    p.widthCm ? `${p.widthCm} CM` : '',
  ]
    .filter(Boolean)
    .join(' · ') || '—'
</script>

<style scoped>
/* 分类 chips 横向滚动时隐藏滚动条（UnoCSS arbitrary property 语法不保证支持，手写兜底） */
.chips-scroll {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.chips-scroll::-webkit-scrollbar {
  display: none;
}
</style>
