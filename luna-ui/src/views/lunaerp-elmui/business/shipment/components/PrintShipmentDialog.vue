<template>
  <PrintDispatcher
    ref="dispatcherRef"
    biz-type="SHIPMENT"
  />
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import PrintDispatcher from '@/components/Print/src/PrintDispatcher.vue'
import type { ShipmentVO } from '@/api/lunaerp-elmui/business/shipment'

defineOptions({ name: 'PrintShipmentDialog' })

const dispatcherRef = ref<InstanceType<typeof PrintDispatcher> | null>(null)

/**
 * 打开打印弹窗并指定单据与版式
 *
 * @param target 发货单VO对象或发货单ID
 * @param mode 'shipment' | 'packing' | 'SHIPMENT_DOC' | 'PACKING_LIST'
 */
const open = (
  target: ShipmentVO | number,
  mode: 'shipment' | 'packing' | 'SHIPMENT_DOC' | 'PACKING_LIST' = 'shipment'
) => {
  const bizId = typeof target === 'number' ? target : target?.id
  const templateCode = mode === 'packing' || mode === 'PACKING_LIST' ? 'PACKING_LIST' : 'SHIPMENT_DOC'
  dispatcherRef.value?.open({
    bizId,
    templateCode
  })
}

defineExpose({
  open
})
</script>
