<template>
  <el-table
    v-loading="loading"
    :data="list"
    border
    stripe
    @selection-change="(rows: ProductVO[]) => emit('selection-change', rows)"
  >
    <el-table-column type="selection" width="48" align="center" />
    <el-table-column label="产品编号" prop="code" width="150" align="center" fixed>
      <template #default="{ row }">
        <span class="cell-code">{{ row.code }}</span>
      </template>
    </el-table-column>
    <el-table-column
      label="产品名称"
      prop="name"
      min-width="180"
      :show-overflow-tooltip="true"
    />
    <el-table-column label="类型" prop="type" width="100" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.type" size="small" type="primary" effect="light">{{ row.type }}</el-tag>
        <span v-else>—</span>
      </template>
    </el-table-column>
    <el-table-column
      label="成分"
      prop="composition"
      min-width="140"
      :show-overflow-tooltip="true"
    />
    <el-table-column label="克重(GSM)" prop="weightGsm" width="105" align="center" />
    <el-table-column label="门幅(CM)" prop="widthCm" width="100" align="center" />
    <el-table-column label="颜色" prop="color" width="100" align="center">
      <template #default="{ row }">{{ row.color || '—' }}</template>
    </el-table-column>
    <el-table-column label="状态" prop="status" width="90" align="center">
      <template #default="{ row }">
        <el-tag v-if="row.status === 'ACTIVE'" type="success" size="small">在售</el-tag>
        <el-tag v-else-if="row.status === 'DRAFT'" type="info" size="small">草稿</el-tag>
        <el-tag v-else type="info" size="small">{{ row.status || '—' }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column
      label="创建时间"
      prop="createTime"
      width="170"
      align="center"
      :formatter="dateFormatter"
    />
    <el-table-column label="操作" width="130" align="center" fixed="right">
      <template #default="{ row }">
        <el-button
          link
          type="primary"
          v-hasPermi="['lunaerp:baseinfo:product:update']"
          @click="emit('edit', row)"
        >
          <Icon icon="ep:edit" class="mr-2px" />编辑
        </el-button>
        <el-button
          link
          type="danger"
          v-hasPermi="['lunaerp:baseinfo:product:delete']"
          @click="emit('delete', row)"
        >
          <Icon icon="ep:delete" class="mr-2px" />删除
        </el-button>
      </template>
    </el-table-column>

    <template #empty>
      <el-empty description="暂无产品数据" :image-size="90" />
    </template>
  </el-table>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'

defineOptions({ name: 'ProductTable' })

defineProps({
  list: {
    type: Array as PropType<ProductVO[]>,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['selection-change', 'edit', 'delete'])
</script>

<style lang="scss" scoped>
.cell-code {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  font-style: italic;
  font-weight: 700;
  color: var(--el-text-color-regular);
}
</style>
