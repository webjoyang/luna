<template>
  <div class="h5-scan-weaving-ticket min-h-screen bg-gray-50 flex flex-col justify-between p-4">
    <div class="header text-center pt-6 pb-4">
      <div
        class="w-16 h-16 mx-auto mb-3 bg-blue-100 rounded-full flex items-center justify-center text-primary shadow-xs"
      >
        <van-icon name="aim" size="32" />
      </div>
      <h2 class="text-18px font-bold text-gray-800">布票扫码分流</h2>
      <p class="text-12px text-gray-500 mt-1">车间移动 H5 扫码落点服务</p>
    </div>

    <div class="content bg-white rounded-xl p-5 shadow-xs border border-gray-100">
      <div class="text-center mb-4 pb-3 border-b border-gray-100">
        <span class="text-12px text-gray-400">当前布票编号</span>
        <div class="font-mono text-18px font-bold text-primary mt-1 break-all">
          {{ currentCode || '未读取到布票编号' }}
        </div>
      </div>

      <!-- 通用扫码组件（支持摄像头扫码与键盘楔入） -->
      <div class="mb-4">
        <H5Scanner
          placeholder="扫描或输入布票编号回车"
          default-mode="qr"
          @scan="handleScannedCode"
        />
      </div>

      <div
        class="notice-box bg-blue-50 border border-blue-100 rounded-lg p-3 text-12px text-blue-700 leading-relaxed mb-4"
      >
        <p class="font-bold mb-1 flex items-center">
          <van-icon name="info-o" class="mr-1" />
          分流规则说明：
        </p>
        <ul class="list-disc pl-4 space-y-0.5 text-gray-600">
          <li>仅具备报工权限工人：自动进入布票报工卡</li>
          <li>仅具备质检权限检验员：自动进入胚布质检台</li>
          <li>具备双角色用户：支持弹窗选择并记住偏好（可在"我的"中重置）</li>
        </ul>
      </div>

      <div class="space-y-2">
        <van-button
          type="primary"
          block
          round
          size="large"
          :disabled="!currentCode"
          @click="handleManualDispatch"
        >
          立即定位进入
        </van-button>
        <van-button block round plain size="large" @click="handleGoBack"> 返回上一页 </van-button>
      </div>
    </div>

    <!-- 双角色作业模式选择 ActionSheet -->
    <van-action-sheet
      v-model:show="showActionSheet"
      :actions="roleActions"
      cancel-text="取消"
      description="检测到您同时具备报工与质检权限，系统已为您记住本次选择，后续可在「我的」随时重置"
      close-on-click-action
      @select="onSelectMode"
    />

    <div class="footer text-center pb-4 text-11px text-gray-400">
      LunaERP 智能织造执行车间移动终端 · 冻结落点
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import H5Scanner from '../components/Scanner.vue'
import { useUserStoreWithOut } from '@/store/modules/user'
import { H5_PERMISSIONS, hasWorkshopPermission } from '@/utils/h5Auth'
import {
  resolveScanLanding,
  getSavedScanLandingChoice,
  setSavedScanLandingChoice,
  type ScanLandingChoice
} from './scan-landing-helper'

defineOptions({ name: 'H5ScanWeavingTicket' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStoreWithOut()

const manualCode = ref('')
const pendingCode = ref('')
const showActionSheet = ref(false)

const roleActions = [
  { name: '车间报工（进入布票卡）', subname: '挡车工开工/完工/异常登记', value: 'report' },
  { name: '移动质检（进入胚布录检）', subname: '检验员扫码录检/改录', value: 'inspection' }
]

const currentCode = computed(() => {
  return (manualCode.value || (route.query.code as string) || '').trim()
})

const canReport = computed(() =>
  hasWorkshopPermission(userStore.permissions, H5_PERMISSIONS.REPORT)
)
const canInspect = computed(() =>
  hasWorkshopPermission(userStore.permissions, H5_PERMISSIONS.INSPECTION)
)

const dispatchTicket = (code: string) => {
  const targetCode = (code || '').trim()
  if (!targetCode) {
    showToast('请扫描或输入布票编号')
    return
  }

  const savedChoice = getSavedScanLandingChoice()
  const decision = resolveScanLanding({
    code: targetCode,
    canReport: canReport.value,
    canInspect: canInspect.value,
    savedChoice
  })

  if (decision.action === 'forbidden') {
    router.replace('/h5/403')
    return
  }

  if (decision.action === 'redirect') {
    router.replace({
      path: decision.target,
      query: decision.query
    })
    return
  }

  if (decision.action === 'choose') {
    pendingCode.value = targetCode
    showActionSheet.value = true
  }
}

const handleScannedCode = (code: string) => {
  manualCode.value = code
  dispatchTicket(code)
}

const handleManualDispatch = () => {
  dispatchTicket(currentCode.value)
}

const onSelectMode = (action: { value: string }) => {
  const choice = action.value as ScanLandingChoice
  setSavedScanLandingChoice(choice)
  const targetPath = choice === 'report' ? '/h5/ticket' : '/h5/inspection'
  router.replace({
    path: targetPath,
    query: { code: pendingCode.value }
  })
}

const handleGoBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/h5/report')
  }
}

onMounted(() => {
  const initialCode = (route.query.code as string)?.trim()
  if (initialCode) {
    dispatchTicket(initialCode)
  }
})
</script>

<style scoped>
.h5-scan-weaving-ticket {
  max-width: 480px;
  margin: 0 auto;
}
</style>
