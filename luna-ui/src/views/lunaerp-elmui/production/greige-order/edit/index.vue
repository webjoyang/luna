<template>
  <ContentWrap>
    <div class="edit-header">
      <el-page-header @back="handleBack">
        <template #content>
          <span class="header-title">{{ pageTitle }}</span>
        </template>
      </el-page-header>
    </div>

    <!-- 新建：整页完整主表表单（编辑/查看随 03 票开放） -->
    <el-form
      v-if="isCreateMode"
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="110px"
      class="greige-form"
      v-loading="loading"
    >
      <div class="section-title">基本信息</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="胚布单号" prop="orderNo">
            <el-input
              v-model="formData.orderNo"
              maxlength="32"
              placeholder="留空自动生成（如 PB0919001），也可手填"
            >
              <template #append>
                <el-button :loading="nextNoLoading" @click="handleGetNextNo">获取编号</el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="销售合同" prop="salesContractId">
            <el-select
              v-model="formData.salesContractId"
              placeholder="输入关键词搜索合同（可空）"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchContracts"
              :loading="contractLoading"
              class="!w-full"
              @change="handleContractChange"
            >
              <el-option
                v-for="item in contractOptions"
                :key="item.id"
                :label="item.contractNo"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单据日期" prop="orderDate">
            <el-date-picker
              v-model="formData.orderDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择单据日期"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="交货日期" prop="deliveryDate">
            <el-date-picker
              v-model="formData.deliveryDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择交货日期（可空）"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="section-title">坯布厂与染厂</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="坯布厂" prop="greigeFactoryId">
            <el-select
              v-model="formData.greigeFactoryId"
              placeholder="输入关键词搜索供应商（必选）"
              filterable
              remote
              reserve-keyword
              :remote-method="(query: string) => searchSuppliers(query, 'greige')"
              :loading="greigeSupplierLoading"
              class="!w-full"
              @change="(val: number) => handleSupplierChange(val, 'greige')"
            >
              <el-option
                v-for="item in greigeSupplierOptions"
                :key="item.id"
                :label="`${item.supplierNo} ${item.companyName}`"
                :value="item.id!"
              >
                <span>{{ item.supplierNo }} {{ item.companyName }}</span>
                <span class="option-type">{{ typeLabel(item.supplierType) }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="指定染厂" prop="dyeingFactoryId">
            <el-select
              v-model="formData.dyeingFactoryId"
              placeholder="输入关键词搜索供应商（可空）"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="(query: string) => searchSuppliers(query, 'dyeing')"
              :loading="dyeingSupplierLoading"
              class="!w-full"
              @change="(val?: number) => handleSupplierChange(val, 'dyeing')"
            >
              <el-option
                v-for="item in dyeingSupplierOptions"
                :key="item.id"
                :label="`${item.supplierNo} ${item.companyName}`"
                :value="item.id!"
              >
                <span>{{ item.supplierNo }} {{ item.companyName }}</span>
                <span class="option-type">{{ typeLabel(item.supplierType) }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <div class="section-title">品种规格</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="产品编号" prop="productCode">
            <el-select
              v-model="formData.productCode"
              placeholder="搜编号/品名回填或手填"
              clearable
              filterable
              remote
              allow-create
              default-first-option
              reserve-keyword
              :remote-method="searchProducts"
              :loading="productLoading"
              class="!w-full"
              @change="handleProductChange"
            >
              <el-option
                v-for="product in productOptions"
                :key="product.id"
                :label="product.code"
                :value="product.code"
              >
                <span>{{ product.code }}</span>
                <span class="product-option-name">{{ product.name }}</span>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="产品名称" prop="productName">
            <el-input v-model="formData.productName" maxlength="128" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="成分" prop="composition">
            <el-input v-model="formData.composition" maxlength="128" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="幅宽" prop="width">
            <el-input v-model="formData.width" maxlength="64" placeholder="如 160cm 有效" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="克重" prop="gsm">
            <el-input v-model="formData.gsm" maxlength="64" placeholder="如 180g" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="缩水率" prop="shrinkage">
            <el-input v-model="formData.shrinkage" maxlength="64" placeholder="如 ≤5%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="匹重(KG)" prop="pieceWeight">
            <el-input-number
              v-model="formData.pieceWeight"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="section-title">生产计划</div>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="计划投胚量" prop="plannedQty">
            <el-input-number
              v-model="formData.plannedQty"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
              placeholder="KG"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="匹数" prop="pieceCount">
            <el-input-number
              v-model="formData.pieceCount"
              :precision="0"
              :controls="false"
              :min="0"
              class="!w-full"
              placeholder="按投胚量÷匹重自动算出，可手改"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="成品数量" prop="finishedQty">
            <el-input-number
              v-model="formData.finishedQty"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
              placeholder="下单成品数量 KG（可空）"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预设损耗" prop="lossPercent">
            <el-input-number
              v-model="lossPercent"
              :precision="2"
              :controls="false"
              :min="0"
              :max="100"
              class="!w-full"
            >
              <template #append>%</template>
            </el-input-number>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="加工费" prop="processingFee">
            <el-input-number
              v-model="formData.processingFee"
              :precision="2"
              :controls="false"
              :min="0"
              class="!w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="胚布品种" prop="greigeTypeId">
            <el-select
              v-model="formData.greigeTypeId"
              placeholder="请选择胚布品种（可空）"
              clearable
              filterable
              class="!w-full"
            >
              <el-option
                v-for="item in greigeTypeOptions"
                :key="item.id"
                :label="`${item.code} ${item.name}`"
                :value="String(item.id)"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <div class="section-title">备注</div>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="生产工艺备注" prop="productionRemark">
            <el-input
              v-model="formData.productionRemark"
              type="textarea"
              :rows="3"
              maxlength="1000"
              placeholder="请输入织造指令等生产工艺备注"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="3" maxlength="1000" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="formData.status" class="!w-full">
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_GREIGE_ORDER_STATUS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <div class="form-footer">
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="['lunaerp:production:greige-order:create']"
        >
          保存
        </el-button>
        <el-button @click="handleBack">返回</el-button>
      </div>
    </el-form>

    <!-- 编辑/查看：随 03 票开放 -->
    <el-empty v-else description="胚布单编辑/查看随 03 票开放" :image-size="120" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions, getDictLabel } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as GreigeOrderApi from '@/api/lunaerp-elmui/production/greige-order'
import type { GreigeOrderVO } from '@/api/lunaerp-elmui/production/greige-order'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import * as GreigeTypeApi from '@/api/lunaerp/baseinfo/greige-type'
import type { GreigeTypeSimpleVO } from '@/api/lunaerp/baseinfo/greige-type'

defineOptions({ name: 'LunaerpElmuiProductionGreigeOrderEdit' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)

const formData = reactive<GreigeOrderVO>({
  orderNo: '',
  salesContractId: undefined,
  contractNo: '',
  greigeFactoryId: undefined as unknown as number,
  greigeFactoryName: '',
  dyeingFactoryId: undefined,
  dyeingFactoryName: '',
  orderDate: formatDate(new Date(), 'YYYY-MM-DD'),
  deliveryDate: undefined,
  productCode: '',
  productName: '',
  composition: '',
  width: '',
  gsm: '',
  shrinkage: '',
  pieceWeight: 25,
  pieceCount: undefined,
  plannedQty: undefined,
  finishedQty: undefined,
  suggestedLoss: 0,
  processingFee: undefined,
  greigeTypeId: undefined,
  productionRemark: '',
  remark: '',
  status: 'ACTIVE'
})

const rules = {
  greigeFactoryId: [{ required: true, message: '坯布厂不能为空', trigger: 'change' }],
  orderDate: [{ required: true, message: '单据日期不能为空', trigger: 'change' }],
  plannedQty: [{ required: true, message: '计划投胚量不能为空', trigger: 'blur' }]
}

const orderId = computed(() => (route.params.id as string) || null)
const isCreateMode = computed(() => !orderId.value)

const pageTitle = computed(() => (isCreateMode.value ? '新建胚布单' : '编辑胚布单'))

// ==================== 获取编号（只读预览、不占号） ====================

const nextNoLoading = ref(false)

const handleGetNextNo = async () => {
  // 用户已手工输入（含去空格）时不自动覆盖
  if (formData.orderNo && formData.orderNo.trim()) return
  nextNoLoading.value = true
  try {
    const no = await GreigeOrderApi.getNextGreigeOrderNo()
    // 请求返回后再次判空，避免与手工输入竞态
    if (!formData.orderNo || !formData.orderNo.trim()) {
      formData.orderNo = no
    }
  } finally {
    nextNoLoading.value = false
  }
}

// ==================== 供应商远程搜索（坯布厂/染厂，ADR-0007 不做类型强过滤） ====================

/**
 * 两个选择器共用同一数据源（全部 ACTIVE 供应商，不按 supplierType 过滤），
 * 仅候选列表与加载态独立，选项显示"供应商编号 公司名（类型标签）"辅助辨认。
 */
const greigeSupplierOptions = ref<SupplierVO[]>([])
const dyeingSupplierOptions = ref<SupplierVO[]>([])
const greigeSupplierLoading = ref(false)
const dyeingSupplierLoading = ref(false)

const typeLabel = (supplierType?: string): string =>
  supplierType ? getDictLabel(DICT_TYPE.LUNAERP_SUPPLIER_TYPE, supplierType) : ''

const searchSuppliers = async (query: string, role: 'greige' | 'dyeing') => {
  const setLoading = role === 'greige' ? greigeSupplierLoading : dyeingSupplierLoading
  const setOptions = role === 'greige' ? greigeSupplierOptions : dyeingSupplierOptions
  setLoading.value = true
  try {
    // 附加 status=ACTIVE：停用供应商不进新单选项；不传 supplierType（ADR-0007）
    const data = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    setOptions.value = data.list
  } finally {
    setLoading.value = false
  }
}

/** 选中供应商：保存公司名快照（建单时名称，档案日后改名不回写） */
const handleSupplierChange = (supplierId: number | undefined, role: 'greige' | 'dyeing') => {
  const options = role === 'greige' ? greigeSupplierOptions : dyeingSupplierOptions
  const supplier = options.value.find((item) => item.id === supplierId)
  if (role === 'greige') {
    formData.greigeFactoryName = supplier?.companyName || ''
  } else {
    formData.dyeingFactoryName = supplier?.companyName || ''
  }
}

// ==================== 销售合同远程搜索（可空引用） ====================

const contractLoading = ref(false)
const contractOptions = ref<SalesContractVO[]>([])

const searchContracts = async (query: string) => {
  contractLoading.value = true
  try {
    const data = await SalesContractApi.getSalesContractPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || ''
    })
    contractOptions.value = data.list
  } finally {
    contractLoading.value = false
  }
}

