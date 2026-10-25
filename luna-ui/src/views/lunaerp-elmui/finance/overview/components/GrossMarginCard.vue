<template>
  <el-card shadow="never" class="!border-gray-200">
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-8px">
          <Icon icon="ep:data-analysis" class="text-18px text-primary" />
          <span class="font-bold text-15px text-gray-900">经营毛利参考（管理口径）</span>
        </div>
        <el-tag type="danger" effect="plain" class="font-medium text-12px">
          {{ marginData?.disclaimer || '管理参考、期间错配、非法定报表' }}
        </el-tag>
      </div>
    </template>

    <!-- 顶部永久口径说明提示 -->
    <el-alert type="warning" :closable="false" show-icon class="mb-16px">
      <template #title>
        <span class="font-medium text-13px">管理毛利计算口径说明</span>
      </template>
      <template #default>
        <div class="text-12px text-gray-600 mt-2px leading-relaxed">
          1. <strong>收入口径</strong>：按发货确认日落入区间的销售收入分录（AR_INC 净额）；<br />
          2. <strong>成本口径</strong>：按确认日落入区间的供应商已确认对账单金额（confirmed
          单据成本）；<br />
          3.
          <strong>期间错配说明</strong
          >：因生产周期与对账账期差异，本卡片数据仅作为日常经营分析与毛利趋势监控参考，严禁作为法定会计报表使用。
        </div>
      </template>
    </el-alert>

    <!-- 无 AR 数据空态提示 -->
    <div
      v-if="marginData && !marginData.hasArData"
      class="p-16px mb-16px bg-amber-50 rounded-6px border border-amber-200 flex items-center space-x-10px text-amber-800 text-13px"
    >
      <Icon icon="ep:info-filled" class="text-18px flex-shrink-0" />
      <div>
        <strong>应收侧暂无分录数据：</strong
        >{{ marginData.emptyStateNotice || '待发货审核落账后将自动展示准确收入与毛利差额' }}
      </div>
    </div>

    <!-- 4 大指标卡片栅格 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-16px" v-loading="loading">
      <!-- 本期收入 -->
      <div class="p-16px rounded-8px bg-slate-50 border border-slate-200">
        <div class="text-12px text-gray-500 font-medium">本期营业收入 (净额)</div>
        <div class="mt-8px text-22px font-bold font-mono text-gray-900">
          ¥ {{ formatAmount(marginData?.revenue) }}
        </div>
        <div class="mt-4px text-11px text-gray-400">发货确认日口径</div>
      </div>

      <!-- 本期成本 -->
      <div class="p-16px rounded-8px bg-slate-50 border border-slate-200">
        <div class="text-12px text-gray-500 font-medium">本期采购成本 (已确认)</div>
        <div class="mt-8px text-22px font-bold font-mono text-gray-900">
          ¥ {{ formatAmount(marginData?.cost) }}
        </div>
        <div class="mt-4px text-11px text-gray-400">供应商确认对账单</div>
      </div>

      <!-- 毛利差额 -->
      <div
        class="p-16px rounded-8px border"
        :class="
          (marginData?.grossProfit || 0) >= 0
            ? 'bg-emerald-50 border-emerald-200'
            : 'bg-rose-50 border-rose-200'
        "
      >
        <div
          class="text-12px font-medium"
          :class="(marginData?.grossProfit || 0) >= 0 ? 'text-emerald-800' : 'text-rose-800'"
        >
          毛利差额 (收入 - 成本)
        </div>
        <div
          class="mt-8px text-22px font-bold font-mono"
          :class="(marginData?.grossProfit || 0) >= 0 ? 'text-emerald-700' : 'text-rose-700'"
        >
          ¥ {{ formatAmount(marginData?.grossProfit) }}
        </div>
        <div
          class="mt-4px text-11px"
          :class="(marginData?.grossProfit || 0) >= 0 ? 'text-emerald-600' : 'text-rose-600'"
        >
          {{ (marginData?.grossProfit || 0) >= 0 ? '经营顺差' : '经营倒挂' }}
        </div>
      </div>

      <!-- 综合毛利率 -->
      <div class="p-16px rounded-8px bg-slate-50 border border-slate-200">
        <div class="text-12px text-gray-500 font-medium">综合参考毛利率</div>
        <div
          class="mt-8px text-22px font-bold font-mono"
          :class="(marginData?.grossMarginRate || 0) >= 0 ? 'text-indigo-600' : 'text-rose-600'"
        >
          {{ formatPercent(marginData?.grossMarginRate) }}
        </div>
        <div class="mt-4px text-11px text-gray-400">毛利差额 / 本期收入</div>
      </div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import type { GrossMarginRespVO } from '@/api/lunaerp-elmui/finance/overview'
import { formatAmount } from '../../account/model/account'
import { formatPercent } from '../model/overview'

defineOptions({ name: 'GrossMarginCard' })

defineProps<{
  marginData?: GrossMarginRespVO
  loading?: boolean
}>()
</script>
