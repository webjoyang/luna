<template>
  <PrintDispatcher
    ref="dispatcherRef"
    biz-type="EXPORT_DOC"
  />
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import PrintDispatcher from '@/components/Print/src/PrintDispatcher.vue'

defineOptions({ name: 'PrintExportDocDialog' })

const dispatcherRef = ref<InstanceType<typeof PrintDispatcher> | null>(null)

/**
 * 打开打印弹窗
 *
 * @param id 出口单证套ID
 * @param templateCode 可选预选模板：'EXPORT_DOC_PI' | 'EXPORT_DOC_CI' | 'EXPORT_DOC_PL'
 */
const open = (
  id: number | string,
  templateCode?: string
) => {
  dispatcherRef.value?.open({
    bizId: id,
    templateCode
  })
}

defineExpose({ open })
</script>
