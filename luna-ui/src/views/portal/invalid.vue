<template>
  <div class="portal-invalid-wrapper">
    <div class="portal-invalid-container">
      <el-result icon="warning" title="链接已失效" sub-title="访问凭证不存在、已过期或已被吊销。">
        <template #extra>
          <div class="portal-invalid-card">
            <p class="tip-line"> 请联系您的销售经理或业务员，重新获取客户门户专属访问链接。 </p>
            <p class="tip-sub"> 若您已收到新的链接，请直接在微信或浏览器中点击新链接进入。 </p>
          </div>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PortalInvalid' })
</script>

<style scoped>
.portal-invalid-wrapper {
  min-height: 100vh;
  background-color: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px;
}

.portal-invalid-container {
  width: 100%;
  max-width: 480px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  padding: 24px 16px;
}

.portal-invalid-card {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.tip-line {
  margin: 0 0 8px 0;
  color: #303133;
}

.tip-sub {
  margin: 0;
  font-size: 12px;
  color: #909399;
}
</style>
