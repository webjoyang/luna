/**
 * 工作台「总览」的数据契约。
 * 全部为前端模拟数据，页面不发起任何网络请求。
 * 口径对齐 oldweb staff 首页工作台（buildMessageHighlights / buildTodoInbox）。
 */

export type HomeScope = 'overview' | 'production' | 'finance' | 'theme'

export type Tone = 'primary' | 'danger' | 'warning'

export type StatTone = 'default' | 'ok' | 'bad'

export type StatKey = 'approve' | 'follow' | 'read' | 'orders'

export interface ScopeTab {
  key: HomeScope
  label: string
  emptyText: string
}

export interface ApprovalItem {
  id: string
  /** 单据类型：请款 / 报销 / 采购 */
  type: string
  title: string
  applicant: string
  amount: number
  /** 最久积压天数 */
  ageDays: number
}

export type TaskStatus = 'in_progress' | 'overdue' | 'blocked' | 'completed'

export type RiskLevel = 'HIGH' | 'MID' | 'LOW'

export interface TaskItem {
  id: string
  title: string
  ownerName: string
  status: TaskStatus
  riskLevel: RiskLevel
  /** 相对今天的到期天数：负数表示已逾期，0 表示今天 */
  dueOffset: number
  steps: number
  stepsDone: number
  nextPlan: string
  riskReason?: string
  /** SELF=我要做的，ASSIGN=我派出去的 */
  source: 'SELF' | 'ASSIGN'
}

export interface PreviewRow {
  title: string
  sub: string
  value: string
}

export interface StatItem {
  key: StatKey
  label: string
  value: number
  unit: string
  tone: StatTone
}

export interface ActionCardData {
  key: 'approve' | 'follow' | 'read'
  label: string
  value: number
  unit: string
  tone: Tone
  icon: string
  brk: string
  warning: string
  secondary?: string
  moreText: string
  previewRows: PreviewRow[]
}

export interface WorkbenchSummary {
  approvals: number
  riskTasks: number
  focusUnread: number
  inProduction: number
  overdueCount: number
  highRiskCount: number
  oldestApprovalDays: number
  totalSteps: number
  doneSteps: number
  openSteps: number
  unFinished: number
  todayCount: number
  weekCount: number
}

export type CalendarEventType = 'contract' | 'payment' | 'shipment' | 'personal'

export interface CalendarEvent {
  type: CalendarEventType
  label: string
}

export type TaskStatusMeta = {
  key: TaskStatus
  label: string
  /** 左侧图标（iconify 名称） */
  icon: string
}

export interface CalendarDay {
  key: string
  offset: number
  /** 一 / 二 / … / 日 */
  weekday: string
  day: number
  month: number
  /** 农历，如「八月十五」 */
  lunar: string
  /** 节气 / 节日，如「中秋节」 */
  festival: string
  isToday: boolean
  taskCount: number
  events: CalendarEvent[]
}

export interface WorkbenchGroup {
  key: string
  label: string
  tasks: TaskItem[]
}

export interface WorldClockCity {
  flag: string
  city: string
  timeZone: string
}

export interface ExchangeRateMock {
  rate: number
  /** 更新时刻（相对今天的小时偏移，仅用于展示） */
  updatedLabel: string
  stale: boolean
}

export interface AiReminderMock {
  urgent: boolean
  label: string
  count: number
  title: string
  detail: string
}

export interface TodayTaskStat {
  total: number
  done: number
}
