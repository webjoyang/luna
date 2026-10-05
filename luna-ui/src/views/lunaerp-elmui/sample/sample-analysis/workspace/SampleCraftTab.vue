<template>
  <el-form
    ref="formRef"
    :model="formData"
    label-width="110px"
    :disabled="isReadOnly"
  >
    <!-- 工艺分类卡片 -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="font-bold text-14px">工艺分类</div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="工序" prop="process">
            <el-select
              v-model="formData.process"
              placeholder="请选择工序（可留空）"
              clearable
              class="!w-full"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SAMPLE_PROCESS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="双染工艺" prop="dyeingProcess">
            <el-select
              v-model="formData.dyeingProcess"
              placeholder="请选择双染工艺（可留空）"
              clearable
              class="!w-full"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SAMPLE_DYEING_PROCESS)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="机型" prop="machineType">
            <el-select
              v-model="formData.machineType"
              placeholder="请选择机型（可留空）"
              clearable
              class="!w-full"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SAMPLE_MACHINE_TYPE)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 机台生产工艺参数卡片 -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="font-bold text-14px">机台生产工艺参数</div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="实测产量" prop="output">
            <el-input v-model="formData.output" placeholder="实测产量（如 120KG/天）" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="花号" prop="flowerNo">
            <el-input v-model="formData.flowerNo" placeholder="花号" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="白胚门幅" prop="whiteWidth">
            <el-input v-model="formData.whiteWidth" placeholder="白胚门幅" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="白胚克重" prop="whiteWeight">
            <el-input v-model="formData.whiteWeight" placeholder="白胚克重" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="花宽" prop="flowerWidth">
            <el-input v-model="formData.flowerWidth" placeholder="花宽" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="花高" prop="flowerHeight">
            <el-input v-model="formData.flowerHeight" placeholder="花高" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="齿轮" prop="gear">
            <el-input v-model="formData.gear" placeholder="齿轮" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="布架" prop="clothRack">
            <el-input v-model="formData.clothRack" placeholder="布架" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="开机转数" prop="startSpeed">
            <el-input v-model="formData.startSpeed" placeholder="开机转数" clearable />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="24">
        <el-col :span="8">
          <el-form-item label="送纱盘" prop="feederDisk">
            <el-input v-model="formData.feederDisk" placeholder="送纱盘" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="理论产量" prop="theoreticalOutput">
            <el-input v-model="formData.theoreticalOutput" placeholder="理论产量" clearable />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="线长" prop="stitchLength">
            <el-input v-model="formData.stitchLength" placeholder="线长（如 9.6cm/50针）" clearable />
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 纱线规格明细卡片（Issue 03） -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="font-bold text-14px">纱线规格明细</span>
            <el-tag :type="ratioTagType" size="small">
              用纱比例合计：{{ ratioTotalText }}%
            </el-tag>
          </div>
          <el-button
            v-if="!isReadOnly"
            type="primary"
            plain
            size="small"
            @click="handleAddYarnSpecRow"
          >
            <Icon icon="ep:plus" class="mr-1" />添加纱线规格
          </el-button>
        </div>
      </template>

      <!-- 比例不足/超标提示 Banner（不阻止保存） -->
      <el-alert
        v-if="ratioAlertMessage"
        :title="ratioAlertMessage"
        type="warning"
        show-icon
        :closable="false"
        class="mb-12px"
      />

      <el-table
        :data="formData.yarnSpecs || []"
        border
        style="width: 100%"
        empty-text="暂无纱线规格明细，请点击上方“添加纱线规格”"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column label="纱线规格" min-width="260">
          <template #default="{ row }">
            <span v-if="isReadOnly">
              {{ row.yarnTypeName ? `${row.yarnTypeName} / ${row.specName}` : (row.specName || '-') }}
            </span>
            <el-cascader
              v-else
              v-model="row.specCascadeValue"
              :options="cascaderOptions"
              :props="{ checkStrictly: false }"
              placeholder="选择材质与规格"
              filterable
              class="!w-full"
              @change="(val) => handleSpecCascadeChange(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="纺纱工艺" min-width="160">
          <template #default="{ row }">
            <span v-if="isReadOnly">{{ row.spinMethod || '-' }}</span>
            <el-select
              v-else
              v-model="row.spinMethod"
              placeholder="请选择纺法"
              clearable
              filterable
              allow-create
              default-first-option
              class="!w-full"
            >
              <el-option
                v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SAMPLE_SPIN_METHOD)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="参考单价(元/KG)" width="160">
          <template #default="{ row }">
            <span v-if="isReadOnly">{{ row.unitPrice != null ? row.unitPrice : '-' }}</span>
            <el-input-number
              v-else
              v-model="row.unitPrice"
              :min="0"
              :precision="2"
              :step="1"
              placeholder="单价"
              class="!w-full"
              controls-position="right"
            />
          </template>
        </el-table-column>

        <el-table-column label="用纱比例(%)" width="160">
          <template #default="{ row }">
            <span v-if="isReadOnly">{{ row.ratio != null ? `${row.ratio}%` : '-' }}</span>
            <el-input-number
              v-else
              v-model="row.ratio"
              :min="0"
              :max="100"
              :precision="2"
              :step="1"
              placeholder="必须 > 0"
              class="!w-full"
              controls-position="right"
            />
          </template>
        </el-table-column>

        <el-table-column label="行备注" min-width="180">
          <template #default="{ row }">
            <span v-if="isReadOnly">{{ row.remark || '-' }}</span>
            <el-input
              v-else
              v-model="row.remark"
              placeholder="备注"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column
          v-if="!isReadOnly"
          label="操作"
          width="80"
          align="center"
          fixed="right"
        >
          <template #default="{ $index }">
            <el-button
              type="danger"
              link
              @click="handleRemoveYarnSpecRow($index)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 大圆机针盘三角排列画板（票 05） -->
    <SampleStitchBoard
      v-model="formData.stitchArrange"
      :is-read-only="isReadOnly"
    />

    <!-- 纱线排列绕纱卡片（Issue 04） -->
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold text-14px">纱线排列绕纱</span>
          <el-button
            v-if="!isReadOnly"
            type="primary"
            plain
            size="small"
            @click="handleAddYarnArrangeRow"
          >
            <Icon icon="ep:plus" class="mr-1" />添加绕纱行
          </el-button>
        </div>
      </template>

      <el-table
        :data="formData.yarnArranges || []"
        border
        style="width: 100%"
        empty-text="暂无纱线排列绕纱，请点击上方“添加绕纱行”"
        :row-key="getRowKey"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column label="路号" min-width="140">
          <template #default="{ row }">
            <span v-if="isReadOnly">{{ row.code || '-' }}</span>
            <el-input
              v-else
              v-model="row.code"
              placeholder="路号（如 1）"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column label="绕纱表述" min-width="220">
          <template #default="{ row }">
            <span v-if="isReadOnly">{{ row.draftText || '-' }}</span>
            <el-input
              v-else
              v-model="row.draftText"
              placeholder="绕纱表述（如 25.1cm/1 花宽）"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column label="对针" min-width="160">
          <template #default="{ row }">
            <span v-if="isReadOnly">{{ row.borderStatus || '-' }}</span>
            <el-input
              v-else
              v-model="row.borderStatus"
              placeholder="对针（如 第一路对针）"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column label="描述" min-width="220">
          <template #default="{ row }">
            <span v-if="isReadOnly">{{ row.description || '-' }}</span>
            <el-input
              v-else
              v-model="row.description"
              placeholder="描述（如 开幅线）"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column
          v-if="!isReadOnly"
          label="操作"
          width="80"
          align="center"
          fixed="right"
        >
          <template #default="{ $index }">
            <el-button
              type="danger"
              link
              @click="handleRemoveYarnArrangeRow($index)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </el-form>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import type {
  SampleAnalysisVO,
  SampleAnalysisYarnSpecVO,
  SampleAnalysisYarnArrangeVO
} from '@/api/lunaerp-elmui/sample/sample-analysis'
import { getYarnSpecCascadeOptions, type YarnSpecCascadeOptionVO } from '@/api/lunaerp-elmui/production/yarn-master'
import SampleStitchBoard from './SampleStitchBoard.vue'

