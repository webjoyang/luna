<template>
  <div class="h5-tab-page p-4">
    <div class="header mb-4">
      <h1 class="text-18px font-bold text-gray-900">移动质检</h1>
      <p class="text-12px text-gray-500 mt-1">车间成品/胚布质量检验与等级录入</p>
    </div>

    <div class="bg-green-50 border border-green-100 rounded-lg p-4 mb-4">
      <div class="flex items-center space-x-3">
        <div
          class="w-10 h-10 bg-green-500 text-white rounded-full flex items-center justify-center"
        >
          <van-icon name="shield-o" size="20" />
        </div>
        <div>
          <div class="text-14px font-bold text-green-900">移动质检台已就绪</div>
          <div class="text-12px text-green-600 mt-0.5">待 13/15 号 Ticket 接入录检单与扫码录检</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'H5Inspection' })
</script>
