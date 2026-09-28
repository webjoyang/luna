<template>
  <div class="progress-feedback-tab">
    <div v-loading="loading" class="tab-body">
      <template v-if="isOrderInactive">
        <el-empty description="该单未下达，暂无进度反馈" :image-size="120" />
      </template>

      <template v-else-if="sectionList.length > 0">
        <ColorSection
          v-for="section in sectionList"
          :key="section.dyeingColorId"
          :section="section"
          @create-vat="handleCreateVatForSection"
          @open-detail="handleOpenVatDetail"
          @open-feedback="handleOpenFeedback"
          @refresh="loadData"
        />
      </template>

      <el-empty v-else-if="!loading" description="暂无进度反馈数据，请先建缸" :image-size="120" />
    </div>

    <VatCreateDialog ref="vatCreateDialogRef" @success="loadData" />
    <VatDetailDrawer ref="vatDetailDrawerRef" @success="loadData" />
    <FeedbackDialog ref="feedbackDialogRef" @success="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import ColorSection from '@/views/lunaerp-elmui/production/progress-feedback/components/ColorSection.vue'
import VatCreateDialog from '@/views/lunaerp-elmui/production/progress-feedback/components/VatCreateDialog.vue'
import VatDetailDrawer from '@/views/lunaerp-elmui/production/progress-feedback/components/VatDetailDrawer.vue'
import FeedbackDialog from '@/views/lunaerp-elmui/production/progress-feedback/components/FeedbackDialog.vue'
import {
  getProgressFeedbackListByOrder,
  type ProgressFeedbackColorSectionVO,
  type ProgressFeedbackVatVO
} from '@/api/lunaerp-elmui/production/progress-feedback'

const props = defineProps<{
  orderId: string | null
  orderStatus?: string
}>()

const loading = ref(false)
const sectionList = ref<ProgressFeedbackColorSectionVO[]>([])
const vatCreateDialogRef = ref<InstanceType<typeof VatCreateDialog>>()
const vatDetailDrawerRef = ref<InstanceType<typeof VatDetailDrawer>>()
const feedbackDialogRef = ref<InstanceType<typeof FeedbackDialog>>()

const isOrderInactive = computed(() => {
  const s = props.orderStatus
  return s === 'DRAFT' || s === 'CANCELLED'
})

const loadData = async () => {
  if (!props.orderId || isOrderInactive.value) {
    sectionList.value = []
    return
  }
  loading.value = true
  try {
    const res = await getProgressFeedbackListByOrder(Number(props.orderId))
    sectionList.value = res || []
  } catch (error) {
    console.error('加载进度反馈失败', error)
    sectionList.value = []
  } finally {
    loading.value = false
  }
}

const handleCreateVatForSection = (section: ProgressFeedbackColorSectionVO) => {
  vatCreateDialogRef.value?.open(section)
}

const handleOpenVatDetail = (vat: ProgressFeedbackVatVO) => {
  vatDetailDrawerRef.value?.open(vat.id)
}

const handleOpenFeedback = (vat: ProgressFeedbackVatVO) => {
  feedbackDialogRef.value?.open(vat.id, vat.vatNo)
}

watch(
  () => [props.orderId, props.orderStatus],
  () => loadData(),
  { immediate: true }
)

defineExpose({ refresh: loadData })
</script>

<style scoped lang="scss">
.progress-feedback-tab {
  padding: 4px 0;
}

.tab-body {
  min-height: 300px;
}
</style>
