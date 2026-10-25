<template>
  <el-dialog
    v-model="dialogVisible"
    :title="isEdit ? `编辑报销单 - 【${editExpenseNo || ''}】` : '新建员工费用报销单'"
    width="960px"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-form :model="formData" label-width="110px" label-position="right" v-loading="loading">
      <!-- 基础信息卡片 -->
      <div
        class="bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded p-14px mb-16px"
      >
        <div
          class="text-13px font-semibold text-gray-800 dark:text-gray-200 mb-12px flex items-center gap-6px"
        >
          <Icon icon="ep:user" class="text-primary" />
          <span>报销主信息</span>
        </div>
        <div class="grid grid-cols-2 gap-x-20px">
          <!-- 报销人（缺省当前登录用户，支持代他人填报） -->
          <el-form-item label="报销人员" required>
            <UserSelect
              v-model="formData.applicantUserId"
              placeholder="请选择报销人（缺省本人）"
              :clearable="false"
              class="!w-full"
            />
          </el-form-item>

          <!-- 付款账户（停用过滤，联动币种） -->
          <el-form-item label="付款账户" required>
            <el-select
              v-model="formData.accountId"
              placeholder="请选择付款资金账户"
              filterable
              class="!w-full"
              @change="handleAccountChange"
            >
              <el-option
                v-for="acc in enabledAccountList"
                :key="acc.id"
                :label="`${acc.accountName} (${acc.currency})`"
                :value="acc.id"
              >
                <div class="flex items-center justify-between">
                  <span>{{ acc.accountName }}</span>
                  <el-tag size="small" type="info">{{ acc.currency }}</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <!-- 币种联动展示 -->
          <el-form-item label="账户币种">
            <div class="flex items-center gap-8px">
              <el-tag type="success" size="default" class="font-mono">
                {{ selectedAccountCurrency || 'RMB' }}
              </el-tag>
              <span class="text-12px text-gray-500">（选择付款账户后自动联动）</span>
            </div>
          </el-form-item>

          <!-- 实时总计 -->
          <el-form-item label="原币总额">
            <span class="font-mono font-bold text-16px text-orange-600">
              {{ selectedAccountCurrency || 'RMB' }} {{ formatAmount(computedTotal) }}
            </span>
          </el-form-item>
        </div>
      </div>

      <!-- 费用明细行表 -->
      <div class="mb-16px">
        <div class="flex items-center justify-between mb-8px">
          <div
            class="text-13px font-semibold text-gray-800 dark:text-gray-200 flex items-center gap-6px"
          >
            <Icon icon="ep:list" class="text-primary" />
            <span>费用明细（至少 1 行，金额 > 0，发生日不晚于今天）</span>
          </div>
          <el-button type="primary" plain size="small" @click="handleAddItem">
            <Icon icon="ep:plus" class="mr-4px" /> 添加明细行
          </el-button>
        </div>

        <el-table :data="formData.items" border stripe size="small" class="w-full">
          <el-table-column label="序号" type="index" width="55" align="center" />

          <el-table-column label="费用类别" width="160">
            <template #default="{ row }">
              <el-select v-model="row.category" placeholder="请选择类别" class="w-full">
                <el-option
                  v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_EXPENSE_CATEGORY)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="原币金额" width="160">
            <template #default="{ row }">
              <el-input-number
                v-model="row.originalAmount"
                :min="0.01"
                :precision="2"
                :step="100"
                :controls="false"
                placeholder="金额 > 0"
                class="!w-full"
              />
            </template>
          </el-table-column>

          <el-table-column label="发生日期" width="160">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.occurDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="发生日期"
                :disabled-date="disabledFutureDate"
                class="!w-full"
              />
            </template>
          </el-table-column>

          <el-table-column label="费用说明 (必填)" min-width="200">
            <template #default="{ row }">
              <el-input
                v-model="row.remark"
                placeholder="费用明细详细说明（如发票项目、出差路线）"
                maxlength="200"
                clearable
              />
            </template>
          </el-table-column>

          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button
                link
                type="danger"
                :disabled="formData.items.length <= 1"
                v-hasPermi="['lunaerp:finance:expense:create', 'lunaerp:finance:expense:update']"
                @click="removeLineItem($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 附件上传（≤10个） -->
      <div class="mb-16px">
        <div
          class="text-13px font-semibold text-gray-800 dark:text-gray-200 mb-8px flex items-center gap-6px"
        >
          <Icon icon="ep:paperclip" class="text-primary" />
          <span>报销附件（发票、票据、行程单等，最多 10 个）</span>
        </div>
        <UploadFile
          v-model="formData.attachments"
          :limit="10"
          :file-size="10"
          :file-type="[
            'doc',
            'docx',
            'xls',
            'xlsx',
            'ppt',
            'pptx',
            'txt',
            'pdf',
            'png',
            'jpg',
            'jpeg'
          ]"
          directory="expense"
        />
      </div>

      <!-- 报销备注说明 -->
      <el-form-item label="备注说明" class="mt-12px">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="出差或报销综合事由说明（可选）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer flex justify-between items-center">
        <div class="text-12px text-gray-500">
          * 提交后将进入审批流；审批通过后同事务完成资金账户扣款并记入资金流水。
        </div>
        <div class="flex gap-8px">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            plain
            :loading="submitting"
            v-hasPermi="['lunaerp:finance:expense:create', 'lunaerp:finance:expense:update']"
            @click="handleSave(false)"
          >
            保存草稿
          </el-button>
          <el-button
            type="primary"
            :loading="submitting"
            v-hasPermi="['lunaerp:finance:expense:create', 'lunaerp:finance:expense:update']"
            @click="handleSave(true)"
          >
            保存并提交审批
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { useMessage } from '@/hooks/web/useMessage'
import UserSelect from '@/views/system/user/components/UserSelect.vue'
import { UploadFile } from '@/components/UploadFile'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import * as ExpenseApi from '@/api/lunaerp-elmui/finance/expense'
import * as AccountApi from '@/api/lunaerp-elmui/finance/account'
import {
  formatAmount,
  calculateExpenseTotal,
  validateExpenseForm,
  parseExpenseErrorMessage,
  type ExpenseFormData
} from '../model/expense'

