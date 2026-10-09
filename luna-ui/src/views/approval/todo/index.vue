<template>
  <ContentWrap>
    <div class="approval-todo-container">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span class="font-bold text-16px">待我审批工作台</span>
            <el-tag v-if="instanceId" type="primary">实例编号: {{ instanceId }}</el-tag>
          </div>
        </template>
        <div v-if="bizNo || instanceId" class="mb-20px">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="审批实例 ID">
              {{ instanceId || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="关联单号">
              {{ bizNo || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <el-empty
          v-if="!instanceId"
          description="暂无选中的审批任务，请从消息铃铛或审批中心进入"
        />
        <div v-else class="text-center text-gray-500 py-20px">
          审批工作台详情加载中（待审批引擎 feature 20 接线完成）
        </div>
      </el-card>
    </div>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineOptions({ name: 'ApprovalTodo' })

const route = useRoute()
const instanceId = computed(() => route.query.instanceId || route.query.id || '')
const bizNo = computed(() => route.query.bizNo || '')
</script>

<style lang="scss" scoped>
.approval-todo-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
