<template>
  <el-dialog
    v-model="dialogVisible"
    title="按日批量打印签字单"
    width="1180px"
    top="5vh"
    :close-on-click-modal="false"
  >
    <!-- 口径说明显著标注 -->
    <el-alert
      title="口径说明：按【发现日】（质检确认记录创建时间）汇总当天全部状态记录（含已撤销标记）。打印后作为车间纸质签字凭单。"
      type="info"
      show-icon
      :closable="false"
      class="!mb-4"
    />

    <!-- 工具栏：日期选择 + 查询 + 打印 -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-3">
        <span class="text-sm font-medium text-gray-700">发现日期：</span>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          placeholder="选择发现日期"
          class="!w-160px"
        />
        <el-button type="primary" @click="handleQuery">
          <Icon icon="ep:search" class="mr-1" /> 查询
        </el-button>
      </div>
      <div>
        <el-button
          type="success"
          :disabled="list.length === 0"
          @click="handlePrint"
        >
          <Icon icon="ep:printer" class="mr-1" /> 打印签字单
        </el-button>
      </div>
    </div>

    <!-- 屏幕展示表格（全状态只读，已撤销置灰标记） -->
    <el-table
      v-loading="loading"
      :data="list"
      border
      stripe
      size="small"
      max-height="480px"
      :row-class-name="tableRowClassName"
    >
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column
        label="检验单号"
        prop="inspectionOrderCode"
        min-width="150"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span class="font-mono">{{ row.inspectionOrderCode || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="卷号" prop="rollNo" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-semibold">{{ row.rollNo || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="缸号" prop="vatNo" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ row.vatNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="等级" prop="grade" width="90" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_INSPECTION_GRADE" :value="row.grade" />
        </template>
      </el-table-column>
      <el-table-column label="产品名称" prop="productName" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.productName || '—' }}</template>
      </el-table-column>
      <el-table-column label="机台号" prop="machineNo" width="90" align="center">
        <template #default="{ row }">{{ row.machineNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="责任类型" prop="responsibility" width="130" align="center">
        <template #default="{ row }">
          <DictTag
            v-if="row.responsibility"
            :type="DICT_TYPE.LUNAERP_INSPECTION_RESPONSIBILITY"
            :value="row.responsibility"
          />
          <span v-else class="text-gray-400">待认定</span>
        </template>
      </el-table-column>
      <el-table-column label="挡车工" prop="workerName" width="100" align="center">
        <template #default="{ row }">{{ row.workerName || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_INSPECTION_CONFIRMATION_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="认定人" prop="confirmedByName" width="100" align="center">
        <template #default="{ row }">{{ row.confirmedByName || '—' }}</template>
      </el-table-column>
      <el-table-column
        label="认定时间"
        prop="confirmedAt"
        width="150"
        align="center"
        :formatter="dateFormatter"
      />
      <el-table-column label="原因备注" prop="note" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.note || '—' }}</template>
      </el-table-column>
      <template #empty>
        <el-empty description="该发现日暂无质检确认记录" :image-size="80" />
      </template>
    </el-table>

    <!-- 底部状态摘要与操作 -->
    <div class="mt-4 flex justify-between items-center text-sm text-gray-500">
      <div>
        共 <span class="font-bold text-gray-700">{{ list.length }}</span> 条记录
        <span v-if="revokedCount > 0" class="ml-2 text-red-500">
          （含已撤销作废 {{ revokedCount }} 条）
        </span>
      </div>
      <div class="flex gap-2">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :disabled="list.length === 0"
          @click="handlePrint"
        >
          <Icon icon="ep:printer" class="mr-1" /> 打印签字单
        </el-button>
      </div>
    </div>

    <!-- 打印专用作用域纸质结构（屏幕隐藏，打印时仅展示此区域） -->
    <div id="inspectionPrintArea" class="print-paper-container">
      <div class="print-header">
        <h2 class="print-title">车间质检确认签字单</h2>
        <div class="print-meta">
          <span><strong>发现日期：</strong>{{ selectedDate }}</span>
          <span><strong>打印时间：</strong>{{ printTime }}</span>
          <span><strong>总卷数：</strong>{{ list.length }} 卷</span>
        </div>
      </div>

      <table class="print-table">
        <thead>
          <tr>
            <th style="width: 40px">序号</th>
            <th style="width: 120px">检验单号</th>
            <th style="width: 90px">卷号</th>
            <th style="width: 70px">缸号</th>
            <th style="width: 50px">等级</th>
            <th>产品名称</th>
            <th style="width: 60px">机台</th>
            <th style="width: 100px">责任类型</th>
            <th style="width: 70px">挡车工</th>
            <th style="width: 70px">状态</th>
            <th style="width: 70px">认定人</th>
            <th style="width: 120px">认定时间</th>
            <th>原因备注</th>
            <th style="width: 90px">挡车工签字</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(item, index) in list"
            :key="item.id"
            :class="{ 'print-row-revoked': item.status === 'REVOKED' }"
          >
            <td style="text-align: center">{{ index + 1 }}</td>
            <td>{{ item.inspectionOrderCode || '—' }}</td>
            <td style="font-weight: bold">{{ item.rollNo }}</td>
            <td>{{ item.vatNo || '—' }}</td>
            <td style="text-align: center">{{ getGradeLabel(item.grade) }}</td>
            <td>{{ item.productName || '—' }}</td>
            <td style="text-align: center">{{ item.machineNo || '—' }}</td>
            <td style="text-align: center">{{ getResponsibilityLabel(item.responsibility) }}</td>
            <td style="text-align: center">{{ item.workerName || '—' }}</td>
            <td style="text-align: center">
              <span v-if="item.status === 'REVOKED'" style="color: #999; font-weight: bold">
                [已撤销]
              </span>
              <span v-else>{{ getStatusLabel(item.status) }}</span>
            </td>
            <td style="text-align: center">{{ item.confirmedByName || '—' }}</td>
            <td style="text-align: center">{{ formatPrintTime(item.confirmedAt) }}</td>
            <td>{{ item.note || '—' }}</td>
            <td style="text-align: center"></td>
          </tr>
        </tbody>
      </table>

      <div class="print-footer">
        <div class="sign-block">
          <span>车间领班签字：____________________</span>
          <span>质检主管签字：____________________</span>
          <span>确认日期：____________________</span>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import { dateFormatter, formatDate } from '@/utils/formatTime'
