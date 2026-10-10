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

      <el-form-item label="操作类型" prop="action">
        <el-select
          v-model="queryParams.action"
          placeholder="请选择操作类型"
          clearable
          class="!w-240px"
        >
          <el-option label="通过" value="APPROVE" />
          <el-option label="驳回" value="REJECT" />
          <el-option label="转交" value="TRANSFER" />
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

    <!-- 已办记录列表表格 -->
    <div class="mt-20px">
      <el-table
        v-loading="loading"
        :data="list"
        :stripe="true"
        class="cursor-pointer"
        @row-click="handleRowClick"
      >
        <el-table-column label="记录编号" align="center" prop="id" width="90px" />

        <el-table-column label="单据类型" align="center" prop="bizType" width="130px">
          <template #default="scope">
            <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE" :value="scope.row.bizType" />
          </template>
        </el-table-column>

        <el-table-column label="业务单号" align="center" prop="bizNo" min-width="160px">
          <template #default="scope">
            <el-link type="primary" :underline="false" @click.stop="goToDetail(scope.row.instanceId)">
              {{ scope.row.bizNo || '无单号' }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="发起人" align="center" prop="submitterName" width="120px">
          <template #default="scope">
            <span>{{ scope.row.submitterName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="实例状态" align="center" prop="instanceStatus" width="110px">
          <template #default="scope">
            <dict-tag
              :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS"
              :value="scope.row.instanceStatus"
            />
          </template>
        </el-table-column>

        <el-table-column label="审批节点" align="center" prop="nodeName" min-width="140px">
          <template #default="scope">
            <span>{{ scope.row.nodeName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="我的操作" align="center" prop="action" width="110px">
          <template #default="scope">
            <dict-tag
              :type="DICT_TYPE.LUNAERP_APPROVAL_RECORD_ACTION"
              :value="scope.row.action"
            />
          </template>
        </el-table-column>

        <el-table-column label="审批意见" align="center" prop="comment" min-width="160px" :show-overflow-tooltip="true">
          <template #default="scope">
            <span>{{ scope.row.comment || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column
          label="操作时间"
          align="center"
          prop="operateTime"
          :formatter="dateFormatter"
          width="170px"
        />

        <el-table-column label="操作" align="center" width="100px" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click.stop="goToDetail(scope.row.instanceId)">
              <Icon icon="ep:view" class="mr-3px" /> 详情
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
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { ApprovalTaskApi, ApprovalTaskDoneRespVO, ApprovalTaskDonePageReqVO } from '@/api/approval/task'

defineOptions({ name: 'ApprovalDone' })

const router = useRouter()

const loading = ref(false)
const list = ref<ApprovalTaskDoneRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()

const queryParams = reactive<ApprovalTaskDonePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  bizType: undefined,
  bizNo: undefined,
  action: undefined
})

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ApprovalTaskApi.getDonePage(queryParams)
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
const handleRowClick = (row: ApprovalTaskDoneRespVO) => {
  if (row.instanceId) {
    goToDetail(row.instanceId)
  }
}

/** 跳转实例详情页 */
const goToDetail = (instanceId: number) => {
  router.push({
    path: '/approval/detail',
    query: { id: instanceId }
  })
}

onMounted(() => {
  getList()
})
</script>
