<template>
  <el-container direction="vertical" class="yarn-master-container">
    <!-- 上方：纱线材质 -->
    <el-header height="auto" class="!p-0">
      <ContentWrap>
        <div class="flex items-center justify-between mb-15px">
          <div class="flex items-center gap-2">
            <span class="text-16px font-bold">纱线材质</span>
            <span class="text-12px text-gray-400">（点击行选中材质，下方规格列表将联动筛选）</span>
          </div>
          <div class="flex items-center gap-2">
            <el-button
              type="primary"
              plain
              @click="openTypeDialog('create')"
              v-hasPermi="['lunaerp:production:yarn-type:create']"
            >
              <Icon icon="ep:plus" class="mr-1" /> 新建材质
            </el-button>
            <el-button @click="getTypeList">
              <Icon icon="ep:refresh" class="mr-1" /> 刷新
            </el-button>
          </div>
        </div>

        <el-table
          v-loading="typeLoading"
          :data="typeList"
          border
          stripe
          highlight-current-row
          @current-change="handleTypeCurrentChange"
          ref="typeTableRef"
          max-height="280"
        >
          <el-table-column label="#" type="index" width="50" align="center" />
          <el-table-column label="材质名称" prop="name" min-width="140">
            <template #default="{ row }">
              <span class="font-medium text-primary">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column label="排序值" prop="sort" width="100" align="center" />
          <el-table-column label="状态" prop="status" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
              </el-tag>
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
                @click.stop="openTypeDialog('update', row)"
                v-hasPermi="['lunaerp:production:yarn-type:update']"
              >
                <Icon icon="ep:edit" class="mr-1" />编辑
              </el-button>
              <el-button
                v-if="row.status === 'ACTIVE'"
                link
                type="warning"
                @click.stop="handleTypeToggleStatus(row, 'INACTIVE')"
                v-hasPermi="['lunaerp:production:yarn-type:update']"
              >
                <Icon icon="ep:video-pause" class="mr-1" />停用
              </el-button>
              <el-button
                v-else
                link
                type="success"
                @click.stop="handleTypeToggleStatus(row, 'ACTIVE')"
                v-hasPermi="['lunaerp:production:yarn-type:update']"
              >
                <Icon icon="ep:video-play" class="mr-1" />启用
              </el-button>
              <el-button
                link
                type="danger"
                @click.stop="handleTypeDelete(row)"
                v-hasPermi="['lunaerp:production:yarn-type:delete']"
              >
                <Icon icon="ep:delete" class="mr-1" />删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无材质数据" :image-size="60" />
          </template>
        </el-table>
      </ContentWrap>
    </el-header>

    <!-- 下方：按选中材质过滤的纱线规格 -->
    <el-main class="!p-0 mt-4">
      <ContentWrap>
        <div class="flex items-center justify-between mb-15px">
          <div class="flex items-center gap-2">
            <span class="text-16px font-bold">纱线规格</span>
            <el-tag v-if="selectedType" type="success" size="small">
              当前材质：{{ selectedType.name }}
            </el-tag>
            <span v-else class="text-12px text-gray-400">请选择上方材质</span>
          </div>
          <div class="flex items-center gap-2">
            <el-button
              type="success"
              plain
              @click="handleDownloadSpecTemplate"
              v-hasPermi="['lunaerp:production:yarn-spec:import']"
            >
              <Icon icon="ep:download" class="mr-1" /> 下载模板
            </el-button>
            <el-button
              type="primary"
              plain
              @click="handleImportSpec"
              v-hasPermi="['lunaerp:production:yarn-spec:import']"
            >
              <Icon icon="ep:upload" class="mr-1" /> 导入规格
            </el-button>
            <el-button
              type="primary"
              plain
              :disabled="!selectedType"
              @click="openSpecDialog('create')"
              v-hasPermi="['lunaerp:production:yarn-type:create']"
            >
              <Icon icon="ep:plus" class="mr-1" /> 新建规格
            </el-button>
          </div>
        </div>

        <!-- 规格搜索区 -->
        <el-form
          class="-mb-15px"
          :model="specQueryParams"
          ref="specQueryFormRef"
          :inline="true"
          label-width="68px"
        >
          <el-form-item label="规格名称" prop="specName">
            <el-input
              v-model="specQueryParams.specName"
              placeholder="如 32S 棉"
              clearable
              @keyup.enter="handleSpecQuery"
              class="!w-200px"
            />
          </el-form-item>
          <el-form-item label="标准名称" prop="standardName">
            <el-input
              v-model="specQueryParams.standardName"
              placeholder="标准名"
              clearable
              @keyup.enter="handleSpecQuery"
              class="!w-200px"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select
              v-model="specQueryParams.status"
              placeholder="全部"
              clearable
              class="!w-120px"
            >
              <el-option label="启用" value="ACTIVE" />
              <el-option label="停用" value="INACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="handleSpecQuery"><Icon icon="ep:search" class="mr-1" />搜索</el-button>
            <el-button @click="resetSpecQuery"><Icon icon="ep:refresh" class="mr-1" />重置</el-button>
          </el-form-item>
        </el-form>

        <!-- 规格列表表格 -->
        <el-table
          v-loading="specLoading"
          :data="specList"
          border
          stripe
          class="mt-4"
        >
          <el-table-column label="所属材质" prop="yarnTypeName" min-width="100" align="center" />
          <el-table-column label="规格名称" prop="specName" min-width="140" show-overflow-tooltip />
          <el-table-column label="标准名称" prop="standardName" min-width="140" show-overflow-tooltip />
          <el-table-column label="纺纱工艺" prop="spinMethod" min-width="100" align="center" />
          <el-table-column label="光泽" prop="luster" min-width="90" align="center" />
          <el-table-column label="参考单价(元)" prop="refPrice" min-width="120" align="right">
            <template #default="{ row }">
              {{ row.refPrice != null ? Number(row.refPrice).toFixed(2) : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
          <el-table-column
            label="创建时间"
            prop="createTime"
            width="170"
            align="center"
            :formatter="dateFormatter"
          />
          <el-table-column label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openSpecDialog('update', row)"
                v-hasPermi="['lunaerp:production:yarn-type:update']"
              >
                <Icon icon="ep:edit" class="mr-1" />编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleSpecDelete(row)"
                v-hasPermi="['lunaerp:production:yarn-type:delete']"
              >
                <Icon icon="ep:delete" class="mr-1" />删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无规格数据" :image-size="70" />
          </template>
        </el-table>

        <Pagination
          :total="specTotal"
          v-model:page="specQueryParams.pageNo"
          v-model:limit="specQueryParams.pageSize"
          @pagination="getSpecList"
        />
      </ContentWrap>
    </el-main>

    <!-- 材质弹窗（新建/编辑） -->
    <el-dialog
      :title="typeDialogTitle"
      v-model="typeDialogVisible"
      width="480px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="typeFormRef"
        :model="typeFormData"
        :rules="typeFormRules"
        label-width="90px"
      >
        <el-form-item label="材质名称" prop="name">
          <el-input v-model="typeFormData.name" placeholder="如 棉 / 涤 / 人棉" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="typeFormData.sort" :min="0" :max="9999" class="!w-full" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="typeFormData.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="typeFormSubmitting" @click="submitTypeForm">
          确 定
        </el-button>
      </template>
    </el-dialog>

    <!-- 规格弹窗（新建/编辑） -->
    <el-dialog
      :title="specDialogTitle"
      v-model="specDialogVisible"
      width="560px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="specFormRef"
        :model="specFormData"
        :rules="specFormRules"
        label-width="100px"
      >
        <el-form-item label="所属材质">
          <el-input :model-value="selectedType?.name || '未选择材质'" disabled class="!w-full" />
        </el-form-item>
        <el-form-item label="规格名称" prop="specName">
          <el-input v-model="specFormData.specName" placeholder="展示用，如 32S 棉" maxlength="128" />
        </el-form-item>
        <el-form-item label="标准名称" prop="standardName">
          <el-input v-model="specFormData.standardName" placeholder="材质内唯一，如 32S-COTTON" maxlength="128" />
        </el-form-item>
        <el-form-item label="纺纱工艺" prop="spinMethod">
          <el-input v-model="specFormData.spinMethod" placeholder="如 环锭纺 / 转杯纺 / 喷气纺" maxlength="64" />
        </el-form-item>
        <el-form-item label="光泽" prop="luster">
          <el-input v-model="specFormData.luster" placeholder="如 半光 / 全光 / 消光" maxlength="64" />
        </el-form-item>
        <el-form-item label="参考单价" prop="refPrice">
          <el-input-number
            v-model="specFormData.refPrice"
            :precision="4"
            :step="0.1"
            :min="0"
            placeholder="元/KG"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="specFormData.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            type="textarea"
            v-model="specFormData.remark"
            placeholder="备注信息"
            :rows="3"
            maxlength="1000"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="specDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="specFormSubmitting" @click="submitSpecForm">
          确 定
        </el-button>
      </template>
    </el-dialog>

    <!-- 规格导入弹窗 -->
    <YarnSpecImportDialog ref="specImportDialogRef" @success="handleSpecImportSuccess" />
  </el-container>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  getYarnTypePage,
  createYarnType,
  updateYarnType,
  deleteYarnType,
  getYarnSpecPage,
  createYarnSpec,
  updateYarnSpec,
  deleteYarnSpec,
  downloadYarnSpecTemplate,
  type YarnTypeVO,
  type YarnSpecVO,
  type YarnSpecPageParam
} from '@/api/lunaerp-elmui/production/yarn-master'
import YarnSpecImportDialog from './YarnSpecImportDialog.vue'

defineOptions({ name: 'LunaerpElmuiProductionYarnMaster' })

// ==================== 材质部分 ====================
const typeLoading = ref(false)
const typeList = ref<YarnTypeVO[]>([])
const selectedType = ref<YarnTypeVO | null>(null)
const typeTableRef = ref()

const typeDialogVisible = ref(false)
const typeDialogTitle = ref('')
const typeDialogAction = ref<'create' | 'update'>('create')
const typeFormSubmitting = ref(false)
const typeFormRef = ref<FormInstance>()
const typeFormData = reactive<YarnTypeVO>({
  name: '',
  sort: 0,
  status: 'ACTIVE'
})
const typeFormRules: FormRules = {
  name: [{ required: true, message: '材质名称不能为空', trigger: 'blur' }]
}

const getTypeList = async () => {
  typeLoading.value = true
  try {
    const res = await getYarnTypePage({ pageNo: 1, pageSize: 100 })
    typeList.value = res.list || []
    if (typeList.value.length > 0) {
      // 保持之前的选择，或者默认选中第一项
      const found = selectedType.value
        ? typeList.value.find((t) => t.id === selectedType.value?.id)
        : null
      const current = found || typeList.value[0]
      selectedType.value = current
      typeTableRef.value?.setCurrentRow(current)
      specQueryParams.yarnTypeId = current.id
    } else {
      selectedType.value = null
      specQueryParams.yarnTypeId = undefined
    }
  } finally {
    typeLoading.value = false
  }
}

const handleTypeCurrentChange = (val: YarnTypeVO | null) => {
  if (!val) return
  selectedType.value = val
  specQueryParams.yarnTypeId = val.id
  specQueryParams.pageNo = 1
  getSpecList()
}

const openTypeDialog = (action: 'create' | 'update', row?: YarnTypeVO) => {
  typeDialogAction.value = action
  typeDialogTitle.value = action === 'create' ? '新建材质' : '编辑材质'
  if (action === 'create') {
    typeFormData.id = undefined
    typeFormData.name = ''
    typeFormData.sort = typeList.value.length + 1
    typeFormData.status = 'ACTIVE'
  } else if (row) {
    typeFormData.id = row.id
    typeFormData.name = row.name
    typeFormData.sort = row.sort ?? 0
    typeFormData.status = row.status ?? 'ACTIVE'
  }
  typeDialogVisible.value = true
}

const submitTypeForm = async () => {
  if (!typeFormRef.value) return
  await typeFormRef.value.validate()
  typeFormSubmitting.value = true
  try {
    if (typeDialogAction.value === 'create') {
      await createYarnType(typeFormData)
      ElMessage.success('创建材质成功')
    } else {
      await updateYarnType(typeFormData)
      ElMessage.success('修改材质成功')
    }
    typeDialogVisible.value = false
    await getTypeList()
  } finally {
    typeFormSubmitting.value = false
  }
}

const handleTypeToggleStatus = async (row: YarnTypeVO, targetStatus: 'ACTIVE' | 'INACTIVE') => {
  const actionText = targetStatus === 'ACTIVE' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定要${actionText}材质【${row.name}】吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: targetStatus === 'INACTIVE' ? 'warning' : 'info'
    })
    await updateYarnType({
      id: row.id,
      name: row.name,
      sort: row.sort,
      status: targetStatus
    })
    ElMessage.success(`${actionText}材质成功`)
    await getTypeList()
  } catch {
    // 用户取消
  }
}

