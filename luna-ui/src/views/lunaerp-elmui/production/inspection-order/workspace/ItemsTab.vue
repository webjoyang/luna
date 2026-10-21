<template>
  <div v-loading="loading" class="items-tab">
    <!-- 未创建主表时提示 -->
    <el-empty v-if="isCreateMode" description="请先保存基本信息后再录检" :image-size="100">
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
          <el-button
            type="warning"
            plain
            :disabled="!canGenerateConfirmation"
            :loading="generatingConfirmation"
            @click="handleGenerateConfirmation"
            v-hasPermi="['lunaerp:production:inspection-confirmation:generate']"
          >
            <Icon icon="ep:circle-check" class="mr-2px" />生成质检确认
          </el-button>
        </div>

        <div class="text-gray-500 text-13px">
          共 <span class="font-bold text-primary">{{ total }}</span> {{ isPieceMode ? '匹' : '卷' }}
        </div>
      </div>

      <!-- 表格列：卷号/匹号/缸号/实称重量/米数/机台号/等级(字典下拉)/颜色/色号/款号/匹数/备注/操作 -->
      <el-table :data="list" border stripe class="w-full">
        <!-- PIECE 模式：匹号 + 卷号（可选） -->
        <el-table-column
          v-if="isPieceMode"
          label="匹号"
          prop="pieceNo"
          min-width="140"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="cell-bold">{{ row.pieceNo }}</span>
            <el-tag v-if="row.weavingTicketId" size="small" type="success" class="ml-1"
              >已联布票</el-tag
            >
          </template>
        </el-table-column>
        <!-- ROLL 模式：卷号 -->
        <el-table-column
          v-if="!isPieceMode"
          label="卷号"
          prop="rollNo"
          min-width="110"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span class="cell-bold">{{ row.rollNo }}</span>
          </template>
        </el-table-column>
        <!-- PIECE 模式：卷号（可选，一卷拆多匹） -->
        <el-table-column
          v-if="isPieceMode"
          label="卷号"
          prop="rollNo"
          min-width="110"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ row.rollNo || '—' }}</template>
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
        <el-table-column label="疵点照片" width="100" align="center">
          <template #default="{ row }">
            <div
              v-if="row.photos && row.photos.length > 0"
              class="flex items-center justify-center gap-1"
            >
              <el-image
                :src="row.photos[0]"
                class="w-36px h-36px rounded border border-gray-200 cursor-pointer object-cover"
                fit="cover"
                preview-teleported
                :preview-src-list="row.photos"
                @click.stop
              />
              <span v-if="row.photos.length > 1" class="text-xs text-gray-500 font-mono">
                +{{ row.photos.length }}
              </span>
            </div>
            <span v-else class="text-gray-300">—</span>
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
        <el-table-column
          v-if="!isPieceMode"
          label="匹数"
          prop="pieceCount"
          width="80"
          align="right"
        >
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

    <!-- 单卷/匹新增/编辑弹窗 -->
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
          <el-col v-if="isPieceMode" :span="12">
            <el-form-item label="匹号" prop="pieceNo">
              <el-input
                v-model="formData.pieceNo"
                placeholder="输入匹号或扫描布票号"
                maxlength="64"
                clearable
                @blur="handlePieceNoBlur"
              />
            </el-form-item>
          </el-col>
          <el-col v-else :span="12">
            <el-form-item label="卷号" prop="rollNo">
              <el-input v-model="formData.rollNo" placeholder="请输入卷号" maxlength="64" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-if="isPieceMode" :gutter="16">
          <el-col :span="12">
            <el-form-item label="卷号" prop="rollNo">
              <el-input
                v-model="formData.rollNo"
                placeholder="请输入卷号（可选，一卷拆多匹）"
                maxlength="64"
              />
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
              <el-select
                v-model="formData.grade"
                placeholder="请选择等级"
                clearable
                class="!w-full"
              >
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_INSPECTION_GRADE)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="!isPieceMode" :span="12">
            <el-form-item label="机台号" prop="machineNo">
              <el-input v-model="formData.machineNo" placeholder="请输入机台号" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col v-else :span="12">
            <el-form-item label="款号" prop="styleNo">
              <el-input v-model="formData.styleNo" placeholder="请输入款号" maxlength="64" />
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

        <el-row v-if="!isPieceMode" :gutter="16">
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

        <el-form-item v-if="formData.photos && formData.photos.length > 0" label="疵点照片">
          <div class="flex items-center gap-2 flex-wrap">
            <el-image
              v-for="(url, idx) in formData.photos"
              :key="idx"
              :src="url"
              class="w-54px h-54px rounded border border-gray-200 cursor-pointer object-cover"
              fit="cover"
              preview-teleported
              :preview-src-list="formData.photos"
              :initial-index="idx"
            />
          </div>
          <div class="text-xs text-gray-400 mt-1">（现场 H5 录检上传，PC 仅供只读查看）</div>
        </el-form-item>

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
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
          v-hasPermi="[
            'lunaerp:production:inspection-order-item:create',
            'lunaerp:production:inspection-order-item:update'
          ]"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as InspectionOrderItemApi from '@/api/lunaerp-elmui/production/inspection-order-item'
import type {
  InspectionOrderItemVO,
  InspectionOrderItemSaveReqVO
} from '@/api/lunaerp-elmui/production/inspection-order-item'
import * as InspectionConfirmationApi from '@/api/lunaerp-elmui/production/inspection-confirmation'
import { getWeavingTicketByCode } from '@/api/lunaerp-elmui/production/weaving-ticket'

defineOptions({ name: 'InspectionOrderItemsTab' })

