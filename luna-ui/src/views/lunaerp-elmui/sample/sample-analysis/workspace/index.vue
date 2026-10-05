<template>
  <ContentWrap>
    <!-- 工作台顶部导航与操作栏 -->
    <div class="workspace-header flex justify-between items-center mb-16px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <span v-if="formData.sampleAnalysisNo" class="text-gray-500 font-mono text-14px">
              {{ formData.sampleAnalysisNo }}
            </span>
            <span v-else-if="previewNo" class="text-gray-400 font-mono text-14px">
              (预览编号: {{ previewNo }})
            </span>
          </div>
        </template>
      </el-page-header>

      <div class="action-buttons flex items-center gap-2">
        <!-- 付款状态标签（常驻展示） -->
        <el-tag :type="formData.isPaid ? 'success' : 'danger'" size="default">
          {{ formData.isPaid ? '已付款' : '未付款' }}
        </el-tag>

        <!-- 单条付款状态切换（仅当已建记录且有收费权限时展示） -->
        <el-button
          v-if="sampleId"
          :type="formData.isPaid ? 'warning' : 'success'"
          plain
          @click="handleTogglePayment"
          v-hasPermi="['lunaerp:sample:sample-analysis:pay']"
        >
          <Icon :icon="formData.isPaid ? 'ep:close' : 'ep:check'" class="mr-2px" />
          {{ formData.isPaid ? '标记为未付款' : '标记为已付款' }}
        </el-button>

        <el-button @click="handleBack">返回</el-button>

        <!-- 查看模式操作 -->
        <template v-if="isViewMode">
          <el-button
            type="primary"
            @click="isViewMode = false"
            v-hasPermi="['lunaerp:sample:sample-analysis:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            type="danger"
            plain
            @click="handleDelete"
            v-hasPermi="['lunaerp:sample:sample-analysis:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>

        <!-- 编辑 / 新建模式操作 -->
        <template v-else>
          <el-button v-if="sampleId" @click="handleCancelEdit">取消编辑</el-button>
          <el-button
            type="primary"
            :loading="saving"
            @click="handleSave"
            v-hasPermi="['lunaerp:sample:sample-analysis:create', 'lunaerp:sample:sample-analysis:update']"
          >
            <Icon icon="ep:check" class="mr-2px" />保存
          </el-button>
        </template>
      </div>
    </div>

    <!-- 3 Tab 容器（来样信息 / 针织工艺 / 成本报价） -->
    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="来样信息" name="basic">
        <SampleBasicInfoTab
          ref="basicInfoTabRef"
          v-model="formData"
          :is-read-only="isViewMode"
          :is-create="!sampleId"
          :preview-no="previewNo"
        />
      </el-tab-pane>

      <el-tab-pane label="针织工艺" name="craft">
        <SampleCraftTab
          ref="craftTabRef"
          v-model="formData"
          :is-read-only="isViewMode"
        />
      </el-tab-pane>

      <el-tab-pane label="成本报价" name="cost">
        <SampleCostTab
          ref="costTabRef"
          v-model="formData"
          :is-read-only="isViewMode"
        />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import SampleBasicInfoTab from './SampleBasicInfoTab.vue'
import SampleCraftTab from './SampleCraftTab.vue'
import SampleCostTab from './SampleCostTab.vue'
import * as SampleAnalysisApi from '@/api/lunaerp-elmui/sample/sample-analysis'
import type { SampleAnalysisVO, SampleAnalysisSaveReqVO } from '@/api/lunaerp-elmui/sample/sample-analysis'
import { createDefaultStitchArrange } from './stitch-board'

defineOptions({ name: 'LunaerpElmuiSampleSampleAnalysisWorkspace' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const activeTab = ref('basic')
const saving = ref(false)
const isViewMode = ref(false)
const previewNo = ref('')
const MAX_PHOTOS_LIMIT = 9

const basicInfoTabRef = ref<InstanceType<typeof SampleBasicInfoTab>>()
const craftTabRef = ref<InstanceType<typeof SampleCraftTab>>()
const costTabRef = ref<InstanceType<typeof SampleCostTab>>()

const sampleId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : undefined
})

const pageTitle = computed(() => {
  if (!sampleId.value) {
    return '新建样品分析'
  }
  return isViewMode.value ? '样品分析详情' : '编辑样品分析'
})

const defaultFormData = (): SampleAnalysisVO => ({
  productName: '',
  sampleAnalysisNo: '',
  customerId: undefined,
  clientName: '',
  clientContact: '',
  clientStyleNo: '',
  composition: '',
  width: undefined,
  weight: undefined,
  process: '',
  dyeingProcess: '',
  machineType: '',
  output: '',
  flowerNo: '',
  whiteWidth: '',
  whiteWeight: '',
  flowerWidth: '',
  flowerHeight: '',
  gear: '',
  clothRack: '',
  startSpeed: '',
  feederDisk: '',
  theoreticalOutput: '',
  stitchLength: '',
  photos: [],
  stitchArrange: createDefaultStitchArrange(),
  yarnSpecs: [],
  yarnArranges: [],
  supplierQuotes: [],
  productionCost: undefined,
  dyeingCost: undefined,
  otherCost: undefined,
  lossRate: undefined,
  totalCost: undefined,
  quotedPrice: undefined,
  quotedRemark: '',
  unitPrice: 30,
  isPaid: false,
  remark: ''
})

const formData = ref<SampleAnalysisVO>(defaultFormData())

/** 返回列表 */
const handleBack = () => {
  router.push('/sample/sample-analysis')
}

