<template>
  <PortalLayout title="对账单详情" :customer-name="customerName" :hide-tab-bar="detail?.status === 'SENT'">
    <div v-loading="loading || detailLoading" class="portal-statement-detail-page">
      <!-- 顶部操作条（返回） -->
      <div class="top-nav">
        <el-button link type="primary" class="back-btn" @click="goBack">
          &lt; 返回对账单列表
        </el-button>
      </div>

      <template v-if="detail">
        <!-- 单据主信息卡片 -->
        <div class="detail-card main-card">
          <div class="card-header">
            <div class="title-wrap">
              <span class="statement-no" data-test="detail-statement-no">{{ detail.statementNo }}</span>
              <el-tag :type="getStatusTagType(detail.status)" size="small" data-test="detail-status-tag">
                {{ getStatusLabel(detail.status) }}
              </el-tag>
            </div>
          </div>

          <div class="card-body">
            <div class="info-row">
              <span class="label">对账期间</span>
              <span class="value" data-test="detail-period">{{ detail.periodStart }} ~ {{ detail.periodEnd }}</span>
            </div>
            <div class="info-row">
              <span class="label">结算币种</span>
              <span class="value" data-test="detail-currency">{{ detail.currency || 'CNY' }}</span>
            </div>
            <div v-if="detail.remark" class="info-row">
              <span class="label">单据备注</span>
              <span class="value" data-test="detail-remark">{{ detail.remark }}</span>
            </div>
          </div>

          <!-- 财务四要素汇总卡片 -->
          <div class="financial-summary-box">
            <div class="amount-card-item">
              <span class="lbl">期初欠款</span>
              <span class="val" data-test="detail-opening-balance">¥{{ formatAmount(detail.openingBalance) }}</span>
            </div>
            <div class="amount-card-item">
              <span class="lbl">本期发货(借)</span>
              <span class="val debit" data-test="detail-period-debit">+¥{{ formatAmount(detail.periodDebit) }}</span>
            </div>
            <div class="amount-card-item">
              <span class="lbl">本期回款(贷)</span>
              <span class="val credit" data-test="detail-period-credit">-¥{{ formatAmount(detail.periodCredit) }}</span>
            </div>
            <div class="amount-card-item closing-item">
              <span class="lbl">期末欠款合计</span>
              <span class="val closing" data-test="detail-closing-balance">¥{{ formatAmount(detail.closingBalance) }}</span>
            </div>
          </div>

          <!-- 确认信息卡片（已确认状态展示） -->
          <div v-if="detail.status === 'CONFIRMED'" class="status-info-box confirm-box" data-test="confirm-info-box">
            <div class="box-title">对账单确认信息</div>
            <div class="info-row">
              <span class="label">确认人姓名</span>
              <span class="value" data-test="detail-confirm-by">{{ detail.confirmByName || '-' }}</span>
            </div>
            <div v-if="detail.confirmTime" class="info-row">
              <span class="label">确认时间</span>
              <span class="value" data-test="detail-confirm-time">{{ detail.confirmTime }}</span>
            </div>
            <div class="info-row">
              <span class="label">确认方式</span>
              <span class="value" data-test="detail-confirm-type">
                {{ detail.confirmType === 'ONLINE_PORTAL' ? '线上门户' : (detail.confirmType || '线上确认') }}
              </span>
            </div>
          </div>

          <!-- 驳回信息卡片（已驳回状态展示） -->
          <div v-if="detail.status === 'REJECTED'" class="status-info-box reject-box" data-test="reject-info-box">
            <div class="box-title">对账单驳回原因</div>
            <div class="info-row">
              <span class="label">驳回原因</span>
              <span class="value text-danger" data-test="detail-reject-reason">{{ detail.rejectReason || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 对账明细行列表 -->
        <div class="detail-section">
          <div class="section-title">
            <span>对账明细清单</span>
            <span class="item-count">共 {{ detail.items ? detail.items.length : 0 }} 笔发生额</span>
          </div>

          <div class="items-list">
            <div
              v-for="(item, idx) in detail.items"
              :key="item.id || idx"
              class="item-card"
              data-test="item-card"
            >
              <div class="item-header">
                <span class="item-date" data-test="item-biz-date">{{ item.bizDate }}</span>
                <span class="item-direction" :class="item.direction === 'DEBIT' ? 'tag-debit' : 'tag-credit'">
                  {{ item.direction === 'DEBIT' ? '借(发货)' : '贷(回款/冲减)' }}
                </span>
              </div>

              <div class="item-body">
                <div v-if="item.refNo" class="info-line">
                  <span class="label">单据编号:</span>
                  <span class="val" data-test="item-ref-no">{{ item.refNo }}</span>
                </div>
                <div v-if="item.summary" class="info-line">
                  <span class="label">摘要事由:</span>
                  <span class="val" data-test="item-summary">{{ item.summary }}</span>
                </div>
                <div class="amount-line">
                  <div class="amt-col">
                    <span class="label">借方金额:</span>
                    <span class="val" :class="{ 'has-amount': item.debitAmount > 0 }" data-test="item-debit-amount">
                      ¥{{ formatAmount(item.debitAmount) }}
                    </span>
                  </div>
                  <div class="amt-col">
                    <span class="label">贷方金额:</span>
                    <span class="val" :class="{ 'has-amount': item.creditAmount > 0 }" data-test="item-credit-amount">
                      ¥{{ formatAmount(item.creditAmount) }}
                    </span>
                  </div>
                </div>
                <div v-if="item.signStatus" class="sign-tag-line">
                  <span class="label">签收标记:</span>
                  <span class="val" data-test="item-sign-status">{{ item.signStatus }}</span>
                </div>
              </div>
            </div>

            <el-empty
              v-if="!detail.items || detail.items.length === 0"
              description="该对账单无明细发生额"
              data-test="empty-items"
            />
          </div>
        </div>

        <!-- 底部吸底操作栏（仅 SENT 待确认状态显示） -->
        <div v-if="detail.status === 'SENT'" class="bottom-action-bar" data-test="action-bar">
          <div class="action-buttons">
            <el-button
              type="danger"
              plain
              class="action-btn"
              data-test="btn-open-reject"
              @click="openRejectDialog"
            >
              驳回对账
            </el-button>
            <el-button
              type="primary"
              class="action-btn"
              data-test="btn-open-confirm"
              @click="openConfirmDialog"
            >
              确认对账
            </el-button>
          </div>
        </div>
      </template>

      <!-- 确认对账弹窗 -->
      <el-dialog
        v-model="confirmDialogVisible"
        title="确认对账单"
        width="90%"
        class="portal-action-dialog"
        :append-to-body="true"
        :close-on-click-modal="false"
        data-test="confirm-dialog"
      >
        <el-form
          ref="confirmFormRef"
          :model="confirmForm"
          :rules="confirmRules"
          label-position="top"
          class="dialog-form"
        >
          <div class="dialog-tips">
            请核对对账单期末欠款金额【¥{{ formatAmount(detail?.closingBalance) }}】，确认无误后请输入您的姓名提交。
          </div>
          <el-form-item label="确认人姓名" prop="confirmByName" required>
            <el-input
              v-model="confirmForm.confirmByName"
              placeholder="请输入确认人真实姓名"
              maxlength="50"
              clearable
              data-test="input-confirm-by"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button data-test="btn-confirm-cancel" @click="confirmDialogVisible = false">取消</el-button>
            <el-button
              type="primary"
              :loading="submitting"
              data-test="btn-confirm-submit"
              @click="handleConfirmSubmit"
            >
              确认提交
            </el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 驳会对账弹窗 -->
      <el-dialog
        v-model="rejectDialogVisible"
        title="驳回对账单"
        width="90%"
        class="portal-action-dialog"
        :append-to-body="true"
        :close-on-click-modal="false"
        data-test="reject-dialog"
      >
        <el-form
          ref="rejectFormRef"
          :model="rejectForm"
          :rules="rejectRules"
          label-position="top"
          class="dialog-form"
        >
          <el-form-item label="驳回原因说明" prop="rejectReason" required>
            <el-input
              v-model="rejectForm.rejectReason"
              type="textarea"
              :rows="4"
              placeholder="请详细描述金额或单据有误的具体原因，以便财务核对修正"
              maxlength="200"
              show-word-limit
              data-test="input-reject-reason"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button data-test="btn-reject-cancel" @click="rejectDialogVisible = false">取消</el-button>
            <el-button
              type="danger"
              :loading="submitting"
              data-test="btn-reject-submit"
              @click="handleRejectSubmit"
            >
              确认驳回
            </el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </PortalLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PortalLayout from '@/views/portal/PortalLayout.vue'
import { usePortalAuth } from '@/views/portal/usePortalAuth'
import {
  getPortalStatementDetail,
  confirmPortalStatement,
  rejectPortalStatement,
  type PortalStatementDetailRespVO
} from '@/api/portal/statement'

const route = useRoute()
const router = useRouter()
const { customerName, loading, bootstrapToken } = usePortalAuth()

const detail = ref<PortalStatementDetailRespVO | null>(null)
const detailLoading = ref(false)

// 弹窗与提交状态
const confirmDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const submitting = ref(false)

const confirmFormRef = ref<FormInstance>()
const confirmForm = reactive({
  confirmByName: ''
})
const confirmRules: FormRules = {
  confirmByName: [
    { required: true, message: '确认人姓名不能为空', trigger: 'blur' },
    { min: 1, max: 50, message: '姓名长度在 1 到 50 个字符之间', trigger: 'blur' }
  ]
}

const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({
  rejectReason: ''
})
const rejectRules: FormRules = {
  rejectReason: [
    { required: true, message: '驳回原因不能为空', trigger: 'blur' },
    { min: 2, max: 200, message: '驳回原因长度在 2 到 200 个字符之间', trigger: 'blur' }
  ]
}

const getStatusTagType = (status?: string): '' | 'success' | 'warning' | 'danger' | 'info' => {
  switch (status) {
    case 'SENT':
      return 'warning'
    case 'CONFIRMED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    default:
      return 'info'
  }
}

const getStatusLabel = (status?: string): string => {
  switch (status) {
    case 'SENT':
      return '待确认'
    case 'CONFIRMED':
      return '已确认'
    case 'REJECTED':
      return '已驳回'
    default:
      return status || '未知'
  }
}

const formatAmount = (num?: number | null): string => {
  if (num === undefined || num === null) return '0.00'
  return Number(num).toFixed(2)
}

const fetchDetail = async () => {
  const id = route.params.id as string
  const no = route.query.no as string

  if (!id && !no) {
    return
  }

  detailLoading.value = true
  try {
    const res = await getPortalStatementDetail({
      id: id || undefined,
      no: no || undefined
    })
    detail.value = res || null
  } catch {
    // 拦截器统一处理 401/403 跳转
  } finally {
    detailLoading.value = false
  }
}

const goBack = () => {
  router.push('/portal/statements')
}

const openConfirmDialog = () => {
  confirmForm.confirmByName = ''
  confirmDialogVisible.value = true
}

const openRejectDialog = () => {
  rejectForm.rejectReason = ''
  rejectDialogVisible.value = true
}

const handleConfirmSubmit = async () => {
  if (!confirmFormRef.value || !detail.value) return
  await confirmFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await confirmPortalStatement({
        id: detail.value!.id,
        confirmByName: confirmForm.confirmByName.trim()
      })
      ElMessage.success('对账单已成功确认')
      confirmDialogVisible.value = false
      await fetchDetail()
    } catch {
      // 错误由拦截器弹出
    } finally {
      submitting.value = false
    }
  })
}

