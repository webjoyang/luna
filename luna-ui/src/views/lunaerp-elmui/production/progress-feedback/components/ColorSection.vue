<template>
  <div class="color-section-card">
    <!-- 节头：染色单与颜色行信息（零快照） + 覆盖率汇总 -->
    <div class="section-header">
      <div class="header-main">
        <div class="title-row">
          <span class="contract-tag">合同: {{ section.contractNo || '未绑定合同' }}</span>
          <span class="client-name">{{ section.clientName || '无客户' }}</span>
          <span class="divider">|</span>
          <span class="factory-name">染厂: {{ section.dyeingFactoryName || '-' }}</span>
          <span class="divider">|</span>
          <span class="product-info">{{ section.productName || section.productCode || '-' }}</span>
          <span
            v-if="section.deliveryDate"
            class="delivery-date"
            :class="{ 'is-past': isPastDelivery }"
          >
            交期: {{ section.deliveryDate }}
          </span>
        </div>

        <div class="color-row">
          <span class="line-badge">#{{ section.lineNo ?? 1 }}</span>
          <span class="color-name">{{ section.colorName || '未命名' }}</span>
          <span v-if="section.customerColorNo" class="customer-color-no">
            ({{ section.customerColorNo }})
          </span>
          <span class="order-quantity">
            投染需求:
            <strong>{{ section.colorQuantity ?? 0 }}</strong>
            {{ section.unit || 'KG' }}
            <span v-if="section.colorPieces != null" class="pieces-text">
              ({{ section.colorPieces }} 匹)
            </span>
          </span>
        </div>
      </div>

      <div class="header-summary">
        <div class="summary-item">
          <span class="summary-label">已建缸合计:</span>
          <span class="summary-value"
            >{{ section.totalVatQuantity }} {{ section.unit || 'KG' }}</span
          >
          <span
            v-if="section.totalVatPieces != null && section.totalVatPieces > 0"
            class="pieces-summary"
          >
            ({{ section.totalVatPieces }} 匹)
          </span>
        </div>

        <div class="coverage-box">
          <span class="coverage-label">排产覆盖率:</span>
          <el-tooltip
            v-if="section.overScheduled"
            content="超排预警：已建缸数量超过颜色行需求数量"
            placement="top"
          >
            <el-tag type="danger" effect="dark" class="coverage-tag">
              {{ section.coveragePercent }}% 超排
            </el-tag>
          </el-tooltip>
          <el-tag v-else :type="coverageTagType" class="coverage-tag">
            {{ section.coveragePercent }}%
          </el-tag>
        </div>

        <el-button
          v-hasPermi="['lunaerp:production:progress-feedback:create']"
          type="primary"
          size="small"
          plain
          :icon="Plus"
          class="create-vat-btn"
          @click="handleCreateVat"
        >
          建缸
        </el-button>
      </div>
    </div>

    <!-- 节体：缸卡网格或虚线未排缸占位卡 -->
    <div class="section-body">
      <div v-if="section.vats && section.vats.length > 0" class="vat-grid">
        <VatCard
          v-for="vat in section.vats"
          :key="vat.id"
          :vat="vat"
          :unit="section.unit"
          @open-detail="handleOpenDetail"
          @refresh="handleRefresh"
          @open-feedback="handleOpenFeedback"
        />
      </div>

      <div v-else class="empty-vat-placeholder">
        <div
          v-if="canCreate"
          class="dashed-add-card is-clickable"
          title="点击为该颜色行建缸"
          @click="handleCreateVat"
        >
          <el-icon class="add-icon"><Plus /></el-icon>
          <span class="add-title">未排缸 / ＋ 建缸</span>
          <span class="add-sub">点击立即为本颜色行排产建缸</span>
        </div>
        <div v-else class="dashed-add-card is-readonly" title="该颜色明细尚未排缸">
          <span class="add-title">未排缸</span>
          <span class="add-sub">暂无排产反馈缸</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { hasPermission } from '@/directives/permission/hasPermi'
import VatCard from './VatCard.vue'
import type {
  ProgressFeedbackColorSectionVO,
  ProgressFeedbackVatVO
} from '@/api/lunaerp-elmui/production/progress-feedback'

const props = defineProps<{
  section: ProgressFeedbackColorSectionVO
}>()

const canCreate = computed(() => {
  return hasPermission(['lunaerp:production:progress-feedback:create'])
})

