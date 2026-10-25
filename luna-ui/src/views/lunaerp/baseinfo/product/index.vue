<template>
  <ContentWrap>
    <!-- 灵动岛工具栏：搜索 / 视图切换 / 批量删除 / 新建 -->
    <ProductToolbar
      v-model:search="queryParams.search"
      v-model:view-mode="viewMode"
      :batch-count="checkedIds.length"
      @search="handleQuery"
      @create="openForm('create')"
      @batch-delete="handleBatchDelete"
      @download-template="handleDownloadTemplate"
      @import="handleImport"
    />

    <!-- 状态分段：在售 / 草稿 -->
    <div class="status-bar">
      <ProductStatusSegment
        v-model="queryParams.status"
        @change="handleStatusChange"
      />
      <span class="total-tip">共 <b>{{ total }}</b> 款产品</span>
    </div>

    <!-- 列表区域：网格 / 表格 视图切换 -->
    <motion.div
      :key="viewMode"
      :initial="{ opacity: 0, y: 10 }"
      :animate="{ opacity: 1, y: 0 }"
      :transition="{ duration: 0.3, ease: EASE }"
    >
      <ProductGrid
        v-if="viewMode === 'grid'"
        :list="list"
        :loading="loading"
        @edit="openForm('update', $event.id)"
        @delete="handleDelete"
      />
      <ProductTable
        v-else
        :list="list"
        :loading="loading"
        @selection-change="handleSelectionChange"
        @edit="openForm('update', $event.id)"
        @delete="handleDelete"
      />
    </motion.div>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新建 / 编辑弹窗 -->
    <ProductEditModal ref="editModalRef" @success="handleSaved" />

    <!-- 导入弹窗 -->
    <ProductImportDialog ref="importDialogRef" @success="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { motion } from 'motion-v'
import download from '@/utils/download'
import * as ProductApi from '@/api/lunaerp/baseinfo/product'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import ProductToolbar from './components/ProductToolbar.vue'
import ProductStatusSegment from './components/ProductStatusSegment.vue'
import ProductGrid from './components/ProductGrid.vue'
import ProductTable from './components/ProductTable.vue'
import ProductEditModal from './components/ProductEditModal.vue'
import ProductImportDialog from './components/ProductImportDialog.vue'

defineOptions({ name: 'LunaerpBaseinfoProduct' })

const EASE: [number, number, number, number] = [0.16, 1, 0.3, 1]

const message = ElMessage

const loading = ref(true) // 列表加载中
const total = ref(0) // 总条数
const list = ref<ProductVO[]>([]) // 当前页数据
const viewMode = ref<'grid' | 'table'>('grid') // 视图模式
const checkedIds = ref<number[]>([]) // 表格勾选的产品 id

const importDialogRef = ref()

/** 下载导入模板 */
const handleDownloadTemplate = async () => {
  try {
    const res = await ProductApi.downloadProductTemplate()
    download.excel(res, '产品导入模板.xlsx')
  } catch (e) {
    message.error('下载模板失败')
  }
}

/** 打开导入弹窗 */
const handleImport = () => {
  importDialogRef.value?.open()
}

const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  search: '',
  status: 'ACTIVE' as 'ACTIVE' | 'DRAFT'
})

/** 查询产品分页 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ProductApi.getProductPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索：回车或点按钮触发，回到第 1 页 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 切换在售 / 草稿：重置到第 1 页并清空勾选 */
const handleStatusChange = () => {
  queryParams.pageNo = 1
  checkedIds.value = []
  getList()
}

/** 表格多选 */
const handleSelectionChange = (rows: ProductVO[]) => {
  checkedIds.value = rows.map((row) => row.id!).filter(Boolean)
}

// ========== 新建 / 编辑 ==========
const editModalRef = ref()
const openForm = (mode: 'create' | 'update', id?: number) => {
  editModalRef.value?.open(mode, id, queryParams.status)
}

/**
 * 保存成功后的原位缓存策略（对齐原 React）：
 *  - 新建：返回详情插入当前列表头部，不重新翻页（已存在同 id 则替换）；
 *  - 编辑：用返回对象原位替换当前行。
 */
const handleSaved = ({
  mode,
  product
}: {
  mode: 'create' | 'update'
  product: ProductVO
}) => {
  if (!product || !product.id) {
    getList()
    return
  }
  const index = list.value.findIndex((item) => item.id === product.id)
  if (mode === 'update') {
    if (index >= 0) {
      list.value.splice(index, 1, product)
    } else {
      // 当前页找不到（极端情况）回退为重查
      getList()
    }
    return
  }
  // 新建
  if (index >= 0) {
    list.value.splice(index, 1, product)
  } else {
    list.value.unshift(product)
    // 超出页容量时丢弃尾部一条，保持分页大小
    if (list.value.length > queryParams.pageSize) {
      list.value.splice(queryParams.pageSize)
    }
    total.value += 1
  }
}

// ========== 删除 ==========
/** 删除后从当前列表移除；当前页删空且非首页时回退一页重查 */
const removeFromList = (ids: number[]) => {
  const idSet = new Set(ids)
  list.value = list.value.filter((item) => !item.id || !idSet.has(item.id))
  total.value = Math.max(0, total.value - ids.length)
  if (list.value.length === 0 && queryParams.pageNo > 1) {
    queryParams.pageNo -= 1
    getList()
  }
}

/** 单条删除 */
const handleDelete = async (row: ProductVO) => {
  try {
    await ElMessageBox.confirm(`是否确认删除产品「${row.name}」？`, '删除提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await ProductApi.deleteProduct(row.id!)
    message.success('删除成功')
    checkedIds.value = checkedIds.value.filter((id) => id !== row.id)
    removeFromList([row.id!])
  } catch {}
}

/** 批量删除 */
const handleBatchDelete = async () => {
  if (checkedIds.value.length === 0) {
    message.warning('请先勾选要删除的产品')
    return
  }
  try {
    await ElMessageBox.confirm(
      `是否确认删除选中的 ${checkedIds.value.length} 个产品？此操作无法撤销。`,
      '批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const ids = [...checkedIds.value]
    await ProductApi.deleteProductList(ids)
    message.success('删除成功')
    checkedIds.value = []
    removeFromList(ids)
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 14px 2px 14px;

  .total-tip {
    font-size: 12.5px;
    color: var(--el-text-color-secondary);

    b {
      margin: 0 2px;
      font-size: 14px;
      color: var(--el-text-color-primary);
    }
  }
}
</style>
