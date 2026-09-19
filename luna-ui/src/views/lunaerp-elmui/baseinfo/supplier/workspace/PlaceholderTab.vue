<template>
  <el-empty :image-size="120" :description="description" />
</template>

<script lang="ts" setup>
defineOptions({ name: 'LunaerpElmuiSupplierPlaceholderTab' })

withDefaults(defineProps<{ description?: string }>(), {
  description: '开发中'
})
</script>