/** 切换当前记录付款状态（单条走收费端点） */
const handleTogglePayment = async () => {
  if (!sampleId.value) return
  const targetPaid = !formData.value.isPaid
  const actionText = targetPaid ? '已付款' : '未付款'
  try {
    await message.confirm(`确认将当前样品分析标记为「${actionText}」吗？`)
    await SampleAnalysisApi.updateSampleAnalysisPayment({
      ids: [sampleId.value],
      isPaid: targetPaid
    })
    formData.value.isPaid = targetPaid
    message.success(`标记为${actionText}成功`)
  } catch {}
}

/** 取消编辑 */
const handleCancelEdit = async () => {
  if (sampleId.value) {
    await loadDetail(sampleId.value)
    isViewMode.value = true
  }
}

/** 保存样品分析 */
const handleSave = async () => {
  // 1. 触发表单必填校验（产品名称等）
  try {
    const valid = await basicInfoTabRef.value?.validate()
    if (!valid) {
      activeTab.value = 'basic'
      return
    }
  } catch {
    activeTab.value = 'basic'
    return
  }

  // 1.1 校验照片数量上限
  if (formData.value.photos && formData.value.photos.length > MAX_PHOTOS_LIMIT) {
    message.error(`样品照片最多上传 ${MAX_PHOTOS_LIMIT} 张`)
    activeTab.value = 'basic'
    return
  }

  // 1.2 校验针织工艺明细（规格必选、比例 > 0）
  try {
    const craftValid = craftTabRef.value?.validate?.()
    if (craftValid === false) {
      activeTab.value = 'craft'
      return
    }
  } catch {
    activeTab.value = 'craft'
    return
  }

  // 1.3 校验成本报价（损耗率 0 <= lossRate < 100）
  try {
    const costValid = costTabRef.value?.validate?.()
    if (costValid === false) {
      activeTab.value = 'cost'
      return
    }
  } catch {
    activeTab.value = 'cost'
    return
  }

  saving.value = true
  try {
    const saveReq: SampleAnalysisSaveReqVO = {
      ...formData.value,
      yarnSpecs: (formData.value.yarnSpecs || []).map((s, idx) => ({
        id: s.id,
        sampleAnalysisId: s.sampleAnalysisId,
        yarnSpecId: s.yarnSpecId,
        yarnTypeId: s.yarnTypeId,
        yarnTypeName: s.yarnTypeName,
        specName: s.specName,
        spinMethod: s.spinMethod,
        ratio: s.ratio,
        unitPrice: s.unitPrice,
        sortOrder: idx + 1,
        remark: s.remark
      })),
      yarnArranges: (formData.value.yarnArranges || []).map((a) => ({
        id: a.id,
        sampleAnalysisId: a.sampleAnalysisId,
        code: a.code,
        draftText: a.draftText,
        borderStatus: a.borderStatus,
        description: a.description
      })),
      supplierQuotes: (formData.value.supplierQuotes || []).map((q) => ({
        id: q.id,
        sampleAnalysisId: q.sampleAnalysisId,
        supplierId: q.supplierId,
        supplierName: q.supplierName,
        greigePrice: q.greigePrice,
        remark: q.remark
      }))
    }
    if (sampleId.value) {
      saveReq.id = sampleId.value
      await SampleAnalysisApi.updateSampleAnalysis(saveReq)
      message.success('更新成功')
      await loadDetail(sampleId.value)
      isViewMode.value = true
    } else {
      const newId = await SampleAnalysisApi.createSampleAnalysis(saveReq)
      message.success('创建成功')
      // 替换路由为查看模式
      router.replace(`/sample/sample-analysis/workspace/${newId}?mode=view`)
    }
  } finally {
    saving.value = false
  }
}

/** 删除样品分析 */
const handleDelete = async () => {
  if (!sampleId.value) return
  try {
    await message.confirm('确认删除该样品分析记录吗？删除后编号不释放、不重用。')
    await SampleAnalysisApi.deleteSampleAnalysis(sampleId.value)
    message.success('删除成功')
    handleBack()
  } catch {}
}

/** 加载详情 */
const loadDetail = async (id: number) => {
  try {
    const data = await SampleAnalysisApi.getSampleAnalysis(id)
    formData.value = {
      ...defaultFormData(),
      ...data,
      photos: data.photos || [],
      stitchArrange: data.stitchArrange ? data.stitchArrange : createDefaultStitchArrange(),
      yarnSpecs: (data.yarnSpecs || []).map((spec) => ({
        ...spec,
        specCascadeValue: spec.yarnTypeId && spec.yarnSpecId ? [spec.yarnTypeId, spec.yarnSpecId] : undefined
      })),
      yarnArranges: (data.yarnArranges || []).map((arrange) => ({
        ...arrange
      })),
      supplierQuotes: (data.supplierQuotes || []).map((quote) => ({
        ...quote,
        _key: quote.id ? `quote_${quote.id}` : `quote_${Date.now()}_${Math.random()}`
      }))
    }
  } catch {}
}

/** 获取预览编号 */
const loadPreviewNo = async () => {
  try {
    previewNo.value = await SampleAnalysisApi.getNextSampleAnalysisNo()
  } catch {}
}

import { checkPermi } from '@/utils/permission'

onMounted(async () => {
  if (sampleId.value) {
    if (!checkPermi(['lunaerp:sample:sample-analysis:query'])) {
      message.error('您没有查看样品分析的权限')
      router.push('/sample/sample-analysis')
      return
    }
    isViewMode.value = route.query.mode === 'view'
    await loadDetail(sampleId.value)
  } else {
    if (!checkPermi(['lunaerp:sample:sample-analysis:create'])) {
      message.error('您没有创建样品分析的权限')
      router.push('/sample/sample-analysis')
      return
    }
    isViewMode.value = false
    await loadPreviewNo()
  }
})
</script>

<style lang="scss" scoped>
.workspace-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }
}
</style>
