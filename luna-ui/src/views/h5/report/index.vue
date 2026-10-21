<template>
  <div class="h5-tab-page p-4">
    <div class="header mb-4">
      <h1 class="text-18px font-bold text-gray-900">车间报工</h1>
      <p class="text-12px text-gray-500 mt-1">挡车工机台任务与布票报工</p>
    </div>

    <div class="bg-blue-50 border border-blue-100 rounded-lg p-4 mb-4">
      <div class="flex items-center space-x-3">
        <div class="w-10 h-10 bg-blue-500 text-white rounded-full flex items-center justify-center">
          <van-icon name="records-o" size="20" />
        </div>
        <div>
          <div class="text-14px font-bold text-blue-900">移动报工台已就绪</div>
          <div class="text-12px text-blue-600 mt-0.5">待 09/10 号 Ticket 接入任务列表与大扫码</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'H5Report' })
</script>