const props = defineProps<{
  /** 所属检验单 ID */
  orderId?: number | null
  /** 检验单状态：DRAFT / ACTIVE / COMPLETED / CANCELLED */
  orderStatus?: string
  /** 检验类型：GREIGE / FINISHED */
  orderType?: string
  /** 包装模式：ROLL / PIECE */
  packageMode?: string
  /** 是否只读查看模式 */
  isViewMode?: boolean
  /** 默认产品编码（取自主单据） */
  defaultProductCode?: string
  /** 默认产品名称（取自主单据） */
  defaultProductName?: string
}>()

const isPieceMode = computed(() => props.packageMode === 'PIECE')

const emit = defineEmits<{
  (e: 'item-changed'): void
  (e: 'switch-to-basic'): void
}>()

const message = useMessage()
const { push } = useRouter()

const loading = ref(false)
const submitting = ref(false)
const generatingConfirmation = ref(false)
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
const canGenerateConfirmation = computed(() => {
  if (!props.orderId) return false
  return props.orderStatus === 'ACTIVE' || props.orderStatus === 'COMPLETED'
})

// 弹窗状态与表单
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<InspectionOrderItemSaveReqVO>({
  id: undefined,
  inspectionOrderId: 0,
  rollNo: '',
  pieceNo: '',
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
  weavingTicketId: undefined,
  photos: undefined,
  remark: ''
})

const dialogTitle = computed(() => {
  const modeText = isPieceMode.value ? '（匹）' : '（卷）'
  return formData.id ? `编辑录检明细${modeText}` : `添加录检明细${modeText}`
})

const isVatNoRequired = computed(() => {
  // FINISHED 或 ROLL 模式必填；GREIGE 且 PIECE 模式可空
  if (props.orderType === 'FINISHED') return true
  if (props.packageMode !== 'PIECE') return true
  return false
})

const rules = computed<FormRules>(() => {
  const vatNoRule = isVatNoRequired.value
    ? [{ required: true, message: '缸号不能为空', trigger: 'blur' }]
    : []
  if (isPieceMode.value) {
    return {
      vatNo: vatNoRule,
      pieceNo: [{ required: true, message: '匹号不能为空', trigger: 'blur' }]
    }
  }
  return {
    vatNo: [{ required: true, message: '缸号不能为空', trigger: 'blur' }],
    rollNo: [{ required: true, message: '卷号不能为空', trigger: 'blur' }]
  }
})

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
      pieceNo: row.pieceNo || '',
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
      pieceCount: isPieceMode.value ? 1 : (row.pieceCount ?? 1),
      weavingTicketId: row.weavingTicketId,
      photos: row.photos ? [...row.photos] : undefined,
      remark: row.remark || ''
    })
  } else {
    Object.assign(formData, {
      id: undefined,
      inspectionOrderId: props.orderId || 0,
      rollNo: '',
      pieceNo: '',
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
      weavingTicketId: undefined,
      photos: undefined,
      remark: ''
    })
  }
  dialogVisible.value = true
}

/** 匹号失焦自动查布票 */
const handlePieceNoBlur = async () => {
  if (!isPieceMode.value || !formData.pieceNo?.trim()) return
  try {
    const ticket = await getWeavingTicketByCode(formData.pieceNo.trim())
    if (ticket) {
      formData.weavingTicketId = ticket.id
      if (ticket.actualMachine && !formData.machineNo) {
        formData.machineNo = ticket.actualMachine
      }
      message.success(`已自动识别布票【${ticket.code}】`)
    }
  } catch {}
}

/** 提交表单 */
const handleSubmit = async () => {
  if (!formRef.value || !props.orderId) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload: InspectionOrderItemSaveReqVO = {
      ...formData,
      vatNo: formData.vatNo?.trim() || undefined,
      inspectionOrderId: props.orderId,
      rollNo: formData.rollNo?.trim() || (isPieceMode.value ? undefined : ''),
      pieceNo: isPieceMode.value ? formData.pieceNo?.trim() : undefined,
      pieceCount: isPieceMode.value ? 1 : (formData.pieceCount ?? 1)
    }
    if (payload.id) {
      await InspectionOrderItemApi.updateInspectionOrderItem(payload)
      message.success('修改成功')
    } else {
      await InspectionOrderItemApi.createInspectionOrderItem(payload)
      message.success('添加成功')
    }
    dialogVisible.value = false
    emit('item-changed')
    await getList()
  } finally {
    submitting.value = false
  }
}

/** 删除单卷/匹明细 */
const handleDelete = async (id?: number) => {
  if (!id) return
  try {
    await message.delConfirm(
      isPieceMode.value ? '是否确认删除该匹明细？' : '是否确认删除该卷明细？'
    )
    await InspectionOrderItemApi.deleteInspectionOrderItem(id)
    message.success('删除成功')
    emit('item-changed')
    await getList()
  } catch {}
}

/** 从当前检验单生成质检确认 */
const handleGenerateConfirmation = async () => {
  if (!props.orderId) return
  generatingConfirmation.value = true
  try {
    const res = await InspectionConfirmationApi.generateFromOrder({
      inspectionOrderId: props.orderId
    })
    ElMessageBox.confirm(
      `生成成功！新增 ${res.generatedCount} 卷，跳过 ${res.skippedCount} 卷。是否前往质检确认列表查看？`,
      '质检确认生成结果',
      {
        confirmButtonText: '前往质检确认列表',
        cancelButtonText: '留在此页',
        type: 'success'
      }
    )
      .then(() => {
        push({
          path: '/production/inspection-confirmation',
          query: { inspectionOrderId: String(props.orderId) }
        })
      })
      .catch(() => {})
  } finally {
    generatingConfirmation.value = false
  }
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
