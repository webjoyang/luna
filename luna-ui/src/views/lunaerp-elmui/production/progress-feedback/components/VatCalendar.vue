<template>
  <div class="vat-calendar-wrapper">
    <!-- 顶部工具栏与筛选 -->
    <el-card class="calendar-toolbar-card" shadow="never">
      <div class="toolbar-content">
        <!-- 左侧月份切换与今天回位 -->
        <div class="month-controls">
          <el-button-group>
            <el-button :icon="ArrowLeft" @click="handlePrevMonth">上个月</el-button>
            <el-button :icon="Calendar" @click="handleToday">今天</el-button>
            <el-button @click="handleNextMonth">
              下个月
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </el-button-group>

          <el-date-picker
            v-model="calendarDate"
            type="month"
            placeholder="选择月份"
            :clearable="false"
            style="width: 140px; margin-left: 12px"
            @change="handleDatePickerChange"
          />

          <span class="current-month-display">
            {{ formatYearMonth(calendarDate) }} 排缸概览
          </span>
        </div>

        <!-- 中间图例说明 -->
        <div class="calendar-legend">
          <span class="legend-item">
            <span class="legend-dot scheduled"></span>
            已排缸
          </span>
          <span class="legend-item">
            <span class="legend-dot in-progress"></span>
            染色/固色/质检
          </span>
          <span class="legend-item">
            <span class="legend-dot completed"></span>
            已完成 (灰化禁改)
          </span>
          <span class="legend-tip">
            <el-icon><InfoFilled /></el-icon>
            点击缸点可快捷改期
          </span>
        </div>

        <!-- 右侧筛选：承染厂与状态 -->
        <div class="filter-controls">
          <el-select
            v-model="filterDyeingFactoryId"
            placeholder="承染厂筛选"
            clearable
            filterable
            remote
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            style="width: 170px"
            @change="loadCalendarData"
            @clear="loadCalendarData"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>

          <el-select
            v-model="filterStatus"
            placeholder="状态筛选"
            clearable
            style="width: 130px; margin-left: 8px"
            @change="loadCalendarData"
            @clear="loadCalendarData"
          >
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_PROGRESS_FEEDBACK_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>

          <el-button
            :icon="Refresh"
            circle
            title="刷新当月数据"
            style="margin-left: 8px"
            @click="loadCalendarData"
          />
        </div>
      </div>
    </el-card>

    <!-- 月历主体 -->
    <el-card class="calendar-main-card" shadow="never">
      <div v-loading="loading" class="calendar-container">
        <el-calendar v-model="calendarDate">
          <!-- 隐藏 el-calendar 默认自带的顶部标题，统一由上方卡片控制 -->
          <template #header>
            <div class="calendar-custom-header">
              <span class="header-month-title">{{ formatYearMonth(calendarDate) }}</span>
              <span class="header-vat-summary">
                本月排缸总计: <strong>{{ monthVats.length }}</strong> 缸
              </span>
            </div>
          </template>

          <!-- 日期格内渲染当日缸点 -->
          <template #date-cell="{ data }">
            <div
              class="calendar-date-cell"
              :class="{
                'is-today': isDateToday(data.day),
                'is-other-month': data.type !== 'current-month'
              }"
            >
              <div class="cell-header">
                <span class="cell-day-num">{{ data.day.split('-')[2] }}</span>
                <span
                  v-if="groupedVats[data.day]?.length"
                  class="cell-vat-count"
                  title="当日缸数"
                >
                  {{ groupedVats[data.day].length }}缸
                </span>
              </div>

              <!-- 当日缸点列表 -->
              <div v-if="groupedVats[data.day]?.length" class="cell-vat-list">
                <el-tooltip
                  v-for="vat in groupedVats[data.day]"
                  :key="vat.feedbackId"
                  placement="top"
                  :enterable="false"
                  raw-content
                  :content="renderVatTooltipContent(vat)"
                >
                  <div
                    class="vat-point"
                    :class="{
                      'is-completed': vat.status === 'COMPLETED',
                      'is-editable': vat.status !== 'COMPLETED'
                    }"
                    :style="getVatPointStyle(vat.status)"
                    @click.stop="handleVatClick(vat)"
                  >
                    <span class="vat-status-dot"></span>
                    <span class="vat-no-text">{{ vat.vatNo }}</span>
                    <span class="vat-qty-text">{{ vat.quantity }}{{ vat.unit || 'KG' }}</span>
                  </div>
                </el-tooltip>
              </div>
            </div>
          </template>
        </el-calendar>
      </div>
    </el-card>

    <!-- 改期小弹窗 -->
    <el-dialog
      v-model="rescheduleDialogVisible"
      title="排缸改期"
      width="420px"
      append-to-body
      destroy-on-close
      class="reschedule-dialog"
    >
      <div v-if="targetVat" class="dialog-body">
        <el-descriptions :column="1" border size="small" class="vat-info-desc">
          <el-descriptions-item label="缸号">
            <strong>{{ targetVat.vatNo }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <DictTag
              :type="DICT_TYPE.LUNAERP_PROGRESS_FEEDBACK_STATUS"
              :value="targetVat.status"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="本缸数量">
            {{ targetVat.quantity }} {{ targetVat.unit || 'KG' }}
            <span v-if="targetVat.pieces">({{ targetVat.pieces }} 匹)</span>
          </el-descriptions-item>
          <el-descriptions-item label="颜色/合同">
            {{ targetVat.colorName || '-' }}
            <span v-if="targetVat.customerColorNo">({{ targetVat.customerColorNo }})</span>
            / {{ targetVat.contractNo || '未关联' }}
          </el-descriptions-item>
          <el-descriptions-item label="承染厂">
            {{ targetVat.dyeingFactoryName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="原排缸日">
            {{ targetVat.scheduledDate || '未排' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-width="90px" class="reschedule-form" style="margin-top: 16px">
          <el-form-item label="新排缸日期" required>
            <el-date-picker
              v-model="newScheduledDate"
              type="date"
              placeholder="选择新排缸日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rescheduleDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="rescheduleSubmitting"
            @click="handleConfirmReschedule"
          >
            确定改期
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  Calendar,
  Refresh,
  InfoFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import {
  getProgressFeedbackCalendar,
  getProgressFeedback,
  updateProgressFeedback,
  type ProgressFeedbackCalendarVatVO
} from '@/api/lunaerp-elmui/production/progress-feedback'
import {
  formatYearMonth,
  formatDate,
  getAdjacentMonth,
  groupVatsByDate,
  canRescheduleVat,
  getVatCalendarStatusStyle,
  buildRescheduleUpdateVO
} from '../model/vat-calendar'

defineOptions({ name: 'VatCalendar' })

const calendarDate = ref<Date>(new Date())
const loading = ref(false)
const monthVats = ref<ProgressFeedbackCalendarVatVO[]>([])

// 筛选条件
const filterDyeingFactoryId = ref<number | undefined>(undefined)
const filterStatus = ref<string | undefined>(undefined)

// 承染厂下拉
const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])

// 改期弹窗状态
const rescheduleDialogVisible = ref(false)
const rescheduleSubmitting = ref(false)
const targetVat = ref<ProgressFeedbackCalendarVatVO | null>(null)
const newScheduledDate = ref<string>('')

// 日期分组缸点
const groupedVats = computed(() => {
  return groupVatsByDate(monthVats.value)
})

// 加载供应商列表
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query || undefined
    })
    supplierOptions.value = res.list || []
  } catch (error) {
    console.error('加载承染厂列表失败', error)
  } finally {
    supplierLoading.value = false
  }
}

