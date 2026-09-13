<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="品种编号" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入品种编号"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="显示名" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入显示名"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="布种" prop="fabricType">
        <el-input
          v-model="queryParams.fabricType"
          placeholder="请输入布种"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="成分" prop="composition">
        <el-input
          v-model="queryParams.composition"
          placeholder="请输入成分"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="纱支" prop="yarnSpec">
        <el-input
          v-model="queryParams.yarnSpec"
          placeholder="请输入纱支"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-200px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['lunaerp:baseinfo:product:greige:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="品种编号" align="center" prop="code" width="110" />
      <el-table-column label="显示名" align="center" prop="name" min-width="180" show-overflow-tooltip />
      <el-table-column label="布种" align="center" prop="fabricType" min-width="100" />
      <el-table-column label="成分" align="center" prop="composition" min-width="120" show-overflow-tooltip />
      <el-table-column label="纱支" align="center" prop="yarnSpec" min-width="120" show-overflow-tooltip />
      <el-table-column label="坯克重(g/m²)" align="center" prop="weightGsm" width="110" />
      <el-table-column label="坯门幅(cm)" align="center" prop="widthCm" width="110" />
      <el-table-column label="工艺类型" align="center" prop="craftType" min-width="100" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'ACTIVE'" type="success">启用</el-tag>
          <el-tag v-else type="info">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['lunaerp:baseinfo:product:greige:update']"
          >
            修改
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['lunaerp:baseinfo:product:greige:delete']"
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

  <!-- 表单弹窗：新增/修改 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="720px"
    :close-on-click-modal="false"
    append-to-body
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="110px"
      @submit.prevent
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="品种编号" prop="code">
            <el-input
              v-model="formData.code"
              :disabled="formType === 'update'"
              placeholder="留空自动生成 GT-XXXX"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio value="ACTIVE">启用</el-radio>
              <el-radio value="INACTIVE">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="显示名" prop="name">
        <el-input
          v-model="formData.name"
          placeholder="留空按规则自动生成，如 30S NR罗马布 260g×160cm"
          clearable
        />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="布种" prop="fabricType">
            <el-input v-model="formData.fabricType" placeholder="请输入布种" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="成分" prop="composition">
            <el-input v-model="formData.composition" placeholder="请输入成分" clearable />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="纱支" prop="yarnSpec">
            <el-input v-model="formData.yarnSpec" placeholder="请输入纱支" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="工艺类型" prop="craftType">
            <el-input v-model="formData.craftType" placeholder="请输入工艺类型" clearable />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="坯克重(g/m²)" prop="weightGsm">
            <el-input-number
              v-model="formData.weightGsm"
              :min="0"
              :precision="2"
              :step="1"
              :controls="false"
              class="!w-100%"
              placeholder="请输入坯克重"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="坯门幅(cm)" prop="widthCm">
            <el-input-number
              v-model="formData.widthCm"
              :min="0"
              :precision="2"
              :step="1"
              :controls="false"
              class="!w-100%"
              placeholder="请输入坯门幅"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="机型" prop="machineType">
        <el-input v-model="formData.machineType" placeholder="请输入机型" clearable />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入备注"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>
</template>
<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import * as GreigeTypeApi from '@/api/lunaerp/baseinfo/greige-type'
import type { GreigeTypeVO } from '@/api/lunaerp/baseinfo/greige-type'
import type { FormInstance, FormRules } from 'element-plus'

defineOptions({ name: 'LunaerpBaseinfoGreigeType' })

const message = ElMessage

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总条数
const list = ref<GreigeTypeVO[]>([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: '',
  name: '',
  fabricType: '',
  composition: '',
  yarnSpec: '',
  status: undefined as string | undefined
})
const queryFormRef = ref() // 搜索的表单

/** 查询坯布品种列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await GreigeTypeApi.getGreigeTypePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

// ========== 新增/修改 ==========
const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formType = ref<'create' | 'update'>('create') // 弹窗类型
const submitLoading = ref(false) // 提交的加载中
const formRef = ref<FormInstance>() // 表单

const defaultFormData = (): GreigeTypeVO => ({
  id: undefined,
  code: '',
  name: '',
  fabricType: '',
  composition: '',
  yarnSpec: '',
  weightGsm: undefined as unknown as number,
  widthCm: undefined as unknown as number,
  craftType: '',
  machineType: '',
  remark: '',
  status: 'ACTIVE'
})
const formData = ref<GreigeTypeVO>(defaultFormData())

/** 数字必须大于 0 的校验 */
const validatePositiveNumber = (_rule: any, value: number, callback: any) => {
  if (value === undefined || value === null || (value as unknown as string) === '') {
    callback(new Error('请输入大于 0 的数字'))
  } else if (Number(value) <= 0) {
    callback(new Error('必须大于 0'))
  } else {
    callback()
  }
}

const formRules = reactive<FormRules>({
  fabricType: [{ required: true, message: '请输入布种', trigger: 'blur' }],
  composition: [{ required: true, message: '请输入成分', trigger: 'blur' }],
  yarnSpec: [{ required: true, message: '请输入纱支', trigger: 'blur' }],
  weightGsm: [{ required: true, validator: validatePositiveNumber, trigger: 'blur' }],
  widthCm: [{ required: true, validator: validatePositiveNumber, trigger: 'blur' }]
})

/** 打开弹窗 */
const openForm = async (type: 'create' | 'update', id?: number) => {
  formType.value = type
  dialogTitle.value = type === 'create' ? '新增坯布品种' : '修改坯布品种'
  formData.value = defaultFormData()
  formRef.value?.clearValidate()
  dialogVisible.value = true
  if (type === 'update' && id) {
    const data = await GreigeTypeApi.getGreigeType(id)
    formData.value = { ...defaultFormData(), ...data }
  }
}

/** 提交表单 */
const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (formType.value === 'create') {
        await GreigeTypeApi.createGreigeType(formData.value)
        message.success('新增成功')
      } else {
        await GreigeTypeApi.updateGreigeType(formData.value)
        message.success('修改成功')
      }
      dialogVisible.value = false
      await getList()
    } finally {
      // 失败（如 matchKey 查重）时弹窗保持打开，仅收尾 loading；错误消息由全局拦截器弹出
      submitLoading.value = false
    }
  })
}

// ========== 删除 ==========
/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await ElMessageBox.confirm('是否确认删除该坯布品种？', '删除提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // 发起删除
    await GreigeTypeApi.deleteGreigeType(id)
    message.success('删除成功')
    // 删除的是当前页最后一条且非第 1 页时，回退一页再查询
    if (list.value.length === 1 && queryParams.pageNo > 1) {
      queryParams.pageNo--
    }
    await getList()
  } catch {}
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
<style lang="scss" scoped></style>
