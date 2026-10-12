<template>
  <div class="portal-invalid-container">
    <el-result
      icon="warning"
      title="链接已失效"
      sub-title="链接已失效，请联系业务员重新获取"
    >
      <template #extra>
        <p class="tip-text">
          若您需要查看发货单或对账单，请向相关业务人员索取最新的客户门户链接。
        </p>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
// 门户链接失效引导页
</script>

<style scoped>
.portal-invalid-container {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 24px 16px;
  box-sizing: border-box;
}

.tip-text {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin-top: 12px;
  text-align: center;
  max-width: 320px;
}
</style>
