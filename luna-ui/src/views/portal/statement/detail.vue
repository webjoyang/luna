<template>
  <div v-loading="loading" class="portal-statement-detail-page">
    <!-- 头部返回导航 -->
    <div class="detail-nav-header">
      <el-button link type="primary" :icon="ArrowLeft" @click="handleBack"> 返回列表 </el-button>
      <span class="nav-title">对账单详情</span>
      <div class="nav-placeholder"></div>
    </div>

    <template v-if="detail">
      <!-- 汇总主卡片 -->
      <div class="summary-card">
        <div class="card-row top-row">
          <span class="statement-no">{{ detail.statementNo }}</span>
          <el-tag :type="getStatementStatusTagType(detail.status)" size="small" effect="light">
            {{ getStatementStatusText(detail.status) }}
          </el-tag>
        </div>

        <div v-if="detail.customerName" class="card-row info-row">
          <span class="info-label">客户名称</span>
          <span class="info-val">{{ detail.customerName }}</span>
        </div>

        <div v-if="detail.periodStart || detail.periodEnd" class="card-row info-row">
          <span class="info-label">对账期间</span>
          <span class="info-val"
            >{{ detail.periodStart || '' }} ~ {{ detail.periodEnd || '' }}</span
          >
        </div>

        <div v-if="detail.statementDate || detail.createTime" class="card-row info-row">
          <span class="info-label">制单时间</span>
          <span class="info-val">{{
            detail.statementDate || formatDateTime(detail.createTime)
          }}</span>
        </div>

        <div class="card-row info-row">
          <span class="info-label">币种</span>
          <span class="info-val">{{ detail.currency || 'RMB' }}</span>
        </div>

        <!-- 财务金额信息 -->
        <div class="balance-summary-grid">
          <div class="grid-item">
            <span class="grid-label">期初余额</span>
            <span class="grid-val">{{ formatAmount(detail.openingBalance, detail.currency) }}</span>
          </div>
          <div class="grid-item">
            <span class="grid-label">本期发生借方</span>
            <span class="grid-val debit">{{
              formatAmount(detail.periodDebit, detail.currency)
            }}</span>
          </div>
          <div class="grid-item">
            <span class="grid-label">本期发生贷方</span>
            <span class="grid-val credit">{{
              formatAmount(detail.periodCredit, detail.currency)
            }}</span>
          </div>
          <div class="grid-item highlight">
            <span class="grid-label">期末应结余额</span>
            <span class="grid-val closing">{{
              formatAmount(detail.closingBalance, detail.currency)
            }}</span>
          </div>
        </div>

        <div v-if="detail.remark" class="card-row info-row remark-row">
          <span class="info-label">备注</span>
          <span class="info-val">{{ detail.remark }}</span>
        </div>

        <!-- 确认/驳回留痕记录 -->
        <template v-if="detail.status === 'CONFIRMED'">
          <div class="history-box confirm-history">
            <div class="history-title">确认记录</div>
            <div class="card-row info-row">
              <span class="info-label">确认人</span>
              <span class="info-val">{{ detail.confirmByName || '-' }}</span>
            </div>
            <div v-if="detail.confirmTime" class="card-row info-row">
              <span class="info-label">确认时间</span>
              <span class="info-val">{{ formatDateTime(detail.confirmTime) }}</span>
            </div>
            <div v-if="detail.confirmType" class="card-row info-row">
              <span class="info-label">确认方式</span>
              <span class="info-val">
                {{
                  detail.confirmType === 'ONLINE_PORTAL' ? '客户门户在线确认' : detail.confirmType
                }}
              </span>
            </div>
          </div>
        </template>

        <template v-if="detail.status === 'REJECTED'">
          <div class="history-box reject-history">
            <div class="history-title">驳回原因</div>
            <div class="card-row info-row">
              <span class="info-label">异议原因</span>
              <span class="info-val reject-reason">{{ detail.rejectReason || '-' }}</span>
            </div>
          </div>
        </template>
      </div>

      <!-- 对账分录明细行列表 -->
      <div class="items-section">
        <div class="section-title">
          <span>对账明细清单</span>
          <span class="item-count">共 {{ detail.items ? detail.items.length : 0 }} 笔记录</span>
        </div>

        <div v-if="detail.items && detail.items.length > 0" class="items-list">
          <div
            v-for="(item, idx) in detail.items"
            :key="item.id || idx"
            class="item-card"
            :class="{ 'reversed-item': item.isReversed }"
          >
            <div class="item-header">
              <div class="item-line-no">#{{ item.lineNo || idx + 1 }}</div>
              <div class="item-biz-no">{{ item.bizNo || '-' }}</div>
              <el-tag v-if="item.isReversed" size="small" type="danger" effect="plain"
                >已红冲</el-tag
              >
            </div>

            <div class="item-meta">
              <span class="meta-label">业务日期:</span>
              <span class="meta-val">{{ item.bizDate || '-' }}</span>
            </div>

            <div v-if="item.summary" class="item-meta">
              <span class="meta-label">业务摘要:</span>
              <span class="meta-val">{{ item.summary }}</span>
            </div>

            <div class="item-amount-row">
              <div v-if="item.debitAmount != null && item.debitAmount > 0" class="amount-col">
                <span class="col-label">借方(应收增加):</span>
                <span class="col-val debit"
                  >+{{ formatAmount(item.debitAmount, detail.currency) }}</span
                >
              </div>
              <div v-if="item.creditAmount != null && item.creditAmount > 0" class="amount-col">
                <span class="col-label">贷方(已收核销):</span>
                <span class="col-val credit"
                  >-{{ formatAmount(item.creditAmount, detail.currency) }}</span
                >
              </div>
              <div class="amount-col balance-col">
                <span class="col-label">结存余额:</span>
                <span class="col-val">{{ formatAmount(item.balanceAmount, detail.currency) }}</span>
              </div>
            </div>

            <div v-if="item.remark" class="item-remark">
              <span class="meta-label">行备注:</span>
              <span class="meta-val">{{ item.remark }}</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-items">
          <el-empty description="暂无对账明细行" :image-size="60" />
        </div>
      </div>

      <!-- 底部固定操作栏（仅 SENT 待确认状态显示） -->
      <div v-if="canConfirmOrReject(detail.status)" class="bottom-action-bar">
        <el-button class="action-btn reject-btn" type="danger" plain @click="openRejectDialog">
          驳回对账单
        </el-button>
        <el-button class="action-btn confirm-btn" type="primary" @click="openConfirmDialog">
          确认对账单
        </el-button>
      </div>

      <!-- 确认弹窗 -->
      <el-dialog
        v-model="confirmDialogVisible"
        title="确认对账单"
        width="90%"
        class="portal-dialog"
        destroy-on-close
      >
        <div class="dialog-tip">请仔细核对对账单金额与明细，在线确认后不可撤回。</div>
        <el-form
          ref="confirmFormRef"
          :model="confirmForm"
          :rules="confirmRules"
          label-position="top"
        >
          <el-form-item label="确认人姓名（手填姓名必填）" prop="confirmByName">
            <el-input
              v-model="confirmForm.confirmByName"
              placeholder="请输入经办人/采购负责人姓名"
              maxlength="30"
              clearable
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button :disabled="submitting" @click="confirmDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="doSubmitConfirm">
              确认提交
            </el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 驳回弹窗 -->
      <el-dialog
        v-model="rejectDialogVisible"
        title="驳回对账单"
        width="90%"
        class="portal-dialog"
        destroy-on-close
      >
        <div class="dialog-tip">请详细填写异议或驳回原因，业务人员核实后将重新出具对账单。</div>
        <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-position="top">
          <el-form-item label="驳回原因（必填）" prop="rejectReason">
            <el-input
              v-model="rejectForm.rejectReason"
              type="textarea"
              :rows="4"
              placeholder="例如：第2行发货单货物已退货，金额请扣减后再出账"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button :disabled="submitting" @click="rejectDialogVisible = false">取消</el-button>
            <el-button type="danger" :loading="submitting" @click="doSubmitReject">
              确认驳回
            </el-button>
          </div>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getPortalStatement,
  confirmPortalStatement,
  rejectPortalStatement,
  type PortalStatementDetailRespVO
} from '@/api/portal'
import {
  getStatementStatusText,
  getStatementStatusTagType,
  formatAmount,
  formatDateTime,
  resolveStatementDetailParams,
  canConfirmOrReject
} from '../model/portalStatement'
import { getPortalToken, handlePortalTokenGuide, setPortalCustomerName } from '@/utils/portalAuth'

