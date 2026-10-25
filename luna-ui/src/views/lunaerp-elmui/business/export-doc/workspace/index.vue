<template>
  <ContentWrap>
    <!-- 头部与操作栏 -->
    <div class="workspace-header flex justify-between items-center mb-16px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <span v-if="formData.docNo" class="text-gray-500 font-mono text-14px">
              {{ formData.docNo }}
            </span>
            <DictTag
              v-if="formData.status"
              :type="DICT_TYPE.LUNAERP_EXPORT_DOC_STATUS"
              :value="formData.status"
            />
            <el-tag v-if="formData.sourceType" size="small" type="info">
              {{ formatSourceType(formData.sourceType) }}
            </el-tag>
          </div>
        </template>
      </el-page-header>

      <!-- 操作按钮栏 -->
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>

        <!-- 重新同步（仅 DRAFT 且非 MANUAL 来源） -->
        <template v-if="formData.id && formData.status === 'DRAFT'">
          <el-tooltip
            v-if="formData.sourceType === 'MANUAL'"
            content="手工新建的单据没有关联来源单据，不可重新同步"
            placement="top"
          >
            <span>
              <el-button disabled>重新同步</el-button>
            </span>
          </el-tooltip>
          <el-button
            v-else
            type="warning"
            plain
            :loading="resyncLoading"
            @click="handleResync"
            v-hasPermi="['lunaerp:business:export-doc:update']"
          >
            <Icon icon="ep:refresh" class="mr-2px" />重新同步
          </el-button>
        </template>

        <!-- 保存（仅草稿态） -->
        <el-button
          v-if="!formData.status || formData.status === 'DRAFT'"
          type="primary"
          :loading="saving"
          @click="handleSave"
          v-hasPermi="['lunaerp:business:export-doc:update', 'lunaerp:business:export-doc:create']"
        >
          保存
        </el-button>

        <!-- 定稿（草稿态可定稿） -->
        <el-button
          v-if="formData.id && formData.status === 'DRAFT'"
          type="success"
          :loading="actionLoading"
          @click="handleFinalize"
          v-hasPermi="['lunaerp:business:export-doc:update']"
        >
          定稿
        </el-button>

        <!-- 撤销定稿（已定稿可撤销） -->
        <el-button
          v-if="formData.id && formData.status === 'FINALIZED'"
          type="warning"
          :loading="actionLoading"
          @click="handleRevoke"
          v-hasPermi="['lunaerp:business:export-doc:update']"
        >
          撤销定稿
        </el-button>

        <!-- 打印（DRAFT 与 FINALIZED 均可调） -->
        <el-button
          v-if="formData.id"
          type="primary"
          plain
          @click="handleOpenPrint"
          v-hasPermi="['lunaerp:business:export-doc:print']"
        >
          <Icon icon="ep:printer" class="mr-2px" />打印单证套
        </el-button>
      </div>
    </div>

    <!-- 骨架屏 / 主体 Tab -->
    <el-tabs v-loading="loading" v-model="activeTab" type="card" class="mt-8px">
      <!-- 1. 共享抬头 -->
      <el-tab-pane label="共享单据抬头" name="basic">
        <BasicInfoTab
          :form-data="formData"
          :is-read-only="isReadOnly"
        />
      </el-tab-pane>

      <!-- 2. 货物明细 -->
      <el-tab-pane label="货物明细表" name="items">
        <ItemsTab
          :items="formData.items || []"
          :is-read-only="isReadOnly"
        />
      </el-tab-pane>

      <!-- 3. 单据页签 -->
      <el-tab-pane label="单证专属元数据 (PI / CI / PL)" name="meta">
        <DocMetaTab
          ref="docMetaTabRef"
          :form-data="formData"
          :is-read-only="isReadOnly"
          :doc-no="formData.docNo"
          @print="handlePrintTemplate"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 打印弹窗 -->
    <PrintExportDocDialog ref="printDialogRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import {
  getExportDoc,
  createExportDoc,
  updateExportDoc,
  getNextDocNo,
  finalizeExportDoc,
  revokeExportDoc,
  resyncFromSource,
  getDraftFromContract,
  getDraftFromShipment,
  type ExportDocVO
} from '@/api/lunaerp-elmui/business/export-doc'
import BasicInfoTab from './BasicInfoTab.vue'
import ItemsTab from './ItemsTab.vue'
import DocMetaTab from './DocMetaTab.vue'
import PrintExportDocDialog from '../components/PrintExportDocDialog.vue'