// 加载月历扁平缸数据（局部刷新）
const loadCalendarData = async () => {
  loading.value = true
  const monthStr = formatYearMonth(calendarDate.value)
  try {
    const res = await getProgressFeedbackCalendar({
      month: monthStr,
      dyeingFactoryId: filterDyeingFactoryId.value,
      status: filterStatus.value
    })
    monthVats.value = res?.vats || []
  } catch (error) {
    console.error('加载排缸月历数据失败', error)
  } finally {
    loading.value = false
  }
}

// 月份切换控制
const handlePrevMonth = () => {
  const currentMonth = formatYearMonth(calendarDate.value)
  const prevMonth = getAdjacentMonth(currentMonth, -1)
  const [y, m] = prevMonth.split('-').map(Number)
  calendarDate.value = new Date(y, m - 1, 1)
}

const handleNextMonth = () => {
  const currentMonth = formatYearMonth(calendarDate.value)
  const nextMonth = getAdjacentMonth(currentMonth, 1)
  const [y, m] = nextMonth.split('-').map(Number)
  calendarDate.value = new Date(y, m - 1, 1)
}

const handleToday = () => {
  calendarDate.value = new Date()
}

const handleDatePickerChange = (val: Date) => {
  if (val) {
    calendarDate.value = val
  }
}

