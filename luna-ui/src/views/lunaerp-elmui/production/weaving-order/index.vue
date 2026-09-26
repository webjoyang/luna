<template>
  <ContentWrap>
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="织造单号 / 胚布单号 / 合同号 / 产品"
          clearable
          @keyup.enter="handleQuery"
          class="!w-260px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-160px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_WEAVING_ORDER_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          type="daterange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-1" /> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-1" /> 重置
        </el-button>
        <el-button
          type="primary"
          plain
          @click="handleCreate"
          v-hasPermi="['lunaerp:production:weaving-order:create']"
        >
          <Icon icon="ep:plus" class="mr-1" /> 新建织造单
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 列表数据 -->
    <el-table
      v-loading="loading"
      :data="list"
      border
      stripe
      style="width: 100%"
      class="mt-4"
    >
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="code" label="织造单号" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" class="font-mono font-bold" @click="handleOpenWorkspace(row.id)">
            {{ row.code }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="greigeOrderNo" label="关联胚布单" min-width="140">
        <template #default="{ row }">
          <span class="font-mono">{{ row.greigeOrderNo || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="contractNo" label="关联合同" min-width="140">
        <template #default="{ row }">
          {{ row.contractNo || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="产品" min-width="180">
        <template #default="{ row }">
          <div class="font-medium">{{ row.productName || '-' }}</div>
          <div class="text-gray-400 text-12px">{{ row.productCode }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="plannedQty" label="计划匹数" width="100" align="right">
        <template #default="{ row }">
          <span class="font-semibold">{{ row.plannedQty ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="plannedWeight" label="计划重量 (KG)" width="130" align="right">
        <template #default="{ row }">
          {{ row.plannedWeight != null ? row.plannedWeight : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="completedQty" label="完工匹数" width="100" align="right">
        <template #default="{ row }">
          <span class="font-bold text-green-600">{{ row.completedQty ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110" align="center">
        <template #default="{ row }">
          <DictTag
            :type="DICT_TYPE.LUNAERP_WEAVING_ORDER_STATUS"
            :value="row.status"
          />
        </template>
      </el-table-column>
      <el-table-column prop="creator" label="创建人" width="100" align="center">
        <template #default="{ row }">
          {{ row.creator || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="160" align="center" :formatter="dateFormatter" />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            @click="handleOpenWorkspace(row.id)"
            v-hasPermi="['lunaerp:production:weaving-order:query']"
          >
            工作台
          </el-button>
          <el-button
            type="success"
            link
            @click="handleOpenWorkspace(row.id, 'requisition')"
            v-hasPermi="['lunaerp:production:weaving-order:query']"
          >
            领纱
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            type="danger"
            link
            @click="handleDelete(row)"
            v-hasPermi="['lunaerp:production:weaving-order:delete']"
          >
            删除
          </el-button>
          <el-button
            v-if="row.status !== 'CANCELLED' && row.status !== 'COMPLETED'"
            type="warning"
            link
            @click="handleCancel(row)"
            v-hasPermi="['lunaerp:production:weaving-order:cancel']"
          >
            取消
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无织造单数据" :image-size="90" />
      </template>
    </el-table>

    <!-- 分页组件 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import {
  WeavingOrderVO,
  WeavingOrderPageParam,
  getWeavingOrderPage,
  deleteWeavingOrder,
  cancelWeavingOrder
} from '@/api/lunaerp-elmui/production/weaving-order'

const router = useRouter()
const loading = ref(false)
const total = ref(0)
const list = ref<WeavingOrderVO[]>([])
const queryFormRef = ref<FormInstance>()

const queryParams = ref<WeavingOrderPageParam>({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined,
  status: undefined,
  createTime: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getWeavingOrderPage(queryParams.value)
    list.value = res.list || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.value.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleCreate = () => {
  router.push('/production/weaving-order/workspace')
}

const handleOpenWorkspace = (id: number, tab?: string) => {
  router.push({
    path: `/production/weaving-order/workspace/${id}`,
    query: tab ? { tab } : undefined
  })
}

const handleDelete = async (row: WeavingOrderVO) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除织造单【${row.code}】？下属机台任务与布票将级联删除。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await deleteWeavingOrder(row.id)
    ElMessage.success('删除成功')
    await getList()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

const handleCancel = async (row: WeavingOrderVO) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认取消织造单【${row.code}】？取消为手动终态，不可逆。`, '取消确认', {
      type: 'warning',
      confirmButtonText: '确定取消',
      cancelButtonText: '取消'
    })
    await cancelWeavingOrder(row.id)
    ElMessage.success('取消成功')
    await getList()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e?.message || '取消失败')
  }
}

onMounted(() => {
  getList()
})
</script>
