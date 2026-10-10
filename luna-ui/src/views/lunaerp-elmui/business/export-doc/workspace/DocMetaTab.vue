<template>
  <div class="py-16px max-w-1100px">
    <el-tabs v-model="activeSubTab" type="border-card">
      <!-- 1. PI 专属元数据 -->
      <el-tab-pane label="形式发票 (PI)" name="PI">
        <div class="flex justify-between items-center mb-16px">
          <span class="text-13px text-gray-500">
            形式发票专属信息（用于客户付款订金确认、信用证开证草案）。
          </span>
          <el-button type="primary" @click="$emit('print', 'EXPORT_DOC_PI')">
            预览/打印形式发票 (PI)
          </el-button>
        </div>

        <el-form :model="piData" label-width="140px" :disabled="isReadOnly">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="PI 编号 (PI No.)">
                <el-input v-model="piData.piNo" :placeholder="docNo ? `默认：${docNo}` : '留空取单证套号'" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="开立日期">
                <el-date-picker
                  v-model="piData.date"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择开立日期"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="有效截止日 (Valid)">
                <el-date-picker
                  v-model="piData.validUntil"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择有效截止日"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预计装运期 (Ship Date)">
                <el-date-picker
                  v-model="piData.shipmentDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择预计装运期"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="定金比例 (Deposit %)">
                <el-input-number
                  v-model="piData.depositPct"
                  :min="0"
                  :max="100"
                  :precision="0"
                  placeholder="例如 30"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="佣金比例 (Commission %)">
                <el-input-number
                  v-model="piData.commissionPct"
                  :min="0"
                  :max="100"
                  :precision="1"
                  placeholder="例如 5.0"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="分批装运 (Partial)">
                <el-select v-model="piData.partialShipment" placeholder="默认 Allowed" class="!w-full">
                  <el-option label="Allowed (允许)" value="Allowed" />
                  <el-option label="Not Allowed (不允许)" value="Not Allowed" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="中途转运 (Transship)">
                <el-select v-model="piData.transshipment" placeholder="默认 Allowed" class="!w-full">
                  <el-option label="Allowed (允许)" value="Allowed" />
                  <el-option label="Not Allowed (不允许)" value="Not Allowed" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">PI 专属收款银行覆盖（留空则取卖方默认银行）</el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="银行名称">
                <el-input v-model="piData.bankName" placeholder="覆盖收款银行名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="银行账号">
                <el-input v-model="piData.bankAccount" placeholder="覆盖银行账号 A/C" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="SWIFT 码">
                <el-input v-model="piData.bankSwift" placeholder="覆盖银行 SWIFT" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="银行地址">
                <el-input v-model="piData.bankAddress" placeholder="覆盖银行地址" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="PI 专属备注">
            <el-input
              v-model="piData.notes"
              type="textarea"
              :rows="2"
              placeholder="形式发票底部专属备注条款"
              maxlength="500"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 2. CI 专属元数据 -->
      <el-tab-pane label="商业发票 (CI)" name="CI">
        <div class="flex justify-between items-center mb-16px">
          <span class="text-13px text-gray-500">
            商业发票专属信息（用于报关、目的港清关、收汇与认证声明）。
          </span>
          <el-button type="primary" @click="$emit('print', 'EXPORT_DOC_CI')">
            预览/打印商业发票 (CI)
          </el-button>
        </div>

        <el-form :model="ciData" label-width="140px" :disabled="isReadOnly">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="发票号 (Invoice No.)">
                <el-input v-model="ciData.ciNo" :placeholder="docNo ? `默认：${docNo}` : '留空取单证套号'" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发票日期">
                <el-date-picker
                  v-model="ciData.invoiceDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择发票日期"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="信用证号 (L/C No.)">
                <el-input v-model="ciData.lcNo" placeholder="请输入信用证号（如有）" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同引用号">
                <el-input v-model="ciData.contractRef" placeholder="默认取抬头合同号" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="原产地声明条款">
            <el-input
              v-model="ciData.originDeclaration"
              placeholder="默认：COUNTRY OF ORIGIN: CHINA"
              maxlength="256"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 3. PL 专属元数据 -->
      <el-tab-pane label="装箱单 (PL)" name="PL">
        <div class="flex justify-between items-center mb-16px">
          <span class="text-13px text-gray-500">
            装箱单专属信息与汇总覆盖（实务尾箱凑整：手填优先、空则按行求和）。
          </span>
          <el-button type="primary" @click="$emit('print', 'EXPORT_DOC_PL')">
            预览/打印装箱单 (PL)
          </el-button>
        </div>

        <el-form :model="plData" label-width="140px" :disabled="isReadOnly">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="装箱单号 (PL No.)">
                <el-input v-model="plData.plNo" :placeholder="docNo ? `默认：${docNo}` : '留空取单证套号'" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="装箱日期">
                <el-date-picker
                  v-model="plData.plDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择装箱日期"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">
            TOTAL 汇总覆盖（手填优先、留空则由系统自动按行求和）
          </el-divider>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="总箱件数 (Packages)">
                <el-input-number
                  v-model="plData.totalPackages"
                  :min="0"
                  :precision="0"
                  placeholder="留空自动求和"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="总净重 (KG)">
                <el-input-number
                  v-model="plData.totalNetWeight"
                  :min="0"
                  :precision="2"
                  placeholder="留空自动求和"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="总毛重 (KG)">
                <el-input-number
                  v-model="plData.totalGrossWeight"
                  :min="0"
                  :precision="2"
                  placeholder="留空自动求和"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="总数量 (Quantity)">
                <el-input-number
                  v-model="plData.totalQuantity"
                  :min="0"
                  :precision="2"
                  placeholder="留空自动求和"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="总体积 (CBM)">
                <el-input-number
                  v-model="plData.totalVolume"
                  :min="0"
                  :precision="3"
                  placeholder="留空自动求和"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="总金额 (Amount)">
                <el-input-number
                  v-model="plData.totalAmount"
                  :min="0"
                  :precision="2"
                  placeholder="留空自动求和"
                  class="!w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="装箱单备注">
            <el-input
              v-model="plData.remark"
              type="textarea"
              :rows="2"
              placeholder="装箱单专属备注"
              maxlength="500"
            />
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, watch } from 'vue'
import type { ExportDocVO } from '@/api/lunaerp-elmui/business/export-doc'