const isPastDelivery = computed(() => {
  if (!props.section.deliveryDate) return false
  const todayStr = new Date().toISOString().split('T')[0]
  return props.section.deliveryDate < todayStr
})

const coverageTagType = computed(() => {
  const cov = Number(props.section.coveragePercent) || 0
  if (cov >= 100) return 'success'
  if (cov > 0) return 'warning'
  return 'info'
})

const emit = defineEmits<{
  (e: 'create-vat', section: ProgressFeedbackColorSectionVO): void
  (e: 'open-detail', vat: ProgressFeedbackVatVO): void
  (e: 'refresh'): void
  (e: 'open-feedback', vat: ProgressFeedbackVatVO): void
}>()

const handleCreateVat = () => {
  emit('create-vat', props.section)
}

const handleOpenDetail = (vat: ProgressFeedbackVatVO) => {
  emit('open-detail', vat)
}

const handleRefresh = () => {
  emit('refresh')
}

const handleOpenFeedback = (vat: ProgressFeedbackVatVO) => {
  emit('open-feedback', vat)
}
</script>

<style scoped lang="scss">
.color-section-card {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.section-header {
  padding: 14px 20px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;

  .header-main {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .title-row {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 13px;
      color: #606266;
      flex-wrap: wrap;

      .contract-tag {
        font-weight: 600;
        color: #409eff;
        background: #ecf5ff;
        padding: 2px 8px;
        border-radius: 4px;
        border: 1px solid #d9ecff;
      }

      .client-name {
        font-weight: 500;
        color: #303133;
      }

      .divider {
        color: #dcdfe6;
      }

      .factory-name,
      .product-info {
        color: #606266;
      }

      .delivery-date {
        margin-left: 6px;
        padding: 2px 6px;
        border-radius: 4px;
        background: #f4f4f5;
        font-size: 12px;

        &.is-past {
          color: #f56c6c;
          background: #fef0f0;
          font-weight: 600;
        }
      }
    }

    .color-row {
      display: flex;
      align-items: center;
      gap: 10px;

      .line-badge {
        background: #909399;
        color: #ffffff;
        font-size: 11px;
        padding: 1px 6px;
        border-radius: 10px;
        font-weight: 600;
      }

      .color-name {
        font-size: 16px;
        font-weight: 700;
        color: #1f2d3d;
      }

      .customer-color-no {
        font-size: 13px;
        color: #909399;
      }

      .order-quantity {
        font-size: 13px;
        color: #606266;
        margin-left: 12px;

        strong {
          font-size: 15px;
          color: #303133;
        }

        .pieces-text {
          color: #909399;
          font-size: 12px;
          margin-left: 4px;
        }
      }
    }
  }

  .header-summary {
    display: flex;
    align-items: center;
    gap: 20px;

    .summary-item {
      font-size: 13px;
      color: #606266;

      .summary-label {
        margin-right: 6px;
      }

      .summary-value {
        font-size: 16px;
        font-weight: 700;
        color: #303133;
      }

      .pieces-summary {
        font-size: 12px;
        color: #909399;
        margin-left: 4px;
      }
    }

    .coverage-box {
      display: flex;
      align-items: center;
      gap: 8px;

      .coverage-label {
        font-size: 13px;
        color: #606266;
      }

      .coverage-tag {
        font-weight: 600;
        font-size: 13px;
      }
    }

    .create-vat-btn {
      margin-left: 6px;
    }
  }
}

.section-body {
  padding: 18px 20px;

  .vat-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
  }

  .empty-vat-placeholder {
    .dashed-add-card {
      border: 2px dashed #dcdfe6;
      border-radius: 8px;
      padding: 32px 20px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      background: #fafbfc;
      color: #909399;
      transition: all 0.2s;

      &.is-clickable {
        cursor: pointer;

        &:hover {
          border-color: #409eff;
          background: #f0f7ff;
          color: #409eff;

          .add-icon,
          .add-title {
            color: #409eff;
          }

          .add-sub {
            color: #79bbff;
          }
        }
      }

      &.is-readonly {
        cursor: default;
        border-style: solid;
        border-color: #ebeef5;
      }

      .add-icon {
        font-size: 26px;
        color: #b4bccc;
        transition: color 0.2s;
      }

      .add-title {
        font-size: 14px;
        font-weight: 600;
        color: #909399;
        transition: color 0.2s;
      }

      .add-sub {
        font-size: 12px;
        color: #c0c4cc;
        transition: color 0.2s;
      }
    }
  }
}
</style>
