<template>
  <ContentWrap>
    <!-- 顶部提示条 -->
    <el-alert type="info" :closable="false" show-icon class="mb-14px">
      <template #title>
        <span class="font-medium text-13px">资金账户与流水管理说明</span>
      </template>
      <template #default>
        <div class="text-12px text-gray-600 mt-2px leading-relaxed">
          1.
          <strong>受控物化缓存</strong
          >：资金账户【当前可用余额】由统一过账服务受控物化，无手工改余额入口；<br />
          2.
          <strong>期初保护</strong
          >：账户期初余额仅在无任何分录时允许修改；已有流水账户修改期初将触发 020_104 守卫；<br />
          3.
          <strong>生命周期</strong
          >：存在分录引用的账户严禁删除（020_103）；废弃或不再使用的账户请通过【停用】处理，停用账户会自动从各业务下拉中过滤。
        </div>
      </template>
    </el-alert>

    <!-- 搜索与操作栏 -->
    <el-card shadow="never" class="mb-14px !border-gray-200">
      <el-form
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="70px"
        class="-mb-15px"
      >
        <el-form-item label="账户名称" prop="accountName">
          <el-input
            v-model="queryParams.accountName"
            placeholder="模糊搜索账户名称"
            clearable
            class="!w-160px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item label="账户类型" prop="accountType">
          <el-select
            v-model="queryParams.accountType"
            placeholder="全部类型"
            clearable
            class="!w-130px"
          >
            <el-option
              v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_TYPE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="账户性质" prop="accountNature">
          <el-select
            v-model="queryParams.accountNature"
            placeholder="全部性质"
            clearable
            class="!w-120px"
          >
            <el-option
              v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_NATURE)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-120px">
            <el-option
              v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button @click="handleQuery">
            <Icon icon="ep:search" class="mr-2px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-2px" /> 重置
          </el-button>
          <el-button
            type="primary"
            plain
            v-hasPermi="['lunaerp:finance:account:create']"
            @click="handleCreate"
          >
            <Icon icon="ep:plus" class="mr-2px" /> 新增账户
          </el-button>
          <el-button
            type="success"
            plain
            v-hasPermi="['lunaerp:finance:account:transfer']"
            @click="handleTransfer()"
          >
            <Icon icon="ep:sort" class="mr-2px" /> 账户间转账
          </el-button>
          <el-button
            type="warning"
            plain
            v-hasPermi="['lunaerp:finance:account:other-txn']"
            @click="handleOtherTxn()"
          >
            <Icon icon="ep:document" class="mr-2px" /> 其他收支
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表表格 -->
    <el-card shadow="never" class="!border-gray-200">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="账户名称" prop="accountName" min-width="160">
          <template #default="{ row }">
            <span class="font-medium text-gray-900">{{ row.accountName }}</span>
          </template>
        </el-table-column>

        <el-table-column label="账户类型" prop="accountType" width="110" align="center">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_TYPE" :value="row.accountType" />
          </template>
        </el-table-column>

        <el-table-column label="公私性质" prop="accountNature" width="100" align="center">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_NATURE" :value="row.accountNature" />
          </template>
        </el-table-column>

        <el-table-column label="账号/标识" prop="accountNo" min-width="180">
          <template #default="{ row }">
            <span class="font-mono text-13px text-gray-700">
              {{ maskAccountNo(row.accountNo) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="币种" prop="currency" width="80" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" class="font-mono font-bold">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="期初余额" prop="openingBalance" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-mono text-gray-600">{{ formatAmount(row.openingBalance) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="当前可用余额" prop="currentBalance" min-width="140" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-14px text-emerald-800">
              ¥ {{ formatAmount(row.currentBalance) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="status" width="90" align="center">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_STATUS" :value="row.status" />
          </template>
        </el-table-column>

        <el-table-column label="备注说明" prop="remark" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createTime" width="150" align="center">
          <template #default="{ row }">
            <span class="font-mono text-12px text-gray-500">{{ row.createTime || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              v-hasPermi="['lunaerp:finance:account:update']"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              v-hasPermi="['lunaerp:finance:account:query']"
              @click="handleOpenLedger(row)"
            >
              流水
            </el-button>
            <el-button
              link
              :type="row.status === 'ENABLE' ? 'warning' : 'success'"
              size="small"
              v-hasPermi="['lunaerp:finance:account:update']"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'ENABLE' ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              v-hasPermi="['lunaerp:finance:account:delete']"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="flex justify-end mt-16px">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>

    <!-- 弹窗与抽屉 -->
    <AccountFormDialog ref="formDialogRef" @success="getList" />
    <LedgerDrawer ref="ledgerDrawerRef" />
    <TransferDialog ref="transferDialogRef" @success="getList" />
    <OtherTxnDialog ref="otherTxnDialogRef" @success="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import { getAccountPage, updateAccount, deleteAccount } from '@/api/lunaerp-elmui/finance/account'
import type { AccountRespVO, AccountPageReqVO } from '@/api/lunaerp-elmui/finance/account'
import { formatAmount, maskAccountNo, parseAccountErrorMessage } from './model/account'
import AccountFormDialog from './components/AccountFormDialog.vue'
import LedgerDrawer from './components/LedgerDrawer.vue'
import TransferDialog from './components/TransferDialog.vue'
import OtherTxnDialog from './components/OtherTxnDialog.vue'

defineOptions({ name: 'LunaerpFinanceAccount' })

const route = useRoute()
const loading = ref(false)
const list = ref<AccountRespVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const formDialogRef = ref<InstanceType<typeof AccountFormDialog>>()
const ledgerDrawerRef = ref<InstanceType<typeof LedgerDrawer>>()
const transferDialogRef = ref<InstanceType<typeof TransferDialog>>()
const otherTxnDialogRef = ref<InstanceType<typeof OtherTxnDialog>>()

const queryParams = reactive<AccountPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  accountName: '',
  accountType: '',
  accountNature: '',
  status: ''
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getAccountPage(queryParams)
    list.value = res.list || []
    total.value = res.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '获取资金账户列表失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.accountName = ''
  queryParams.accountType = ''
  queryParams.accountNature = ''
  queryParams.status = ''
  handleQuery()
}

const handleCreate = () => {
  formDialogRef.value?.open()
}

const handleEdit = (row: AccountRespVO) => {
  formDialogRef.value?.open(row)
}

const handleOpenLedger = (row: AccountRespVO) => {
  ledgerDrawerRef.value?.open({
    accountId: row.id,
    accountName: row.accountName
  })
}

const handleTransfer = (defaultFromAccountId?: number) => {
  transferDialogRef.value?.open(defaultFromAccountId)
}

const handleOtherTxn = (defaultAccountId?: number) => {
  otherTxnDialogRef.value?.open(defaultAccountId)
}

const handleToggleStatus = async (row: AccountRespVO) => {
  const nextStatus = row.status === 'ENABLE' ? 'DISABLE' : 'ENABLE'
  const actionText = nextStatus === 'ENABLE' ? '启用' : '停用'

  try {
    await ElMessageBox.confirm(
      `确定要${actionText}资金账户【${row.accountName}】吗？${
        nextStatus === 'DISABLE' ? '停用后该账户将不再出现在各业务单据的下拉选择中。' : ''
      }`,
      `${actionText}二次确认`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: nextStatus === 'DISABLE' ? 'warning' : 'info'
      }
    )

    await updateAccount({
      id: row.id,
      status: nextStatus
    })
    ElMessage.success(`资金账户已成功${actionText}`)
    getList()
  } catch (error: any) {
    if (error !== 'cancel') {
      const msg = parseAccountErrorMessage(error?.code, error?.message || `${actionText}失败`)
      ElMessage.error(msg)
    }
  }
}

const handleDelete = async (row: AccountRespVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除资金账户【${row.accountName}】吗？若账户已有分录引用将无法删除，请改为停用。`,
      '删除二次确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'danger'
      }
    )

    await deleteAccount(row.id)
    ElMessage.success('资金账户已删除并移至回收站')
    getList()
  } catch (error: any) {
    if (error !== 'cancel') {
      const msg = parseAccountErrorMessage(error?.code, error?.message || '删除账户失败')
      ElMessage.error(msg)
    }
  }
}

onMounted(() => {
  getList()
  // 支持从流水跳转来源解析快捷弹窗
  if (route.query.tab === 'transfer') {
    handleTransfer()
  } else if (route.query.tab === 'other-txn') {
    handleOtherTxn()
  }
})
</script>
