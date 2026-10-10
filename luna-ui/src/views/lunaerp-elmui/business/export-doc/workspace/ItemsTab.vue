<template>
  <div class="py-16px">
    <div v-if="!isReadOnly" class="mb-12px flex justify-between items-center">
      <div>
        <el-button type="primary" plain @click="addItem">
          <Icon icon="ep:plus" class="mr-4px" /> 添加明细行
        </el-button>
        <span class="text-12px text-gray-500 ml-12px">
          单价与数量修改后金额将自动重算，定稿时服务端校验明细非空。
        </span>
      </div>
    </div>

    <!-- 明细表格 -->
    <el-table
      :data="items"
      border
      stripe
      style="width: 100%"
      empty-text="暂无货物明细，点击上方添加"
      show-summary
      :summary-method="getSummaries"
    >
      <el-table-column label="#" type="index" width="50" align="center" />

      <el-table-column label="中文品名" min-width="140">
        <template #default="{ row }">
          <el-input v-if="!isReadOnly" v-model="row.nameCn" placeholder="中文品名" />
          <span v-else>{{ row.nameCn || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="英文品名" min-width="160">
        <template #default="{ row }">
          <el-input v-if="!isReadOnly" v-model="row.nameEn" placeholder="英文品名 (模板优先显示)" />
          <span v-else>{{ row.nameEn || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="HS 编码" width="120">
        <template #default="{ row }">
          <el-input v-if="!isReadOnly" v-model="row.hsCode" placeholder="HS Code" />
          <span v-else class="font-mono">{{ row.hsCode || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="规格" min-width="130">
        <template #default="{ row }">
          <el-input v-if="!isReadOnly" v-model="row.spec" placeholder="成分/门幅/规格" />
          <span v-else>{{ row.spec || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="数量" width="110" align="right">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.quantity"
            :precision="2"
            :step="1"
            :min="0"
            :controls="false"
            class="!w-full"
            @change="handleCalcRowAmount(row)"
          />
          <span v-else class="font-mono">{{ row.quantity != null ? row.quantity.toFixed(2) : '0.00' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="单位" width="75" align="center">
        <template #default="{ row }">
          <el-input v-if="!isReadOnly" v-model="row.unit" placeholder="M/KG" />
          <span v-else>{{ row.unit || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="单价" width="110" align="right">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.unitPrice"
            :precision="2"
            :step="0.1"
            :min="0"
            :controls="false"
            class="!w-full"
            @change="handleCalcRowAmount(row)"
          />
          <span v-else class="font-mono">{{ row.unitPrice != null ? row.unitPrice.toFixed(2) : '0.00' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          <span class="font-mono font-bold">{{ row.amount != null ? Number(row.amount).toFixed(2) : '0.00' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="净重(KG)" width="100" align="right">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.netWeight"
            :precision="2"
            :step="1"
            :min="0"
            :controls="false"
            class="!w-full"
          />
          <span v-else class="font-mono">{{ row.netWeight != null ? row.netWeight.toFixed(2) : '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="毛重(KG)" width="100" align="right">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.grossWeight"
            :precision="2"
            :step="1"
            :min="0"
            :controls="false"
            class="!w-full"
          />
          <span v-else class="font-mono">{{ row.grossWeight != null ? row.grossWeight.toFixed(2) : '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="箱数" width="90" align="right">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.packages"
            :precision="0"
            :step="1"
            :min="0"
            :controls="false"
            class="!w-full"
          />
          <span v-else class="font-mono">{{ row.packages != null ? row.packages : '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="体积(CBM)" width="100" align="right">
        <template #default="{ row }">
          <el-input-number
            v-if="!isReadOnly"
            v-model="row.volume"
            :precision="3"
            :step="0.01"
            :min="0"
            :controls="false"
            class="!w-full"
          />
          <span v-else class="font-mono">{{ row.volume != null ? row.volume.toFixed(3) : '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="原产地" width="90" align="center">
        <template #default="{ row }">
          <el-input v-if="!isReadOnly" v-model="row.origin" placeholder="CHINA" />
          <span v-else>{{ row.origin || 'CHINA' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="备注" min-width="110">
        <template #default="{ row }">
          <el-input v-if="!isReadOnly" v-model="row.remark" placeholder="备注" />
          <span v-else>{{ row.remark || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column v-if="!isReadOnly" label="操作" width="90" align="center" fixed="right">
        <template #default="{ $index }">
          <el-button type="danger" link @click="removeItem($index)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script lang="ts" setup>
import type { ExportDocItemVO } from '@/api/lunaerp-elmui/business/export-doc'

defineOptions({ name: 'ExportDocItemsTab' })

const props = defineProps<{
  items: ExportDocItemVO[]
  isReadOnly: boolean
}>()

/** 添加一行 */
const addItem = () => {
  props.items.push({
    lineNo: props.items.length + 1,
    nameCn: '',
    nameEn: '',
    hsCode: '',
    spec: '',
    quantity: 0,
    unit: 'M',
    unitPrice: 0,
    amount: 0,
    netWeight: undefined,
    grossWeight: undefined,
    packages: undefined,
    volume: undefined,
    origin: 'CHINA',
    remark: ''
  })
}

/** 删除一行 */
const removeItem = (idx: number) => {
  props.items.splice(idx, 1)
  props.items.forEach((item, i) => {
    item.lineNo = i + 1
  })
}

/** 单价/数量变动重算行金额 */
const handleCalcRowAmount = (row: ExportDocItemVO) => {
  const qty = Number(row.quantity || 0)
  const price = Number(row.unitPrice || 0)
  row.amount = Number((qty * price).toFixed(2))
}

/** 底部合计行 */
const getSummaries = (param: { columns: any[]; data: ExportDocItemVO[] }) => {
  const { columns, data } = param
  const sums: string[] = []
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    const prop = column.property
    if (['quantity', 'amount', 'netWeight', 'grossWeight', 'packages', 'volume'].includes(prop)) {
      const values = data.map((item) => Number(item[prop as keyof ExportDocItemVO] || 0))
      const sum = values.reduce((prev, curr) => prev + curr, 0)
      if (prop === 'packages') {
        sums[index] = sum > 0 ? String(Math.round(sum)) : '—'
      } else if (prop === 'volume') {
        sums[index] = sum > 0 ? sum.toFixed(3) : '—'
      } else {
        sums[index] = sum.toFixed(2)
      }
    } else {
      sums[index] = ''
    }
  })
  return sums
}
</script>
