<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="search">
        <el-input
          v-model="queryParams.search"
          placeholder="公司名 / 简称 / 电话 / 信用代码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="全部"
          clearable
          class="!w-180px"
        >
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace('create')"
          v-hasPermi="['lunaerp:baseinfo:customer:create']"
        >
          <Icon icon="ep:plus" /> 新建
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="checkedIds.length === 0"
          @click="handleDeleteBatch"
          v-hasPermi="['lunaerp:baseinfo:customer:delete']"
        >
          <Icon icon="ep:delete" /> 批量删除
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      border
      stripe
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column label="客户" prop="companyName" min-width="220">
        <template #default="{ row }">
          <div class="cell-customer">
            <span class="cell-customer-name">{{ row.companyName }}</span>
            <span v-if="row.shortName && row.shortName !== row.companyName" class="cell-short">
              {{ row.shortName }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="客户类型" prop="clientType" width="110" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_CUSTOMER_CLIENT_TYPE" :value="row.clientType" />
        </template>
      </el-table-column>
      <el-table-column label="电话" prop="phone" width="160" align="center">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="合作状态" prop="cooperationStatus" width="120" align="center">
        <template #default="{ row }">
          <DictTag
            v-if="row.cooperationStatus"
            :type="DICT_TYPE.LUNAERP_CUSTOMER_COOPERATION_STATUS"
            :value="row.cooperationStatus"
          />
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        prop="createTime"
        width="170"
        align="center"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openWorkspace('view', row.id)"
            v-hasPermi="['lunaerp:baseinfo:customer:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            link
            type="primary"
            @click="openWorkspace('edit', row.id)"
            v-hasPermi="['lunaerp:baseinfo:customer:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:baseinfo:customer:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无客户数据" :image-size="90" />
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { DICT_TYPE } from '@/utils/dict'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'

defineOptions({ name: 'LunaerpElmuiBaseinfoCustomer' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<CustomerVO[]>([])
const queryFormRef = ref()
const checkedIds = ref<number[]>([])

const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  search: '',
  status: ''
})

/** 查询客户分页 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CustomerApi.getCustomerPage(queryParams)
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

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  getList()
}

/** 打开工作台：create 走无 id 路由，view/edit 走带 id 路由（工作台默认进基础档案 Tab） */
const openWorkspace = (mode: 'create' | 'view' | 'edit', id?: number) => {
  if (mode === 'create') {
    router.push('/baseinfo/customer/workspace')
  } else if (id) {
    router.push(`/baseinfo/customer/workspace/${id}`)
  }
}

/** 表格勾选行变化 */
const handleSelectionChange = (rows: CustomerVO[]) => {
  checkedIds.value = rows.map((row) => row.id!)
}

/** 删除单个客户（二次确认） */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await CustomerApi.deleteCustomer(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 批量删除客户（二次确认） */
const handleDeleteBatch = async () => {
  try {
    await message.delConfirm()
    await CustomerApi.deleteCustomerList(checkedIds.value)
    checkedIds.value = []
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.cell-customer {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .cell-customer-name {
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .cell-short {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  font-style: italic;
  }
}
</style>
