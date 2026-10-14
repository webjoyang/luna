<template>
  <div class="color-instruction-sheet-wrapper print-paper text-gray-900 mx-auto">
    <div
      v-for="(sheet, sheetIdx) in sheetList"
      :key="sheet.id || sheetIdx"
      class="sheet-page"
      :class="{ 'page-break-after': sheetIdx < sheetList.length - 1 }"
    >
      <!-- 1. 单据抬头 -->
      <div class="print-header text-center mb-12px">
        <div class="title text-22px font-bold tracking-widest text-black mb-4px">打色指示单</div>
        <div class="subtitle text-11px text-gray-500 uppercase tracking-wider">LAB DIP INSTRUCTION</div>
      </div>

      <!-- 2. 抬头区核心元数据 -->
      <div class="meta-box mb-12px text-12px border border-gray-800 p-8px bg-gray-50">
        <div class="grid grid-cols-4 gap-y-6px gap-x-12px">
          <div>
            <span class="text-gray-600">打色单号：</span>
            <span class="font-mono font-bold">{{ sheet.instructionNo || '—' }}</span>
          </div>
          <div>
            <span class="text-gray-600">单据日期：</span>
            <span class="font-medium">{{ sheet.instructionDate || '—' }}</span>
          </div>
          <div>
            <span class="text-gray-600">要求完成日：</span>
            <span class="font-medium">{{ sheet.deliveryDate || '—' }}</span>
          </div>
          <div>
            <span class="text-gray-600">单据状态：</span>
            <span class="font-bold">{{ formatStatusText(sheet.status) }}</span>
          </div>

          <div class="col-span-2">
            <span class="text-gray-600">客户名称：</span>
            <span class="font-bold">{{ sheet.clientName || '—' }}</span>
          </div>
          <div class="col-span-2">
            <span class="text-gray-600">客户联系人：</span>
            <!-- 打印豁免惯例全量展示，不脱敏 -->
            <span class="font-medium">{{ sheet.contactPerson || '—' }}</span>
          </div>

          <div class="col-span-2">
            <span class="text-gray-600">产品编号/名称：</span>
            <span class="font-bold">{{ formatProductInfo(sheet) }}</span>
          </div>
          <div class="col-span-2">
            <span class="text-gray-600">打色承接方：</span>
            <span class="font-bold">{{ formatLabFactoryName(sheet) }}</span>
          </div>

          <div>
            <span class="text-gray-600">成分：</span>
            <span>{{ sheet.composition || '—' }}</span>
          </div>
          <div>
            <span class="text-gray-600">幅宽：</span>
            <span>{{ sheet.width || '—' }}</span>
          </div>
          <div>
            <span class="text-gray-600">克重：</span>
            <span>{{ sheet.gsm || '—' }}</span>
          </div>
          <div>
            <span class="text-gray-600">确认类型：</span>
            <span>{{ formatConfirmType(sheet.confirmType) }}</span>
          </div>
        </div>
      </div>

      <!-- 3. 颜色行表（输出全部轮次行，REJECTED 行划线标注轮次与行状态） -->
      <table class="print-table w-full text-11px border-collapse mb-12px">
        <thead>
          <tr class="bg-gray-100 font-bold">
            <th class="w-36px text-center">#</th>
            <th class="w-90px text-left">客户色号</th>
            <th class="w-90px text-left">厂内色号</th>
            <th class="w-80px text-left">颜色名称</th>
            <th class="w-70px text-center">色块</th>
            <th class="w-65px text-center">主灯源</th>
            <th class="w-65px text-center">辅灯源</th>
            <th class="w-100px text-left">色卡参考</th>
            <th class="w-45px text-right">份数</th>
            <th class="w-55px text-center">轮次</th>
            <th class="w-65px text-center">行状态</th>
            <th class="text-left">驳回意见 / 行备注</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(color, cIdx) in sheet.colors || []"
            :key="color.id || cIdx"
            :class="{ 'rejected-row': color.status === 'REJECTED' }"
          >
            <td class="text-center" :class="{ 'strike-text': color.status === 'REJECTED' }">{{ color.lineNo || cIdx + 1 }}</td>
            <td :class="{ 'strike-text': color.status === 'REJECTED' }" class="font-mono">
              {{ color.customerColorNo || '—' }}
            </td>
            <td :class="{ 'strike-text': color.status === 'REJECTED' }" class="font-mono">
              {{ color.factoryColorNo || '—' }}
            </td>
            <td :class="{ 'strike-text': color.status === 'REJECTED' }" class="font-medium">
              {{ color.colorName || '—' }}
            </td>
            <td class="text-center">
              <span
                v-if="color.colorHex"
                class="color-badge"
                :style="{ backgroundColor: color.colorHex }"
                :title="color.colorHex"
              />
              <span :class="{ 'strike-text': color.status === 'REJECTED' }" class="font-mono text-10px">
                {{ color.colorHex || '—' }}
              </span>
            </td>
            <td class="text-center" :class="{ 'strike-text': color.status === 'REJECTED' }">{{ color.lightPrimary || '—' }}</td>
            <td class="text-center" :class="{ 'strike-text': color.status === 'REJECTED' }">{{ color.lightSecondary || '—' }}</td>
            <td :class="{ 'strike-text': color.status === 'REJECTED' }">{{ color.colorCardRef || '—' }}</td>
            <td class="text-right font-mono" :class="{ 'strike-text': color.status === 'REJECTED' }">{{ color.copies != null ? color.copies : '—' }}</td>
            <td class="text-center font-bold" :class="{ 'strike-text': color.status === 'REJECTED' }">
              {{ formatColorRound(color.round) }}
            </td>
            <td class="text-center" :class="{ 'strike-text': color.status === 'REJECTED' }">
              <DictTag :type="DICT_TYPE.LUNAERP_COLOR_LINE_STATUS" :value="color.status" size="small" />
            </td>
            <td>
              <div v-if="color.status === 'REJECTED' && color.rejectReason" class="reject-reason-tip">
                <strong>驳回意见：</strong>{{ color.rejectReason }}
              </div>
              <div v-if="color.remark" class="text-gray-600 text-10px">
                {{ color.remark }}
              </div>
              <span v-if="!color.rejectReason && !color.remark">—</span>
            </td>
          </tr>
          <tr v-if="!sheet.colors || sheet.colors.length === 0">
            <td colspan="12" class="text-center py-4 text-gray-400">暂无颜色明细行</td>
          </tr>
        </tbody>
      </table>

      <!-- 4. 客户反馈与备注区 -->
      <div class="section-box mb-12px text-11px border border-gray-800 p-8px bg-white">
        <div class="grid grid-cols-2 gap-x-12px gap-y-4px">
          <div>
            <span class="font-bold text-gray-800">客户反馈意见：</span>
            <span>{{ sheet.customerFeedback || '—' }}</span>
          </div>
          <div>
            <span class="font-bold text-gray-800">单据备注：</span>
            <span>{{ sheet.remark || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 5. 签字位 -->
      <div class="sign-box text-11px border border-gray-800 p-10px bg-gray-50 mb-12px">
        <div class="grid grid-cols-3 gap-x-16px">
          <div class="sign-col">
            <div><span class="text-gray-600">制单人：</span><span class="font-medium">{{ sheet.creator || '—' }}</span></div>
            <div><span class="text-gray-600">制单签字：</span><span class="sign-line"></span></div>
          </div>
          <div class="sign-col">
            <div><span class="text-gray-600">打色打样人：</span><span class="sign-line"></span></div>
            <div><span class="text-gray-600">完成日期：</span><span class="sign-line"></span></div>
          </div>
          <div class="sign-col">
            <div><span class="text-gray-600">客户确认签字：</span><span class="sign-line"></span></div>
            <div><span class="text-gray-600">确认日期：</span><span class="sign-line"></span></div>
          </div>
        </div>
      </div>

      <!-- 6. 底部信息 -->
      <div class="flex justify-between text-10px text-gray-500 mt-6px">
        <span>系统打印凭证 · 生产管理部</span>
        <span>打印时间：{{ formatPrintTime(sheet.printTime) }}</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { PropType } from 'vue'
import { DICT_TYPE } from '@/utils/dict'
import type { ColorInstructionPrintVO } from '@/api/lunaerp-elmui/production/color-instruction'
import {
  formatStatusText,
  formatLineStatusText,
  formatConfirmType,
  formatProductInfo,
  formatLabFactoryName,
  formatColorRound,
  formatPrintTime
} from './utils'

defineOptions({ name: 'ColorInstructionSheet' })

const props = defineProps({
  /** 单张打色指示单数据 */
  data: {
    type: Object as PropType<ColorInstructionPrintVO | null>,
    default: null
  },
  /** 批量打色指示单数据（可选） */
  list: {
    type: Array as PropType<ColorInstructionPrintVO[]>,
    default: () => []
  }
})

const sheetList = computed<ColorInstructionPrintVO[]>(() => {
  if (props.list && props.list.length > 0) {
    return props.list
  }
  if (props.data) {
    return [props.data]
  }
  return []
})
</script>

<style scoped>
.color-instruction-sheet-wrapper {
  width: 100%;
  max-width: 820px;
  min-height: 500px;
}

.sheet-page {
  width: 100%;
}

.page-break-after {
  page-break-after: always;
  margin-bottom: 24px;
}

.print-table th,
.print-table td {
  border: 1px solid #333;
  padding: 4px 6px;
}

.rejected-row {
  background-color: #fff5f5 !important;
  color: #777;
}

.strike-text {
  text-decoration: line-through;
  color: #888;
}

.strike-text :deep(.el-tag) {
  text-decoration: line-through;
}

.reject-reason-tip {
  color: #c53030;
  font-size: 10px;
  font-weight: 500;
  margin-top: 2px;
  text-decoration: none !important;
}

.color-badge {
  display: inline-block;
  width: 12px;
  height: 12px;
  vertical-align: middle;
  margin-right: 3px;
  border: 1px solid #999;
  border-radius: 2px;
}

.status-tag {
  display: inline-block;
  padding: 1px 4px;
  border-radius: 2px;
  font-size: 10px;
  font-weight: 500;
}

.status-tag.approved {
  background-color: #e6f7ed;
  color: #1e7e34;
  border: 1px solid #a3cfbb;
}

.status-tag.rejected {
  background-color: #fdf0f0;
  color: #dc3545;
  border: 1px solid #f5c2c7;
}

.status-tag.pending {
  background-color: #f4f6f8;
  color: #6c757d;
  border: 1px solid #dee2e6;
}

.sign-line {
  border-bottom: 1px solid #333;
  display: inline-block;
  min-width: 100px;
  height: 14px;
  vertical-align: bottom;
  margin-left: 4px;
}

@media print {
  @page {
    size: portrait;
    margin: 10mm;
  }
  .color-instruction-sheet-wrapper {
    width: 100%;
    margin: 0;
  }
}
</style>
