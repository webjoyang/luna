<!--
  LunaLoginForm — 在 linaagi 光晕风视觉基础上重写的登录表单。
  字段集对齐 yudao 原 LoginForm.vue：租户名（条件渲染）/ 手机号 / 密码 / 记住我 / 滑块验证码。
  提交按钮、扫码/Passkey 入口由父组件 LunaLogin.vue 持有；本组件只负责表单字段、校验、Verify 实例。
-->
<template>
  <el-form
    ref="formRef"
    :model="loginData"
    :rules="rules"
    label-position="top"
    class="luna-login-form"
  >
    <el-form-item v-if="loginData.tenantEnable === 'true'" prop="tenantName">
      <el-input
        v-model="loginData.tenantName"
        :prefix-icon="iconHouse"
        placeholder="请输入租户名"
      />
    </el-form-item>

    <el-form-item prop="phone">
      <el-input
        v-model="loginData.phone"
        :prefix-icon="iconPhone"
        maxlength="11"
        type="tel"
        placeholder="请输入 11 位企业手机号"
      />
    </el-form-item>

    <el-form-item prop="password">
      <el-input
        v-model="loginData.password"
        :prefix-icon="iconLock"
        :type="loginData.showPassword ? 'text' : 'password'"
        placeholder="请输入您的登录密码"
        @keyup.enter="emit('get-code')"
      >
        <template #suffix>
          <el-icon class="cursor-pointer" @click="loginData.showPassword = !loginData.showPassword">
            <View v-if="!loginData.showPassword" />
            <Hide v-else />
          </el-icon>
        </template>
      </el-input>
    </el-form-item>

    <el-form-item>
      <el-row justify="space-between" style="width: 100%">
        <el-col :span="12">
          <el-checkbox v-model="loginData.rememberMe">记住我</el-checkbox>
        </el-col>
      </el-row>
    </el-form-item>

    <div v-if="errorMessage" class="luna-error">
      <el-icon><CircleClose /></el-icon>
      <span>{{ errorMessage }}</span>
    </div>

    <el-button :loading="loading" class="luna-submit" @click="emit('get-code')">
      登录系统
      <el-icon class="ml-5px"><ArrowRight /></el-icon>
    </el-button>

    <Verify
      v-if="loginData.captchaEnable === 'true'"
      ref="verifyRef"
      :captcha-type="captchaType"
      :img-size="{ width: '400px', height: '200px' }"
      mode="pop"
      @success="(payload: any) => emit('submit', payload)"
    />
  </el-form>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { View, Hide, ArrowRight, CircleClose } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useIcon } from '@/hooks/web/useIcon'
import { Verify } from '@/components/Verifition'

defineOptions({ name: 'LunaLoginForm' })

defineProps<{
  loading: boolean
  errorMessage: string | null
}>()

const emit = defineEmits<{
  (e: 'get-code'): void
  (e: 'submit', payload: { captchaVerification: string }): void
}>()

const iconHouse = useIcon({ icon: 'ep:house' })
const iconPhone = useIcon({ icon: 'ep:iphone' })
const iconLock = useIcon({ icon: 'ep:lock' })

const formRef = ref<FormInstance>()
const verifyRef = ref()
const captchaType = ref('blockPuzzle') // blockPuzzle 滑块 clickWord 点击文字 pictureWord 文字验证码

const loginData = reactive({
  tenantName: import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '',
  phone: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
  password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
  showPassword: false,
  rememberMe: true,
  tenantEnable: import.meta.env.VITE_APP_TENANT_ENABLE,
  captchaEnable: import.meta.env.VITE_APP_CAPTCHA_ENABLE
})

const rules: FormRules = {
  tenantName: [{ required: true, message: '请输入租户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入 11 位手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

defineExpose({ formRef, verifyRef, loginData })
</script>

<style lang="scss" scoped>
.luna-login-form {
  :deep(.el-input__wrapper) {
    background: rgb(255 255 255 / 65%);
    border: 1px solid rgb(255 255 255 / 50%);
    border-radius: 9999px;
    box-shadow: 0 1px 2px rgb(0 0 0 / 5%);
    backdrop-filter: blur(8px);
  }

  :deep(.el-input__wrapper.is-focus) {
    background: rgb(255 255 255 / 85%);
    border-color: rgb(255 255 255 / 100%);
    box-shadow: 0 0 0 4px rgb(255 255 255 / 30%);
  }

  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-form-item__error) {
    padding-left: 16px;
  }

  .luna-submit {
    width: 100%;
    height: 56px;
    margin-top: 8px;
    font-size: 16px;
    font-weight: 700;
    color: #1e293b;
    background: #fff;
    border: none;
    border-radius: 9999px;
    box-shadow: 0 18px 50px -12px rgb(80 90 200 / 60%);
  }

  .luna-submit:hover {
    transform: translateY(-2px);
    box-shadow: 0 22px 60px -12px rgb(80 90 200 / 70%);
  }

  .luna-error {
    display: flex;
    padding: 10px 16px;
    margin-bottom: 12px;
    font-size: 12px;
    color: #f43f5e;
    background: #ffe4e6;
    border-radius: 1rem;
    align-items: center;
    gap: 8px;
  }
}
</style>