defineProps<{
  isReadOnly: boolean
}>()

const formData = defineModel<SampleAnalysisVO>({ required: true })

// 级联选项原始数据
const rawCascadeOptions = ref<YarnSpecCascadeOptionVO[]>([])

// 加载两级级联选项
const loadCascadeOptions = async () => {
  try {
    const res = await getYarnSpecCascadeOptions()
    rawCascadeOptions.value = res || []
  } catch (e) {
    console.error('加载纱线规格级联选项失败', e)
  }
}

onMounted(() => {
  loadCascadeOptions()
})

// 为行初始化 specCascadeValue
const initCascadeValues = () => {
  if (!formData.value.yarnSpecs || !formData.value.yarnSpecs.length) return
  for (const item of formData.value.yarnSpecs) {
    if (item.yarnSpecId && !item.specCascadeValue) {
      if (item.yarnTypeId) {
        item.specCascadeValue = [item.yarnTypeId, item.yarnSpecId]
      } else {
        for (const t of rawCascadeOptions.value) {
          if (t.specs?.some((s) => s.id === item.yarnSpecId)) {
            item.yarnTypeId = t.yarnTypeId
            item.yarnTypeName = t.yarnTypeName
            item.specCascadeValue = [t.yarnTypeId, item.yarnSpecId]
            break
          }
        }
      }
    }
  }
}

