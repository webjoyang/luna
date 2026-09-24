<template>
  <div class="warehouse-container">
    <el-row :gutter="16">
      <!-- 左侧：仓库列表 -->
      <el-col :span="12">
        <ContentWrap>
          <div class="flex items-center justify-between mb-15px">
            <div class="flex items-center gap-2">
              <span class="text-16px font-bold">仓库管理</span>
              <span class="text-12px text-gray-400">（点击行选中仓库，右侧展示其库位列表）</span>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                type="primary"
                plain
                @click="openWarehouseDialog('create')"
                v-hasPermi="['lunaerp:stock:warehouse:create']"
              >
                <Icon icon="ep:plus" class="mr-1" /> 新建仓库
              </el-button>
              <el-button @click="getWarehouseList">
                <Icon icon="ep:refresh" class="mr-1" /> 刷新
              </el-button>
            </div>
          </div>

          <!-- 搜索区 -->
          <el-form :model="warehouseQueryParams" ref="warehouseQueryFormRef" :inline="true" class="-mb-15px">
            <el-form-item label="关键词" prop="search">
              <el-input
                v-model="warehouseQueryParams.search"
                placeholder="编号 / 名称 / 地址"
                clearable
                @keyup.enter="handleWarehouseQuery"
                class="!w-160px"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="warehouseQueryParams.status"
                placeholder="全部"
                clearable
                class="!w-110px"
              >
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleWarehouseQuery">
                <Icon icon="ep:search" class="mr-1" />搜索
              </el-button>
              <el-button @click="resetWarehouseQuery">
                <Icon icon="ep:refresh" class="mr-1" />重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="warehouseLoading"
            :data="warehouseList"
            border
            stripe
            highlight-current-row
            @current-change="handleWarehouseCurrentChange"
            ref="warehouseTableRef"
            class="mt-15px"
          >
            <el-table-column label="编号" prop="warehouseNo" min-width="130">
              <template #default="{ row }">
                <span class="font-medium text-primary">{{ row.warehouseNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="名称" prop="name" min-width="120" :show-overflow-tooltip="true" />
            <el-table-column label="地址" prop="address" min-width="130" :show-overflow-tooltip="true">
              <template #default="{ row }">{{ row.address || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click.stop="openWarehouseDialog('update', row)"
                  v-hasPermi="['lunaerp:stock:warehouse:update']"
                >
                  <Icon icon="ep:edit" class="mr-1" />编辑
                </el-button>
                <el-button
                  v-if="row.status === 'ACTIVE'"
                  link
                  type="warning"
                  @click.stop="handleWarehouseToggleStatus(row, 'INACTIVE')"
                  v-hasPermi="['lunaerp:stock:warehouse:update']"
                >
                  <Icon icon="ep:video-pause" class="mr-1" />停用
                </el-button>
                <el-button
                  v-else
                  link
                  type="success"
                  @click.stop="handleWarehouseToggleStatus(row, 'ACTIVE')"
                  v-hasPermi="['lunaerp:stock:warehouse:update']"
                >
                  <Icon icon="ep:video-play" class="mr-1" />启用
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click.stop="handleWarehouseDelete(row)"
                  v-hasPermi="['lunaerp:stock:warehouse:delete']"
                >
                  <Icon icon="ep:delete" class="mr-1" />删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无仓库数据" :image-size="60" />
            </template>
          </el-table>

          <Pagination
            :total="warehouseTotal"
            v-model:page="warehouseQueryParams.pageNo"
            v-model:limit="warehouseQueryParams.pageSize"
            @pagination="getWarehouseList"
          />
        </ContentWrap>
      </el-col>

      <!-- 右侧：选中仓库的库位列表 -->
      <el-col :span="12">
        <ContentWrap>
          <div class="flex items-center justify-between mb-15px">
            <div class="flex items-center gap-2">
              <span class="text-16px font-bold">库位列表</span>
              <el-tag v-if="selectedWarehouse" type="success" size="small">
                当前仓库：{{ selectedWarehouse.warehouseNo }} - {{ selectedWarehouse.name }}
              </el-tag>
              <span v-else class="text-12px text-gray-400">请选择左侧仓库</span>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                type="primary"
                plain
                :disabled="!selectedWarehouse"
                @click="openLocationDialog('create')"
                v-hasPermi="['lunaerp:stock:warehouse-location:create']"
              >
                <Icon icon="ep:plus" class="mr-1" /> 新建库位
              </el-button>
              <el-button :disabled="!selectedWarehouse" @click="getLocationList">
                <Icon icon="ep:refresh" class="mr-1" /> 刷新
              </el-button>
            </div>
          </div>

          <!-- 搜索区 -->
          <el-form :model="locationQueryParams" ref="locationQueryFormRef" :inline="true" class="-mb-15px">
            <el-form-item label="关键词" prop="search">
              <el-input
                v-model="locationQueryParams.search"
                placeholder="编码 / 名称"
                clearable
                :disabled="!selectedWarehouse"
                @keyup.enter="handleLocationQuery"
                class="!w-160px"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="locationQueryParams.status"
                placeholder="全部"
                clearable
                :disabled="!selectedWarehouse"
                class="!w-110px"
              >
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button :disabled="!selectedWarehouse" @click="handleLocationQuery">
                <Icon icon="ep:search" class="mr-1" />搜索
              </el-button>
              <el-button :disabled="!selectedWarehouse" @click="resetLocationQuery">
                <Icon icon="ep:refresh" class="mr-1" />重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table
            v-loading="locationLoading"
            :data="locationList"
            border
            stripe
            class="mt-15px"
          >
            <el-table-column label="库位编码" prop="code" min-width="120">
              <template #default="{ row }">
                <span class="font-medium text-primary">{{ row.code }}</span>
              </template>
            </el-table-column>
            <el-table-column label="库位名称" prop="name" min-width="120" :show-overflow-tooltip="true">
              <template #default="{ row }">{{ row.name || '—' }}</template>
            </el-table-column>
            <el-table-column label="排序" prop="sort" width="70" align="center" />
            <el-table-column label="状态" prop="status" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="110" :show-overflow-tooltip="true">
              <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openLocationDialog('update', row)"
                  v-hasPermi="['lunaerp:stock:warehouse-location:update']"
                >
                  <Icon icon="ep:edit" class="mr-1" />编辑
                </el-button>
                <el-button
                  v-if="row.status === 'ACTIVE'"
                  link
                  type="warning"
                  @click="handleLocationToggleStatus(row, 'INACTIVE')"
                  v-hasPermi="['lunaerp:stock:warehouse-location:update']"
                >
                  <Icon icon="ep:video-pause" class="mr-1" />停用
                </el-button>
                <el-button
                  v-else
                  link
                  type="success"
                  @click="handleLocationToggleStatus(row, 'ACTIVE')"
                  v-hasPermi="['lunaerp:stock:warehouse-location:update']"
                >
                  <Icon icon="ep:video-play" class="mr-1" />启用
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleLocationDelete(row)"
                  v-hasPermi="['lunaerp:stock:warehouse-location:delete']"
                >
                  <Icon icon="ep:delete" class="mr-1" />删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty
                :description="selectedWarehouse ? '该仓库暂无库位数据' : '请先选择左侧仓库'"
                :image-size="60"
              />
            </template>
          </el-table>

          <Pagination
            :total="locationTotal"
            v-model:page="locationQueryParams.pageNo"
            v-model:limit="locationQueryParams.pageSize"
            @pagination="getLocationList"
          />
        </ContentWrap>
      </el-col>
    </el-row>

    <!-- 仓库弹窗（新建/编辑） -->
    <el-dialog
      :title="warehouseDialogTitle"
      v-model="warehouseDialogVisible"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="warehouseFormRef"
        :model="warehouseFormData"
        :rules="warehouseFormRules"
        label-width="100px"
      >
        <el-form-item label="仓库编号" prop="warehouseNo">
          <div class="flex items-center gap-2 w-full">
            <el-input
              v-model="warehouseFormData.warehouseNo"
              placeholder="留空自动生成（WH+年份+4位流水）"
              maxlength="32"
            />
            <el-button
              v-if="warehouseDialogAction === 'create'"
              @click="handleFetchNextWarehouseNo"
              size="small"
              type="info"
              plain
            >
              预览编号
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="warehouseFormData.name" placeholder="请输入仓库名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="仓库地址" prop="address">
          <el-input v-model="warehouseFormData.address" placeholder="请输入仓库地址" maxlength="255" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="warehouseFormData.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            type="textarea"
            v-model="warehouseFormData.remark"
            placeholder="备注信息"
            :rows="3"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warehouseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="warehouseFormSubmitting" @click="submitWarehouseForm">
          确 定
        </el-button>
      </template>
    </el-dialog>

    <!-- 库位弹窗（新建/编辑） -->
    <el-dialog
      :title="locationDialogTitle"
      v-model="locationDialogVisible"
      width="500px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="locationFormRef"
        :model="locationFormData"
        :rules="locationFormRules"
        label-width="100px"
      >
        <el-form-item label="所属仓库">
          <el-input :value="selectedWarehouse ? selectedWarehouse.name : ''" disabled />
        </el-form-item>
        <el-form-item label="库位编码" prop="code">
          <el-input
            v-model="locationFormData.code"
            placeholder="如 A-01-02（同仓库内唯一）"
            maxlength="32"
          />
        </el-form-item>
        <el-form-item label="库位名称" prop="name">
          <el-input v-model="locationFormData.name" placeholder="如 A区1排2层" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="locationFormData.sort" :min="0" :max="9999" class="!w-full" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="locationFormData.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            type="textarea"
            v-model="locationFormData.remark"
            placeholder="备注信息"
            :rows="3"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="locationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="locationFormSubmitting" @click="submitLocationForm">
          确 定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import {
  getWarehousePage,
  createWarehouse,
  updateWarehouse,
  deleteWarehouse,
  getNextWarehouseNo,
  type WarehouseVO,
  type WarehousePageParam
} from '@/api/lunaerp-elmui/stock/warehouse'
import {
  getWarehouseLocationPage,
  createWarehouseLocation,
  updateWarehouseLocation,
  deleteWarehouseLocation,
  type WarehouseLocationVO,
  type WarehouseLocationPageParam
} from '@/api/lunaerp-elmui/stock/warehouse-location'

defineOptions({ name: 'LunaerpElmuiStockWarehouse' })

const message = useMessage()

// ==================== 仓库部分 ====================
const warehouseLoading = ref(false)
const warehouseList = ref<WarehouseVO[]>([])
const warehouseTotal = ref(0)
const selectedWarehouse = ref<WarehouseVO | null>(null)
const warehouseTableRef = ref()

const warehouseQueryParams = reactive<WarehousePageParam>({
  pageNo: 1,
  pageSize: 10,
  search: '',
  status: undefined
})

const warehouseDialogVisible = ref(false)
const warehouseDialogTitle = ref('')
const warehouseDialogAction = ref<'create' | 'update'>('create')
const warehouseFormSubmitting = ref(false)
const warehouseFormRef = ref<FormInstance>()
const warehouseFormData = reactive<WarehouseVO>({
  warehouseNo: '',
  name: '',
  address: '',
  status: 'ACTIVE',
  remark: ''
})
const warehouseFormRules: FormRules = {
  name: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }]
}