const handleTypeDelete = async (row: YarnTypeVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除材质【${row.name}】吗？删除后不可恢复。`, '二次确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteYarnType(row.id!)
    ElMessage.success('删除材质成功')
    if (selectedType.value?.id === row.id) {
      selectedType.value = null
    }
    await getTypeList()
    await getSpecList()
  } catch {
    // 用户取消
  }
}

// ==================== 规格部分 ====================
const specLoading = ref(false)
const specList = ref<YarnSpecVO[]>([])
const specTotal = ref(0)
const specQueryFormRef = ref<FormInstance>()
const specQueryParams = reactive<YarnSpecPageParam>({
  pageNo: 1,
  pageSize: 10,
  yarnTypeId: undefined,
  specName: undefined,
  standardName: undefined,
  status: undefined
})

const specDialogVisible = ref(false)
const specDialogTitle = ref('')
const specFormSubmitting = ref(false)
const specFormRef = ref<FormInstance>()
const specFormData = reactive<YarnSpecVO>({
  yarnTypeId: 0,
  specName: '',
  standardName: '',
  spinMethod: '',
  luster: '',
  refPrice: undefined,
  remark: '',
  status: 'ACTIVE'
})
const specFormRules: FormRules = {
  specName: [{ required: true, message: '规格名称不能为空', trigger: 'blur' }],
  standardName: [{ required: true, message: '标准名称不能为空', trigger: 'blur' }]
}

const getSpecList = async () => {
  if (!specQueryParams.yarnTypeId) {
    specList.value = []
    specTotal.value = 0
    return
  }
  specLoading.value = true
  try {
    const res = await getYarnSpecPage(specQueryParams)
    specList.value = res.list || []
    specTotal.value = res.total || 0
  } finally {
    specLoading.value = false
  }
}

const handleSpecQuery = () => {
  specQueryParams.pageNo = 1
  getSpecList()
}

const resetSpecQuery = () => {
  specQueryFormRef.value?.resetFields()
  specQueryParams.pageNo = 1
  getSpecList()
}

const openSpecDialog = (action: 'create' | 'update', row?: YarnSpecVO) => {
  specDialogTitle.value = action === 'create' ? '新建规格' : '编辑规格'
  if (action === 'create') {
    specFormData.id = undefined
    specFormData.yarnTypeId = selectedType.value?.id || 0
    specFormData.specName = ''
    specFormData.standardName = ''
    specFormData.spinMethod = ''
    specFormData.luster = ''
    specFormData.refPrice = undefined
    specFormData.remark = ''
    specFormData.status = 'ACTIVE'
  } else if (row) {
    specFormData.id = row.id
    specFormData.yarnTypeId = row.yarnTypeId
    specFormData.specName = row.specName
    specFormData.standardName = row.standardName
    specFormData.spinMethod = row.spinMethod ?? ''
    specFormData.luster = row.luster ?? ''
    specFormData.refPrice = row.refPrice
    specFormData.remark = row.remark ?? ''
    specFormData.status = row.status ?? 'ACTIVE'
  }
  specDialogVisible.value = true
}

const submitSpecForm = async () => {
  if (!specFormRef.value) return
  await specFormRef.value.validate()
  specFormSubmitting.value = true
  try {
    if (!specFormData.id) {
      await createYarnSpec(specFormData)
      ElMessage.success('创建规格成功')
    } else {
      await updateYarnSpec(specFormData)
      ElMessage.success('修改规格成功')
    }
    specDialogVisible.value = false
    await getSpecList()
  } finally {
    specFormSubmitting.value = false
  }
}

const handleSpecDelete = async (row: YarnSpecVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除规格【${row.specName}】吗？`, '二次确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteYarnSpec(row.id!)
    ElMessage.success('删除规格成功')
    await getSpecList()
  } catch {
    // 用户取消
  }
}

const specImportDialogRef = ref()

/** 下载规格导入模板 */
const handleDownloadSpecTemplate = async () => {
  try {
    const res = await downloadYarnSpecTemplate()
    download.excel(res, '纱线规格导入模板.xlsx')
  } catch (e) {
    ElMessage.error('下载模板失败')
  }
}

/** 打开规格导入弹窗 */
const handleImportSpec = () => {
  specImportDialogRef.value?.open()
}

/** 规格导入成功后刷新列表 */
const handleSpecImportSuccess = async () => {
  await getSpecList()
}

onMounted(async () => {
  await getTypeList()
  await getSpecList()
})
</script>

<style scoped>
.yarn-master-container {
  padding: 0;
}
</style>
