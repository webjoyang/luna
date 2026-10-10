<template>
  <ContentWrap>
    <!-- 头部导航与操作 -->
    <div class="workspace-header flex justify-between items-center mb-20px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <el-tag v-if="formData.expired" type="info" effect="plain">已失效</el-tag>
            <dict-tag
              v-else-if="formData.status"
              :type="DICT_TYPE.LUNAERP_QUOTATION_STATUS"
              :value="formData.status"
            />
          </div>
        </template>
      </el-page-header>
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>
        <template v-if="!isReadOnly">
          <el-button
            type="primary"
            :loading="saving"
            @click="handleSave"
            v-hasPermi="['lunaerp:business:quotation:create', 'lunaerp:business:quotation:update']"
          >
            保存草稿
          </el-button>
          <el-button
            v-if="formData.id && formData.status === 'DRAFT'"
            type="success"
            :loading="confirming"
            @click="handleConfirm"
            v-hasPermi="['lunaerp:business:quotation:update']"
          >
            确认报价
          </el-button>
        </template>
        <el-button
          v-if="formData.id && formData.status === 'CONFIRMED'"
          type="warning"
          :loading="revising"
          @click="handleRevise"
          v-hasPermi="['lunaerp:business:quotation:update']"
        >
          修订报价
        </el-button>
        <el-button
          v-if="formData.id && formData.status === 'CONFIRMED'"
          type="success"
          @click="handleToContract"
          v-hasPermi="['lunaerp:business:sales-contract:create']"
        >
          生成销售合同
        </el-button>
        <el-button
          v-if="formData.id"
          type="primary"
          plain
          @click="handlePrint"
          v-hasPermi="['lunaerp:business:quotation:print']"
        >
          打印
        </el-button>
        <el-button
          v-if="formData.id"
          type="info"
          plain
          @click="openVersionsDrawer"
        >
          历史版本
        </el-button>
      </div>
    </div>

    <!-- 状态横幅提醒 -->
    <el-alert
      v-if="formData.status === 'SUPERSEDED'"
      title="当前版本已修订，处于只读归档状态。请点击右上角「历史版本」查看最新版本。"
      type="warning"
      show-icon
      :closable="false"
      class="mb-20px"
    />
    <el-alert
      v-if="formData.status === 'CANCELLED'"
      title="当前报价单已取消，处于只读终态。"
      type="error"
      show-icon
      :closable="false"
      class="mb-20px"
    />

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
            <el-form-item label="报价单号" prop="quotationNo">
              <el-input
                v-model="formData.quotationNo"
                placeholder="留空自动生成 QT{yyyy}NNNN"
                :disabled="!!formData.id"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="版本号">
              <el-tag type="info">v{{ formData.versionNo || 1 }}</el-tag>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报价币种" prop="currency">
              <el-select
                v-model="formData.currency"
                placeholder="选择币种"
                class="!w-full"
                @change="handleCurrencyChange"
              >
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_CURRENCY)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="客户名称" prop="customerId">
              <el-select
                v-model="formData.customerId"
                placeholder="搜索选择客户"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchCustomers"
                :loading="customerLoading"
                class="!w-full"
                @change="handleCustomerChange"
              >
                <el-option
                  v-for="item in customerOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系人" prop="clientContact">
              <el-input v-model="formData.clientContact" placeholder="联系人快照" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="formData.phone" placeholder="电话快照" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="报价日期" prop="quotationDate">
              <el-date-picker
                v-model="formData.quotationDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择报价日期"
                class="!w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效期自" prop="validFrom">
              <el-date-picker
                v-model="formData.validFrom"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择生效日期"
                class="!w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="有效期至" prop="validTo">
              <el-date-picker
                v-model="formData.validTo"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="留空为长期有效"
                clearable
                class="!w-full"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item
              label="外币汇率"
              prop="exchangeRate"
              :rules="[
                { required: formData.currency !== 'RMB', message: '外币报价必须填写汇率', trigger: 'blur' },
                {
                  validator: (_rule: any, value: any, callback: any) => {
                    if (formData.currency !== 'RMB' && (!value || Number(value) <= 0)) {
                      callback(new Error('外币汇率必须大于 0'))
                    } else {
                      callback()
                    }
                  },
                  trigger: 'blur'
                }
              ]"
            >
              <el-input-number
                v-model="formData.exchangeRate"
                :precision="6"
                :step="0.01"
                :min="0.000001"
                :disabled="formData.currency === 'RMB'"
                placeholder="1 外币兑人民币"
                class="!w-full"
                @change="handleExchangeRateChange"
              />
              <span class="text-xs text-gray-400 mt-1 block">
                {{ formData.currency === 'RMB' ? '人民币汇率固定为 1' : `含义: 1 ${formData.currency} = 汇率 人民币` }}
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注" prop="remarks">
              <el-input v-model="formData.remarks" type="textarea" :rows="2" placeholder="请输入备注信息" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 2. 报价明细表格 -->
    <el-card shadow="never" class="mb-20px">
      <template #header>
        <div class="flex justify-between items-center">
          <div class="font-bold flex items-center gap-2">
            <span>报价明细</span>
            <el-tag size="small" type="info">共 {{ (formData.items || []).length }} 行</el-tag>
          </div>
          <div v-if="!isReadOnly" class="flex gap-2">
            <el-button type="primary" plain @click="openProductSelectDialog">
              <Icon icon="ep:plus" class="mr-1" />从产品库添加
            </el-button>
            <el-button plain @click="addManualItem">
              <Icon icon="ep:document-add" class="mr-1" />添加手工行
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="formData.items || []" border stripe>
        <el-table-column label="#" type="index" width="50" align="center" />

        <el-table-column label="品名 / 规格" min-width="200">
          <template #default="{ row }">
            <div class="space-y-1">
              <div class="font-bold text-gray-800">{{ row.productName || '未命名产品' }}</div>
              <div class="text-xs text-gray-400">
                编号: {{ row.productCode || '—' }} | 色号: {{ row.customerColor || '—' }}
              </div>
              <div class="text-xs text-gray-500">
                成分: {{ row.composition || '—' }} | 门幅: {{ row.width || '—' }} | 克重: {{ row.weight || '—' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="单位" width="90" align="center">
          <template #default="{ row }">
            <el-select v-model="row.unit" size="small" :disabled="isReadOnly">
              <el-option label="KG" value="KG" />
              <el-option label="M" value="M" />
              <el-option label="Y" value="Y" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="数量" width="130" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="0.01"
              :precision="2"
              :step="100"
              size="small"
              class="!w-full"
              :disabled="isReadOnly"
              @change="() => recalcRow(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="成本来源" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.costSource === 'CARD'" type="primary" size="small">
              {{ row.costCardNo }}
            </el-tag>
            <el-tag v-else type="warning" size="small" effect="plain">
              无成本卡(手工)
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column v-if="hasAmountPerm" label="RMB成本价" width="130" align="right">
          <template #default="{ row }">
            <el-input-number
              v-if="row.costSource === 'MANUAL'"
              v-model="row.costUnitPriceRmb"
              :min="0"
              :precision="4"
              :step="1"
              size="small"
              class="!w-full"
              :disabled="isReadOnly"
              @change="() => recalcRow(row)"
            />
            <span v-else class="font-mono text-gray-700">
              ¥{{ Number(row.costUnitPriceRmb || 0).toFixed(4) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          v-if="hasAmountPerm && formData.currency !== 'RMB'"
          :label="`折${formData.currency}成本`"
          width="130"
          align="right"
        >
          <template #default="{ row }">
            <span class="font-mono text-gray-700">
              {{ Number(row.costUnitPrice || 0).toFixed(4) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column v-if="hasAmountPerm" label="目标毛利率(%)" width="130" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row._targetMarginPercent"
              :min="-99.99"
              :max="99.99"
              :precision="2"
              :step="5"
              size="small"
              class="!w-full"
              :disabled="isReadOnly"
              placeholder="反算单价"
              @change="() => handleTargetMarginChange(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="报价单价" width="130" align="right">
          <template #default="{ row }">
            <el-input-number
              v-model="row.unitPrice"
              :min="0"
              :precision="4"
              :step="1"
              size="small"
              class="!w-full"
              :disabled="isReadOnly"
              @change="() => handleUnitPriceChange(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="金额小计" width="130" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold">
              {{ formatCurrency(row.amount, formData.currency) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column v-if="hasAmountPerm" label="实际毛利率" width="120" align="right">
          <template #default="{ row }">
            <template v-if="row.marginRate != null">
              <span
                :class="[
                  'font-mono font-medium',
                  row.marginRate < 0 ? 'text-red-600 font-bold' : 'text-gray-700'
                ]"
              >
                {{ (Number(row.marginRate) * 100).toFixed(2) }}%
              </span>
              <el-tag
                v-if="row.lowMarginWarning"
                size="small"
                type="danger"
                class="ml-1 scale-90"
              >
                低
              </el-tag>
            </template>
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="行备注" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" :disabled="isReadOnly" placeholder="备注" />
          </template>
        </el-table-column>

        <el-table-column v-if="!isReadOnly" label="操作" width="70" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="handleRemoveItem($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 3. 整单汇总看板 -->
    <el-card shadow="never" class="mb-20px bg-gray-50 dark:bg-gray-800">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="p-3 bg-white dark:bg-gray-900 rounded border">
          <div class="text-xs text-gray-500 font-bold mb-1">总数量</div>
          <div class="text-xl font-bold font-mono">
            {{ Number(totalSummary.totalQuantity || 0).toFixed(2) }}
          </div>
        </div>

        <div class="p-3 bg-white dark:bg-gray-900 rounded border">
          <div class="text-xs text-gray-500 font-bold mb-1">总金额 ({{ formData.currency }})</div>
          <div class="text-xl font-bold text-primary font-mono">
            {{ formatCurrency(totalSummary.totalAmount, formData.currency) }}
          </div>
        </div>

        <div v-if="hasAmountPerm" class="p-3 bg-white dark:bg-gray-900 rounded border">
          <div class="text-xs text-gray-500 font-bold mb-1">总成本额 ({{ formData.currency }})</div>
          <div class="text-xl font-bold text-gray-700 font-mono">
            {{ formatCurrency(totalSummary.totalCostAmount, formData.currency) }}
          </div>
        </div>

        <div v-if="hasAmountPerm" class="p-3 bg-white dark:bg-gray-900 rounded border">
          <div class="text-xs text-gray-500 font-bold mb-1">整单加权毛利率</div>
          <div
            :class="[
              'text-xl font-bold font-mono',
              totalSummary.avgMarginRate != null && totalSummary.avgMarginRate < 0
                ? 'text-red-600'
                : 'text-success'
            ]"
          >
            {{ totalSummary.avgMarginRate != null ? `${(Number(totalSummary.avgMarginRate) * 100).toFixed(2)}%` : '—' }}
          </div>
        </div>
      </div>
    </el-card>

    <!-- 产品选择对话框 -->
    <el-dialog
      v-model="productSelectDialogVisible"
      title="从产品库选择产品添加到报价单"
      width="750px"
    >
      <div class="mb-10px flex gap-2">
        <el-input
          v-model="productSearchKeyword"
          placeholder="搜索产品编号 / 品名"
          clearable
          @keyup.enter="handleSearchProducts"
        />
        <el-button type="primary" @click="handleSearchProducts">搜索</el-button>
      </div>
      <el-table
        :data="productTableList"
        v-loading="productTableLoading"
        max-height="400"
        stripe
        border
        @selection-change="handleProductSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="编号" prop="code" width="130" />
        <el-table-column label="品名" prop="name" min-width="150" />
        <el-table-column label="成分" prop="composition" min-width="140" show-overflow-tooltip />
        <el-table-column label="结构" prop="fabricConstruction" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fabricConstruction === 'KNIT' ? '针织' : '梭织' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="productSelectDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="selectedProducts.length === 0" @click="confirmSelectProducts">
          加入报价单 ({{ selectedProducts.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 历史版本抽屉 -->
    <el-drawer
      v-model="versionsDrawerVisible"
      :title="`报价单 [${formData.quotationNo}] 历史版本`"
      size="720px"
    >
      <el-table :data="versionsList" stripe border v-loading="versionsLoading">
        <el-table-column label="版本号" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.versionNo === formData.versionNo ? 'success' : 'info'" size="small">
              v{{ row.versionNo }} {{ row.versionNo === formData.versionNo ? '(当前)' : '' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报价日期" align="center" prop="quotationDate" width="120" />
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.expired" type="info" effect="plain">已失效</el-tag>
            <dict-tag
              v-else
              :type="DICT_TYPE.LUNAERP_QUOTATION_STATUS"
              :value="row.status"
            />
          </template>
        </el-table-column>
        <el-table-column label="总金额" align="right" min-width="120">
          <template #default="{ row }">
            <span class="font-mono">{{ formatCurrency(row.totalAmount, row.currency) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="hasAmountPerm"
          label="整单毛利率"
          align="right"
          width="110"
        >
          <template #default="{ row }">
            <span v-if="row.avgMarginRate != null" class="font-mono">
              {{ (Number(row.avgMarginRate) * 100).toFixed(2) }}%
            </span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.id !== formData.id"
              link
              type="primary"
              @click="handleSwitchVersion(row)"
            >
              切换查看
            </el-button>
            <span v-else class="text-xs text-gray-400">当前浏览</span>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- W0 打印分发器 -->
    <PrintDispatcher ref="printDispatcherRef" biz-type="QUOTATION" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import { PrintDispatcher } from '@/components/Print'
import * as QuotationApi from '@/api/lunaerp/business/quotation'
import type { QuotationVO, QuotationItemVO } from '@/api/lunaerp/business/quotation'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import * as CustomerContactApi from '@/api/lunaerp-elmui/baseinfo/customer/contact'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import * as CostCardApi from '@/api/lunaerp/business/cost-card'
import type { CostCardVO } from '@/api/lunaerp/business/cost-card'

defineOptions({ name: 'LunaerpBusinessQuotationEdit' })

interface QuotationItemRow extends QuotationItemVO {
  _targetMarginPercent?: number
}

const route = useRoute()
const router = useRouter()
const message = ElMessage

const loading = ref(false)
const saving = ref(false)
const confirming = ref(false)
const revising = ref(false)
const formRef = ref<FormInstance>()

// 金额脱敏权限
const hasAmountPerm = computed(() => checkPermi(['lunaerp:sensitive:amount']))

// 当前客户的默认毛利率（若客户无设置则 15%）
const defaultMarginRate = ref<number>(0.15)

const formatDate = (d: Date) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const formData = reactive<QuotationVO & { items: QuotationItemRow[] }>({
  id: undefined,
  quotationNo: '',
  versionNo: 1,
  customerId: undefined as unknown as number,
  clientName: '',
  clientContact: '',
  phone: '',
  salespersonUserId: undefined,
  quotationDate: formatDate(new Date()),
  validFrom: formatDate(new Date()),
  validTo: undefined,
  currency: 'RMB',
  exchangeRate: 1.0,
  totalQuantity: 0,
  totalAmount: 0,
  totalCostAmount: 0,
  avgMarginRate: undefined,
  status: 'DRAFT',
  remarks: '',
  items: []
})

const rules: FormRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  quotationDate: [{ required: true, message: '请选择报价日期', trigger: 'change' }],
  validFrom: [{ required: true, message: '请选择有效期自', trigger: 'change' }],
  currency: [{ required: true, message: '请选择币种', trigger: 'change' }]
}

const isReadOnly = computed(() => {
  if (route.query.readonly === '1') return true
  return formData.status && formData.status !== 'DRAFT'
})

const pageTitle = computed(() => {
  if (formData.id) {
    const noDisplay = formData.versionNo ? `${formData.quotationNo}-v${formData.versionNo}` : formData.quotationNo
    return isReadOnly.value ? `报价单详情: ${noDisplay}` : `编辑报价单: ${noDisplay}`
  }
  return '新建报价单'
})

// 客户检索
const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])

const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const data = await CustomerApi.getCustomerPage({ pageNo: 1, pageSize: 20, search: query || '' })
    customerOptions.value = data.list
  } finally {
    customerLoading.value = false
  }
}

const handleCustomerChange = async (customerId: number) => {
  const customer = customerOptions.value.find((c) => c.id === customerId)
  if (!customer) return
  formData.clientName = customer.companyName || ''
  formData.phone = customer.phone || ''

  // 客户默认毛利率（若无则 15%）
  defaultMarginRate.value = customer.defaultMarginRate != null ? Number(customer.defaultMarginRate) : 0.15

  // 带出主联系人
  try {
    const contactPage = await CustomerContactApi.getCustomerContactPage({ customerId, pageNo: 1, pageSize: 10 })
    if (contactPage.list && contactPage.list.length > 0) {
      const primary = contactPage.list.find((c: any) => c.isPrimary === 1) || contactPage.list[0]
      formData.clientContact = primary.name || ''
      if (primary.phone) formData.phone = primary.phone
    }
  } catch {}
}

// 币种切换
const handleCurrencyChange = (curr: string) => {
  if (curr === 'RMB') {
    formData.exchangeRate = 1.0
  }
  recalcAllRows()
}

// 汇率切换
const handleExchangeRateChange = () => {
  recalcAllRows()
}

// 格式化金额
const formatCurrency = (amount: number | undefined, currency: string | undefined) => {
  if (amount == null) return '—'
  const symbol = currency === 'USD' ? '$' : currency === 'EUR' ? '€' : '¥'
  return `${symbol}${Number(amount).toFixed(4)}`
}

// ==================== 明细计算逻辑 ====================

// 反算单价：unitPrice = costUnitPrice / (1 - marginRate)
const handleTargetMarginChange = (row: QuotationItemRow) => {
  const percent = row._targetMarginPercent
  if (percent == null || isNaN(percent)) {
    row.targetMarginRate = undefined
    recalcRow(row)
    return
  }
  const margin = percent / 100
  row.targetMarginRate = margin
  const cost = Number(row.costUnitPrice || 0)

  if (margin < 1) {
    const divisor = 1 - margin
    row.unitPrice = Number((cost / divisor).toFixed(4))
  }
  recalcRow(row)
}

// 修改单价：反算实际毛利率 marginRate = 1 - cost / unitPrice
const handleUnitPriceChange = (row: QuotationItemRow) => {
  recalcRow(row)
}

// 重新计算行数据
const recalcRow = (row: QuotationItemRow) => {
  const fx = Number(formData.exchangeRate || 1)
  const rmbCost = Number(row.costUnitPriceRmb || 0)
  const costUnitPrice = fx > 0 ? Number((rmbCost / fx).toFixed(4)) : 0
  row.costUnitPrice = costUnitPrice

  const qty = Number(row.quantity || 0)
  const unitPrice = Number(row.unitPrice || 0)
  const amount = Number((qty * unitPrice).toFixed(4))
  const costAmount = Number((qty * costUnitPrice).toFixed(4))

  row.amount = amount
  row.costAmount = costAmount

  // 计算实际毛利率
  if (amount > 0) {
    const margin = Number(((amount - costAmount) / amount).toFixed(6))
    row.marginRate = margin
  } else {
    row.marginRate = undefined
  }
}

// 重新计算所有行
const recalcAllRows = () => {
  for (const row of formData.items || []) {
    recalcRow(row)
  }
}

// 整单汇总计算
const totalSummary = computed(() => {
  let qty = 0
  let amt = 0
  let costAmt = 0

  for (const row of formData.items || []) {
    qty += Number(row.quantity || 0)
    amt += Number(row.amount || 0)
    costAmt += Number(row.costAmount || 0)
  }

  const avgMargin = amt > 0 ? Number(((amt - costAmt) / amt).toFixed(6)) : undefined

  return {
    totalQuantity: Number(qty.toFixed(2)),
    totalAmount: Number(amt.toFixed(4)),
    totalCostAmount: Number(costAmt.toFixed(4)),
    avgMarginRate: avgMargin
  }
})

// ==================== 产品选择与明细操作 ====================

const productSelectDialogVisible = ref(false)
const productSearchKeyword = ref('')
const productTableList = ref<ProductVO[]>([])
const productTableLoading = ref(false)
const selectedProducts = ref<ProductVO[]>([])

const openProductSelectDialog = () => {
  productSelectDialogVisible.value = true
  selectedProducts.value = []
  handleSearchProducts()
}

const handleSearchProducts = async () => {
  productTableLoading.value = true
  try {
    const res = await ProductApi.getProductPage({
      pageNo: 1,
      pageSize: 30,
      search: productSearchKeyword.value.trim()
    })
    productTableList.value = res.list || []
  } finally {
    productTableLoading.value = false
  }
}

const handleProductSelectionChange = (selection: ProductVO[]) => {
  selectedProducts.value = selection
}

const confirmSelectProducts = async () => {
  for (const prod of selectedProducts.value) {
    // 异步拉取该产品当前 EFFECTIVE 成本卡
    let effectiveCard: CostCardVO | null = null
    try {
      effectiveCard = await CostCardApi.getEffectiveCostCardByProductId(prod.id!)
    } catch {}

    const hasCard = !!effectiveCard
    const rmbCost = hasCard
      ? Number(effectiveCard.basis === 'METER' ? effectiveCard.unitCostPerMeter : effectiveCard.unitCostPerKg) || 0
      : 0

    const fx = Number(formData.exchangeRate || 1)
    const costUnitPrice = fx > 0 ? Number((rmbCost / fx).toFixed(4)) : 0
    const margin = defaultMarginRate.value
    const marginPercent = Number((margin * 100).toFixed(2))

    let unitPrice = 0
    if (margin < 1) {
      unitPrice = Number((costUnitPrice / (1 - margin)).toFixed(4))
    }

    const row: QuotationItemRow = {
      productId: prod.id,
      productCode: prod.code,
      productName: prod.name,
      customerColor: '',
      composition: prod.composition,
      width: prod.finishedWidthCm ? `${prod.finishedWidthCm}CM` : '',
      weight: prod.finishedWeightGsm ? `${prod.finishedWeightGsm}GSM` : '',
      unit: effectiveCard?.basis || 'KG',
      quantity: 100,
      costCardId: hasCard ? effectiveCard!.id : undefined,
      costCardNo: hasCard ? effectiveCard!.cardNo : undefined,
      costSource: hasCard ? 'CARD' : 'MANUAL',
      costSnapshot: hasCard ? JSON.stringify(effectiveCard) : undefined,
      costUnitPriceRmb: rmbCost,
      costUnitPrice: costUnitPrice,
      targetMarginRate: margin,
      _targetMarginPercent: marginPercent,
      unitPrice: unitPrice,
      amount: Number((100 * unitPrice).toFixed(4)),
      costAmount: Number((100 * costUnitPrice).toFixed(4)),
      marginRate: margin,
      remark: ''
    }
    formData.items.push(row)
  }
  productSelectDialogVisible.value = false
}

const addManualItem = () => {
  const margin = defaultMarginRate.value
  const marginPercent = Number((margin * 100).toFixed(2))

  const row: QuotationItemRow = {
    productName: '',
    unit: 'KG',
    quantity: 100,
    costSource: 'MANUAL',
    costUnitPriceRmb: 0,
    costUnitPrice: 0,
    targetMarginRate: margin,
    _targetMarginPercent: marginPercent,
    unitPrice: 0,
    amount: 0,
    costAmount: 0,
    remark: ''
  }
  formData.items.push(row)
}

const handleRemoveItem = (index: number) => {
  formData.items.splice(index, 1)
}

// ==================== 保存与提交 ====================

const handleSave = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  if (!formData.items || formData.items.length === 0) {
    message.warning('请至少添加一行报价明细')
    return
  }

  for (let i = 0; i < formData.items.length; i++) {
    const item = formData.items[i]
    if (!item.productName) {
      message.warning(`第 ${i + 1} 行品名不能为空`)
      return
    }
    if (!item.quantity || item.quantity <= 0) {
      message.warning(`第 ${i + 1} 行数量必须大于 0`)
      return
    }
  }

  saving.value = true
  try {
    const payload: QuotationVO = {
      ...formData,
      items: formData.items.map((row) => ({
        ...row,
        targetMarginRate: row._targetMarginPercent != null ? row._targetMarginPercent / 100 : undefined
      }))
    }

    if (formData.id) {
      await QuotationApi.updateQuotation(payload)
      message.success('更新成功')
    } else {
      const id = await QuotationApi.createQuotation(payload)
      message.success('保存草稿成功')
      router.replace(`/business/quotation/edit?id=${id}`)
      loadDetail(id)
    }
  } finally {
    saving.value = false
  }
}

const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm('确定要确认此报价单吗？确认后将不可修改。', '提示', { type: 'warning' })
    confirming.value = true
    await QuotationApi.confirmQuotation(formData.id!)
    message.success('报价单确认成功')
    loadDetail(formData.id!)
  } catch {} finally {
    confirming.value = false
  }
}

const handleRevise = async () => {
  try {
    await ElMessageBox.confirm('确定对已确认报价进行修订吗？旧版本将标记为已修订，并生成新版本草稿。', '修订确认', {
      type: 'primary'
    })
    revising.value = true
    const newId = await QuotationApi.reviseQuotation(formData.id!)
    message.success('已生成新版本草稿')
    router.replace(`/business/quotation/edit?id=${newId}`)
    loadDetail(newId)
  } catch {} finally {
    revising.value = false
  }
}

const handleBack = () => {
  router.push('/business/quotation')
}

// 加载详情
const loadDetail = async (id: number) => {
  loading.value = true
  try {
    const data = await QuotationApi.getQuotation(id)
    Object.assign(formData, data)
    formData.items = (data.items || []).map((item) => ({
      ...item,
      _targetMarginPercent: item.targetMarginRate != null ? Number((item.targetMarginRate * 100).toFixed(2)) : undefined
    }))
    if (data.customerId) {
      customerOptions.value = [{ id: data.customerId, companyName: data.clientName || '' } as CustomerVO]
    }
  } finally {
    loading.value = false
  }
}

// 历史版本抽屉
const versionsDrawerVisible = ref(false)
const versionsLoading = ref(false)
const versionsList = ref<QuotationVO[]>([])

const openVersionsDrawer = async () => {
  if (!formData.quotationNo) return
  versionsDrawerVisible.value = true
  versionsLoading.value = true
  try {
    versionsList.value = await QuotationApi.getQuotationVersions(formData.quotationNo)
  } finally {
    versionsLoading.value = false
  }
}

const handleSwitchVersion = (row: QuotationVO) => {
  versionsDrawerVisible.value = false
  router.push(`/business/quotation/edit?id=${row.id}&readonly=${row.status !== 'DRAFT' ? 1 : 0}`)
  loadDetail(row.id!)
}

const printDispatcherRef = ref<InstanceType<typeof PrintDispatcher> | null>(null)

const handleToContract = () => {
  router.push({
    path: '/business/sales-contract/edit',
    query: { quotationId: formData.id }
  })
}

const handlePrint = () => {
  printDispatcherRef.value?.open({ bizId: formData.id })
}

onMounted(async () => {
  searchCustomers('')
  const id = route.query.id
  if (id) {
    loadDetail(Number(id))
  } else {
    try {
      formData.quotationNo = await QuotationApi.getNextQuotationNo()
    } catch {}
  }
})
</script>

<style scoped>
.workspace-header {
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 12px;
}
</style>
