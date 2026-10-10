<template>
  <ContentWrap>
    <div class="edit-header mb-4 flex items-center justify-between">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
        </template>
      </el-page-header>
      <div class="header-actions">
        <el-button @click="handleBack">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
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
          <span class="card-title font-bold text-gray-700">基本信息</span>
        </template>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="打色单号" prop="instructionNo">
              <el-input
                v-model="formData.instructionNo"
                maxlength="32"
                placeholder="留空自动生成（如 DS20261010001）"
              >
                <template #append>
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
                placeholder="选择交货日期（可空）"
                class="!w-full"
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
              <el-input v-model="formData.productCode" placeholder="产品编号快照" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="formData.productName" placeholder="产品名称快照" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="成分" prop="composition">
              <el-input v-model="formData.composition" placeholder="成分快照，如 95%棉 5%氨纶" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="幅宽" prop="width">
              <el-input v-model="formData.width" placeholder="幅宽快照，如 180cm" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="克重" prop="gsm">
              <el-input v-model="formData.gsm" placeholder="克重快照，如 200gsm" />
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
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- 颜色明细 -->
      <el-card shadow="never" class="mb-4">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="card-title font-bold text-gray-700">颜色明细</span>
            <el-button type="primary" plain @click="handleAddColorRow">
              <Icon icon="ep:plus" class="mr-1" />添加颜色
            </el-button>
          </div>
        </template>

        <el-table :data="formData.colors" border stripe class="w-full">
          <el-table-column label="#" type="index" width="50" align="center" />
          <el-table-column label="客户色号 *" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.customerColorNo" placeholder="如 C01" maxlength="64" />
            </template>
          </el-table-column>
          <el-table-column label="颜色名称 *" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.colorName" placeholder="如 大红" maxlength="64" />
            </template>
          </el-table-column>
          <el-table-column label="厂内色号" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.factoryColorNo" placeholder="如 F001" maxlength="64" />
            </template>
          </el-table-column>
          <el-table-column label="色值" width="130" align="center">
            <template #default="{ row }">
              <div class="flex items-center justify-center space-x-1">
                <el-color-picker v-model="row.colorHex" size="small" />
                <span class="text-xs font-mono text-gray-500">{{ row.colorHex || '—' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="主灯源 *" min-width="120">
            <template #default="{ row }">
              <el-select v-model="row.lightPrimary" placeholder="选择灯源">
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
              <el-select v-model="row.lightSecondary" placeholder="辅灯源" clearable>
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
              <el-input v-model="row.colorCardRef" placeholder="如 Pantone TPX" maxlength="128" />
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
              <el-input-number v-model="row.copies" :min="1" :max="99" controls-position="right" class="!w-80px" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="备注" maxlength="255" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" @click="handleDeleteColorRow($index)">
                <Icon icon="ep:delete" />
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 底部保存按钮 -->
      <div class="bottom-actions flex justify-center space-x-4 py-4">
        <el-button @click="handleBack">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存打色单</el-button>
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
          height="120px"
          width="120px"
        />
      </div>
      <template #footer>
        <el-button @click="photosDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmPhotos">确定</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
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

const id = computed(() => route.params.id as string)
const pageTitle = computed(() => (id.value ? '编辑打色指示单' : '新建打色指示单'))

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

// 颜色明细行添加/删除
const handleAddColorRow = () => {
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

// 样布照片管理弹窗
const photosDialogVisible = ref(false)
const activeColorRowIndex = ref<number>(-1)
const currentPhotoUrls = ref<string[]>([])

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

// 返回
const handleBack = () => {
  router.push('/production/color-instruction')
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
    await ColorInstructionApi.createColorInstruction(formData)
    ElMessage.success('创建打色指示单成功')
    router.push('/production/color-instruction')
  } catch (error) {
    console.error('Failed to create color instruction:', error)
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => {
  searchCustomers('')
  searchProducts('')
  searchSuppliers('')
  // 缺省初始化一行颜色
  if (!formData.colors || formData.colors.length === 0) {
    handleAddColorRow()
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
</style>
