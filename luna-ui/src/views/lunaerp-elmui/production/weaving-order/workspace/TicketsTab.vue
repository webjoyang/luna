<template>
  <div class="tickets-tab py-24px">
    <el-empty description="布票二维码凭证（P1 阶段基础展示，Issue 05 进阶支持扫码开工与完工）" />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  weavingOrderId?: number
}>()
</script>
