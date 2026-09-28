<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="680px"
    direction="rtl"
    destroy-on-close
    class="vat-detail-drawer"
  >
    <div v-loading="loading" class="drawer-content">
      <template v-if="detail">
        <!-- 1. 顶部工序流转状态与操作条 -->
        <div class="status-action-bar">
          <div class="status-left">
            <span class="status-label">当前工序:</span>
            <DictTag
              :type="DICT_TYPE.LUNAERP_PROGRESS_FEEDBACK_STATUS"
              :value="detail.status"
              size="default"
            />
            <el-tag v-if="detail.delayed" type="danger" effect="dark" size="small">
              已延期
            </el-tag>
            <el-badge
              v-if="detail.unresolvedIssueCount > 0"
              :value="detail.unresolvedIssueCount"
              type="danger"
              class="issue-badge"
              title="未决问题数"
            />
          </div>

          <div class="status-right">
            <!-- 推进按钮（COMPLETED 无后继、推进入口消失） -->
            <el-button
              v-if="nextStep"
              v-hasPermi="['lunaerp:production:progress-feedback:advance']"
              type="primary"
              :icon="Right"
              :loading="advancing"
              @click="handleAdvance"
            >
              推进至 {{ nextStep.label }}
            </el-button>

            <!-- 回退按钮（SCHEDULED 无前序不显示回退；二次确认在前端） -->
            <el-button
              v-if="prevStep"
              v-hasPermi="['lunaerp:production:progress-feedback:update']"
              type="warning"
              plain
              :icon="Back"
              :loading="rollingBack"
              @click="handleRollback"
            >
              回退至 {{ prevStep.label }}
            </el-button>
          </div>
        </div>

        <!-- 2. 六工序时间轴 -->
        <div class="timeline-section">
          <div class="section-title">六工序时间轴</div>
          <el-steps :active="activeStepIndex" finish-status="success" align-center class="steps-flow">
            <el-step
              v-for="(st, idx) in sixSteps"
              :key="st.key"
              :title="st.label"
              :status="getStepStatus(idx)"
            >
              <template #description>
                <div class="step-desc">
                  <span v-if="st.key === 'NOT_SCHEDULED'" class="step-past">
                    -
                  </span>
                  <span v-else-if="getStepDate(st.key)" class="step-date">
                    {{ getStepDate(st.key) }}
                  </span>
                  <span v-else-if="idx <= activeStepIndex" class="step-empty">
                    -
                  </span>
                </div>
              </template>
            </el-step>
          </el-steps>
        </div>

        <el-divider class="content-divider" />

        <!-- 3. 染色单与颜色行实时信息（零快照） -->
        <div class="context-section">
          <div class="section-title">订单与颜色明细（实时关联）</div>
          <el-descriptions :column="2" border size="small" class="detail-descriptions">
            <el-descriptions-item label="销售合同">
              {{ detail.contractNo || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="客户名称">
              {{ detail.clientName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="承染厂">
              {{ detail.dyeingFactoryName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="染色单交期">
              <span :class="{ 'is-delayed-text': detail.delayed }">
                {{ detail.deliveryDate || '-' }}
                <span v-if="detail.delayed">(已延期)</span>
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="产品信息">
              {{ detail.productName || detail.productCode || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="颜色明细">
              {{ detail.colorName || '-' }}
              <span v-if="detail.customerColorNo" class="sub-text">({{ detail.customerColorNo }})</span>
            </el-descriptions-item>
            <el-descriptions-item label="投染需求">
              {{ detail.colorQuantity ?? 0 }} {{ detail.unit || 'KG' }}
              <span v-if="detail.colorPieces != null" class="sub-text">/ {{ detail.colorPieces }} 匹</span>
            </el-descriptions-item>
            <el-descriptions-item label="排产覆盖率">
              <span :class="{ 'is-over-text': detail.overScheduled }">
                {{ detail.coveragePercent }}%
                <span v-if="detail.overScheduled">(超排)</span>
              </span>
              <span class="sub-text">(已建: {{ detail.totalVatQuantity }} {{ detail.unit || 'KG' }})</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <el-divider class="content-divider" />

        <!-- 4. 缸信息与阶段日期编辑表单 -->
        <div class="edit-section">
          <div class="section-title-row">
            <span class="section-title">缸信息与阶段日期</span>
            <span class="tip-text">内部跟单记录非过账凭证，已完成缸仍可修正日期与备注</span>
          </div>

          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-width="95px"
            class="vat-edit-form"
          >
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="缸号" prop="vatNo">
                  <el-input
                    v-model="formData.vatNo"
                    placeholder="请输入缸号"
                    maxlength="50"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="本缸数量" prop="quantity">
                  <el-input-number
                    v-model="formData.quantity"
                    :min="0.01"
                    :precision="2"
                    :step="10"
                    placeholder="数量"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="匹数" prop="pieces">
                  <el-input-number
                    v-model="formData.pieces"
                    :min="0"
                    :precision="0"
                    placeholder="匹数（可选）"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="延期原因" prop="delayReason">
                  <el-input
                    v-model="formData.delayReason"
                    placeholder="染厂反馈的延期原因"
                    maxlength="500"
                    clearable
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <div class="dates-header">
              <span class="dates-title">五阶段工序日期人工修正</span>
            </div>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="排缸日" prop="scheduledDate">
                  <el-date-picker
                    v-model="formData.scheduledDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="阶段一：排缸日"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="染色开始" prop="dyeingStartedDate">
                  <el-date-picker
                    v-model="formData.dyeingStartedDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="阶段二：染色开始日"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="固色开始" prop="fixingStartedDate">
                  <el-date-picker
                    v-model="formData.fixingStartedDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="阶段三：固色开始日"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="质检日期" prop="qualityCheckDate">
                  <el-date-picker
                    v-model="formData.qualityCheckDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="阶段四：质检日期"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="完成日期" prop="completedDate">
                  <el-date-picker
                    v-model="formData.completedDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="阶段五：出厂完成日"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="跟单备注" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="2"
                placeholder="录入跟单补充说明或变更说明"
                maxlength="500"
              />
            </el-form-item>
          </el-form>
        </div>
      </template>
    </div>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="visible = false">关闭</el-button>
        <el-button
          v-hasPermi="['lunaerp:production:progress-feedback:update']"
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存缸信息修改
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { Right, Back } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import {
  getProgressFeedback,
  updateProgressFeedback,
  advanceProgressFeedback,
  type ProgressFeedbackDetailVO,
  type ProgressFeedbackUpdateVO
} from '@/api/lunaerp-elmui/production/progress-feedback'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const advancing = ref(false)
const rollingBack = ref(false)
const detail = ref<ProgressFeedbackDetailVO>()
const formRef = ref<FormInstance>()

// 六工序定义（含虚拟未排缸）
const sixSteps = [
  { key: 'NOT_SCHEDULED', label: '未排缸' },
  { key: 'SCHEDULED', label: '已排缸' },
  { key: 'DYEING', label: '染色中' },
  { key: 'FIXING', label: '固色中' },
  { key: 'QUALITY_CHECK', label: '质检中' },
  { key: 'COMPLETED', label: '已完成' }
]

// 五个实体工序（用于状态机流转）
const entitySteps = [
  { key: 'SCHEDULED', label: '已排缸' },
  { key: 'DYEING', label: '染色中' },
  { key: 'FIXING', label: '固色中' },
  { key: 'QUALITY_CHECK', label: '质检中' },
  { key: 'COMPLETED', label: '已完成' }
]

const drawerTitle = computed(() => {
  return detail.value ? `缸详情与工序时间轴 - ${detail.value.vatNo}` : '缸详情'
})

const activeStepIndex = computed(() => {
  if (!detail.value) return 0
  const index = sixSteps.findIndex((s) => s.key === detail.value?.status)
  return index >= 0 ? index : 1
})

const currentEntityIndex = computed(() => {
  if (!detail.value) return 0
  const idx = entitySteps.findIndex((s) => s.key === detail.value?.status)
  return idx >= 0 ? idx : 0
})

const nextStep = computed(() => {
  if (currentEntityIndex.value < entitySteps.length - 1) {
    return entitySteps[currentEntityIndex.value + 1]
  }
  return null
})

const prevStep = computed(() => {
  if (currentEntityIndex.value > 0) {
    return entitySteps[currentEntityIndex.value - 1]
  }
  return null
})

const getStepStatus = (idx: number) => {
  if (idx < activeStepIndex.value) return 'finish'
  if (idx === activeStepIndex.value) {
    return detail.value?.status === 'COMPLETED' ? 'success' : 'process'
  }
  return 'wait'
}

const getStepDate = (key: string): string => {
  if (!detail.value) return ''
  switch (key) {
    case 'SCHEDULED':
      return detail.value.scheduledDate || ''
    case 'DYEING':
      return detail.value.dyeingStartedDate || ''
    case 'FIXING':
      return detail.value.fixingStartedDate || ''
    case 'QUALITY_CHECK':
      return detail.value.qualityCheckDate || ''
    case 'COMPLETED':
      return detail.value.completedDate || ''
    default:
      return ''
  }
}

// 表单响应式数据
const formData = reactive<ProgressFeedbackUpdateVO>({
  id: 0,
  vatNo: '',
  quantity: 0,
  pieces: undefined,
  status: undefined,
  scheduledDate: undefined,
  dyeingStartedDate: undefined,
  fixingStartedDate: undefined,
  qualityCheckDate: undefined,
  completedDate: undefined,
  delayReason: '',
  remark: ''
})

const formRules: FormRules = {
  vatNo: [{ required: true, message: '缸号不能为空', trigger: 'blur' }],
  quantity: [
    { required: true, message: '本缸数量不能为空', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value == null || value <= 0) {
          callback(new Error('本缸数量必须大于0'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 打开抽屉
const open = async (vatId: number) => {
  visible.value = true
  await loadDetail(vatId)
}

// 加载详情数据
const loadDetail = async (vatId: number) => {
  loading.value = true
  try {
    const res = await getProgressFeedback(vatId)
    detail.value = res
    // 同步到表单
    formData.id = res.id
    formData.vatNo = res.vatNo
    formData.quantity = res.quantity
    formData.pieces = res.pieces
    formData.status = res.status
    formData.scheduledDate = res.scheduledDate
    formData.dyeingStartedDate = res.dyeingStartedDate
    formData.fixingStartedDate = res.fixingStartedDate
    formData.qualityCheckDate = res.qualityCheckDate
    formData.completedDate = res.completedDate
    formData.delayReason = res.delayReason || ''
    formData.remark = res.remark || ''
  } catch (error) {
    console.error('加载缸详情失败', error)
  } finally {
    loading.value = false
  }
}

// 推进工序
const handleAdvance = async () => {
  if (!detail.value || !nextStep.value) return
  try {
    await ElMessageBox.confirm(
      `确认要将缸【${detail.value.vatNo}】推进至下一工序【${nextStep.value.label}】吗？推进后将自动记录当前日期为该工序阶段时间戳。`,
      '工序推进确认',
      {
        confirmButtonText: '确定推进',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    advancing.value = true
    await advanceProgressFeedback({
      id: detail.value.id,
      targetStatus: nextStep.value.key
    })
    ElMessage.success(`已推进至【${nextStep.value.label}】`)
    await loadDetail(detail.value.id)
    emit('success')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('推进失败', error)
    }
  } finally {
    advancing.value = false
  }
}

// 回退工序（二次确认）
const handleRollback = async () => {
  if (!detail.value || !prevStep.value) return
  if (formRef.value) {
    const valid = await formRef.value.validate()
    if (!valid) return
  }
  try {
    await ElMessageBox.confirm(
      `确认要将该缸状态回退到上一工序【${prevStep.value.label}】吗？回退后阶段日期不会自动清除，日期以人工修正为准。`,
      '状态回退确认',
      {
        confirmButtonText: '确认回退',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    rollingBack.value = true
    await updateProgressFeedback({
      ...formData,
      status: prevStep.value.key
    })
    ElMessage.success(`已回退到【${prevStep.value.label}】`)
    await loadDetail(detail.value.id)
    emit('success')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('回退失败', error)
    }
  } finally {
    rollingBack.value = false
  }
}

// 保存修改
const handleSave = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  saving.value = true
  try {
    await updateProgressFeedback({
      ...formData,
      status: detail.value?.status // 保持当前状态
    })
    ElMessage.success('缸信息保存成功')
    await loadDetail(formData.id)
    emit('success')
  } catch (error) {
    console.error('保存缸信息失败', error)
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.drawer-content {
  padding: 10px 4px;
}

.status-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  margin-bottom: 20px;

  .status-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-label {
      font-size: 13px;
      color: #606266;
      font-weight: 500;
    }

    .issue-badge {
      :deep(.el-badge__content) {
        height: 18px;
        line-height: 18px;
        padding: 0 5px;
      }
    }
  }

  .status-right {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;

  .tip-text {
    font-size: 12px;
    color: #909399;
  }
}

.timeline-section {
  margin-bottom: 10px;

  .steps-flow {
    padding: 12px 0;

    .step-desc {
      margin-top: 6px;
      font-size: 11px;

      .step-date {
        color: #409eff;
        font-weight: 500;
      }

      .step-empty {
        color: #909399;
      }

      .step-pending {
        color: #c0c4cc;
      }
    }
  }
}

.content-divider {
  margin: 18px 0;
}

.context-section {
  .detail-descriptions {
    :deep(.el-descriptions__label) {
      width: 100px;
      background: #fafbfc;
      color: #606266;
      font-weight: 500;
    }

    .sub-text {
      color: #909399;
      font-size: 12px;
      margin-left: 4px;
    }

    .is-delayed-text {
      color: #f56c6c;
      font-weight: 600;
    }

    .is-over-text {
      color: #f56c6c;
      font-weight: 600;
    }
  }
}

.edit-section {
  .dates-header {
    margin: 16px 0 12px 0;
    padding-left: 8px;
    border-left: 3px solid #409eff;

    .dates-title {
      font-size: 13px;
      font-weight: 600;
      color: #303133;
    }
  }
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
