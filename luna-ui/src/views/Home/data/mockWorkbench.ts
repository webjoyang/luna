import type {
  ActionCardData,
  ApprovalItem,
  PreviewRow,
  ScopeTab,
  StatItem,
  TaskItem,
  TodayTaskStat,
  WorkbenchGroup,
  WorkbenchSummary
} from './types'

/** 页签：本版只实现总览，其余三个页签为空态占位 */
export const scopeTabs: ScopeTab[] = [
  { key: 'overview', label: '总览', emptyText: '' },
  { key: 'production', label: '生产', emptyText: '生产工作台正在建设中，先把总览用起来。' },
  { key: 'finance', label: '财务', emptyText: '财务工作台正在建设中，先把总览用起来。' },
  { key: 'theme', label: '个性化', emptyText: '个性化装扮正在建设中，先把总览用起来。' }
]

/** 待审批单据（9 件：请款 4 · 报销 3 · 采购 2） */
export const workbenchApprovals: ApprovalItem[] = [
  {
    id: 'AP-2401',
    type: '请款',
    title: '杭州织造车间 9 月电费请款',
    applicant: '沈嘉禾',
    amount: 42600,
    ageDays: 3
  },
  {
    id: 'AP-2402',
    type: '请款',
    title: '坯布运输费结算 · 德邦物流 8 月',
    applicant: '周予安',
    amount: 18300,
    ageDays: 1
  },
  {
    id: 'AP-2403',
    type: '请款',
    title: '外发定型加工费请款',
    applicant: '苏景行',
    amount: 52800,
    ageDays: 2
  },
  {
    id: 'AP-2404',
    type: '请款',
    title: '园区物业与水电分摊请款',
    applicant: '何若初',
    amount: 9600,
    ageDays: 0
  },
  {
    id: 'AP-2405',
    type: '报销',
    title: '南京国际面料展差旅报销',
    applicant: '林清越',
    amount: 7860,
    ageDays: 1
  },
  {
    id: 'AP-2406',
    type: '报销',
    title: '车间空压机维修配件报销',
    applicant: '陈亦舟',
    amount: 3240,
    ageDays: 0
  },
  {
    id: 'AP-2407',
    type: '报销',
    title: '客户到厂验样接待餐费报销',
    applicant: '温以宁',
    amount: 5120,
    ageDays: 2
  },
  {
    id: 'AP-2408',
    type: '采购',
    title: '精梳棉纱 32S 采购申请',
    applicant: '顾南亭',
    amount: 96000,
    ageDays: 2
  },
  {
    id: 'AP-2409',
    type: '采购',
    title: '活性染料与助剂补货申请',
    applicant: '许知砚',
    amount: 23500,
    ageDays: 1
  }
]

/**
 * 协作任务池。dueOffset 相对今天：负数=已逾期，0=今天，正数=未来。
 * 逾期 3 项 + 高风险未逾期 2 项 = 要你跟 5 项。
 */
