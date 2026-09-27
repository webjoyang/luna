<template>
  <div class="py-16px">
    <div class="mb-12px flex justify-between items-center">
      <div class="text-14px font-medium text-gray-700">
        卷清单总览（跨明细平铺，共 {{ allRolls.length }} 条卷记录）
      </div>
      <div v-if="allRolls.length" class="text-13px text-gray-500">
        合计卷重：<span class="font-bold text-blue-700">{{ totalWeight.toFixed(2) }} KG</span>
      </div>
    </div>

    <el-table :data="allRolls" border stripe class="w-full">
      <el-table-column label="#" type="index" width="50" align="center" />

      <el-table-column label="卷号" min-width="140">
        <template #default="{ row }">
          {{ row.rollNo || '—' }}
        </template>
      </el-table-column>

      <el-table-column label="所属产品" min-width="180">
        <template #default="{ row }">
          <span>{{ row.productName || '—' }}</span>
          <span v-if="row.productCode" class="ml-4px text-gray-400 text-12px">({{ row.productCode }})</span>
        </template>
      </el-table-column>

      <el-table-column label="缸号/批次" min-width="120">
        <template #default="{ row }">
          {{ row.vatNo || '—' }}
        </template>
      </el-table-column>

      <el-table-column label="实称重量 KG" width="130" align="right">
        <template #default="{ row }">
          {{ row.weight != null ? `${Number(row.weight).toFixed(2)} KG` : '—' }}
        </template>
      </el-table-column>

      <el-table-column label="米数" width="110" align="right">
        <template #default="{ row }">
          {{ row.lengthM != null ? `${Number(row.lengthM).toFixed(2)} m` : '—' }}
        </template>
      </el-table-column>

      <el-table-column label="备注" min-width="160">
        <template #default="{ row }">
          {{ row.remark || '—' }}
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!allRolls.length" description="暂无卷清单记录" :image-size="80" class="mt-24px" />
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { ShipmentItemVO, ShipmentRollVO } from '@/api/lunaerp-elmui/business/shipment'

defineOptions({ name: 'ShipmentWorkspaceRollsOverviewTab' })

const props = defineProps<{
  /** 所有明细行（含各自的 rolls 数组） */
  items?: ShipmentItemVO[]
}>()

/** 卷清单条目附带所属明细的归属信息，用于总览平铺展示 */
interface RollWithContext extends ShipmentRollVO {
  /** 所属明细的产品名称快照 */
  productName?: string
  /** 所属明细的产品编码快照 */
  productCode?: string
  /** 所属明细的缸号/批次快照 */
  vatNo?: string
}

/** 跨所有明细行平铺卷清单，附加所属产品/缸号归属列 */
const allRolls = computed<RollWithContext[]>(() => {
  const result: RollWithContext[] = []
  for (const item of props.items ?? []) {
    for (const roll of item.rolls ?? []) {
      result.push({
        ...roll,
        productName: item.productName,
        productCode: item.productCode,
        vatNo: item.vatNo
      })
    }
  }
  return result
})

/** 全部卷合计重量 */
const totalWeight = computed(() =>
  allRolls.value.reduce((acc, r) => acc + (r.weight ?? 0), 0)
)
</script>
