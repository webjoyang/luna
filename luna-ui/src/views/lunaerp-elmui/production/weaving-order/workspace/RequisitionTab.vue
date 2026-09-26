<template>
  <div class="requisition-tab">
    <!-- 顶部织造单用料概况条 -->
    <el-card shadow="never" class="mb-16px !border-gray-200">
      <div class="flex justify-between items-center flex-wrap gap-4">
        <div class="flex items-center gap-6">
          <div>
            <span class="text-gray-500 text-13px">品种名称：</span>
            <span class="font-bold text-14px">{{ weavingOrder?.productName || '-' }}</span>
            <span v-if="weavingOrder?.productCode" class="text-gray-400 text-12px ml-1">({{ weavingOrder.productCode }})</span>
          </div>
          <div>
            <span class="text-gray-500 text-13px">计划匹数：</span>
            <span class="font-bold text-14px">{{ weavingOrder?.plannedQty ?? '-' }} 匹</span>
          </div>
          <div>
            <span class="text-gray-500 text-13px">计划总重量：</span>
            <span class="font-bold text-14px">{{ weavingOrder?.plannedWeight != null ? weavingOrder.plannedWeight + ' KG' : '-' }}</span>
          </div>
          <div>
            <span class="text-gray-500 text-13px">单匹重量：</span>
            <span class="font-bold text-14px text-blue-600">
              {{ piecePlannedWeightText }}
            </span>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <!-- 测算按钮（后端权威计算，ADR-0027/0010） -->
          <el-button
            type="primary"
            plain
            :loading="calculateLoading"
            @click="handleTriggerCalculate"
            v-hasPermi="['lunaerp:production:weaving-order-requisition:create', 'lunaerp:production:weaving-order-requisition:update']"
          >
            <Icon icon="ep:cpu" class="mr-1" /> BOM 配比测算
          </el-button>

          <!-- 新建领纱单 -->
          <el-button
            type="primary"
            :loading="createLoading"
            @click="handleCreateRequisition"
            v-hasPermi="['lunaerp:production:weaving-order-requisition:create']"
          >
            <Icon icon="ep:plus" class="mr-1" /> 新建领纱单
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 领纱单选择与主卡片 -->
    <div v-loading="loading">
      <div v-if="requisitionList.length === 0" class="empty-wrap py-40px text-center">
        <el-empty description="当前织造单暂无领纱单">
          <el-button
            type="primary"
            @click="handleCreateRequisition"
            v-hasPermi="['lunaerp:production:weaving-order-requisition:create']"
          >
            立即创建领纱单
          </el-button>
        </el-empty>
      </div>

      <div v-else>
        <!-- 多个领纱单选项卡切换 -->
        <div class="requisition-selector flex items-center justify-between mb-16px pb-8px border-b border-gray-100">
          <el-radio-group v-model="selectedRequisitionId" @change="handleRequisitionChange">
            <el-radio-button
              v-for="req in requisitionList"
              :key="req.id"
              :value="req.id!"
            >
              {{ req.code }}
              <DictTag
                :type="DICT_TYPE.LUNAERP_YARN_REQUISITION_STATUS"
                :value="req.status || ''"
                class="ml-1"
              />
            </el-radio-button>
          </el-radio-group>

          <!-- 当前单操作按钮群 -->
          <div v-if="currentRequisition" class="flex items-center gap-2">
            <!-- 提交（DRAFT → ACTIVE） -->
            <el-button
              v-if="currentRequisition.status === 'DRAFT'"
              type="success"
              :loading="submitLoading"
              @click="handleSubmitRequisition"
              v-hasPermi="['lunaerp:production:weaving-order-requisition:submit']"
            >
              <Icon icon="ep:check" class="mr-1" /> 提交领纱单
            </el-button>

            <!-- 关闭领纱单 -->
            <el-button
              v-if="currentRequisition.status !== 'CLOSED'"
              type="warning"
              plain
              @click="handleCloseRequisition"
              v-hasPermi="['lunaerp:production:weaving-order-requisition:close']"
            >
              关闭领纱单
            </el-button>

            <!-- 删除领纱单（仅 DRAFT 可删） -->
            <el-button
              v-if="currentRequisition.status === 'DRAFT'"
              type="danger"
              plain
              @click="handleDeleteRequisition"
              v-hasPermi="['lunaerp:production:weaving-order-requisition:delete']"
            >
              删除领纱单
            </el-button>
          </div>
        </div>

        <!-- 领纱单信息摘要 -->
        <el-card v-if="currentRequisition" shadow="never" class="mb-16px !bg-gray-50/50">
          <el-descriptions :column="4" size="small" border>
            <el-descriptions-item label="领纱单号">
              <span class="font-mono font-bold">{{ currentRequisition.code }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="领纱状态">
              <DictTag
                :type="DICT_TYPE.LUNAERP_YARN_REQUISITION_STATUS"
                :value="currentRequisition.status || ''"
              />
            </el-descriptions-item>
            <el-descriptions-item label="总需求量">
              <span class="font-bold text-blue-600">{{ currentRequisition.totalRequiredQty ?? 0 }} KG</span>
            </el-descriptions-item>
            <el-descriptions-item label="总已发量">
              <span class="font-bold text-green-600">{{ currentRequisition.totalDispatchedQty ?? 0 }} KG</span>
            </el-descriptions-item>
            <el-descriptions-item label="明细行数">
              {{ currentRequisition.itemCount ?? currentItems.length }} 行
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ currentRequisition.createTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">
              <span class="text-gray-600">{{ currentRequisition.remark || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 领纱明细表（独立子资源） -->
        <el-card shadow="never">
          <template #header>
            <div class="flex justify-between items-center">
              <div class="flex items-center gap-2">
                <span class="font-bold text-15px">领纱明细行</span>
                <span class="text-gray-400 text-12px">（独立子资源维护，逐行测算与发纱）</span>
              </div>
              <div v-if="isCurrentDraft" class="flex items-center gap-2">
                <el-button
                  type="primary"
                  size="small"
                  plain
                  @click="handleOpenCalculateDialog"
                  v-hasPermi="['lunaerp:production:weaving-order-requisition:create']"
                >
                  <Icon icon="ep:cpu" class="mr-1" /> BOM 测算填充
                </el-button>
                <el-button
                  type="primary"
                  size="small"
                  @click="handleOpenItemDialog()"
                  v-hasPermi="['lunaerp:production:weaving-order-requisition-item:create']"
                >
                  <Icon icon="ep:plus" class="mr-1" /> 添加用纱明细
                </el-button>
              </div>
            </div>
          </template>

          <el-table
            :data="currentItems"
            v-loading="itemsLoading"
            border
            stripe
            style="width: 100%"
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="yarnTypeName" label="纱线材质" min-width="120" />
            <el-table-column prop="yarnSpecName" label="纱线规格" min-width="160">
              <template #default="{ row }">
                <span class="font-bold text-gray-800">{{ row.yarnSpecName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="requiredQty" label="需求数量 (KG)" min-width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold text-blue-600">{{ row.requiredQty }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="dispatchedQty" label="已发数量 (KG)" min-width="130" align="right">
              <template #default="{ row }">
                <span class="font-bold" :class="row.dispatchedQty > 0 ? 'text-green-600' : 'text-gray-400'">
                  {{ row.dispatchedQty ?? 0 }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="80" align="center">
              <template #default>KG</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <template v-if="isCurrentDraft">
                  <el-button
                    type="primary"
                    link
                    @click="handleOpenItemDialog(row)"
                    v-hasPermi="['lunaerp:production:weaving-order-requisition-item:update']"
                  >
                    编辑
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    @click="handleDeleteItem(row)"
                    v-hasPermi="['lunaerp:production:weaving-order-requisition-item:delete']"
                  >
                    删除
                  </el-button>
                </template>
                <template v-else>
                  <el-tag type="info" size="small">已锁定</el-tag>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>

    <!-- BOM 测算弹窗 -->
    <el-dialog
      v-model="calcDialogVisible"
      title="纱线配比 BOM 测算"
      width="750px"
      append-to-body
      destroy-on-close
    >
      <div v-loading="calcDialogLoading">
        <el-alert
          v-if="calcResult?.tip"
          :title="calcResult.tip"
          type="warning"
          show-icon
          :closable="false"
          class="mb-16px"
        />

        <div v-if="calcResult" class="mb-16px text-13px bg-blue-50/50 p-12px rounded text-gray-700">
          <div class="grid grid-cols-3 gap-2">
            <div>织造单计划匹数：<span class="font-bold">{{ calcResult.plannedQty ?? '-' }} 匹</span></div>
            <div>织造单计划总重：<span class="font-bold">{{ calcResult.plannedWeight ?? '-' }} KG</span></div>
            <div>单匹计划重量：<span class="font-bold text-blue-600">{{ calcResult.piecePlannedWeight ?? '-' }} KG/匹</span></div>
          </div>
          <div class="mt-2 text-12px text-gray-500">
            测算公式：某纱线规格需求 KG = 计划匹数 × 单匹计划重量 × (配比 ratio / 100)
          </div>
        </div>

        <el-table
          :data="calcResult?.items || []"
          border
          stripe
          size="small"
          class="w-full"
        >
          <el-table-column prop="yarnTypeName" label="纱线材质" min-width="100" />
          <el-table-column prop="yarnSpecName" label="纱线规格" min-width="140" />
          <el-table-column prop="ratio" label="配比 (%)" width="100" align="right">
            <template #default="{ row }">
              {{ row.ratio }} %
            </template>
          </el-table-column>
          <el-table-column prop="requiredQty" label="测算需求量 (KG)" width="150" align="right">
            <template #default="{ row }">
              <el-input-number
                v-model="row.requiredQty"
                :precision="2"
                :step="1"
                :min="0.01"
                size="small"
                controls-position="right"
                class="!w-120px"
              />
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="70" align="center">
            <template #default>KG</template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="100" show-overflow-tooltip />
        </el-table>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="calcDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!calcResult || calcResult.items.length === 0"
            :loading="calcApplyLoading"
            @click="handleApplyCalculateItems"
          >
            应用并写入明细
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 单条明细编辑/新增弹窗 -->
    <el-dialog
      v-model="itemDialogVisible"
      :title="itemForm.id ? '编辑用纱明细' : '添加用纱明细'"
      width="500px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="itemFormRef"
        :model="itemForm"
        :rules="itemRules"
        label-width="100px"
      >
        <el-form-item label="纱线规格" prop="yarnSpecId">
          <el-cascader
            v-model="selectedSpecCascade"
            :options="cascadeOptions"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            placeholder="请选择纱线材质与规格"
            filterable
            clearable
            class="w-full"
            @change="handleSpecCascadeChange"
          />
        </el-form-item>
        <el-form-item label="需求量 (KG)" prop="requiredQty">
          <el-input-number
            v-model="itemForm.requiredQty"
            :precision="2"
            :step="1"
            :min="0.01"
            controls-position="right"
            class="!w-full"
            placeholder="请输入需求重量 KG"
          />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="itemForm.unit" disabled />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="itemForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注说明（可空）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="itemDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="itemSaveLoading" @click="handleSaveItem">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import {
  YarnRequisitionVO,
  YarnRequisitionItemVO,
  YarnRequisitionCalculateRespVO,
  getYarnRequisitionPage,
  getYarnRequisition,
  createYarnRequisition,
  deleteYarnRequisition,
  submitYarnRequisition,
  closeYarnRequisition,
  calculateYarnRequisition,
  createYarnRequisitionItem,
  updateYarnRequisitionItem,
  deleteYarnRequisitionItem
} from '@/api/lunaerp-elmui/production/weaving-order-requisition'
import {
  getYarnSpecCascadeOptions,
  YarnSpecCascadeOptionVO
} from '@/api/lunaerp-elmui/production/yarn-master'
import { WeavingOrderVO } from '@/api/lunaerp-elmui/production/weaving-order'

const props = defineProps<{
  weavingOrderId: number
  weavingOrder?: WeavingOrderVO
  isViewMode?: boolean
}>()

const loading = ref(false)
const itemsLoading = ref(false)
const createLoading = ref(false)
const submitLoading = ref(false)
const calculateLoading = ref(false)

// 领纱单列表与当前选中单
const requisitionList = ref<YarnRequisitionVO[]>([])
const selectedRequisitionId = ref<number>()
const currentRequisition = ref<YarnRequisitionVO>()
const currentItems = ref<YarnRequisitionItemVO[]>([])

// 单匹重量计算提示
const piecePlannedWeightText = computed(() => {
  if (!props.weavingOrder?.plannedQty || !props.weavingOrder?.plannedWeight) {
    return '-'
  }
  const per = (props.weavingOrder.plannedWeight / props.weavingOrder.plannedQty).toFixed(4)
  return `${per} KG/匹`
})

// 当前领纱单是否处于草稿态
const isCurrentDraft = computed(() => {
  return currentRequisition.value?.status === 'DRAFT'
})

// ==================== 1. 加载领纱单与明细 ====================

const loadRequisitions = async () => {
  if (!props.weavingOrderId) return
  loading.value = true
  try {
    const res = await getYarnRequisitionPage({
      pageNo: 1,
      pageSize: 50,
      weavingOrderId: props.weavingOrderId
    })
    requisitionList.value = res.list || []
    if (requisitionList.value.length > 0) {
      // 保持当前选中，或默认选中第一张
      if (!selectedRequisitionId.value || !requisitionList.value.some(r => r.id === selectedRequisitionId.value)) {
        selectedRequisitionId.value = requisitionList.value[0].id
      }
      await loadRequisitionDetail(selectedRequisitionId.value!)
    } else {
      selectedRequisitionId.value = undefined
      currentRequisition.value = undefined
      currentItems.value = []
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载领纱单失败')
  } finally {
    loading.value = false
  }
}

const loadRequisitionDetail = async (reqId: number) => {
  itemsLoading.value = true
  try {
    const res = await getYarnRequisition(reqId)
    currentRequisition.value = res
    currentItems.value = res.items || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载领纱单详情失败')
  } finally {
    itemsLoading.value = false
  }
}

const handleRequisitionChange = (reqId: any) => {
  selectedRequisitionId.value = reqId
  loadRequisitionDetail(reqId)
}

// ==================== 2. 新建/提交/关闭/删除领纱单 ====================

const handleCreateRequisition = async () => {
  createLoading.value = true
  try {
    const id = await createYarnRequisition({
      weavingOrderId: props.weavingOrderId
    })
    ElMessage.success('领纱单创建成功')
    selectedRequisitionId.value = id
    await loadRequisitions()
  } catch (e: any) {
    ElMessage.error(e?.message || '创建领纱单失败')
  } finally {
    createLoading.value = false
  }
}

const handleSubmitRequisition = async () => {
  if (!currentRequisition.value?.id) return
  if (currentItems.value.length === 0) {
    ElMessage.warning('领纱单尚未录入明细，无法提交')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认提交领纱单【${currentRequisition.value.code}】？提交后将进入待发纱状态，明细将锁定。`,
      '提交确认',
      { confirmButtonText: '确定提交', cancelButtonText: '取消', type: 'warning' }
    )
    submitLoading.value = true
    await submitYarnRequisition(currentRequisition.value.id)
    ElMessage.success('领纱单提交成功，已转为待发纱')
    await loadRequisitions()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '提交领纱单失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const handleCloseRequisition = async () => {
  if (!currentRequisition.value?.id) return
  try {
    await ElMessageBox.confirm(
      `确认关闭领纱单【${currentRequisition.value.code}】？关闭为终态操作。`,
      '关闭确认',
      { confirmButtonText: '确定关闭', cancelButtonText: '取消', type: 'warning' }
    )
    await closeYarnRequisition(currentRequisition.value.id)
    ElMessage.success('领纱单已关闭')
    await loadRequisitions()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '关闭领纱单失败')
    }
  }
}

const handleDeleteRequisition = async () => {
  if (!currentRequisition.value?.id) return
  try {
    await ElMessageBox.confirm(
      `确认删除草稿领纱单【${currentRequisition.value.code}】？下属明细将一并删除。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteYarnRequisition(currentRequisition.value.id)
    ElMessage.success('领纱单已删除')
    selectedRequisitionId.value = undefined
    await loadRequisitions()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除领纱单失败')
    }
  }
}

// ==================== 3. BOM 配比权威测算 ====================

const calcDialogVisible = ref(false)
const calcDialogLoading = ref(false)
const calcApplyLoading = ref(false)
const calcResult = ref<YarnRequisitionCalculateRespVO>()

const handleTriggerCalculate = async () => {
  calcDialogVisible.value = true
  calcDialogLoading.value = true
  try {
    const res = await calculateYarnRequisition({
      weavingOrderId: props.weavingOrderId
    })
    calcResult.value = res
    if (res.tip) {
      ElMessage.warning(res.tip)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '测算失败')
  } finally {
    calcDialogLoading.value = false
  }
}

const handleOpenCalculateDialog = () => {
  handleTriggerCalculate()
}

// 应用测算结果到当前领纱单
const handleApplyCalculateItems = async () => {
  if (!calcResult.value || !calcResult.value.items.length) {
    ElMessage.warning('无有效测算明细可应用')
    return
  }

  // 若尚未有领纱单，自动创建一张
  calcApplyLoading.value = true
  try {
    let reqId = currentRequisition.value?.id
    if (!reqId || !isCurrentDraft.value) {
      reqId = await createYarnRequisition({ weavingOrderId: props.weavingOrderId })
      selectedRequisitionId.value = reqId
      await loadRequisitions()
    }

    // 逐行调用独立子资源创建端点
    for (const item of calcResult.value.items) {
      if (!item.yarnSpecId || !item.requiredQty) continue
      await createYarnRequisitionItem({
        requisitionId: reqId,
        yarnTypeId: item.yarnTypeId,
        yarnSpecId: item.yarnSpecId,
        yarnSpecName: item.yarnSpecName,
        requiredQty: item.requiredQty,
        unit: 'KG',
        remark: item.remark || 'BOM 测算生成'
      })
    }

    ElMessage.success('测算明细已成功填充至领纱单')
    calcDialogVisible.value = false
    await loadRequisitionDetail(reqId)
  } catch (e: any) {
    ElMessage.error(e?.message || '应用测算明细失败')
  } finally {
    calcApplyLoading.value = false
  }
}

// ==================== 4. 领纱明细独立子资源编辑 ====================

const itemDialogVisible = ref(false)
const itemSaveLoading = ref(false)
const itemFormRef = ref<FormInstance>()
const cascadeOptions = ref<any[]>([])
const selectedSpecCascade = ref<number[]>([])

const itemForm = ref<{
  id?: number
  requisitionId?: number
  yarnTypeId?: number
  yarnSpecId?: number
  yarnSpecName?: string
  requiredQty?: number
  unit: string
  remark?: string
}>({
  unit: 'KG'
})

const itemRules = {
  yarnSpecId: [{ required: true, message: '请选择纱线规格', trigger: 'change' }],
  requiredQty: [{ required: true, message: '请输入需求数量且大于0', trigger: 'blur' }]
}

const loadCascadeOptions = async () => {
  try {
    const list = await getYarnSpecCascadeOptions()
    cascadeOptions.value = (list || []).map((t: YarnSpecCascadeOptionVO) => ({
      id: t.yarnTypeId,
      label: t.yarnTypeName,
      children: (t.specs || []).map(s => ({
        id: s.id,
        label: s.specName,
        specName: s.specName,
        yarnTypeId: t.yarnTypeId
      }))
    }))
  } catch (e) {
    console.error('加载纱线规格级联选项失败', e)
  }
}

const handleOpenItemDialog = (row?: YarnRequisitionItemVO) => {
  if (row) {
    itemForm.value = {
      id: row.id,
      requisitionId: row.requisitionId,
      yarnTypeId: row.yarnTypeId,
      yarnSpecId: row.yarnSpecId,
      yarnSpecName: row.yarnSpecName,
      requiredQty: row.requiredQty,
      unit: row.unit || 'KG',
      remark: row.remark
    }
    selectedSpecCascade.value = row.yarnTypeId && row.yarnSpecId ? [row.yarnTypeId, row.yarnSpecId] : []
  } else {
    itemForm.value = {
      requisitionId: currentRequisition.value?.id,
      unit: 'KG',
      requiredQty: undefined,
      remark: ''
    }
    selectedSpecCascade.value = []
  }
  itemDialogVisible.value = true
}

const handleSpecCascadeChange = (val: any) => {
  if (val && val.length === 2) {
    itemForm.value.yarnTypeId = val[0]
    itemForm.value.yarnSpecId = val[1]
    // 查找规格名称
    const typeOpt = cascadeOptions.value.find(t => t.id === val[0])
    const specOpt = typeOpt?.children?.find((s: any) => s.id === val[1])
    if (specOpt) {
      itemForm.value.yarnSpecName = specOpt.specName
    }
  } else {
    itemForm.value.yarnTypeId = undefined
    itemForm.value.yarnSpecId = undefined
    itemForm.value.yarnSpecName = undefined
  }
}

const handleSaveItem = async () => {
  if (!itemFormRef.value) return
  await itemFormRef.value.validate()

  if (!itemForm.value.requisitionId && currentRequisition.value?.id) {
    itemForm.value.requisitionId = currentRequisition.value.id
  }

  itemSaveLoading.value = true
  try {
    if (itemForm.value.id) {
      await updateYarnRequisitionItem({
        id: itemForm.value.id,
        requisitionId: itemForm.value.requisitionId,
        yarnTypeId: itemForm.value.yarnTypeId,
        yarnSpecId: itemForm.value.yarnSpecId!,
        yarnSpecName: itemForm.value.yarnSpecName,
        requiredQty: itemForm.value.requiredQty!,
        unit: itemForm.value.unit,
        remark: itemForm.value.remark
      })
      ElMessage.success('明细修改成功')
    } else {
      await createYarnRequisitionItem({
        requisitionId: itemForm.value.requisitionId!,
        yarnTypeId: itemForm.value.yarnTypeId,
        yarnSpecId: itemForm.value.yarnSpecId!,
        yarnSpecName: itemForm.value.yarnSpecName,
        requiredQty: itemForm.value.requiredQty!,
        unit: itemForm.value.unit,
        remark: itemForm.value.remark
      })
      ElMessage.success('明细新增成功')
    }
    itemDialogVisible.value = false
    if (currentRequisition.value?.id) {
      await loadRequisitionDetail(currentRequisition.value.id)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '保存明细失败')
  } finally {
    itemSaveLoading.value = false
  }
}

const handleDeleteItem = async (row: YarnRequisitionItemVO) => {
  if (!row.id) return
  try {
    await ElMessageBox.confirm(`确认删除该纱线明细【${row.yarnSpecName}】？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await deleteYarnRequisitionItem(row.id)
    ElMessage.success('删除成功')
    if (currentRequisition.value?.id) {
      await loadRequisitionDetail(currentRequisition.value.id)
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

watch(
  () => props.weavingOrderId,
  (newVal) => {
    if (newVal) {
      loadRequisitions()
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadCascadeOptions()
})
</script>

<style scoped>
.requisition-tab {
  padding: 8px 0;
}
</style>
