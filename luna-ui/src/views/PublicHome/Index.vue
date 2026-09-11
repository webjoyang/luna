<template>
  <!-- 多租户官网首页：吸顶导航 + 滚动叙事 + 面料库 + 页脚 -->
  <div class="public-home min-h-screen bg-white text-[#1d1d1f] antialiased">
    <HomeNav :brand="brand" />
    <main>
      <HomeHero :brand="brand" />
      <HomeProducts :brand="brand" />
    </main>
    <HomeFooter :brand="brand" />
  </div>
</template>

<script lang="ts" setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { useBrand } from '@/views/PublicHome/hooks/useBrand'
import HomeNav from './components/HomeNav.vue'
import HomeHero from './components/HomeHero.vue'
import HomeProducts from './components/HomeProducts.vue'
import HomeFooter from './components/HomeFooter.vue'

defineOptions({ name: 'PublicHome' })

const { brand } = useBrand()

// 后台全局样式（App.vue）将 html/body 设为 height:100% + overflow:hidden（内部容器滚动），
// 官网首页依赖文档级滚动（useScroll / sticky / window.scrollY），挂载期间临时还原为普通文档流。
let savedHtmlOverflow = ''
let savedBodyOverflow = ''
let savedHtmlHeight = ''
let savedBodyHeight = ''

onMounted(() => {
  const html = document.documentElement
  const body = document.body
  savedHtmlOverflow = html.style.overflow
  savedBodyOverflow = body.style.overflow
  savedHtmlHeight = html.style.height
  savedBodyHeight = body.style.height
  html.style.overflow = 'visible'
  body.style.overflow = 'visible'
  html.style.height = 'auto'
  body.style.height = 'auto'
})

onBeforeUnmount(() => {
  const html = document.documentElement
  const body = document.body
  html.style.overflow = savedHtmlOverflow
  body.style.overflow = savedBodyOverflow
  html.style.height = savedHtmlHeight
  body.style.height = savedBodyHeight
})
</script>

<style scoped>
/* 对齐 React 版 Tailwind font-sans 默认字体栈，保证视觉一致 */
.public-home {
  font-family:
    ui-sans-serif,
    system-ui,
    -apple-system,
    'Segoe UI',
    Roboto,
    'Helvetica Neue',
    Arial,
    'PingFang SC',
    'Microsoft YaHei',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>