const getWarehouseList = async () => {
  warehouseLoading.value = true
  try {
    const res = await getWarehousePage(warehouseQueryParams)
    warehouseList.value = res.list || []
    warehouseTotal.value = res.total || 0

    // 维持或默认选中第一项
    if (warehouseList.value.length > 0) {
      const currentId = selectedWarehouse.value?.id
      const found = warehouseList.value.find((item) => item.id === currentId)
      if (found) {
        selectedWarehouse.value = found
        nextTick(() => {
          warehouseTableRef.value?.setCurrentRow(found)
        })
      } else {
        selectedWarehouse.value = warehouseList.value[0]
        nextTick(() => {
          warehouseTableRef.value?.setCurrentRow(warehouseList.value[0])
        })
        getLocationList()
      }
    } else {
      selectedWarehouse.value = null
      locationList.value = []
      locationTotal.value = 0
    }
  } finally {
    warehouseLoading.value = false
  }
}

const handleWarehouseQuery = () => {
  warehouseQueryParams.pageNo = 1
  getWarehouseList()
}

const resetWarehouseQuery = () => {
  warehouseQueryParams.search = ''
  warehouseQueryParams.status = undefined
  handleWarehouseQuery()
}

const handleWarehouseCurrentChange = (row: WarehouseVO | undefined) => {
  if (row && row.id !== selectedWarehouse.value?.id) {
    selectedWarehouse.value = row
    locationQueryParams.pageNo = 1
    getLocationList()
  }
}

