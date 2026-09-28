<template>
  <div class="vat-card is-clickable" :class="{ 'is-delayed': vat.delayed }" @click="handleCardClick">
    <div class="card-header">
      <div class="header-left">
        <span class="vat-no" :title="vat.vatNo">{{ vat.vatNo }}</span>
        <el-tag v-if="vat.delayed" type="danger" size="small" effect="dark" class="delay-tag">
          延期
        </el-tag>
      </div>
      <div class="header-right">
        <el-badge
          v-if="vat.unresolvedIssueCount > 0"
          :value="vat.unresolvedIssueCount"
          class="issue-badge"
          type="danger"
          title="未决问题数"
        />
        <DictTag
          :type="DICT_TYPE.LUNAERP_PROGRESS_FEEDBACK_STATUS"
          :value="vat.status"
          size="small"
        />
      </div>
    </div>

    <div class="card-body">
      <div class="quantity-row">
        <span class="qty-num">{{ vat.quantity ?? 0 }}</span>
        <span class="qty-unit">{{ unit || 'KG' }}</span>
        <span class="pieces-info">/ {{ vat.pieces != null ? vat.pieces + ' 匹' : '- 匹' }}</span>
      </div>

      <!-- 五工序步进进度指示 -->
      <div class="step-indicator">
        <div
          v-for="(step, idx) in steps"
          :key="step.key"
          class="step-node"
          :class="{
            'is-passed': currentStepIndex > idx,
            'is-current': currentStepIndex === idx,
            'is-pending': currentStepIndex < idx
          }"
          :title="step.label + (stepDate(step.key) ? ' (' + stepDate(step.key) + ')' : '')"
        >
          <div class="node-dot"></div>
          <span class="node-label">{{ step.label }}</span>
        </div>
      </div>

      <!-- 完成态五个阶段日期齐全，非完成态展示排缸与完成日 -->
      <div v-if="vat.status === 'COMPLETED'" class="completed-dates-box">
        <span v-if="vat.scheduledDate" class="date-chip">排: {{ vat.scheduledDate }}</span>
        <span v-if="vat.dyeingStartedDate" class="date-chip">染: {{ vat.dyeingStartedDate }}</span>
        <span v-if="vat.fixingStartedDate" class="date-chip">固: {{ vat.fixingStartedDate }}</span>
        <span v-if="vat.qualityCheckDate" class="date-chip">检: {{ vat.qualityCheckDate }}</span>
        <span v-if="vat.completedDate" class="date-chip">完: {{ vat.completedDate }}</span>
      </div>
      <div v-else class="dates-row">
        <span v-if="vat.scheduledDate" class="date-item">排: {{ vat.scheduledDate }}</span>
        <span v-if="vat.completedDate" class="date-item">完: {{ vat.completedDate }}</span>
      </div>

      <div v-if="vat.delayReason" class="delay-reason" :title="'延期原因: ' + vat.delayReason">
        <el-icon class="reason-icon"><Warning /></el-icon>
        <span class="reason-text">{{ vat.delayReason }}</span>
      </div>
    </div>

    <!-- 卡片底部操作栏 -->
    <div class="card-footer" @click.stop>
      <!-- COMPLETED 无后继、推进入口消失 -->
      <el-button
        v-if="nextStep && canAdvance"
        type="primary"
        size="small"
        plain
        :icon="Right"
        :loading="advancing"
        class="advance-btn"
        @click="handleAdvance"
      >
        推进至 {{ nextStep.label }}
      </el-button>
      <el-button
        size="small"
        text
        :icon="View"
        class="detail-btn"
        @click="handleCardClick"
      >
        详情
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { DICT_TYPE } from '@/utils/dict'
import { Warning, Right, View } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { hasPermission } from '@/directives/permission/hasPermi'
import { advanceProgressFeedback, type ProgressFeedbackVatVO } from '@/api/lunaerp-elmui/production/progress-feedback'

const props = defineProps<{
  vat: ProgressFeedbackVatVO
  unit?: string
}>()

const emit = defineEmits<{
  (e: 'open-detail', vat: ProgressFeedbackVatVO): void
  (e: 'refresh'): void
}>()

const advancing = ref(false)

const steps = [
  { key: 'SCHEDULED', label: '排缸' },
  { key: 'DYEING', label: '染色' },
  { key: 'FIXING', label: '固色' },
  { key: 'QUALITY_CHECK', label: '质检' },
  { key: 'COMPLETED', label: '完成' }
]

