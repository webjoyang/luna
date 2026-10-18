import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  sendCustomerStatement,
  voidCustomerStatement
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'

/**
 * 客户对账单业务动作提交助手（遵循 coding-standards.md §6 DRY Submit Helpers 单一来源规约）
 */
export function useCustomerStatementActions() {
  const actionLoading = ref(false)

  /**
   * 发送客户对账单（DRAFT -> SENT）
   */
  const handleSend = async (
    statement: { id: number; statementNo?: string },
    onDone?: () => Promise<void> | void
  ) => {
    await ElMessageBox.confirm(
      `确定要将客户对账单【${statement.statementNo || statement.id}】发送给客户吗？发送后客户可在门户查阅并进行核对确认。`,
      '发送确认',
      {
        confirmButtonText: '确定发送',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    actionLoading.value = true
    try {
      await sendCustomerStatement(statement.id)
      ElMessage.success('对账单已成功发送')
      if (onDone) {
        await onDone()
      }
    } finally {
      actionLoading.value = false
    }
  }

  /**
   * 作废客户对账单（非终态 -> VOID）
   */
  const handleVoid = async (
    statement: { id: number; statementNo?: string },
    onDone?: () => Promise<void> | void
  ) => {
    await ElMessageBox.confirm(
      `确定要作废客户对账单【${statement.statementNo || statement.id}】吗？作废后已占用的明细分录将释放，单据无法恢复。`,
      '作废确认',
      {
        confirmButtonText: '确定作废',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    actionLoading.value = true
    try {
      await voidCustomerStatement(statement.id)
      ElMessage.success('对账单已成功作废')
      if (onDone) {
        await onDone()
      }
    } finally {
      actionLoading.value = false
    }
  }

  return {
    actionLoading,
    handleSend,
    handleVoid
  }
}