// 监听 calendarDate 月份变化重新请求当月数据
watch(
  () => formatYearMonth(calendarDate.value),
  () => {
    loadCalendarData()
  }
)

// 判断日期是否为今天
const todayStr = formatDate(new Date())
const isDateToday = (dayStr: string) => dayStr === todayStr

// 缸点样式
const getVatPointStyle = (status: string) => {
  const style = getVatCalendarStatusStyle(status)
  if (style.isCompleted) {
    return {
      backgroundColor: '#f4f4f5',
      color: '#909399',
      borderColor: '#e4e7ed'
    }
  }
  return {
    backgroundColor: style.bg,
    color: style.color,
    borderColor: style.border
  }
}

// Tooltip 浮层内容格式化
const renderVatTooltipContent = (vat: ProgressFeedbackCalendarVatVO): string => {
  const statusLabel =
    getStrDictOptions(DICT_TYPE.LUNAERP_PROGRESS_FEEDBACK_STATUS).find(
      (d) => d.value === vat.status
    )?.label || vat.status

  const isCompleted = vat.status === 'COMPLETED'
  const actionTip = isCompleted
    ? '<span style="color: #909399;">(已完成缸不可改期)</span>'
    : '<span style="color: #409eff;">(点击改期)</span>'

  return `
    <div style="line-height: 1.5; font-size: 12px; text-align: left;">
      <div><strong>缸号:</strong> ${vat.vatNo} ${actionTip}</div>
      <div><strong>状态:</strong> ${statusLabel}</div>
      <div><strong>数量:</strong> ${vat.quantity} ${vat.unit || 'KG'}${vat.pieces ? ' (' + vat.pieces + ' 匹)' : ''}</div>
      <div><strong>颜色:</strong> ${vat.colorName || '-'} ${vat.customerColorNo ? '(' + vat.customerColorNo + ')' : ''}</div>
      <div><strong>销售合同:</strong> ${vat.contractNo || '-'}</div>
      <div><strong>承染厂:</strong> ${vat.dyeingFactoryName || '-'}</div>
      <div><strong>排缸日:</strong> ${vat.scheduledDate || '-'}</div>
    </div>
  `
}

// 点击缸点
const handleVatClick = (vat: ProgressFeedbackCalendarVatVO) => {
  if (!canRescheduleVat(vat.status)) {
    ElMessage.warning('已完成缸不可改期')
    return
  }

  targetVat.value = vat
  newScheduledDate.value = vat.scheduledDate || formatDate(new Date())
  rescheduleDialogVisible.value = true
}

