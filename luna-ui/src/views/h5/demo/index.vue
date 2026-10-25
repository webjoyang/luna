<template>
  <div class="h5-demo-page min-h-screen bg-gray-50 p-4 pb-12">
    <!-- 头部导航与标题 -->
    <div class="header mb-4">
      <div class="flex items-center space-x-2">
        <van-icon name="arrow-left" size="18" class="cursor-pointer" @click="handleBack" />
        <h1 class="text-17px font-bold text-gray-900">拍照压缩与弱网交互 Demo</h1>
      </div>
      <p class="text-12px text-gray-500 mt-1 pl-6">
        Ticket 08：通用拍照压缩 + 上传 + 本地草稿 + 弱网交互模式
      </p>
    </div>

    <!-- 表单内容 -->
    <div class="bg-white rounded-lg p-4 shadow-sm border border-gray-200 mb-4">
      <div
        class="text-14px font-bold text-gray-800 mb-3 pb-2 border-b border-gray-100 flex justify-between items-center"
      >
        <span>单据表单与草稿测试</span>
        <span v-if="lastSavedAt" class="text-11px text-green-600 font-normal">
          已自动保存: {{ formatTime(lastSavedAt) }}
        </span>
      </div>

      <!-- 单据编号（作为草稿分键维度） -->
      <div class="mb-3">
        <label class="block text-12px font-medium text-gray-600 mb-1"
          >单据编号 (code/id 分键)</label
        >
        <van-field
          v-model="formData.ticketCode"
          placeholder="请输入布票编号/任务编号"
          clearable
          class="!p-2 border border-gray-200 rounded text-13px"
        />
      </div>

      <!-- 异常类型 -->
      <div class="mb-3">
        <label class="block text-12px font-medium text-gray-600 mb-1">异常类型</label>
        <van-field
          v-model="formData.exceptionType"
          placeholder="例如：纬向断纱 / 表面油污"
          clearable
          class="!p-2 border border-gray-200 rounded text-13px"
        />
      </div>

      <!-- 现场说明 -->
      <div class="mb-3">
        <label class="block text-12px font-medium text-gray-600 mb-1">现场说明</label>
        <van-field
          v-model="formData.remarks"
          rows="2"
          autosize
          type="textarea"
          placeholder="请输入现场实际异常说明，内容将实时持久化到 localStorage 草稿"
          class="!p-2 border border-gray-200 rounded text-13px"
        />
      </div>

      <!-- 通用拍照压缩上传组件 -->
      <div class="mb-4">
        <PhotoUploader v-model="formData.photos" :max-count="6" directory="workshop/demo" />
      </div>

      <!-- 草稿管理按钮群 -->
      <div class="flex space-x-2 pt-2 border-t border-gray-100">
        <van-button size="small" plain type="primary" class="flex-1" @click="handleManualSaveDraft">
          手动保存草稿
        </van-button>
        <van-button size="small" plain type="default" class="flex-1" @click="handleManualLoadDraft">
          恢复已有草稿
        </van-button>
        <van-button size="small" plain type="danger" class="flex-1" @click="handleManualClearDraft">
          清除当前草稿
        </van-button>
      </div>
    </div>

    <!-- 弱网模拟与提交控制器 -->
    <div class="bg-white rounded-lg p-4 shadow-sm border border-gray-200 mb-4">
      <div class="text-14px font-bold text-gray-800 mb-3 pb-2 border-b border-gray-100">
        提交交互与分色反馈验证
      </div>

      <div class="mb-3">
        <label class="block text-12px font-medium text-gray-600 mb-1">模拟提交模式：</label>
        <van-radio-group v-model="mockMode" direction="horizontal" class="text-12px">
          <van-radio name="success" class="mr-3">正常成功</van-radio>
          <van-radio name="network-error" class="mr-3">模拟弱网/断网超时</van-radio>
          <van-radio name="business-error">模拟业务错误 (015-111)</van-radio>
        </van-radio-group>
      </div>

      <div class="bg-gray-50 p-2.5 rounded text-11px text-gray-500 mb-4">
        <p>• 提交中自动开启 Loading 与防连点锁定，阻止并发二次点击；</p>
        <p>• 弱网异常弹出橙色 Warning："网络异常，单据未提交，可点重试"，保留草稿；</p>
        <p>• 业务错误弹出红色 Danger；成功提交将弹出绿色 Success 并自动清空草稿。</p>
      </div>

      <van-button
        type="primary"
        block
        size="large"
        :loading="submitting"
        :disabled="submitting"
        loading-text="单据提交中 (防连点已生效)..."
        @click="doSubmit"
      >
        提交单据
      </van-button>
    </div>

    <!-- 表单实时数据预览 -->
    <div class="bg-white rounded-lg p-3 shadow-sm border border-gray-200 text-11px text-gray-600">
      <div class="font-bold mb-1 text-gray-700">实时表单数据 JSON 快照：</div>
      <pre class="bg-gray-50 p-2 rounded overflow-x-auto font-mono">{{
        JSON.stringify(formData, null, 2)
      }}</pre>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PhotoUploader from '../components/PhotoUploader.vue'
