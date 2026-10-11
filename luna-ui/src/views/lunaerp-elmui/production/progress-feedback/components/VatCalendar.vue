<template>
  <div class="vat-calendar-container" v-loading="loading">
    <!-- 顶部筛选与月历控制栏 -->
    <el-card class="calendar-toolbar-card" shadow="never">
      <div class="calendar-toolbar">
        <div class="toolbar-left">
          <!-- 月份切换与今天回位 -->
          <el-button-group class="month-nav-group">
            <el-button :icon="ArrowLeft" @click="handlePrevMonth">上个月</el-button>
            <el-button @click="handleToday">今天</el-button>
            <el-button @click="handleNextMonth">
              下个月
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </el-button-group>

          <!-- 月份选择器 -->
          <el-date-picker
            v-model="monthPickerValue"
            type="month"
            format="YYYY 年 MM 月"
            value-format="YYYY-MM"
            placeholder="选择月份"
            :clearable="false"
            class="month-picker"
            @change="handleMonthPickerChange"
          />

          <span class="month-display">{{ currentMonthTitle }}</span>
        </div>

        <div class="toolbar-right">
          <!-- 承染厂筛选 -->
          <el-select
            v-model="filterParams.dyeingFactoryId"
            placeholder="全部承染厂"
            clearable
            filterable
            remote
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            style="width: 180px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>

          <!-- 进度状态筛选 -->
          <el-select
            v-model="filterParams.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
            @change="handleFilterChange"
          >
            <el-option
              v-for="dict in statusOptions"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>

          <!-- 状态图例 -->
          <div class="calendar-legend">
            <span class="legend-item"><i class="dot scheduled"></i>已排缸</span>
            <span class="legend-item"><i class="dot dyeing"></i>工序中</span>
            <span class="legend-item"><i class="dot completed"></i>已完成(灰化)</span>
          </div>

          <el-button :icon="Refresh" circle title="刷新当前月数据" @click="loadData" />
        </div>
      </div>
    </el-card>

    <!-- el-calendar 月历主体 -->
    <el-card class="calendar-main-card" shadow="never">
      <el-calendar v-model="calendarDate">
        <template #header>
          <!-- 隐藏默认头部，由外部自定义工具栏驱动 -->
          <div style="display: none"></div>
        </template>
        <template #date-cell="{ data }">
          <div
            class="calendar-date-cell"
            :class="{
              'is-current-month': data.type === 'current-month',
              'is-today': data.day === todayStr
            }"
          >
            <div class="date-header">
              <span class="day-num">{{ formatDayNumber(data.day) }}</span>
              <span
                v-if="vatsByDate[data.day] && vatsByDate[data.day].length > 0"
                class="vat-count-badge"
              >
                {{ vatsByDate[data.day].length }} 缸
              </span>
            </div>

            <!-- 当日缸列表 -->
            <div class="vat-dots-list">
              <div
                v-for="vat in vatsByDate[data.day] || []"
                :key="vat.feedbackId"
                class="vat-item"
                :class="[
                  getVatStatusTheme(vat.status).className,
                  { 'is-completed': vat.status === 'COMPLETED' }
                ]"
                :title="getVatTooltip(vat)"
                @click.stop="handleVatClick(vat)"
              >
                <span class="status-dot"></span>
                <span class="vat-no">{{ vat.vatNo }}</span>
                <span class="vat-quantity"> {{ vat.quantity ?? 0 }}{{ vat.unit || 'KG' }} </span>
                <span v-if="vat.pieces != null" class="vat-pieces"> ({{ vat.pieces }}匹) </span>
              </div>
            </div>
          </div>
        </template>
      </el-calendar>
    </el-card>

    <!-- 点缸改期弹窗 -->
    <el-dialog
      v-model="rescheduleDialogVisible"
      title="调整排缸日期"
      width="420px"
      :close-on-click-modal="false"
      destroy-on-close
      append-to-body
    >
      <div v-if="activeVat" class="reschedule-dialog-content">
        <el-descriptions :column="1" border size="small" class="vat-meta-desc">
          <el-descriptions-item label="缸号">
            <strong class="vat-no-highlight">{{ activeVat.vatNo }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="承染厂">
            {{ activeVat.dyeingFactoryName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="合同号 / 颜色">
            {{ activeVat.contractNo || '-' }} /
            {{ activeVat.colorName || activeVat.customerColorNo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="本缸数量">
            {{ activeVat.quantity ?? 0 }} {{ activeVat.unit || 'KG' }}
            <span v-if="activeVat.pieces != null">({{ activeVat.pieces }} 匹)</span>
          </el-descriptions-item>
          <el-descriptions-item label="当前排缸日">
            <span class="current-date-tag">{{ activeVat.scheduledDate || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="reschedule-picker-row">
          <div class="picker-label"><span class="required-star">*</span> 新排缸日期：</div>
          <el-date-picker
            v-model="newScheduledDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择新排缸日期"
            style="width: 100%"
          />
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rescheduleDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!newScheduledDate || !canRescheduleVat(activeVat)"
            @click="handleConfirmReschedule"
          >
            确认改期
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import {
  getVatCalendar,
  updateProgressFeedback,
  type VatCalendarVatVO,
  type VatCalendarReqVO
} from '@/api/lunaerp-elmui/production/progress-feedback'
import {
  getVatStatusTheme,
  groupVatsByDate,
  filterVats,
  canRescheduleVat,
  formatDayNumber,
  shiftMonth,
  buildReschedulePayload
} from './vat-calendar-helper'

defineOptions({ name: 'VatCalendar' })

const loading = ref(false)
const rawVats = ref<VatCalendarVatVO[]>([])

// 日历基准日期（Date 驱动 el-calendar）
const calendarDate = ref<Date>(new Date())
const todayStr = dayjs().format('YYYY-MM-DD')

// 当前月份字符串 (YYYY-MM)
const currentMonthStr = computed(() => {
  return dayjs(calendarDate.value).format('YYYY-MM')
})

// 月份选择器绑定值
const monthPickerValue = ref<string>(dayjs().format('YYYY-MM'))

// 月份展示标题
const currentMonthTitle = computed(() => {
  return dayjs(calendarDate.value).format('YYYY 年 MM 月')
})

// 筛选条件
const filterParams = reactive<{
  dyeingFactoryId?: number
  status?: string
}>({
  dyeingFactoryId: undefined,
  status: undefined
})

// 供应商下拉列表
const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])

// 状态选项列表（对齐 lunaerp_progress_feedback_status 字典）
const statusOptions = computed(() => {
  const dicts = getStrDictOptions(DICT_TYPE.LUNAERP_PROGRESS_FEEDBACK_STATUS)
  if (dicts && dicts.length > 0) {
    return dicts.filter((d) => d.value !== 'NOT_SCHEDULED')
  }
  return [
    { label: '已排缸', value: 'SCHEDULED' },
    { label: '染色中', value: 'DYEING' },
    { label: '固色中', value: 'FIXING' },
    { label: '质检中', value: 'QUALITY_CHECK' },
    { label: '已完成', value: 'COMPLETED' }
  ]
})

// 过滤后的缸列表
const filteredVats = computed(() => {
  return filterVats(rawVats.value, filterParams)
})

// 按日期分组聚合的缸数据映射表
const vatsByDate = computed(() => {
  return groupVatsByDate(filteredVats.value)
})

// 加载承染方列表
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query || undefined
    })
    supplierOptions.value = res.list || []
  } catch (err) {
    console.error('加载供应商列表失败', err)
  } finally {
    supplierLoading.value = false
  }
}

// 查询当前月排缸数据
const loadData = async () => {
  loading.value = true
  try {
    const req: VatCalendarReqVO = {
      month: currentMonthStr.value,
      dyeingFactoryId: filterParams.dyeingFactoryId,
      status: filterParams.status
    }
    const res = await getVatCalendar(req)
    rawVats.value = res.vats || []
  } catch (err) {
    console.error('查询排缸日历数据失败', err)
  } finally {
    loading.value = false
  }
}

// 切换至上个月
const handlePrevMonth = () => {
  calendarDate.value = shiftMonth(calendarDate.value, -1)
  monthPickerValue.value = currentMonthStr.value
}

// 切换至今天
const handleToday = () => {
  calendarDate.value = new Date()
  monthPickerValue.value = dayjs().format('YYYY-MM')
}

// 切换至下个月
const handleNextMonth = () => {
  calendarDate.value = shiftMonth(calendarDate.value, 1)
  monthPickerValue.value = currentMonthStr.value
}

// 月份选择器变动
const handleMonthPickerChange = (val: string) => {
  if (val) {
    calendarDate.value = dayjs(`${val}-01`).toDate()
  }
}

// 筛选条件变动
const handleFilterChange = () => {
  loadData()
}

// 监听月份变化重新加载
watch(currentMonthStr, () => {
  monthPickerValue.value = currentMonthStr.value
  loadData()
})

// 缸卡提示文本
const getVatTooltip = (vat: VatCalendarVatVO) => {
  const theme = getVatStatusTheme(vat.status)
  const lines = [
    `缸号: ${vat.vatNo} (${theme.label})`,
    `数量: ${vat.quantity ?? 0} ${vat.unit || 'KG'}${vat.pieces != null ? ` / ${vat.pieces}匹` : ''}`,
    `染厂: ${vat.dyeingFactoryName || '-'}`,
    `颜色: ${vat.customerColorNo || ''} ${vat.colorName || ''}`,
    `合同: ${vat.contractNo || '-'}`
  ]
  if (vat.status === 'COMPLETED') {
    lines.push('【已完成缸，不可修改排缸日】')
  }
  return lines.join('\n')
}

// 点缸改期弹窗控制
const rescheduleDialogVisible = ref(false)
const activeVat = ref<VatCalendarVatVO | null>(null)
const newScheduledDate = ref<string>('')
const submitting = ref(false)

// 点缸交互：COMPLETED 缸前端禁用
const handleVatClick = (vat: VatCalendarVatVO) => {
  if (!canRescheduleVat(vat)) {
    ElMessage.warning('已完成的缸不允许修改排缸日期')
    return
  }
  activeVat.value = vat
  newScheduledDate.value = vat.scheduledDate
  rescheduleDialogVisible.value = true
}

// 确认改期：调现有 updateProgressFeedback 接口
const handleConfirmReschedule = async () => {
  if (!activeVat.value) return
  if (!canRescheduleVat(activeVat.value)) {
    ElMessage.warning('已完成的缸不允许修改排缸日期')
    return
  }
  if (!newScheduledDate.value) {
    ElMessage.warning('请选择新排缸日期')
    return
  }
  if (newScheduledDate.value === activeVat.value.scheduledDate) {
    ElMessage.info('排缸日期未变动')
    rescheduleDialogVisible.value = false
    return
  }

  submitting.value = true
  try {
    const payload = buildReschedulePayload(activeVat.value, newScheduledDate.value)
    await updateProgressFeedback(payload)
    ElMessage.success('排缸日期调整成功')
    rescheduleDialogVisible.value = false
    // 成功后局部刷新当月数据
    await loadData()
  } catch (err) {
    console.error('改期失败', err)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  searchSuppliers('')
  loadData()
})
</script>

<style scoped lang="scss">
.vat-calendar-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.calendar-toolbar-card {
  :deep(.el-card__body) {
    padding: 12px 16px;
  }
}

.calendar-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .month-picker {
      width: 140px;
    }

    .month-display {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-left: 4px;
    }
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .calendar-legend {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 0 8px;
      font-size: 12px;
      color: #606266;

      .legend-item {
        display: flex;
        align-items: center;
        gap: 4px;

        .dot {
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;

          &.scheduled {
            background-color: #409eff;
          }
          &.dyeing {
            background-color: #e6a23c;
          }
          &.completed {
            background-color: #909399;
          }
        }
      }
    }
  }
}

