<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="样品编号 / 客户 / 产品 / 款号 / 成分"
          clearable
          @keyup.enter="handleQuery"
          class="!w-280px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace()"
          v-hasPermi="['lunaerp:sample:sample-analysis:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新建样品分析
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
      @row-click="handleRowClick"
      class="cursor-pointer"
    >
      <!-- 照片缩略图列（先留空位，后续票补） -->
      <el-table-column label="照片" width="80" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.firstPhotoUrl || (row.photos && row.photos.length > 0)"
            :src="row.firstPhotoUrl || row.photos[0]"
            class="w-40px h-40px rounded"
            fit="cover"
            preview-teleported
            :preview-src-list="[row.firstPhotoUrl || row.photos[0]]"
            @click.stop
          />
          <span v-else class="text-gray-400 text-12px">—</span>
        </template>
      </el-table-column>

      <el-table-column label="样品编号" prop="sampleAnalysisNo" min-width="140">
        <template #default="{ row }">
          <span class="cell-no">{{ row.sampleAnalysisNo || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="客户" prop="clientName" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.clientName || '—' }}
        </template>
      </el-table-column>

      <el-table-column label="产品名称" prop="productName" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.productName || '—' }}
        </template>
      </el-table-column>

      <el-table-column label="客户款号" prop="clientStyleNo" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.clientStyleNo || '—' }}
        </template>
      </el-table-column>

      <el-table-column label="门幅(CM)" prop="width" width="100" align="right">
        <template #default="{ row }">
          {{ row.width != null ? row.width : '—' }}
        </template>
      </el-table-column>

      <el-table-column label="克重(GSM)" prop="weight" width="100" align="right">
        <template #default="{ row }">
          {{ row.weight != null ? row.weight : '—' }}
        </template>
      </el-table-column>

      <el-table-column label="登记人" prop="creatorName" width="100" align="center">
        <template #default="{ row }">
          {{ row.creatorName || '—' }}
        </template>
      </el-table-column>

      <!-- 付款状态标签列（先留空位，后续票补） -->
      <el-table-column label="付款状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isPaid" type="success" size="small">已付款</el-tag>
          <el-tag v-else type="danger" size="small">未付款</el-tag>
        </template>
      </el-table-column>

      <el-table-column
        label="登记日期"
        prop="createTime"
        width="160"
        align="center"
        :formatter="dateFormatter"
      />

      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click.stop="openWorkspace(row.id, true)"
            v-hasPermi="['lunaerp:sample:sample-analysis:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            link
            type="primary"
            @click.stop="openWorkspace(row.id, false)"
            v-hasPermi="['lunaerp:sample:sample-analysis:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click.stop="handleDelete(row.id)"
            v-hasPermi="['lunaerp:sample:sample-analysis:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无样品分析记录" :image-size="90" />
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
import * as SampleAnalysisApi from '@/api/lunaerp-elmui/sample/sample-analysis'
import type { SampleAnalysisVO, SampleAnalysisPageReqVO } from '@/api/lunaerp-elmui/sample/sample-analysis'

defineOptions({ name: 'LunaerpElmuiSampleSampleAnalysis' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<SampleAnalysisVO[]>([])
const queryFormRef = ref()

interface QueryParams {
  pageNo: number
  pageSize: number
  keyword: string
  isPaid?: boolean
}

const queryParams = reactive<QueryParams>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  isPaid: undefined
})

/** 查询样品分析分页 */
const getList = async () => {
  loading.value = true
  try {
    const params: SampleAnalysisPageReqVO = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      isPaid: queryParams.isPaid
    }
    const data = await SampleAnalysisApi.getSampleAnalysisPage(params)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
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

/** 行点击进入只读工作台 */
const handleRowClick = (row: SampleAnalysisVO) => {
  if (row.id) {
    openWorkspace(row.id, true)
  }
}

/** 打开工作台 */
const openWorkspace = (id?: number, viewOnly: boolean = false) => {
  if (!id) {
    router.push('/sample/sample-analysis/workspace')
  } else {
    router.push(
      viewOnly
        ? `/sample/sample-analysis/workspace/${id}?mode=view`
        : `/sample/sample-analysis/workspace/${id}`
    )
  }
}

/** 删除样品分析（逻辑删除） */
const handleDelete = async (id: number) => {
  try {
    await message.confirm('确认删除该样品分析记录吗？删除后编号不释放、不重用。')
    await SampleAnalysisApi.deleteSampleAnalysis(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.cell-no {
  font-weight: 600;
  color: var(--el-color-primary);
}
</style>