defineOptions({ name: 'PortalStatementDetail' })

const route = useRoute()
const router = useRouter()

const loading = ref<boolean>(false)
const submitting = ref<boolean>(false)
const detail = ref<PortalStatementDetailRespVO | null>(null)

// 确认弹窗
const confirmDialogVisible = ref<boolean>(false)
const confirmFormRef = ref<FormInstance>()
const confirmForm = ref<{ confirmByName: string }>({ confirmByName: '' })
const confirmRules: FormRules = {
  confirmByName: [
    { required: true, message: '请输入确认人姓名', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !value.trim()) {
          callback(new Error('确认人姓名不能为空'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 驳回弹窗
const rejectDialogVisible = ref<boolean>(false)
const rejectFormRef = ref<FormInstance>()
const rejectForm = ref<{ rejectReason: string }>({ rejectReason: '' })
const rejectRules: FormRules = {
  rejectReason: [
    { required: true, message: '请输入驳回/异议原因', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value || !value.trim()) {
          callback(new Error('驳回原因不能为空'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleBack = () => {
  router.push('/portal/statements')
}

const loadDetail = async () => {
  // 1. 优先提取 ?t=
  if (route.query?.t) {
    handlePortalTokenGuide(route.query.t as string)
  }

  // 2. 无会话直达（无 token）→ 引导至 invalid 页
  const token = getPortalToken()
  if (!token) {
    router.replace('/portal/invalid')
    return
  }

  // 3. 解析双形态入参
  const { id, no, isValid } = resolveStatementDetailParams(
    { id: route.params.id as string },
    { no: route.query.no as string, statementNo: route.query.statementNo as string }
  )

  if (!isValid) {
    router.replace('/portal/invalid')
    return
  }

  loading.value = true
  try {
    const res = await getPortalStatement({ id, no })
    detail.value = res
    if (res?.customerName) {
      setPortalCustomerName(res.customerName)
    }
  } catch (err) {
    console.error('获取对账单详情失败:', err)
  } finally {
    loading.value = false
  }
}

const openConfirmDialog = () => {
  confirmForm.value.confirmByName = ''
  confirmDialogVisible.value = true
}

const openRejectDialog = () => {
  rejectForm.value.rejectReason = ''
  rejectDialogVisible.value = true
}

const doSubmitConfirm = async () => {
  if (!confirmFormRef.value || !detail.value) return
  await confirmFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await confirmPortalStatement({
        id: detail.value!.id,
        confirmByName: confirmForm.value.confirmByName.trim()
      })
      ElMessage.success('对账单在线确认成功')
      confirmDialogVisible.value = false
      await loadDetail()
    } catch (err) {
      console.error('确认对账单失败:', err)
    } finally {
      submitting.value = false
    }
  })
}

const doSubmitReject = async () => {
  if (!rejectFormRef.value || !detail.value) return
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await rejectPortalStatement({
        id: detail.value!.id,
        rejectReason: rejectForm.value.rejectReason.trim()
      })
      ElMessage.success('对账单驳回已提交')
      rejectDialogVisible.value = false
      await loadDetail()
    } catch (err) {
      console.error('驳回对账单失败:', err)
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.portal-statement-detail-page {
  padding: 12px;
  background-color: #f7f8fa;
  min-height: calc(100vh - 100px);
  padding-bottom: 80px;
}

.detail-nav-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.nav-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.nav-placeholder {
  width: 60px;
}

.summary-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.card-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.top-row {
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #f2f3f5;
  margin-bottom: 12px;
}

.statement-no {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.info-row {
  font-size: 13px;
  line-height: 1.5;
}

.info-label {
  color: #909399;
  white-space: nowrap;
  margin-right: 8px;
}

.info-val {
  color: #303133;
  text-align: right;
  word-break: break-all;
}

.balance-summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  background-color: #f8fafc;
  border-radius: 6px;
  padding: 12px;
  margin: 10px 0;
}

.grid-item {
  display: flex;
  flex-direction: column;
}

.grid-label {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}

.grid-val {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.grid-val.debit {
  color: #e6a23c;
}

.grid-val.credit {
  color: #67c23a;
}

.grid-item.highlight {
  grid-column: span 2;
  border-top: 1px dashed #dcdfe6;
  padding-top: 6px;
  margin-top: 4px;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
}

.grid-val.closing {
  font-size: 18px;
  color: #f56c6c;
}

.history-box {
  border-radius: 6px;
  padding: 10px 12px;
  margin-top: 12px;
}

.confirm-history {
  background-color: #f0f9eb;
  border: 1px solid #e1f3d8;
}

.reject-history {
  background-color: #fef0f0;
  border: 1px solid #fde2e2;
}

.history-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
}

.reject-reason {
  color: #f56c6c;
  font-weight: 500;
}

.items-section {
  margin-top: 16px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.item-count {
  font-size: 12px;
  font-weight: normal;
  color: #909399;
}

.item-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.item-card.reversed-item {
  border-left: 3px solid #f56c6c;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-line-no {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
}

.item-biz-no {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.item-meta {
  display: flex;
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.item-amount-row {
  display: flex;
  justify-content: space-between;
  background-color: #fafafa;
  border-radius: 4px;
  padding: 8px;
  margin-top: 8px;
}

.amount-col {
  display: flex;
  flex-direction: column;
}

.col-label {
  font-size: 10px;
  color: #909399;
  margin-bottom: 2px;
}

.col-val {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.col-val.debit {
  color: #e6a23c;
}

.col-val.credit {
  color: #67c23a;
}

.item-remark {
  font-size: 11px;
  color: #909399;
  margin-top: 6px;
}

.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  padding: 12px 16px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  gap: 12px;
  z-index: 100;
}

.action-btn {
  flex: 1;
  height: 40px;
  font-size: 15px;
  font-weight: 500;
}

.dialog-tip {
  font-size: 13px;
  color: #606266;
  margin-bottom: 16px;
  line-height: 1.4;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
