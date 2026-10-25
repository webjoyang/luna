<template>
  <div class="h5-page-wrapper">
    <div class="h5-container">
      <div class="h5-content flex-1 flex flex-col min-h-0 overflow-y-auto">
        <router-view />
      </div>

      <van-tabbar
        v-if="visibleTabs.length > 0"
        route
        fixed
        placeholder
        safe-area-inset-bottom
        class="h5-tabbar"
      >
        <van-tabbar-item
          v-for="item in visibleTabs"
          :key="item.path"
          replace
          :to="item.path"
          :icon="item.icon"
        >
          {{ item.title }}
        </van-tabbar-item>
      </van-tabbar>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useUserStoreWithOut } from '@/store/modules/user'
import { getVisibleTabs } from '@/utils/h5Auth'

defineOptions({ name: 'H5Layout' })

const userStore = useUserStoreWithOut()
const visibleTabs = computed(() => getVisibleTabs(userStore.permissions))
</script>

<style lang="scss">
@use '@/styles/h5.scss';

.h5-content {
  box-sizing: border-box;
}

.h5-tabbar {
  max-width: 640px;
  left: 50% !important;
  transform: translateX(-50%);
}
</style>
