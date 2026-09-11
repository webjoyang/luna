<template>
  <!--
    LunaLogin — LUNA 平台员工登录页（React→Vue3 迁移版）
    视觉/交互：迁移自 oldweb/app/auth/login/page.tsx（参考 https://www.lunaagi.cn/auth/login）
    动画：motion-v（Motion Vue）对应 React 版的 framer-motion
    登录逻辑：复用 luna-ui 现有 Login.vue 的逻辑（LoginApi.login + authUtil + permissionStore）
    光球：thinking-orb-vue（已 vendored 到 src/components/thinking-orb）
  -->
  <div
    class="relative min-h-screen w-full overflow-hidden bg-[#f5f8ff] font-sans text-slate-900 antialiased"
    @mousemove="handleMouseMove"
  >
    <!-- 签名背景 -->
    <LoginAura />

    <!-- 顶部导航：品牌字标 -->
    <header class="relative z-20 flex items-center px-6 py-5 sm:px-10">
      <LoginLogo />
    </header>

    <!-- 主体：中央品牌视觉 + 登录玻璃卡 -->
    <main
      class="relative z-10 flex min-h-[calc(100vh-80px)] flex-col items-center justify-center px-4 pb-12"
    >
      <!-- 品牌视觉：光球 + 标语 -->
      <motion.div
        :initial="{ opacity: 0, y: 16 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.7, ease: [0.16, 1, 0.3, 1] }"
      >
        <LoginHero :logo-url="brand.logoUrl" :brand-name-en="brand.nameEn" />
      </motion.div>

      <!-- 登录玻璃卡 -->
      <motion.div
        ref="cardRef"
        :initial="{ opacity: 0, y: 24 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{ duration: 0.8, delay: 0.1, ease: [0.16, 1, 0.3, 1] }"
        class="group relative w-full max-w-[360px]"
      >
        <!-- 鼠标柔光（hover 时显隐） -->
        <div
          class="pointer-events-none absolute -inset-px rounded-[2rem] opacity-0 transition-opacity duration-500 group-hover:opacity-100"
          :style="glowStyle"
        />

        <!-- 登录卡片内容 -->
        <div class="relative">
          <h2 class="mb-6 text-lg font-semibold text-white drop-shadow-[0_1px_8px_rgba(0,0,0,0.3)]">
            登入系统
          </h2>

          <!-- 账号密码登录（Element Plus，对齐 LoginForm.vue） -->
          <el-form
            ref="formLogin"
            :model="loginForm"
            :rules="loginRules"
            class="luna-login-form"
            size="large"
            @submit.prevent="handleLogin"
          >
            <el-form-item v-if="tenantEnable === 'true'" prop="tenantName">
              <el-input
                v-model.trim="loginForm.tenantName"
                :prefix-icon="iconHouse"
                placeholder="请输入租户名称"
                clearable
              />
            </el-form-item>

            <el-form-item prop="username">
              <el-input
                v-model.trim="loginForm.username"
                :prefix-icon="iconAvatar"
                placeholder="请输入用户名"
                clearable
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model.trim="loginForm.password"
                :prefix-icon="iconLock"
                type="password"
                show-password
                placeholder="请输入登录密码"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item class="luna-login-form__meta">
              <div class="flex w-full items-center justify-between">
                <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
                <el-link :underline="false" class="luna-login-form__forgot">忘记密码？</el-link>
              </div>
            </el-form-item>

            <!-- 错误提示（高度过渡） -->
            <AnimatePresence>
              <motion.div
                v-if="errorMessage"
                key="err"
                :initial="{ opacity: 0, height: 0 }"
                :animate="{ opacity: 1, height: 'auto' }"
                :exit="{ opacity: 0, height: 0 }"
                class="overflow-hidden"
              >
                <div class="mb-2 rounded-2xl bg-rose-50 px-4 py-2.5 text-xs font-medium text-rose-500">
                  {{ errorMessage }}
                </div>
              </motion.div>
            </AnimatePresence>

            <el-form-item class="luna-login-form__submit">
              <el-button
                class="luna-login-form__btn"
                :loading="loginLoading"
                native-type="submit"
              >
                登录系统
                <Icon v-if="!loginLoading" icon="ep:right" class="ml-1" />
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 安全提示 -->
          <div
            class="mt-6 flex items-center justify-center gap-1.5 text-[11px] text-white/70 drop-shadow-[0_1px_6px_rgba(0,0,0,0.25)]"
          >
            <svg class="h-3.5 w-3.5 text-emerald-200" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z" />
              <path d="m9 12 2 2 4-4" />
            </svg>
            仅限授权人员访问 · 已通过安全加密
          </div>
        </div>

        <!-- 验证码弹窗（如开启） -->
        <Verify
          v-if="captchaEnable"
          ref="verifyRef"
          :captcha-type="captchaType"
          :img-size="{ width: '400px', height: '200px' }"
          mode="pop"
          @success="handleLogin"
        />
      </motion.div>
    </main>
  </div>
