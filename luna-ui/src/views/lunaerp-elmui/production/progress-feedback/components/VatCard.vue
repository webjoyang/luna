<template>
  <div class="vat-card" :class="{ 'is-delayed': vat.delayed }">
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
          <div class="node-dot" />
          <span class="node-label">{{ step.label }}</span>
        </div>
      </div>

      <div class="dates-row">
        <span v-if="vat.scheduledDate" class="date-item">排: {{ vat.scheduledDate }}</span>
        <span v-if="vat.completedDate" class="date-item">完: {{ vat.completedDate }}</span>
      </div>

      <div v-if="vat.delayReason" class="delay-reason" :title="'延期原因: ' + vat.delayReason">
        <el-icon class="reason-icon"><Warning /></el-icon>
        <span class="reason-text">{{ vat.delayReason }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DICT_TYPE } from '@/utils/dict'
import { Warning } from '@element-plus/icons-vue'
import type { ProgressFeedbackVatVO } from '@/api/lunaerp-elmui/production/progress-feedback'

const props = defineProps<{
  vat: ProgressFeedbackVatVO
  unit?: string
}>()

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

const stepDateMap = computed<Record<string, string | undefined>>(() => ({
  SCHEDULED: props.vat.scheduledDate,
  DYEING: props.vat.dyeingStartedDate,
  FIXING: props.vat.fixingStartedDate,
  QUALITY_CHECK: props.vat.qualityCheckDate,
  COMPLETED: props.vat.completedDate
}))

const stepDate = (key: string) => stepDateMap.value[key] || ''
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

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-1px);
    border-color: #c0c4cc;
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
</style>
