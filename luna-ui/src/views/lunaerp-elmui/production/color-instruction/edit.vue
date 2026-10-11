<template>
  <ContentWrap>
    <div class="edit-header mb-4 flex items-center justify-between">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center space-x-3">
            <span class="header-title">{{ pageTitle }}</span>
            <el-tag v-if="formData.status" :type="getDocStatusTagType(formData.status)">
              {{ getDocStatusLabel(formData.status) }}
            </el-tag>
          </div>
        </template>
      </el-page-header>
      <div class="header-actions flex items-center space-x-2">
        <el-button
          v-if="formData.status === 'PENDING' && id"
          type="success"
          plain
          :loading="startLoading"
          v-hasPermi="['lunaerp:production:color-instruction:update']"
          @click="handleStart"
        >
          开始打色
        </el-button>
        <el-button v-if="isViewMode" @click="handleBack">返回</el-button>
        <template v-else>
          <el-button @click="handleBack">取消</el-button>
          <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
        </template>
      </div>
    </div>

    <!-- 表单区 -->
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      v-loading="loading"
    >
      <!-- 基本信息 -->
      <el-card shadow="never" class="mb-4">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="card-title font-bold text-gray-700">基本信息</span>
            <span v-if="formData.status" class="text-xs text-gray-500">
              单据状态：<el-tag size="small" :type="getDocStatusTagType(formData.status)">{{ getDocStatusLabel(formData.status) }}</el-tag>
            </span>
          </div>
        </template>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="打色单号" prop="instructionNo">
              <el-input
                v-model="formData.instructionNo"
                maxlength="32"
                placeholder="留空自动生成（如 DS20261010001）"
                :disabled="isViewMode"
              >
                <template #append v-if="!isViewMode && !id">
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
                :disabled="isViewMode"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="要求完成日" prop="deliveryDate">
              <el-date-picker
                v-model="formData.deliveryDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择交货日期（可空）"
                class="!w-full"
                :disabled="isViewMode"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="客户档案" prop="customerId">
              <el-select
                v-model="formData.customerId"
                placeholder="搜索选择客户（必填）"
                clearable
                filterable
                remote
                :remote-method="searchCustomers"
                :loading="customerLoading"
                class="!w-full"
                :disabled="isViewMode"
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
              <el-select
                v-if="contactOptions.length > 0"
                v-model="formData.contactPerson"
                placeholder="选择或输入联系人"
                filterable
                allow-create
                default-first-option
                clearable
                class="!w-full"
                :disabled="isViewMode"
              >
                <el-option
                  v-for="contact in contactOptions"
                  :key="contact.id"
                  :label="contact.name + (contact.phone ? ' (' + contact.phone + ')' : '')"
                  :value="contact.name"
                />
              </el-select>
              <el-input
                v-else
                v-model="formData.contactPerson"
                placeholder="客户联系人及电话"
                maxlength="64"
                :disabled="isViewMode"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="承接方" prop="labFactoryId">
              <el-select
                v-model="formData.labFactoryId"
                placeholder="本厂打色房自做（可空）"
                clearable
                filterable
                remote
                :remote-method="searchSuppliers"
                :loading="supplierLoading"
                class="!w-full"
                :disabled="isViewMode"
                @change="handleSupplierChange"
              >
                <el-option
                  v-for="item in supplierOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="产品档案" prop="productId">
              <el-select
                v-model="formData.productId"
                placeholder="搜索选择产品（必填）"
                clearable
                filterable
                remote
                :remote-method="searchProducts"
                :loading="productLoading"
                class="!w-full"
                :disabled="isViewMode"
                @change="handleProductChange"
              >
                <el-option
                  v-for="product in productOptions"
                  :key="product.id"
                  :label="product.code + ' ' + product.name"
                  :value="product.id!"
                >
                  <span class="font-mono">{{ product.code }}</span>
                  <span class="ml-2 text-gray-600">{{ product.name }}</span>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品编号" prop="productCode">
              <el-input v-model="formData.productCode" placeholder="产品编号快照" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="formData.productName" placeholder="产品名称快照" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="成分" prop="composition">
              <el-input v-model="formData.composition" placeholder="成分快照，如 95%棉 5%氨纶" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="幅宽" prop="width">
              <el-input v-model="formData.width" placeholder="幅宽快照，如 180cm" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="克重" prop="gsm">
              <el-input v-model="formData.gsm" placeholder="克重快照，如 200gsm" :disabled="isViewMode" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="24">
            <el-form-item label="单据备注" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入单据备注"
                maxlength="500"
                show-word-limit
                :disabled="isViewMode"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 颜色明细 -->
      <el-card shadow="never" class="mb-4">
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-2">
              <span class="card-title font-bold text-gray-700">颜色明细</span>
              <span class="text-xs text-gray-400">（历史轮次行置灰冻结不可修改或删除，保存时自动按色号分组重排）</span>
            </div>
            <el-button v-if="!isViewMode" type="primary" plain @click="handleAddColorRow">
              <Icon icon="ep:plus" class="mr-1" />添加颜色
            </el-button>
          </div>
        </template>

        <el-table
          :data="formData.colors"
          :row-key="(row: ColorInstructionColorVO) => row.id || row._tempId"
          :row-class-name="tableRowClassName"
          border
          stripe
          class="w-full"
        >
          <el-table-column label="#" type="index" width="50" align="center" />
          <el-table-column label="轮次" width="70" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.round && row.round > 1 ? 'warning' : 'info'">
                R{{ row.round || 1 }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="85" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getLineTagType(row.status)">
                {{ getLineStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="驳回原因" min-width="140">
            <template #default="{ row }">
              <span v-if="row.rejectReason" class="text-xs text-red-500 font-medium">
                {{ row.rejectReason }}
              </span>
              <span v-else class="text-xs text-gray-400">—</span>
            </template>
          </el-table-column>
          <el-table-column label="客户色号 *" min-width="130">
            <template #default="{ row }">
              <el-input
                v-model="row.customerColorNo"
                placeholder="如 C01"
                maxlength="64"
                :disabled="isViewMode || isRowFrozen(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="颜色名称 *" min-width="130">
            <template #default="{ row }">
              <el-input
                v-model="row.colorName"
                placeholder="如 大红"
                maxlength="64"
                :disabled="isViewMode || isRowFrozen(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="厂内色号" min-width="120">
            <template #default="{ row }">
              <el-input
                v-model="row.factoryColorNo"
                placeholder="如 F001"
                maxlength="64"
                :disabled="isViewMode || isRowFrozen(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="色值" width="130" align="center">
            <template #default="{ row }">
              <div class="flex items-center justify-center space-x-1">
                <el-color-picker
                  v-model="row.colorHex"
                  size="small"
                  :disabled="isViewMode || isRowFrozen(row)"
                />
                <span class="text-xs font-mono text-gray-500">{{ row.colorHex || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="主灯源 *" min-width="120">
            <template #default="{ row }">
              <el-select
                v-model="row.lightPrimary"
                placeholder="选择灯源"
                :disabled="isViewMode || isRowFrozen(row)"
              >
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="辅灯源" min-width="120">
            <template #default="{ row }">
              <el-select
                v-model="row.lightSecondary"
                placeholder="辅灯源"
                clearable
                :disabled="isViewMode || isRowFrozen(row)"
              >
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
              <el-input
                v-model="row.colorCardRef"
                placeholder="如 Pantone TPX"
                maxlength="128"
                :disabled="isViewMode || isRowFrozen(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="样布照片" width="110" align="center">
            <template #default="{ row, $index }">
              <el-button link type="primary" @click="openPhotosDialog($index)">
                照片 ({{ getPhotoCount(row.samplePhotos) }})
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="份数" width="100" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.copies"
                :min="1"
                :max="99"
                controls-position="right"
                class="!w-80px"
                :disabled="isViewMode || isRowFrozen(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="120">
            <template #default="{ row }">
              <el-input
                v-model="row.remark"
                placeholder="备注"
                maxlength="255"
                :disabled="isViewMode || isRowFrozen(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center" fixed="right" v-if="!isViewMode">
            <template #default="{ row, $index }">
              <div class="flex items-center justify-center space-x-1">
                <!-- 存在已持久化 ID 且允许操作状态 -->
                <template v-if="row.id && ['PENDING', 'IN_PROGRESS'].includes(formData.status)">
                  <el-button
                    v-if="row.status === 'PENDING'"
                    link
                    type="success"
                    size="small"
                    v-hasPermi="['lunaerp:production:color-instruction:confirm']"
                    @click="handleApproveRow(row)"
                  >
                    通过
                  </el-button>
                  <el-button
                    v-if="row.status === 'PENDING' || row.status === 'APPROVED'"
                    link
                    type="warning"
                    size="small"
                    v-hasPermi="['lunaerp:production:color-instruction:confirm']"
                    @click="handleRejectRestrikeRow(row)"
                  >
                    驳回重打
                  </el-button>
                </template>

                <!-- 排序与删除操作（仅未冻结行可用） -->
                <template v-if="!isRowFrozen(row)">
                  <el-button
                    link
                    size="small"
                    :disabled="$index === 0"
                    @click="moveRowUp($index)"
                    title="上移"
                  >
                    <Icon icon="ep:top" />
                  </el-button>
                  <el-button
                    link
                    size="small"
                    :disabled="!formData.colors || $index === formData.colors.length - 1"
                    @click="moveRowDown($index)"
                    title="下移"
                  >
                    <Icon icon="ep:bottom" />
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    size="small"
                    @click="handleDeleteColorRow($index)"
                    title="删除"
                  >
                    <Icon icon="ep:delete" />
                  </el-button>
                </template>
                <template v-else-if="row.status === 'REJECTED'">
                  <span class="text-xs text-gray-400">已终结</span>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 底部保存按钮 -->
      <div class="bottom-actions flex justify-center space-x-4 py-4">
        <el-button v-if="isViewMode" @click="handleBack">返回</el-button>
        <template v-else>
          <el-button @click="handleBack">取消</el-button>
          <el-button type="primary" :loading="saveLoading" @click="handleSave">
            {{ id ? '保存修改' : '保存打色单' }}
          </el-button>
        </template>
      </div>
    </el-form>

    <!-- 样布照片管理弹窗 -->
    <el-dialog
      v-model="photosDialogVisible"
      title="样布照片管理"
      width="600px"
      append-to-body
      destroy-on-close
    >
      <div class="py-2">
        <UploadImgs
          v-model="currentPhotoUrls"
          :limit="9"
          :disabled="isPhotoReadOnly"
          height="120px"
          width="120px"
        />
      </div>
      <template #footer>
        <el-button v-if="isPhotoReadOnly" @click="photosDialogVisible = false">关闭</el-button>
        <template v-else>
          <el-button @click="photosDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleConfirmPhotos">确定</el-button>
        </template>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
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

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)
const saveLoading = ref(false)
const nextNoLoading = ref(false)
const startLoading = ref(false)

let tempIdCounter = 0
const generateTempId = () => `temp_${Date.now()}_${++tempIdCounter}`

const id = computed(() => (route.query.id as string) || (route.params.id as string) || '')
const isViewMode = computed(() => route.query.mode === 'view' || (!!formData.status && !['PENDING', 'IN_PROGRESS'].includes(formData.status)))
const pageTitle = computed(() => {
  if (isViewMode.value) return '查看打色指示单'
  if (id.value) return '编辑打色指示单'
  return '新建打色指示单'
})

// 单据状态 & 行状态标签辅助
const getDocStatusTagType = (status?: string) => {
  switch (status) {
    case 'PENDING': return 'warning'
    case 'IN_PROGRESS': return 'primary'
    case 'CONFIRMED': return 'success'
    case 'IN_PRODUCTION': return 'info'
    case 'VOID': return 'danger'
    default: return 'info'
  }
}

const getDocStatusLabel = (status?: string) => {
  switch (status) {
    case 'PENDING': return '草稿/待打色'
    case 'IN_PROGRESS': return '打色中'
    case 'CONFIRMED': return '已确认'
    case 'IN_PRODUCTION': return '已投产'
    case 'VOID': return '已作废'
    default: return status || '草稿'
  }
}

const getLineTagType = (status?: string) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

const getLineStatusLabel = (status?: string) => {
  if (status === 'APPROVED') return '已接受'
  if (status === 'REJECTED') return '已驳回'
  return '待审核'
}

/** 判断颜色行是否冻结（APPROVED 或 REJECTED 均置灰只读） */
const isRowFrozen = (row: ColorInstructionColorVO) => {
  return row.status === 'APPROVED' || row.status === 'REJECTED'
}

const tableRowClassName = ({ row }: { row: ColorInstructionColorVO }) => {
  if (row.status === 'REJECTED') {
    return 'row-rejected'
  }
  if (row.status === 'APPROVED') {
    return 'row-frozen'
  }
  return ''
}

// 表单数据
const formData = reactive<Partial<ColorInstructionVO>>({
  instructionNo: undefined,
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
  remark: '',
  colors: []
})

// 表单校验规则
const formRules = reactive<FormRules>({
  instructionDate: [{ required: true, message: '单据日期不能为空', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择客户档案', trigger: 'change' }],
  productId: [{ required: true, message: '请选择产品档案', trigger: 'change' }]
})

// 客户远程搜索
const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])
const contactOptions = ref<CustomerContactVO[]>([])

const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const data = await CustomerApi.getCustomerPage({ pageNo: 1, pageSize: 20, search: query || '' })
    customerOptions.value = data.list || []
  } catch (error) {
    console.error('Failed to search customers:', error)
  } finally {
    customerLoading.value = false
  }
}

const handleCustomerChange = async (customerId?: number) => {
  if (!customerId) {
    formData.clientName = ''
    formData.contactPerson = ''
    contactOptions.value = []
    return
  }
  const cust = customerOptions.value.find((c) => c.id === customerId)
  if (cust) {
    formData.clientName = cust.companyName
  }
  try {
    const contactPage = await CustomerContactApi.getCustomerContactPage({
      customerId,
      pageNo: 1,
      pageSize: 20
    })
    contactOptions.value = contactPage.list || []
    if (contactOptions.value.length > 0) {
      const primary = contactOptions.value.find((c) => c.isPrimary === 1) || contactOptions.value[0]
      formData.contactPerson = primary.name
    }
  } catch (error) {
    console.error('Failed to fetch customer contacts:', error)
  }
}

// 产品远程搜索
const productLoading = ref(false)
const productOptions = ref<ProductVO[]>([])

const searchProducts = async (query: string) => {
  productLoading.value = true
  try {
    const data = await ProductApi.getProductPage({ pageNo: 1, pageSize: 20, search: query || '' })
    productOptions.value = data.list || []
  } catch (error) {
    console.error('Failed to search products:', error)
  } finally {
    productLoading.value = false
  }
}

const handleProductChange = (productId?: number) => {
  if (!productId) {
    formData.productCode = ''
    formData.productName = ''
    formData.composition = ''
    formData.width = ''
    formData.gsm = ''
    return
  }
  const prod = productOptions.value.find((p) => p.id === productId)
  if (prod) {
    formData.productCode = prod.code
    formData.productName = prod.name
    formData.composition = prod.composition || ''
    formData.width = prod.widthCm ? `${prod.widthCm}cm` : ''
    formData.gsm = prod.weightGsm ? `${prod.weightGsm}gsm` : ''
  }
}

// 承接方供应商远程搜索
const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])

