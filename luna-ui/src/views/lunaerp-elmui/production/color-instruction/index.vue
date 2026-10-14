<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="78px">
      <el-form-item label="单号" prop="instructionNo">
        <el-input
          v-model="queryParams.instructionNo"
          placeholder="打色单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="queryParams.customerId"
          placeholder="搜索客户"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="searchCustomers"
          :loading="customerLoading"
          class="!w-200px"
        >
          <el-option
            v-for="item in customerOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="产品" prop="productCodeOrName">
        <el-input
          v-model="queryParams.productCodeOrName"
          placeholder="产品编号 / 名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="客户色号" prop="customerColorNo">
        <el-input
          v-model="queryParams.customerColorNo"
          placeholder="客户色号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-150px">
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
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-1" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-1" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openCreate"
          v-hasPermi="['lunaerp:production:color-instruction:create']"
        >
          <Icon icon="ep:plus" class="mr-1" /> 新建打色单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="打色单号" prop="instructionNo" min-width="140">
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="openView(row.id)">
            <span class="font-mono font-medium">{{ row.instructionNo }}</span>
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="客户" prop="clientName" min-width="150" show-overflow-tooltip />
      <el-table-column label="产品编号 / 名称" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div>
            <span class="font-mono text-gray-500 mr-1">{{ row.productCode }}</span>
            <span>{{ row.productName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="颜色行摘要" prop="colorSummaries" min-width="240">
        <template #default="{ row }">
          <div v-if="row.colorSummaries && row.colorSummaries.length > 0" class="flex flex-wrap gap-1">
            <el-tag
              v-for="(color, idx) in row.colorSummaries"
              :key="idx"
              size="small"
              :type="getColorTagType(color.status)"
              class="items-center"
            >
              <span
                v-if="color.colorHex"
                class="inline-block w-2.5 h-2.5 rounded-full mr-1 border border-gray-300 align-middle"
                :style="{ backgroundColor: color.colorHex }"
              ></span>
              <span>{{ color.customerColorNo }} {{ color.colorName }}</span>
              <span v-if="color.round && color.round > 1" class="ml-1 text-xs opacity-80">(R{{ color.round }})</span>
            </el-tag>
          </div>
          <span v-else class="text-gray-400 text-xs">-</span>
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
          <span>{{ row.deliveryDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="确认类型" prop="confirmType" width="110" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.confirmType === 'CUSTOMER'" type="success" size="small">客户确认</el-tag>
          <el-tag v-else-if="row.confirmType === 'INTERNAL'" type="primary" size="small">内部确认</el-tag>
          <span v-else class="text-gray-400">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openView(row.id)"
            v-hasPermi="['lunaerp:production:color-instruction:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            link
            type="primary"
            @click="openEdit(row)"
            v-hasPermi="['lunaerp:production:color-instruction:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <PrintDispatcher
            biz-type="COLOR_INSTRUCTION"
            :biz-id="row.id"
            class="inline-block"
          >
            <template #default="{ open }">
              <el-button
                link
                type="primary"
                @click="open()"
                v-hasPermi="['lunaerp:production:color-instruction:print']"
              >
                <Icon icon="ep:printer" class="mr-2px" />打印
              </el-button>
            </template>
          </PrintDispatcher>
          <el-button
            v-if="row.status === 'PENDING'"
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:production:color-instruction:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无打色指示单数据" />
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

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { useMessage } from '@/hooks/web/useMessage'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import * as ColorInstructionApi from '@/api/lunaerp-elmui/production/color-instruction'
import type { ColorInstructionVO, ColorInstructionPageReqVO } from '@/api/lunaerp-elmui/production/color-instruction'
import { PrintDispatcher } from '@/components/Print'

defineOptions({ name: 'LunaerpElmuiProductionColorInstruction' })

const router = useRouter()
const message = useMessage()

const loading = ref(false)
const list = ref<ColorInstructionVO[]>([])
const total = ref(0)
const queryFormRef = ref()

const queryParams = reactive<ColorInstructionPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  instructionNo: undefined,
  customerId: undefined,
  productCodeOrName: undefined,
  customerColorNo: undefined,
  status: undefined,
  instructionDateRange: undefined
})

// 客户远程搜索候选列表
const customerOptions = ref<CustomerVO[]>([])
const customerLoading = ref(false)
const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const res = await CustomerApi.getCustomerPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    customerOptions.value = res.list || []
  } catch {
    customerOptions.value = []
  } finally {
    customerLoading.value = false
  }
}

// 颜色芯片标签类型映射
const getColorTagType = (status?: string): 'success' | 'danger' | 'info' | '' => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PENDING') return 'info'
  return ''
}

// 查询列表
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

// 搜索
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

// 重置
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  queryParams.instructionDateRange = undefined
  getList()
}

// 新建打色单
const openCreate = () => {
  router.push('/production/color-instruction/edit')
}

// 查看打色单
const openView = (id?: number) => {
  if (!id) return
  router.push({
    path: `/production/color-instruction/edit/${id}`,
    query: { mode: 'view' }
  })
}

// 编辑打色单（可操作态 PENDING / IN_PROGRESS / CONFIRMED 进入，终态单据进入只读模式）
const openEdit = (row: ColorInstructionVO) => {
  if (!row?.id) return
  const isEditable = ColorInstructionApi.isColorInstructionOperable(row.status)
  router.push({
    path: `/production/color-instruction/edit/${row.id}`,
    query: isEditable ? {} : { mode: 'view' }
  })
}

// 删除打色单（仅 PENDING 状态，级联逻辑删除颜色行）
const handleDelete = async (id?: number) => {
  if (!id) return
  try {
    await message.delConfirm('是否确认删除该打色指示单？')
    await ColorInstructionApi.deleteColorInstruction(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
  searchCustomers('')
})
</script>