defineOptions({ name: 'ExportDocWorkspace' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const actionLoading = ref(false)
const resyncLoading = ref(false)
const activeTab = ref('basic')
const docMetaTabRef = ref<InstanceType<typeof DocMetaTab>>()
const printDialogRef = ref<InstanceType<typeof PrintExportDocDialog>>()

const formData = reactive<ExportDocVO>({
  id: undefined,
  docNo: '',
  status: 'DRAFT',
  sourceType: 'MANUAL',
  contractId: undefined,
  contractNo: '',
  shipmentId: undefined,
  shipmentNo: '',
  customerId: undefined,
  customerName: '',
  sellerName: '',
  sellerAddress: '',
  sellerBankInfo: '',
  buyerName: '',
  buyerAddress: '',
  buyerBankInfo: '',
  incoterm: '',
  portOfLoading: '',
  portOfDischarge: '',
  countryOfDest: '',
  transportMode: '',
  paymentTerm: '',
  currency: 'USD',
  shippingMark: '',
  piMeta: '',
  ciMeta: '',
  plMeta: '',
  remarks: '',
  items: []
})

const isReadOnly = computed(() => {
  return formData.status === 'FINALIZED'
})

const pageTitle = computed(() => {
  if (!formData.id) return '新建出口单证套'
  return formData.status === 'FINALIZED' ? '查看出口单证套' : '编辑出口单证套'
})

const formatSourceType = (type?: string) => {
  switch (type) {
    case 'CONTRACT':
      return '合同生成'
    case 'SHIPMENT':
      return '发货生成'
    default:
      return '手工自建'
  }
}

/** 载入数据 */
onMounted(async () => {
  const id = route.params.id ? Number(route.params.id) : undefined
  const sourceMode = route.query.sourceType as string | undefined
  const contractId = route.query.contractId ? Number(route.query.contractId) : undefined
  const shipmentId = route.query.shipmentId ? Number(route.query.shipmentId) : undefined

  loading.value = true
  try {
    if (id) {
      // 详情查看 / 编辑模式
      const res = await getExportDoc(id)
      if (res) {
        Object.assign(formData, res)
        if (!formData.items) formData.items = []
      }
    } else if (sourceMode === 'CONTRACT' && contractId) {
      // 从外销销售合同带出草稿
      const draft = await getDraftFromContract(contractId)
      if (draft) {
        Object.assign(formData, draft)
        if (!formData.items) formData.items = []
      }
    } else if (sourceMode === 'SHIPMENT' && shipmentId) {
      // 从发货单带出草稿
      const draft = await getDraftFromShipment(shipmentId)
      if (draft) {
        Object.assign(formData, draft)
        if (!formData.items) formData.items = []
      }
    } else {
      // 手工空白新建
      formData.sourceType = 'MANUAL'
      const nextNo = await getNextDocNo()
      if (nextNo) {
        formData.docNo = nextNo
      }
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '加载出口单证数据失败')
  } finally {
    loading.value = false
  }
})

/** 保存草稿 */
const handleSave = async () => {
  // 1. 同步单据专属元数据 JSON
  docMetaTabRef.value?.syncMetaJson()

  if (!formData.customerId && !formData.customerName) {
    ElMessage.warning('请选择买方客户')
    activeTab.value = 'basic'
    return
  }

  saving.value = true
  try {
    if (formData.id) {
      await updateExportDoc(formData)
      ElMessage.success('出口单证套保存成功')
    } else {
      const res = await createExportDoc(formData)
      ElMessage.success('出口单证套创建成功')
      const newId = (res as any)?.data || res
      if (newId) {
        router.replace(`/business/export-doc/workspace/${newId}`)
      }
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/** 定稿操作 */
const handleFinalize = async () => {
  if (!formData.id) return
  if (!formData.items || formData.items.length === 0) {
    ElMessage.error('定稿失败：单证套没有货物明细')
    return
  }

  await ElMessageBox.confirm('定稿后单证套将变为只读归档状态，确认定稿吗？', '提示', {
    type: 'warning'
  })

  actionLoading.value = true
  try {
    await finalizeExportDoc(formData.id)
    ElMessage.success('定稿成功')
    const res = await getExportDoc(formData.id)
    if (res) Object.assign(formData, res)
  } catch (err: any) {
    ElMessage.error(err?.message || '定稿失败')
  } finally {
    actionLoading.value = false
  }
}

/** 撤销定稿操作 */
const handleRevoke = async () => {
  if (!formData.id) return
  await ElMessageBox.confirm('撤销定稿后单证套将重新恢复为可编辑草稿，确认撤销吗？', '提示', {
    type: 'info'
  })

  actionLoading.value = true
  try {
    await revokeExportDoc(formData.id)
    ElMessage.success('撤销定稿成功')
    const res = await getExportDoc(formData.id)
    if (res) Object.assign(formData, res)
  } catch (err: any) {
    ElMessage.error(err?.message || '撤销定稿失败')
  } finally {
    actionLoading.value = false
  }
}

/** 重新同步来源数据 */
const handleResync = async () => {
  if (!formData.id) return
  await ElMessageBox.confirm(
    '重新同步将仅补充来源单据中的空白字段，永不覆盖您已编辑的内容。是否继续？',
    '重新同步确认',
    { type: 'warning' }
  )

  resyncLoading.value = true
  try {
    await resyncFromSource(formData.id)
    ElMessage.success('重新同步完成')
    const res = await getExportDoc(formData.id)
    if (res) Object.assign(formData, res)
  } catch (err: any) {
    ElMessage.error(err?.message || '重新同步失败')
  } finally {
    resyncLoading.value = false
  }
}

/** 打开打印选择弹窗 */
const handleOpenPrint = () => {
  if (!formData.id) return
  printDialogRef.value?.open(formData.id)
}

/** 打印指定模板 */
const handlePrintTemplate = (templateCode: string) => {
  if (!formData.id) {
    ElMessage.warning('请先保存单证套后再进行打印')
    return
  }
  printDialogRef.value?.open(formData.id, templateCode)
}

const handleBack = () => {
  router.push('/business/export-doc')
}
</script>

<style scoped>
.workspace-header {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 12px;
}
</style>
