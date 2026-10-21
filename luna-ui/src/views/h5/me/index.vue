<template>
  <div class="h5-tab-page p-4 flex flex-col justify-between min-h-full">
    <div>
      <div
        class="bg-gradient-to-r from-blue-500 to-indigo-600 rounded-xl p-5 text-white shadow-sm mb-4"
      >
        <div class="flex items-center space-x-4">
          <div
            class="w-14 h-14 bg-white/20 rounded-full flex items-center justify-center text-24px"
          >
            <van-icon name="user-o" />
          </div>
          <div>
            <div class="text-18px font-bold">{{
              user.nickname || user.username || '车间员工'
            }}</div>
            <div class="text-12px text-white/80 mt-1">账号：{{ user.username || '—' }}</div>
          </div>
        </div>
      </div>

      <van-cell-group inset class="!mx-0 mb-4 shadow-sm">
        <van-cell title="用户角色" :value="rolesDisplay" />
        <van-cell title="车间权限">
          <template #value>
            <div class="flex flex-wrap gap-1 justify-end">
              <van-tag v-if="hasPerm(H5_PERMISSIONS.REPORT)" type="primary">报工</van-tag>
              <van-tag v-if="hasPerm(H5_PERMISSIONS.INSPECTION)" type="success">质检</van-tag>
              <van-tag v-if="hasPerm(H5_PERMISSIONS.DISPATCH)" type="warning">发纱</van-tag>
              <van-tag v-if="isSuper" type="danger">超管</van-tag>
            </div>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <div class="space-y-3 pt-6 pb-4">
      <van-button round block plain size="large" @click="handleGoPC"> 进入 PC 桌面端 </van-button>
      <van-button round block type="danger" size="large" @click="handleLogout">
        退出登录
      </van-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog } from 'vant'
import { useUserStoreWithOut } from '@/store/modules/user'
import { H5_PERMISSIONS, hasWorkshopPermission, hasSuperPermission } from '@/utils/h5Auth'

defineOptions({ name: 'H5Me' })

const router = useRouter()
const userStore = useUserStoreWithOut()

const user = computed(() => userStore.getUser)
const isSuper = computed(() => hasSuperPermission(userStore.permissions))

const rolesDisplay = computed(() => {
  const roles = userStore.getRoles || []
  if (roles.length === 0) return '普通用户'
  return roles
    .map((r) => {
      if (r === 'workshop_worker') return '车间工人'
      if (r === 'workshop_inspector') return '车间检验员'
      if (r === 'super_admin' || r === 'admin') return '系统管理员'
      return r
    })
    .join('、')
})

const hasPerm = (perm: string) => hasWorkshopPermission(userStore.permissions, perm)

const handleGoPC = () => {
  router.push('/index')
}

const handleLogout = () => {
  showConfirmDialog({
    title: '提示',
    message: '确定要退出当前账号登录吗？'
  })
    .then(async () => {
      await userStore.loginOut()
      router.replace('/h5/login')
    })
    .catch(() => {})
}
</script>
