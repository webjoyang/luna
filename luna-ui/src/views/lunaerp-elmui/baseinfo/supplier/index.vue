<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="search">
        <el-input
          v-model="queryParams.search"
          placeholder="编号 / 公司名 / 简称 / 电话"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="供应商类型" prop="supplierType">
        <el-select
          v-model="queryParams.supplierType"
          placeholder="全部"
          clearable
          class="!w-180px"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SUPPLIER_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="全部"
          clearable
          class="!w-180px"
        >
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace('create')"
          v-hasPermi="['lunaerp:baseinfo:supplier:create']"
        >
          <Icon icon="ep:plus" /> 新建
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="checkedIds.length === 0"
          @click="handleDeleteBatch"
          v-hasPermi="['lunaerp:baseinfo:supplier:delete']"
        >
          <Icon icon="ep:delete" /> 批量删除
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleDownloadTemplate"
          v-hasPermi="['lunaerp:baseinfo:supplier:import']"
        >
          <Icon icon="ep:download" /> 下载模板
        </el-button>
        <el-button
          type="primary"
          plain
          @click="handleImport"
          v-hasPermi="['lunaerp:baseinfo:supplier:import']"
        >
          <Icon icon="ep:upload" /> 导入
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      border
      stripe
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="编号" prop="supplierNo" width="160" align="center">
        <template #default="{ row }">
          <span class="cell-no">{{ row.supplierNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="公司名称" prop="companyName" min-width="220">
        <template #default="{ row }">
          <div class="cell-supplier">
            <span class="cell-supplier-name">{{ row.companyName }}</span>
            <span v-if="row.shortName && row.shortName !== row.companyName" class="cell-short">
              {{ row.shortName }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="供应商类型" prop="supplierType" width="130" align="center">
        <template #default="{ row }">
          <DictTag
            v-if="row.supplierType"
            :type="DICT_TYPE.LUNAERP_SUPPLIER_TYPE"
            :value="row.supplierType"
          />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="电话" prop="phone" width="160" align="center">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="地址" prop="address" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.address || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'ACTIVE'" type="success" size="small">启用</el-tag>
          <el-tag v-else type="info" size="small">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openWorkspace('view', row.id)"
            v-hasPermi="['lunaerp:baseinfo:supplier:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            link
            type="primary"
            @click="openWorkspace('edit', row.id)"
            v-hasPermi="['lunaerp:baseinfo:supplier:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:baseinfo:supplier:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无供应商数据" :image-size="90" />
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 导入弹窗 -->
  <SupplierImportDialog ref="importDialogRef" @success="getList" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import SupplierImportDialog from './SupplierImportDialog.vue'

defineOptions({ name: 'LunaerpElmuiBaseinfoSupplier' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<SupplierVO[]>([])
const queryFormRef = ref()
const checkedIds = ref<number[]>([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  search: '',
  supplierType: '',
  status: ''
})

/** 查询供应商分页 */
const getList = async () => {
  loading.value = true
  try {
    const data = await SupplierApi.getSupplierPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索：回车或点按钮触发，回到第 1 页 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  getList()
}

/** 打开工作台：create 走无 id 路由，view/edit 走带 id 路由（工作台默认进基础档案 Tab） */
const openWorkspace = (mode: 'create' | 'view' | 'edit', id?: number) => {
  if (mode === 'create') {
    router.push('/baseinfo/supplier/workspace')
  } else if (id) {
    router.push(`/baseinfo/supplier/workspace/${id}`)
  }
}

/** 表格勾选行变化 */
const handleSelectionChange = (rows: SupplierVO[]) => {
  checkedIds.value = rows.map((row) => row.id!)
}

/** 删除单个供应商（二次确认） */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await SupplierApi.deleteSupplier(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 批量删除供应商（二次确认） */
const handleDeleteBatch = async () => {
  try {
    await message.delConfirm()
    await SupplierApi.deleteSupplierList(checkedIds.value)
    checkedIds.value = []
    message.success('删除成功')
    await getList()
  } catch {}
}

const importDialogRef = ref()

/** 下载导入模板 */
const handleDownloadTemplate = async () => {
  try {
    const res = await SupplierApi.downloadSupplierTemplate()
    download.excel(res, '供应商导入模板.xlsx')
  } catch (e) {
    message.error('下载模板失败')
  }
}

/** 打开导入弹窗 */
const handleImport = () => {
  importDialogRef.value?.open()
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.cell-supplier {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .cell-supplier-name {
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .cell-short {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    font-style: italic;
  }
}

.cell-no {
  font-family: monospace;
  color: var(--el-color-primary);
}
</style>
