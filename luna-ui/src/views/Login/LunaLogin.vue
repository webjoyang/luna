<!--
  LunaLogin — 把 linaagi (Next.js) 登录页转写成 Vue 3 + yudao 规范的版本。
  - 视觉：保留 linaagi 光晕风（46 列色波 + 3 极光柔光 + LUNA 光球 + 标题呼吸 + 错误抖动）
  - 功能：账号登录（手机号+密码）对接 yudao /system/auth/login；扫码 / Passkey 仅留 UI 入口
  - 字段：租户名（VITE_APP_TENANT_ENABLE=true 时显示）、手机号（塞 yudao username 字段）、
          密码、记住我、滑块验证码（VITE_APP_CAPTCHA_ENABLE=true 时启用）
-->
<template>
  <div class="luna-login relative min-h-screen overflow-hidden bg-[#2f63cf] font-sans text-slate-900">
    <!-- 背景：46 列色波 + 极光柔光 -->
    <div class="login-aura pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
      <div class="absolute inset-0 flex">
        <div
          v-for="i in 46"
          :key="i"
          class="aura-col h-full flex-1"
          :style="{ animationDelay: `-${(((i - 1) / 46) * 10).toFixed(2)}s` }"
        ></div>
      </div>
      <div class="aura-glow aura-glow--cyan absolute inset-0 mix-blend-screen"></div>
      <div class="aura-glow aura-glow--violet absolute inset-0 mix-blend-screen"></div>
      <div class="aura-glow aura-glow--mint absolute inset-0 mix-blend-screen"></div>
      <div
        class="absolute inset-x-0 bottom-0 h-[58%]"
        :style="{
          background:
            'linear-gradient(180deg, transparent 0%, rgba(255,231,224,0.42) 55%, rgba(255,236,228,0.66) 100%)'
        }"
      ></div>
      <div class="absolute inset-x-0 top-0 h-28 bg-gradient-to-b from-black/15 to-transparent"></div>
    </div>

    <!-- 顶部品牌头 -->
    <header class="relative z-20 flex items-center justify-between px-6 py-5 sm:px-10">
      <div class="flex items-center text-white">
        <img alt="" class="mr-10px h-48px w-48px" src="@/assets/imgs/logo.png" />
        <span class="text-20px font-bold">{{ underlineToHump(appStore.getTitle) }}</span>
      </div>
    </header>

    <!-- 中部 -->
    <main class="relative z-10 flex min-h-[calc(100vh-80px)] flex-col items-center justify-center px-4 pb-12">
      <!-- Hero: LUNA 光球 + 标语 -->
      <div class="relative mb-9 flex flex-col items-center text-center sm:mb-11">
        <div class="luna-orb mb-[-1rem] sm:mb-[-1.5rem]" aria-hidden="true"></div>
        <span
          class="mb-6 inline-flex items-center gap-1.5 rounded-full border border-white/30 bg-white/15 px-4 py-1.5 shadow-sm backdrop-blur"
        >
          <span class="text-[11px] font-semibold uppercase tracking-[0.3em] text-white/90">LUNA</span>
        </span>
        <div class="relative">
          <h1 aria-hidden="true" class="login-title login-title--glow absolute inset-0 select-none">
            <span class="block text-[3rem] font-semibold leading-[1.05] tracking-tight sm:text-[5.5rem]">
              让智能 接管工作
            </span>
          </h1>
          <h1 class="login-title login-title--above relative">
            <span class="block text-[3rem] font-semibold leading-[1.05] tracking-tight sm:text-[5.5rem]">
              让智能 接管工作
            </span>
          </h1>
        </div>
        <p class="mt-6 max-w-md text-sm font-medium leading-relaxed text-white/80 sm:text-base">
          让 AI 驱动每一天的业务协作
        </p>
      </div>

      <!-- 登录控件区（去卡片化） -->
      <div class="group relative w-full max-w-[360px]">
        <div :class="{ 'animate-shake': state.status === 'error' }">
          <!-- 模式切换：账号登录 / 扫码验证 -->
          <div class="mb-6 flex gap-1 rounded-full border border-white/25 bg-white/15 p-1 backdrop-blur">
            <button
              type="button"
              class="flex flex-1 items-center justify-center gap-1.5 rounded-full py-2.5 text-xs font-semibold transition"
              :class="
                loginMode === 'password'
                  ? 'bg-white text-indigo-600 shadow-sm'
                  : 'text-white/70 hover:text-white'
              "
              @click="onSelectMode('password')"
            >
              <el-icon><Lock /></el-icon>
              账号登录
            </button>
            <button
              type="button"
              class="flex flex-1 items-center justify-center gap-1.5 rounded-full py-2.5 text-xs font-semibold transition"
              :class="
                loginMode === 'qr'
                  ? 'bg-white text-indigo-600 shadow-sm'
                  : 'text-white/70 hover:text-white'
              "
              @click="onSelectMode('qr')"
            >
              <Icon icon="mdi:qrcode-scan" :size="14" />
              扫码验证
            </button>
          </div>

          <!-- 表单（账号模式下显示） -->
          <LunaLoginForm
            v-show="loginMode === 'password'"
            ref="lunaLoginFormRef"
            :loading="state.loading"
            :error-message="state.errorMessage"
            @get-code="getCode"
            @submit="handleLogin"
          />

          <!-- 扫码占位 -->
          <div
            v-if="loginMode === 'qr'"
            class="rounded-2xl border border-white/30 bg-white/10 px-4 py-12 text-center text-xs text-white/80 backdrop-blur"
          >
            扫码登录暂未开通，请切回账号登录
          </div>

          <!-- Passkey 入口按钮 -->
          <button
            type="button"
            class="mt-4 flex w-full items-center justify-center gap-2 rounded-full border border-white/30 bg-white/10 px-4 py-2.5 text-xs font-semibold text-white/90 backdrop-blur transition hover:bg-white/20"
            @click="onPasskeyClick"
          >
            <Icon icon="mdi:fingerprint" :size="14" />
            使用 Passkey 登录
          </button>

          <!-- 底部安全说明 -->
          <div class="mt-5 flex items-center justify-center gap-1.5 text-[11px] text-white/70">
            <el-icon><CircleCheckFilled /></el-icon>
            LUNA · 已通过安全加密
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElLoading, ElMessage } from 'element-plus'
import { Lock, CircleCheckFilled } from '@element-plus/icons-vue'
import * as LoginApi from '@/api/login'
import * as authUtil from '@/utils/auth'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { underlineToHump } from '@/utils'
import LunaLoginForm from './components/LunaLoginForm.vue'

