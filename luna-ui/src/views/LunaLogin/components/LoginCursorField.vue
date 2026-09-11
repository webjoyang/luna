<template>
  <!-- LoginCursorField — 桌面端鼠标青白光晕 + 拖尾互动层（Canvas）。纯 LUNA 自有实现。 -->
  <canvas
    ref="canvasRef"
    class="pointer-events-none absolute inset-0 h-full w-full"
    :style="{ mixBlendMode: 'overlay' }"
    aria-hidden="true"
  />
</template>

<script lang="ts" setup>
/**
 * 迁移自 oldweb/components/auth/LoginCursorField.tsx
 *
 * 对标 QQ 首屏交互层的 cursor.glow / cursor.trail 量级（青白光、~280px 响应半径、白色拖尾），
 * 全部本地 Canvas 绘制。mix-blend-mode: screen 让光晕在深色背景上做加色发光。
 * 仅在 pointer:fine 且非 reduced-motion 时启用；否则不渲染任何动画。
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)

let cleanup: (() => void) | null = null

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const fine = window.matchMedia('(pointer: fine)').matches
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (!fine || reduce) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  let w = 0
  let h = 0
  let dpr = 1
  const resize = () => {
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    w = window.innerWidth
    h = window.innerHeight
    canvas.width = w * dpr
    canvas.height = h * dpr
    canvas.style.width = `${w}px`
    canvas.style.height = `${h}px`
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
  resize()
  window.addEventListener('resize', resize)

  // 拖尾点：最近若干个位置，按生命周期淡出
  const trail: { x: number; y: number; life: number }[] = []
  let mx = w / 2
  let my = h * 0.4
  let hasPointer = false
  let raf = 0

  const onMove = (e: MouseEvent) => {
    mx = e.clientX
    my = e.clientY
    hasPointer = true
    trail.push({ x: mx, y: my, life: 1 })
    if (trail.length > 22) trail.shift()
  }
  window.addEventListener('mousemove', onMove, { passive: true })

  const draw = () => {
    ctx.clearRect(0, 0, w, h)
    if (hasPointer) {
      // 拖尾（白色，逐点变细变淡）
      for (let i = 0; i < trail.length; i++) {
        const p = trail[i]
        p.life *= 0.9
        const r = 26 + i * 2.2
        const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, r)
        g.addColorStop(0, `rgba(255,255,255,${0.16 * p.life})`)
        g.addColorStop(1, 'rgba(255,255,255,0)')
        ctx.fillStyle = g
        ctx.beginPath()
        ctx.arc(p.x, p.y, r, 0, Math.PI * 2)
        ctx.fill()
      }
      // 主光晕（青白，~280px 半径）
      const R = 280
      const glow = ctx.createRadialGradient(mx, my, 0, mx, my, R)
      glow.addColorStop(0, 'rgba(255,255,255,0.42)')
      glow.addColorStop(0.32, 'rgba(150,235,255,0.22)')
      glow.addColorStop(1, 'rgba(136,251,255,0)')
      ctx.fillStyle = glow
      ctx.beginPath()
      ctx.arc(mx, my, R, 0, Math.PI * 2)
      ctx.fill()
    }
    raf = requestAnimationFrame(draw)
  }
  raf = requestAnimationFrame(draw)

  cleanup = () => {
    window.removeEventListener('resize', resize)
    window.removeEventListener('mousemove', onMove)
    cancelAnimationFrame(raf)
  }
})

onBeforeUnmount(() => {
  cleanup?.()
  cleanup = null
})
</script>