watch(
  [() => formData.value.yarnSpecs, rawCascadeOptions],
  () => {
    initCascadeValues()
  },
  { deep: true, immediate: true }
)

// 转换级联选项，并为已停用/软删的历史快照提供兜底渲染节点
const cascaderOptions = computed(() => {
  const options = rawCascadeOptions.value.map((type) => ({
    value: type.yarnTypeId,
    label: type.yarnTypeName,
    children: (type.specs || []).map((spec) => ({
      value: spec.id,
      label: spec.specName,
      rawSpec: spec,
      typeId: type.yarnTypeId,
      typeName: type.yarnTypeName
    }))
  }))

  for (const row of formData.value.yarnSpecs || []) {
    if (!row.yarnSpecId) continue
    const targetTypeId = row.yarnTypeId || 0
    let foundType = options.find((t) => t.value === targetTypeId)
    if (!foundType && targetTypeId) {
      foundType = {
        value: targetTypeId,
        label: row.yarnTypeName || `材质 #${targetTypeId}`,
        children: []
      }
      options.push(foundType)
    }
    if (foundType) {
      const foundSpec = foundType.children.find((s) => s.value === row.yarnSpecId)
      if (!foundSpec) {
        foundType.children.push({
          value: row.yarnSpecId,
          label: row.specName ? `${row.specName} (已停用/历史)` : `规格 #${row.yarnSpecId} (历史)`,
          rawSpec: {
            id: row.yarnSpecId,
            specName: row.specName || '',
            standardName: row.specName || ''
          } as any,
          typeId: targetTypeId,
          typeName: row.yarnTypeName || foundType.label
        })
      }
    }
  }
  return options
})