const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const data = await SupplierApi.getSupplierPage({ pageNo: 1, pageSize: 20, search: query || '' })
    supplierOptions.value = data.list || []
  } catch (error) {
    console.error('Failed to search suppliers:', error)
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierChange = (supplierId?: number) => {
  if (!supplierId) {
    formData.labFactoryName = ''
    return
  }
  const supp = supplierOptions.value.find((s) => s.id === supplierId)
  if (supp) {
    formData.labFactoryName = supp.companyName
  }
}

// 获取下一个编号
const handleGetNextNo = async () => {
  nextNoLoading.value = true
  try {
    const nextNo = await ColorInstructionApi.getNextNo()
    formData.instructionNo = nextNo
    ElMessage.success('编号获取成功')
  } catch (error) {
    console.error('Failed to get next number:', error)
  } finally {
    nextNoLoading.value = false
  }
}

// 颜色明细行添加/删除/调序
const handleAddColorRow = () => {
  if (!formData.colors) {
    formData.colors = []
  }
  formData.colors.push({
    _tempId: generateTempId(),
    customerColorNo: '',
    colorName: '',
    factoryColorNo: '',
    colorHex: '',
    lightPrimary: 'D65',
    lightSecondary: '',
    colorCardRef: '',
    samplePhotos: '',
    copies: 1,
    round: 1,
    status: 'PENDING',
    remark: ''
  })
}

const handleDeleteColorRow = (index: number) => {
  if (formData.colors) {
    formData.colors.splice(index, 1)
  }
}

const moveRowUp = (index: number) => {
  if (index <= 0 || !formData.colors) return
  const temp = formData.colors[index]
  formData.colors[index] = formData.colors[index - 1]
  formData.colors[index - 1] = temp
}

const moveRowDown = (index: number) => {
  if (!formData.colors || index >= formData.colors.length - 1) return
  const temp = formData.colors[index]
  formData.colors[index] = formData.colors[index + 1]
  formData.colors[index + 1] = temp
}

// 样布照片管理弹窗
const photosDialogVisible = ref(false)
const activeColorRowIndex = ref<number>(-1)
const currentPhotoUrls = ref<string[]>([])

const isPhotoReadOnly = computed(() => {
  if (isViewMode.value) return true
  if (activeColorRowIndex.value >= 0 && formData.colors) {
    const row = formData.colors[activeColorRowIndex.value]
    return row && isRowFrozen(row)
  }
  return false
})

const getPhotoCount = (photosStr?: string): number => {
  if (!photosStr) return 0
  try {
    const parsed = JSON.parse(photosStr)
    return Array.isArray(parsed) ? parsed.length : 0
  } catch {
    return photosStr.split(',').filter(Boolean).length
  }
}

const openPhotosDialog = (rowIndex: number) => {
  activeColorRowIndex.value = rowIndex
  const rawPhotos = formData.colors?.[rowIndex]?.samplePhotos || ''
  if (rawPhotos) {
    try {
      const parsed = JSON.parse(rawPhotos)
      if (Array.isArray(parsed)) {
        currentPhotoUrls.value = parsed.map((item) => (typeof item === 'string' ? item : item.url)).filter(Boolean)
      } else {
        currentPhotoUrls.value = []
      }
    } catch {
      currentPhotoUrls.value = rawPhotos.split(',').filter(Boolean)
    }
  } else {
    currentPhotoUrls.value = []
  }
  photosDialogVisible.value = true
}

const handleConfirmPhotos = () => {
  if (activeColorRowIndex.value >= 0 && formData.colors) {
    const urls = currentPhotoUrls.value || []
    if (urls.length === 0) {
      formData.colors[activeColorRowIndex.value].samplePhotos = ''
    } else {
      const jsonArr = urls.map((url, idx) => ({ name: `样布照片_${idx + 1}`, url }))
      formData.colors[activeColorRowIndex.value].samplePhotos = JSON.stringify(jsonArr)
    }
  }
  photosDialogVisible.value = false
}

// 开始打色（PENDING -> IN_PROGRESS）
const handleStart = async () => {
  if (!id.value) return
  try {
    await ElMessageBox.confirm('确认开始打色吗？开始后单据状态将转为打色中。', '开始打色', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    startLoading.value = true
    await ColorInstructionApi.startColorInstruction(Number(id.value))
    ElMessage.success('开始打色成功')
    await loadDetail(Number(id.value))
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to start color instruction:', error)
    }
  } finally {
    startLoading.value = false
  }
}

// 通过打色行（PENDING -> APPROVED）
const handleApproveRow = async (row: ColorInstructionColorVO) => {
  if (!row.id) {
    ElMessage.warning('请先保存打色单再执行通过操作')
    return
  }
  try {
    await ElMessageBox.confirm(`确认通过颜色【${row.customerColorNo} - ${row.colorName}】吗？`, '通过颜色行', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    loading.value = true
    await ColorInstructionApi.approveColorLine(row.id)
    ElMessage.success('颜色行已通过')
    if (id.value) {
      await loadDetail(Number(id.value))
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to approve color row:', error)
    }
  } finally {
    loading.value = false
  }
}

// 驳回重打（PENDING/APPROVED -> REJECTED 并生成 round+1 新行）
const handleRejectRestrikeRow = async (row: ColorInstructionColorVO) => {
  if (!row.id) {
    ElMessage.warning('请先保存打色单再执行驳回重打操作')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      `确认驳回颜色【${row.customerColorNo} - ${row.colorName}】并开启下一轮打色吗？请输入驳回意见：`,
      '驳回重打',
      {
        confirmButtonText: '确定驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入驳回意见（如：色光偏深、需回调等）',
        inputPattern: /\S+/,
        inputErrorMessage: '驳回意见不能为空'
      }
    )
    loading.value = true
    await ColorInstructionApi.rejectRestrikeColorLine({
      id: row.id,
      rejectReason: value
    })
    ElMessage.success('已驳回并生成下一轮打色行')
    if (id.value) {
      await loadDetail(Number(id.value))
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to reject restrike color row:', error)
    }
  } finally {
    loading.value = false
  }
}

// 返回
const handleBack = () => {
  router.push('/production/color-instruction')
}

// 加载详情数据（查看与编辑模式）
const loadDetail = async (instructionId: number) => {
  loading.value = true
  try {
    const data = await ColorInstructionApi.getColorInstruction(instructionId)
    if (data) {
      Object.assign(formData, data)
      // 为颜色行补充稳定 row-key（_tempId）
      if (formData.colors && formData.colors.length > 0) {
        formData.colors.forEach((c) => {
          c._tempId = c.id ? `id_${c.id}` : generateTempId()
        })
      }
      // 回填客户联系人下拉
      if (data.customerId) {
        CustomerContactApi.getCustomerContactPage({
          customerId: data.customerId,
          pageNo: 1,
          pageSize: 50
        })
          .then((contactPage) => {
            contactOptions.value = contactPage.list || []
          })
          .catch((e) => console.error(e))
      }
      // 补全搜索下拉回显项
      if (data.customerId && data.clientName) {
        if (!customerOptions.value.some((c) => c.id === data.customerId)) {
          customerOptions.value.push({
            id: data.customerId,
            companyName: data.clientName
          } as CustomerVO)
        }
      }
      if (data.productId && data.productCode) {
        if (!productOptions.value.some((p) => p.id === data.productId)) {
          productOptions.value.push({
            id: data.productId,
            code: data.productCode,
            name: data.productName || ''
          } as ProductVO)
        }
      }
      if (data.labFactoryId && data.labFactoryName) {
        if (!supplierOptions.value.some((s) => s.id === data.labFactoryId)) {
          supplierOptions.value.push({
            id: data.labFactoryId,
            companyName: data.labFactoryName
          } as SupplierVO)
        }
      }
    }
  } catch (error) {
    console.error('Failed to load color instruction detail:', error)
    ElMessage.error('加载打色指示单详情失败')
  } finally {
    loading.value = false
  }
}

// 保存打色单
const handleSave = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  // 校验颜色行
  if (!formData.colors || formData.colors.length === 0) {
    ElMessage.warning('请至少添加一行颜色明细')
    return
  }

  for (let i = 0; i < formData.colors.length; i++) {
    const row = formData.colors[i]
    if (!row.customerColorNo || !row.customerColorNo.trim()) {
      ElMessage.warning(`第 ${i + 1} 行客户色号不能为空`)
      return
    }
    if (!row.colorName || !row.colorName.trim()) {
      ElMessage.warning(`第 ${i + 1} 行颜色名称不能为空`)
      return
    }
    if (!row.lightPrimary) {
      ElMessage.warning(`第 ${i + 1} 行主灯源不能为空`)
      return
    }
  }

  saveLoading.value = true
  try {
    if (id.value) {
      formData.id = Number(id.value)
      await ColorInstructionApi.updateColorInstruction(formData as any)
      ElMessage.success('更新打色指示单成功')
    } else {
      await ColorInstructionApi.createColorInstruction(formData as any)
      ElMessage.success('创建打色指示单成功')
    }
    router.push('/production/color-instruction')
  } catch (error) {
    console.error('Failed to save color instruction:', error)
  } finally {
    saveLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    searchCustomers(''),
    searchProducts(''),
    searchSuppliers('')
  ])

  if (id.value) {
    await loadDetail(Number(id.value))
  } else {
    // 缺省初始化一行颜色
    if (!formData.colors || formData.colors.length === 0) {
      handleAddColorRow()
    }
  }
})
</script>

<style scoped>
.header-title {
  font-weight: 600;
  font-size: 16px;
}
.card-title {
  font-size: 15px;
}
:deep(.row-frozen) {
  background-color: #fafafa !important;
}
:deep(.row-frozen td) {
  color: #606266;
}
:deep(.row-rejected) {
  background-color: #fafafa !important;
}
:deep(.row-rejected td) {
  color: #909399 !important;
  text-decoration: line-through;
}
:deep(.row-rejected .el-tag),
:deep(.row-rejected .reject-reason-text),
:deep(.row-rejected .el-button),
:deep(.row-rejected .font-mono) {
  text-decoration: none !important;
}
</style>
