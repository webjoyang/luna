<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="80px"
    >
      <el-form-item label="关键词" prop="search">
        <el-input
          v-model="queryParams.search"
          placeholder="卡号 / 产品编号 / 品名"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          class="!w-160px"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COST_CARD_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="织物结构" prop="fabricConstruction">
        <el-select
          v-model="queryParams.fabricConstruction"
          placeholder="请选择结构"
          clearable
          class="!w-160px"
        >
          <el-option label="针织" value="KNIT" />
          <el-option label="梭织" value="WOVEN" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="handleCreate"
          v-hasPermi="['lunaerp:business:cost-card:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新建成本卡
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="成本卡号" prop="cardNo" min-width="150">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="handleOpenEdit(row.id)">
            {{ row.cardNo }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="产品编号" prop="productCode" min-width="130" />
      <el-table-column label="品名" prop="productName" min-width="160" show-overflow-tooltip />
      <el-table-column label="织物结构" prop="fabricConstruction" width="100">
        <template #default="{ row }">
          <el-tag :type="row.fabricConstruction === 'KNIT' ? 'success' : 'warning'" size="small">
            {{ row.fabricConstruction === 'KNIT' ? '针织' : '梭织' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计费基准" prop="basis" width="90">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.LUNAERP_COST_CARD_BASIS" :value="row.basis" />
        </template>
      </el-table-column>
      <el-table-column label="不含税成本" min-width="130" align="right">
        <template #default="{ row }">
          <span v-if="row.basis === 'KG'">
            ¥{{ Number(row.unitCostPerKg || 0).toFixed(4) }} / KG
          </span>
          <span v-else>
            {{ row.unitCostPerMeter != null ? `¥${Number(row.unitCostPerMeter).toFixed(4)} / M` : '—' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="含税成本" min-width="130" align="right">
        <template #default="{ row }">
          <span v-if="row.basis === 'KG'">
            ¥{{ Number(row.unitCostPerKgWithTax || 0).toFixed(4) }} / KG
          </span>
          <span v-else>
            {{ row.unitCostPerMeterWithTax != null ? `¥${Number(row.unitCostPerMeterWithTax).toFixed(4)} / M` : '—' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="缺价纱线" prop="missingPriceCount" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.missingPriceCount > 0" type="danger" size="small">
            缺 {{ row.missingPriceCount }} 项
          </el-tag>
          <el-tag v-else type="success" size="small">完整</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效日期" prop="effectiveDate" width="120">
        <template #default="{ row }">
          {{ row.effectiveDate || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.LUNAERP_COST_CARD_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="handleOpenEdit(row.id)"
            v-hasPermi="['lunaerp:business:cost-card:query']"
          >
            {{ row.status === 'DRAFT' ? '编辑' : '查看' }}
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="success"
            @click="handleEffect(row)"
            v-hasPermi="['lunaerp:business:cost-card:update']"
          >
            生效
          </el-button>
          <el-button
            v-if="row.status === 'EFFECTIVE'"
            link
            type="warning"
            @click="handleExpire(row)"
            v-hasPermi="['lunaerp:business:cost-card:update']"
          >
            失效
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleCopy(row)"
            v-hasPermi="['lunaerp:business:cost-card:update']"
          >
            复制
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            @click="handleDelete(row)"
            v-hasPermi="['lunaerp:business:cost-card:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as CostCardApi from '@/api/lunaerp/business/cost-card'
import type { CostCardVO, CostCardPageReqVO } from '@/api/lunaerp/business/cost-card'

defineOptions({ name: 'LunaerpBusinessCostCard' })

const router = useRouter()
const message = ElMessage

const loading = ref(false)
const list = ref<CostCardVO[]>([])
const total = ref(0)
const queryFormRef = ref()

const queryParams = reactive<CostCardPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  search: '',
  status: '',
  fabricConstruction: ''
})

const getList = async () => {
  loading.value = true
  try {
    const data = await CostCardApi.getCostCardPage(queryParams)
    list.value = data.list
    total.value = data.total
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
  handleQuery()
}

const handleCreate = () => {
  router.push('/business/cost-card/edit')
}

const handleOpenEdit = (id?: number) => {
  if (id) {
    router.push(`/business/cost-card/edit/${id}`)
  } else {
    router.push('/business/cost-card/edit')
  }
}

const handleEffect = async (row: CostCardVO) => {
  if (row.missingPriceCount && row.missingPriceCount > 0) {
    message.error(`该成本卡存在 ${row.missingPriceCount} 个未取到价的纱线行，不能生效！`)
    return
  }
  await ElMessageBox.confirm('生效后，同产品的旧生效成本卡将自动失效，确认将该成本卡置为生效状态？', '生效确认', {
    confirmButtonText: '确认生效',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await CostCardApi.effectCostCard(row.id!)
    message.success('成本卡已生效')
    getList()
  } catch {}
}

const handleExpire = async (row: CostCardVO) => {
  await ElMessageBox.confirm('确认手工将该成本卡失效？失效后不可恢复生效，需复制新建卡。', '失效确认', {
    confirmButtonText: '确认失效',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await CostCardApi.expireCostCard(row.id!)
    message.success('成本卡已失效')
    getList()
  } catch {}
}

const handleCopy = async (row: CostCardVO) => {
  try {
    const newId = await CostCardApi.copyCostCard(row.id!)
    message.success('复制成功，已生成新草稿卡')
    router.push(`/business/cost-card/edit/${newId}`)
  } catch {}
}

const handleDelete = async (row: CostCardVO) => {
  await ElMessageBox.confirm(`确认删除草稿成本卡【${row.cardNo}】？删除后可进入回收站恢复。`, '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  try {
    await CostCardApi.deleteCostCard(row.id!)
    message.success('删除成功')
    getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