defineOptions({ name: 'LunaLogin' })

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const lunaLoginFormRef = ref<InstanceType<typeof LunaLoginForm>>()
const loginMode = ref<'password' | 'qr'>('password')
const redirect = ref('')

const state = reactive({
  loading: false,
  status: 'idle' as 'idle' | 'success' | 'error',
  errorMessage: null as string | null
})

// 模式切换：扫码 tab 仅弹提示，不真正切换
const onSelectMode = (mode: 'password' | 'qr') => {
  if (mode === 'qr') {
    ElMessage.info('扫码登录暂未开通，请使用账号登录')
    return
  }
  loginMode.value = mode
}

const onPasskeyClick = () => {
  ElMessage.info('Passkey 登录暂未开通，请使用账号登录')
}

// 用租户名换租户 ID（与 yudao 原 LoginForm 第 207-213 行一致）
const getTenantId = async () => {
  const loginData = lunaLoginFormRef.value?.loginData
  if (!loginData) return
  if (loginData.tenantEnable === 'true') {
    const res = await LoginApi.getTenantIdByName(loginData.tenantName)
    authUtil.setTenantId(res)
  }
}

// 按域名回填租户名 + 租户 ID（与 yudao 原 LoginForm 第 227-237 行一致）
const getTenantByWebsite = async () => {
  const loginData = lunaLoginFormRef.value?.loginData
  if (!loginData) return
  if (loginData.tenantEnable === 'true') {
    const website = location.host
    const res = await LoginApi.getTenantByWebsite(website)
    if (res) {
      loginData.tenantName = res.name
      authUtil.setTenantId(res.id)
    }
  }
}