</template>

<script lang="ts" setup>
/**
 * 登录逻辑复用 luna-ui 现有 Login.vue 的 LoginForm.vue 逻辑：
 *  - LoginApi.login() 调用系统鉴权
 *  - authUtil.setToken() / setLoginForm() 管理本地凭证
 *  - permissionStore.addRouters[0].path 作为默认跳转目标
 *  - Verify 组件处理验证码（如后端开启）
 * 视觉/交互迁移自 oldweb/app/auth/login/page.tsx。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElLoading } from 'element-plus'
import { motion, AnimatePresence } from 'motion-v'
import type { FormInstance, FormRules } from 'element-plus'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

import LoginAura from './components/LoginAura.vue'
import LoginHero from './components/LoginHero.vue'
import LoginLogo from './components/LoginLogo.vue'
import { Verify } from '@/components/Verifition'
import { useIcon } from '@/hooks/web/useIcon'
import * as authUtil from '@/utils/auth'
import { usePermissionStore } from '@/store/modules/permission'
import * as LoginApi from '@/api/login'

defineOptions({ name: 'LunaLogin' })

const { currentRoute, push } = useRouter()
const permissionStore = usePermissionStore()
const iconHouse = useIcon({ icon: 'ep:house' })
const iconAvatar = useIcon({ icon: 'ep:avatar' })
const iconLock = useIcon({ icon: 'ep:lock' })

// ====== 品牌配置（占位：未接入 cloud brand API，先使用默认值） ======
const brand = reactive({
  name: 'LUNAAGI',
  nameEn: 'LUNAAGI',
  logoUrl: ''
})

// ====== 表单状态 ======
const formLogin = ref<FormInstance>()
const tenantEnable = import.meta.env.VITE_APP_TENANT_ENABLE
const captchaEnable = import.meta.env.VITE_APP_CAPTCHA_ENABLE
const loginForm = reactive({
  tenantName: import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '',
  username: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
  password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
  captchaVerification: '',
  rememberMe: true
})
const loginRules: FormRules = {
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const loginLoading = ref(false)
const errorMessage = ref<string | null>(null)
const redirect = ref<string>('')
const verifyRef = ref()
const captchaType = ref('blockPuzzle') // blockPuzzle 滑块 clickWord 点击文字 pictureWord 文字验证码

// ====== 鼠标柔光（对应 React 版 useMotionValue + useMotionTemplate） ======
const cardRef = ref<HTMLDivElement | null>(null)
const mouseX = ref(0)
const mouseY = ref(0)

function handleMouseMove(evt: MouseEvent) {
  const host = cardRef.value
  if (!host) return
  const rect = host.getBoundingClientRect()
  mouseX.value = evt.clientX - rect.left
  mouseY.value = evt.clientY - rect.top
}
const glowStyle = computed(() => ({
  background: `radial-gradient(420px circle at ${mouseX.value}px ${mouseY.value}px, rgba(99,102,241,0.10), transparent 70%)`
}))

// ====== 登录逻辑（对齐 LoginForm.vue） ======
async function getTenantId() {
  if (tenantEnable === 'true') {
    const res = await LoginApi.getTenantIdByName(loginForm.tenantName)
    authUtil.setTenantId(res)
  }
}

async function getTenantByWebsite() {
  if (tenantEnable === 'true') {
    const res = await LoginApi.getTenantByWebsite(location.host)
    if (res) {
      loginForm.tenantName = res.name
      authUtil.setTenantId(res.id)
    }
  }
}

async function validForm(): Promise<typeof loginForm | undefined> {
  if (!formLogin.value) return
  try {
    await formLogin.value.validate()
    return loginForm
  } catch {
    return
  }
}

async function handleLogin(params?: any) {
  loginLoading.value = true
  errorMessage.value = null
  try {
    // 情况一：未开启验证码 → 直接登录；情况二：已开启 → 验证码通过后回调
    if (captchaEnable && captchaEnable !== 'false' && !params?.captchaVerification) {
      verifyRef.value?.show()
      return
    }

    await getTenantId()

    const data = await validForm()
    if (!data) return

    const submitForm = { ...loginForm }
    submitForm.captchaVerification = params?.captchaVerification || ''

    const res = await LoginApi.login(submitForm)
    if (!res) {
      errorMessage.value = '登录失败'
      return
    }

    const loading = ElLoading.service({
      lock: true,
      text: '正在加载系统中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    try {
      if (loginForm.rememberMe) {
        authUtil.setLoginForm({
          username: loginForm.username,
          password: loginForm.password,
          rememberMe: loginForm.rememberMe,
          tenantName: loginForm.tenantName
        })
      } else {
        authUtil.removeLoginForm()
      }
      authUtil.setToken(res)

      if (!redirect.value) redirect.value = '/index'

      // SSO 跳转判断
      if (redirect.value.indexOf('sso') !== -1) {
        window.location.href = window.location.href.replace('/luna-login?redirect=', '')
      } else {
        await push({ path: redirect.value || permissionStore.addRouters[0]?.path || '/index' })
      }
    } finally {
      loading.close()
    }
  } catch (err: any) {
    const msg = err?.message || '登录时发生错误'
    errorMessage.value = msg
  } finally {
    loginLoading.value = false
  }
}

// 监听路由参数，获取 redirect
watch(
  () => currentRoute.value,
  (route: RouteLocationNormalizedLoaded) => {
    redirect.value = route?.query?.redirect as string
  },
  { immediate: true }
)

onMounted(() => {
  // 恢复记住的登录信息
  const cache = authUtil.getLoginForm()
  if (cache) {
    loginForm.username = cache.username || loginForm.username
    loginForm.password = cache.password || loginForm.password
    loginForm.rememberMe = cache.rememberMe
    loginForm.tenantName = cache.tenantName || loginForm.tenantName
  }
  getTenantByWebsite()
})
</script>

<style scoped lang="scss">
.luna-login-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-form-item__error) {
    padding-left: 18px;
    color: #fecaca;
    text-shadow: 0 1px 6px rgba(0, 0, 0, 0.35);
  }

  :deep(.el-input__wrapper) {
    border-radius: 9999px;
    padding: 4px 16px;
    background: rgba(255, 255, 255, 0.65);
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.5) inset;
    backdrop-filter: blur(8px);
    transition:
      background 0.2s ease,
      box-shadow 0.2s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.75);
    }

    &.is-focus {
      background: rgba(255, 255, 255, 0.85);
      box-shadow:
        0 0 0 1px #fff inset,
        0 0 0 4px rgba(255, 255, 255, 0.3);
    }
  }

  :deep(.el-input__inner) {
    font-size: 14px;
    font-weight: 500;
    color: #1e293b;

    &::placeholder {
      color: #94a3b8;
    }
  }

  :deep(.el-input__prefix),
  :deep(.el-input__suffix) {
    color: #94a3b8;
  }

  &__meta {
    margin-bottom: 8px;

    :deep(.el-form-item__content) {
      line-height: 1.2;
    }

    :deep(.el-checkbox__label) {
      font-size: 12px;
      font-weight: 500;
      color: rgba(255, 255, 255, 0.8);
      text-shadow: 0 1px 6px rgba(0, 0, 0, 0.25);
    }

    :deep(.el-checkbox__inner) {
      border-color: rgba(255, 255, 255, 0.7);
      background: rgba(255, 255, 255, 0.25);
    }

    :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
      border-color: #818cf8;
      background: #818cf8;
    }
  }

  &__forgot {
    font-size: 12px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.7) !important;
    text-shadow: 0 1px 6px rgba(0, 0, 0, 0.25);

    &:hover {
      color: #fff !important;
    }
  }

  &__submit {
    margin-bottom: 0;
  }

  &__btn {
    width: 100%;
    height: 52px;
    border: none;
    border-radius: 9999px;
    background: #fff;
    font-size: 16px;
    font-weight: 700;
    color: #1e293b;
    box-shadow: 0 18px 50px -12px rgba(80, 90, 200, 0.6);
    transition:
      transform 0.2s ease,
      box-shadow 0.2s ease;

    &:hover,
    &:focus {
      background: #fff;
      color: #1e293b;
      transform: translateY(-2px);
      box-shadow: 0 22px 60px -12px rgba(80, 90, 200, 0.7);
    }

    &:active {
      transform: translateY(0);
    }
  }
}
</style>
