<template>
  <ContentWrap>
    <!-- 头部导航与操作 -->
    <div class="workspace-header flex justify-between items-center mb-20px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <dict-tag
              v-if="formData.status"
              :type="DICT_TYPE.LUNAERP_COST_CARD_STATUS"
              :value="formData.status"
            />
          </div>
        </template>
      </el-page-header>
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>
        <template v-if="!isReadOnly">
          <el-button
            type="warning"
            plain
            :loading="refreshing"
            @click="handleRefreshPrices"
            v-hasPermi="['lunaerp:business:cost-card:update']"
          >
            <Icon icon="ep:refresh" class="mr-1" />重新取价
          </el-button>
          <el-button
            type="primary"
            :loading="saving"
            @click="handleSave"
            v-hasPermi="['lunaerp:business:cost-card:create', 'lunaerp:business:cost-card:update']"
          >
            保存草稿
          </el-button>
          <el-button
            v-if="formData.id"
            type="success"
            :loading="effecting"
            @click="handleEffect"
            v-hasPermi="['lunaerp:business:cost-card:update']"
          >
            生效
          </el-button>
        </template>
        <el-button
          v-if="formData.id"
          type="primary"
          plain
          @click="handleCopy"
          v-hasPermi="['lunaerp:business:cost-card:update']"
        >
          复制新建
        </el-button>
      </div>
    </div>

    <!-- 1. 基本信息卡片 -->
    <el-card shadow="never" class="mb-20px" v-loading="loading">
      <template #header>
        <div class="font-bold">基本信息</div>
      </template>
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="110px"
        :disabled="isReadOnly"
      >
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="选择产品" prop="productId">
              <el-select
                v-model="formData.productId"
                placeholder="搜索产品编号/品名带出快照与BOM"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchProducts"
                :loading="productLoading"
                class="!w-full"
                @change="handleProductChange"
                :disabled="!!formData.id || isReadOnly"
              >
                <el-option
                  v-for="item in productOptions"
                  :key="item.id"
                  :label="`${item.code} - ${item.name}`"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成本卡号" prop="cardNo">
              <el-input
                v-model="formData.cardNo"
                placeholder="留空自动生成 CC{yyyy}NNNN"
                :disabled="!!formData.id"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="取价价类" prop="yarnPriceType">
              <el-radio-group v-model="formData.yarnPriceType" @change="handleRefreshPrices">
                <el-radio-button label="PURCHASE">采购参考价</el-radio-button>
                <el-radio-button label="MARKET">市场参考价</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="产品编号">
              <el-input v-model="formData.productCode" disabled placeholder="选择产品后带出" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品名">
              <el-input v-model="formData.productName" disabled placeholder="选择产品后带出" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成分快照">
              <el-input v-model="formData.composition" disabled placeholder="选择产品后带出" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="织物结构" prop="fabricConstruction">
              <el-radio-group v-model="formData.fabricConstruction" @change="handleStructureChange">
                <el-radio-button label="KNIT">针织</el-radio-button>
                <el-radio-button label="WOVEN">梭织</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="formData.fabricConstruction === 'WOVEN'">
            <el-form-item label="梭织模式" prop="wovenCalculationMode">
              <el-radio-group v-model="formData.wovenCalculationMode" @change="triggerTrial">
                <el-radio-button label="SIMPLE">简易估算</el-radio-button>
                <el-radio-button label="PROFESSIONAL">经纬精算</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计费基准" prop="basis">
              <el-select v-model="formData.basis" class="!w-full" disabled>
                <el-option label="KG (针织口径)" value="KG" />
                <el-option label="METER (梭织口径)" value="METER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="6">
            <el-form-item label="坯布克重(GSM)" prop="greigeWeightGsm">
              <el-input-number
                v-model="formData.greigeWeightGsm"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-full"
                @change="triggerTrial"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="坯布门幅(CM)" prop="greigeWidthCm">
              <el-input-number
                v-model="formData.greigeWidthCm"
                :min="0"
                :precision="2"
                :controls="false"
                class="!w-full"
                @change="triggerTrial"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="每米重量(KG/M)">
              <div class="h-8 flex items-center px-3 text-sm font-mono font-bold bg-gray-50 border rounded text-gray-700">
                {{ calculatedKgPerMeter }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="税率(%)" prop="taxRate">
              <el-input-number
                v-model="taxRatePercent"
                :min="0"
                :max="100"
                :precision="2"
                controls-position="right"
                class="!w-full"
                @change="handleTaxChange"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="备注" prop="remarks">
              <el-input
                v-model="formData.remarks"
                type="textarea"
                :rows="2"
                placeholder="请输入备注说明"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 2. 纱线明细行卡片 -->
    <el-card shadow="never" class="mb-20px">
      <template #header>
        <div class="flex justify-between items-center">
          <div class="font-bold flex items-center gap-2">
            <span>纱线行明细</span>
            <span class="text-xs text-gray-500 font-normal">
              配比合计：{{ totalRatio.toFixed(2) }}%
            </span>
            <el-tag v-if="formData.missingPriceCount && formData.missingPriceCount > 0" type="danger" size="small">
              {{ formData.missingPriceCount }} 项缺价
            </el-tag>
          </div>
        </div>
      </template>

      <el-table :data="formData.yarnItems" stripe border>
        <el-table-column label="序号" type="index" width="55" align="center" />
        <el-table-column label="纱线规格" prop="yarnSpecName" min-width="160" />
        <el-table-column label="方向" prop="direction" width="95">
          <template #default="{ row }">
            <el-select v-if="!isReadOnly && formData.fabricConstruction === 'WOVEN'" v-model="row.direction" size="small">
              <el-option label="综合" value="GENERAL" />
              <el-option label="经向" value="WARP" />
              <el-option label="纬向" value="WEFT" />
            </el-select>
            <span v-else>{{ row.direction || 'GENERAL' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="配比(%)" prop="ratio" width="100">
          <template #default="{ row }">
            <el-input-number
              v-if="!isReadOnly"
              v-model="row.ratio"
              :min="0"
              :max="100"
              :precision="2"
              :controls="false"
              size="small"
              class="!w-full"
              @change="triggerTrial"
            />
            <span v-else>{{ row.ratio }}%</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="formData.fabricConstruction === 'WOVEN' && formData.wovenCalculationMode === 'PROFESSIONAL'"
          label="每米用量(KG/M)"
          prop="consumptionKgPerMeter"
          width="130"
        >
          <template #default="{ row }">
            <el-input-number
              v-if="!isReadOnly"
              v-model="row.consumptionKgPerMeter"
              :min="0"
              :precision="6"
              :controls="false"
              size="small"
              class="!w-full"
              @change="triggerTrial"
            />
            <span v-else>{{ row.consumptionKgPerMeter }}</span>
          </template>
        </el-table-column>
        <el-table-column label="损耗率(%)" prop="wasteRate" width="100">
          <template #default="{ row }">
            <el-input-number
              v-if="!isReadOnly"
              v-model="row.wasteRate"
              :min="0"
              :max="99.9"
              :precision="2"
              :controls="false"
              size="small"
              class="!w-full"
              @change="triggerTrial"
            />
            <span v-else>{{ row.wasteRate }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="单价 (元/KG)" prop="price" width="130">
          <template #default="{ row }">
            <div v-if="!row.price || row.price <= 0" class="text-red-500 font-bold flex items-center gap-1">
              <span>¥0.0000</span>
              <el-tag type="danger" size="small">缺价</el-tag>
            </div>
            <div v-else class="font-mono text-gray-800">
              ¥{{ Number(row.price).toFixed(4) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格来源" prop="priceSource" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.priceSource === 'PRICE_TABLE'" size="small">纱价表</el-tag>
            <el-tag v-else-if="row.priceSource === 'SPEC_REF_FALLBACK'" type="warning" size="small">规格参考价</el-tag>
            <el-tag v-else-if="row.priceSource === 'COPY_SNAPSHOT'" type="info" size="small">复制快照</el-tag>
            <span v-else>{{ row.priceSource || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="取价生效日" prop="priceEffectiveDate" width="110">
          <template #default="{ row }">
            {{ row.priceEffectiveDate || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="行贡献(元/KG)" prop="contributionPerKg" width="120" align="right">
          <template #default="{ row }">
            ¥{{ Number(row.contributionPerKg || 0).toFixed(4) }}
          </template>
        </el-table-column>
        <el-table-column label="行贡献(元/M)" prop="contributionPerMeter" width="120" align="right">
          <template #default="{ row }">
            {{ row.contributionPerMeter != null ? `¥${Number(row.contributionPerMeter).toFixed(4)}` : '—' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 3. 费用明细行卡片 -->
    <el-card shadow="never" class="mb-20px">
      <template #header>
        <div class="flex justify-between items-center">
          <div class="font-bold">加工与其他费用行</div>
          <el-button v-if="!isReadOnly" type="primary" link size="small" @click="handleAddFeeRow">
            <Icon icon="ep:plus" class="mr-1" />添加费用行
          </el-button>
        </div>
      </template>

      <el-table :data="formData.feeItems" stripe border>
        <el-table-column label="序号" type="index" width="55" align="center" />
        <el-table-column label="费用类型" prop="feeType" width="150">
          <template #default="{ row }">
            <el-select v-if="!isReadOnly" v-model="row.feeType" size="small" class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COST_CARD_FEE_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
            <dict-tag v-else :type="DICT_TYPE.LUNAERP_COST_CARD_FEE_TYPE" :value="row.feeType" />
          </template>
        </el-table-column>
        <el-table-column label="计费单位" prop="feeUnit" width="140">
          <template #default="{ row }">
            <el-select v-if="!isReadOnly" v-model="row.feeUnit" size="small" class="!w-full" @change="triggerTrial">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COST_FEE_UNIT)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
            <dict-tag v-else :type="DICT_TYPE.LUNAERP_COST_FEE_UNIT" :value="row.feeUnit" />
          </template>
        </el-table-column>
        <el-table-column label="单价" prop="price" width="130">
          <template #default="{ row }">
            <el-input-number
              v-if="!isReadOnly"
              v-model="row.price"
              :min="0"
              :precision="4"
              :controls="false"
              size="small"
              class="!w-full"
              @change="triggerTrial"
            />
            <span v-else>¥{{ Number(row.price || 0).toFixed(4) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="损耗率(%)" prop="lossRate" width="110">
          <template #default="{ row }">
            <el-input-number
              v-if="!isReadOnly"
              v-model="row.lossRate"
              :min="0"
              :max="99.9"
              :precision="2"
              :controls="false"
              size="small"
              class="!w-full"
              @change="triggerTrial"
            />
            <span v-else>{{ row.lossRate || 0 }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="行贡献(元/KG)" prop="contributionPerKg" width="130" align="right">
          <template #default="{ row }">
            {{ row.contributionPerKg != null ? `¥${Number(row.contributionPerKg).toFixed(4)}` : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="行贡献(元/M)" prop="contributionPerMeter" width="130" align="right">
          <template #default="{ row }">
            {{ row.contributionPerMeter != null ? `¥${Number(row.contributionPerMeter).toFixed(4)}` : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="备注说明" prop="remark" min-width="150">
          <template #default="{ row }">
            <el-input v-if="!isReadOnly" v-model="row.remark" size="small" placeholder="选填" />
            <span v-else>{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isReadOnly" label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="handleRemoveFeeRow($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 4. 底部权威四单位汇总面板 -->
    <el-card shadow="never" class="bg-gray-50/60 dark:bg-gray-800/40 border-primary/30">
      <div class="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div class="p-3 bg-white dark:bg-gray-900 rounded border">
          <div class="text-xs text-gray-500 font-bold mb-1">不含税成本 (元/KG)</div>
          <div class="text-lg font-bold text-gray-800 font-mono">
            ¥{{ Number(formData.unitCostPerKg || 0).toFixed(4) }}
          </div>
          <div class="text-xs text-gray-400 mt-1">
            纱 ¥{{ Number(formData.yarnCostPerKg || 0).toFixed(4) }} + 费 ¥{{ Number(formData.totalFeePerKg || 0).toFixed(4) }}
          </div>
        </div>

        <div class="p-3 bg-white dark:bg-gray-900 rounded border">
          <div class="text-xs text-gray-500 font-bold mb-1">含税成本 (元/KG)</div>
          <div class="text-lg font-bold text-primary font-mono">
            ¥{{ Number(formData.unitCostPerKgWithTax || 0).toFixed(4) }}
          </div>
          <div class="text-xs text-gray-400 mt-1">
            税率：{{ (taxRatePercent || 0).toFixed(2) }}%
          </div>
        </div>

        <div class="p-3 bg-white dark:bg-gray-900 rounded border">
          <div class="text-xs text-gray-500 font-bold mb-1">不含税成本 (元/M)</div>
          <div class="text-lg font-bold text-gray-800 font-mono">
            {{ formData.unitCostPerMeter != null ? `¥${Number(formData.unitCostPerMeter).toFixed(4)}` : '—' }}
          </div>
          <div class="text-xs text-gray-400 mt-1">
            {{ formData.unitCostPerMeter != null ? `纱 ¥${Number(formData.yarnCostPerMeter || 0).toFixed(4)} + 费 ¥${Number(formData.totalFeePerMeter || 0).toFixed(4)}` : '无法双向换算' }}
          </div>
        </div>

        <div class="p-3 bg-white dark:bg-gray-900 rounded border">
          <div class="text-xs text-gray-500 font-bold mb-1">含税成本 (元/M)</div>
          <div class="text-lg font-bold text-primary font-mono">
            {{ formData.unitCostPerMeterWithTax != null ? `¥${Number(formData.unitCostPerMeterWithTax).toFixed(4)}` : '—' }}
          </div>
          <div class="text-xs text-gray-400 mt-1">
            税率：{{ (taxRatePercent || 0).toFixed(2) }}%
          </div>
        </div>

        <div class="p-3 bg-white dark:bg-gray-900 rounded border flex flex-col justify-center">
          <div class="text-xs text-gray-500 font-bold mb-1">成本卡健康状态</div>
          <div class="flex items-center gap-2">
            <el-tag v-if="formData.missingPriceCount && formData.missingPriceCount > 0" type="danger">
              存在 {{ formData.missingPriceCount }} 项缺价
            </el-tag>
            <el-tag v-else type="success">全部纱价完备</el-tag>
          </div>
        </div>
      </div>
    </el-card>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as CostCardApi from '@/api/lunaerp/business/cost-card'
import type { CostCardVO, CostCardFeeItemVO, CostCardYarnItemVO } from '@/api/lunaerp/business/cost-card'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import * as YarnRecipeBomApi from '@/api/lunaerp-elmui/production/yarn-recipe-bom'

defineOptions({ name: 'LunaerpBusinessCostCardEdit' })

const route = useRoute()
const router = useRouter()
const message = ElMessage

const loading = ref(false)
const saving = ref(false)
const effecting = ref(false)
const refreshing = ref(false)
const formRef = ref<FormInstance>()

const productLoading = ref(false)
const productOptions = ref<ProductVO[]>([])
const taxRatePercent = ref<number>(13)

const formData = reactive<CostCardVO>({
  id: undefined,
  cardNo: '',
  productId: undefined as unknown as number,
  productCode: '',
  productName: '',
  composition: '',
  widthSnapshot: '',
  weightSnapshot: '',
  fabricConstruction: 'KNIT',
  wovenCalculationMode: 'SIMPLE',
  yarnPriceType: 'PURCHASE',
  greigeWeightGsm: undefined,
  greigeWidthCm: undefined,
  kgPerMeter: undefined,
  taxRate: 0.13,
  basis: 'KG',
  yarnCostPerKg: 0,
  yarnCostPerMeter: undefined,
  totalFeePerKg: 0,
  totalFeePerMeter: undefined,
  unitCostPerKg: 0,
  unitCostPerKgWithTax: 0,
  unitCostPerMeter: undefined,
  unitCostPerMeterWithTax: undefined,
  missingPriceCount: 0,
  effectiveDate: undefined,
  status: 'DRAFT',
  remarks: '',
  yarnItems: [],
  feeItems: []
})

const rules: FormRules = {
  productId: [{ required: true, message: '请选择关联产品', trigger: 'change' }],
  fabricConstruction: [{ required: true, message: '请选择织物结构', trigger: 'change' }],
  yarnPriceType: [{ required: true, message: '请选择取价价类', trigger: 'change' }],
  basis: [{ required: true, message: '计费基准口径不能为空', trigger: 'change' }]
}

const pageTitle = computed(() => {
  if (formData.id) {
    return isReadOnly.value ? `成本卡详情: ${formData.cardNo}` : `编辑成本卡: ${formData.cardNo}`
  }
  return '新建成本卡'
})

const isReadOnly = computed(() => {
  return formData.status && formData.status !== 'DRAFT'
})

const totalRatio = computed(() => {
  return (formData.yarnItems || []).reduce((acc, cur) => acc + (Number(cur.ratio) || 0), 0)
})

const calculatedKgPerMeter = computed(() => {
  const gsm = Number(formData.greigeWeightGsm) || 0
  const width = Number(formData.greigeWidthCm) || 0
  if (gsm > 0 && width > 0) {
    return ((gsm * width) / 100000).toFixed(4)
  }
  return '0.0000'
})

const handleTaxChange = () => {
  formData.taxRate = Number(((taxRatePercent.value || 0) / 100).toFixed(6))
  triggerTrial()
}

const handleStructureChange = () => {
  if (formData.fabricConstruction === 'KNIT') {
    formData.basis = 'KG'
  } else {
    formData.basis = 'METER'
  }
  triggerTrial()
}

const searchProducts = async (query: string) => {
  productLoading.value = true
  try {
    const res = await ProductApi.getProductPage({
      pageNo: 1,
      pageSize: 20,
      search: query || ''
    })
    productOptions.value = res.list || []
  } finally {
    productLoading.value = false
  }
}

const handleProductChange = async (productId: number) => {
  if (!productId) return
  const prod = productOptions.value.find((p) => p.id === productId)
  if (prod) {
    formData.productCode = prod.code
    formData.productName = prod.name
    formData.composition = prod.composition
    formData.widthSnapshot = prod.widthCm ? `${prod.widthCm}CM` : ''
    formData.weightSnapshot = prod.weightGsm ? `${prod.weightGsm}GSM` : ''
    formData.fabricConstruction = (prod.fabricConstruction as 'KNIT' | 'WOVEN') || 'KNIT'
    formData.wovenCalculationMode = (prod.wovenCalculationMode as 'SIMPLE' | 'PROFESSIONAL') || 'SIMPLE'
    formData.greigeWeightGsm = prod.greigeWeightGsm != null ? Number(prod.greigeWeightGsm) : (prod.weightGsm != null ? Number(prod.weightGsm) : undefined)
    formData.greigeWidthCm = prod.greigeWidthCm != null ? Number(prod.greigeWidthCm) : (prod.widthCm != null ? Number(prod.widthCm) : undefined)
    formData.basis = formData.fabricConstruction === 'KNIT' ? 'KG' : 'METER'

    if (prod.taxRate != null) {
      formData.taxRate = Number(prod.taxRate)
      taxRatePercent.value = Number((Number(prod.taxRate) * 100).toFixed(2))
    }

    // 预填初始加工费用行
    const initialFees: CostCardFeeItemVO[] = []
    if (formData.fabricConstruction === 'KNIT') {
      if (prod.knitProcessingFeePerKg != null && Number(prod.knitProcessingFeePerKg) > 0) {
        initialFees.push({
          feeType: 'WEAVING',
          feeUnit: 'PER_KG',
          price: Number(prod.knitProcessingFeePerKg),
          lossRate: 0,
          remark: '产品档案针织加工费预填'
        })
      }
    } else {
      if (prod.weavingFeePerMeter != null && Number(prod.weavingFeePerMeter) > 0) {
        initialFees.push({
          feeType: 'WEAVING',
          feeUnit: 'PER_METER',
          price: Number(prod.weavingFeePerMeter),
          lossRate: 0,
          remark: '产品档案织造费预填'
        })
      }
      if (prod.otherProcessingFeePerMeter != null && Number(prod.otherProcessingFeePerMeter) > 0) {
        initialFees.push({
          feeType: 'DYEING',
          feeUnit: 'PER_METER',
          price: Number(prod.otherProcessingFeePerMeter),
          lossRate: 0,
          remark: '产品档案其他加工费预填'
        })
      }
    }
    if (prod.packagingFee != null && Number(prod.packagingFee) > 0) {
      initialFees.push({
        feeType: 'PACKAGING',
        feeUnit: formData.basis === 'KG' ? 'PER_KG' : 'PER_METER',
        price: Number(prod.packagingFee),
        lossRate: 0,
        remark: '产品档案包装费预填'
      })
    }
    formData.feeItems = initialFees
  }

  // 获取产品配比 BOM
  try {
    const bomRes = await YarnRecipeBomApi.getYarnRecipeBomByProductId(productId)
    if (!bomRes || !bomRes.items || bomRes.items.length === 0) {
      message.error('该产品无有效配比 BOM 或 BOM 无明细，请先维护纱线配比 BOM')
      formData.yarnItems = []
      return
    }

    // 映射 BOM 行
    formData.yarnItems = bomRes.items.map((item) => ({
      yarnSpecId: item.yarnSpecId,
      yarnSpecName: item.yarnSpecName || '',
      direction: item.direction || 'GENERAL',
      ratio: item.ratio != null ? Number(item.ratio) : 0,
      density: item.density != null ? Number(item.density) : undefined,
      crimpRate: item.crimpRate != null ? Number(item.crimpRate) : undefined,
      wasteRate: item.wasteRate != null ? Number(item.wasteRate) : 0,
      consumptionKgPerMeter: item.consumptionKgPerMeter != null ? Number(item.consumptionKgPerMeter) : undefined,
      price: 0,
      priceType: formData.yarnPriceType,
      priceSource: 'PRICE_TABLE'
    }))

    // 自动触发重新拉价试算
    await handleRefreshPrices()
  } catch (e: any) {
    message.error(e?.message || '获取配比 BOM 失败')
  }
}

const handleAddFeeRow = () => {
  formData.feeItems?.push({
    feeType: 'WEAVING',
    feeUnit: formData.basis === 'KG' ? 'PER_KG' : 'PER_METER',
    price: 0,
    lossRate: 0,
    remark: ''
  })
}

const handleRemoveFeeRow = (index: number) => {
  formData.feeItems?.splice(index, 1)
  triggerTrial()
}

const triggerTrial = async () => {
  if (!formData.productId || !formData.yarnItems || formData.yarnItems.length === 0) {
    return
  }
  try {
    const res = await CostCardApi.trialCostCard(formData)
    applyTrialResult(res)
  } catch {}
}

const handleRefreshPrices = async () => {
  if (!formData.productId || !formData.yarnItems || formData.yarnItems.length === 0) {
    return
  }
  refreshing.value = true
  try {
    const res = await CostCardApi.refreshPrices(formData)
    applyTrialResult(res)
    message.success('已刷新最新纱价')
  } finally {
    refreshing.value = false
  }
}

const applyTrialResult = (res: CostCardApi.CostCardTrialRespVO) => {
  formData.basis = res.basis as 'KG' | 'METER'
  formData.kgPerMeter = res.kgPerMeter
  formData.yarnCostPerKg = res.yarnCostPerKg
  formData.yarnCostPerMeter = res.yarnCostPerMeter
  formData.totalFeePerKg = res.totalFeePerKg
  formData.totalFeePerMeter = res.totalFeePerMeter
  formData.unitCostPerKg = res.unitCostPerKg
  formData.unitCostPerKgWithTax = res.unitCostPerKgWithTax
  formData.unitCostPerMeter = res.unitCostPerMeter
  formData.unitCostPerMeterWithTax = res.unitCostPerMeterWithTax
  formData.missingPriceCount = res.missingPriceCount

  if (res.yarnItems && res.yarnItems.length === formData.yarnItems?.length) {
    for (let i = 0; i < formData.yarnItems.length; i++) {
      const target = formData.yarnItems[i]
      const source = res.yarnItems[i]
      target.price = source.price
      target.priceType = source.priceType
      target.priceSource = source.priceSource
      target.priceEffectiveDate = source.priceEffectiveDate
      target.contributionPerKg = source.contributionPerKg
      target.contributionPerMeter = source.contributionPerMeter
    }
  }

  if (res.feeItems && res.feeItems.length === formData.feeItems?.length) {
    for (let i = 0; i < formData.feeItems.length; i++) {
      const target = formData.feeItems[i]
      const source = res.feeItems[i]
      target.contributionPerKg = source.contributionPerKg
      target.contributionPerMeter = source.contributionPerMeter
    }
  }
}

const handleSave = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (formData.id) {
      await CostCardApi.updateCostCard(formData)
      message.success('保存草稿成功')
    } else {
      const newId = await CostCardApi.createCostCard(formData)
      message.success('创建成本卡成功')
      formData.id = newId
    }
    await loadData(formData.id!)
  } finally {
    saving.value = false
  }
}

const handleEffect = async () => {
  if (formData.missingPriceCount && formData.missingPriceCount > 0) {
    message.error(`该成本卡存在 ${formData.missingPriceCount} 个未取到价的纱线行，不能生效！`)
    return
  }
  await ElMessageBox.confirm('生效后，同产品的旧生效成本卡将自动失效，确认将该成本卡置为生效状态？', '生效确认', {
    confirmButtonText: '确认生效',
    cancelButtonText: '取消',
    type: 'warning'
  })
  effecting.value = true
  try {
    await CostCardApi.effectCostCard(formData.id!)
    message.success('成本卡已生效')
    await loadData(formData.id!)
  } finally {
    effecting.value = false
  }
}

const handleCopy = async () => {
  try {
    const newId = await CostCardApi.copyCostCard(formData.id!)
    message.success('复制新建草稿卡成功')
    router.push(`/business/cost-card/edit/${newId}`)
    await loadData(newId)
  } catch {}
}

const handleBack = () => {
  router.push('/business/cost-card')
}

const loadData = async (id: number) => {
  loading.value = true
  try {
    const data = await CostCardApi.getCostCard(id)
    Object.assign(formData, data)
    if (data.taxRate != null) {
      taxRatePercent.value = Number((Number(data.taxRate) * 100).toFixed(2))
    }
    if (data.productId) {
      productOptions.value = [
        {
          id: data.productId,
          code: data.productCode || '',
          name: data.productName || ''
        } as ProductVO
      ]
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const idParam = route.params.id
  if (idParam) {
    await loadData(Number(idParam))
  }
})
</script>

<style scoped>
.workspace-header {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 12px;
}
</style>