// 记住我回填（与 yudao 原 LoginForm 第 214-226 行一致）
const getLoginFormCache = () => {
  const loginData = lunaLoginFormRef.value?.loginData
  if (!loginData) return
  const cached = authUtil.getLoginForm()
  if (cached) {
    if (cached.tenantName) loginData.tenantName = cached.tenantName
    if (cached.username) loginData.phone = cached.username
    if (cached.password) loginData.password = cached.password
    loginData.rememberMe = cached.rememberMe
  }
}

// 提交按钮入口：若开启验证码则弹滑块，否则直接登录（与 yudao 原 LoginForm 第 196-206 行一致）
const getCode = async () => {
  const loginData = lunaLoginFormRef.value?.loginData
  if (!loginData) return
  if (loginData.captchaEnable === 'false') {
    await handleLogin({})
  } else {
    lunaLoginFormRef.value?.verifyRef?.show()
  }
}

// 核心登录流程（与 yudao 原 LoginForm.handleLogin 第 240-278 行对齐）
const handleLogin = async (params: { captchaVerification?: string }) => {
  const loginData = lunaLoginFormRef.value?.loginData
  if (!loginData) return
  state.loading = true
  state.status = 'idle'
  state.errorMessage = null
  let loadingInstance: ReturnType<typeof ElLoading.service> | undefined
  try {
    await getTenantId()
    const valid = await lunaLoginFormRef.value?.formRef?.validate().catch(() => false)
    if (!valid) return
    const captchaVerification = params?.captchaVerification || ''
    const res = await LoginApi.login({
      username: loginData.phone, // 手机号塞 yudao 的 username 字段
      password: loginData.password,
      captchaVerification
    })
    if (!res) return
    loadingInstance = ElLoading.service({
      lock: true,
      text: '正在加载系统中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    if (loginData.rememberMe) {
      authUtil.setLoginForm({
        tenantName: loginData.tenantName,
        username: loginData.phone,
        password: loginData.password,
        rememberMe: loginData.rememberMe
      })
    } else {
      authUtil.removeLoginForm()
    }
    authUtil.setToken(res)
    // 登录后立即拉权限和用户信息（yudao 路由守卫也会兜底，这里主动拉一次更稳）
    await userStore.setUserInfoAction()
    if (!redirect.value) redirect.value = '/'
    await router.push({ path: redirect.value })
  } catch (err: any) {
    const msg = err?.message || ''
    if (
      msg.includes('Failed to fetch') ||
      msg.includes('ERR_NETWORK') ||
      msg.includes('NetworkError') ||
      msg.includes('Load failed')
    ) {
      state.errorMessage = '无法连接服务器，请检查网络连接或稍后再试'
    } else {
      state.errorMessage = msg || '登录失败'
    }
    state.status = 'error'
  } finally {
    state.loading = false
    loadingInstance?.close?.()
  }
}

watch(
  () => route,
  (r) => {
    redirect.value = (r?.query?.redirect as string) || ''
  },
  { immediate: true, deep: true }
)

onMounted(() => {
  getLoginFormCache()
  getTenantByWebsite()
})
</script>

<style lang="scss" scoped>
.luna-login {
  font-family:
    ui-sans-serif,
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    'Helvetica Neue',
    Arial,
    'Noto Sans',
    sans-serif;
}

.luna-orb {
  width: 150px;
  height: 150px;
  background: radial-gradient(
    circle at 30% 30%,
    #a5e8ff 0%,
    #5b9dff 35%,
    #2b4ab8 70%,
    transparent 100%
  );
  border-radius: 50%;
  box-shadow: 0 0 80px 20px rgb(120 180 255 / 50%);
  animation: orb-float 4s ease-in-out infinite;
}

@keyframes orb-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-8px);
  }
}

