<template>
  <el-form
    ref="formRef"
    :model="formData"
    label-width="110px"
    :disabled="isReadOnly"
  >
    <!-- 月结收费管理卡片（Issue 02） -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold text-14px">月结收费设置</span>
          <el-tag :type="formData.isPaid ? 'success' : 'danger'" size="small">
            {{ formData.isPaid ? '已付款' : '未付款' }}
          </el-tag>
        </div>
      </template>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="月结单价(元)" prop="unitPrice">
            <el-input-number
              v-model="formData.unitPrice"
              :precision="2"
              :step="5"
              :min="0"
              placeholder="默认 30.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="付款状态">
            <div class="flex items-center gap-3">
              <el-tag :type="formData.isPaid ? 'success' : 'danger'" size="default">
                {{ formData.isPaid ? '已付款' : '未付款' }}
              </el-tag>
              <span class="text-12px text-gray-400">
                （新建缺省未付，有收费权限人员可在工作台顶部或列表批量标记）
              </span>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-card>

    <!-- 内部成本核算（Issue 03） -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold text-14px">内部成本核算</span>
          <el-tag type="primary" size="small">
            权威重算口径
          </el-tag>
        </div>
      </template>

      <el-row :gutter="24">
        <el-col :span="6">
          <el-form-item label="生产工费" prop="productionCost">
            <el-input-number
              v-model="formData.productionCost"
              :precision="2"
              :step="0.5"
              :min="0"
              placeholder="0.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="染整费" prop="dyeingCost">
            <el-input-number
              v-model="formData.dyeingCost"
              :precision="2"
              :step="0.5"
              :min="0"
              placeholder="0.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="其他费用" prop="otherCost">
            <el-input-number
              v-model="formData.otherCost"
              :precision="2"
              :step="0.5"
              :min="0"
              placeholder="0.00"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="损耗率(%)" prop="lossRate">
            <el-input-number
              v-model="formData.lossRate"
              :precision="2"
              :step="1"
              :min="0"
              :max="99.99"
              placeholder="如 5 表示 5%"
              class="!w-full"
              controls-position="right"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 损耗率非法警告 -->
      <el-alert
        v-if="!costPreview.isLossRateValid"
        :title="costPreview.errorMessage || '损耗率取值非法（必须 ≥ 0 且 < 100）'"
        type="error"
        show-icon
        :closable="false"
        class="mb-16px"
      />

      <!-- 实时预览与核算汇总面板 -->
      <div class="bg-gray-50 dark:bg-gray-800 p-16px rounded-4px border border-gray-100 dark:border-gray-700">
        <div class="text-13px font-bold text-gray-700 dark:text-gray-200 mb-12px flex items-center justify-between">
          <span>成本测算汇总（实时预览）</span>
          <span class="text-12px font-normal text-gray-400">
            公式：总成本 = (纱线成本 + 生产工费 + 染整费 + 其他费用) ÷ (1 − 损耗率 ÷ 100)
          </span>
        </div>

        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="纱线成本合计">
            <span class="font-medium text-14px text-blue-600">
              ¥ {{ costPreview.yarnCost.toFixed(2) }}
            </span>
            <span class="text-12px text-gray-400 ml-1">/KG</span>
          </el-descriptions-item>

          <el-descriptions-item label="加工费用合计">
            <span>
              ¥ {{ (costPreview.productionCost + costPreview.dyeingCost + costPreview.otherCost).toFixed(2) }}
            </span>
            <span class="text-12px text-gray-400 ml-1">/KG</span>
          </el-descriptions-item>

          <el-descriptions-item label="适用损耗率">
            <span>{{ costPreview.lossRate }}%</span>
          </el-descriptions-item>

          <el-descriptions-item label="预估内部总成本">
            <span v-if="costPreview.totalCost != null" class="font-bold text-16px text-orange-600">
              ¥ {{ costPreview.totalCost.toFixed(2) }}
              <span class="text-12px text-gray-400 ml-1 font-normal">/KG</span>
            </span>
            <span v-else class="text-red-500 font-medium">损耗率非法</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 服务端权威核算结果覆盖提示 -->
        <div v-if="formData.totalCost != null" class="mt-12px flex items-center gap-2 text-13px">
          <el-tag type="success" size="small">落库权威值</el-tag>
          <span class="text-gray-600 dark:text-gray-300">
            服务端权威核算总成本为：
            <strong class="text-15px text-green-600 font-mono">¥ {{ formData.totalCost.toFixed(2) }} /KG</strong>
          </span>
          <span v-if="costPreview.totalCost != null && Math.abs(costPreview.totalCost - formData.totalCost) > 0.01" class="text-12px text-orange-500">
            （检测到当前表单有未保存的调整，点击顶部保存后重新落库权威计算）
          </span>
        </div>
      </div>
    </el-card>

    <!-- 供应商报价登记与对外报价占位卡片（票 06） -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold text-14px">供应商报价登记与对外报价</span>
          <el-tag type="info" size="small">功能待后续上线</el-tag>
        </div>
      </template>
      <div class="py-20px text-center text-gray-400">
        <Icon icon="ep:money" class="text-32px mb-8px" />
        <div>供应商报价登记与对外报价（票 06）将在后续票中开放</div>
      </div>
    </el-card>
  </el-form>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { SampleAnalysisVO } from '@/api/lunaerp-elmui/sample/sample-analysis'
import { calculateSampleCost, validateLossRate } from './sample-cost-calculator'

defineProps<{
  isReadOnly: boolean
}>()

const formData = defineModel<SampleAnalysisVO>({ required: true })

// 实时计算成本预览
const costPreview = computed(() => {
  return calculateSampleCost(formData.value.yarnSpecs, formData.value)
})

// 表单校验
const validate = (): boolean => {
  const check = validateLossRate(formData.value.lossRate)
  if (!check.isValid) {
    ElMessage.error(check.message || '损耗率取值非法（必须 ≥ 0 且 < 100）')
    return false
  }
  return true
}

defineExpose({
  validate
})
</script>
