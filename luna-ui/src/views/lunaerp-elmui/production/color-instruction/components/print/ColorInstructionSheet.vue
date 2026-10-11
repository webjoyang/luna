<template>
  <div class="color-instruction-sheet print-paper text-gray-900 mx-auto">
    <!-- 1. 抬头与大标题 -->
    <div class="print-header text-center mb-16px">
      <div class="text-22px font-bold tracking-widest text-black mb-4px">打色指示单</div>
      <div class="text-11px text-gray-500 uppercase tracking-wider">COLOR INSTRUCTION SHEET</div>
    </div>

    <!-- 2. 单据核心元数据网格 -->
    <div class="meta-box mb-12px text-12px border border-gray-800 p-8px bg-gray-50">
      <div class="grid grid-cols-3 gap-y-6px gap-x-12px">
        <div>
          <span class="text-gray-600">打色单号：</span>
          <span class="font-mono font-bold">{{ instruction?.instructionNo || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">单据日期：</span>
          <span class="font-medium">{{ instruction?.instructionDate || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">要求完成日：</span>
          <span class="font-medium">{{ instruction?.deliveryDate || '—' }}</span>
        </div>

        <div>
          <span class="text-gray-600">客户名称：</span>
          <span class="font-bold">{{ instruction?.clientName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">客户联系人：</span>
          <span>{{ instruction?.contactPerson || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">打色承接方：</span>
          <span class="font-medium">{{ labFactoryDisplay }}</span>
        </div>

        <div>
          <span class="text-gray-600">产品编号：</span>
          <span class="font-mono font-bold">{{ instruction?.productCode || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">产品名称：</span>
          <span class="font-medium">{{ instruction?.productName || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">确认类型：</span>
          <span>{{ confirmTypeText }}</span>
        </div>

        <div>
          <span class="text-gray-600">成　　份：</span>
          <span>{{ instruction?.composition || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">幅　　宽：</span>
          <span>{{ instruction?.width || '—' }}</span>
        </div>
        <div>
          <span class="text-gray-600">克　　重：</span>
          <span>{{ instruction?.gsm || '—' }}</span>
        </div>
      </div>
    </div>

    <!-- 3. 颜色行明细表格（全部轮次均输出，REJECTED 行划线标注并显示驳回原因） -->
    <table class="print-table w-full text-11px border-collapse mb-12px">
      <thead>
        <tr class="bg-gray-100 font-bold">
          <th class="w-32px text-center">#</th>
          <th class="w-80px text-left">客户色号</th>
          <th class="w-80px text-left">厂内色号</th>
          <th class="text-left">颜色名称</th>
          <th class="w-50px text-center">色块</th>
          <th class="w-60px text-center">主灯源</th>
          <th class="w-60px text-center">辅灯源</th>
          <th class="w-90px text-left">色卡参考</th>
          <th class="w-45px text-center">份数</th>
          <th class="w-45px text-center">轮次</th>
          <th class="w-70px text-center">行状态</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(item, idx) in colors"
          :key="item.id || idx"
          :class="{ 'rejected-row': item.status === 'REJECTED' }"
        >
          <td class="text-center">{{ item.lineNo || idx + 1 }}</td>
          <td :class="{ 'rejected-strike': item.status === 'REJECTED' }">
            <span class="font-mono font-medium">{{ item.customerColorNo || '—' }}</span>
          </td>
          <td :class="{ 'rejected-strike': item.status === 'REJECTED' }">
            <span class="font-mono">{{ item.factoryColorNo || '—' }}</span>
          </td>
          <td :class="{ 'rejected-strike': item.status === 'REJECTED' }">
            <div>{{ item.colorName || '—' }}</div>
            <div v-if="item.status === 'REJECTED' && item.rejectReason" class="reject-reason-tag">
              驳回原因：{{ item.rejectReason }}
            </div>
            <div v-if="item.remark" class="text-10px text-gray-500 mt-0.5">
              备注：{{ item.remark }}
            </div>
          </td>
          <td class="text-center">
            <span
              v-if="item.colorHex"
              class="inline-block w-4 h-4 rounded border border-gray-400 align-middle shadow-xs"
              :style="{ backgroundColor: item.colorHex }"
              :title="item.colorHex"
            />
            <span v-else class="text-gray-400">—</span>
          </td>
          <td class="text-center">{{ item.lightPrimary || '—' }}</td>
          <td class="text-center">{{ item.lightSecondary || '—' }}</td>
          <td>{{ item.colorCardRef || '—' }}</td>
          <td class="text-center font-mono">{{ item.copies != null ? item.copies : 1 }}</td>
          <td class="text-center font-bold text-blue-700 font-mono">R{{ item.round || 1 }}</td>
          <td class="text-center">
            <span v-if="item.status === 'APPROVED'" class="font-bold text-green-700">通过</span>
            <span v-else-if="item.status === 'REJECTED'" class="font-bold text-red-600">驳回</span>
            <span v-else-if="item.status === 'PENDING'" class="text-blue-600">待打色</span>
            <span v-else>{{ item.status || '—' }}</span>
          </td>
        </tr>
        <tr v-if="!colors.length">
          <td colspan="11" class="text-center py-12px text-gray-400">暂无颜色行数据</td>
        </tr>
      </tbody>
    </table>

    <!-- 4. 反馈意见与单据备注 -->
    <div class="feedback-box border border-gray-800 p-8px mb-16px text-12px bg-gray-50">
      <div class="mb-4px">
        <span class="font-bold text-gray-700">客户反馈意见：</span>
        <span :class="instruction?.customerFeedback ? 'text-gray-900 font-medium' : 'text-gray-400'">
          {{ instruction?.customerFeedback || '暂无反馈意见' }}
        </span>
      </div>
      <div>
        <span class="font-bold text-gray-700">单据备注：</span>
        <span :class="instruction?.remark ? 'text-gray-900' : 'text-gray-400'">
          {{ instruction?.remark || '无' }}
        </span>
      </div>
    </div>

    <!-- 5. 底部签字位 -->
    <div class="sign-block mt-20px text-12px">
      <div class="grid grid-cols-4 gap-16px pt-6px">
        <div class="border border-gray-400 p-10px min-h-80px flex flex-col justify-between">
          <div class="font-bold text-12px">制单人：</div>
          <div class="mt-24px text-gray-700">
            <span>{{ instruction?.creator || '___________' }}</span>
          </div>
        </div>
        <div class="border border-gray-400 p-10px min-h-80px flex flex-col justify-between">
          <div class="font-bold text-12px">打色人：</div>
          <div class="mt-24px text-gray-700">
            <span>签字：___________</span>
          </div>
        </div>
        <div class="border border-gray-400 p-10px min-h-80px flex flex-col justify-between">
          <div class="font-bold text-12px">客户确认人：</div>
          <div class="mt-24px text-gray-700">
            <span>签字：___________</span>
          </div>
        </div>
        <div class="border border-gray-400 p-10px min-h-80px flex flex-col justify-between">
          <div class="font-bold text-12px">确认日期：</div>
          <div class="mt-24px text-gray-700">
            <span>{{ confirmedDateDisplay }}</span>
          </div>
        </div>
      </div>
      <div class="flex justify-between items-center text-10px text-gray-500 mt-16px pt-6px border-t border-dashed border-gray-300">
        <div>注：打色样需经客户或内部确认签字后方可下达投产。</div>
        <div>打印时间：{{ printTimeStr }}</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { PropType } from 'vue'
import type { ColorInstructionPrintVO } from '@/api/lunaerp-elmui/production/color-instruction'

defineOptions({ name: 'ColorInstructionSheet' })

const props = defineProps({
  data: {
    type: Object as PropType<ColorInstructionPrintVO | null>,
    default: null
  },
  list: {
    type: Array as PropType<ColorInstructionPrintVO[]>,
    default: () => []
  }
})

const instruction = computed<ColorInstructionPrintVO | null>(() => {
  if (props.data) return props.data
  if (props.list && props.list.length > 0) return props.list[0]
  return null
})

const colors = computed(() => {
  return instruction.value?.colors || []
})

const labFactoryDisplay = computed(() => {
  return instruction.value?.labFactoryName ? instruction.value.labFactoryName : '本厂打色房'
})

const confirmTypeText = computed(() => {
  const type = instruction.value?.confirmType
  if (type === 'CUSTOMER') return '客户确认'
  if (type === 'INTERNAL') return '内部确认'
  return '—'
})

const confirmedDateDisplay = computed(() => {
  if (instruction.value?.confirmedAt) {
    return instruction.value.confirmedAt.substring(0, 10)
  }
  return '_____年____月____日'
})

const printTimeStr = computed(() => {
  if (instruction.value?.printTime) {
    return instruction.value.printTime.replace('T', ' ').substring(0, 19)
  }
  const d = new Date()
  const pad = (n: number) => (n < 10 ? `0${n}` : `${n}`)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})
</script>

<style scoped>
.color-instruction-sheet {
  max-width: 800px;
  padding: 16px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  box-sizing: border-box;
}

.print-table th,
.print-table td {
  border: 1px solid #333;
  padding: 4px 6px;
}

.print-table th {
  background-color: #f3f4f6 !important;
  font-weight: 600;
  -webkit-print-color-adjust: exact;
  print-color-adjust: exact;
}

.meta-box,
.feedback-box {
  -webkit-print-color-adjust: exact;
  print-color-adjust: exact;
}

.rejected-row td {
  background-color: #fafafa;
}

.rejected-strike {
  text-decoration: line-through;
  color: #888;
}

.reject-reason-tag {
  text-decoration: none !important;
  display: block;
  font-size: 10px;
  color: #dc2626;
  font-weight: 500;
  margin-top: 2px;
}

@media print {
  @page {
    size: portrait;
    margin: 10mm;
  }
  .color-instruction-sheet {
    width: 100%;
    max-width: 100%;
    padding: 0;
    margin: 0;
  }
}
</style>
