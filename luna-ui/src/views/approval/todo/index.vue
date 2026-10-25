<template>
  <ContentWrap>
    <!-- 1. 待我审批列表视图 -->
    <div v-if="!currentTask" class="todo-list-view">
      <!-- 搜索表单 -->
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="80px"
      >
        <el-form-item label="单据类型" prop="bizType">
          <el-select
            v-model="queryParams.bizType"
            placeholder="请选择单据类型"
            clearable
            class="!w-240px"
          >
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="业务单号" prop="bizNo">
          <el-input
            v-model="queryParams.bizNo"
            placeholder="请输入单号模糊搜索"
            clearable
            class="!w-240px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 待办任务列表表格 -->
      <div class="mt-20px">
        <el-table v-loading="loading" :data="list" :stripe="true">
          <el-table-column label="任务编号" align="center" prop="id" width="90px" />

          <el-table-column label="单据类型" align="center" prop="bizType" width="130px">
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE" :value="scope.row.bizType" />
            </template>
          </el-table-column>

          <el-table-column label="业务单号" align="center" prop="bizNo" min-width="160px">
            <template #default="scope">
              <el-link type="primary" :underline="false" @click="enterWorkbench(scope.row)">
                {{ scope.row.bizNo || '无单号' }}
              </el-link>
            </template>
          </el-table-column>

          <el-table-column label="发起人" align="center" prop="submitterName" width="120px">
            <template #default="scope">
              <span>{{ scope.row.submitterName || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="当前审批节点" align="center" prop="nodeName" min-width="150px">
            <template #default="scope">
              <div class="flex items-center justify-center gap-1">
                <span>{{ scope.row.nodeName }}</span>
                <dict-tag
                  v-if="scope.row.nodeType"
                  :type="DICT_TYPE.LUNAERP_APPROVAL_NODE_TYPE"
                  :value="scope.row.nodeType"
                  size="small"
                />
              </div>
            </template>
          </el-table-column>

          <el-table-column
            label="到达时间"
            align="center"
            prop="startTime"
            :formatter="dateFormatter"
            width="170px"
          />

          <el-table-column label="已等待时长" align="center" width="130px">
            <template #default="scope">
              <el-tag :type="getTimeoutTagType(scope.row.timeoutDuration)" size="small">
                {{ scope.row.timeoutDurationDesc || '刚刚' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" align="center" width="120px" fixed="right">
            <template #default="scope">
              <el-button type="primary" link @click="enterWorkbench(scope.row)">
                <Icon icon="ep:edit" class="mr-3px" /> 审批
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <!-- 2. 审批工作台视图 -->
    <div v-else class="approval-workbench-view">
      <!-- 顶栏导航与操作 -->
      <div class="flex items-center justify-between pb-16px mb-16px border-b border-gray-200 dark:border-gray-700">
        <div class="flex items-center gap-3">
          <el-button link type="primary" @click="backToList">
            <Icon icon="ep:back" class="mr-4px" /> 返回待办列表
          </el-button>
          <div class="h-16px w-1px bg-gray-300 dark:bg-gray-600"></div>
          <span class="font-bold text-18px">
            审批工作台 · {{ currentTask.bizTypeName || '单据' }} ({{ currentTask.bizNo || '-' }})
          </span>
          <dict-tag
            v-if="instanceDetail"
            :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS"
            :value="instanceDetail.status"
          />
        </div>

        <div class="flex items-center gap-2">
          <el-button type="success" @click="openApproveDialog">
            <Icon icon="ep:check" class="mr-4px" /> 通过
          </el-button>
          <el-button type="danger" @click="openRejectDialog">
            <Icon icon="ep:close" class="mr-4px" /> 驳回
          </el-button>
          <el-button type="warning" @click="openTransferDialog">
            <Icon icon="ep:share" class="mr-4px" /> 转交
          </el-button>
        </div>
      </div>

      <!-- 工作台主内容 -->
      <div class="grid grid-cols-1 gap-4">
        <!-- 审批流程进度条 -->
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
                  <div class="text-xs text-gray-500 mt-1">
                    <dict-tag
                      :type="DICT_TYPE.LUNAERP_APPROVAL_NODE_TYPE"
                      :value="node.nodeType"
                      size="small"
                      class="mb-1"
                    />
                    <div>状态: {{ node.statusText }}</div>
                  </div>
                </template>
              </el-step>
            </el-steps>
          </div>
          <el-empty v-else description="暂无审批链节点快照" :image-size="60" />
        </el-card>

        <!-- 单据概要信息 -->
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
              <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE" :value="currentTask.bizType" />
            </el-descriptions-item>
            <el-descriptions-item label="业务单号">
              {{ currentTask.bizNo || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="发起人">
              {{ currentTask.submitterName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="任务到达时间">
              {{ dateFormatter(null, null, currentTask.startTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="已等待时长">
              <el-tag :type="getTimeoutTagType(currentTask.timeoutDuration)" size="small">
                {{ currentTask.timeoutDurationDesc || '刚刚' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前任务序号">
              第 {{ currentTask.nodeSort }} 级 ({{ currentTask.nodeName }})
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 审批历史操作记录时间线 -->
        <el-card shadow="never">
          <template #header>
            <div class="flex items-center gap-2">
              <Icon icon="ep:time" class="text-primary text-16px" />
              <span class="font-semibold text-15px">审批操作记录</span>
            </div>
          </template>

          <div v-if="instanceDetail?.records && instanceDetail.records.length > 0" class="py-10px">
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
                  <div v-if="record.comment" class="text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 p-2 rounded text-13px mt-1">
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
  </ContentWrap>

  <!-- 弹窗 1：审批通过 -->
  <el-dialog v-model="approveDialogVisible" title="审批通过" width="500px" append-to-body>
    <el-form :model="approveForm" label-width="80px">
      <el-form-item label="审批意见">
        <el-input
          v-model="approveForm.comment"
          type="textarea"
          :rows="4"
          placeholder="请输入审批意见（选填，默认为“同意”）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="approveDialogVisible = false">取消</el-button>
      <el-button type="success" :loading="submitLoading" @click="submitApprove">确认通过</el-button>
    </template>
  </el-dialog>

  <!-- 弹窗 2：审批驳回 -->
  <el-dialog v-model="rejectDialogVisible" title="审批驳回" width="500px" append-to-body>
    <el-form :model="rejectForm" ref="rejectFormRef" :rules="rejectRules" label-width="80px">
      <el-form-item label="驳回意见" prop="comment">
        <el-input
          v-model="rejectForm.comment"
          type="textarea"
          :rows="4"
          placeholder="请输入驳回意见（必填，将打回发起人）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectDialogVisible = false">取消</el-button>
      <el-button type="danger" :loading="submitLoading" @click="submitReject">确认驳回</el-button>
    </template>
  </el-dialog>

  <!-- 弹窗 3：审批转交 -->
  <el-dialog v-model="transferDialogVisible" title="审批转交" width="500px" append-to-body>
    <el-form :model="transferForm" ref="transferFormRef" :rules="transferRules" label-width="90px">
      <el-form-item label="转交接收人" prop="toUserId">
        <el-select
          v-model="transferForm.toUserId"
          placeholder="请选择转交接收人（不能是自己）"
          filterable
          class="w-full"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.id"
            :label="`${user.nickname} (${user.username})`"
            :value="user.id"
            :disabled="user.id === currentUserId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="转交说明" prop="comment">
        <el-input
          v-model="transferForm.comment"
          type="textarea"
          :rows="4"
          placeholder="请输入转交说明（选填）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="transferDialogVisible = false">取消</el-button>
      <el-button type="warning" :loading="submitLoading" @click="submitTransfer">确认转交</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from '@/hooks/web/useMessage'
import { useUserStore } from '@/store/modules/user'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import {
  ApprovalTaskApi,
  ApprovalTaskTodoRespVO,
  ApprovalTaskTodoPageReqVO
} from '@/api/approval/task'
import {
  ApprovalInstanceApi,
  ApprovalInstanceRespVO
} from '@/api/approval/instance'
import { getSimpleUserList, UserVO } from '@/api/system/user'
import {
  parseSnapshotNodes,
  getBizJumpUrl,
  getTimeoutTagType,
  validateRejectComment,
  validateTransfer,
  ParsedStepNode
} from './helper'

defineOptions({ name: 'ApprovalTodo' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const userStore = useUserStore()

const currentUserId = computed(() => userStore.getUser?.id || 0)

// 待办列表状态
const loading = ref(false)
const list = ref<ApprovalTaskTodoRespVO[]>([])
const total = ref(0)
const queryParams = reactive<ApprovalTaskTodoPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  bizType: undefined,
  bizNo: undefined
})
const queryFormRef = ref()

// 当前选中的任务与工作台状态
const currentTask = ref<ApprovalTaskTodoRespVO | null>(null)
const instanceDetail = ref<ApprovalInstanceRespVO | null>(null)
const stepNodes = ref<ParsedStepNode[]>([])

// 弹窗与提交状态
const submitLoading = ref(false)
const approveDialogVisible = ref(false)
const approveForm = reactive({ comment: '' })

const rejectDialogVisible = ref(false)
const rejectFormRef = ref()
const rejectForm = reactive({ comment: '' })
const rejectRules = reactive({
  comment: [{ required: true, message: '驳回意见不能为空', trigger: 'blur' }]
})

const transferDialogVisible = ref(false)
const transferFormRef = ref()
const transferForm = reactive<{ toUserId: number | undefined; comment: string }>({
  toUserId: undefined,
  comment: ''
})
const transferRules = reactive({
  toUserId: [{ required: true, message: '请选择转交接收人', trigger: 'change' }]
})
const userOptions = ref<UserVO[]>([])

// 计算当前进行中步骤序号
const activeStepIndex = computed(() => {
  if (!stepNodes.value || stepNodes.value.length === 0) return 0
  const currentNodeSort = instanceDetail.value?.currentNodeSort || currentTask.value?.nodeSort || 1
  if (currentNodeSort === -1) {
    return stepNodes.value.length
  }
  const idx = stepNodes.value.findIndex((n) => n.sort === currentNodeSort)
  return idx >= 0 ? idx : 0
})

// 原单据跳转路径
const bizJumpUrl = computed(() => {
  if (!currentTask.value) return ''
  return getBizJumpUrl(currentTask.value.bizType, currentTask.value.bizId)
})

/** 获取待办列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ApprovalTaskApi.getTodoPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

/** 搜索操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置搜索 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 进入审批工作台 */
const enterWorkbench = async (task: ApprovalTaskTodoRespVO) => {
  currentTask.value = task
  try {
    const inst = await ApprovalInstanceApi.getInstance(task.instanceId)
    instanceDetail.value = inst
    stepNodes.value = parseSnapshotNodes(inst.processSnapshot, inst.currentNodeSort, inst.status)
  } catch (e) {
    console.warn('获取审批实例详情失败:', e)
  }
}

/** 返回待办列表 */
const backToList = () => {
  currentTask.value = null
  instanceDetail.value = null
  stepNodes.value = []
  getList()
}

/** 跳转到原单据详情页 */
const handleJumpToBiz = () => {
  if (bizJumpUrl.value) {
    router.push(bizJumpUrl.value)
  }
}

/** 打开通过弹窗 */
const openApproveDialog = () => {
  approveForm.comment = ''
  approveDialogVisible.value = true
}

/** 提交通过 */
const submitApprove = async () => {
  if (!currentTask.value) return
  submitLoading.value = true
  try {
    await ApprovalTaskApi.approveTask(currentTask.value.id, {
      comment: approveForm.comment || '同意'
    })
    message.success('审批通过成功')
    approveDialogVisible.value = false
    backToList()
  } catch (error: any) {
    console.error('审批通过失败:', error)
  } finally {
    submitLoading.value = false
  }
}

/** 打开驳回弹窗 */
const openRejectDialog = () => {
  rejectForm.comment = ''
  rejectDialogVisible.value = true
}

/** 提交驳回 */
const submitReject = async () => {
  if (!currentTask.value) return
  const check = validateRejectComment(rejectForm.comment)
  if (!check.valid) {
    message.error(check.message!)
    return
  }

  submitLoading.value = true
  try {
    await ApprovalTaskApi.rejectTask(currentTask.value.id, {
      comment: rejectForm.comment
    })
    message.success('已驳回该审批')
    rejectDialogVisible.value = false
    backToList()
  } catch (error: any) {
    console.error('审批驳回失败:', error)
  } finally {
    submitLoading.value = false
  }
}

/** 打开转交弹窗 */
const openTransferDialog = async () => {
  transferForm.toUserId = undefined
  transferForm.comment = ''
  transferDialogVisible.value = true

  if (userOptions.value.length === 0) {
    try {
      const users = await getSimpleUserList()
      userOptions.value = users || []
    } catch (e) {
      console.warn('加载用户列表失败:', e)
    }
  }
}

/** 提交转交 */
const submitTransfer = async () => {
  if (!currentTask.value) return
  const check = validateTransfer(transferForm.toUserId, currentUserId.value)
  if (!check.valid) {
    message.error(check.message!)
    return
  }

  submitLoading.value = true
  try {
    await ApprovalTaskApi.transferTask(currentTask.value.id, {
      toUserId: transferForm.toUserId!,
      comment: transferForm.comment
    })
    message.success('转交成功')
    transferDialogVisible.value = false
    backToList()
  } catch (error: any) {
    console.error('审批转交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  await getList()

  // 若路由携带 taskId 或 instanceId，自动进入工作台
  const paramTaskId = Number(route.query.taskId || route.query.id)
  const paramInstanceId = Number(route.query.instanceId)

  if (paramTaskId) {
    const found = list.value.find((t) => t.id === paramTaskId)
    if (found) {
      await enterWorkbench(found)
    }
  } else if (paramInstanceId) {
    const found = list.value.find((t) => t.instanceId === paramInstanceId)
    if (found) {
      await enterWorkbench(found)
    }
  }
})
</script>

<style lang="scss" scoped>
.approval-workbench-view {
  :deep(.el-step__description) {
    padding-right: 10px;
  }
}
</style>
