<template>
  <el-form
    ref="formRef"
    :model="formData"
    label-width="110px"
    :disabled="isReadOnly"
  >
    <!-- 月结收费管理卡片（Issue 02） -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold text-14px">月结收费设置</span>
          <el-tag :type="formData.isPaid ? 'success' : 'danger'" size="small">
            {{ formData.isPaid ? '已付款' : '未付款' }}
          </el-tag>
        </div>
      </template>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="月结单价(元)" prop="unitPrice">
            <el-input-number
              v-model="formData.unitPrice"
              :precision="2"
              :step="5"
              :min="0"
              placeholder="默认 30.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="付款状态">
            <div class="flex items-center gap-3">
              <el-tag :type="formData.isPaid ? 'success' : 'danger'" size="default">
                {{ formData.isPaid ? '已付款' : '未付款' }}
              </el-tag>
              <span class="text-12px text-gray-400">
                （新建缺省未付，有收费权限人员可在工作台顶部或列表批量标记）
              </span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 内部成本核算（Issue 03） -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold text-14px">内部成本核算</span>
          <el-tag type="primary" size="small">
            权威重算口径
          </el-tag>
        </div>
      </template>

      <el-row :gutter="24">
        <el-col :span="6">
          <el-form-item label="生产工费" prop="productionCost">
            <el-input-number
              v-model="formData.productionCost"
              :precision="2"
              :step="0.5"
              :min="0"
              placeholder="0.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="染整费" prop="dyeingCost">
            <el-input-number
              v-model="formData.dyeingCost"
              :precision="2"
              :step="0.5"
              :min="0"
              placeholder="0.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="其他费用" prop="otherCost">
            <el-input-number
              v-model="formData.otherCost"
              :precision="2"
              :step="0.5"
              :min="0"
              placeholder="0.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="损耗率(%)" prop="lossRate">
            <el-input-number
              v-model="formData.lossRate"
              :precision="2"
              :step="1"
              :min="0"
              :max="99.99"
              placeholder="如 5 表示 5%"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 损耗率非法警告 -->
      <el-alert
        v-if="!costPreview.isLossRateValid"
        :title="costPreview.errorMessage || '损耗率取值非法（必须 ≥ 0 且 < 100）'"
        type="error"
        show-icon
        :closable="false"
        class="mb-16px"
      />

      <!-- 实时预览与核算汇总面板 -->
      <div class="bg-gray-50 dark:bg-gray-800 p-16px rounded-4px border border-gray-100 dark:border-gray-700">
        <div class="text-13px font-bold text-gray-700 dark:text-gray-200 mb-12px flex items-center justify-between">
          <span>成本测算汇总（实时预览）</span>
          <span class="text-12px font-normal text-gray-400">
            公式：总成本 = (纱线成本 + 生产工费 + 染整费 + 其他费用) ÷ (1 − 损耗率 ÷ 100)
          </span>
        </div>

        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="纱线成本合计">
            <span class="font-medium text-14px text-blue-600">
              ¥ {{ costPreview.yarnCost.toFixed(2) }}
            </span>
            <span class="text-12px text-gray-400 ml-1">/KG</span>
          </el-descriptions-item>

          <el-descriptions-item label="加工费用合计">
            <span>
              ¥ {{ (costPreview.productionCost + costPreview.dyeingCost + costPreview.otherCost).toFixed(2) }}
            </span>
            <span class="text-12px text-gray-400 ml-1">/KG</span>
          </el-descriptions-item>

          <el-descriptions-item label="适用损耗率">
            <span>{{ costPreview.lossRate }}%</span>
          </el-descriptions-item>

          <el-descriptions-item label="预估内部总成本">
            <span v-if="costPreview.totalCost != null" class="font-bold text-16px text-orange-600">
              ¥ {{ costPreview.totalCost.toFixed(2) }}
              <span class="text-12px text-gray-400 ml-1 font-normal">/KG</span>
            </span>
            <span v-else class="text-red-500 font-medium">损耗率非法</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 服务端权威核算结果覆盖提示 -->
        <div v-if="formData.totalCost != null" class="mt-12px flex items-center gap-2 text-13px">
          <el-tag type="success" size="small">落库权威值</el-tag>
          <span class="text-gray-600 dark:text-gray-300">
            服务端权威核算总成本为：
            <strong class="text-15px text-green-600 font-mono">¥ {{ formData.totalCost.toFixed(2) }} /KG</strong>
          </span>
          <span v-if="costPreview.totalCost != null && Math.abs(costPreview.totalCost - formData.totalCost) > 0.01" class="text-12px text-orange-500">
            （检测到当前表单有未保存的调整，点击顶部保存后重新落库权威计算）
          </span>
        </div>
      </div>
    </el-card>

    <!-- 供应商报价登记（Issue 06） -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="font-bold text-14px">供应商报价登记</span>
            <el-tag type="info" size="small">纯登记参考 · 不进总成本</el-tag>
          </div>
          <el-button
            v-if="!isReadOnly"
            type="primary"
            plain
            size="small"
            @click="handleAddQuote"
          >
            <Icon icon="ep:plus" class="mr-4px" />
            新增报价行
          </el-button>
        </div>
      </template>

      <div class="text-12px text-gray-400 mb-12px">
        登记供应商胚布报价供商务参考。可下拉搜索已有供应商档案（自动快照公司名），亦可纯手填供应商名称；报价留空可保存，纯登记绝不计入内部总成本。
      </div>

      <el-table
        :data="formData.supplierQuotes || []"
        border
        size="small"
        class="w-full"
        empty-text="暂无供应商报价，点击右上角「新增报价行」添加"
      >
        <el-table-column label="#" type="index" width="50" align="center" />

        <el-table-column label="关联供应商档案" min-width="180">
          <template #default="{ row }">
            <template v-if="!isReadOnly">
              <el-select
                v-model="row.supplierId"
                filterable
                remote
                clearable
                :remote-method="searchSuppliers"
                :loading="supplierLoading"
                placeholder="搜索供应商档案(可选)"
                class="!w-full"
                @change="(val: number | null) => handleSupplierChange(row, val)"
                @clear="handleSupplierClear(row)"
              >
                <!-- 历史存量兜底选项（防不在分页中或已停用/已软删时不回显） -->
                <el-option
                  v-if="row.supplierId && !supplierOptions.some((s) => s.id === row.supplierId)"
                  :key="row.supplierId"
                  :label="row.supplierName ? `${row.supplierName} (#${row.supplierId})` : `供应商 #${row.supplierId}`"
                  :value="row.supplierId"
                />
                <el-option
                  v-for="s in supplierOptions"
                  :key="s.id"
                  :label="`${s.companyName}${s.status === 'INACTIVE' ? ' (已停用)' : ''}`"
                  :value="s.id!"
                />
              </el-select>
            </template>
            <template v-else>
              <span v-if="row.supplierId" class="text-gray-600">
                档案编号 #{{ row.supplierId }}
              </span>
              <span v-else class="text-gray-400">未关联档案</span>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="供应商名称 (快照/手填)" min-width="180">
          <template #default="{ row }">
            <template v-if="!isReadOnly">
              <el-input
                v-model="row.supplierName"
                placeholder="选择档案自动填充，亦可直接手填"
                clearable
              />
            </template>
            <template v-else>
              <span>{{ row.supplierName || '-' }}</span>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="胚布报价 (元/KG)" width="160">
          <template #default="{ row }">
            <template v-if="!isReadOnly">
              <el-input-number
                v-model="row.greigePrice"
                :precision="2"
                :step="0.5"
                :min="0"
                placeholder="可留空"
                controls-position="right"
                class="!w-full"
              />
            </template>
            <template v-else>
              <span v-if="row.greigePrice != null" class="font-medium text-blue-600">
                ¥ {{ Number(row.greigePrice).toFixed(2) }}
              </span>
              <span v-else class="text-gray-400">-</span>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="备注说明" min-width="200">
          <template #default="{ row }">
            <template v-if="!isReadOnly">
              <el-input
                v-model="row.remark"
                placeholder="如门幅克重要求、含税运说明等"
                clearable
              />
            </template>
            <template v-else>
              <span>{{ row.remark || '-' }}</span>
            </template>
          </template>
        </el-table-column>

        <el-table-column v-if="!isReadOnly" label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button
              type="danger"
              link
              size="small"
              @click="handleRemoveQuote($index)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 客户对外报价（Issue 06） -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <span class="font-bold text-14px">客户对外报价</span>
            <el-tag type="warning" size="small">对外商用 · 不进成本与月结收费</el-tag>
          </div>
        </div>
      </template>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="对外报价(元/米)" prop="quotedPrice">
            <el-input-number
              v-model="formData.quotedPrice"
              :precision="2"
              :step="1"
              :min="0"
              placeholder="0.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="报价备注" prop="quotedRemark">
            <el-input
              v-model="formData.quotedRemark"
              placeholder="如：含税开票不含运费、起订量 500KG 等"
              clearable
            />
          </el-form-item>
        </el-col>
      </el-row>
      <div class="text-12px text-gray-400 pl-110px">
        （对外报价为向客户提供的成品报价参考，纯登记记录，不参与内部总成本核算，亦不计入月结收费金额统计）
      </div>
    </el-card>
  </el-form>