// 确认改期
const handleConfirmReschedule = async () => {
  if (!targetVat.value) return
  if (!newScheduledDate.value) {
    ElMessage.warning('请选择新排缸日期')
    return
  }

  rescheduleSubmitting.value = true
  try {
    // 1. 获取完整缸详情以安全保留全部阶段日期与备注信息
    const detail = await getProgressFeedback(targetVat.value.feedbackId)
    // 2. 构造改期 VO 并校验
    const updateVO = buildRescheduleUpdateVO(detail, newScheduledDate.value)
    // 3. 提交现有反馈缸更新接口
    await updateProgressFeedback(updateVO)
    ElMessage.success('排缸改期成功')
    rescheduleDialogVisible.value = false
    // 4. 局部刷新当月数据
    await loadCalendarData()
  } catch (error: any) {
    console.error('改期失败', error)
  } finally {
    rescheduleSubmitting.value = false
  }
}

onMounted(() => {
  searchSuppliers('')
  loadCalendarData()
})
</script>

<style scoped lang="scss">
.vat-calendar-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.calendar-toolbar-card {
  :deep(.el-card__body) {
    padding: 12px 16px;
  }

  .toolbar-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
  }

  .month-controls {
    display: flex;
    align-items: center;

    .current-month-display {
      margin-left: 14px;
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }
  }

  .calendar-legend {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: 12px;
    color: #606266;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .legend-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        display: inline-block;

        &.scheduled {
          background-color: #409eff;
        }

        &.in-progress {
          background-color: #e6a23c;
        }

        &.completed {
          background-color: #909399;
        }
      }
    }

    .legend-tip {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #909399;
      font-size: 12px;
    }
  }

  .filter-controls {
    display: flex;
    align-items: center;
  }
}

.calendar-main-card {
  :deep(.el-card__body) {
    padding: 16px;
  }

  .calendar-custom-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding-bottom: 6px;

    .header-month-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .header-vat-summary {
      font-size: 13px;
      color: #606266;

      strong {
        color: #409eff;
        font-size: 15px;
      }
    }
  }

  :deep(.el-calendar-table) {
    .el-calendar-day {
      height: 110px;
      padding: 4px 6px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    td.is-selected .el-calendar-day {
      background-color: #f0f7ff;
    }
  }

  .calendar-date-cell {
    height: 100%;
    display: flex;
    flex-direction: column;

    &.is-other-month {
      opacity: 0.45;
    }

    .cell-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;

      .cell-day-num {
        font-size: 13px;
        font-weight: 600;
        color: #303133;
      }

      .cell-vat-count {
        font-size: 11px;
        color: #909399;
        background: #f0f2f5;
        padding: 0 4px;
        border-radius: 2px;
        line-height: 16px;
      }
    }

    &.is-today .cell-day-num {
      color: #409eff;
      font-weight: 700;
    }

    .cell-vat-list {
      flex: 1;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 3px;
      padding-right: 2px;

      &::-webkit-scrollbar {
        width: 3px;
      }
      &::-webkit-scrollbar-thumb {
        background: #e4e7ed;
        border-radius: 2px;
      }

      .vat-point {
        display: flex;
        align-items: center;
        gap: 4px;
        padding: 2px 6px;
        border-radius: 4px;
        border: 1px solid transparent;
        font-size: 11px;
        line-height: 16px;
        transition: all 0.15s ease;
        user-select: none;

        .vat-status-dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;
          background-color: currentColor;
          flex-shrink: 0;
        }

        .vat-no-text {
          font-weight: 600;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          max-width: 65px;
        }

        .vat-qty-text {
          white-space: nowrap;
          font-size: 10px;
          opacity: 0.85;
          margin-left: auto;
        }

        &.is-editable {
          cursor: pointer;

          &:hover {
            transform: translateY(-1px);
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.08);
            filter: brightness(0.96);
          }
        }

        &.is-completed {
          cursor: not-allowed;
          filter: grayscale(1);
          opacity: 0.7;
        }
      }
    }
  }
}

.reschedule-dialog {
  .dialog-body {
    padding: 0 4px;
  }
}
</style>