export const workbenchTasks: TaskItem[] = [
  {
    id: 'TK-01',
    title: '确认苏州云锦秋季面料打样进度',
    ownerName: '温以宁',
    status: 'overdue',
    riskLevel: 'HIGH',
    dueOffset: -4,
    steps: 5,
    stepsDone: 2,
    nextPlan: '催打样间今天下午出一版对比色卡',
    riskReason: '客户 9/20 要确认首版，打样已延后两天',
    source: 'SELF'
  },
  {
    id: 'TK-02',
    title: '跟进精梳棉纱 32S 到货与验收入库',
    ownerName: '顾南亭',
    status: 'overdue',
    riskLevel: 'MID',
    dueOffset: -2,
    steps: 4,
    stepsDone: 3,
    nextPlan: '与供应商确认 9/14 到厂车次',
    source: 'SELF'
  },
  {
    id: 'TK-03',
    title: '整理 8 月外发定型加工结算单',
    ownerName: '苏景行',
    status: 'overdue',
    riskLevel: 'LOW',
    dueOffset: -1,
    steps: 3,
    stepsDone: 2,
    nextPlan: '补齐三张对账单后交财务复核',
    source: 'SELF'
  },
  {
    id: 'TK-04',
    title: '杭州织造车间 9 月电费请款复核',
    ownerName: '沈嘉禾',
    status: 'in_progress',
    riskLevel: 'HIGH',
    dueOffset: 1,
    steps: 4,
    stepsDone: 2,
    nextPlan: '核对峰谷电量与上月差异',
    riskReason: '单笔金额超过 4 万，需二次复核',
    source: 'ASSIGN'
  },
  {
    id: 'TK-05',
    title: '南京面料展客户线索回访',
    ownerName: '林清越',
    status: 'in_progress',
    riskLevel: 'HIGH',
    dueOffset: 3,
    steps: 5,
    stepsDone: 2,
    nextPlan: '先回访 3 家明确要样的客户',
    riskReason: '线索搁置超过一周会明显掉转化',
    source: 'ASSIGN'
  },
  {
    id: 'TK-06',
    title: '车间空压机季度保养排期',
    ownerName: '陈亦舟',
    status: 'in_progress',
    riskLevel: 'LOW',
    dueOffset: 4,
    steps: 3,
    stepsDone: 1,
    nextPlan: '与设备科约 9/17 停机窗口',
    source: 'SELF'
  },
  {
    id: 'TK-07',
    title: '成品仓 9 月盘点计划确认',
    ownerName: '何若初',
    status: 'blocked',
    riskLevel: 'MID',
    dueOffset: 2,
    steps: 4,
    stepsDone: 1,
    nextPlan: '等仓储部确认盘点人员名单',
    riskReason: '盘点人员未落实，计划无法下发',
    source: 'SELF'
  },
  {
    id: 'TK-08',
    title: '广州仓首批发货放行单准备',
    ownerName: '苏景行',
    status: 'blocked',
    riskLevel: 'MID',
    dueOffset: 2,
    steps: 3,
    stepsDone: 0,
    nextPlan: '待质检出具首件报告后放行',
    riskReason: '首件检验报告尚未回传',
    source: 'SELF'
  },
  {
    id: 'TK-09',
    title: '秋季新品面料成本核算',
    ownerName: '许知砚',
    status: 'completed',
    riskLevel: 'LOW',
    dueOffset: -3,
    steps: 4,
    stepsDone: 4,
    nextPlan: '已交付销售部用于报价',
    source: 'ASSIGN'
  },
  {
    id: 'TK-10',
    title: '8 月车间考勤汇总确认',
    ownerName: '周予安',
    status: 'completed',
    riskLevel: 'LOW',
    dueOffset: 0,
    steps: 2,
    stepsDone: 2,
    nextPlan: '已同步人事归档',
    source: 'ASSIGN'
  },
  {
    id: 'TK-11',
    title: '染化料安全库存复核',
    ownerName: '许知砚',
    status: 'completed',
    riskLevel: 'LOW',
    dueOffset: 0,
    steps: 3,
    stepsDone: 3,
    nextPlan: '补货申请已提交',
    source: 'SELF'
  },
  {
    id: 'TK-12',
    title: '接待苏州云锦采购负责人到厂验样',
    ownerName: '温以宁',
    status: 'in_progress',
    riskLevel: 'MID',
    dueOffset: 0,
    steps: 4,
    stepsDone: 1,
    nextPlan: '下午两点带看打样间与成品仓',
    source: 'SELF'
  },
  {
    id: 'TK-13',
    title: '整理三季度客户投诉与改进记录',
    ownerName: '林清越',
    status: 'in_progress',
    riskLevel: 'MID',
    dueOffset: 0,
    steps: 3,
    stepsDone: 1,
    nextPlan: '先汇总 7-8 月共 6 条投诉',
    source: 'SELF'
  },
  {
    id: 'TK-14',
    title: '9 月排产计划初稿评审',
    ownerName: '沈嘉禾',
    status: 'completed',
    riskLevel: 'LOW',
    dueOffset: -8,
    steps: 3,
    stepsDone: 3,
    nextPlan: '评审意见已并入正式计划',
    source: 'SELF'
  },
  {
    id: 'TK-15',
    title: '坯布库存盘点差异复盘',
    ownerName: '何若初',
    status: 'completed',
    riskLevel: 'LOW',
    dueOffset: -6,
    steps: 2,
    stepsDone: 2,
    nextPlan: '差异原因已归档',
    source: 'SELF'
  },
  {
    id: 'TK-16',
    title: '8 月供应商交期达成率统计',
    ownerName: '顾南亭',
    status: 'completed',
    riskLevel: 'LOW',
    dueOffset: -5,
    steps: 4,
    stepsDone: 4,
    nextPlan: '结果已同步采购例会',
    source: 'SELF'
  },
  {
    id: 'TK-17',
    title: '秋季新品首单打样排产确认',
    ownerName: '沈嘉禾',
    status: 'in_progress',
    riskLevel: 'MID',
    dueOffset: 5,
    steps: 3,
    stepsDone: 1,
    nextPlan: '确认 9/17 织造机台排布',
    source: 'SELF'
  },
  {
    id: 'TK-18',
    title: '云锦服饰包装规范对接',
    ownerName: '温以宁',
    status: 'in_progress',
    riskLevel: 'LOW',
    dueOffset: 6,
    steps: 2,
    stepsDone: 0,
    nextPlan: '下周与客户包装组开一次对接会',
    source: 'SELF'
  },
  {
    id: 'TK-19',
    title: '织造车间安全生产自查',
    ownerName: '陈亦舟',
    status: 'in_progress',
    riskLevel: 'MID',
    dueOffset: 7,
    steps: 4,
    stepsDone: 1,
    nextPlan: '按检查表逐台排查隐患',
    source: 'SELF'
  },
  {
    id: 'TK-20',
    title: '四季度原料采购预算编制',
    ownerName: '许知砚',
    status: 'in_progress',
    riskLevel: 'LOW',
    dueOffset: 9,
    steps: 3,
    stepsDone: 0,
    nextPlan: '先按前三季度用量推算',
    source: 'SELF'
  }
]