const currentStepIndex = computed(() => {
  const index = steps.findIndex((s) => s.key === props.vat.status)
  return index >= 0 ? index : 0
})

const nextStep = computed(() => {
  if (currentStepIndex.value < steps.length - 1) {
    return steps[currentStepIndex.value + 1]
  }
  return null
})

const canAdvance = computed(() => {
  return hasPermission(['lunaerp:production:progress-feedback:advance'])
})

const stepDateMap = computed<Record<string, string | undefined>>(() => ({
  SCHEDULED: props.vat.scheduledDate,
  DYEING: props.vat.dyeingStartedDate,
  FIXING: props.vat.fixingStartedDate,
  QUALITY_CHECK: props.vat.qualityCheckDate,
  COMPLETED: props.vat.completedDate
}))

const stepDate = (key: string) => stepDateMap.value[key] || ''

const handleCardClick = () => {
  emit('open-detail', props.vat)
}

const handleAdvance = async () => {
  if (!nextStep.value) return
  try {
    await ElMessageBox.confirm(
      `确定将缸【${props.vat.vatNo}】推进至【${nextStep.value.label}】吗？推进后将自动记录当前日期为该工序阶段时间戳。`,
      '工序推进确认',
      {
        confirmButtonText: '确定推进',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    advancing.value = true
    await advanceProgressFeedback({
      id: props.vat.id,
      targetStatus: nextStep.value.key
    })
    ElMessage.success(`缸【${props.vat.vatNo}】已推进至【${nextStep.value.label}】`)
    emit('refresh')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('推进失败', error)
    }
  } finally {
    advancing.value = false
  }
}
</script>

<style scoped lang="scss">
.vat-card {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 12px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  &.is-clickable {
    cursor: pointer;
  }

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-1px);
    border-color: #409eff;
  }

  &.is-delayed {
    border-color: #f89898;
    background: #fef0f0;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 6px;
    overflow: hidden;

    .vat-no {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .delay-tag {
      font-size: 11px;
      padding: 0 4px;
      height: 18px;
      line-height: 16px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;

    .issue-badge {
      :deep(.el-badge__content) {
        height: 16px;
        line-height: 16px;
        padding: 0 4px;
      }
    }
  }
}

.card-body {
  .quantity-row {
    margin-bottom: 8px;
    display: flex;
    align-items: baseline;
    gap: 4px;

    .qty-num {
      font-size: 18px;
      font-weight: 700;
      color: #1f2d3d;
    }

    .qty-unit {
      font-size: 12px;
      font-weight: 500;
      color: #606266;
    }

    .pieces-info {
      font-size: 12px;
      color: #909399;
      margin-left: 2px;
    }
  }

  .step-indicator {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 10px 0 8px 0;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: 5px;
      left: 6px;
      right: 6px;
      height: 2px;
      background: #e4e7ed;
      z-index: 1;
    }

    .step-node {
      position: relative;
      z-index: 2;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 3px;

      .node-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        background: #dcdfe6;
        border: 2px solid #ffffff;
        transition: all 0.2s;
      }

      .node-label {
        font-size: 10px;
        color: #909399;
      }

      &.is-passed {
        .node-dot {
          background: #67c23a;
        }
        .node-label {
          color: #67c23a;
        }
      }

      &.is-current {
        .node-dot {
          background: #409eff;
          box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
        }
        .node-label {
          color: #409eff;
          font-weight: 600;
        }
      }
    }
  }

  .dates-row {
    display: flex;
    justify-content: space-between;
    font-size: 11px;
    color: #909399;
    margin-top: 4px;
  }

  .completed-dates-box {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-top: 5px;

    .date-chip {
      font-size: 10px;
      color: #67c23a;
      background: #f0f9eb;
      padding: 1px 4px;
      border-radius: 3px;
      border: 1px solid #e1f3d8;
    }
  }

  .delay-reason {
    margin-top: 6px;
    padding: 3px 6px;
    background: #fdf6ec;
    border-radius: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    color: #e6a23c;
    overflow: hidden;

    .reason-icon {
      font-size: 12px;
      flex-shrink: 0;
    }

    .reason-text {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.card-footer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;

  .advance-btn {
    flex: 1;
    font-size: 12px;
    height: 26px;
    padding: 0 8px;
  }

  .detail-btn {
    font-size: 12px;
    color: #909399;
    height: 26px;
    padding: 0 4px;

    &:hover {
      color: #409eff;
    }
  }
}
</style>