// 规格级联选择事件
const handleSpecCascadeChange = (row: SampleAnalysisYarnSpecVO, val: any) => {
  if (!val || val.length < 2) {
    row.yarnTypeId = null
    row.yarnSpecId = null
    row.yarnTypeName = ''
    row.specName = ''
    return
  }
  const [typeId, specId] = val as [number, number]
  row.yarnTypeId = typeId
  row.yarnSpecId = specId

  const typeOpt = rawCascadeOptions.value.find((t) => t.yarnTypeId === typeId)
  if (typeOpt) {
    row.yarnTypeName = typeOpt.yarnTypeName
    const specOpt = (typeOpt.specs || []).find((s) => s.id === specId)
    if (specOpt) {
      row.specName = specOpt.specName
      if (specOpt.spinMethod) {
        row.spinMethod = specOpt.spinMethod
      }
      if (specOpt.refPrice != null) {
        row.unitPrice = specOpt.refPrice
      }
    }
  }
}

// 增行
const handleAddYarnSpecRow = () => {
  if (!formData.value.yarnSpecs) {
    formData.value.yarnSpecs = []
  }
  formData.value.yarnSpecs.push({
    yarnSpecId: null,
    yarnTypeId: null,
    yarnTypeName: '',
    specName: '',
    spinMethod: '',
    ratio: null,
    unitPrice: null,
    remark: '',
    sortOrder: formData.value.yarnSpecs.length + 1
  })
}

// 删行
const handleRemoveYarnSpecRow = (index: number) => {
  formData.value.yarnSpecs?.splice(index, 1)
  formData.value.yarnSpecs?.forEach((row, idx) => {
    row.sortOrder = idx + 1
  })
}

// 比例汇总计算
const ratioTotal = computed(() => {
  const specs = formData.value.yarnSpecs || []
  const sum = specs.reduce((acc, cur) => acc + (Number(cur.ratio) || 0), 0)
  return Math.round((sum + Number.EPSILON) * 100) / 100
})

const ratioTotalText = computed(() => ratioTotal.value.toFixed(2))

const ratioTagType = computed(() => {
  if (!formData.value.yarnSpecs?.length) return 'info'
  if (Math.abs(ratioTotal.value - 100) < 0.001) return 'success'
  return 'warning'
})

const ratioAlertMessage = computed(() => {
  if (!formData.value.yarnSpecs?.length) return ''
  if (Math.abs(ratioTotal.value - 100) < 0.001) return ''
  if (ratioTotal.value < 100) {
    const diff = (100 - ratioTotal.value).toFixed(2)
    return `用纱比例合计不足 100%（当前：${ratioTotalText.value}%，相差 ${diff}%），不影响保存，请按需核对。`
  }
  const over = (ratioTotal.value - 100).toFixed(2)
  return `用纱比例合计超标（当前：${ratioTotalText.value}%，超标 ${over}%），不影响保存，请按需核对。`
})

// 绕纱行增行
let nextArrangeKey = 1
const handleAddYarnArrangeRow = () => {
  if (!formData.value.yarnArranges) {
    formData.value.yarnArranges = []
  }
  formData.value.yarnArranges.push({
    _key: `temp_${Date.now()}_${nextArrangeKey++}`,
    code: '',
    draftText: '',
    borderStatus: '',
    description: ''
  })
}

// 绕纱行删行
const handleRemoveYarnArrangeRow = (index: number) => {
  formData.value.yarnArranges?.splice(index, 1)
}

const getRowKey = (row: SampleAnalysisYarnArrangeVO): string => {
  return String(row.id ?? row._key ?? '')
}

// 校验子表有效性：规格必选，比例 > 0
const validate = (): boolean => {
  const specs = formData.value.yarnSpecs || []
  for (let i = 0; i < specs.length; i++) {
    const row = specs[i]
    if (!row.yarnSpecId) {
      ElMessage.error(`纱线规格明细第 ${i + 1} 行：必须选择纱线规格`)
      return false
    }
    const ratioNum = Number(row.ratio)
    if (row.ratio === null || row.ratio === undefined || isNaN(ratioNum) || ratioNum <= 0) {
      ElMessage.error(`纱线规格明细第 ${i + 1} 行：用纱比例必须大于 0`)
      return false
    }
  }
  return true
}

defineExpose({
  validate
})
</script>
