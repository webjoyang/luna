<template>
  <el-form
    ref="formRef"
    :model="formData"
    label-width="110px"
    :disabled="isReadOnly"
  >
    <!-- 成本核算与报价占位卡片 -->
    <el-card shadow="never" class="mb-16px">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-bold text-14px">内部成本核算与对外报价</span>
          <el-tag type="info" size="small">功能待后续上线</el-tag>
        </div>
      </template>
      <div class="py-30px text-center text-gray-400">
        <Icon icon="ep:money" class="text-36px mb-10px" />
        <div class="text-15px font-medium mb-6px">成本核算、供应商报价登记与对外报价功能将在后续票中开放</div>
        <div class="text-12px text-gray-400">
          包含：生产工费、染整费、其他费用、损耗率与服务端权威总成本计算（票 03）、月结收费（票 02）、供应商报价登记与对外报价（票 06）
        </div>
      </div>
    </el-card>
  </el-form>
</template>

<script lang="ts" setup>
import type { SampleAnalysisVO } from '@/api/lunaerp-elmui/sample/sample-analysis'

defineProps<{
  isReadOnly: boolean
}>()

const formData = defineModel<SampleAnalysisVO>({ required: true })
</script>