/** 消息统计（对应源站服务端 stats：总未读 − 被折叠的进度通知） */
export const messageStats = {
  unread: 17,
  tidied: 5
}

/** 订单分状态计数（在产 = PENDING + FABRIC_PREPARING + DYEING_IN_PROGRESS） */
export const orderStats = {
  PENDING: 6,
  FABRIC_PREPARING: 7,
  DYEING_IN_PROGRESS: 10,
  COMPLETED: 42,
  SHIPPED: 15
}

const isOpen = (task: TaskItem) => task.status !== 'completed'

/** 逾期未完成任务 */
export const overdueTasks = workbenchTasks.filter((t) => isOpen(t) && t.dueOffset < 0)

/** 高风险且未逾期的未完成任务 */
export const highRiskTasks = workbenchTasks.filter(
  (t) => isOpen(t) && t.riskLevel === 'HIGH' && t.dueOffset >= 0
)

export const riskTasks = [...overdueTasks, ...highRiskTasks]

export const todayTaskStat: TodayTaskStat = {
  total: workbenchTasks.filter((t) => t.dueOffset === 0).length,
  done: workbenchTasks.filter((t) => t.dueOffset === 0 && t.status === 'completed').length
}

const sum = (list: TaskItem[], pick: (t: TaskItem) => number) =>
  list.reduce((acc, t) => acc + pick(t), 0)

const totalSteps = sum(workbenchTasks, (t) => t.steps)
const doneSteps = sum(workbenchTasks, (t) => t.stepsDone)

export const workbenchSummary: WorkbenchSummary = {
  approvals: workbenchApprovals.length,
  riskTasks: riskTasks.length,
  focusUnread: messageStats.unread - messageStats.tidied,
  inProduction: orderStats.PENDING + orderStats.FABRIC_PREPARING + orderStats.DYEING_IN_PROGRESS,
  overdueCount: overdueTasks.length,
  highRiskCount: highRiskTasks.length,
  oldestApprovalDays: workbenchApprovals.reduce((max, a) => Math.max(max, a.ageDays), 0),
  totalSteps,
  doneSteps,
  openSteps: totalSteps - doneSteps,
  unFinished: workbenchTasks.filter(isOpen).length,
  todayCount: workbenchTasks.filter((t) => isOpen(t) && t.dueOffset === 0).length,
  weekCount: workbenchTasks.filter((t) => isOpen(t) && t.dueOffset >= 1 && t.dueOffset <= 7).length
}

/* ── 口径小工具 ───────────────────────────── */

export const formatMoney = (value: number) => `¥${value.toLocaleString('zh-CN')}`

/** 审批卡分类小字：按类型计数 top3 + 合计金额 */
const buildApprovalBrk = () => {
  const counter = new Map<string, number>()
  let total = 0
  workbenchApprovals.forEach((a) => {
    counter.set(a.type, (counter.get(a.type) ?? 0) + 1)
    total += a.amount
  })
  const parts = [...counter.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([type, count]) => `${type} ${count}`)
  return `${parts.join(' · ')} · 合计 ${formatMoney(total)}`
}

const approvalRows: PreviewRow[] = [...workbenchApprovals]
  .sort((a, b) => b.amount - a.amount)
  .slice(0, 3)
  .map((a) => ({
    title: a.title,
    sub: `${a.applicant} · ${a.type}`,
    value: formatMoney(a.amount)
  }))

const taskValueLabel = (t: TaskItem) => {
  if (t.status === 'overdue') return `逾期 ${Math.abs(t.dueOffset)} 天`
  if (t.status === 'blocked') return '受阻'
  return t.riskLevel === 'HIGH' ? '高风险' : '进行中'
}

const taskRows: PreviewRow[] = riskTasks.slice(0, 3).map((t) => ({
  title: t.title,
  sub: `${t.ownerName} · ${t.nextPlan}`,
  value: taskValueLabel(t)
}))

