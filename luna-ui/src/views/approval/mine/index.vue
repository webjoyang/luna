<template>
  <ContentWrap>
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

      <el-form-item label="审批状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择审批状态"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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

    <!-- 我发起的审批列表表格 -->
    <div class="mt-20px">
      <el-table
        v-loading="loading"
        :data="list"
        :stripe="true"
        class="cursor-pointer"
        @row-click="handleRowClick"
      >
        <el-table-column label="实例编号" align="center" prop="id" width="90px" />

        <el-table-column label="单据类型" align="center" prop="bizType" width="130px">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE" :value="scope.row.bizType" />
          </template>
        </el-table-column>

        <el-table-column label="业务单号" align="center" prop="bizNo" min-width="160px">
          <template #default="scope">
            <el-link type="primary" :underline="false" @click.stop="goToDetail(scope.row.id)">
              {{ scope.row.bizNo || '无单号' }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="发起人" align="center" prop="submitterName" width="120px">
          <template #default="scope">
            <span>{{ scope.row.submitterName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="当前节点" align="center" prop="currentNodeName" min-width="140px">
          <template #default="scope">
            <span>{{ scope.row.currentNodeName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="审批状态" align="center" prop="status" width="110px">
          <template #default="scope">
            <dict-tag
              :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS"
              :value="scope.row.status"
            />
          </template>
        </el-table-column>

        <el-table-column
          label="发起时间"
          align="center"
          prop="startTime"
          :formatter="dateFormatter"
          width="170px"
        />

        <el-table-column
          label="结束时间"
          align="center"
          prop="endTime"
          :formatter="dateFormatter"
          width="170px"
        >
          <template #default="scope">
            <span>{{ scope.row.endTime ? dateFormatter(null, null, scope.row.endTime) : '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="140px" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click.stop="goToDetail(scope.row.id)">
              <Icon icon="ep:view" class="mr-3px" /> 详情
            </el-button>
            <el-button
              v-if="scope.row.status === 'APPROVING'"
              type="danger"
              link
              @click.stop="openWithdrawDialog(scope.row)"
            >
              <Icon icon="ep:refresh-left" class="mr-3px" /> 撤回
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import {
  ApprovalInstanceApi,
  ApprovalInstanceMyRespVO,
  ApprovalInstanceMyPageReqVO
} from '@/api/approval/instance'

defineOptions({ name: 'ApprovalMine' })

const router = useRouter()

const loading = ref(false)
const list = ref<ApprovalInstanceMyRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()

const queryParams = reactive<ApprovalInstanceMyPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  bizType: undefined,
  bizNo: undefined,
  status: undefined
})

// 撤回弹窗控制
const withdrawDialogVisible = ref(false)
const withdrawLoading = ref(false)
const currentInstanceId = ref<number>()
const withdrawForm = reactive({
  reason: ''
})

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ApprovalInstanceApi.getMyPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 点击行跳转详情 */
const handleRowClick = (row: ApprovalInstanceMyRespVO) => {
  if (row.id) {
    goToDetail(row.id)
  }
}

/** 跳转实例详情页 */
const goToDetail = (instanceId: number) => {
  router.push({
    path: '/approval/detail',
    query: { id: instanceId }
  })
}

/** 打开撤回弹窗 */
const openWithdrawDialog = (row: ApprovalInstanceMyRespVO) => {
  currentInstanceId.value = row.id
  withdrawForm.reason = ''
  withdrawDialogVisible.value = true
}

/** 确认撤回 */
const confirmWithdraw = async () => {
  if (!currentInstanceId.value) return
  withdrawLoading.value = true
  try {
    await ApprovalInstanceApi.withdrawInstance(currentInstanceId.value, {
      reason: withdrawForm.reason
    })
    ElMessage.success('审批已成功撤回')
    withdrawDialogVisible.value = false
    getList()
  } finally {
    withdrawLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
