<template>
  <ContentWrap>
    <!-- 搜索区（5 个条件） -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="检验单号 / 产品编码 / 产品名称 / 承检方"
          clearable
          @keyup.enter="handleQuery"
          class="!w-260px"
        />
      </el-form-item>
      <el-form-item label="类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="全部" clearable class="!w-160px">
          <el-option label="胚布检验" value="GREIGE" />
          <el-option label="成品检验" value="FINISHED" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-160px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_INSPECTION_ORDER_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="承检方" prop="inspectionFactoryId">
        <el-select
          v-model="queryParams.inspectionFactoryId"
          placeholder="搜索承检方"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="searchSuppliers"
          :loading="supplierLoading"
          class="!w-180px"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单据日期" prop="orderDate">
        <el-date-picker
          v-model="queryParams.orderDate"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="单据日期起"
          end-placeholder="单据日期止"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace()"
          v-hasPermi="['lunaerp:production:inspection-order:create']"
        >
          <Icon icon="ep:plus" /> 新建检验单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <!-- 11 列结构 + 操作列 -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="检验单号" prop="code" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="cell-code">{{ row.code || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单据日期" prop="orderDate" width="110" align="center" />
      <el-table-column label="类型" prop="type" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.type === 'GREIGE' ? 'warning' : 'primary'">
            {{ row.type === 'GREIGE' ? '胚布检验' : row.type === 'FINISHED' ? '成品检验' : (row.type || '—') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="产品编码" prop="productCode" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.productCode || '—' }}</template>
      </el-table-column>
      <el-table-column label="产品名称" prop="productName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.productName || '—' }}</template>
      </el-table-column>
      <el-table-column label="承检方" prop="inspectionFactoryName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.inspectionFactoryName || '—' }}</template>
      </el-table-column>
      <el-table-column label="目标重量" prop="targetWeight" width="120" align="right">
        <template #default="{ row }">
          {{ row.targetWeight != null ? `${row.targetWeight} KG` : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="已录卷数" prop="rollCount" width="100" align="right">
        <template #default="{ row }">{{ row.rollCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_INSPECTION_ORDER_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建人" prop="creator" width="110" align="center">
        <template #default="{ row }">{{ row.creator || '—' }}</template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        prop="createTime"
        width="160"
        align="center"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openWorkspace(row.id, true)"
            v-hasPermi="['lunaerp:production:inspection-order:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT' || row.status === 'ACTIVE'"
            link
            type="primary"
            @click="openWorkspace(row.id, false)"
            v-hasPermi="['lunaerp:production:inspection-order:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:production:inspection-order:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无检验单数据" :image-size="90" />
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as InspectionOrderApi from '@/api/lunaerp-elmui/production/inspection-order'
import type { InspectionOrderVO, InspectionOrderPageParam } from '@/api/lunaerp-elmui/production/inspection-order'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineOptions({ name: 'LunaerpElmuiProductionInspectionOrder' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<InspectionOrderVO[]>([])
const queryFormRef = ref()

interface QueryParams {
  pageNo: number
  pageSize: number
  keyword: string
  type: string
  status: string
  inspectionFactoryId: number | undefined
  orderDate: string[] | undefined
}

const queryParams = reactive<QueryParams>({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  type: '',
  status: '',
  inspectionFactoryId: undefined,
  orderDate: undefined
})

// 承检方（供应商）远程搜索
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list
  } finally {
    supplierLoading.value = false
  }
}

/** 查询检验单分页 */
const getList = async () => {
  loading.value = true
  try {
    const params: InspectionOrderPageParam = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      type: queryParams.type || undefined,
      status: queryParams.status || undefined,
      inspectionFactoryId: queryParams.inspectionFactoryId || undefined,
      orderDateStart: queryParams.orderDate?.[0],
      orderDateEnd: queryParams.orderDate?.[1]
    }
    const data = await InspectionOrderApi.getInspectionOrderPage(params)
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

/** 打开工作台：无 id 新建 / 有 id 查看编辑 */
const openWorkspace = (id?: number, viewOnly: boolean = false) => {
  if (!id) {
    router.push('/production/inspection-order/workspace')
  } else {
    router.push(
      viewOnly
        ? `/production/inspection-order/workspace/${id}?mode=view`
        : `/production/inspection-order/workspace/${id}`
    )
  }
}

/** 删除检验单（二次确认） */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await InspectionOrderApi.deleteInspectionOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.cell-code {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