const handleRejectSubmit = async () => {
  if (!rejectFormRef.value || !detail.value) return
  await rejectFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await rejectPortalStatement({
        id: detail.value!.id,
        rejectReason: rejectForm.rejectReason.trim()
      })
      ElMessage.success('对账单已驳回')
      rejectDialogVisible.value = false
      await fetchDetail()
    } catch {
      // 错误由拦截器弹出
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  const authOk = await bootstrapToken()
  if (authOk) {
    await fetchDetail()
  }
})
</script>

<style scoped>
.portal-statement-detail-page {
  padding: 4px 0 80px;
}

.top-nav {
  margin-bottom: 8px;
}

.back-btn {
  font-size: 13px;
  padding: 0;
}

.detail-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  margin-bottom: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 10px;
  margin-bottom: 12px;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.statement-no {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 14px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-row .label {
  color: #6b7280;
}

.info-row .value {
  color: #1f2937;
  font-weight: 500;
}

.text-danger {
  color: #dc2626 !important;
}

.financial-summary-box {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 12px;
  background: #f8fafc;
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-bottom: 12px;
}

.amount-card-item {
  display: flex;
  flex-direction: column;
}

.amount-card-item .lbl {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 2px;
}

.amount-card-item .val {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.amount-card-item .val.debit {
  color: #dc2626;
}

.amount-card-item .val.credit {
  color: #16a34a;
}

.amount-card-item .val.closing {
  color: #2563eb;
  font-size: 16px;
}

.status-info-box {
  border-radius: 6px;
  padding: 10px 12px;
  margin-top: 10px;
  font-size: 12px;
}

.confirm-box {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.reject-box {
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.box-title {
  font-weight: 600;
  margin-bottom: 6px;
  font-size: 13px;
}

.confirm-box .box-title {
  color: #166534;
}

.reject-box .box-title {
  color: #991b1b;
}

.detail-section {
  margin-top: 14px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
  padding: 0 2px;
}

.item-count {
  font-size: 12px;
  font-weight: normal;
  color: #9ca3af;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 6px;
  margin-bottom: 8px;
}

.item-date {
  font-weight: 600;
  font-size: 13px;
  color: #334155;
}

.item-direction {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
}

.tag-debit {
  background: #fee2e2;
  color: #b91c1c;
}

.tag-credit {
  background: #dcfce7;
  color: #15803d;
}

.item-body {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 12px;
}

.info-line {
  display: flex;
  gap: 6px;
}

.info-line .label {
  color: #94a3b8;
}

.info-line .val {
  color: #475569;
}

.amount-line {
  display: flex;
  justify-content: space-between;
  background: #f8fafc;
  padding: 6px 8px;
  border-radius: 4px;
  margin-top: 4px;
}

.amt-col {
  display: flex;
  gap: 4px;
}

.amt-col .label {
  color: #64748b;
}

.amt-col .val {
  font-weight: 500;
  color: #64748b;
}

.amt-col .val.has-amount {
  font-weight: 600;
  color: #0f172a;
}

.sign-tag-line {
  display: flex;
  gap: 6px;
  font-size: 11px;
  color: #94a3b8;
}

/* 底部吸底操作栏 */
.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  padding: 10px 16px;
  box-sizing: border-box;
  z-index: 100;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.action-btn {
  flex: 1;
  height: 42px;
  font-size: 15px;
}

.dialog-tips {
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  padding: 8px 10px;
  color: #1e40af;
  font-size: 12px;
  margin-bottom: 12px;
  line-height: 1.4;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
