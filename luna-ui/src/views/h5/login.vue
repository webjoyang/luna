<template>
  <div class="h5-page-wrapper">
    <div class="h5-container flex flex-col justify-between p-6">
      <div class="h5-login-header text-center pt-8 pb-4">
        <div
          class="w-16 h-16 mx-auto mb-4 bg-blue-50 text-blue-600 rounded-2xl flex items-center justify-center shadow-sm"
        >
          <van-icon name="shop-o" size="36" />
        </div>
        <h1 class="text-20px font-bold text-gray-900 tracking-wide">车间移动工作台</h1>
        <p class="text-13px text-gray-500 mt-2">LunaERP 智能织造执行车间移动终端</p>
      </div>

      <div class="h5-login-form flex-1 flex flex-col justify-center max-w-sm w-full mx-auto">
        <van-form @submit="onSubmit">
          <van-cell-group inset class="!mx-0 shadow-sm border border-gray-100">
            <van-field
              v-model="form.username"
              name="username"
              label="账号"
              left-icon="user-o"
              placeholder="请输入员工账号"
              autocomplete="username"
              :rules="[{ required: true, message: '请填写账号' }]"
            />
            <van-field
              v-model="form.password"
              type="password"
              name="password"
              label="密码"
              left-icon="lock"
              placeholder="请输入登录密码"
              autocomplete="current-password"
              :rules="[{ required: true, message: '请填写密码' }]"
            />
          </van-cell-group>

          <div class="mt-8">
            <van-button
              round
              block
              type="primary"
              native-type="submit"
              :loading="loading"
              loading-text="登录中..."
              size="large"
            >
              登 录
            </van-button>
          </div>
        </van-form>

        <div class="mt-6 text-center text-12px text-gray-400">
          <span>登录态与 PC 端同源共享</span>
        </div>
      </div>

      <div class="h5-login-footer text-center pb-4 text-11px text-gray-400">
        LunaERP · 车间移动终端 (ADR-0045)
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { login } from '@/api/login'
import { getAccessToken, setToken } from '@/utils/auth'

defineOptions({ name: 'H5Login' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

onMounted(() => {
  // 若已具备有效登录态，直接跳往回跳目标或 /h5
  if (getAccessToken()) {
    const redirect = (route.query.redirect as string) || '/h5'
    router.replace(redirect)
  }
})

const onSubmit = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await login({
      username: form.username.trim(),
      password: form.password,
      captchaVerification: ''
    })
    if (res) {
      setToken(res)
      showSuccessToast('登录成功')
      const redirect = (route.query.redirect as string) || '/h5'
      await router.replace(redirect)
    }
  } catch (err: any) {
    const msg = err?.msg || err?.message || '登录失败，请检查账号密码'
    showFailToast(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
@use '@/styles/h5.scss';
</style>
