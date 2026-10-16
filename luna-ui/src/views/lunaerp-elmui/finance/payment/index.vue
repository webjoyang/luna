<template>
  <ContentWrap>
    <el-tabs v-model="activeTab" class="payment-tabs" type="card">
      <!-- Tab 1: 付款申请 -->
      <el-tab-pane label="付款申请" name="apply">
        <template #label>
          <span class="flex items-center">
            <Icon icon="ep:document" class="mr-4px" /> 付款申请
          </span>
        </template>
        <PaymentApplyTab @switch-to-register="handleSwitchToRegister" />
      </el-tab-pane>

      <!-- Tab 2: 付款登记 -->
      <el-tab-pane label="付款登记" name="register">
        <template #label>
          <span class="flex items-center"> <Icon icon="ep:coin" class="mr-4px" /> 付款登记 </span>
        </template>
        <PaymentRegisterTab ref="registerTabRef" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import type { PaymentApplyRespVO } from '@/api/lunaerp-elmui/finance/payment'
import PaymentApplyTab from './components/PaymentApplyTab.vue'
import PaymentRegisterTab from './components/PaymentRegisterTab.vue'

defineOptions({ name: 'LunaerpFinancePayment' })

const activeTab = ref<'apply' | 'register'>('apply')
const registerTabRef = ref<InstanceType<typeof PaymentRegisterTab>>()

const handleSwitchToRegister = (row: PaymentApplyRespVO) => {
  activeTab.value = 'register'
  registerTabRef.value?.selectAndFocusApply(row)
}
</script>

<style scoped>
.payment-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.payment-tabs :deep(.el-tabs__item) {
  font-weight: 500;
  font-size: 14px;
}
.payment-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 700;
}
</style>
