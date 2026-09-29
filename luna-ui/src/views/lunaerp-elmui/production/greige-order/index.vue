<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="胚布单号 / 合同号 / 产品名称 / 产品编号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-180px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_GREIGE_ORDER_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="生产模式" prop="productionMode">
        <el-select v-model="queryParams.productionMode" placeholder="全部" clearable class="!w-140px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_GREIGE_PRODUCTION_MODE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openEdit()"
          v-hasPermi="['lunaerp:production:greige-order:create']"
        >
          <Icon icon="ep:plus" /> 新建
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="胚布单号" prop="orderNo" min-width="130">
        <template #default="{ row }">
          <span class="cell-no">{{ row.orderNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="生产模式" prop="productionMode" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_GREIGE_PRODUCTION_MODE" :value="row.productionMode || 'OUTSOURCE'" />
        </template>
      </el-table-column>
      <el-table-column label="合同号" prop="contractNo" min-width="130" />
      <el-table-column label="产品名称" prop="productName" min-width="160" show-overflow-tooltip />
      <el-table-column label="坯布厂" prop="greigeFactoryName" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.greigeFactoryName || (row.productionMode === 'SELF' ? '-' : '') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="指定染厂" prop="dyeingFactoryName" min-width="170" show-overflow-tooltip />
      <el-table-column label="计划投胚(KG)" prop="plannedQty" width="130" align="right">
        <template #default="{ row }">{{ formatQty(row.plannedQty) }}</template>
      </el-table-column>
      <el-table-column label="交货日期" prop="deliveryDate" width="110" align="center" />
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_GREIGE_ORDER_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openView(row.id)"
            v-hasPermi="['lunaerp:production:greige-order:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            link
            type="primary"
            @click="openEdit(row.id)"
            v-hasPermi="['lunaerp:production:greige-order:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            link
            type="primary"
            @click="openDispatchDrawer(row.id, row.orderNo)"
            v-hasPermi="['lunaerp:greige-dispatch-record:query']"
          >
            <Icon icon="ep:van" class="mr-2px" />发染记录
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:production:greige-order:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无胚布单数据" :image-size="90" />
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 发染记录抽屉 -->
  <DispatchRecordDrawer ref="dispatchDrawerRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as GreigeOrderApi from '@/api/lunaerp-elmui/production/greige-order'
import type { GreigeOrderVO } from '@/api/lunaerp-elmui/production/greige-order'
import DispatchRecordDrawer from './components/DispatchRecordDrawer.vue'

defineOptions({ name: 'LunaerpElmuiProductionGreigeOrder' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<GreigeOrderVO[]>([])
const queryFormRef = ref()
const dispatchDrawerRef = ref<InstanceType<typeof DispatchRecordDrawer>>()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  status: '',
  productionMode: ''
})

/** 查询胚布单分页 */
const getList = async () => {
  loading.value = true
  try {
    const data = await GreigeOrderApi.getGreigeOrderPage(queryParams)
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

/** 数量：千分位两位小数 */
const formatQty = (value?: number | string) => {
  return Number(value ?? 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/** 新建/编辑：整页表单（无 id 新建，有 id 编辑；02/03 票落地前为占位页） */
const openEdit = (id?: number) => {
  router.push(id ? `/production/greige-order/edit/${id}` : '/production/greige-order/edit')
}

/** 查看：同一表单页只读模式打开 */
const openView = (id: number) => {
  router.push(`/production/greige-order/edit/${id}?mode=view`)
}

/** 打开发染记录抽屉 */
const openDispatchDrawer = (id: number, orderNo: string) => {
  dispatchDrawerRef.value?.open(id, orderNo)
}

/** 删除：二次确认后级联逻辑删除主表与颜色行，成功后即时刷新列表 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await GreigeOrderApi.deleteGreigeOrder(id)
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
.cell-no {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