/** 选中合同：保存合同号快照（可手改） */
const handleContractChange = (contractId: number | undefined) => {
  const contract = contractOptions.value.find((item) => item.id === contractId)
  formData.contractNo = contract?.contractNo || ''
}

// ==================== 产品档案远程搜索（回填品种规格后再改） ====================

const productLoading = ref(false)
const productOptions = ref<ProductVO[]>([])

const searchProducts = async (query: string) => {
  productLoading.value = true
  try {
    // 产品分页 search 对编号/品名等分词 AND 匹配；不传 status 默认查 ACTIVE
    const data = await ProductApi.getProductPage({ pageNo: 1, pageSize: 20, search: query || '' })
    productOptions.value = data.list || []
  } finally {
    productLoading.value = false
  }
}

/** 选中产品后回填品名/成分/幅宽/克重，全部字段仍可手改；选中值不在候选中即为手填，不回填 */
const handleProductChange = (val: string) => {
  const product = productOptions.value.find((p) => p.code === val)
  if (!product) return
  formData.productName = product.name || ''
  formData.composition = product.composition || ''
  formData.width = product.widthCm != null ? String(product.widthCm) : ''
  formData.gsm = product.weightGsm != null ? String(product.weightGsm) : ''
}

// ==================== 胚布品种（手选、不自动识别） ====================

