<template>
  <el-card shadow="never" class="mb-16px stitch-board-card">
    <template #header>
      <div class="flex items-center justify-between flex-wrap gap-2">
        <div class="flex items-center gap-3">
          <span class="font-bold text-14px">三角排列画板</span>
          <el-tag type="info" size="small" effect="plain">
            {{ board.rows }} 行 × {{ board.columns }} 列
          </el-tag>
          <!-- 符号图例说明 -->
          <div class="hidden sm:flex items-center gap-2 text-12px text-gray-500 ml-2">
            <span>图例:</span>
            <el-tag size="small" type="info" effect="plain">空: 空</el-tag>
            <el-tag size="small" type="primary" effect="light">v: 成圈</el-tag>
            <el-tag size="small" type="warning" effect="light">-: 集圈</el-tag>
            <el-tag size="small" type="danger" effect="light">+: 浮线</el-tag>
            <el-tag size="small" effect="light">=: 其它</el-tag>
          </div>
        </div>

        <!-- 操作按钮群 -->
        <div class="flex items-center gap-2">
          <el-button
            size="small"
            type="primary"
            plain
            :disabled="isReadOnly"
            @click="handleAddCols"
          >
            <Icon icon="ep:plus" class="mr-4px" /> +4 列
          </el-button>
          <el-button
            size="small"
            type="primary"
            plain
            :disabled="isReadOnly || board.columns <= MIN_COLUMNS"
            @click="handleRemoveCols"
          >
            <Icon icon="ep:minus" class="mr-4px" /> -4 列
          </el-button>
          <el-button
            size="small"
            type="danger"
            plain
            :disabled="isReadOnly || isBoardEmpty"
            @click="handleClear"
          >
            <Icon icon="ep:delete" class="mr-4px" /> 清空
          </el-button>
        </div>
      </div>
    </template>

    <!-- 0 列时空态提示 -->
    <div v-if="board.columns === 0" class="py-30px text-center text-gray-400">
      <Icon icon="ep:grid" class="text-32px mb-8px" />
      <div>当前画板列数为 0，点击右上角 [+4 列] 开始绘制针盘三角排列</div>
    </div>

    <!-- 网格画布容器（横向自适应滚动） -->
    <div v-else class="overflow-x-auto stitch-board-scroll-container pb-8px">
      <div class="stitch-grid inline-block min-w-full">
        <!-- 表头：列编号（按4列微弱分界） -->
        <div class="flex items-center mb-6px pl-48px">
          <div
            v-for="c in board.columns"
            :key="`header-${c}`"
            class="stitch-col-header text-center text-11px text-gray-400"
            :class="{ 'border-r-2 border-gray-300': c % 4 === 0 && c !== board.columns }"
          >
            {{ c }}
          </div>
        </div>

        <!-- 4 行网格数据 -->
        <div
          v-for="(row, rIndex) in board.data"
          :key="`row-${rIndex}`"
          class="flex items-center mb-6px"
        >
          <!-- 行标号（如道 1 ~ 道 4） -->
          <div class="w-48px flex-shrink-0 text-12px font-medium text-gray-500 text-right pr-8px select-none">
            道 {{ rIndex + 1 }}
          </div>

          <!-- 列单元格按钮 -->
          <div
            v-for="(cell, cIndex) in row"
            :key="`cell-${rIndex}-${cIndex}`"
            class="stitch-cell-wrapper flex items-center justify-center"
            :class="{ 'mr-6px': (cIndex + 1) % 4 === 0 && cIndex + 1 !== board.columns }"
          >
            <button
              type="button"
              class="stitch-cell-btn transition-colors duration-150"
              :class="[
                getCellClass(cell),
                {
                  'cursor-pointer hover:opacity-80 active:scale-95': !isReadOnly,
                  'cursor-not-allowed opacity-75': isReadOnly
                }
              ]"
              :disabled="isReadOnly"
              :title="getCellTitle(cell, rIndex, cIndex)"
              @click="handleCellClick(rIndex, cIndex)"
            >
              {{ cell || '' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import { computed, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  type SampleAnalysisStitchArrangeVO,
  type StitchSymbol,
  MIN_COLUMNS,
  STEP_COLUMNS,
  STITCH_SYMBOL_LABELS,
  createDefaultStitchArrange,
  getNextStitchSymbol,
  addStitchColumns,
  removeStitchColumns,
  clearStitchBoard,
  toggleStitchCell,
  normalizeStitchArrange
} from './stitch-board'

const props = defineProps<{
  modelValue?: SampleAnalysisStitchArrangeVO
  isReadOnly: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: SampleAnalysisStitchArrangeVO): void
}>()

// 确保始终有合法的 4×N 结构
const board = computed<SampleAnalysisStitchArrangeVO>(() => {
  return normalizeStitchArrange(props.modelValue)
})

// 若外部传入 undefined / null，则自动同步默认 4×24 结构
watch(
  () => props.modelValue,
  (val) => {
    if (!val || !Array.isArray(val.data) || val.data.length !== 4) {
      if (!props.isReadOnly) {
        emit('update:modelValue', createDefaultStitchArrange())
      }
    }
  },
  { immediate: true }
)

const isBoardEmpty = computed(() => {
  if (!board.value.data) return true
  return board.value.data.every((row) => row.every((cell) => !cell))
})

/** 点击单个格子按五态循环 */
const handleCellClick = (rowIndex: number, colIndex: number) => {
  if (props.isReadOnly) return
  const updated = toggleStitchCell(board.value, rowIndex, colIndex)
  emit('update:modelValue', updated)
}

/** 扩充 4 列 */
const handleAddCols = () => {
  if (props.isReadOnly) return
  const updated = addStitchColumns(board.value, STEP_COLUMNS)
  emit('update:modelValue', updated)
}

/** 扣减 4 列（保留已有符号，0列下限） */
const handleRemoveCols = () => {
  if (props.isReadOnly || board.value.columns <= MIN_COLUMNS) return
  const updated = removeStitchColumns(board.value, STEP_COLUMNS, MIN_COLUMNS)
  emit('update:modelValue', updated)
}

/** 清空全部符号（二次确认） */
const handleClear = async () => {
  if (props.isReadOnly || isBoardEmpty.value) return
  try {
    await ElMessageBox.confirm('确认清空三角排列画板上的全部符号吗？', '清空确认', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const cleared = clearStitchBoard(board.value)
    emit('update:modelValue', cleared)
    ElMessage.success('已清空画板')
  } catch {
    // 用户取消
  }
}

/** 获取格子样式 */
const getCellClass = (symbol: StitchSymbol) => {
  switch (symbol) {
    case 'v':
      return 'stitch-symbol-v'
    case '-':
      return 'stitch-symbol-minus'
    case '+':
      return 'stitch-symbol-plus'
    case '=':
      return 'stitch-symbol-equal'
    default:
      return 'stitch-symbol-empty'
  }
}

const getCellTitle = (symbol: StitchSymbol, rIndex: number, cIndex: number) => {
  const name = STITCH_SYMBOL_LABELS[symbol] || '空'
  return `道 ${rIndex + 1}, 第 ${cIndex + 1} 列: ${name}`
}
</script>

<style scoped>
.stitch-col-header {
  width: 32px;
  min-width: 32px;
}

.stitch-cell-wrapper {
  width: 32px;
  min-width: 32px;
}

.stitch-cell-btn {
  width: 30px;
  height: 30px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: monospace;
  font-size: 15px;
  font-weight: 700;
  border: 1px solid #dcdfe6;
  background-color: #fff;
  user-select: none;
}

.stitch-symbol-empty {
  color: transparent;
  background-color: #fafafa;
  border-color: #e4e7ed;
}

.stitch-symbol-empty:hover:not(:disabled) {
  background-color: #f2f3f5;
  border-color: #c0c4cc;
}

.stitch-symbol-v {
  color: #409eff;
  background-color: #ecf5ff;
  border-color: #b3d8ff;
}

.stitch-symbol-minus {
  color: #e6a23c;
  background-color: #fdf6ec;
  border-color: #f5dab1;
}

.stitch-symbol-plus {
  color: #f56c6c;
  background-color: #fef0f0;
  border-color: #fde2e2;
}

.stitch-symbol-equal {
  color: #909399;
  background-color: #f4f4f5;
  border-color: #d3d4d6;
}
</style>
