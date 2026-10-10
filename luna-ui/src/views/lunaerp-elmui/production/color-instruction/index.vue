<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="单号" prop="instructionNo">
        <el-input
          v-model="queryParams.instructionNo"
          placeholder="打色单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="queryParams.customerId"
          placeholder="选择客户"
          clearable
          filterable
          remote
          :remote-method="searchCustomers"
          :loading="customerLoading"
          class="!w-200px"
        >
          <el-option
            v-for="item in customerOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产品" prop="productCodeOrName">
        <el-input
          v-model="queryParams.productCodeOrName"
          placeholder="产品编号 / 名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="客户色号" prop="customerColorNo">
        <el-input
          v-model="queryParams.customerColorNo"
          placeholder="客户色号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-150px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-140px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_INSTRUCTION_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单据日期" prop="instructionDateRange">
        <el-date-picker
          v-model="queryParams.instructionDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-1" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-1" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openEdit()"
          v-hasPermi="['lunaerp:production:color-instruction:create']"
        >
          <Icon icon="ep:plus" class="mr-1" />新建
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="打色单号" prop="instructionNo" min-width="140">
        <template #default="{ row }">
          <span class="cell-no cursor-pointer text-blue-600 hover:underline" @click="openView(row.id)">
            {{ row.instructionNo }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="客户" prop="clientName" min-width="160" show-overflow-tooltip />
      <el-table-column label="产品编号" prop="productCode" min-width="120" show-overflow-tooltip />
      <el-table-column label="产品名称" prop="productName" min-width="160" show-overflow-tooltip />
      <el-table-column label="颜色行摘要" min-width="220">
        <template #default="{ row }">
          <div v-if="row.colorSummary && row.colorSummary.length > 0" class="flex flex-wrap gap-1">
            <el-tag
              v-for="(item, idx) in row.colorSummary"
              :key="idx"
              size="small"
              :type="getLineTagType(item.status)"
              effect="plain"
              class="color-chip"
            >
              <span
                v-if="item.colorHex"
                class="color-dot inline-block w-2.5 h-2.5 rounded-full mr-1 align-middle"
                :style="{ backgroundColor: item.colorHex }"
              />
              <span class="font-medium">{{ item.customerColorNo }}</span>
              <span v-if="item.colorName" class="text-gray-500 ml-1">({{ item.colorName }})</span>
              <span class="ml-1 text-xs text-blue-600 font-semibold">R{{ item.round || 1 }}</span>
            </el-tag>
          </div>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_COLOR_INSTRUCTION_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="单据日期" prop="instructionDate" width="110" align="center" />
      <el-table-column label="要求完成日" prop="deliveryDate" width="110" align="center">
        <template #default="{ row }">
          <span>{{ row.deliveryDate || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="确认类型" prop="confirmType" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.confirmType === 'CUSTOMER'" class="text-green-600 font-medium">客户确认</span>
          <span v-else-if="row.confirmType === 'INTERNAL'" class="text-blue-600 font-medium">内部确认</span>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openView(row.id)"
            v-hasPermi="['lunaerp:production:color-instruction:query']"
          >
            <Icon icon="ep:view" class="mr-1" />查看
          </el-button>
          <el-button
            link
            type="primary"
            @click="openEdit(row.id, row.status)"
            v-hasPermi="['lunaerp:production:color-instruction:update']"
          >
            <Icon icon="ep:edit" class="mr-1" />编辑
          </el-button>
          <el-button
            link
            type="danger"
            v-if="row.status === 'PENDING'"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:production:color-instruction:delete']"
          >
            <Icon icon="ep:delete" class="mr-1" />删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无打色指示单数据" :image-size="90" />
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
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as ColorInstructionApi from '@/api/lunaerp-elmui/production/color-instruction'
import type { ColorInstructionVO, ColorInstructionPageReqVO } from '@/api/lunaerp-elmui/production/color-instruction'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'

defineOptions({ name: 'LunaerpElmuiProductionColorInstruction' })

const router = useRouter()

const loading = ref(true)
const total = ref(0)
const list = ref<ColorInstructionVO[]>([])
const queryFormRef = ref()

const queryParams = reactive<ColorInstructionPageReqVO>({
  pageNo: 1,
  pageSize: 20,
  instructionNo: undefined,
  customerId: undefined,
  productCodeOrName: undefined,
  customerColorNo: undefined,
  status: undefined,
  instructionDateRange: undefined
})

// 客户选项远程搜索
const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])

const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const data = await CustomerApi.getCustomerPage({ pageNo: 1, pageSize: 20, search: query || '' })
    customerOptions.value = data.list
  } catch (error) {
    console.error('Failed to load customers:', error)
  } finally {
    customerLoading.value = false
  }
}

/** 颜色行状态 tag 类型 */
const getLineTagType = (status?: string) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

/** 查询打色指示单分页 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ColorInstructionApi.getColorInstructionPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
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

/** 新建 / 编辑页面跳转 */
const openEdit = (id?: number, status?: string) => {
  if (!id) {
    router.push('/production/color-instruction/edit')
    return
  }
  if (status && !['PENDING', 'IN_PROGRESS'].includes(status)) {
    router.push(`/production/color-instruction/edit?id=${id}&mode=view`)
  } else {
    router.push(`/production/color-instruction/edit?id=${id}`)
  }
}

/** 查看详情 */
const openView = (id: number) => {
  router.push(`/production/color-instruction/edit?id=${id}&mode=view`)
}

/** 删除操作占位（Issue 05 完整实现） */
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('是否确认删除该打色指示单？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await ColorInstructionApi.deleteColorInstruction(id)
    ElMessage.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  searchCustomers('')
  getList()
})
</script>

<style lang="scss" scoped>
.cell-no {
  font-weight: 600;
}

.color-chip {
  display: inline-flex;
  align-items: center;
}

.color-dot {
  border: 1px solid #dcdfe6;
}
</style>
