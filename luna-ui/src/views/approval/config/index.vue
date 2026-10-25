<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
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

      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['lunaerp:approval:config:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新建流程
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true">
      <el-table-column label="流程编号" align="center" prop="id" width="90px" />

      <el-table-column label="单据类型" align="center" prop="bizType" width="130px">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_BIZ_TYPE" :value="scope.row.bizType" />
        </template>
      </el-table-column>

      <el-table-column label="流程名称" align="center" prop="name" min-width="160px" />

      <el-table-column label="审批链节点概览" min-width="260px">
        <template #default="scope">
          <div v-if="scope.row.nodes && scope.row.nodes.length > 0" class="flex flex-wrap items-center gap-1 py-1">
            <template v-for="(node, idx) in scope.row.nodes" :key="node.id || idx">
              <el-tag size="small" effect="plain" type="info">
                {{ node.sort }}. {{ node.name }}
                <span class="text-xs text-gray-400">
                  (<dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_NODE_TYPE" :value="node.nodeType" />)
                </span>
              </el-tag>
              <Icon
                v-if="idx < scope.row.nodes.length - 1"
                icon="ep:arrow-right"
                class="text-gray-400 text-12px"
              />
            </template>
          </div>
          <span v-else class="text-gray-400 text-12px">暂未配置节点</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" align="center" prop="status" width="100px">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.LUNAERP_APPROVAL_CONFIG_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>

      <el-table-column label="备注" align="center" prop="remark" min-width="140px" :show-overflow-tooltip="true" />

      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="170px"
      />

      <el-table-column label="操作" align="center" width="220px" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['lunaerp:approval:config:update']"
          >
            编辑
          </el-button>

          <!-- 启用 / 停用 切换 -->
          <el-button
            v-if="scope.row.status === 'ENABLED'"
            link
            type="warning"
            @click="handleStatusToggle(scope.row, 'DISABLED')"
            v-hasPermi="['lunaerp:approval:config:update']"
          >
            停用
          </el-button>
          <el-button
            v-else
            link
            type="success"
            @click="handleStatusToggle(scope.row, 'ENABLED')"
            v-hasPermi="['lunaerp:approval:config:update']"
          >
            启用
          </el-button>

          <!-- 删除 -->
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row)"
            v-hasPermi="['lunaerp:approval:config:delete']"
          >
            删除
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
  </ContentWrap>

  <!-- 表单弹窗：新建/编辑 -->
  <ApprovalConfigForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import {
  ApprovalConfigApi,
  ApprovalConfigVO,
  ApprovalConfigPageReqVO
} from '@/api/approval/config'
import ApprovalConfigForm from './ApprovalConfigForm.vue'

defineOptions({ name: 'ApprovalConfig' })

const { t } = useI18n()
const message = useMessage()

const loading = ref(false)
const list = ref<ApprovalConfigVO[]>([])
const total = ref(0)

const queryParams = reactive<ApprovalConfigPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  bizType: undefined
})
const queryFormRef = ref()

/** 查询流程列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ApprovalConfigApi.getApprovalConfigPage(queryParams)
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

/** 重置搜索 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 打开新建/编辑表单 */
const formRef = ref()
const openForm = (type: 'create' | 'update', id?: number) => {
  formRef.value?.open(type, id)
}

/** 启用/停用状态切换 */
const handleStatusToggle = async (row: ApprovalConfigVO, targetStatus: string) => {
  const isEnabling = targetStatus === 'ENABLED'
  const actionText = isEnabling ? '启用' : '停用'
  const confirmText = isEnabling
    ? `是否确认启用审批流程【${row.name}】？注意：同单据类型仅允许保持一条启用流程。`
    : `是否确认停用审批流程【${row.name}】？停用后该单据类型将无生效审批链。`

  try {
    await message.confirm(confirmText, `确认${actionText}`)
    await ApprovalConfigApi.updateApprovalConfigStatus(row.id!, targetStatus)
    message.success(`${actionText}成功`)
    await getList()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    // 后端拒绝若返回提示，拦截器已提示或此处兜底
    console.warn(`流程${actionText}失败:`, error)
  }
}

/** 删除流程（仅未被实例引用） */
const handleDelete = async (row: ApprovalConfigVO) => {
  try {
    await message.delConfirm(`是否确认删除审批流程【${row.name}】？`)
    await ApprovalConfigApi.deleteApprovalConfig(row.id!)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    const errorMsg = error?.message || String(error)
    if (errorMsg.includes('引用') || errorMsg.includes('1030030008')) {
      message.error('流程已被引用，无法删除')
    }
  }
}

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped></style>
