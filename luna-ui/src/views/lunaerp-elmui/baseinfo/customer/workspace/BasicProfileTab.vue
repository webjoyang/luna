<template>
  <div class="basic-profile-tab-wrapper">
    <!-- 客户门户卡片（仅编辑模式展示） -->
    <el-card v-if="!isCreateMode" class="portal-card mb-4" shadow="never">
      <template #header>
        <div class="portal-card-header">
          <div class="title-group">
            <span class="card-title">客户门户</span>
            <el-tag :type="portalTokenInfo?.token ? 'success' : 'info'" size="small">
              {{ portalTokenInfo?.token ? '已开通' : '未开通' }}
            </el-tag>
          </div>
          <div class="portal-actions">
            <el-button
              v-if="!portalTokenInfo?.token"
              type="primary"
              size="small"
              :loading="portalActionLoading"
              v-hasPermi="['lunaerp:baseinfo:customer:portal-token']"
              @click="handleGenerateToken(false)"
            >
              生成门户链接
            </el-button>
            <template v-else>
              <el-button
                type="primary"
                size="small"
                :loading="portalActionLoading"
                v-hasPermi="['lunaerp:baseinfo:customer:portal-token']"
                @click="handleGenerateToken(true)"
              >
                重新生成
              </el-button>
              <el-button
                size="small"
                v-hasPermi="['lunaerp:baseinfo:customer:portal-token']"
                @click="handleCopyLink"
              >
                复制链接
              </el-button>
              <el-button
                type="danger"
                plain
                size="small"
                :loading="portalActionLoading"
                v-hasPermi="['lunaerp:baseinfo:customer:portal-token']"
                @click="handleRevokeToken"
              >
                吊销
              </el-button>
            </template>
          </div>
        </div>
      </template>

      <div v-loading="portalLoading" class="portal-card-body">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="门户状态">
            <span v-if="portalTokenInfo?.token" class="text-success font-medium">
              有效期至 {{ formatExpireTime(portalTokenInfo.expireTime) }} (剩余 {{ portalTokenInfo.expireDays ?? 0 }} 天)
            </span>
            <span v-else class="text-gray-400">未开通</span>
          </el-descriptions-item>
          <el-descriptions-item label="生成时间">
            <span>{{ formatDateTime(portalTokenInfo?.generateTime) }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="portalTokenInfo?.token" label="门户链接" :span="2">
            <div class="portal-url-display">
              <span v-if="portalTokenInfo.url" class="portal-url-text">{{ portalTokenInfo.url }}</span>
              <span v-else class="text-warning">未配置客户门户基准地址(luna.portal.base-url)，仅生成 Token: {{ portalTokenInfo.token }}</span>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
      class="basic-profile-form"
      v-loading="loading"
    >
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="公司名称" prop="companyName">
          <el-input v-model="formData.companyName" placeholder="请输入公司名称" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="简称" prop="shortName">
          <el-input v-model="formData.shortName" placeholder="留空时自动用公司名称回填" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="公司地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入公司地址" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="英文名称" prop="nameEn">
          <el-input v-model="formData.nameEn" placeholder="请输入英文名称（外销单据带出）" maxlength="128" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="英文地址" prop="addressEn">
          <el-input v-model="formData.addressEn" placeholder="请输入英文地址（外销单据带出）" maxlength="512" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="客户类型" prop="clientType">
          <el-select v-model="formData.clientType" placeholder="请选择客户类型" class="!w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_CLIENT_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="所属行业" prop="industry">
          <el-select v-model="formData.industry" placeholder="请选择行业" clearable class="!w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_INDUSTRY)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="企业规模" prop="scale">
          <el-select v-model="formData.scale" placeholder="请选择规模" clearable class="!w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_SCALE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="客户来源" prop="source">
          <el-select v-model="formData.source" placeholder="请选择来源" clearable class="!w-full">
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_SOURCE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="合作状态" prop="cooperationStatus">
          <el-select
            v-model="formData.cooperationStatus"
            placeholder="请选择合作状态"
            clearable
            class="!w-full"
          >
            <el-option
              v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_CUSTOMER_COOPERATION_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="统一信用代码" prop="unifiedSocialCreditCode">
          <el-input v-model="formData.unifiedSocialCreditCode" placeholder="请输入统一社会信用代码" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态" class="!w-full">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="归属业务员" prop="salespersonUserId">
          <UserSelect
            v-model="formData.salespersonUserId"
            placeholder="请选择归属业务员（可空）"
            clearable
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="默认毛利率 (%)" prop="defaultMarginRate">
          <el-input-number
            v-model="marginRatePercent"
            :min="-99"
            :max="99.9"
            :step="1"
            :precision="2"
            controls-position="right"
            placeholder="留空回退全局 15%"
            class="!w-full"
          />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-col>
    </el-row>
    <div class="form-footer">
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
        v-hasPermi="
          isCreateMode
            ? ['lunaerp:baseinfo:customer:create']
            : ['lunaerp:baseinfo:customer:update']
        "
      >
        保存
      </el-button>
      <el-button @click="handleBack">返回</el-button>
    </div>
  </el-form>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO, PortalTokenRespVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import UserSelect from '@/views/system/user/components/UserSelect.vue'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'LunaerpElmuiCustomerBasicProfileTab' })

const props = defineProps<{
  /** 客户 ID，create 模式为空 */
  customerId?: number | string | null
}>()

