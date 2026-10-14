<template>
  <div
    class="task-list-placeholder p-6 text-center bg-[var(--el-bg-color)] rounded-md border border-[var(--el-border-color-lighter)]"
  >
    <el-empty
      description="跨单机台任务列表（支持分页、按冲突/织机过滤与就地编辑）将在 Issue 08 落地"
    >
      <template #extra>
        <span class="text-xs text-gray-400">顶部筛选条件将与甘特图联动共享</span>
      </template>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
// 占位组件，Issue 08 落地跨单机台任务列表与编辑功能
</script>