</template>

<script lang="ts" setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { SampleAnalysisVO, SampleAnalysisSupplierQuoteVO } from '@/api/lunaerp-elmui/sample/sample-analysis'
import { calculateSampleCost, validateLossRate } from './sample-cost-calculator'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineProps<{
  isReadOnly: boolean
}>()

const formData = defineModel<SampleAnalysisVO>({ required: true })

// 实时计算成本预览
const costPreview = computed(() => {
  return calculateSampleCost(formData.value.yarnSpecs, formData.value)
})

// 供应商下拉联想
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)

const searchSuppliers = async (query = '') => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      search: query,
      pageNo: 1,
      pageSize: 20
    })
    supplierOptions.value = res.list || []
  } catch {
    supplierOptions.value = []
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierChange = (row: SampleAnalysisSupplierQuoteVO, supplierId: number | null) => {
  if (!supplierId) {
    row.supplierId = null
    return
  }
  const found = supplierOptions.value.find((s) => s.id === supplierId)
  if (found) {
    row.supplierId = found.id
    row.supplierName = found.companyName
  }
}

const handleSupplierClear = (row: SampleAnalysisSupplierQuoteVO) => {
  row.supplierId = null
}

const handleAddQuote = () => {
  if (!formData.value.supplierQuotes) {
    formData.value.supplierQuotes = []
  }
  formData.value.supplierQuotes.push({
    supplierId: null,
    supplierName: '',
    greigePrice: null,
    remark: '',
    _key: `quote_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`
  })
}

const handleRemoveQuote = (index: number) => {
  formData.value.supplierQuotes?.splice(index, 1)
}

onMounted(() => {
  searchSuppliers()
})

// 表单校验
const validate = (): boolean => {
  const check = validateLossRate(formData.value.lossRate)
  if (!check.isValid) {
    ElMessage.error(check.message || '损耗率取值非法（必须 ≥ 0 且 < 100）')
    return false
  }
  return true
}

defineExpose({
  validate
})
</script>
