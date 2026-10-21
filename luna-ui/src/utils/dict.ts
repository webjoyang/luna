/**
 * 数据字典工具类
 */
import { useDictStoreWithOut } from '@/store/modules/dict'
import { ElementPlusInfoType } from '@/types/elementPlus'

const dictStore = useDictStoreWithOut()

/**
 * 获取 dictType 对应的数据字典数组
 *
 * @param dictType 数据类型
 * @returns {*|Array} 数据字典数组
 */
export interface DictDataType {
  dictType: string
  label: string
  value: string | number | boolean
  colorType: ElementPlusInfoType | ''
  cssClass: string
}

export interface NumberDictDataType extends DictDataType {
  value: number
}

export interface StringDictDataType extends DictDataType {
  value: string
}

export interface BooleanDictDataType extends DictDataType {
  value: boolean
}

const defaultApprovalDicts: Record<string, DictDataType[]> = {
  lunaerp_approval_biz_type: [
    { dictType: 'lunaerp_approval_biz_type', label: '销售合同', value: 'sales_contract', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_approval_biz_type', label: '付款申请', value: 'payment', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_approval_biz_type', label: '费用报销', value: 'expense', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_approval_biz_type', label: '工资单', value: 'payroll', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_approval_biz_type', label: '销售退货', value: 'sales_return', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_approval_node_type: [
    { dictType: 'lunaerp_approval_node_type', label: '逐级审批', value: 'SEQUENTIAL', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_approval_node_type', label: '或签', value: 'OR_SIGN', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_approval_node_type', label: '会签', value: 'AND_SIGN', colorType: 'success', cssClass: '' }
  ],
  lunaerp_approval_approver_type: [
    { dictType: 'lunaerp_approval_approver_type', label: '指定用户', value: 'USER', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_approval_approver_type', label: '指定岗位', value: 'POST', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_approval_approver_type', label: '指定角色', value: 'ROLE', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_approval_approver_type', label: '部门负责人', value: 'DEPT_LEADER', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_approval_approver_type', label: '提交人直属上级', value: 'SUBMITTER_LEADER', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_approval_config_status: [
    { dictType: 'lunaerp_approval_config_status', label: '启用', value: 'ENABLED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_approval_config_status', label: '停用', value: 'DISABLED', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_approval_instance_status: [
    { dictType: 'lunaerp_approval_instance_status', label: '草稿', value: 'DRAFT', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_approval_instance_status', label: '审批中', value: 'APPROVING', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_approval_instance_status', label: '已通过', value: 'APPROVED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_approval_instance_status', label: '已驳回', value: 'REJECTED', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_approval_instance_status', label: '已撤回', value: 'CANCELLED', colorType: 'info', cssClass: '' }
  ],
  lunaerp_approval_task_action: [
    { dictType: 'lunaerp_approval_task_action', label: '通过', value: 'DONE', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_approval_task_action', label: '驳回', value: 'REJECTED', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_approval_task_action', label: '转交', value: 'TRANSFERRED', colorType: 'warning', cssClass: '' }
  ],
  lunaerp_approval_record_action: [
    { dictType: 'lunaerp_approval_record_action', label: '提交审批', value: 'SUBMIT', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_approval_record_action', label: '通过', value: 'APPROVE', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_approval_record_action', label: '驳回', value: 'REJECT', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_approval_record_action', label: '转交', value: 'TRANSFER', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_approval_record_action', label: '撤回', value: 'WITHDRAW', colorType: 'info', cssClass: '' }
  ],
  lunaerp_export_doc_status: [
    { dictType: 'lunaerp_export_doc_status', label: '草稿', value: 'DRAFT', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_export_doc_status', label: '已定稿', value: 'FINALIZED', colorType: 'success', cssClass: '' }
  ],
  lunaerp_incoterm: [
    { dictType: 'lunaerp_incoterm', label: 'FOB', value: 'FOB', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_incoterm', label: 'CIF', value: 'CIF', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_incoterm', label: 'CFR', value: 'CFR', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_incoterm', label: 'EXW', value: 'EXW', colorType: 'warning', cssClass: '' }
  ],
  lunaerp_payment_term: [
    { dictType: 'lunaerp_payment_term', label: 'T/T', value: 'TT', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_payment_term', label: 'L/C', value: 'LC', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_payment_term', label: 'D/P', value: 'DP', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_payment_term', label: 'D/A', value: 'DA', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_payment_term', label: 'O/A', value: 'OA', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_transport_mode: [
    { dictType: 'lunaerp_transport_mode', label: '海运', value: 'SEA', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_transport_mode', label: '空运', value: 'AIR', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_transport_mode', label: '陆运', value: 'LAND', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_transport_mode', label: '快递', value: 'EXPRESS', colorType: 'warning', cssClass: '' }
  ],
  lunaerp_cost_card_status: [
    { dictType: 'lunaerp_cost_card_status', label: '草稿', value: 'DRAFT', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_cost_card_status', label: '生效', value: 'EFFECTIVE', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_cost_card_status', label: '失效', value: 'EXPIRED', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_cost_card_basis: [
    { dictType: 'lunaerp_cost_card_basis', label: '按千克', value: 'KG', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_cost_card_basis', label: '按米', value: 'METER', colorType: 'success', cssClass: '' }
  ],
  lunaerp_cost_card_fee_type: [
    { dictType: 'lunaerp_cost_card_fee_type', label: '织造', value: 'WEAVING', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_cost_card_fee_type', label: '染色', value: 'DYEING', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_cost_card_fee_type', label: '定型', value: 'SETTING', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_cost_card_fee_type', label: '检验', value: 'INSPECTION', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_cost_card_fee_type', label: '包装', value: 'PACKAGING', colorType: '', cssClass: '' },
    { dictType: 'lunaerp_cost_card_fee_type', label: '运费', value: 'FREIGHT', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_cost_fee_unit: [
    { dictType: 'lunaerp_cost_fee_unit', label: '元/千克', value: 'PER_KG', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_cost_fee_unit', label: '元/米', value: 'PER_METER', colorType: 'success', cssClass: '' }
  ],
  lunaerp_yarn_price_type: [
    { dictType: 'lunaerp_yarn_price_type', label: '采购参考价', value: 'PURCHASE', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_yarn_price_type', label: '市场参考价', value: 'MARKET', colorType: 'warning', cssClass: '' }
  ],
  lunaerp_quotation_status: [
    { dictType: 'lunaerp_quotation_status', label: '草稿', value: 'DRAFT', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_quotation_status', label: '已确认', value: 'CONFIRMED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_quotation_status', label: '已修订', value: 'SUPERSEDED', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_quotation_status', label: '已取消', value: 'CANCELLED', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_quotation_status', label: '已失效', value: 'EXPIRED', colorType: 'info', cssClass: '' }
  ],
  lunaerp_quotation_cost_source: [
    { dictType: 'lunaerp_quotation_cost_source', label: '成本卡', value: 'CARD', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_quotation_cost_source', label: '手工', value: 'MANUAL', colorType: 'warning', cssClass: '' }
  ],
  lunaerp_color_light_source: [
    { dictType: 'lunaerp_color_light_source', label: '标准光源 D65', value: 'D65', colorType: '', cssClass: '' },
    { dictType: 'lunaerp_color_light_source', label: 'TL84 三基色', value: 'TL84', colorType: '', cssClass: '' },
    { dictType: 'lunaerp_color_light_source', label: 'CWF 冷白荧光', value: 'CWF', colorType: '', cssClass: '' },
    { dictType: 'lunaerp_color_light_source', label: 'A 光源/白炽灯', value: 'A', colorType: '', cssClass: '' },
    { dictType: 'lunaerp_color_light_source', label: 'UV 紫外光源', value: 'UV', colorType: '', cssClass: '' },
    { dictType: 'lunaerp_color_light_source', label: 'TL83 暖白荧光', value: 'TL83', colorType: '', cssClass: '' }
  ],
  lunaerp_color_line_status: [
    { dictType: 'lunaerp_color_line_status', label: '待结果', value: 'PENDING', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_color_line_status', label: '通过', value: 'APPROVED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_color_line_status', label: '驳回', value: 'REJECTED', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_color_instruction_status: [
    { dictType: 'lunaerp_color_instruction_status', label: '待打色', value: 'PENDING', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_color_instruction_status', label: '打色中', value: 'IN_PROGRESS', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_color_instruction_status', label: '已确认', value: 'CONFIRMED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_color_instruction_status', label: '已投产', value: 'IN_PRODUCTION', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_color_instruction_status', label: '已作废', value: 'VOID', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_supplier_recon_status: [
    { dictType: 'lunaerp_supplier_recon_status', label: '草稿', value: 'DRAFT', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_supplier_recon_status', label: '已确认', value: 'CONFIRMED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_supplier_recon_status', label: '已作废', value: 'CANCELLED', colorType: 'info', cssClass: '' }
  ],
  lunaerp_payment_apply_type: [
    { dictType: 'lunaerp_payment_apply_type', label: '对账付款', value: 'RECON', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_payment_apply_type', label: '预付付款', value: 'PREPAY', colorType: 'success', cssClass: '' }
  ],
  lunaerp_finance_payment_direction: [
    { dictType: 'lunaerp_finance_payment_direction', label: '收款', value: 'IN', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_finance_payment_direction', label: '付款', value: 'OUT', colorType: 'warning', cssClass: '' }
  ],
  lunaerp_finance_account_type: [
    { dictType: 'lunaerp_finance_account_type', label: '现金', value: 'CASH', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_finance_account_type', label: '银行账户', value: 'BANK', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_finance_account_type', label: '微信支付', value: 'WECHAT', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_finance_account_type', label: '支付宝', value: 'ALIPAY', colorType: 'info', cssClass: '' }
  ],
  lunaerp_finance_account_nature: [
    { dictType: 'lunaerp_finance_account_nature', label: '公户', value: 'PUBLIC', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_finance_account_nature', label: '私户', value: 'PRIVATE', colorType: 'warning', cssClass: '' }
  ],
  lunaerp_finance_account_status: [
    { dictType: 'lunaerp_finance_account_status', label: '启用', value: 'ENABLE', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_finance_account_status', label: '停用', value: 'DISABLE', colorType: 'info', cssClass: '' }
  ],
  lunaerp_other_txn_category: [
    { dictType: 'lunaerp_other_txn_category', label: '利息收入', value: 'INTEREST_IN', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_other_txn_category', label: '废品收入', value: 'WASTE_IN', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_other_txn_category', label: '其他收入', value: 'OTHER_IN', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_other_txn_category', label: '手续费', value: 'FEE_OUT', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_other_txn_category', label: '工本费', value: 'COST_OUT', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_other_txn_category', label: '其他支出', value: 'OTHER_OUT', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_finance_ledger_category: [
    { dictType: 'lunaerp_finance_ledger_category', label: '员工报销支出', value: 'OUT_EXPENSE', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_finance_ledger_category', label: '员工工资发放', value: 'OUT_SALARY', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_finance_ledger_category', label: '账户转账转入', value: 'TRANSFER_IN', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_finance_ledger_category', label: '账户转账转出', value: 'TRANSFER_OUT', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_finance_ledger_category', label: '其他收入入账', value: 'IN_OTHER', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_finance_ledger_category', label: '其他支出流出', value: 'OUT_OTHER', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_approval_status: [
    { dictType: 'lunaerp_approval_status', label: '待提交', value: 'PENDING', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_approval_status', label: '审批中', value: 'APPROVING', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_approval_status', label: '已通过', value: 'APPROVED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_approval_status', label: '已驳回', value: 'REJECTED', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_approval_status', label: '已撤回', value: 'CANCELLED', colorType: 'info', cssClass: '' }
  ],
  lunaerp_expense_category: [
    { dictType: 'lunaerp_expense_category', label: '差旅费', value: 'TRAVEL', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_expense_category', label: '办公费', value: 'OFFICE', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_expense_category', label: '运输费', value: 'TRANSPORT', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_expense_category', label: '业务招待费', value: 'ENTERTAIN', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_expense_category', label: '房租', value: 'RENT', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_expense_category', label: '水电费', value: 'UTILITY', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_expense_category', label: '通讯费', value: 'COMM', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_expense_category', label: '其他', value: 'OTHER', colorType: 'info', cssClass: '' }
  ],
  lunaerp_payroll_earning_type: [
    { dictType: 'lunaerp_payroll_earning_type', label: '基本工资', value: 'BASE', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_payroll_earning_type', label: '计件工资', value: 'PIECE', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_payroll_earning_type', label: '提成', value: 'COMMISSION', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_payroll_earning_type', label: '奖金', value: 'BONUS', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_payroll_earning_type', label: '补贴', value: 'SUBSIDY', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_payroll_earning_type', label: '其他收入', value: 'OTHER', colorType: 'default', cssClass: '' }
  ],
  lunaerp_payroll_deduction_type: [
    { dictType: 'lunaerp_payroll_deduction_type', label: '次品扣款', value: 'DEFECT', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_payroll_deduction_type', label: '其他扣款', value: 'OTHER', colorType: 'info', cssClass: '' }
  ],
  lunaerp_payroll_payment_status: [
    { dictType: 'lunaerp_payroll_payment_status', label: '未发放', value: 'UNPAID', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_payroll_payment_status', label: '部分发放', value: 'PARTIAL', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_payroll_payment_status', label: '已发清', value: 'PAID', colorType: 'success', cssClass: '' }
  ],
  // ========== LunaERP - 车间移动 H5 模块默认回退字典 ==========
  lunaerp_weaving_exception_type: [
    { dictType: 'lunaerp_weaving_exception_type', label: '断纱', value: 'YARN_BREAK', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_weaving_exception_type', label: '坏布', value: 'CLOTH_DAMAGE', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_weaving_exception_type', label: '缺纱停机', value: 'YARN_SHORTAGE', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_weaving_exception_type', label: '换机', value: 'MACHINE_CHANGE', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_weaving_exception_type', label: '换人', value: 'WORKER_CHANGE', colorType: 'info', cssClass: '' }
  ],
  lunaerp_inspection_grade: [
    { dictType: 'lunaerp_inspection_grade', label: '一等品', value: 'A', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_inspection_grade', label: '二等品', value: 'B', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_inspection_grade', label: '三等品', value: 'C', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_inspection_grade', label: '等外品', value: 'DEFECTIVE', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_weaving_ticket_status: [
    { dictType: 'lunaerp_weaving_ticket_status', label: '已生成', value: 'GENERATED', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_weaving_ticket_status', label: '已开工', value: 'STARTED', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_weaving_ticket_status', label: '已完工', value: 'COMPLETED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_weaving_ticket_status', label: '已检验', value: 'INSPECTED', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_weaving_ticket_status', label: '已入库', value: 'STOCKED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_weaving_ticket_status', label: '异常', value: 'EXCEPTION', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_weaving_ticket_status', label: '作废', value: 'VOID', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_weaving_task_status: [
    { dictType: 'lunaerp_weaving_task_status', label: '待开始', value: 'PENDING', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_weaving_task_status', label: '生产中', value: 'IN_PROGRESS', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_weaving_task_status', label: '暂停异常', value: 'HALTED', colorType: 'danger', cssClass: '' },
    { dictType: 'lunaerp_weaving_task_status', label: '已完工', value: 'COMPLETED', colorType: 'success', cssClass: '' }
  ],
  lunaerp_yarn_requisition_status: [
    { dictType: 'lunaerp_yarn_requisition_status', label: '草稿', value: 'DRAFT', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_yarn_requisition_status', label: '待发纱', value: 'ACTIVE', colorType: 'primary', cssClass: '' },
    { dictType: 'lunaerp_yarn_requisition_status', label: '部分发纱', value: 'PARTIAL', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_yarn_requisition_status', label: '已发纱', value: 'DISPATCHED', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_yarn_requisition_status', label: '已关闭', value: 'CLOSED', colorType: 'danger', cssClass: '' }
  ],
  lunaerp_inspection_order_status: [
    { dictType: 'lunaerp_inspection_order_status', label: '草稿', value: 'DRAFT', colorType: 'warning', cssClass: '' },
    { dictType: 'lunaerp_inspection_order_status', label: '录检中', value: 'ACTIVE', colorType: 'success', cssClass: '' },
    { dictType: 'lunaerp_inspection_order_status', label: '录检完成', value: 'COMPLETED', colorType: 'info', cssClass: '' },
    { dictType: 'lunaerp_inspection_order_status', label: '已取消', value: 'CANCELLED', colorType: 'danger', cssClass: '' }
  ]
}

export const getDictOptions = (dictType: string) => {
  const options = dictStore.getDictByType(dictType)
  if (options && options.length > 0) {
    return options
  }
  return defaultApprovalDicts[dictType] || []
}

export const getIntDictOptions = (dictType: string): NumberDictDataType[] => {
  // 获得通用的 DictDataType 列表
  const dictOptions: DictDataType[] = getDictOptions(dictType)
  // 转换成 number 类型的 NumberDictDataType 类型
  // why 需要特殊转换：避免 IDEA 在 v-for="dict in getIntDictOptions(...)" 时，el-option 的 key 会告警
  const dictOption: NumberDictDataType[] = []
  dictOptions.forEach((dict: DictDataType) => {
    dictOption.push({
      ...dict,
      value: parseInt(dict.value + '')
    })
  })
  return dictOption
}

export const getStrDictOptions = (dictType: string) => {
  // 获得通用的 DictDataType 列表
  const dictOptions: DictDataType[] = getDictOptions(dictType)
  // 转换成 string 类型的 StringDictDataType 类型
  // why 需要特殊转换：避免 IDEA 在 v-for="dict in getStrDictOptions(...)" 时，el-option 的 key 会告警
  const dictOption: StringDictDataType[] = []
  dictOptions.forEach((dict: DictDataType) => {
    dictOption.push({
      ...dict,
      value: dict.value + ''
    })
  })
  return dictOption
}

export const getBoolDictOptions = (dictType: string): BooleanDictDataType[] => {
  const dictOption: BooleanDictDataType[] = []
  const dictOptions: DictDataType[] = getDictOptions(dictType)
  dictOptions.forEach((dict: DictDataType) => {
    dictOption.push({
      ...dict,
      value: dict.value + '' === 'true'
    })
  })
  return dictOption
}

/**
 * 获取指定字典类型的指定值对应的字典对象
 * @param dictType 字典类型
 * @param value 字典值
 * @return DictDataType 字典对象
 */
export const getDictObj = (dictType: string, value: any): DictDataType | undefined => {
  const dictOptions: DictDataType[] = getDictOptions(dictType)
  for (const dict of dictOptions) {
    if (dict.value === value + '') {
      return dict
    }
  }
}

/**
 * 获得字典数据的文本展示
 *
 * @param dictType 字典类型
 * @param value 字典数据的值
 * @return 字典名称
 */
export const getDictLabel = (dictType: string, value: any): string => {
  const dictOptions: DictDataType[] = getDictOptions(dictType)
  const dictLabel = ref('')
  dictOptions.forEach((dict: DictDataType) => {
    if (dict.value === value + '') {
      dictLabel.value = dict.label
    }
  })
  return dictLabel.value
}

export enum DICT_TYPE {
  USER_TYPE = 'user_type',
  COMMON_STATUS = 'common_status',
  TERMINAL = 'terminal', // 终端
  DATE_INTERVAL = 'date_interval', // 数据间隔

  // ========== SYSTEM 模块 ==========
  SYSTEM_USER_SEX = 'system_user_sex',
  SYSTEM_MENU_TYPE = 'system_menu_type',
  SYSTEM_ROLE_TYPE = 'system_role_type',
  SYSTEM_DATA_SCOPE = 'system_data_scope',
  SYSTEM_NOTICE_TYPE = 'system_notice_type',
  SYSTEM_LOGIN_TYPE = 'system_login_type',
  SYSTEM_LOGIN_RESULT = 'system_login_result',
  SYSTEM_SMS_CHANNEL_CODE = 'system_sms_channel_code',
  SYSTEM_SMS_TEMPLATE_TYPE = 'system_sms_template_type',
  SYSTEM_SMS_SEND_STATUS = 'system_sms_send_status',
  SYSTEM_SMS_RECEIVE_STATUS = 'system_sms_receive_status',
  SYSTEM_OAUTH2_GRANT_TYPE = 'system_oauth2_grant_type',
  SYSTEM_MAIL_SEND_STATUS = 'system_mail_send_status',
  SYSTEM_NOTIFY_TEMPLATE_TYPE = 'system_notify_template_type',
  SYSTEM_SOCIAL_TYPE = 'system_social_type',

  // ========== INFRA 模块 ==========
  INFRA_BOOLEAN_STRING = 'infra_boolean_string',
  INFRA_JOB_STATUS = 'infra_job_status',
  INFRA_JOB_LOG_STATUS = 'infra_job_log_status',
  INFRA_API_ERROR_LOG_PROCESS_STATUS = 'infra_api_error_log_process_status',
  INFRA_CONFIG_TYPE = 'infra_config_type',
  INFRA_CODEGEN_TEMPLATE_TYPE = 'infra_codegen_template_type',
  INFRA_CODEGEN_FRONT_TYPE = 'infra_codegen_front_type',
  INFRA_CODEGEN_SCENE = 'infra_codegen_scene',
  INFRA_FILE_STORAGE = 'infra_file_storage',
  INFRA_OPERATE_TYPE = 'infra_operate_type',

  // ========== BPM 模块 ==========
  BPM_MODEL_TYPE = 'bpm_model_type',
  BPM_MODEL_FORM_TYPE = 'bpm_model_form_type',
  BPM_TASK_CANDIDATE_STRATEGY = 'bpm_task_candidate_strategy',
  BPM_PROCESS_INSTANCE_STATUS = 'bpm_process_instance_status',
  BPM_TASK_STATUS = 'bpm_task_status',
  BPM_COMMENT_TYPE = 'bpm_comment_type',
  BPM_OA_LEAVE_TYPE = 'bpm_oa_leave_type',
  BPM_PROCESS_LISTENER_TYPE = 'bpm_process_listener_type',
  BPM_PROCESS_LISTENER_VALUE_TYPE = 'bpm_process_listener_value_type',

  // ========== PAY 模块 ==========
  PAY_CHANNEL_CODE = 'pay_channel_code', // 支付渠道编码类型
  PAY_ORDER_STATUS = 'pay_order_status', // 商户支付订单状态
  PAY_REFUND_STATUS = 'pay_refund_status', // 退款订单状态
  PAY_NOTIFY_STATUS = 'pay_notify_status', // 商户支付回调状态
  PAY_NOTIFY_TYPE = 'pay_notify_type', // 商户支付回调状态
  PAY_TRANSFER_TYPE = 'pay_transfer_type', // 转账订单类型
  PAY_TRANSFER_STATUS = 'pay_transfer_status', // 转账订单状态

  // ========== MP 模块 ==========
  MP_AUTO_REPLY_REQUEST_MATCH = 'mp_auto_reply_request_match', // 自动回复请求匹配类型
  MP_MESSAGE_TYPE = 'mp_message_type', // 消息类型

  // ========== Member 会员模块 ==========
  MEMBER_POINT_BIZ_TYPE = 'member_point_biz_type', // 积分的业务类型
  MEMBER_EXPERIENCE_BIZ_TYPE = 'member_experience_biz_type', // 会员经验业务类型

  // ========== MALL - 商品模块 ==========
  PRODUCT_SPU_STATUS = 'product_spu_status', //商品状态

  // ========== MALL - 交易模块 ==========
  EXPRESS_CHARGE_MODE = 'trade_delivery_express_charge_mode', //快递的计费方式
  TRADE_AFTER_SALE_STATUS = 'trade_after_sale_status', // 售后 - 状态
  TRADE_AFTER_SALE_WAY = 'trade_after_sale_way', // 售后 - 方式
  TRADE_AFTER_SALE_TYPE = 'trade_after_sale_type', // 售后 - 类型
  TRADE_ORDER_TYPE = 'trade_order_type', // 订单 - 类型
  TRADE_ORDER_STATUS = 'trade_order_status', // 订单 - 状态
  TRADE_ORDER_ITEM_AFTER_SALE_STATUS = 'trade_order_item_after_sale_status', // 订单项 - 售后状态
  TRADE_DELIVERY_TYPE = 'trade_delivery_type', // 配送方式
  BROKERAGE_ENABLED_CONDITION = 'brokerage_enabled_condition', // 分佣模式
  BROKERAGE_BIND_MODE = 'brokerage_bind_mode', // 分销关系绑定模式
  BROKERAGE_BANK_NAME = 'brokerage_bank_name', // 佣金提现银行
  BROKERAGE_WITHDRAW_TYPE = 'brokerage_withdraw_type', // 佣金提现类型
  BROKERAGE_RECORD_BIZ_TYPE = 'brokerage_record_biz_type', // 佣金业务类型
  BROKERAGE_RECORD_STATUS = 'brokerage_record_status', // 佣金状态
  BROKERAGE_WITHDRAW_STATUS = 'brokerage_withdraw_status', // 佣金提现状态

  // ========== MALL - 营销模块 ==========
  PROMOTION_DISCOUNT_TYPE = 'promotion_discount_type', // 优惠类型
  PROMOTION_PRODUCT_SCOPE = 'promotion_product_scope', // 营销的商品范围
  PROMOTION_COUPON_TEMPLATE_VALIDITY_TYPE = 'promotion_coupon_template_validity_type', // 优惠劵模板的有限期类型
  PROMOTION_COUPON_STATUS = 'promotion_coupon_status', // 优惠劵的状态
  PROMOTION_COUPON_TAKE_TYPE = 'promotion_coupon_take_type', // 优惠劵的领取方式
  PROMOTION_CONDITION_TYPE = 'promotion_condition_type', // 营销的条件类型枚举
  PROMOTION_BARGAIN_RECORD_STATUS = 'promotion_bargain_record_status', // 砍价记录的状态
  PROMOTION_COMBINATION_RECORD_STATUS = 'promotion_combination_record_status', // 拼团记录的状态
  PROMOTION_BANNER_POSITION = 'promotion_banner_position', // banner 定位

  // ========== CRM - 客户管理模块 ==========
  CRM_AUDIT_STATUS = 'crm_audit_status', // CRM 审批状态
  CRM_BIZ_TYPE = 'crm_biz_type', // CRM 业务类型
  CRM_BUSINESS_END_STATUS_TYPE = 'crm_business_end_status_type', // CRM 商机结束状态类型
  CRM_RECEIVABLE_RETURN_TYPE = 'crm_receivable_return_type', // CRM 回款的还款方式
  CRM_CUSTOMER_INDUSTRY = 'crm_customer_industry', // CRM 客户所属行业
  CRM_CUSTOMER_LEVEL = 'crm_customer_level', // CRM 客户级别
  CRM_CUSTOMER_SOURCE = 'crm_customer_source', // CRM 客户来源
  CRM_PRODUCT_STATUS = 'crm_product_status', // CRM 商品状态
  CRM_PERMISSION_LEVEL = 'crm_permission_level', // CRM 数据权限的级别
  CRM_PRODUCT_UNIT = 'crm_product_unit', // CRM 产品单位
  CRM_FOLLOW_UP_TYPE = 'crm_follow_up_type', // CRM 跟进方式

  // ========== PMS - 项目管理模块 ==========
  PMS_PROJECT_TYPE = 'pms_project_type', // PMS 项目类型
  PMS_PROJECT_STATUS = 'pms_project_status', // PMS 项目状态
  PMS_PROJECT_LEVEL = 'pms_project_level', // PMS 项目优先级
  PMS_PROJECT_GROUP_TYPE = 'pms_project_group_type', // PMS 项目分组类型
  PMS_PROJECT_SCENE_TYPE = 'pms_project_scene_type', // PMS 项目列表场景
  PMS_PROJECT_SORT_TYPE = 'pms_project_sort_type', // PMS 项目排序类型
  PMS_PROJECT_MEMBER_LEVEL = 'pms_project_member_level', // PMS 项目成员权限级别
  PMS_ITERATION_STATUS = 'pms_iteration_status', // PMS 迭代状态
  PMS_WORK_ITEM_TYPE = 'pms_work_item_type', // PMS 工作项类型
  PMS_WORK_ITEM_STATUS_TYPE = 'pms_work_item_status_type', // PMS 工作项语义状态
  PMS_WORK_ITEM_LIFECYCLE_STATUS = 'pms_work_item_lifecycle_status', // PMS 工作项生命周期状态
  PMS_WORK_ITEM_PRIORITY = 'pms_work_item_priority', // PMS 工作项优先级
  PMS_WORK_ITEM_DEFECT_TYPE = 'pms_work_item_defect_type', // PMS 工作项缺陷类型
  PMS_KNOWLEDGE_OBJECT_TYPE = 'pms_knowledge_object_type', // PMS 知识对象类型
  PMS_KNOWLEDGE_DOCUMENT_TYPE = 'pms_knowledge_document_type', // PMS 知识文档类型
  PMS_KNOWLEDGE_DOCUMENT_STATUS = 'pms_knowledge_document_status', // PMS 知识文档状态
  PMS_KNOWLEDGE_CONTENT_LEVEL = 'pms_knowledge_content_level', // PMS 知识内容协作等级
  PMS_KNOWLEDGE_GROUP_TYPE = 'pms_knowledge_group_type', // PMS 知识库分组类型
  PMS_KNOWLEDGE_LIBRARY_MEMBER_LEVEL = 'pms_knowledge_library_member_level', // PMS 知识库成员等级

  // ========== OA - 办公自动化模块 ==========
  OA_ATTENDANCE_TYPE = 'oa_attendance_type', // OA 考勤类型
  OA_ATTENDANCE_STATUS = 'oa_attendance_status', // OA 考勤状态

  // ========== FMS - 财务管理模块 ==========
  FMS_ACCOUNT_USER_LEVEL = 'fms_account_user_level', // FMS 账套用户权限级别
  FMS_SUBJECT_CATEGORY = 'fms_subject_category', // FMS 科目类别
  FMS_DEBIT_CREDIT_DIRECTION = 'fms_debit_credit_direction', // FMS 借贷方向
  FMS_FINANCE_INDICATOR_TYPE = 'fms_finance_indicator_type', // FMS 财务指标取数报表类型
  FMS_SUBJECT_TYPE = 'fms_subject_type', // FMS 科目类型
  FMS_AUXILIARY_TYPE = 'fms_auxiliary_type', // FMS 辅助核算类别
  FMS_VOUCHER_STATUS = 'fms_voucher_status', // FMS 凭证状态
  FMS_VOUCHER_TIDY_TYPE = 'fms_voucher_tidy_type', // FMS 凭证整理方式
  FMS_FORMULA_RULE = 'fms_formula_rule', // FMS 报表公式取数规则
  FMS_REPORT_TYPE = 'fms_report_type', // FMS 财务报表类型
  FMS_REPORT_PERIOD_TYPE = 'fms_report_period_type', // FMS 财务报表期间类型
  FMS_LEDGER_BALANCE_MODE = 'fms_ledger_balance_mode', // FMS 账簿余额方向模式
  FMS_ACCOUNTING_STANDARD = 'fms_accounting_standard', // FMS 会计制度
  FMS_CLOSING_TYPE = 'fms_closing_type', // FMS 结账方案类型
  FMS_CLOSING_TIME_TYPE = 'fms_closing_time_type', // FMS 结账取数期间
  FMS_CLOSING_VOUCHER_TYPE = 'fms_closing_voucher_type', // FMS 结账凭证类型
  FMS_CLOSING_TEMPLATE_CATEGORY = 'fms_closing_template_category', // FMS 结账模板分类

  // ========== HRM - 人力资源模块 ==========
  HRM_EMPLOYEE_ENTRY_STATUS = 'hrm_employee_entry_status', // HRM 员工入职状态
  HRM_EMPLOYEE_STATUS = 'hrm_employee_status', // HRM 员工状态
  HRM_EMPLOYEE_TYPE = 'hrm_employee_type', // HRM 聘用形式
  HRM_EMPLOYEE_EDUCATION = 'hrm_employee_education', // HRM 员工学历
  HRM_RECRUIT_POST_STATUS = 'hrm_recruit_post_status', // HRM 招聘职位状态
  HRM_RECRUIT_JOB_NATURE = 'hrm_recruit_job_nature', // HRM 招聘工作性质
  HRM_RECRUIT_WORK_TIME = 'hrm_recruit_work_time', // HRM 招聘工作经验
  HRM_RECRUIT_POST_EDUCATION = 'hrm_recruit_post_education', // HRM 招聘职位学历要求
  HRM_RECRUIT_SALARY_UNIT = 'hrm_recruit_salary_unit', // HRM 招聘薪资单位
  HRM_RECRUIT_EMERGENCY_LEVEL = 'hrm_recruit_emergency_level', // HRM 招聘紧急程度
  HRM_RECRUIT_CANDIDATE_STATUS = 'hrm_recruit_candidate_status', // HRM 招聘候选人状态
  HRM_RECRUIT_CANDIDATE_EDUCATION = 'hrm_recruit_candidate_education', // HRM 招聘候选人学历
  HRM_RECRUIT_INTERVIEW_TYPE = 'hrm_recruit_interview_type', // HRM 招聘面试方式
  HRM_RECRUIT_INTERVIEW_RESULT = 'hrm_recruit_interview_result', // HRM 招聘面试结果
  HRM_ATTENDANCE_CLOCK_TYPE = 'hrm_attendance_clock_type', // HRM 打卡类型
  HRM_ATTENDANCE_CLOCK_SOURCE = 'hrm_attendance_clock_source', // HRM 打卡来源
  HRM_ATTENDANCE_CLOCK_STATUS = 'hrm_attendance_clock_status', // HRM 打卡状态
  HRM_ATTENDANCE_HOLIDAY_TYPE = 'hrm_attendance_holiday_type', // HRM 考勤节假日类型
  HRM_ATTENDANCE_LEAVE_TYPE = 'hrm_attendance_leave_type', // HRM 请假类型
  HRM_ATTENDANCE_YES_NO = 'hrm_attendance_yes_no', // HRM 考勤是否
  HRM_ATTENDANCE_LATE_EARLY_DEDUCT_METHOD = 'hrm_attendance_late_early_deduct_method', // HRM 迟到早退扣款方式
  HRM_ATTENDANCE_ABSENTEEISM_DEDUCT_METHOD = 'hrm_attendance_absenteeism_deduct_method', // HRM 旷工扣款方式
  HRM_ATTENDANCE_MISSCARD_DEDUCT_METHOD = 'hrm_attendance_misscard_deduct_method', // HRM 缺卡扣款方式
  HRM_SALARY_CHANGE_REASON = 'hrm_salary_change_reason', // HRM 薪资调整原因
  HRM_SALARY_CHANGE_TYPE = 'hrm_salary_change_type', // HRM 薪资档案状态
  HRM_SALARY_CHANGE_RECORD_STATUS = 'hrm_salary_change_record_status', // HRM 薪资调整记录状态
  HRM_SALARY_MONTH_STATUS = 'hrm_salary_month_status', // HRM 工资表状态
  HRM_SALARY_TAX_TYPE = 'hrm_salary_tax_type', // HRM 薪资计税类型
  HRM_SALARY_OPTION_TYPE = 'hrm_salary_option_type', // HRM 薪资项类型
  HRM_SALARY_SLIP_READ_STATUS = 'hrm_salary_slip_read_status', // HRM 工资条阅读状态
  HRM_SALARY_YES_NO = 'hrm_salary_yes_no', // HRM 薪资是否
  HRM_INSURANCE_SCHEME_TYPE = 'hrm_insurance_scheme_type', // HRM 社保方案类型
  HRM_INSURANCE_PROJECT_TYPE = 'hrm_insurance_project_type', // HRM 社保项目类型
  HRM_INSURANCE_MONTH_STATUS = 'hrm_insurance_month_status', // HRM 社保表状态
  HRM_INSURANCE_EMP_STATUS = 'hrm_insurance_emp_status', // HRM 参保状态
  HRM_PERFORMANCE_PLAN_STATUS = 'hrm_performance_plan_status', // HRM 绩效计划状态
  HRM_PERFORMANCE_STAGE_STATUS = 'hrm_performance_stage_status', // HRM 绩效阶段状态
  HRM_PERFORMANCE_ASSESSMENT_STAGE_STATUS = 'hrm_performance_assessment_stage_status', // HRM 绩效考核阶段处理状态
  HRM_PERFORMANCE_APPEAL_STATUS = 'hrm_performance_appeal_status', // HRM 绩效申诉状态
  HRM_PERFORMANCE_SCORE_CALCULATION = 'hrm_performance_score_calculation', // HRM 绩效计分方式
  HRM_PERFORMANCE_UPPER_LIMIT_TYPE = 'hrm_performance_upper_limit_type', // HRM 绩效上限类型
  HRM_PERFORMANCE_YES_NO = 'hrm_performance_yes_no', // HRM 绩效是否

  // ========== ERP - 企业资源计划模块  ==========
  ERP_AUDIT_STATUS = 'erp_audit_status', // ERP 审批状态
  ERP_STOCK_RECORD_BIZ_TYPE = 'erp_stock_record_biz_type', // 库存明细的业务类型

  // ========== WMS - 仓库管理模块 ==========
  WMS_MERCHANT_TYPE = 'merchant_type', // WMS 往来企业类型
  WMS_ORDER_TYPE = 'wms_order_type', // WMS 单据类型
  WMS_ORDER_STATUS = 'wms_order_status', // WMS 单据状态
  WMS_RECEIPT_ORDER_TYPE = 'wms_receipt_order_type', // WMS 入库单类型
  WMS_SHIPMENT_ORDER_TYPE = 'wms_shipment_order_type', // WMS 出库单类型

  // ========== AI - 人工智能模块  ==========
  AI_PLATFORM = 'ai_platform', // AI 平台
  AI_MODEL_TYPE = 'ai_model_type', // AI 模型类型
  AI_IMAGE_STATUS = 'ai_image_status', // AI 图片状态
  AI_MUSIC_STATUS = 'ai_music_status', // AI 音乐状态
  AI_GENERATE_MODE = 'ai_generate_mode', // AI 生成模式
  AI_WRITE_TYPE = 'ai_write_type', // AI 写作类型
  AI_WRITE_LENGTH = 'ai_write_length', // AI 写作长度
  AI_WRITE_FORMAT = 'ai_write_format', // AI 写作格式
  AI_WRITE_TONE = 'ai_write_tone', // AI 写作语气
  AI_WRITE_LANGUAGE = 'ai_write_language', // AI 写作语言
  AI_MCP_CLIENT_NAME = 'ai_mcp_client_name', // AI MCP Client 名字

  // ========== IOT - 物联网模块  ==========
  IOT_NET_TYPE = 'iot_net_type', // IOT 联网方式
  IOT_PRODUCT_STATUS = 'iot_product_status', // IOT 产品状态
  IOT_PRODUCT_DEVICE_TYPE = 'iot_product_device_type', // IOT 产品设备类型
  IOT_PROTOCOL_TYPE = 'iot_protocol_type', // IOT 协议类型
  IOT_SERIALIZE_TYPE = 'iot_serialize_type', // IOT 序列化类型
  IOT_LOCATION_TYPE = 'iot_location_type', // IOT 定位类型
  IOT_DEVICE_STATE = 'iot_device_state', // IOT 设备状态
  IOT_THING_MODEL_TYPE = 'iot_thing_model_type', // IOT 产品功能类型
  IOT_THING_MODEL_UNIT = 'iot_thing_model_unit', // IOT 物模型单位
  IOT_RW_TYPE = 'iot_rw_type', // IOT 读写类型
  IOT_DATA_SINK_TYPE_ENUM = 'iot_data_sink_type_enum', // IoT 数据流转目的类型
  IOT_RULE_SCENE_TRIGGER_TYPE_ENUM = 'iot_rule_scene_trigger_type_enum', // IoT 场景流转的触发类型枚举
  IOT_RULE_SCENE_ACTION_TYPE_ENUM = 'iot_rule_scene_action_type_enum', // IoT 规则场景的触发类型枚举
  IOT_ALERT_LEVEL = 'iot_alert_level', // IoT 告警级别
  IOT_ALERT_RECEIVE_TYPE = 'iot_alert_receive_type', // IoT 告警接收类型
  IOT_OTA_TASK_DEVICE_SCOPE = 'iot_ota_task_device_scope', // IoT OTA任务设备范围
  IOT_OTA_TASK_STATUS = 'iot_ota_task_status', // IoT OTA 任务状态
  IOT_OTA_TASK_RECORD_STATUS = 'iot_ota_task_record_status', // IoT OTA 记录状态
  IOT_MODBUS_MODE = 'iot_modbus_mode', // IoT Modbus 工作模式
  IOT_MODBUS_FRAME_FORMAT = 'iot_modbus_frame_format', // IoT Modbus 帧格式

  // ========== MES - 制造执行系统模块  ==========
  MES_MD_ITEM_OR_PRODUCT = 'mes_md_item_or_product', // MES 物料产品标识
  MES_CLIENT_TYPE = 'mes_client_type', // MES 客户类型
  MES_VENDOR_LEVEL = 'mes_vendor_level', // MES 供应商级别
  MES_CAL_HOLIDAY_TYPE = 'mes_cal_holiday_type', // MES 假期类型
  MES_CAL_SHIFT_TYPE = 'mes_cal_shift_type', // MES 轮班方式
  MES_CAL_SHIFT_METHOD = 'mes_cal_shift_method', // MES 倒班方式
  MES_CAL_CALENDAR_TYPE = 'mes_cal_calendar_type', // MES 班组类型
  MES_CAL_PLAN_STATUS = 'mes_cal_plan_status', // MES 排班计划状态
  MES_TM_TOOL_STATUS = 'mes_tm_tool_status', // MES 工具状态
  MES_TM_MAINTEN_TYPE = 'mes_tm_mainten_type', // MES 保养维护类型
  MES_DV_MACHINERY_STATUS = 'mes_dv_machinery_status', // MES 设备状态
  MES_DV_SUBJECT_TYPE = 'mes_dv_subject_type', // MES 点检保养项目类型
  MES_INDICATOR_TYPE = 'mes_indicator_type', // MES 检测项类型
  MES_QC_RESULT_TYPE = 'mes_qc_result_type', // MES 质检结果值类型
  MES_DEFECT_LEVEL = 'mes_defect_level', // MES 缺陷等级
  MES_PRO_WORK_ORDER_STATUS = 'mes_pro_work_order_status', // MES 生产工单状态
  MES_PRO_WORK_ORDER_SOURCE_TYPE = 'mes_pro_work_order_source_type', // MES 工单来源类型
  MES_PRO_WORK_ORDER_TYPE = 'mes_pro_work_order_type', // MES 工单类型
  MES_QC_TYPE = 'mes_qc_type', // MES 质检方案类型
  MES_PRO_LINK_TYPE = 'mes_pro_link_type', // MES 工序关系类型
  MES_PRO_TASK_STATUS = 'mes_pro_task_status', // MES 生产任务状态
  MES_TIME_UNIT_TYPE = 'mes_time_unit_type', // MES 时间单位
  MES_ORDER_STATUS = 'mes_order_status', // MES 单据状态
  MES_QC_CHECK_RESULT = 'mes_qc_check_result', // MES 检测结果
  MES_QC_SOURCE_DOC_TYPE = 'mes_qc_source_doc_type', // MES 来源单据类型
  MES_IPQC_TYPE = 'mes_ipqc_type', // MES IPQC 检验类型
  MES_DV_CYCLE_TYPE = 'mes_dv_cycle_type', // MES 点检保养周期类型
  MES_DV_CHECK_PLAN_STATUS = 'mes_dv_check_plan_status', // MES 点检保养方案状态
  MES_MAINTEN_RECORD_STATUS = 'mes_mainten_record_status', // MES 保养记录状态
  MES_MAINTEN_STATUS = 'mes_mainten_status', // MES 保养结果
  MES_DV_REPAIR_STATUS = 'mes_dv_repair_status', // MES 维修工单状态
  MES_DV_REPAIR_RESULT = 'mes_dv_repair_result', // MES 维修结果
  MES_DV_CHECK_RECORD_STATUS = 'mes_dv_check_record_status', // MES 点检记录状态
  MES_DV_CHECK_RESULT = 'mes_dv_check_result', // MES 点检结果
  MES_PRO_FEEDBACK_STATUS = 'mes_pro_feedback_status', // MES 生产报工状态
  MES_PRO_FEEDBACK_TYPE = 'mes_pro_feedback_type', // MES 生产报工类型
  MES_PRO_FEEDBACK_CHANNEL = 'mes_pro_feedback_channel', // MES 生产报工途径
  MES_PRO_ANDON_STATUS = 'mes_pro_andon_status', // MES 安灯处置状态
  MES_PRO_ANDON_LEVEL = 'mes_pro_andon_level', // MES 安灯级别
  MES_PRO_WORK_RECORD_TYPE = 'mes_pro_work_record_type', // MES 上下工状态类型
  MES_RQC_TYPE = 'mes_rqc_type', // MES 退货检验类型
  MES_WM_ARRIVAL_NOTICE_STATUS = 'mes_wm_arrival_notice_status', // MES 到货通知单状态
  MES_WM_ITEM_RECEIPT_STATUS = 'mes_wm_item_receipt_status', // MES 物料接收单状态
  MES_WM_TRANSFER_STATUS = 'mes_wm_transfer_status', // MES 转移单状态
  MES_WM_TRANSFER_TYPE = 'mes_wm_transfer_type', // MES 转移单类型
  MES_WM_STOCK_TAKING_TYPE = 'mes_wm_stock_taking_type', // MES 盘点类型
  MES_WM_STOCK_TAKING_TASK_STATUS = 'mes_wm_stock_taking_task_status', // MES 盘点任务状态
  MES_WM_STOCK_TAKING_LINE_STATUS = 'mes_wm_stock_taking_task_line_status', // MES 盘点任务行状态
  MES_WM_STOCK_TAKING_PLAN_PARAM_TYPE = 'mes_wm_stock_taking_plan_param_type', // MES 盘点方案参数类型
  MES_WM_OUTSOURCE_RECPT_STATUS = 'mes_wm_outsource_recpt_status', // MES 外协入库单状态
  MES_WM_PRODUCT_ISSUE_STATUS = 'mes_wm_product_issue_status', // MES 领料出库单状态
  MES_WM_PRODUCT_PRODUCE_STATUS = 'mes_wm_product_produce_status', // MES 生产入库单状态
  MES_WM_RETURN_VENDOR_STATUS = 'mes_wm_return_vendor_status', // MES 供应商退货单状态
  MES_WM_QUALITY_STATUS = 'mes_wm_quality_status', // MES 质量状态
  MES_WM_RETURN_ISSUE_STATUS = 'mes_wm_return_issue_status', // MES 生产退料单状态
  MES_WM_RETURN_ISSUE_TYPE = 'mes_wm_return_issue_type', // MES 退料类型
  MES_WM_PRODUCT_RECPT_STATUS = 'mes_wm_product_receipt_status', // MES 成品入库单状态
  MES_WM_RETURN_SALES_STATUS = 'mes_wm_return_sales_status', // MES 销售退货单状态
  MES_WM_PRODUCT_SALES_STATUS = 'mes_wm_product_sales_status', // MES 销售出库单状态
  MES_WM_SALES_NOTICE_STATUS = 'mes_wm_sales_notice_status', // MES 发货通知单状态
  MES_WM_MISC_ISSUE_TYPE = 'mes_wm_misc_issue_type', // MES 杂项出库类型
  MES_WM_MISC_ISSUE_STATUS = 'mes_wm_misc_issue_status', // MES 杂项出库单状态
  MES_WM_MISC_RECEIPT_TYPE = 'mes_wm_misc_receipt_type', // MES 杂项单类型
  MES_WM_MISC_RECEIPT_STATUS = 'mes_wm_misc_receipt_status', // MES 杂项入库单状态
  MES_WM_OUTSOURCE_RECEIPT_STATUS = 'mes_wm_outsource_receipt_status', // MES 外协入库单状态
  MES_WM_OUTSOURCE_ISSUE_STATUS = 'mes_wm_outsource_issue_status', // MES 外协出库单状态
  MES_MD_AUTO_CODE_PART_TYPE = 'mes_md_auto_code_part_type', // MES 编码规则分段类型
  MES_MD_AUTO_CODE_PADDED_METHOD = 'mes_md_auto_code_padded_method', // MES 编码规则补齐方式
  MES_MD_AUTO_CODE_CYCLE_METHOD = 'mes_md_auto_code_cycle_method', // MES 编码规则循环方式
  MES_WM_BARCODE_FORMAT = 'mes_wm_barcode_format', // MES 条码格式
  MES_WM_BARCODE_BIZ_TYPE = 'mes_wm_barcode_biz_type', // MES 条码业务类型
  MES_WM_PACKAGE_STATUS = 'mes_wm_package_status', // MES 装箱单状态

  // ========== IM - 即时通讯模块  ==========
  IM_CONTENT_TYPE = 'im_content_type', // IM 内容类型
  IM_MESSAGE_STATUS = 'im_message_status', // IM 消息状态：0=正常 / 2=已撤回（私聊 / 群聊共用）
  IM_MESSAGE_RECEIPT_STATUS = 'im_message_receipt_status', // IM 消息回执状态：0=不需要 / 1=待完成 / 2=已完成
  IM_FRIEND_STATUS = 'im_friend_status', // IM 好友状态
  IM_FRIEND_ADD_SOURCE = 'im_friend_add_source', // IM 好友添加来源
  IM_FRIEND_REQUEST_HANDLE_RESULT = 'im_friend_request_handle_result', // IM 好友申请处理结果
  IM_GROUP_STATUS = 'im_group_status', // IM 群状态
  IM_GROUP_MEMBER_ROLE = 'im_group_member_role', // IM 群成员角色
  IM_GROUP_ADD_SOURCE = 'im_group_add_source', // IM 加群来源
  IM_GROUP_REQUEST_HANDLE_RESULT = 'im_group_request_handle_result', // IM 加群申请处理结果
  IM_RTC_CALL_MEDIA_TYPE = 'im_rtc_call_media_type', // IM 通话媒体类型：1=语音 / 2=视频
  IM_RTC_CALL_CONVERSATION_TYPE = 'im_rtc_call_conversation_type', // IM 通话会话类型：1=私聊 / 2=群聊
  IM_RTC_CALL_STATUS = 'im_rtc_call_status', // IM 通话状态：10=创建 / 20=进行中 / 30=已结束
  IM_RTC_CALL_END_REASON = 'im_rtc_call_end_reason', // IM 通话结束原因：1=通话结束 / 2=已拒绝 / 3=已取消 / 4=无人接听 / 5=对方正忙 / 9=通话异常
  IM_RTC_PARTICIPANT_ROLE = 'im_rtc_participant_role', // IM 通话参与角色：1=发起人 / 2=被邀请者 / 3=主动加入者
  IM_RTC_PARTICIPANT_STATUS = 'im_rtc_participant_status', // IM 通话参与状态：10=邀请中 / 20=已加入 / 30=已拒绝 / 40=未应答 / 50=已离开
  IM_CHANNEL_MATERIAL_TYPE = 'im_channel_material_type', // IM 频道素材内容类型：1=富文本 / 2=外链

  // ========== LunaERP - 基本信息-客户模块  ==========
  LUNAERP_CUSTOMER_CLIENT_TYPE = 'lunaerp_customer_client_type', // 客户类型
  LUNAERP_CUSTOMER_INDUSTRY = 'lunaerp_customer_industry', // 客户所属行业
  LUNAERP_CUSTOMER_SCALE = 'lunaerp_customer_scale', // 客户企业规模
  LUNAERP_CUSTOMER_SOURCE = 'lunaerp_customer_source', // 客户来源
  LUNAERP_CUSTOMER_COOPERATION_STATUS = 'lunaerp_customer_cooperation_status', // 客户合作状态

  // ========== LunaERP - 基本信息-供应商模块 ==========
  LUNAERP_SUPPLIER_TYPE = 'lunaerp_supplier_type', // 供应商类型（18 项经营类别）

  // ========== LunaERP - 基本信息-产品模块 ==========
  LUNAERP_PRODUCT_CATEGORY = 'lunaerp_product_category', // 产品类别（成品/胚布/半成品，存 product.type + type_label）
  LUNAERP_FABRIC_TYPE = 'lunaerp_fabric_type', // 布种（针织/梭织/经编/纬编，存 product.fabric_type + fabric_type_label）

  // ========== LunaERP - 业务管理-销售合同模块 ==========
  LUNAERP_SALES_CONTRACT_STATUS = 'lunaerp_sales_contract_status', // 销售合同状态
  LUNAERP_SALES_CONTRACT_TRADE_TYPE = 'lunaerp_sales_contract_trade_type', // 销售合同贸易类型
  LUNAERP_SALES_CONTRACT_ORDER_TYPE = 'lunaerp_sales_contract_order_type', // 销售合同订单属性
  LUNAERP_SALES_CONTRACT_UNIT = 'lunaerp_sales_contract_unit', // 销售合同单位
  LUNAERP_SALES_CONTRACT_CURRENCY = 'lunaerp_sales_contract_currency', // 销售合同币种

  // ========== LunaERP - 生产管理-胚布单模块 ==========
  LUNAERP_GREIGE_ORDER_STATUS = 'lunaerp_greige_order_status', // 胚布单状态
  LUNAERP_GREIGE_PRODUCTION_MODE = 'lunaerp_greige_production_mode', // 胚布单生产模式（自织/外发）

  // ========== LunaERP - 生产管理-染色单模块 ==========
  LUNAERP_DYEING_ORDER_STATUS = 'lunaerp_dyeing_order_status', // 染色单状态

  // ========== LunaERP - 生产管理-纱线采购单模块 ==========
  LUNAERP_YARN_ORDER_STATUS = 'lunaerp_yarn_order_status', // 纱线采购单状态

  // ========== LunaERP - 生产管理-纱线规格模块 ==========
  LUNAERP_YARN_SPIN_METHOD = 'lunaerp_yarn_spin_method', // 纺纱方式
  LUNAERP_YARN_LUSTER = 'lunaerp_yarn_luster', // 光泽

  // ========== LunaERP - 生产管理-检验单模块 ==========
  LUNAERP_INSPECTION_ORDER_STATUS = 'lunaerp_inspection_order_status', // 检验单状态
  LUNAERP_INSPECTION_PACKAGE_MODE = 'lunaerp_inspection_package_mode', // 包装模式（卷装/匹装）
  LUNAERP_INSPECTION_GRADE = 'lunaerp_inspection_grade', // 检验等级

  // ========== LunaERP - 库存管理-入库模块 ==========
  LUNAERP_STOCK_IN_STATUS = 'lunaerp_stock_in_status', // 入库单状态
  LUNAERP_STOCK_IN_TYPE = 'lunaerp_stock_in_type', // 入库类型
  LUNAERP_STOCK_IN_UNIT = 'lunaerp_stock_in_unit', // 入库单位

  // ========== LunaERP - 生产管理-织造执行模块 ==========
  LUNAERP_WEAVING_ORDER_STATUS = 'lunaerp_weaving_order_status', // 织造单状态（待排产/生产中/已完工/已取消）
  LUNAERP_WEAVING_TASK_STATUS = 'lunaerp_weaving_task_status', // 机台任务状态（待开始/生产中/暂停异常/已完工）
  LUNAERP_WEAVING_TICKET_STATUS = 'lunaerp_weaving_ticket_status', // 布票状态（已生成/已开工/已完工/已检验/已入库/异常/作废）
  LUNAERP_WEAVING_EXCEPTION_TYPE = 'lunaerp_weaving_exception_type', // 织造异常类型（断纱/坏布/缺纱停机/换机/换人）
  LUNAERP_YARN_REQUISITION_STATUS = 'lunaerp_yarn_requisition_status', // 领纱单状态（草稿/待发纱/部分发纱/已发纱/已关闭）

  // ========== LunaERP - 生产管理-质检确认模块 ==========
  LUNAERP_INSPECTION_CONFIRMATION_STATUS = 'lunaerp_inspection_confirmation_status', // 质检确认状态（待确认/已认定/已撤销）
  LUNAERP_INSPECTION_RESPONSIBILITY = 'lunaerp_inspection_responsibility', // 质检责任类型（设备故障（免扣）/操作失误（扣款）/免扣）

  // ========== LunaERP - 业务管理-发货模块 ==========
  LUNAERP_SHIPMENT_STATUS = 'lunaerp_shipment_status', // 发货单状态（草稿/已发货/已取消）
  LUNAERP_SHIPMENT_SIGN_STATUS = 'lunaerp_shipment_sign_status', // 发货单签收状态（待签收/已签收/拒签）

  // ========== LunaERP - 生产管理-进度反馈模块 ==========
  LUNAERP_PROGRESS_FEEDBACK_STATUS = 'lunaerp_progress_feedback_status', // 进度反馈状态（未排缸/已排缸/染色中/固色中/质检中/已完成）

  // ========== LunaERP - 样品管理-样品分析模块 ==========
  LUNAERP_SAMPLE_PROCESS = 'lunaerp_sample_process', // 工序
  LUNAERP_SAMPLE_DYEING_PROCESS = 'lunaerp_sample_dyeing_process', // 双染工艺
  LUNAERP_SAMPLE_MACHINE_TYPE = 'lunaerp_sample_machine_type', // 机型
  LUNAERP_SAMPLE_SPIN_METHOD = 'lunaerp_sample_spin_method', // 纺法

  // ========== LunaERP - 审批引擎模块 ==========
  LUNAERP_APPROVAL_CONFIG_STATUS = 'lunaerp_approval_config_status', // 审批流程配置状态（ENABLED/DISABLED）
  LUNAERP_APPROVAL_BIZ_TYPE = 'lunaerp_approval_biz_type', // 审批单据类型
  LUNAERP_APPROVAL_NODE_TYPE = 'lunaerp_approval_node_type', // 审批节点类型
  LUNAERP_APPROVAL_APPROVER_TYPE = 'lunaerp_approval_approver_type', // 审批人来源
  LUNAERP_APPROVAL_INSTANCE_STATUS = 'lunaerp_approval_instance_status', // 审批实例状态
  LUNAERP_APPROVAL_TASK_ACTION = 'lunaerp_approval_task_action', // 审批任务动作
  LUNAERP_APPROVAL_RECORD_ACTION = 'lunaerp_approval_record_action', // 审批记录动作

  // ========== LunaERP - 业务管理-出口单证模块 ==========
  LUNAERP_EXPORT_DOC_STATUS = 'lunaerp_export_doc_status', // 出口单证状态
  LUNAERP_INCOTERM = 'lunaerp_incoterm', // 贸易术语
  LUNAERP_PAYMENT_TERM = 'lunaerp_payment_term', // 付款方式
  LUNAERP_TRANSPORT_MODE = 'lunaerp_transport_mode', // 运输方式

  // ========== LunaERP - 售前链-成本卡与智能报价模块 ==========
  LUNAERP_COST_CARD_STATUS = 'lunaerp_cost_card_status', // 成本卡状态
  LUNAERP_COST_CARD_BASIS = 'lunaerp_cost_card_basis', // 成本卡口径
  LUNAERP_COST_CARD_FEE_TYPE = 'lunaerp_cost_card_fee_type', // 费用类型
  LUNAERP_COST_FEE_UNIT = 'lunaerp_cost_fee_unit', // 费用计费口径
  LUNAERP_YARN_PRICE_TYPE = 'lunaerp_yarn_price_type', // 纱价类型
  LUNAERP_QUOTATION_STATUS = 'lunaerp_quotation_status', // 报价单状态
  LUNAERP_QUOTATION_COST_SOURCE = 'lunaerp_quotation_cost_source', // 报价成本来源

  // ========== LunaERP - 生产管理-打色指示单模块 ==========
  LUNAERP_COLOR_LIGHT_SOURCE = 'lunaerp_color_light_source', // 灯源
  LUNAERP_COLOR_LINE_STATUS = 'lunaerp_color_line_status', // 颜色行状态
  LUNAERP_COLOR_INSTRUCTION_STATUS = 'lunaerp_color_instruction_status', // 打色指示单状态

  // ========== LunaERP - 财务管理-供应商对账模块 ==========
  LUNAERP_SUPPLIER_RECON_STATUS = 'lunaerp_supplier_recon_status', // 供应商对账单状态

  // ========== LunaERP - 财务管理-收付款与付款申请模块 ==========
  LUNAERP_PAYMENT_APPLY_TYPE = 'lunaerp_payment_apply_type', // 付款申请类型
  LUNAERP_FINANCE_PAYMENT_DIRECTION = 'lunaerp_finance_payment_direction', // 收付款方向
  LUNAERP_APPROVAL_STATUS = 'lunaerp_approval_status', // 业务单据审批状态

  // ========== LunaERP - 销售退货模块 ==========
  LUNAERP_SALES_RETURN_STATUS = 'lunaerp_sales_return_status', // 销售退货状态
  LUNAERP_SALES_RETURN_REASON = 'lunaerp_sales_return_reason', // 销售退货原因
  LUNAERP_SALES_RETURN_QC_RESULT = 'lunaerp_sales_return_qc_result', // 销售退货质检判定
  LUNAERP_FINANCE_ACCOUNT_TYPE = 'lunaerp_finance_account_type', // 资金账户类型
  LUNAERP_FINANCE_ACCOUNT_NATURE = 'lunaerp_finance_account_nature', // 资金账户性质
  LUNAERP_FINANCE_ACCOUNT_STATUS = 'lunaerp_finance_account_status', // 资金账户状态
  LUNAERP_FINANCE_LEDGER_CATEGORY = 'lunaerp_finance_ledger_category', // 资金流水类别
  LUNAERP_OTHER_TXN_CATEGORY = 'lunaerp_other_txn_category', // 其他收支类别
  LUNAERP_EXPENSE_CATEGORY = 'lunaerp_expense_category', // 费用报销类别（2284）
  LUNAERP_PAYROLL_EARNING_TYPE = 'lunaerp_payroll_earning_type', // 工资收入款项类型（2285）
  LUNAERP_PAYROLL_DEDUCTION_TYPE = 'lunaerp_payroll_deduction_type', // 工资扣款款项类型（2286）
  LUNAERP_PAYROLL_PAYMENT_STATUS = 'lunaerp_payroll_payment_status' // 工资派生发放状态
}

/**
 * 车间移动 H5 专属既有字典引用常量（ADR-0045，零新增字典，仅常量名暴露）
 * 异常类型 2244、检验等级 2232、布票状态、机台任务状态、领纱单状态、检验单状态
 */
export const H5_DICT_TYPE = {
  WEAVING_EXCEPTION_TYPE: DICT_TYPE.LUNAERP_WEAVING_EXCEPTION_TYPE,
  INSPECTION_GRADE: DICT_TYPE.LUNAERP_INSPECTION_GRADE,
  WEAVING_TICKET_STATUS: DICT_TYPE.LUNAERP_WEAVING_TICKET_STATUS,
  WEAVING_TASK_STATUS: DICT_TYPE.LUNAERP_WEAVING_TASK_STATUS,
  YARN_REQUISITION_STATUS: DICT_TYPE.LUNAERP_YARN_REQUISITION_STATUS,
  INSPECTION_ORDER_STATUS: DICT_TYPE.LUNAERP_INSPECTION_ORDER_STATUS
} as const

export const H5_WEAVING_EXCEPTION_TYPE = DICT_TYPE.LUNAERP_WEAVING_EXCEPTION_TYPE // 异常类型 2244
export const H5_INSPECTION_GRADE = DICT_TYPE.LUNAERP_INSPECTION_GRADE // 检验等级 2232
export const H5_WEAVING_TICKET_STATUS = DICT_TYPE.LUNAERP_WEAVING_TICKET_STATUS // 布票状态
export const H5_WEAVING_TASK_STATUS = DICT_TYPE.LUNAERP_WEAVING_TASK_STATUS // 机台任务状态
export const H5_YARN_REQUISITION_STATUS = DICT_TYPE.LUNAERP_YARN_REQUISITION_STATUS // 领纱单状态
export const H5_INSPECTION_ORDER_STATUS = DICT_TYPE.LUNAERP_INSPECTION_ORDER_STATUS // 检验单状态