defineOptions({ name: 'ExpenseFormDialog' })

const emit = defineEmits<{
  success: []
}>()

const message = useMessage()
const userStore = useUserStore()

const dialogVisible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const editExpenseId = ref<number | undefined>(undefined)
const editExpenseNo = ref<string>('')

// 账户列表缓存
const accountList = ref<AccountApi.FinanceAccountSimpleRespVO[]>([])

// 仅保留 ENABLE 状态账户（停用过滤）
const enabledAccountList = computed(() => {
  return accountList.value.filter((item) => item.status === 'ENABLE')
})

const getTodayString = () => new Date().toLocaleDateString('en-CA')

const formData = reactive<ExpenseFormData>({
  id: undefined,
  applicantUserId: undefined,
  accountId: undefined,
  attachments: [],
  remark: '',
  items: []
})

// 选定账户币种联动
const selectedAccountCurrency = computed(() => {
  if (!formData.accountId) return 'RMB'
  const acc = accountList.value.find((a) => a.id === formData.accountId)
  return acc?.currency || 'RMB'
})

// 实时合计金额
const computedTotal = computed(() => {
  return calculateExpenseTotal(formData.items)
})

const disabledFutureDate = (time: Date) => {
  const endOfToday = new Date()
  endOfToday.setHours(23, 59, 59, 999)
  return time.getTime() > endOfToday.getTime()
}

const handleAccountChange = () => {
  // 联动刷新币种展示，保留已填明细
}

const handleAddItem = () => {
  formData.items.push({
    category: 'TRAVEL',
    originalAmount: undefined as any,
    occurDate: getTodayString(),
    remark: ''
  })
}

const removeLineItem = (index: number) => {
  if (formData.items.length <= 1) {
    message.warning('报销明细行至少需要保留一行')
    return
  }
  formData.items.splice(index, 1)
}

const loadAccountList = async () => {
  try {
    const list = await AccountApi.getSimpleAccountList()
    accountList.value = list || []
  } catch (err: any) {
    console.error('[ExpenseFormDialog] loadAccountList failed:', err)
  }
}

/** 打开弹窗（新建或编辑） */
const open = async (id?: number) => {
  dialogVisible.value = true
  isEdit.value = !!id
  editExpenseId.value = id
  editExpenseNo.value = ''

  await loadAccountList()

  if (id) {
    loading.value = true
    try {
      const detail = await ExpenseApi.getExpense(id)
      editExpenseNo.value = detail.expenseNo
      formData.id = detail.id
      formData.applicantUserId = detail.applicantUserId
      formData.accountId = detail.accountId
      formData.attachments = detail.attachments ? [...detail.attachments] : []
      formData.remark = detail.remark || ''
      formData.items = (detail.items || []).map((it) => ({
        id: it.id,
        category: it.category,
        originalAmount: it.originalAmount,
        occurDate: it.occurDate,
        remark: it.remark
      }))
      if (formData.items.length === 0) {
        handleAddItem()
      }
    } catch (err: any) {
      message.error(parseExpenseErrorMessage(err))
      dialogVisible.value = false
    } finally {
      loading.value = false
    }
  } else {
    // 新建：报销人缺省当前登录用户
    const currentUserId = userStore.getUser?.id
    formData.id = undefined
    formData.applicantUserId = currentUserId ? Number(currentUserId) : undefined
    // 默认选用第一个启用的人民币或活期账户
    const defaultAcc =
      enabledAccountList.value.find((a) => a.currency === 'RMB') || enabledAccountList.value[0]
    formData.accountId = defaultAcc?.id
    formData.attachments = []
    formData.remark = ''
    formData.items = [
      {
        category: 'TRAVEL',
        originalAmount: undefined as any,
        occurDate: getTodayString(),
        remark: ''
      }
    ]
  }
}

const handleSave = async (submitAfterSave: boolean) => {
  // 1. 前端表单校验
  const validationError = validateExpenseForm(formData)
  if (validationError) {
    message.warning(validationError)
    return
  }

  submitting.value = true
  try {
    let savedId = editExpenseId.value

    if (isEdit.value && savedId) {
      await ExpenseApi.updateExpense({
        id: savedId,
        applicantUserId: formData.applicantUserId,
        accountId: formData.accountId!,
        attachments: formData.attachments || [],
        remark: formData.remark,
        items: formData.items
      })
      if (!submitAfterSave) {
        message.success('报销单修改成功')
      }
    } else {
      savedId = await ExpenseApi.createExpense({
        applicantUserId: formData.applicantUserId,
        accountId: formData.accountId!,
        attachments: formData.attachments || [],
        remark: formData.remark,
        items: formData.items
      })
      if (!submitAfterSave) {
        message.success('报销单草稿已创建')
      }
    }

    // 若选择保存并提交
    if (submitAfterSave && savedId) {
      await ExpenseApi.submitExpense(savedId)
      message.success('报销单已提交审批')
    }

    dialogVisible.value = false
    emit('success')
  } catch (err: any) {
    message.error(parseExpenseErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
