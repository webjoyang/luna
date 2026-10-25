<template>
  <el-drawer
    v-model="drawerVisible"
    :title="`报销单详情 - ${detail?.expenseNo || ''}`"
    size="720px"
    destroy-on-close
  >
    <div v-loading="loading" class="flex flex-col h-full">
      <div v-if="detail" class="flex-1 overflow-y-auto pr-10px">
        <!-- 顶部状态卡片 -->
        <div
          class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded p-12px mb-16px flex items-center justify-between"
        >
          <div class="flex items-center gap-10px">
            <span class="font-mono text-16px font-bold text-gray-800 dark:text-gray-200">
              {{ detail.expenseNo }}
            </span>
            <dict-tag
              :type="DICT_TYPE.LUNAERP_APPROVAL_STATUS"
              :value="detail.approvalStatus"
              size="default"
            />
          </div>
          <div class="text-right">
            <span class="text-12px text-gray-500 mr-4px">报销总金额</span>
            <span class="font-mono text-18px font-bold text-orange-600">
              {{ detail.currency }} {{ formatAmount(detail.totalAmount) }}
            </span>
          </div>
        </div>

        <!-- 基本主表信息 -->
        <el-card shadow="never" class="mb-16px !border-gray-200">
          <template #header>
            <div
              class="flex items-center gap-6px text-14px font-semibold text-gray-800 dark:text-gray-200"
            >
              <Icon icon="ep:info-filled" class="text-primary" />
              <span>基本信息</span>
            </div>
          </template>

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="报销人员">
              <span class="font-medium text-gray-900 dark:text-gray-100">
                {{ detail.applicantName || '—' }}
              </span>
            </el-descriptions-item>

            <el-descriptions-item label="付款账户">
              <span>{{ detail.accountName || '—' }}</span>
            </el-descriptions-item>

            <el-descriptions-item label="账户币种">
              <el-tag size="small" type="info">{{ detail.currency || 'RMB' }}</el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="业务日期">
              <span class="font-mono">{{ detail.expenseDate || '—' }}</span>
            </el-descriptions-item>

            <el-descriptions-item label="原币总额">
              <span class="font-mono font-bold text-gray-800">
                {{ detail.currency }} {{ formatAmount(detail.originalTotal) }}
              </span>
            </el-descriptions-item>

            <el-descriptions-item label="本位币总额">
              <span class="font-mono text-gray-800"> ¥ {{ formatAmount(detail.baseTotal) }} </span>
              <span
                v-if="detail.exchangeRate && detail.currency !== 'RMB'"
                class="text-11px text-gray-500 ml-4px"
              >
                (汇率: {{ detail.exchangeRate }})
              </span>
            </el-descriptions-item>

            <el-descriptions-item label="制单人">
              <span>{{ detail.creator || '—' }}</span>
            </el-descriptions-item>

            <el-descriptions-item label="制单时间">
              <span class="font-mono">{{ detail.createTime || '—' }}</span>
            </el-descriptions-item>

            <el-descriptions-item label="提交时间">
              <span class="font-mono">{{ detail.submitTime || '—' }}</span>
            </el-descriptions-item>

            <el-descriptions-item label="审批通过时间">
              <span class="font-mono">{{ detail.approveTime || '—' }}</span>
            </el-descriptions-item>

            <el-descriptions-item label="备注说明" :span="2">
              <span class="text-gray-700 dark:text-gray-300">{{ detail.remark || '无' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 费用明细列表 -->
        <el-card shadow="never" class="mb-16px !border-gray-200">
          <template #header>
            <div
              class="flex items-center justify-between text-14px font-semibold text-gray-800 dark:text-gray-200"
            >
              <div class="flex items-center gap-6px">
                <Icon icon="ep:list" class="text-primary" />
                <span>费用分项明细 (共 {{ detail.items?.length || 0 }} 笔)</span>
              </div>
            </div>
          </template>

          <el-table :data="detail.items || []" border stripe size="small" class="w-full">
            <el-table-column label="序号" type="index" width="55" align="center" />
            <el-table-column label="费用类型" width="130" align="center">
              <template #default="{ row }">
                <dict-tag
                  :type="DICT_TYPE.LUNAERP_EXPENSE_CATEGORY"
                  :value="row.category"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column label="原币金额" width="140" align="right">
              <template #default="{ row }">
                <span class="font-mono font-medium">
                  {{ detail.currency }} {{ formatAmount(row.originalAmount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="发生日期" width="120" align="center">
              <template #default="{ row }">
                <span class="font-mono text-12px">{{ row.occurDate }}</span>
              </template>
            </el-table-column>
            <el-table-column label="费用说明" prop="remark" min-width="180" show-overflow-tooltip />
          </el-table>
        </el-card>

        <!-- 附件清单 -->
        <el-card shadow="never" class="mb-16px !border-gray-200">
          <template #header>
            <div
              class="flex items-center gap-6px text-14px font-semibold text-gray-800 dark:text-gray-200"
            >
              <Icon icon="ep:paperclip" class="text-primary" />
              <span>报销附件清单 ({{ detail.attachments?.length || 0 }} 份)</span>
            </div>
          </template>

          <div
            v-if="detail.attachments && detail.attachments.length > 0"
            class="flex flex-col gap-8px"
          >
            <div
              v-for="(url, idx) in detail.attachments"
              :key="idx"
              class="flex items-center justify-between p-8px rounded bg-gray-50 dark:bg-gray-800 border border-gray-100 dark:border-gray-700 text-13px"
            >
              <div
                class="flex items-center gap-8px overflow-hidden text-ellipsis whitespace-nowrap"
              >
                <Icon icon="ep:document" class="text-gray-500 flex-shrink-0" />
                <span class="truncate">{{ getFileNameFromUrl(url) }}</span>
              </div>
              <el-link
                :href="url"
                target="_blank"
                type="primary"
                :underline="false"
                class="flex-shrink-0 ml-12px"
              >
                查看 / 下载
              </el-link>
            </div>
          </div>
          <el-empty v-else description="暂无上传附件" :image-size="50" class="!py-8px" />
        </el-card>

        <!-- 审批进度摘要时间线 (复用 02 号审批引擎) -->
        <el-card shadow="never" class="mb-16px !border-gray-200">
          <template #header>
            <div class="flex items-center justify-between">
              <div
                class="flex items-center gap-6px text-14px font-semibold text-gray-800 dark:text-gray-200"
              >
                <Icon icon="ep:time" class="text-primary" />
                <span>审批进度与记录时间线</span>
              </div>
              <el-button
                v-if="detail.approvalProgress?.id"
                link
                type="primary"
                size="small"
                @click="goToApprovalCenter(detail.approvalProgress.id)"
              >
                审批中心查看 <Icon icon="ep:arrow-right" class="ml-2px" />
              </el-button>
            </div>
          </template>

          <!-- 审批节点步骤条 -->
          <div
            v-if="approvalSteps && approvalSteps.length > 0"
            class="mb-20px pb-14px border-b border-gray-100 dark:border-gray-800"
          >
            <el-steps :active="activeStepIndex" finish-status="success" align-center>
              <el-step
                v-for="(node, nIdx) in approvalSteps"
                :key="nIdx"
                :title="node.name"
                :description="node.approverNames?.join(', ') || node.statusText"
                :status="node.stepStatus"
              />
            </el-steps>
          </div>

          <!-- 审批操作记录时间线 -->
          <div
            v-if="detail.approvalProgress?.records && detail.approvalProgress.records.length > 0"
            class="py-6px"
          >
            <el-timeline>
              <el-timeline-item
                v-for="record in detail.approvalProgress.records"
                :key="record.id"
                :timestamp="record.operateTime"
                placement="top"
                :type="
                  record.action === 'APPROVE'
                    ? 'success'
                    : record.action === 'REJECT'
                      ? 'danger'
                      : 'primary'
                "
              >
                <div class="flex flex-col gap-1 text-13px">
                  <div class="flex items-center gap-8px">
                    <span class="font-medium text-gray-800 dark:text-gray-200">
                      {{ record.operatorName || '审批人' }}
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
                    class="text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 p-2 rounded text-12px mt-1"
                  >
                    意见：{{ record.comment }}
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
          <div
            v-else-if="detail.approvalStatus === 'PENDING'"
            class="text-12px text-gray-400 py-10px text-center"
          >
            报销单尚未提交审批，暂无审批记录
          </div>
          <el-empty v-else description="暂无审批操作记录" :image-size="50" class="!py-8px" />
        </el-card>
      </div>

      <!-- 底部抽屉操作栏 -->
      <div
        class="pt-12px border-t border-gray-200 dark:border-gray-700 flex justify-between items-center bg-white dark:bg-gray-900"
      >
        <div>
          <span v-if="actionFlags.isApproved" class="text-12px text-green-700 font-medium">
            ✓ 审批已通过，资金流水已扣款过账
          </span>
          <span v-else-if="actionFlags.isRejected" class="text-12px text-red-600 font-medium">
            ✕ 审批已被驳回，可修改明细后重新提交
          </span>
        </div>
        <div class="flex gap-8px">
          <!-- 草稿 / 驳回：编辑 -->
          <el-button
            v-if="actionFlags.canEdit"
            v-hasPermi="['lunaerp:finance:expense:update']"
            type="primary"
            plain
            @click="handleEdit"
          >
            <Icon icon="ep:edit" class="mr-4px" />
            {{ actionFlags.isRejected ? '修改再提' : '编辑' }}
          </el-button>

          <!-- 草稿 / 驳回：提交审批 -->
          <el-button
            v-if="actionFlags.canSubmit"
            v-hasPermi="['lunaerp:finance:expense:submit']"
            type="primary"
            :loading="actionLoading"
            @click="handleSubmit"
          >
            <Icon icon="ep:check" class="mr-4px" /> 提交审批
          </el-button>

          <!-- 审批中：撤回 -->
          <el-button
            v-if="actionFlags.canWithdraw"
            v-hasPermi="['lunaerp:finance:expense:submit']"
            type="warning"
            plain
            :loading="actionLoading"
            @click="handleWithdraw"
          >
            <Icon icon="ep:refresh-left" class="mr-4px" /> 撤回审批
          </el-button>

          <!-- 草稿 / 驳回：删除 -->
          <el-button
            v-if="actionFlags.canDelete"
            v-hasPermi="['lunaerp:finance:expense:delete']"
            type="danger"
            plain
            :loading="actionLoading"
            @click="handleDelete"
          >
            <Icon icon="ep:delete" class="mr-4px" /> 删除
          </el-button>

          <!-- APPROVED：套打报销单 (A4) -->
          <PrintDispatcher
            v-if="actionFlags.isApproved"
            biz-type="EXPENSE"
            :biz-id="detail?.id"
            class="inline-block"
          >
            <template #default="{ open: openPrint }">
              <el-button
                type="success"
                v-hasPermi="['lunaerp:finance:expense:print']"
                @click="openPrint()"
              >
                <Icon icon="ep:printer" class="mr-4px" /> 套打报销单 (A4)
              </el-button>
            </template>
          </PrintDispatcher>

          <el-button @click="drawerVisible = false">关闭</el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from '@/hooks/web/useMessage'
import { DICT_TYPE } from '@/utils/dict'
import { PrintDispatcher } from '@/components/Print'
import * as ExpenseApi from '@/api/lunaerp-elmui/finance/expense'
import { formatAmount, getExpenseActionFlags, parseExpenseErrorMessage } from '../model/expense'
import { resolveDetailStepItems, type DetailStepItem } from '@/views/approval/detail/helper'

defineOptions({ name: 'ExpenseDetailDrawer' })

const emit = defineEmits<{
  edit: [id: number]
  refresh: []
}>()

const router = useRouter()
const message = useMessage()

const drawerVisible = ref(false)
const loading = ref(false)
const actionLoading = ref(false)
const detail = ref<ExpenseApi.ExpenseRespVO | null>(null)

const actionFlags = computed(() => {
  return getExpenseActionFlags(detail.value?.approvalStatus)
})

const approvalSteps = computed<DetailStepItem[]>(() => {
  if (!detail.value?.approvalProgress) return []
  return resolveDetailStepItems(detail.value.approvalProgress)
})

const activeStepIndex = computed(() => {
  if (!approvalSteps.value.length) return 0
  const currentIdx = approvalSteps.value.findIndex((s) => s.stepStatus === 'process')
  if (currentIdx >= 0) return currentIdx
  if (detail.value?.approvalStatus === 'APPROVED') return approvalSteps.value.length
  return 0
})

const getFileNameFromUrl = (url: string) => {
  if (!url) return ''
  const decoded = decodeURIComponent(url)
  const slashIdx = decoded.lastIndexOf('/')
  return slashIdx >= 0 ? decoded.slice(slashIdx + 1) : decoded
}

const goToApprovalCenter = (instanceId: number) => {
  router.push({
    path: '/approval/detail',
    query: { id: instanceId }
  })
}

const loadDetail = async (id: number) => {
  loading.value = true
  try {
    detail.value = await ExpenseApi.getExpense(id)
  } catch (err: any) {
    message.error(parseExpenseErrorMessage(err))
  } finally {
    loading.value = false
  }
}

const open = (id: number) => {
  drawerVisible.value = true
  loadDetail(id)
}

const handleEdit = () => {
  if (!detail.value?.id) return
  const id = detail.value.id
  drawerVisible.value = false
  emit('edit', id)
}

const handleSubmit = async () => {
  if (!detail.value?.id) return
  try {
    await message.confirm(`确定提交报销单【${detail.value.expenseNo}】审批吗？`)
    actionLoading.value = true
    await ExpenseApi.submitExpense(detail.value.id)
    message.success('已成功提交审批')
    await loadDetail(detail.value.id)
    emit('refresh')
  } catch (err: any) {
    if (err !== 'cancel') {
      message.error(parseExpenseErrorMessage(err))
    }
  } finally {
    actionLoading.value = false
  }
}

const handleWithdraw = async () => {
  if (!detail.value?.id) return
  try {
    await message.confirm(`确定撤回报销单【${detail.value.expenseNo}】的审批申请吗？`)
    actionLoading.value = true
    await ExpenseApi.cancelApproveExpense(detail.value.id)
    message.success('已撤回报销审批申请')
    await loadDetail(detail.value.id)
    emit('refresh')
  } catch (err: any) {
    if (err !== 'cancel') {
      message.error(parseExpenseErrorMessage(err))
    }
  } finally {
    actionLoading.value = false
  }
}

const handleDelete = async () => {
  if (!detail.value?.id) return
  try {
    await message.confirm(`确定删除报销单【${detail.value.expenseNo}】吗？删除后进入回收站。`)
    actionLoading.value = true
    await ExpenseApi.deleteExpense(detail.value.id)
    message.success('删除成功')
    drawerVisible.value = false
    emit('refresh')
  } catch (err: any) {
    if (err !== 'cancel') {
      message.error(parseExpenseErrorMessage(err))
    }
  } finally {
    actionLoading.value = false
  }
}

defineExpose({ open })
</script>
