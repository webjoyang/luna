<template>
  <ContentWrap>
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px" class="-mb-15px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="单证套号 / 合同号 / 买方"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>

      <el-form-item label="单据状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-150px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_EXPORT_DOC_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-2px" />搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-2px" />重置
        </el-button>

        <!-- 新建单证入口下拉 -->
        <el-dropdown
          v-hasPermi="['lunaerp:business:export-doc:create']"
          @command="handleCreateCommand"
        >
          <el-button type="primary">
            <Icon icon="ep:plus" class="mr-2px" /> 新建单证套 <Icon icon="ep:arrow-down" class="ml-2px" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="MANUAL">空白自建单证套</el-dropdown-item>
              <el-dropdown-item command="CONTRACT">从外销合同生成</el-dropdown-item>
              <el-dropdown-item command="SHIPMENT">从发货单生成</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 卖方出口资料配置入口 -->
        <el-button
          type="info"
          plain
          @click="openSellerConfigDialog"
          v-hasPermi="['lunaerp:business:export-doc:seller-config']"
          class="ml-12px"
        >
          <Icon icon="ep:setting" class="mr-2px" /> 卖方出口资料
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="单证套号" prop="docNo" min-width="150">
        <template #default="{ row }">
          <span class="font-mono text-blue-600 cursor-pointer" @click="openWorkspace(row.id)">
            {{ row.docNo || '—' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="买方客户" prop="customerName" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <div>{{ row.customerName || '—' }}</div>
          <div v-if="row.buyerName" class="text-11px text-gray-500">{{ row.buyerName }}</div>
        </template>
      </el-table-column>

      <el-table-column label="销售合同号" prop="contractNo" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-mono">{{ row.contractNo || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="发货单号" prop="shipmentNo" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-mono">{{ row.shipmentNo || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="币种" prop="currency" width="80" align="center">
        <template #default="{ row }">{{ row.currency || 'USD' }}</template>
      </el-table-column>

      <el-table-column label="贸易术语" prop="incoterm" width="95" align="center">
        <template #default="{ row }">{{ row.incoterm || '—' }}</template>
      </el-table-column>

      <el-table-column label="来源" prop="sourceType" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.sourceType === 'CONTRACT'" size="small" type="primary">合同生成</el-tag>
          <el-tag v-else-if="row.sourceType === 'SHIPMENT'" size="small" type="success">发货生成</el-tag>
          <el-tag v-else size="small" type="info">手工自建</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="状态" prop="status" width="95" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_EXPORT_DOC_STATUS" :value="row.status" />
        </template>
      </el-table-column>

      <el-table-column label="创建时间" prop="createTime" width="160" align="center">
        <template #default="{ row }">
          <span>{{ row.createTime }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openWorkspace(row.id)">
            查看
          </el-button>

          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            @click="openWorkspace(row.id)"
            v-hasPermi="['lunaerp:business:export-doc:update']"
          >
            编辑
          </el-button>

          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="success"
            @click="handleFinalize(row)"
            v-hasPermi="['lunaerp:business:export-doc:update']"
          >
            定稿
          </el-button>

          <el-button
            v-if="row.status === 'FINALIZED'"
            link
            type="warning"
            @click="handleRevoke(row)"
            v-hasPermi="['lunaerp:business:export-doc:update']"
          >
            撤销
          </el-button>

          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            @click="handleDelete(row)"
            v-hasPermi="['lunaerp:business:export-doc:delete']"
          >
            删除
          </el-button>

          <el-button
            link
            type="primary"
            @click="handlePrint(row)"
            v-hasPermi="['lunaerp:business:export-doc:print']"
          >
            打印
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="flex justify-end mt-16px">
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="getList"
        @current-change="getList"
      />
    </div>
  </ContentWrap>

  <!-- 来源单据选择弹窗 -->
  <SelectSourceDialog ref="selectSourceDialogRef" @select="handleSourceSelected" />

  <!-- 卖方出口配置弹窗 -->
  <SellerConfigDialog ref="sellerConfigDialogRef" @success="getList" />

  <!-- 打印弹窗 -->
  <PrintExportDocDialog ref="printDialogRef" />
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import {
  getExportDocPage,
  deleteExportDoc,
  finalizeExportDoc,
  revokeExportDoc,
  type ExportDocVO
} from '@/api/lunaerp-elmui/business/export-doc'
import SelectSourceDialog from './components/SelectSourceDialog.vue'
import SellerConfigDialog from './components/SellerConfigDialog.vue'
import PrintExportDocDialog from './components/PrintExportDocDialog.vue'

defineOptions({ name: 'ExportDocList' })

const router = useRouter()

const loading = ref(false)
const list = ref<ExportDocVO[]>([])
const total = ref(0)
const queryFormRef = ref()

const selectSourceDialogRef = ref<InstanceType<typeof SelectSourceDialog>>()
const sellerConfigDialogRef = ref<InstanceType<typeof SellerConfigDialog>>()
const printDialogRef = ref<InstanceType<typeof PrintExportDocDialog>>()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined
})

onMounted(() => {
  getList()
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getExportDocPage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status || undefined
    })
    list.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  getList()
}

/** 打开工作台 */
const openWorkspace = (id?: number) => {
  if (id) {
    router.push(`/business/export-doc/workspace/${id}`)
  } else {
    router.push('/business/export-doc/workspace')
  }
}

/** 新建下拉命令 */
const handleCreateCommand = (command: 'MANUAL' | 'CONTRACT' | 'SHIPMENT') => {
  if (command === 'MANUAL') {
    openWorkspace()
  } else if (command === 'CONTRACT') {
    selectSourceDialogRef.value?.open('CONTRACT')
  } else if (command === 'SHIPMENT') {
    selectSourceDialogRef.value?.open('SHIPMENT')
  }
}

/** 来源选定后打开工作台 */
const handleSourceSelected = (payload: { type: 'CONTRACT' | 'SHIPMENT'; id: number; no: string }) => {
  if (payload.type === 'CONTRACT') {
    router.push({
      path: '/business/export-doc/workspace',
      query: { sourceType: 'CONTRACT', contractId: payload.id }
    })
  } else {
    router.push({
      path: '/business/export-doc/workspace',
      query: { sourceType: 'SHIPMENT', shipmentId: payload.id }
    })
  }
}

/** 打开卖方配置弹窗 */
const openSellerConfigDialog = () => {
  sellerConfigDialogRef.value?.open()
}

/** 定稿单证套 */
const handleFinalize = async (row: ExportDocVO) => {
  await ElMessageBox.confirm(`确认定稿单证套【${row.docNo}】吗？定稿后将变为只读归档状态。`, '提示', {
    type: 'warning'
  })
  try {
    await finalizeExportDoc(row.id!)
    ElMessage.success('定稿成功')
    getList()
  } catch (err: any) {
    ElMessage.error(err?.message || '定稿失败')
  }
}

/** 撤销定稿 */
const handleRevoke = async (row: ExportDocVO) => {
  await ElMessageBox.confirm(`确认撤销单证套【${row.docNo}】的定稿状态吗？撤销后将恢复为草稿。`, '提示', {
    type: 'info'
  })
  try {
    await revokeExportDoc(row.id!)
    ElMessage.success('撤销定稿成功')
    getList()
  } catch (err: any) {
    ElMessage.error(err?.message || '撤销定稿失败')
  }
}

/** 删除单证套（仅草稿） */
const handleDelete = async (row: ExportDocVO) => {
  await ElMessageBox.confirm(`确认删除单证套【${row.docNo}】吗？删除后将进入回收站。`, '删除确认', {
    type: 'warning'
  })
  try {
    await deleteExportDoc(row.id!)
    ElMessage.success('删除成功')
    getList()
  } catch (err: any) {
    ElMessage.error(err?.message || '删除失败')
  }
}

/** 打印单证套 */
const handlePrint = (row: ExportDocVO) => {
  printDialogRef.value?.open(row.id!)
}
</script>