import * as InspectionConfirmationApi from '@/api/lunaerp-elmui/production/inspection-confirmation'
import type { InspectionConfirmationPrintViewVO } from '@/api/lunaerp-elmui/production/inspection-confirmation'

defineOptions({ name: 'InspectionConfirmationPrintDialog' })

const dialogVisible = ref(false)
const loading = ref(false)
const selectedDate = ref(formatDate(new Date(), 'YYYY-MM-DD'))
const list = ref<InspectionConfirmationPrintViewVO[]>([])
const printTime = ref('')

const revokedCount = computed(() => {
  return list.value.filter((item) => item.status === 'REVOKED').length
})

/** 表格行样式：已撤销行置灰 */
const tableRowClassName = ({ row }: { row: InspectionConfirmationPrintViewVO }) => {
  return row.status === 'REVOKED' ? 'revoked-row' : ''
}

/** 字典文案转换（打印专用纯文本） */
const getDictLabel = (dictType: string, val?: string, defaultVal = '—') => {
  if (!val) return defaultVal
  const options = getDictOptions(dictType)
  const found = options.find((o) => o.value === val)
  return found ? found.label : val
}

const getGradeLabel = (val?: string) => getDictLabel(DICT_TYPE.LUNAERP_INSPECTION_GRADE, val)
const getResponsibilityLabel = (val?: string) =>
  getDictLabel(DICT_TYPE.LUNAERP_INSPECTION_RESPONSIBILITY, val, '待认定')
const getStatusLabel = (val?: string) =>
  getDictLabel(DICT_TYPE.LUNAERP_INSPECTION_CONFIRMATION_STATUS, val)

const formatPrintTime = (val?: string) => {
  if (!val) return '—'
  return formatDate(val, 'YYYY-MM-DD HH:mm')
}

/** 查询数据 */
const loadData = async () => {
  loading.value = true
  try {
    const data = await InspectionConfirmationApi.getPrintView({ date: selectedDate.value })
    list.value = data || []
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  loadData()
}

/** 触发打印 */
const handlePrint = () => {
  printTime.value = formatDate(new Date(), 'YYYY-MM-DD HH:mm:ss')
  // 延迟一帧确保 DOM 更新后调起浏览器打印窗口
  setTimeout(() => {
    window.print()
  }, 100)
}

/** 打开弹窗 */
const open = (dateStr?: string) => {
  selectedDate.value = dateStr || formatDate(new Date(), 'YYYY-MM-DD')
  dialogVisible.value = true
  loadData()
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
:deep(.revoked-row) {
  color: #909399;
  background-color: #fafafa !important;
  text-decoration: line-through;

  .el-tag {
    opacity: 0.6;
  }
}

/* 屏幕模式下隐藏打印专用 DOM */
.print-paper-container {
  display: none;
}
</style>

<!-- 浏览器打印样式（全局非 scoped，确保能隐藏弹窗外元素且保持正常流式分页） -->
<style lang="scss">
@media print {
  /* 隐藏页面主结构与非打印元素 */
  body > div:not(.el-overlay):not(.el-overlay-dialog) {
    display: none !important;
  }

  .el-overlay {
    background: transparent !important;
    position: static !important;
    inset: auto !important;
  }

  .el-overlay-dialog {
    position: static !important;
  }

  .el-dialog {
    box-shadow: none !important;
    border: none !important;
    background: transparent !important;
    width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  .el-dialog__header,
  .el-dialog__footer,
  .el-dialog__body > *:not(#inspectionPrintArea) {
    display: none !important;
  }

  /* 仅展示打印区域，采用 static 正常流布局以支持自然分页 */
  #inspectionPrintArea {
    display: block !important;
    position: static !important;
    width: 100% !important;
    margin: 0 !important;
    padding: 0 !important;
    background: #fff !important;
    box-sizing: border-box !important;
  }

  @page {
    size: landscape;
    margin: 10mm;
  }

  .print-header {
    text-align: center;
    margin-bottom: 12px;

    .print-title {
      font-size: 20px;
      font-weight: bold;
      color: #000;
      margin-bottom: 8px;
    }

    .print-meta {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #333;
      padding: 0 4px;
    }
  }

  .print-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 11px;
    color: #000;
    page-break-inside: auto;

    tr {
      page-break-inside: avoid;
      page-break-after: auto;
    }

    th,
    td {
      border: 1px solid #000;
      padding: 4px 6px;
      line-height: 1.3;
    }

    th {
      background-color: #f2f2f2 !important;
      font-weight: bold;
      text-align: center;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }

    .print-row-revoked {
      color: #888;
      text-decoration: line-through;
    }
  }

  .print-footer {
    margin-top: 24px;
    font-size: 12px;
    color: #000;
    page-break-inside: avoid;

    .sign-block {
      display: flex;
      justify-content: space-between;
      padding: 0 16px;
    }
  }
}
</style>

