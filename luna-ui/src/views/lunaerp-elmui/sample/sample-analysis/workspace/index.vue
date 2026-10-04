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
          v-model="formData"
          :is-read-only="isViewMode"
        />
      </el-tab-pane>

      <el-tab-pane label="成本报价" name="cost">
        <SampleCostTab
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

defineOptions({ name: 'LunaerpElmuiSampleSampleAnalysisWorkspace' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const activeTab = ref('basic')
const saving = ref(false)
const isViewMode = ref(false)
const previewNo = ref('')

const basicInfoTabRef = ref<InstanceType<typeof SampleBasicInfoTab>>()

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
  productionCost: undefined,
  dyeingCost: undefined,
  otherCost: undefined,
  lossRate: undefined,
  quotedPrice: undefined,
  quotedRemark: '',
  unitPrice: 30,
  remark: ''
})

const formData = ref<SampleAnalysisVO>(defaultFormData())

/** 返回列表 */
const handleBack = () => {
  router.push('/sample/sample-analysis')
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

  saving.value = true
  try {
    const saveReq: SampleAnalysisSaveReqVO = {
      ...formData.value
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
      ...data
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
