<template>
  <ContentWrap>
    <!-- 404 / 无权查看提示视图 -->
    <div v-if="notFound" class="py-40px text-center">
      <el-result
        icon="warning"
        title="无权查看或实例不存在"
        sub-title="审批实例不存在，或当前登录用户非该单据的发起人/审批人，无法查看该审批详情。"
      >
        <template #extra>
          <el-button type="primary" @click="handleBack">
            <Icon icon="ep:back" class="mr-4px" /> 返回上一页
          </el-button>
        </template>
      </el-result>
    </div>

    <!-- 正常详情视图 -->
    <div v-else v-loading="loading" class="approval-detail-view">
      <!-- 顶栏导航与操作 -->
      <div class="flex items-center justify-between pb-16px mb-16px border-b border-gray-200 dark:border-gray-700">
        <div class="flex items-center gap-3">
          <el-button link type="primary" @click="handleBack">
            <Icon icon="ep:back" class="mr-4px" /> 返回
          </el-button>
          <div class="h-16px w-1px bg-gray-300 dark:bg-gray-600"></div>
          <span class="font-bold text-18px">
            审批实例详情 · {{ instanceDetail?.bizTypeName || '单据' }} ({{ instanceDetail?.bizNo || '-' }})
          </span>
          <dict-tag
            v-if="instanceDetail"
            :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS"
            :value="instanceDetail.status"
          />
        </div>

        <div class="flex items-center gap-2">
          <el-button
            v-if="instanceDetail?.status === 'APPROVING'"
            type="danger"
            @click="openWithdrawDialog"
          >
            <Icon icon="ep:refresh-left" class="mr-4px" /> 撤回审批
          </el-button>
        </div>
      </div>

      <div v-if="instanceDetail" class="grid grid-cols-1 gap-4">
        <!-- 1. 审批流程进度条 -->
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center gap-2">
              <Icon icon="ep:odometer" class="text-primary text-16px" />
              <span class="font-semibold text-15px">审批流程进度</span>
            </div>
          </template>

          <div v-if="stepNodes.length > 0" class="py-10px">
            <el-steps :active="activeStepIndex" finish-status="success" align-center>
              <el-step
                v-for="node in stepNodes"
                :key="node.sort"
                :title="node.name"
                :status="node.stepStatus"
              >
                <template #description>
                  <div class="text-xs text-gray-500 mt-1 flex flex-col items-center">
                    <dict-tag
                      :type="DICT_TYPE.LUNAERP_APPROVAL_NODE_TYPE"
                      :value="node.nodeType"
                      size="small"
                      class="mb-1"
                    />
                    <div>状态: {{ node.statusText }}</div>
                    <div v-if="node.approverNames && node.approverNames.length > 0" class="mt-1 text-gray-400">
                      审批人: {{ node.approverNames.join(', ') }}
                    </div>
                  </div>
                </template>
              </el-step>
            </el-steps>
          </div>
          <el-empty v-else description="暂无审批链节点快照" :image-size="60" />
        </el-card>

        <!-- 2. 业务单据概要信息 -->
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <Icon icon="ep:document" class="text-primary text-16px" />
                <span class="font-semibold text-15px">业务单据详情</span>
              </div>
              <el-button
                v-if="bizJumpUrl"
                type="primary"
                link
                @click="handleJumpToBiz"
              >
                在原单据详情页查看 <Icon icon="ep:top-right" class="ml-4px" />
              </el-button>
            </div>
          </template>

          <el-descriptions :column="3" border>
            <el-descriptions-item label="单据类型">
              <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE" :value="instanceDetail.bizType" />
            </el-descriptions-item>
            <el-descriptions-item label="业务单号">
              {{ instanceDetail.bizNo || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="发起人">
              {{ instanceDetail.submitterName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="发起时间">
              {{ dateFormatter(null, null, instanceDetail.startTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="结束时间">
              {{ instanceDetail.endTime ? dateFormatter(null, null, instanceDetail.endTime) : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="实例状态">
              <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS" :value="instanceDetail.status" />
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 3. 审批历史操作记录时间线 -->
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center gap-2">
              <Icon icon="ep:time" class="text-primary text-16px" />
              <span class="font-semibold text-15px">审批操作记录时间线</span>
            </div>
          </template>

          <div v-if="instanceDetail.records && instanceDetail.records.length > 0" class="py-10px">
            <el-timeline>
              <el-timeline-item
                v-for="record in instanceDetail.records"
                :key="record.id"
                :timestamp="dateFormatter(null, null, record.operateTime)"
                placement="top"
              >
                <div class="flex flex-col gap-1 text-14px">
                  <div class="flex items-center gap-2">
                    <span class="font-medium text-gray-800 dark:text-gray-200">
                      {{ record.operatorName || '操作人' }}
                    </span>
                    <dict-tag
                      :type="DICT_TYPE.LUNAERP_APPROVAL_RECORD_ACTION"
                      :value="record.action"
                      size="small"
                    />
                    <span v-if="record.transferToName" class="text-xs text-orange-500">
                      (转交给: {{ record.transferToName }})
                    </span>
                  </div>
                  <div
                    v-if="record.comment"
                    class="text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 p-2 rounded text-13px mt-1"
                  >
                    意见：{{ record.comment }}
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
          <el-empty v-else description="暂无操作记录" :image-size="60" />
        </el-card>
      </div>
    </div>

    <!-- 撤回审批弹窗 -->
    <el-dialog v-model="withdrawDialogVisible" title="撤回审批" width="480px" append-to-body>
      <div class="mb-3 text-14px text-gray-600 dark:text-gray-400">
        确认撤回该单据的审批流程吗？撤回后单据将回到草稿状态，可重新编辑后提交。
      </div>
      <el-form :model="withdrawForm" label-width="80px">
        <el-form-item label="撤回原因">
          <el-input
            v-model="withdrawForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入撤回原因（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="withdrawDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="withdrawLoading" @click="confirmWithdraw">
          确认撤回
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import {
  ApprovalInstanceApi,
  ApprovalInstanceRespVO
} from '@/api/approval/instance'
import {
  resolveDetailStepItems,
  getActiveStepIndex,
  getBizJumpUrl,
  DetailStepItem
} from './helper'

defineOptions({ name: 'ApprovalDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const notFound = ref(false)
const instanceDetail = ref<ApprovalInstanceRespVO>()

// 撤回弹窗控制
const withdrawDialogVisible = ref(false)
const withdrawLoading = ref(false)
const withdrawForm = reactive({
  reason: ''
})

/** 计算进度条步骤项 */
const stepNodes = computed<DetailStepItem[]>(() => {
  return resolveDetailStepItems(instanceDetail.value)
})

/** 计算激活步骤序号 */
const activeStepIndex = computed<number>(() => {
  return getActiveStepIndex(stepNodes.value, instanceDetail.value?.status)
})

/** 原单据跳转路径 */
const bizJumpUrl = computed<string>(() => {
  return getBizJumpUrl(instanceDetail.value?.bizType, instanceDetail.value?.bizId)
})

/** 加载实例详情 */
const getDetail = async () => {
  const id = Number(route.query.id || route.params.id)
  if (!id) {
    notFound.value = true
    return
  }

  loading.value = true
  notFound.value = false
  try {
    const data = await ApprovalInstanceApi.getInstance(id)
    if (!data) {
      notFound.value = true
    } else {
      instanceDetail.value = data
    }
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

/** 返回上一页 */
const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/approval/mine')
  }
}

/** 跳转业务单据详情页 */
const handleJumpToBiz = () => {
  if (bizJumpUrl.value) {
    router.push(bizJumpUrl.value)
  }
}

/** 打开撤回弹窗 */
const openWithdrawDialog = () => {
  withdrawForm.reason = ''
  withdrawDialogVisible.value = true
}

/** 确认撤回 */
const confirmWithdraw = async () => {
  if (!instanceDetail.value?.id) return
  withdrawLoading.value = true
  try {
    await ApprovalInstanceApi.withdrawInstance(instanceDetail.value.id, {
      reason: withdrawForm.reason
    })
    ElMessage.success('审批已成功撤回')
    withdrawDialogVisible.value = false
    await getDetail()
  } finally {
    withdrawLoading.value = false
  }
}

onMounted(() => {
  getDetail()
})
</script>