const openWarehouseDialog = (action: 'create' | 'update', row?: WarehouseVO) => {
  warehouseDialogAction.value = action
  warehouseDialogTitle.value = action === 'create' ? '新建仓库' : '修改仓库'
  warehouseDialogVisible.value = true
  if (action === 'create') {
    Object.assign(warehouseFormData, {
      id: undefined,
      warehouseNo: '',
      name: '',
      address: '',
      status: 'ACTIVE',
      remark: ''
    })
  } else if (row) {
    Object.assign(warehouseFormData, {
      id: row.id,
      warehouseNo: row.warehouseNo,
      name: row.name,
      address: row.address,
      status: row.status,
      remark: row.remark
    })
  }
}

const handleFetchNextWarehouseNo = async () => {
  try {
    const nextNo = await getNextWarehouseNo()
    warehouseFormData.warehouseNo = nextNo
  } catch {}
}

const submitWarehouseForm = async () => {
  if (!warehouseFormRef.value) return
  await warehouseFormRef.value.validate()
  warehouseFormSubmitting.value = true
  try {
    if (warehouseDialogAction.value === 'create') {
      await createWarehouse(warehouseFormData)
      message.success('创建仓库成功')
    } else {
      await updateWarehouse(warehouseFormData)
      message.success('修改仓库成功')
    }
    warehouseDialogVisible.value = false
    await getWarehouseList()
  } finally {
    warehouseFormSubmitting.value = false
  }
}