.calendar-main-card {
  :deep(.el-card__body) {
    padding: 0;
  }

  :deep(.el-calendar__body) {
    padding: 12px 20px 20px;
  }

  :deep(.el-calendar-table) {
    td.is-selected {
      background-color: #f5f7fa;
    }

    .el-calendar-day {
      height: 120px;
      padding: 6px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      box-sizing: border-box;

      &:hover {
        background-color: #f0f7ff;
      }
    }
  }
}

.calendar-date-cell {
  height: 100%;
  display: flex;
  flex-direction: column;

  &.is-today {
    .day-num {
      color: #409eff;
      font-weight: bold;
    }
  }

  &:not(.is-current-month) {
    opacity: 0.45;
  }

  .date-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;

    .day-num {
      font-size: 13px;
      color: #303133;
    }

    .vat-count-badge {
      font-size: 11px;
      color: #409eff;
      background: #ecf5ff;
      padding: 0 4px;
      border-radius: 3px;
      font-weight: 500;
    }
  }

  .vat-dots-list {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding-right: 2px;

    &::-webkit-scrollbar {
      width: 3px;
    }
    &::-webkit-scrollbar-thumb {
      background: #dcdfe6;
      border-radius: 2px;
    }

    .vat-item {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 11px;
      cursor: pointer;
      border: 1px solid transparent;
      transition: all 0.15s ease;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      &:hover:not(.is-completed) {
        transform: translateY(-1px);
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        flex-shrink: 0;
      }

      .vat-no {
        font-weight: 600;
        flex-shrink: 0;
      }

      .vat-quantity {
        flex-shrink: 0;
      }

      .vat-pieces {
        color: inherit;
        opacity: 0.8;
      }

      // 已排缸态
      &.status-scheduled {
        background-color: #ecf5ff;
        border-color: #d9ecff;
        color: #409eff;

        .status-dot {
          background-color: #409eff;
        }
      }

      // 染色中态
      &.status-dyeing {
        background-color: #fdf6ec;
        border-color: #faecd8;
        color: #e6a23c;

        .status-dot {
          background-color: #e6a23c;
        }
      }

      // 固色中态
      &.status-fixing {
        background-color: #fdf6ec;
        border-color: #faecd8;
        color: #e6a23c;

        .status-dot {
          background-color: #e6a23c;
        }
      }

      // 质检中态
      &.status-quality-check {
        background-color: #fdf6ec;
        border-color: #faecd8;
        color: #e6a23c;

        .status-dot {
          background-color: #e6a23c;
        }
      }

      // 已完成态：强制灰化与禁用
      &.status-completed,
      &.is-completed {
        background-color: #f4f4f5 !important;
        border-color: #e9e9eb !important;
        color: #909399 !important;
        opacity: 0.65;
        cursor: not-allowed;

        .status-dot {
          background-color: #909399 !important;
        }

        .vat-no,
        .vat-quantity,
        .vat-pieces {
          color: #909399 !important;
        }
      }
    }
  }
}

.reschedule-dialog-content {
  .vat-meta-desc {
    margin-bottom: 16px;

    .vat-no-highlight {
      color: #409eff;
      font-size: 14px;
    }

    .current-date-tag {
      color: #e6a23c;
      font-weight: 500;
    }
  }

  .reschedule-picker-row {
    margin-top: 12px;

    .picker-label {
      font-size: 13px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 8px;

      .required-star {
        color: #f56c6c;
      }
    }
  }
}
</style>
