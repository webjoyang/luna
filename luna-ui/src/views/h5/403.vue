<template>
  <div class="h5-page-wrapper">
    <div class="h5-container flex flex-col justify-between p-6">
      <div class="pt-8"></div>

      <div class="flex-1 flex flex-col items-center justify-center">
        <van-empty image="error" description="无车间角色权限">
          <template #description>
            <div class="text-center px-4">
              <h2 class="text-16px font-bold text-gray-800">暂无车间移动端权限</h2>
              <p class="text-13px text-gray-500 mt-2 leading-relaxed">
                当前账号未分配车间工人（报工/发纱）或车间检验员权限，请联系系统管理员开通相关角色。
              </p>
            </div>
          </template>
        </van-empty>

        <div class="w-full max-w-xs space-y-3 mt-4">
          <van-button round block type="primary" size="large" @click="handleRelogin">
            切换账号登录
          </van-button>
          <van-button round block plain size="large" @click="handleGoPC">
            进入 PC 桌面端
          </van-button>
        </div>
      </div>

      <div class="text-center pb-4 text-11px text-gray-400">
        LunaERP · 车间移动终端 (ADR-0045)
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router'
import { resetRouter } from '@/router'
import { deleteUserCache } from '@/hooks/web/useCache'
import { removeToken } from '@/utils/auth'

defineOptions({ name: 'H5Forbidden' })

const router = useRouter()

const handleRelogin = () => {
  resetRouter()
  deleteUserCache()
  removeToken()
  router.replace('/h5/login')
}

const handleGoPC = () => {
  router.push('/index')
}
</script>

<style lang="scss">
@use '@/styles/h5.scss';
</style>
