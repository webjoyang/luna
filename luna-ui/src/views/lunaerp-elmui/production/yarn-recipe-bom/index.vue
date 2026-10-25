<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="产品编码" prop="productCode">
        <el-input
          v-model="queryParams.productCode"
          placeholder="请输入产品编码"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="产品名称" prop="productName">
        <el-input
          v-model="queryParams.productName"
          placeholder="请输入产品名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-160px">
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
          @click="openWorkspace()"
          v-hasPermi="['lunaerp:production:yarn-recipe-bom:create']"
        >
          <Icon icon="ep:plus" /> 新建配比BOM
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <!-- 列表区 -->
    <el-table v-loading="loading" :data="list">
      <el-table-column label="产品编码" align="center" prop="productCode" min-width="140" />
      <el-table-column label="产品名称" align="center" prop="productName" min-width="160" />
      <el-table-column label="明细行数" align="center" prop="itemCount" width="100">
        <template #default="scope">
          <span>{{ scope.row.itemCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ scope.row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="备注"
        align="center"
        prop="remark"
        min-width="150"
        show-overflow-tooltip
      />
      <el-table-column label="创建人" align="center" prop="creator" width="120" />
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openWorkspace(scope.row.id)"
            v-hasPermi="['lunaerp:production:yarn-recipe-bom:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['lunaerp:production:yarn-recipe-bom:delete']"
          >
            删除
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
import { dateFormatter } from '@/utils/formatTime'
import {
  getYarnRecipeBomPage,
  deleteYarnRecipeBom,
  type YarnRecipeBomVO,
  type YarnRecipeBomPageParam
} from '@/api/lunaerp-elmui/production/yarn-recipe-bom'

defineOptions({ name: 'LunaerpElmuiProductionYarnRecipeBom' })

const router = useRouter()
const route = useRoute()
const message = useMessage()
const { t } = useI18n()

const loading = ref(true)
const total = ref(0)
const list = ref<YarnRecipeBomVO[]>([])
const queryFormRef = ref()

const queryParams = reactive<YarnRecipeBomPageParam>({
  pageNo: 1,
  pageSize: 10,
  productId: route.query?.productId ? Number(route.query.productId) : undefined,
  productCode: (route.query?.productCode as string) || undefined,
  productName: (route.query?.productName as string) || undefined,
  status: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await getYarnRecipeBomPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const openWorkspace = (id?: number) => {
  if (id) {
    router.push(`/production/yarn-recipe-bom/workspace/${id}`)
  } else {
    router.push('/production/yarn-recipe-bom/workspace')
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await deleteYarnRecipeBom(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