.login-title {
  margin: 0;
}

.login-title--above {
  color: rgb(255 255 255 / 95%);
  text-shadow: 0 2px 22px rgb(90 110 180 / 40%);
}

.login-title--glow {
  color: #fff;
  filter: blur(13px);
  animation: title-breathe 2.6s ease-in-out infinite;
  will-change: opacity, transform;
}

@keyframes title-breathe {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(1);
  }

  50% {
    opacity: 0.85;
    transform: scale(1.025);
  }
}

.animate-shake {
  animation: shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}

@keyframes shake {
  10%,
  90% {
    transform: translate3d(-1px, 0, 0);
  }

  20%,
  80% {
    transform: translate3d(2px, 0, 0);
  }

  30%,
  50%,
  70% {
    transform: translate3d(-4px, 0, 0);
  }

  40%,
  60% {
    transform: translate3d(4px, 0, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .luna-orb,
  .login-title--glow,
  .animate-shake {
    animation: none !important;
  }

  .login-title--glow {
    opacity: 0.6;
  }
}
</style>

<!-- 非 scoped：aura-col / aura-glow 的 keyframes（颜色值原样照搬 linaagi LoginAura.tsx） -->
<style lang="scss">
.luna-login .aura-col {
  background-color: #2f63cf;
  box-shadow:
    inset 1px 0 0 rgb(255 255 255 / 6%),
    inset -1px 0 0 rgb(8 16 50 / 7%);
  animation-duration: 10s;
  animation-iteration-count: infinite;
  animation-name: aura-flow;
  animation-timing-function: linear;
  will-change: background-color;
}

@keyframes aura-flow {
  0% {
    background-color: #2f63cf;
  }

  9% {
    background-color: #2e87d4;
  }

  18% {
    background-color: #1eb6cf;
  }

  26% {
    background-color: #5fd0d8;
  }

  32% {
    background-color: #aeece2;
  }

  39% {
    background-color: #4fc2cf;
  }

  50% {
    background-color: #3a64d8;
  }

  60% {
    background-color: #5147c8;
  }

  70% {
    background-color: #6f4ad2;
  }

  80% {
    background-color: #9150d4;
  }

  88% {
    background-color: #b265cf;
  }

  94% {
    background-color: #7a52cc;
  }

  100% {
    background-color: #2f63cf;
  }
}

.luna-login .aura-glow {
  filter: blur(48px);
  will-change: transform, opacity;
}

.luna-login .aura-glow--cyan {
  background: radial-gradient(42% 72% at 34% 28%, rgb(120 240 225 / 50%), transparent 70%);
  animation: aurora-a 15s ease-in-out infinite;
}

.luna-login .aura-glow--violet {
  background: radial-gradient(40% 66% at 72% 46%, rgb(168 122 240 / 46%), transparent 72%);
  animation: aurora-b 19s ease-in-out infinite;
}

.luna-login .aura-glow--mint {
  background: radial-gradient(34% 60% at 54% 60%, rgb(150 235 220 / 40%), transparent 74%);
  animation: aurora-c 23s ease-in-out infinite;
}

@keyframes aurora-a {
  0%,
  100% {
    opacity: 0.85;
    transform: translate(-5%, -3%) scale(1);
  }

  50% {
    opacity: 1;
    transform: translate(7%, 4%) scale(1.12);
  }
}

@keyframes aurora-b {
  0%,
  100% {
    opacity: 0.8;
    transform: translate(5%, 2%) scale(1.06);
  }

  50% {
    opacity: 1;
    transform: translate(-6%, -4%) scale(1);
  }
}

@keyframes aurora-c {
  0%,
  100% {
    opacity: 0.65;
    transform: translate(0, 5%) scale(1);
  }

  50% {
    opacity: 0.95;
    transform: translate(-4%, -5%) scale(1.1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .luna-login .aura-col,
  .luna-login .aura-glow {
    animation: none !important;
  }
}
</style>