const greigeTypeOptions = ref<GreigeTypeSimpleVO[]>([])

const loadGreigeTypes = async () => {
  try {
    greigeTypeOptions.value = (await GreigeTypeApi.getSimpleGreigeTypeList()) || []
  } catch {
    // 加载失败不阻断表单，品种字段可留空
    greigeTypeOptions.value = []
  }
}

// ==================== 计划投胚量 → 匹数自动计算（可手改） ====================

/**
 * 计划投胚量或匹重变化时按 ceil(投胚量÷匹重) 重算匹数；
 * 用户手改匹数后，再次修改投胚量/匹重仍会重算（以最新输入为准）。
 */
watch([() => formData.plannedQty, () => formData.pieceWeight], ([plannedQty, pieceWeight]) => {
  const qty = Number(plannedQty ?? 0)
  const weight = Number(pieceWeight ?? 0)
  if (qty > 0 && weight > 0) {
    formData.pieceCount = Math.ceil(qty / weight)
  }
})

// ==================== 预设损耗：百分比输入、小数传输 ====================

const lossPercent = ref(0)

/** 提交与读取时统一以 lossPercent 为准换算为小数（0.05 = 5%） */
const toSuggestedLoss = (): number => Number((lossPercent.value ?? 0) / 100)

// ==================== 保存 ====================

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload: GreigeOrderVO = {
      ...formData,
      orderNo: formData.orderNo?.trim() || undefined, // 留空走后端自动号
      contractNo: formData.contractNo?.trim() || undefined,
      greigeFactoryName: formData.greigeFactoryName?.trim() || undefined,
      dyeingFactoryName: formData.dyeingFactoryName?.trim() || undefined,
      suggestedLoss: toSuggestedLoss(),
      productCode: formData.productCode?.trim() || undefined,
      greigeTypeId: formData.greigeTypeId || undefined
    }
    await GreigeOrderApi.createGreigeOrder(payload)
    message.success('保存成功')
    router.push('/production/greige-order')
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  router.push('/production/greige-order')
}

/** 初始化 */
onMounted(() => {
  loadGreigeTypes()
  // 新建时预取一批供应商/合同候选，保证下拉一打开就有内容可看
  searchSuppliers('', 'greige')
  searchSuppliers('', 'dyeing')
  searchContracts('')
})
</script>

<style lang="scss" scoped>
.edit-header {
  margin-bottom: 16px;

  .header-title {
    font-size: 16px;
    font-weight: 600;
  }
}

.section-title {
  margin-bottom: 12px;
  padding-left: 8px;
  font-weight: 600;
  border-left: 3px solid var(--el-color-primary);
}

.greige-form {
  max-width: 1100px;
  padding: 4px 8px;
}

.option-type {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.product-option-name {
  float: right;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.form-footer {
  margin-top: 12px;
  padding-left: 110px;
}
</style>
