<template>
  <div v-loading="loading" class="items-tab">
    <!-- 未创建主表时提示 -->
    <el-empty
      v-if="isCreateMode"
      description="请先保存基本信息后再录检"
      :image-size="100"
    >
      <el-button type="primary" plain @click="emit('switch-to-basic')">
        前往填写基本信息
      </el-button>
    </el-empty>

    <!-- 已有检验单：明细 CRUD 表格 -->
    <template v-else>
      <!-- 顶部工具栏：添加明细 + 同步入库（默认禁用） -->
      <div class="items-toolbar flex justify-between items-center mb-16px">
        <div class="flex items-center gap-3">
          <el-button
            type="primary"
            plain
            :disabled="!canEdit"
            @click="openFormDialog()"
            v-hasPermi="['lunaerp:production:inspection-order-item:create']"
          >
            <Icon icon="ep:plus" class="mr-2px" />添加明细
          </el-button>

          <el-tooltip content="功能开发中（ADR-0013）" placement="top">
            <span>
              <el-button
                type="warning"
                plain
                disabled
                v-hasPermi="['lunaerp:production:inspection-order:sync']"
              >
                <Icon icon="ep:upload" class="mr-2px" />同步入库
              </el-button>
            </span>
          </el-tooltip>
        </div>

        <div class="text-gray-500 text-13px">
          共 <span class="font-bold text-primary">{{ total }}</span> 卷
        </div>
      </div>

      <!-- 表格列：卷号/缸号/实称重量/米数/机台号/等级(字典下拉)/颜色/色号/款号/匹数/备注/操作 -->
      <el-table :data="list" border stripe class="w-full">
        <el-table-column label="卷号" prop="rollNo" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cell-bold">{{ row.rollNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缸号" prop="vatNo" min-width="110" show-overflow-tooltip />
        <el-table-column label="实称重量" prop="measuredWeight" min-width="110" align="right">
          <template #default="{ row }">
            {{ row.measuredWeight != null ? `${row.measuredWeight} KG` : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="米数" prop="lengthM" min-width="100" align="right">
          <template #default="{ row }">
            {{ row.lengthM != null ? `${row.lengthM} M` : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="机台号" prop="machineNo" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.machineNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="等级" prop="grade" width="100" align="center">
          <template #default="{ row }">
            <DictTag
              v-if="row.grade"
              :type="DICT_TYPE.LUNAERP_INSPECTION_GRADE"
              :value="row.grade"
            />
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="颜色" prop="colorName" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.colorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="色号" prop="colorNo" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.colorNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="款号" prop="styleNo" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.styleNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="匹数" prop="pieceCount" width="80" align="right">
          <template #default="{ row }">{{ row.pieceCount ?? 1 }}</template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="canEdit">
              <el-button
                link
                type="primary"
                @click="openFormDialog(row)"
                v-hasPermi="['lunaerp:production:inspection-order-item:update']"
              >
                <Icon icon="ep:edit" class="mr-2px" />编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(row.id)"
                v-hasPermi="['lunaerp:production:inspection-order-item:delete']"
              >
                <Icon icon="ep:delete" class="mr-2px" />删除
              </el-button>
            </template>
            <span v-else class="text-gray-400">只读</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无录检明细数据" :image-size="80" />
        </template>
      </el-table>

      <!-- 底部栏：添加明细 + 分页器 -->
      <div class="items-bottom-bar flex justify-between items-center mt-16px">
        <el-button
          type="primary"
          plain
          :disabled="!canEdit"
          @click="openFormDialog()"
          v-hasPermi="['lunaerp:production:inspection-order-item:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" />添加明细
        </el-button>

        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </template>

    <!-- 单卷新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="640px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        class="inspection-item-form"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="缸号" prop="vatNo">
              <el-input v-model="formData.vatNo" placeholder="请输入缸号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="卷号" prop="rollNo">
              <el-input v-model="formData.rollNo" placeholder="请输入卷号" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="实称重量" prop="measuredWeight">
              <el-input-number
                v-model="formData.measuredWeight"
                :min="0"
                :precision="2"
                :step="0.5"
                placeholder="重量 KG"
                class="!w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="米数" prop="lengthM">
              <el-input-number
                v-model="formData.lengthM"
                :min="0"
                :precision="2"
                :step="1"
                placeholder="米数 M"
                class="!w-full"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="等级" prop="grade">
              <el-select v-model="formData.grade" placeholder="请选择等级" clearable class="!w-full">
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_INSPECTION_GRADE)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机台号" prop="machineNo">
              <el-input v-model="formData.machineNo" placeholder="请输入机台号" maxlength="32" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="颜色" prop="colorName">
              <el-input v-model="formData.colorName" placeholder="请输入颜色名称" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="色号" prop="colorNo">
              <el-input v-model="formData.colorNo" placeholder="请输入产品色号" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="款号" prop="styleNo">
              <el-input v-model="formData.styleNo" placeholder="请输入款号" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="匹数" prop="pieceCount">
              <el-input-number
                v-model="formData.pieceCount"
                :min="1"
                :precision="0"
                placeholder="默认 1 匹"
                class="!w-full"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as InspectionOrderItemApi from '@/api/lunaerp-elmui/production/inspection-order-item'
import type {
  InspectionOrderItemVO,
  InspectionOrderItemSaveReqVO
} from '@/api/lunaerp-elmui/production/inspection-order-item'

defineOptions({ name: 'InspectionOrderItemsTab' })

const props = defineProps<{
  /** 所属检验单 ID */
  orderId?: number | null
  /** 检验单状态：DRAFT / ACTIVE / COMPLETED / CANCELLED */
  orderStatus?: string
  /** 是否只读查看模式 */
  isViewMode?: boolean
  /** 默认产品编码（取自主单据） */
  defaultProductCode?: string
  /** 默认产品名称（取自主单据） */
  defaultProductName?: string
}>()

const emit = defineEmits<{
  (e: 'item-changed'): void
  (e: 'switch-to-basic'): void
}>()

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)
const list = ref<InspectionOrderItemVO[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 20
})

const isCreateMode = computed(() => !props.orderId)
const canEdit = computed(() => {
  if (props.isViewMode) return false
  return props.orderStatus === 'DRAFT' || props.orderStatus === 'ACTIVE'
})

// 弹窗状态与表单
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<InspectionOrderItemSaveReqVO>({
  id: undefined,
  inspectionOrderId: 0,
  rollNo: '',
  vatNo: '',
  measuredWeight: undefined,
  lengthM: undefined,
  machineNo: '',
  grade: 'A',
  colorName: '',
  colorNo: '',
  styleNo: '',
  productCode: '',
  productName: '',
  pieceCount: 1,
  remark: ''
})

const dialogTitle = computed(() => (formData.id ? '编辑录检明细' : '添加录检明细'))

const rules: FormRules = {
  vatNo: [{ required: true, message: '缸号不能为空', trigger: 'blur' }],
  rollNo: [{ required: true, message: '卷号不能为空', trigger: 'blur' }]
}

/** 查询录检明细分页列表 */
const getList = async () => {
  if (!props.orderId) {
    list.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await InspectionOrderItemApi.getInspectionOrderItemPage({
      inspectionOrderId: props.orderId,
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

/** 打开新增或编辑弹窗 */
const openFormDialog = (row?: InspectionOrderItemVO) => {
  formRef.value?.resetFields()
  if (row) {
    Object.assign(formData, {
      id: row.id,
      inspectionOrderId: props.orderId || row.inspectionOrderId,
      rollNo: row.rollNo || '',
      vatNo: row.vatNo || '',
      measuredWeight: row.measuredWeight,
      lengthM: row.lengthM,
      machineNo: row.machineNo || '',
      grade: row.grade || 'A',
      colorName: row.colorName || '',
      colorNo: row.colorNo || '',
      styleNo: row.styleNo || '',
      productCode: row.productCode || props.defaultProductCode || '',
      productName: row.productName || props.defaultProductName || '',
      pieceCount: row.pieceCount ?? 1,
      remark: row.remark || ''
    })
  } else {
    Object.assign(formData, {
      id: undefined,
      inspectionOrderId: props.orderId || 0,
      rollNo: '',
      vatNo: list.value.length > 0 ? list.value[0].vatNo : '',
      measuredWeight: undefined,
      lengthM: undefined,
      machineNo: '',
      grade: 'A',
      colorName: '',
      colorNo: '',
      styleNo: '',
      productCode: props.defaultProductCode || '',
      productName: props.defaultProductName || '',
      pieceCount: 1,
      remark: ''
    })
  }
  dialogVisible.value = true
}

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value || !props.orderId) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (formData.id) {
      await InspectionOrderItemApi.updateInspectionOrderItem({
        ...formData,
        inspectionOrderId: props.orderId
      })
      message.success('修改成功')
    } else {
      await InspectionOrderItemApi.createInspectionOrderItem({
        ...formData,
        inspectionOrderId: props.orderId
      })
      message.success('添加成功')
    }
    dialogVisible.value = false
    emit('item-changed')
    await getList()
  } finally {
    submitting.value = false
  }
}

/** 删除单卷明细 */
const handleDelete = async (id?: number) => {
  if (!id) return
  try {
    await message.delConfirm('是否确认删除该卷明细？')
    await InspectionOrderItemApi.deleteInspectionOrderItem(id)
    message.success('删除成功')
    emit('item-changed')
    await getList()
  } catch {}
}

// 监听 orderId 变动
watch(
  () => props.orderId,
  (newId) => {
    if (newId) {
      queryParams.pageNo = 1
      getList()
    } else {
      list.value = []
      total.value = 0
    }
  },
  { immediate: true }
)

defineExpose({
  getList
})
</script>

<style lang="scss" scoped>
.items-tab {
  padding: 8px 4px;
}

.items-toolbar {
  padding-bottom: 8px;
}

.cell-bold {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.inspection-item-form {
  padding: 0 12px;
}
</style>
