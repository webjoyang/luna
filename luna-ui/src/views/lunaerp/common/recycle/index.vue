<template>
  <ContentWrap>
    <el-tabs v-model="activeBizType" type="card" @tab-change="handleTabChange">
      <el-tab-pane
        v-for="item in bizTypes"
        :key="item.value"
        :label="item.label"
        :name="item.value"
      />
    </el-tabs>

    <div class="flex justify-between items-center mb-15px">
      <div class="text-14px text-gray-500">
        已软删除的数据可在回收站中查看并恢复，主数据恢复后为停用状态。
      </div>
      <div>
        <el-button @click="getList">
          <Icon icon="ep:refresh" class="mr-5px" /> 刷新
        </el-button>
      </div>
    </div>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="名称/单号" prop="displayName" min-width="220" show-overflow-tooltip />
      <el-table-column label="删除人" prop="deletedBy" width="160" align="center" />
      <el-table-column
        label="删除时间"
        prop="deletedTime"
        :formatter="dateFormatter"
        width="180"
        align="center"
      />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            @click="handleRestore(row)"
            v-hasPermi="['lunaerp:common:recycle:restore']"
          >
            <Icon icon="ep:refresh-left" class="mr-3px" /> 恢复
          </el-button>
          <el-button
            type="danger"
            link
            @click="handlePurge(row)"
            v-hasPermi="['lunaerp:common:recycle:purge']"
          >
            <Icon icon="ep:delete" class="mr-3px" /> 彻底删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import * as RecycleApi from '@/api/lunaerp/common/recycle'
import type { RecycleItemRespVO } from '@/api/lunaerp/common/recycle'

defineOptions({ name: 'LunaerpRecycleBin' })

const message = ElMessage

const bizTypes = [
  { label: '客户档案', value: 'CUSTOMER' },
  { label: '供应商档案', value: 'SUPPLIER' },
  { label: '产品档案', value: 'PRODUCT' },
  { label: '纱线材质', value: 'YARN_TYPE' },
  { label: '纱线规格', value: 'YARN_SPEC' },
  { label: '销售合同', value: 'SALES_CONTRACT' },
  { label: '胚布单', value: 'GREIGE_ORDER' },
  { label: '染色单', value: 'DYEING_ORDER' },
  { label: '纱线采购单', value: 'YARN_PURCHASE_ORDER' },
  { label: '入库单', value: 'STOCK_IN' },
  { label: '织造单', value: 'WEAVING_ORDER' },
  { label: '发货单', value: 'SHIPMENT' }
]

const activeBizType = ref('CUSTOMER')
const loading = ref(false)
const total = ref(0)
const list = ref<RecycleItemRespVO[]>([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  bizType: 'CUSTOMER'
})

/** 获取回收站列表 */
const getList = async () => {
  loading.value = true
  try {
    queryParams.bizType = activeBizType.value
    const data = await RecycleApi.getRecyclePage(queryParams)
    list.value = data?.list || []
    total.value = data?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 切换 Tab */
const handleTabChange = (name: string | number) => {
  activeBizType.value = String(name)
  queryParams.pageNo = 1
  getList()
}

/** 恢复按钮操作 */
const handleRestore = async (row: RecycleItemRespVO) => {
  try {
    await ElMessageBox.confirm(
      `是否确认恢复【${row.displayName}】？恢复后主数据需手动启用。`,
      '恢复提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    await RecycleApi.restoreRecycleItem({
      bizType: row.bizType,
      bizId: row.bizId
    })
    message.success('恢复成功')
    if (list.value.length === 1 && queryParams.pageNo > 1) {
      queryParams.pageNo--
    }
    await getList()
  } catch {}
}

/** 彻底删除按钮操作 */
const handlePurge = async (row: RecycleItemRespVO) => {
  try {
    await ElMessageBox.confirm(
      `彻底删除【${row.displayName}】后数据将无法找回，是否确认彻底删除？`,
      '彻底删除提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await RecycleApi.purgeRecycleItem(row.bizType, row.bizId)
    message.success('彻底删除成功')
    if (list.value.length === 1 && queryParams.pageNo > 1) {
      queryParams.pageNo--
    }
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped></style>