const emit = defineEmits<{ (e: 'saved', customer: CustomerVO): void }>()

const message = ElMessage
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const marginRatePercent = ref<number | undefined>(undefined)

// 客户门户状态
const portalLoading = ref(false)
const portalActionLoading = ref(false)
const portalTokenInfo = ref<PortalTokenRespVO | null>(null)

const formData = reactive<CustomerVO>({
  companyName: '',
  shortName: '',
  phone: '',
  address: '',
  clientType: '',
  industry: '',
  scale: '',
  source: '',
  cooperationStatus: '',
  unifiedSocialCreditCode: '',
  remark: '',
  status: 'ACTIVE',
  salespersonUserId: undefined,
  defaultMarginRate: undefined,
  nameEn: '',
  addressEn: ''
})

const rules: FormRules = {
  companyName: [{ required: true, message: '公司名称不能为空', trigger: 'blur' }],
  clientType: [{ required: true, message: '客户类型不能为空', trigger: 'change' }]
}

const isCreateMode = computed(() => !props.customerId)

const formatExpireTime = (time?: string | Date | null) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

const formatDateTime = (time?: string | Date | null) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm')
}

const copyToClipboard = async (text: string) => {
  if (navigator?.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
  } else {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
}

const loadCustomer = async () => {
  if (!props.customerId) return
  loading.value = true
  try {
    const data = await CustomerApi.getCustomer(Number(props.customerId))
    Object.assign(formData, data)
    marginRatePercent.value =
      data.defaultMarginRate != null ? Number((data.defaultMarginRate * 100).toFixed(4)) : undefined
  } finally {
    loading.value = false
  }
}

const loadPortalToken = async () => {
  if (!props.customerId) return
  portalLoading.value = true
  try {
    const data = await CustomerApi.getPortalToken(Number(props.customerId))
    portalTokenInfo.value = data
  } catch {
    portalTokenInfo.value = null
  } finally {
    portalLoading.value = false
  }
}

const handleGenerateToken = async (isRegenerate: boolean) => {
  if (!props.customerId) return
  if (isRegenerate) {
    try {
      await ElMessageBox.confirm('重新生成将使旧链接立即失效，是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }

  portalActionLoading.value = true
  try {
    const data = await CustomerApi.generatePortalToken(Number(props.customerId))
    portalTokenInfo.value = data
    message.success(isRegenerate ? '重新生成成功' : '生成成功')
  } finally {
    portalActionLoading.value = false
  }
}

const handleCopyLink = async () => {
  if (!portalTokenInfo.value?.token) return
  if (portalTokenInfo.value.url) {
    await copyToClipboard(portalTokenInfo.value.url)
    message.success('链接已复制到剪贴板')
  } else {
    await copyToClipboard(portalTokenInfo.value.token)
    message.warning('未配置客户门户基准地址(luna.portal.base-url)，仅复制 Token')
  }
}

const handleRevokeToken = async () => {
  if (!props.customerId) return
  try {
    await ElMessageBox.confirm('确定要吊销该客户的门户链接吗？吊销后旧链接将立即失效。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  portalActionLoading.value = true
  try {
    await CustomerApi.revokePortalToken(Number(props.customerId))
    portalTokenInfo.value = null
    message.success('吊销成功')
  } finally {
    portalActionLoading.value = false
  }
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    formData.defaultMarginRate =
      marginRatePercent.value != null && !Number.isNaN(marginRatePercent.value)
        ? Number((marginRatePercent.value / 100).toFixed(6))
        : undefined
    if (isCreateMode.value) {
      const id = await CustomerApi.createCustomer(formData)
      message.success('保存成功')
      const saved = { ...formData, id } as CustomerVO
      emit('saved', saved)
      router.push('/baseinfo/customer')
    } else {
      // 编辑模式：update 后回列表
      await CustomerApi.updateCustomer(formData)
      message.success('保存成功')
      emit('saved', { ...formData })
      router.push('/baseinfo/customer')
    }
  } finally {
    submitting.value = false
  }
}

const handleBack = () => {
  router.push('/baseinfo/customer')
}

watch(
  () => props.customerId,
  (newVal) => {
    if (newVal) {
      loadCustomer()
      loadPortalToken()
    }
  }
)

onMounted(() => {
  if (isCreateMode.value) {
    const userStore = useUserStore()
    if (userStore.getUser?.id) {
      formData.salespersonUserId = userStore.getUser.id
    }
  } else {
    loadCustomer()
    loadPortalToken()
  }
})
</script>

<style lang="scss" scoped>
.basic-profile-tab-wrapper {
  max-width: 1200px;
  padding: 16px;
}

.portal-card {
  margin-bottom: 20px;

  .portal-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title-group {
      display: flex;
      align-items: center;
      gap: 8px;

      .card-title {
        font-weight: 600;
        font-size: 15px;
      }
    }

    .portal-actions {
      display: flex;
      gap: 8px;
    }
  }

  .portal-card-body {
    .text-success {
      color: var(--el-color-success);
    }

    .text-gray-400 {
      color: var(--el-text-color-placeholder);
    }

    .text-warning {
      color: var(--el-color-warning);
    }

    .portal-url-text {
      word-break: break-all;
    }
  }
}

.basic-profile-form {
  padding: 0;
}

.form-footer {
  margin-top: 12px;
  padding-left: 120px;
}
</style>
