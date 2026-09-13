<template>
  <el-dialog
    v-model="visible"
    title="选择胚布品种"
    width="720px"
    append-to-body
    :close-on-click-modal="false"
  >
    <div class="picker-search">
      <el-input
        v-model="keyword"
        placeholder="搜索编号 / 名称 / 成分 / 纱支"
        clearable
      >
        <template #prefix>
          <Icon icon="ep:search" :size="15" />
        </template>
      </el-input>
    </div>

    <el-table
      v-loading="loading"
      :data="filteredOptions"
      height="380"
      highlight-current-row
      @row-click="handleRowClick"
      @row-dblclick="handleRowDblClick"
    >
      <el-table-column width="46" align="center">
        <template #default="{ row }">
          <el-radio :model-value="tempId" :value="String(row.id)">&nbsp;</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="品种编号" prop="code" width="120" />
      <el-table-column label="显示名" min-width="200" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <span class="greige-name">{{ formatGreige(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="布种" prop="fabricType" width="90" align="center" />
      <el-table-column label="成分" prop="composition" min-width="120" :show-overflow-tooltip="true" />
      <template #empty>
        <el-empty description="未找到匹配的胚布品种" :image-size="80" />
      </template>
    </el-table>

    <template #footer>
      <el-button @click="handleClear">清除选择</el-button>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!tempId" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import type { GreigeTypeSimpleVO } from '@/api/lunaerp/baseinfo/greige-type'

defineOptions({ name: 'GreigeTypePicker' })

/** 「30S NR罗马布 260g×160cm」式展示名 */
const formatGreige = (row?: GreigeTypeSimpleVO | null): string => {
  if (!row) return ''
  return `${row.name ?? ''} ${row.weightGsm ?? '-'}g×${row.widthCm ?? '-'}cm`
}

const props = defineProps({
  /** 当前选中的胚布品种 id */
  modelValue: {
    type: String as PropType<string | number | undefined>,
    default: undefined
  },
  /** 父组件加载好的精简列表 */
  options: {
    type: Array as PropType<GreigeTypeSimpleVO[]>,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const visible = ref(false)
const keyword = ref('')
const tempId = ref<string>('')

/** 打开弹窗（每次同步外部当前值） */
const open = () => {
  tempId.value = props.modelValue !== undefined && props.modelValue !== null
    ? String(props.modelValue)
    : ''
  keyword.value = ''
  visible.value = true
}
defineExpose({ open })

/** 简单前端搜索（列表为全量精简数据） */
const filteredOptions = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return props.options
  return props.options.filter((row) =>
    [row.code, row.name, row.fabricType, row.composition, row.yarnSpec]
      .filter(Boolean)
      .some((field) => String(field).toLowerCase().includes(kw))
  )
})

const handleRowClick = (row: GreigeTypeSimpleVO) => {
  tempId.value = String(row.id)
}

/** 双击行直接确定 */
const handleRowDblClick = (row: GreigeTypeSimpleVO) => {
  tempId.value = String(row.id)
  handleConfirm()
}

const handleConfirm = () => {
  const row = props.options.find((item) => String(item.id) === tempId.value) ?? null
  emit('update:modelValue', row ? String(row.id) : undefined)
  emit('change', row)
  visible.value = false
}

const handleClear = () => {
  tempId.value = ''
  emit('update:modelValue', undefined)
  emit('change', null)
  visible.value = false
}
</script>

<style lang="scss" scoped>
.picker-search {
  margin-bottom: 12px;
}

.greige-name {
  font-weight: 600;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