const readRows: PreviewRow[] = [
  { title: '苏州云锦回复：秋季面料打样下周一到样', sub: '温以宁 @你 · 客户跟进', value: '09:42' },
  { title: '沈嘉禾 @你 复核电费请款明细', sub: '审批协作 · 待你确认', value: '08:31' },
  { title: '顾南亭 指派你跟进精梳棉纱验收入库', sub: '任务指派 · 需在今天回复', value: '昨天' }
]

const oldestApproval = workbenchSummary.oldestApprovalDays

export const workbenchCards: ActionCardData[] = [
  {
    key: 'approve',
    label: '要你批',
    value: workbenchSummary.approvals,
    unit: '件',
    tone: 'danger',
    icon: 'ep:circle-check',
    brk: buildApprovalBrk(),
    warning: oldestApproval > 0 ? `最久一张压了 ${oldestApproval} 天` : '都是今天新到的',
    secondary: '另有 3 张历史单据在审批中心',
    moreText: `查看全部 ${workbenchSummary.approvals} 件`,
    previewRows: approvalRows
  },
  {
    key: 'follow',
    label: '要你跟',
    value: workbenchSummary.riskTasks,
    unit: '项',
    tone: 'warning',
    icon: 'ep:warning',
    brk: `逾期 ${workbenchSummary.overdueCount} · 高风险 ${workbenchSummary.highRiskCount}`,
    warning: overdueTasks.length
      ? `最急的一项已逾期 ${Math.abs(overdueTasks[0].dueOffset)} 天`
      : '当前没有逾期任务',
    moreText: '查看全部并处理',
    previewRows: taskRows
  },
  {
    key: 'read',
    label: '要你看',
    value: workbenchSummary.focusUnread,
    unit: '条',
    tone: 'primary',
    icon: 'ep:bell',
    brk: '最久没处理 12 天 · 可能延迟回款',
    warning: '最久一条挂了 12 天',
    secondary: `另有 ${messageStats.tidied} 条进度通知已折叠`,
    moreText: '去消息中心',
    previewRows: readRows
  }
]

/** 统计格：要你批 / 要你跟 / 要你看 / 在产订单 */
export const workbenchStats: StatItem[] = [
  {
    key: 'approve',
    label: '要你批',
    value: workbenchSummary.approvals,
    unit: '件',
    tone: 'default'
  },
  { key: 'follow', label: '要你跟', value: workbenchSummary.riskTasks, unit: '项', tone: 'bad' },
  {
    key: 'read',
    label: '要你看',
    value: workbenchSummary.focusUnread,
    unit: '条',
    tone: 'default'
  },
  { key: 'orders', label: '在产订单', value: workbenchSummary.inProduction, unit: '单', tone: 'ok' }
]

/* ── 我的工作台小组件 ───────────────────────── */

export const BUCKET_ORDER = ['overdue', 'today', 'tomorrow', 'week', 'later', 'unscheduled', 'done']

const BUCKET_LABELS: Record<string, string> = {
  overdue: '逾期与受阻',
  today: '今天',
  tomorrow: '明天',
  week: '本周内',
  later: '以后',
  unscheduled: '没排期',
  done: '已完成'
}

const bucketOf = (t: TaskItem): string => {
  if (t.status === 'completed') return 'done'
  if (t.status === 'overdue' || t.status === 'blocked') return 'overdue'
  if (t.dueOffset <= 0) return 'today'
  if (t.dueOffset === 1) return 'tomorrow'
  if (t.dueOffset <= 7) return 'week'
  return 'later'
}

/** 按源站分组口径把任务分桶（组内保持原顺序） */
export const buildGroups = (source: 'SELF' | 'ASSIGN'): WorkbenchGroup[] => {
  const list = workbenchTasks.filter((t) => t.source === source)
  return BUCKET_ORDER.map((key) => ({
    key,
    label: BUCKET_LABELS[key],
    tasks: list.filter((t) => bucketOf(t) === key)
  })).filter((g) => g.tasks.length > 0)
}

export const lunaBrief = (() => {
  const stuck = workbenchTasks.filter(
    (t) => isOpen(t) && (t.status === 'overdue' || t.status === 'blocked')
  )
  if (!stuck.length) return '今天没有逾期或受阻的任务，节奏不错。'
  const first = [...stuck].sort((a, b) => a.dueOffset - b.dueOffset)[0]
  return `${stuck.length} 项逾期或受阻。「${first.title}」${first.riskReason ?? first.nextPlan}。`
})()

export const workbenchGreeting = (hour: number) => {
  if (hour < 5) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}
