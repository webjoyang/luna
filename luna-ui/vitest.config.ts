import { resolve } from 'path'
import { defineConfig } from 'vitest/config'

// 独立的 vitest 配置：不复用 vite.config.ts（避免加载构建插件与 env），仅运行纯函数单元测试
export default defineConfig({
  resolve: {
    alias: [
      {
        find: /\@\//,
        replacement: `${resolve(__dirname, 'src')}/`
      }
    ]
  },
  test: {
    environment: 'node',
    include: ['src/**/*.{spec,test}.ts']
  }
})
