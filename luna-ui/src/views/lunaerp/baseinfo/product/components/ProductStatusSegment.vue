<template>
  <!-- 在售 / 草稿 分段筛选：两个不同的产品状态数据源 -->
  <div class="status-segment">
    <button
      v-for="opt in OPTIONS"
      :key="opt.value"
      type="button"
      class="segment-item"
      :class="{ 'is-active': modelValue === opt.value }"
      @click="handleChange(opt.value)"
    >
      {{ opt.label }}
    </button>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'ProductStatusSegment' })

type ProductStatusFilter = 'ACTIVE' | 'DRAFT'

const OPTIONS: Array<{ value: ProductStatusFilter; label: string }> = [
  { value: 'ACTIVE', label: '在售' },
  { value: 'DRAFT', label: '草稿' }
]

const props = defineProps({
  modelValue: {
    type: String as PropType<ProductStatusFilter>,
    default: 'ACTIVE'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const handleChange = (value: ProductStatusFilter) => {
  if (value === props.modelValue) return
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style lang="scss" scoped>
.status-segment {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
}

.segment-item {
  padding: 6px 20px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    color: var(--el-text-color-primary);
  }

  &.is-active {
    background: var(--el-bg-color);
    color: var(--el-text-color-primary);
    box-shadow: var(--el-box-shadow-lighter);
  }
}
</style>