defineOptions({ name: 'ExportDocDocMetaTab' })

const props = defineProps<{
  formData: ExportDocVO
  isReadOnly: boolean
  docNo?: string
}>()

defineEmits<{
  (e: 'print', templateCode: string): void
}>()

const activeSubTab = ref<'PI' | 'CI' | 'PL'>('PI')

const piData = reactive<any>({
  piNo: '',
  date: '',
  validUntil: '',
  shipmentDate: '',
  depositPct: undefined,
  commissionPct: undefined,
  partialShipment: 'Allowed',
  transshipment: 'Allowed',
  bankName: '',
  bankAccount: '',
  bankSwift: '',
  bankAddress: '',
  notes: ''
})

const ciData = reactive<any>({
  ciNo: '',
  invoiceDate: '',
  lcNo: '',
  contractRef: '',
  originDeclaration: 'COUNTRY OF ORIGIN: CHINA'
})

const plData = reactive<any>({
  plNo: '',
  plDate: '',
  totalPackages: undefined,
  totalNetWeight: undefined,
  totalGrossWeight: undefined,
  totalQuantity: undefined,
  totalVolume: undefined,
  totalAmount: undefined,
  remark: ''
})

/** 初始化解析与监听同步 */
watch(
  () => props.formData.piMeta,
  (val) => {
    if (val) {
      try {
        Object.assign(piData, JSON.parse(val))
      } catch {}
    }
  },
  { immediate: true }
)

watch(
  () => props.formData.ciMeta,
  (val) => {
    if (val) {
      try {
        Object.assign(ciData, JSON.parse(val))
      } catch {}
    }
  },
  { immediate: true }
)

watch(
  () => props.formData.plMeta,
  (val) => {
    if (val) {
      try {
        Object.assign(plData, JSON.parse(val))
      } catch {}
    }
  },
  { immediate: true }
)

/** 暴露给父组件取回序列化的 JSON */
const syncMetaJson = () => {
  props.formData.piMeta = JSON.stringify(piData)
  props.formData.ciMeta = JSON.stringify(ciData)
  props.formData.plMeta = JSON.stringify(plData)
}

defineExpose({ syncMetaJson })
</script>
