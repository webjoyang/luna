<template>
  <!-- 灵动岛工具栏：圆角胶囊容器，紧凑间距、轻阴影 -->
  <motion.div
    class="product-toolbar"
    :initial="{ opacity: 0, y: -14 }"
    :animate="{ opacity: 1, y: 0 }"
    :transition="{ duration: 0.4, ease: EASE }"
  >
    <el-input
      :model-value="search"
      class="toolbar-search"
      placeholder="搜索编号、品名、成分…"
      clearable
      @update:model-value="emit('update:search', $event)"
      @keyup.enter="handleSearch"
      @clear="handleSearch"
    >
      <template #prefix>
        <Icon icon="ep:search" :size="15" class="search-icon" />
      </template>
    </el-input>

    <el-button type="primary" @click="handleSearch">
      <Icon icon="ep:search" class="mr-3px" />搜索
    </el-button>

    <span class="toolbar-divider" />

    <!-- 网格 / 表格 视图切换 -->
    <div class="view-switch">
      <button
        type="button"
        class="switch-item"
        :class="{ 'is-active': viewMode === 'grid' }"
        title="网格视图"
        @click="emit('update:viewMode', 'grid')"
      >
        <Icon icon="ep:grid" :size="15" />
        <span>网格</span>
      </button>
      <button
        type="button"
        class="switch-item"
        :class="{ 'is-active': viewMode === 'table' }"
        title="表格视图"
        @click="emit('update:viewMode', 'table')"
      >
        <Icon icon="ep:menu" :size="15" />
        <span>表格</span>
      </button>
    </div>

    <!-- 表格视图下的批量删除 -->
    <el-button
      v-if="viewMode === 'table'"
      type="danger"
      plain
      :disabled="batchCount === 0"
      @click="emit('batch-delete')"
      v-hasPermi="['lunaerp:baseinfo:product:delete']"
    >
      <Icon icon="ep:delete" class="mr-3px" />批量删除
      <span v-if="batchCount > 0" class="batch-count">{{ batchCount }}</span>
    </el-button>

    <span class="toolbar-spacer" />

    <el-button
      type="primary"
      @click="emit('create')"
      v-hasPermi="['lunaerp:baseinfo:product:create']"
    >
      <Icon icon="ep:plus" class="mr-3px" />新建产品
    </el-button>
  </motion.div>
</template>

<script lang="ts" setup>
import { motion } from 'motion-v'

defineOptions({ name: 'ProductToolbar' })

const EASE: [number, number, number, number] = [0.16, 1, 0.3, 1]

defineProps({
  /** 搜索关键字（v-model:search） */
  search: {
    type: String,
    default: ''
  },
  /** 视图模式（v-model:viewMode） */
  viewMode: {
    type: String as PropType<'grid' | 'table'>,
    default: 'grid'
  },
  /** 表格勾选条数（控制批量删除按钮禁用与角标） */
  batchCount: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits([
  'update:search',
  'update:viewMode',
  'search',
  'create',
  'batch-delete'
])

/** 回车/点按钮触发查询（回第 1 页由父组件处理） */
const handleSearch = () => {
  emit('search')
}
</script>

<style lang="scss" scoped>
.product-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 999px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}

.toolbar-search {
  width: 240px;

  :deep(.el-input__wrapper) {
    border-radius: 999px;
    box-shadow: none;
    background: var(--el-fill-color-light);
  }

  .search-icon {
    color: var(--el-text-color-secondary);
  }
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--el-border-color);
  margin: 0 2px;
}

.toolbar-spacer {
  flex: 1;
}

.view-switch {
  display: inline-flex;
  align-items: center;
  padding: 3px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
}

.switch-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
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
    color: var(--el-color-primary);
    box-shadow: var(--el-box-shadow-lighter);
  }
}

.batch-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  margin-left: 4px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--el-color-danger);
  color: #fff;
  font-size: 10px;
  line-height: 1;
}
</style>
