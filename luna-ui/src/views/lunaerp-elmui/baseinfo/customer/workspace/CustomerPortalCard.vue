<template>
  <el-card shadow="never" class="customer-portal-card !mb-16px" v-loading="loading">
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="font-bold text-14px">客户门户</span>
          <el-tag :type="statusBadge.type" size="small">{{ statusBadge.text }}</el-tag>
        </div>
        <div v-if="tokenData?.exists" class="flex items-center gap-2">
          <el-button
            type="primary"
            size="small"
            plain
            @click="handleCopy"
            v-hasPermi="['lunaerp:baseinfo:customer:portal-token']"
          >
            复制链接
          </el-button>
          <el-button
            type="warning"
            size="small"
            plain
            :loading="actionLoading"
            @click="handleRegenerate"
            v-hasPermi="['lunaerp:baseinfo:customer:portal-token']"
          >
            重新生成
          </el-button>
          <el-button
            type="danger"
            size="small"
            plain
            :loading="actionLoading"
            @click="handleRevoke"
            v-hasPermi="['lunaerp:baseinfo:customer:portal-token']"
          >
            吊销
          </el-button>
        </div>
        <div v-else>
          <el-button
            type="primary"
            size="small"
            :loading="actionLoading"
            @click="handleGenerate"
            v-hasPermi="['lunaerp:baseinfo:customer:portal-token']"
          >
            生成门户链接
          </el-button>
        </div>
      </div>
    </template>

    <div v-if="tokenData?.exists" class="portal-info">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="生成时间">
          {{ formatDateTime(tokenData.generateTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="剩余有效期">
          <el-tag
            :type="tokenData.remainingDays && tokenData.remainingDays > 5 ? 'success' : 'danger'"
            size="small"
          >
            {{ formatPortalRemainingDays(tokenData.remainingDays) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="令牌指纹">
          <el-tag type="info" size="small">{{ tokenData.tokenFingerprint || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="访问链接">
          <span v-if="tokenData.url" class="text-gray-600 break-all select-all">{{
            tokenData.url
          }}</span>
          <el-text v-else type="warning" class="text-12px">
            未配置基准地址 (luna.portal.base-url)，点击「复制链接」将仅复制令牌明文
          </el-text>
        </el-descriptions-item>
      </el-descriptions>
    </div>
    <div v-else class="py-2 text-gray-500 text-13px">
      该客户尚未生成门户免登链接。生成后可复制链接人工发送给客户，供其免登录查看发货单、签收与对账单确认。
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useClipboard } from '@vueuse/core'
import { formatDate } from '@/utils/formatTime'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerPortalTokenVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import {
  getPortalStatusBadge,
  formatPortalRemainingDays,
  getCopyLinkPayload
} from '../model/portalToken'

defineOptions({ name: 'LunaerpElmuiCustomerPortalCard' })

const props = defineProps<{
  customerId?: number | string | null
}>()

const loading = ref(false)
const actionLoading = ref(false)
const tokenData = ref<CustomerPortalTokenVO | null>(null)
const { copy } = useClipboard({ legacy: true })

const statusBadge = computed(() => getPortalStatusBadge(Boolean(tokenData.value?.exists)))

const formatDateTime = (val?: string | Date) => {
  if (!val) return '-'
  return formatDate(val, 'YYYY-MM-DD HH:mm:ss')
}

const fetchTokenState = async () => {
  if (!props.customerId) return
  loading.value = true
  try {
    const res = await CustomerApi.getCustomerPortalToken(Number(props.customerId))
    tokenData.value = res || { exists: false }
  } catch (err: any) {
    // 若查询失败兜底为未开通
    tokenData.value = { exists: false }
  } finally {
    loading.value = false
  }
}

const handleGenerate = async () => {
  if (!props.customerId) return
  actionLoading.value = true
  try {
    const res = await CustomerApi.generateCustomerPortalToken(Number(props.customerId))
    tokenData.value = res || { exists: false }
    ElMessage.success('门户链接生成成功')
  } catch (err: any) {
    // 错误由全局拦截器提示
  } finally {
    actionLoading.value = false
  }
}

const handleRegenerate = async () => {
  if (!props.customerId) return
  try {
    await ElMessageBox.confirm(
      '重新生成后，旧链接将立即失效！客户将无法使用之前的链接访问门户，是否确定重新生成？',
      '重新生成提示',
      {
        confirmButtonText: '确定重新生成',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  actionLoading.value = true
  try {
    const res = await CustomerApi.generateCustomerPortalToken(Number(props.customerId))
    tokenData.value = res || { exists: false }
    ElMessage.success('门户链接重新生成成功，旧链接已失效')
  } catch (err: any) {
    // 错误由全局拦截器提示
  } finally {
    actionLoading.value = false
  }
}

const handleRevoke = async () => {
  if (!props.customerId) return
  try {
    await ElMessageBox.confirm(
      '吊销后该客户将无法通过现有链接访问门户，是否确定吊销？',
      '吊销提示',
      {
        confirmButtonText: '确认吊销',
        cancelButtonText: '取消',
        type: 'danger'
      }
    )
  } catch {
    return
  }

  actionLoading.value = true
  try {
    await CustomerApi.revokeCustomerPortalToken(Number(props.customerId))
    tokenData.value = { exists: false }
    ElMessage.success('门户链接已成功吊销')
  } catch (err: any) {
    // 错误由全局拦截器提示
  } finally {
    actionLoading.value = false
  }
}

const handleCopy = async () => {
  const payload = getCopyLinkPayload(tokenData.value)
  if (!payload.canCopy) {
    ElMessage.info(payload.message)
    return
  }
  await copy(payload.text)
  if (payload.isDegraded) {
    ElMessage.warning(payload.message)
  } else {
    ElMessage.success(payload.message)
  }
}

watch(
  () => props.customerId,
  (newId) => {
    if (newId) {
      fetchTokenState()
    } else {
      tokenData.value = null
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.customer-portal-card {
  border-radius: 4px;
}
</style>
