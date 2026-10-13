<template>
  <ContentWrap>
    <div class="edit-header mb-20px">
      <el-page-header @back="goBack">
        <template #content>
          <span class="text-large font-600 mr-3">
            {{ isCreateMode ? '新建打色指示单' : (isViewMode ? `查看打色指示单 #${id}` : `编辑打色指示单 #${id}`) }}
          </span>
          <DictTag
            v-if="formData.status"
            :type="DICT_TYPE.LUNAERP_COLOR_INSTRUCTION_STATUS"
            :value="formData.status"
            class="ml-2"
          />
        </template>
        <template #extra>
          <div class="flex items-center gap-2">
            <el-button
              v-if="!isViewMode && formData.id && formData.status === 'PENDING'"
              type="success"
              plain
              :loading="starting"
              @click="handleStartInstruction"
              v-hasPermi="['lunaerp:production:color-instruction:update']"
            >
              <Icon icon="ep:video-play" class="mr-1" /> 开始打色
            </el-button>
            <el-button v-if="!isViewMode" type="primary" :loading="submitting" @click="handleSave">
              <Icon icon="ep:check" class="mr-1" /> 保存
            </el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </template>
      </el-page-header>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :disabled="isViewMode"
      label-width="110px"
      v-loading="loading"
    >
      <!-- 基本信息卡片 -->
      <el-card shadow="never" class="mb-20px">
        <template #header>
          <span class="font-bold text-16px">单据头信息</span>
        </template>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="打色单号" prop="instructionNo">
              <el-input
                v-model="formData.instructionNo"
                maxlength="32"
                placeholder="留空自动生成（如 DS20261013001）"
              >
                <template v-if="isCreateMode" #append>
                  <el-button :loading="nextNoLoading" @click="handleGetNextNo">获取编号</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单据日期" prop="instructionDate">
              <el-date-picker
                v-model="formData.instructionDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择单据日期"
                class="!w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="要求完成日" prop="deliveryDate">
              <el-date-picker
                v-model="formData.deliveryDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择要求完成日"
                class="!w-full"
              />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="客户" prop="customerId">
              <el-select
                v-model="formData.customerId"
                placeholder="输入关键词搜索客户（必填）"
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
            <el-form-item label="客户联系人" prop="contactPerson">
              <el-autocomplete
                v-model="formData.contactPerson"
                :fetch-suggestions="queryContactSuggestions"
                placeholder="可选择联系人或自由手填"
                clearable
                class="!w-full"
              >
                <template #default="{ item }">
                  <div class="flex justify-between items-center">
                    <span>{{ item.value }}</span>
                    <span v-if="item.phone" class="text-gray-400 text-xs">{{ item.phone }}</span>
                  </div>
                </template>
              </el-autocomplete>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="打色承接方" prop="labFactoryId">
              <el-select
                v-model="formData.labFactoryId"
                placeholder="留空即本厂打色房自做"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchSuppliers"
                :loading="supplierLoading"
                class="!w-full"
                @change="handleSupplierChange"
              >
                <el-option
                  v-for="item in supplierOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                >
                  <span>{{ item.supplierNo }} {{ item.companyName }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="产品档案" prop="productId">
              <el-select
                v-model="formData.productId"
                placeholder="输入关键词搜索产品档案（强关联）"
                clearable
                filterable
                remote
                reserve-keyword
                :remote-method="searchProducts"
                :loading="productLoading"
                class="!w-full"
                @change="handleProductChange"
              >
                <el-option
                  v-for="item in productOptions"
                  :key="item.id"
                  :label="`${item.code} - ${item.name}`"
                  :value="item.id!"
                >
                  <span class="font-mono mr-2">{{ item.code }}</span>
                  <span>{{ item.name }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成分快照" prop="composition">
              <el-input v-model="formData.composition" placeholder="产品带出，可修改" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="幅宽快照" prop="width">
              <el-input v-model="formData.width" placeholder="如 180cm" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="克重快照" prop="gsm">
              <el-input v-model="formData.gsm" placeholder="如 200g/m²" clearable />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="备注说明" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                placeholder="请输入指示单备注说明"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 颜色明细卡片 -->
      <el-card shadow="never" class="mb-20px">
        <template #header>
          <div class="flex justify-between items-center">
            <span class="font-bold text-16px">颜色行明细（打色要求）</span>
            <el-button v-if="!isViewMode" type="primary" plain size="small" @click="addColorLine">
              <Icon icon="ep:plus" class="mr-1" /> 添加颜色行
            </el-button>
          </div>
        </template>

        <el-table
          :data="formData.colors"
          :row-key="getRowKey"
          :row-class-name="tableRowClassName"
          border
          stripe
        >
          <el-table-column label="#" type="index" width="50" align="center" />
          <el-table-column label="轮次" width="70" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'REJECTED' ? 'danger' : 'info'">
                R{{ row.round || 1 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="85" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getColorTagType(row.status)">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="客户色号 *" min-width="120">
            <template #default="{ row }">
              <el-input
                v-model="row.customerColorNo"
                :disabled="isViewMode || isRowFrozen(row)"
                :class="{ 'line-through-input': row.status === 'REJECTED' }"
                placeholder="如 C01"
              />
            </template>
          </el-table-column>
          <el-table-column label="颜色名称 *" min-width="120">
            <template #default="{ row }">
              <el-input
                v-model="row.colorName"
                :disabled="isViewMode || isRowFrozen(row)"
                :class="{ 'line-through-input': row.status === 'REJECTED' }"
                placeholder="如 藏青"
              />
            </template>
          </el-table-column>
          <el-table-column label="厂内色号" min-width="100">
            <template #default="{ row }">
              <el-input v-model="row.factoryColorNo" :disabled="isViewMode || isRowFrozen(row)" placeholder="如 F-01" />
            </template>
          </el-table-column>
          <el-table-column label="色值" width="120" align="center">
            <template #default="{ row }">
              <div class="flex items-center gap-1 justify-center">
                <el-color-picker v-model="row.colorHex" :disabled="isViewMode || isRowFrozen(row)" size="small" />
                <span class="text-xs font-mono text-gray-500">{{ row.colorHex || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="主灯源 *" min-width="110">
            <template #default="{ row }">
              <el-select v-model="row.lightPrimary" :disabled="isViewMode || isRowFrozen(row)" placeholder="选择主灯源">
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="辅灯源" min-width="110">
            <template #default="{ row }">
              <el-select v-model="row.lightSecondary" :disabled="isViewMode || isRowFrozen(row)" clearable placeholder="可选辅灯源">
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="色卡参考" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.colorCardRef" :disabled="isViewMode || isRowFrozen(row)" placeholder="如 Pantone TPX 19-4052" />
            </template>
          </el-table-column>
          <el-table-column label="样布照片" min-width="150">
            <template #default="{ row }">
              <UploadImgs
                v-model="row.samplePhotos"
                :disabled="isViewMode || isRowFrozen(row)"
                :limit="5"
                height="60px"
                width="60px"
              />
            </template>
          </el-table-column>
          <el-table-column label="份数" width="90" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.copies"
                :disabled="isViewMode || isRowFrozen(row)"
                :min="1"
                :precision="0"
                controls-position="right"
                class="!w-full"
              />
            </template>
          </el-table-column>
          <el-table-column label="驳回意见" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.rejectReason" class="text-red-500 font-medium text-xs">{{ row.rejectReason }}</span>
              <span v-else class="text-gray-400 text-xs">-</span>
            </template>
          </el-table-column>
          <el-table-column label="行备注" min-width="110">
            <template #default="{ row }">
              <el-input v-model="row.remark" :disabled="isViewMode || isRowFrozen(row)" placeholder="备注说明" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row, $index }">
              <div class="flex items-center justify-center gap-1 flex-wrap">
                <!-- 行状态流转动作（针对已持久化行） -->
                <el-button
                  v-if="row.id && row.status === 'PENDING' && canDoLineAction"
                  link
                  type="success"
                  size="small"
                  v-hasPermi="['lunaerp:production:color-instruction:confirm']"
                  @click="handleApproveLine(row)"
                >
                  通过
                </el-button>
                <el-button
                  v-if="row.id && (row.status === 'PENDING' || row.status === 'APPROVED') && canDoLineAction"
                  link
                  type="warning"
                  size="small"
                  v-hasPermi="['lunaerp:production:color-instruction:confirm']"
                  @click="openRejectDialog(row)"
                >
                  驳回重打
                </el-button>
                <span v-if="row.status === 'REJECTED'" class="text-gray-400 text-xs select-none">已驳回</span>

                <!-- 基础编辑动作（仅非查看模式且未冻结行） -->
                <template v-if="!isViewMode && !isRowFrozen(row)">
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="$index === 0"
                    @click="moveColorLine($index, -1)"
                    title="上移"
                  >
                    <Icon icon="ep:top" />
                  </el-button>
                  <el-button
                    link
                    type="primary"
                    size="small"
                    :disabled="$index === formData.colors.length - 1"
                    @click="moveColorLine($index, 1)"
                    title="下移"
                  >
                    <Icon icon="ep:bottom" />
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    size="small"
                    @click="removeColorLine($index)"
                    title="删除"
                  >
                    <Icon icon="ep:delete" />
                  </el-button>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-form>

    <!-- 驳回重打意见弹窗 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="驳回重打颜色行"
      width="480px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="rejectFormRef" :model="rejectFormData" :rules="rejectFormRules" label-width="90px">
        <el-form-item label="颜色信息">
          <span class="font-medium text-gray-700">
            {{ activeRejectRow?.customerColorNo }} {{ activeRejectRow?.colorName }} (第 {{ activeRejectRow?.round || 1 }} 轮)
          </span>
        </el-form-item>
        <el-form-item label="驳回意见" prop="rejectReason">
          <el-input
            v-model="rejectFormData.rejectReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入驳回意见（如：颜色偏深，需调浅半度）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="danger" :loading="rejectSubmitting" @click="handleConfirmReject">
            确认驳回并克隆新一轮
          </el-button>
        </div>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { useMessage } from '@/hooks/web/useMessage'
import { UploadImgs } from '@/components/UploadFile'
import * as ColorInstructionApi from '@/api/lunaerp-elmui/production/color-instruction'
import type { ColorInstructionVO, ColorInstructionColorVO } from '@/api/lunaerp-elmui/production/color-instruction'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import * as CustomerContactApi from '@/api/lunaerp-elmui/baseinfo/customer/contact'
import type { CustomerContactVO } from '@/api/lunaerp-elmui/baseinfo/customer/contact'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineOptions({ name: 'LunaerpElmuiProductionColorInstructionEdit' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const starting = ref(false)
const nextNoLoading = ref(false)

const id = computed(() => {
  const val = route.params.id
  return val ? Number(val) : undefined
})

const isCreateMode = computed(() => !id.value)
const isViewMode = computed(() => {
  if (route.query.mode === 'view') {
    return true
  }
  // 非可编辑态（非 PENDING / IN_PROGRESS）自动进入只读模式
  if (formData.status && formData.status !== 'PENDING' && formData.status !== 'IN_PROGRESS') {
    return true
  }
  return false
})

const canDoLineAction = computed(() => !isViewMode.value && (formData.status === 'PENDING' || formData.status === 'IN_PROGRESS'))

// 表单数据
const formData = reactive<ColorInstructionVO>({
  instructionNo: '',
  instructionDate: dayjs().format('YYYY-MM-DD'),
  deliveryDate: undefined,
  customerId: undefined,
  clientName: '',
  contactPerson: '',
  productId: undefined,
  productCode: '',
  productName: '',
  labFactoryId: undefined,
  labFactoryName: '',
  composition: '',
  width: '',
  gsm: '',
  status: 'PENDING',
  remark: '',
  colors: []
})

// 表单校验规则
const formRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  productId: [{ required: true, message: '请选择产品档案', trigger: 'change' }],
  instructionDate: [{ required: true, message: '请选择单据日期', trigger: 'change' }]
}

// 候选选项
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

// 客户联系人候选
const contactCandidates = ref<{ value: string; phone?: string }[]>([])
const loadCustomerContacts = async (customerId?: number) => {
  if (!customerId) {
    contactCandidates.value = []
    return
  }
  try {
    const res = await CustomerContactApi.getCustomerContactPage({
      pageNo: 1,
      pageSize: 50,
      customerId
    })
    const list = res.list || []
    contactCandidates.value = list.map((item: CustomerContactVO) => ({
      value: item.name + (item.phone ? ` ${item.phone}` : ''),
      phone: item.phone
    }))
    // 若当前未填联系人且存在主联系人，则自动带入
    if (!formData.contactPerson) {
      const primary = list.find((c: CustomerContactVO) => c.isPrimary === 1) || list[0]
      if (primary) {
        formData.contactPerson = primary.name + (primary.phone ? ` ${primary.phone}` : '')
      }
    }
  } catch {
    contactCandidates.value = []
  }
}

const queryContactSuggestions = (queryString: string, cb: (results: any[]) => void) => {
  const results = queryString
    ? contactCandidates.value.filter((c) => c.value.toLowerCase().includes(queryString.toLowerCase()))
    : contactCandidates.value
  cb(results)
}

const handleCustomerChange = (val?: number) => {
  formData.contactPerson = ''
  if (val) {
    const found = customerOptions.value.find((c) => c.id === val)
    if (found) {
      formData.clientName = found.companyName
    }
    loadCustomerContacts(val)
  } else {
    formData.clientName = ''
    contactCandidates.value = []
  }
}

// 产品候选
const productOptions = ref<ProductVO[]>([])
const productLoading = ref(false)
const searchProducts = async (query: string) => {
  productLoading.value = true
  try {
    const res = await ProductApi.getProductPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    productOptions.value = res.list || []
  } catch {
    productOptions.value = []
  } finally {
    productLoading.value = false
  }
}

const handleProductChange = (val?: number) => {
  if (val) {
    const found = productOptions.value.find((p) => p.id === val)
    if (found) {
      formData.productCode = found.code
      formData.productName = found.name
      formData.composition = found.composition || ''
      formData.width = found.widthCm ? `${found.widthCm}cm` : ''
      formData.gsm = found.weightGsm ? `${found.weightGsm}g/m²` : ''
    }
  } else {
    formData.productCode = ''
    formData.productName = ''
    formData.composition = ''
    formData.width = ''
    formData.gsm = ''
  }
}

// 供应商候选（打色承接方）
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
    supplierOptions.value = res.list || []
  } catch {
    supplierOptions.value = []
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierChange = (val?: number) => {
  if (val) {
    const found = supplierOptions.value.find((s) => s.id === val)
    formData.labFactoryName = found ? found.companyName : ''
  } else {
    formData.labFactoryName = ''
  }
}

// 获取下一个编号预览
const handleGetNextNo = async () => {
  nextNoLoading.value = true
  try {
    const nextNo = await ColorInstructionApi.getNextInstructionNo()
    formData.instructionNo = nextNo
  } finally {
    nextNoLoading.value = false
  }
}

// 颜色行唯一标识与状态判断
let tempIdCounter = 0
const getRowKey = (row: ColorInstructionColorVO & { _tempId?: string }) => {
  if (row.id) {
    return `id_${row.id}`
  }
  if (!row._tempId) {
    row._tempId = `temp_${++tempIdCounter}`
  }
  return row._tempId
}

const isRowFrozen = (row: ColorInstructionColorVO) => {
  return row.status === 'APPROVED' || row.status === 'REJECTED'
}

const getColorTagType = (status?: string): 'success' | 'danger' | 'info' | '' => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PENDING') return 'info'
  return ''
}

const getStatusLabel = (status?: string) => {
  if (status === 'APPROVED') return '通过'
  if (status === 'REJECTED') return '驳回'
  if (status === 'PENDING') return '待结果'
  return status || '-'
}

const tableRowClassName = ({ row, rowIndex }: { row: ColorInstructionColorVO; rowIndex: number }) => {
  const classes: string[] = []
  if (row.status === 'REJECTED') {
    classes.push('row-rejected')
  } else if (isRowFrozen(row)) {
    classes.push('row-frozen')
  }
  // 色组视觉分割：当当前行客户色号与上一行不同时，增加组分割线
  if (
    rowIndex > 0 &&
    formData.colors &&
    formData.colors[rowIndex]?.customerColorNo &&
    formData.colors[rowIndex]?.customerColorNo !== formData.colors[rowIndex - 1]?.customerColorNo
  ) {
    classes.push('row-group-start')
  }
  return classes.join(' ')
}

const deletedColorIds = ref<number[]>([])

// 颜色行操作
const addColorLine = () => {
  if (!formData.colors) {
    formData.colors = []
  }
  formData.colors.push({
    customerColorNo: '',
    colorName: '',
    factoryColorNo: '',
    colorHex: '',
    lightPrimary: 'D65',
    lightSecondary: '',
    colorCardRef: '',
    samplePhotos: [],
    copies: 2,
    round: 1,
    status: 'PENDING',
    remark: ''
  })
}

// 调序（上移/下移）
const moveColorLine = (index: number, direction: number) => {
  if (!formData.colors) return
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= formData.colors.length) return
  const current = formData.colors[index]
  const target = formData.colors[targetIndex]
  formData.colors.splice(index, 1, target)
  formData.colors.splice(targetIndex, 1, current)
}

// 删除颜色行（已冻结行不可删）
const removeColorLine = (index: number) => {
  if (!formData.colors) return
  if (formData.colors.length <= 1) {
    message.warning('打色单至少需要保留一行颜色明细')
    return
  }
  const removed = formData.colors[index]
  if (isRowFrozen(removed)) {
    message.warning('已冻结行不可删除')
    return
  }
  if (removed.id) {
    deletedColorIds.value.push(removed.id)
  }
  formData.colors.splice(index, 1)
}

// 保存
const handleSave = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  if (!formData.colors || formData.colors.length === 0) {
    message.warning('请至少添加一行打色颜色明细')
    return
  }

  for (let i = 0; i < formData.colors.length; i++) {
    const line = formData.colors[i]
    if (!line.customerColorNo?.trim()) {
      message.warning(`第 ${i + 1} 行颜色明细的客户色号不能为空`)
      return
    }
    if (!line.colorName?.trim()) {
      message.warning(`第 ${i + 1} 行颜色明细的颜色名称不能为空`)
      return
    }
    if (!line.lightPrimary?.trim()) {
      message.warning(`第 ${i + 1} 行颜色明细的主灯源不能为空`)
      return
    }
  }

  submitting.value = true
  try {
    const payload = {
      ...formData,
      deletedColorIds: deletedColorIds.value.length > 0 ? deletedColorIds.value : undefined
    }
    if (isCreateMode.value) {
      await ColorInstructionApi.createColorInstruction(payload)
      message.success('创建打色指示单成功')
    } else {
      await ColorInstructionApi.updateColorInstruction(payload)
      message.success('更新打色指示单成功')
    }
    router.push('/production/color-instruction')
  } finally {
    submitting.value = false
  }
}

// 返回
const goBack = () => {
  router.push('/production/color-instruction')
}

// 开始打色（PENDING -> IN_PROGRESS）
const handleStartInstruction = async () => {
  if (!formData.id) return
  try {
    await message.confirm(`确定将打色指示单【${formData.instructionNo || formData.id}】开始打色（状态转为打色中）吗？`)
    starting.value = true
    await ColorInstructionApi.startColorInstruction({ id: formData.id })
    message.success('已开始打色')
    await loadData(formData.id)
  } catch {
  } finally {
    starting.value = false
  }
}

// 通过颜色行
const handleApproveLine = async (row: ColorInstructionColorVO) => {
  if (!row.id) return
  try {
    await message.confirm(`确定通过颜色行【${row.customerColorNo} ${row.colorName}】吗？`)
    await ColorInstructionApi.approveColorLine({ id: row.id })
    message.success('颜色行已通过')
    if (formData.id) {
      await loadData(formData.id)
    }
  } catch {}
}

// 驳回重打弹窗逻辑
const rejectDialogVisible = ref(false)
const rejectSubmitting = ref(false)
const rejectFormRef = ref()
const activeRejectRow = ref<ColorInstructionColorVO | null>(null)
const rejectFormData = reactive({
  rejectReason: ''
})
const rejectFormRules = {
  rejectReason: [{ required: true, message: '请输入驳回意见', trigger: 'blur' }]
}

const openRejectDialog = (row: ColorInstructionColorVO) => {
  activeRejectRow.value = row
  rejectFormData.rejectReason = ''
  rejectDialogVisible.value = true
}

const handleConfirmReject = async () => {
  if (!rejectFormRef.value) return
  const valid = await rejectFormRef.value.validate()
  if (!valid) return
  if (!activeRejectRow.value?.id) return

  rejectSubmitting.value = true
  try {
    await ColorInstructionApi.rejectRestrikeColorLine({
      id: activeRejectRow.value.id,
      rejectReason: rejectFormData.rejectReason
    })
    message.success('已驳回并克隆生成新一轮打色行')
    rejectDialogVisible.value = false
    if (formData.id) {
      await loadData(formData.id)
    }
  } finally {
    rejectSubmitting.value = false
  }
}

// 加载已有数据
const loadData = async (loadId: number) => {
  loading.value = true
  try {
    const data = await ColorInstructionApi.getColorInstruction(loadId)
    Object.assign(formData, data)
    // 确保 colors 是数组
    formData.colors = data.colors || []
    // 回显客户/产品/供应商候选
    if (data.customerId) {
      customerOptions.value = [{ id: data.customerId, companyName: data.clientName || '' } as CustomerVO]
      loadCustomerContacts(data.customerId)
    }
    if (data.productId) {
      productOptions.value = [
        { id: data.productId, code: data.productCode || '', name: data.productName || '' } as ProductVO
      ]
    }
    if (data.labFactoryId) {
      supplierOptions.value = [
        { id: data.labFactoryId, companyName: data.labFactoryName || '' } as SupplierVO
      ]
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (id.value) {
    loadData(id.value)
  } else {
    // 新建模式初始化 1 行颜色
    addColorLine()
    searchCustomers('')
    searchProducts('')
    searchSuppliers('')
  }
})
</script>

<style scoped>
.edit-header {
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 12px;
}
:deep(.row-frozen) {
  background-color: var(--el-fill-color-light) !important;
  opacity: 0.85;
}
:deep(.row-rejected) {
  background-color: var(--el-fill-color-lighter) !important;
  opacity: 0.75;
}
:deep(.row-rejected td) {
  background-color: var(--el-fill-color-lighter) !important;
}
:deep(.row-rejected input),
:deep(.row-rejected .el-input__inner),
:deep(.row-rejected .el-select__selected-item),
:deep(.row-rejected span) {
  text-decoration: line-through;
  color: var(--el-text-color-placeholder) !important;
}
:deep(.row-rejected .el-tag span) {
  text-decoration: none !important;
}
:deep(.row-group-start td) {
  border-top: 2px solid var(--el-border-color) !important;
}
:deep(.line-through-input input),
:deep(.line-through-input .el-input__inner) {
  text-decoration: line-through;
  color: var(--el-text-color-placeholder) !important;
}
</style>