import { useH5Draft } from '@/utils/h5-draft'
import { useH5Submit } from '@/utils/h5-submit'

defineOptions({ name: 'H5Demo' })

const router = useRouter()

interface DemoFormData {
  ticketCode: string
  exceptionType: string
  remarks: string
  photos: string[]
}

const formData = ref<DemoFormData>({
  ticketCode: 'WT20261021-001',
  exceptionType: '纬向断纱',
  remarks: '现场机台 3 号锭位异常跳线',
  photos: []
})

const mockMode = ref<'success' | 'network-error' | 'business-error'>('success')

// 草稿管理 Composable
const { lastSavedAt, saveDraft, loadDraft, clearDraft } = useH5Draft<DemoFormData>({
  namespace: 'demo',
  key: () => formData.value.ticketCode,
  formData,
  autoSave: true,
  debounceMs: 500
})

// 格式化时间戳
const formatTime = (ts: number): string => {
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// 模拟提交端点
const executeSubmit = async (): Promise<{ code: number; message: string }> => {
  // 模拟网络请求耗时 1000ms
  await new Promise((resolve) => setTimeout(resolve, 1000))

  if (mockMode.value === 'network-error') {
    const error: any = new Error('Network Error')
    error.isAxiosError = true
    error.response = undefined
    throw error
  }

  if (mockMode.value === 'business-error') {
    const error: any = new Error('布票状态不正确 (015-111)，当前单据不可重复提交')
    error.code = '015-111'
    error.response = { status: 400, data: { code: '015-111' } }
    throw error
  }

  return { code: 0, message: '单据提交成功' }
}

// 提交管理器 Composable
const { submitting, submit: doSubmit } = useH5Submit(executeSubmit, {
  successMessage: '单据提交成功，草稿已清除',
  feedbackMode: 'notify',
  onClearDraft: () => {
    clearDraft()
  },
  onSuccess: () => {
    // 成功后可重置非关键字段
    formData.value.remarks = ''
    formData.value.photos = []
  }
})

const handleManualSaveDraft = () => {
  const ok = saveDraft()
  if (ok) {
    showToast({ message: '草稿已保存', type: 'success' })
  } else {
    showToast({ message: '保存草稿失败，单据编号不能为空', type: 'fail' })
  }
}

const handleManualLoadDraft = () => {
  const ok = loadDraft()
  if (ok) {
    showToast({ message: '已成功从本地草稿恢复', type: 'success' })
  } else {
    showToast({ message: '未找到对应编号的本地草稿', type: 'fail' })
  }
}

const handleManualClearDraft = () => {
  clearDraft()
  showToast({ message: '当前草稿已清理', type: 'success' })
}

const handleBack = () => {
  router.back()
}
</script>

<style scoped>
.h5-demo-page :deep(.van-field__control) {
  font-size: 13px;
}
</style>
