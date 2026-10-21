<template>
  <div class="h5-scan-weaving-ticket min-h-screen bg-gray-100 flex flex-col justify-between p-4">
    <div class="header text-center pt-6 pb-4">
      <div
        class="w-16 h-16 mx-auto mb-3 bg-blue-100 rounded-full flex items-center justify-center text-primary"
      >
        <Icon icon="ep:aim" :size="32" />
      </div>
      <h2 class="text-18px font-bold text-gray-800">布票扫码识别</h2>
      <p class="text-12px text-gray-500 mt-1">车间移动 H5 报工终端</p>
    </div>

    <div class="content bg-white rounded-lg p-5 shadow-sm border border-gray-200">
      <div class="text-center mb-4 pb-3 border-b border-gray-100">
        <span class="text-12px text-gray-500">已扫描布票编号</span>
        <div class="font-mono text-16px font-bold text-primary mt-1">
          {{ scannedCode || '未读取到布票编号' }}
        </div>
      </div>

      <!-- 通用扫码组件（支持摄像头扫码与键盘楔入） -->
      <div class="mb-4">
        <H5Scanner placeholder="扫描或输入布票编号" default-mode="qr" @scan="handleScannedCode" />
      </div>

      <div
        class="notice-box bg-blue-50 border border-blue-200 rounded p-3 text-12px text-blue-700 leading-relaxed mb-4"
      >
        <p class="font-bold mb-1">提示：</p>
        <p>本页为布票二维码与条码扫码落点页。</p>
        <p class="mt-1">
          二维码 URL 格式规范：
          <span class="font-mono bg-white px-1 py-0.5 rounded border border-blue-300 break-all">
            /h5/scan/weaving-ticket?code={{ scannedCode || '{code}' }}
          </span>
        </p>
        <p class="mt-1 text-gray-600">
          完整的工人自助开工、完工、现场异常照片上报流程将于后续工单接入上线。
        </p>
      </div>

      <div class="space-y-2">
        <el-button type="primary" class="w-full" size="large" disabled>
          开始报工（待后续工单接入）
        </el-button>
        <el-button class="w-full" size="large" @click="handleGoBack"> 返回上一页 </el-button>
      </div>
    </div>

    <div class="footer text-center pb-4 text-11px text-gray-400">
      LunaERP 智能织造执行车间移动终端
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import H5Scanner from '../components/Scanner.vue'

defineOptions({ name: 'H5ScanWeavingTicket' })

const route = useRoute()
const router = useRouter()

const manualCode = ref('')

const scannedCode = computed(() => {
  return manualCode.value || (route.query.code as string) || ''
})

const handleScannedCode = (code: string) => {
  manualCode.value = code
  router.replace({ query: { ...route.query, code } })
}

const handleGoBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.h5-scan-weaving-ticket {
  max-width: 480px;
  margin: 0 auto;
}
</style>
