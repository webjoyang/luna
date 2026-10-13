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

        <el-table :data="formData.colors" border stripe>
          <el-table-column label="#" type="index" width="50" align="center" />
          <el-table-column label="客户色号 *" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.customerColorNo" :disabled="isViewMode" placeholder="如 C01" />
            </template>
          </el-table-column>
          <el-table-column label="颜色名称 *" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.colorName" :disabled="isViewMode" placeholder="如 藏青" />
            </template>
          </el-table-column>
          <el-table-column label="厂内色号" min-width="110">
            <template #default="{ row }">
              <el-input v-model="row.factoryColorNo" :disabled="isViewMode" placeholder="如 F-01" />
            </template>
          </el-table-column>
          <el-table-column label="色值" width="130" align="center">
            <template #default="{ row }">
              <div class="flex items-center gap-1 justify-center">
                <el-color-picker v-model="row.colorHex" :disabled="isViewMode" size="small" />
                <span class="text-xs font-mono text-gray-500">{{ row.colorHex || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="主灯源 *" min-width="120">
            <template #default="{ row }">
              <el-select v-model="row.lightPrimary" :disabled="isViewMode" placeholder="选择主灯源">
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
              <el-select v-model="row.lightSecondary" :disabled="isViewMode" clearable placeholder="可选辅灯源">
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="色卡参考" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.colorCardRef" :disabled="isViewMode" placeholder="如 Pantone TPX 19-4052" />
            </template>
          </el-table-column>
          <el-table-column label="样布照片" min-width="160">
            <template #default="{ row }">
              <UploadImgs
                v-model="row.samplePhotos"
                :disabled="isViewMode"
                :limit="5"
                height="60px"
                width="60px"
              />
            </template>
          </el-table-column>
          <el-table-column label="份数" width="100" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.copies"
                :disabled="isViewMode"
                :min="1"
                :precision="0"
                controls-position="right"
                class="!w-full"
              />
            </template>
          </el-table-column>
          <el-table-column label="行备注" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.remark" :disabled="isViewMode" placeholder="备注说明" />
            </template>
          </el-table-column>
          <el-table-column v-if="!isViewMode" label="操作" width="70" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeColorLine($index)">
                <Icon icon="ep:delete" />
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-form>
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
const nextNoLoading = ref(false)

const id = computed(() => {
  const val = route.params.id
  return val ? Number(val) : undefined
})

const isCreateMode = computed(() => !id.value)
const isViewMode = computed(() => route.query.mode === 'view')

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

const removeColorLine = (index: number) => {
  if (!formData.colors) return
  if (formData.colors.length <= 1) {
    message.warning('打色单至少需要保留一行颜色明细')
    return
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
    if (isCreateMode.value) {
      await ColorInstructionApi.createColorInstruction(formData)
      message.success('创建打色指示单成功')
    } else {
      await ColorInstructionApi.updateColorInstruction(formData)
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
</style>