const handleWarehouseToggleStatus = async (row: WarehouseVO, newStatus: string) => {
  const actionText = newStatus === 'ACTIVE' ? '启用' : '停用'
  try {
    await message.confirm(`确认要${actionText}仓库【${row.name}】吗？`)
    await updateWarehouse({
      id: row.id,
      name: row.name,
      status: newStatus
    })
    message.success(`${actionText}成功`)
    await getWarehouseList()
  } catch {}
}

const handleWarehouseDelete = async (row: WarehouseVO) => {
  try {
    await message.confirm(`确认要删除仓库【${row.name}】吗？若仓库下有库位或已有入库单引用将禁止删除。`)
    await deleteWarehouse(row.id!)
    message.success('删除仓库成功')
    if (selectedWarehouse.value?.id === row.id) {
      selectedWarehouse.value = null
    }
    await getWarehouseList()
  } catch {}
}

// ==================== 库位部分 ====================
const locationLoading = ref(false)
const locationList = ref<WarehouseLocationVO[]>([])
const locationTotal = ref(0)

const locationQueryParams = reactive<WarehouseLocationPageParam>({
  pageNo: 1,
  pageSize: 10,
  warehouseId: undefined,
  search: '',
  status: undefined
})

const locationDialogVisible = ref(false)
const locationDialogTitle = ref('')
const locationDialogAction = ref<'create' | 'update'>('create')
const locationFormSubmitting = ref(false)
const locationFormRef = ref<FormInstance>()
const locationFormData = reactive<WarehouseLocationVO>({
  warehouseId: 0,
  code: '',
  name: '',
  sort: 0,
  status: 'ACTIVE',
  remark: ''
})
const locationFormRules: FormRules = {
  code: [{ required: true, message: '库位编码不能为空', trigger: 'blur' }]
}

const getLocationList = async () => {
  if (!selectedWarehouse.value?.id) {
    locationList.value = []
    locationTotal.value = 0
    return
  }
  locationLoading.value = true
  try {
    locationQueryParams.warehouseId = selectedWarehouse.value.id
    const res = await getWarehouseLocationPage(locationQueryParams)
    locationList.value = res.list || []
    locationTotal.value = res.total || 0
  } finally {
    locationLoading.value = false
  }
}

const handleLocationQuery = () => {
  locationQueryParams.pageNo = 1
  getLocationList()
}

const resetLocationQuery = () => {
  locationQueryParams.search = ''
  locationQueryParams.status = undefined
  handleLocationQuery()
}

const openLocationDialog = (action: 'create' | 'update', row?: WarehouseLocationVO) => {
  if (!selectedWarehouse.value?.id) return
  locationDialogAction.value = action
  locationDialogTitle.value = action === 'create' ? '新建库位' : '修改库位'
  locationDialogVisible.value = true
  if (action === 'create') {
    Object.assign(locationFormData, {
      id: undefined,
      warehouseId: selectedWarehouse.value.id,
      code: '',
      name: '',
      sort: 0,
      status: 'ACTIVE',
      remark: ''
    })
  } else if (row) {
    Object.assign(locationFormData, {
      id: row.id,
      warehouseId: row.warehouseId,
      code: row.code,
      name: row.name,
      sort: row.sort,
      status: row.status,
      remark: row.remark
    })
  }
}

const submitLocationForm = async () => {
  if (!locationFormRef.value) return
  await locationFormRef.value.validate()
  locationFormSubmitting.value = true
  try {
    if (locationDialogAction.value === 'create') {
      await createWarehouseLocation(locationFormData)
      message.success('创建库位成功')
    } else {
      await updateWarehouseLocation(locationFormData)
      message.success('修改库位成功')
    }
    locationDialogVisible.value = false
    await getLocationList()
  } finally {
    locationFormSubmitting.value = false
  }
}

const handleLocationToggleStatus = async (row: WarehouseLocationVO, newStatus: string) => {
  const actionText = newStatus === 'ACTIVE' ? '启用' : '停用'
  try {
    await message.confirm(`确认要${actionText}库位【${row.code}】吗？`)
    await updateWarehouseLocation({
      id: row.id,
      warehouseId: row.warehouseId,
      code: row.code,
      status: newStatus
    })
    message.success(`${actionText}成功`)
    await getLocationList()
  } catch {}
}

const handleLocationDelete = async (row: WarehouseLocationVO) => {
  try {
    await message.confirm(`确认要删除库位【${row.code}】吗？若已被入库放置记录引用将禁止删除。`)
    await deleteWarehouseLocation(row.id!)
    message.success('删除库位成功')
    await getLocationList()
  } catch {}
}

onMounted(() => {
  getWarehouseList()
})
</script>

<style scoped>
.warehouse-container {
  padding: 0;
}
</style>
