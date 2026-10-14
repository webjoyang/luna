<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="采购单号 / 关联胚布单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-180px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_YARN_ORDER_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
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
      <el-form-item label="交货日期" prop="deliveryDate">
        <el-date-picker
          v-model="queryParams.deliveryDate"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="交货日期起"
          end-placeholder="交货日期止"
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
          v-hasPermi="['lunaerp:production:yarn-order:create']"
        >
          <Icon icon="ep:plus" /> 新建采购单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="采购单号" prop="orderNo" min-width="150">
        <template #default="{ row }">
          <span class="cell-order-no">{{ row.orderNo || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单据日期" prop="orderDate" width="110" align="center" />
      <el-table-column label="交货日期" prop="deliveryDate" width="110" align="center">
        <template #default="{ row }">{{ row.deliveryDate || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_YARN_ORDER_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column
        label="关联胚布单号"
        prop="sourceGreigeOrderNos"
        min-width="180"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ row.sourceGreigeOrderNos || '—' }}</template>
      </el-table-column>
      <el-table-column label="明细数" prop="itemCount" width="90" align="right">
        <template #default="{ row }">{{ row.itemCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="总金额" prop="totalAmount" width="120" align="right">
        <template #default="{ row }">
          {{ row.totalAmount != null ? `¥${Number(row.totalAmount).toFixed(2)}` : '—' }}
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
            v-hasPermi="['lunaerp:production:yarn-order:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            @click="openWorkspace(row.id, false)"
            v-hasPermi="['lunaerp:production:yarn-order:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:production:yarn-order:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无采购单数据" :image-size="90" />
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
import * as YarnOrderApi from '@/api/lunaerp-elmui/production/yarn-order'
import type { YarnOrderVO, YarnOrderPageReqVO } from '@/api/lunaerp-elmui/production/yarn-order'

defineOptions({ name: 'LunaerpElmuiProductionYarnOrder' })

const router = useRouter()
const route = useRoute()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<YarnOrderVO[]>([])
const queryFormRef = ref()

interface QueryParams {
  pageNo: number
  pageSize: number
  keyword: string
  status: string
  orderDate: string[] | undefined
  deliveryDate: string[] | undefined
}

const queryParams = reactive<QueryParams>({
  pageNo: 1,
  pageSize: 20,
  keyword: (route.query?.keyword as string) || '',
  status: '',
  orderDate: undefined,
  deliveryDate: undefined
})

/** 查询采购单分页 */
const getList = async () => {
  loading.value = true
  try {
    const params: YarnOrderPageReqVO = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status || undefined,
      orderDateStart: queryParams.orderDate?.[0],
      orderDateEnd: queryParams.orderDate?.[1],
      deliveryDateStart: queryParams.deliveryDate?.[0],
      deliveryDateEnd: queryParams.deliveryDate?.[1]
    }
    const data = await YarnOrderApi.getYarnOrderPage(params)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
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

/** 打开工作台 */
const openWorkspace = (id?: number, viewOnly: boolean = false) => {
  if (!id) {
    router.push('/production/yarn-order/workspace')
  } else {
    router.push(
      viewOnly
        ? `/production/yarn-order/workspace/${id}?mode=view`
        : `/production/yarn-order/workspace/${id}`
    )
  }
}

/** 删除采购单（仅草稿态） */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await YarnOrderApi.deleteYarnOrder(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.cell-order-no {
  font-weight: 600;
  color: var(--el-color-primary);
}
</style>
