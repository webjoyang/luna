import type {
  AiReminderMock,
  CalendarDay,
  CalendarEvent,
  ExchangeRateMock,
  TaskStatus,
  TaskStatusMeta,
  WorldClockCity
} from './types'
import { riskTasks, workbenchTasks } from './mockWorkbench'

// 演示数据：农历、节气与节日均以「今天」为锚点做相对偏移，任意日期打开都能看到完整演示，
// 因此中秋等日期不具真实农历准确性。接入真实接口后整份文件删除。

const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']
const LUNAR_DAY_NAMES = [
  '初一',
  '初二',
  '初三',
  '初四',
  '初五',
  '初六',
  '初七',
  '初八',
  '初九',
  '初十',
  '十一',
  '十二',
  '十三',
  '十四',
  '十五',
  '十六',
  '十七',
  '十八',
  '十九',
  '二十',
  '廿一',
  '廿二',
  '廿三',
  '廿四',
  '廿五',
  '廿六',
  '廿七',
  '廿八',
  '廿九',
  '三十'
]
/** 今天往前第 1 天 = 八月初一（农历），据此推算出整段窗口的农历 */
const LUNAR_MONTH_START_OFFSET = -1

const pad = (n: number) => String(n).padStart(2, '0')

const dateKey = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const shiftDays = (base: Date, offset: number) =>
  new Date(base.getFullYear(), base.getMonth(), base.getDate() + offset)

/** 节气 / 节日（按相对今天的天数偏移） */
const FESTIVALS: Record<number, string> = {
  '-5': '白露',
  11: '秋分',
  13: '中秋节',
  14: '中秋假期'
}

/** 业务事项彩点（按相对今天的天数偏移） */
const BUSINESS_EVENTS: Record<number, CalendarEvent[]> = {
  '-4': [{ type: 'contract', label: '云锦秋季订单交期' }],
  0: [
    { type: 'payment', label: '德邦物流货款到期' },
    { type: 'shipment', label: '广州仓首批发货' }
  ],
  2: [{ type: 'contract', label: '精梳棉纱到厂验收' }],
  5: [{ type: 'payment', label: '上半月货款结算' }],
  8: [{ type: 'shipment', label: '云锦服饰首批出货' }],
  13: [{ type: 'personal', label: '中秋节假期' }]
}

const lunarOf = (offset: number) => {
  const day = offset >= LUNAR_MONTH_START_OFFSET ? offset + 2 : offset + 31
  const month = offset >= LUNAR_MONTH_START_OFFSET ? '八月' : '七月'
  const name = LUNAR_DAY_NAMES[Math.min(Math.max(day, 1), 30) - 1]
  return `${month}${name}`
}

const taskCountOf = (offset: number) => workbenchTasks.filter((t) => t.dueOffset === offset).length

/** 以「今天」为锚点生成前后各 10 天（共 21 天）的日历窗口 */
export const buildCalendarDays = (now: Date): CalendarDay[] => {
  const keys = new Set<string>()
  const days: CalendarDay[] = []
  for (let offset = -10; offset <= 10; offset += 1) {
    const date = shiftDays(now, offset)
    const key = dateKey(date)
    if (keys.has(key)) continue
    keys.add(key)
    days.push({
      key,
      offset,
      weekday: WEEKDAYS[date.getDay()],
      day: date.getDate(),
      month: date.getMonth() + 1,
      lunar: lunarOf(offset),
      festival: FESTIVALS[offset] ?? '',
      isToday: offset === 0,
      taskCount: taskCountOf(offset),
      events: BUSINESS_EVENTS[offset] ?? []
    })
  }
  return days
}

/** 业务事项图例 */
export const calendarEventLegend: { type: CalendarEvent['type']; label: string }[] = [
  { type: 'contract', label: '合同交期' },
  { type: 'payment', label: '货款到期' },
  { type: 'shipment', label: '计划发货' },
  { type: 'personal', label: '个人事项' }
]

/** AI 智能决策建议（由任务口径推导，非写死文案） */
export const aiSuggestions = (dayCount: number): string[] => {
  const list: string[] = []
  if (dayCount === 0) return ['今日无异常事项，系统运行良好。']
  const overdue = riskTasks.filter((t) => t.status === 'overdue').length
  const blocked = riskTasks.filter((t) => t.status === 'blocked').length
  const highRisk = riskTasks.filter((t) => t.riskLevel === 'HIGH').length
  if (overdue) list.push(`有 ${overdue} 项协作任务已逾期，建议今天优先处理。`)
  if (blocked) list.push(`${blocked} 项任务处于受阻状态，需先解除前置依赖。`)
  if (highRisk) list.push(`${highRisk} 项任务被标记为高风险，建议同步给对应负责人。`)
  list.push('今日有 1 笔货款到期，留意资金账户余额。')
  return list
}

export const taskStatusMeta: Record<TaskStatus, TaskStatusMeta> = {
  completed: { key: 'completed', label: '已完成', icon: 'ep:circle-check' },
  blocked: { key: 'blocked', label: '受阻', icon: 'ep:remove-filled' },
  overdue: { key: 'overdue', label: '已逾期', icon: 'ep:warning-filled' },
  in_progress: { key: 'in_progress', label: '进行中', icon: 'ep:loading' }
}

/** 世界时钟（9 城） */
export const worldClockCities: WorldClockCity[] = [
  { flag: '🇨🇳', city: '北京', timeZone: 'Asia/Shanghai' },
  { flag: '🇯🇵', city: '东京', timeZone: 'Asia/Tokyo' },
  { flag: '🇸🇬', city: '新加坡', timeZone: 'Asia/Singapore' },
  { flag: '🇦🇪', city: '迪拜', timeZone: 'Asia/Dubai' },
  { flag: '🇬🇧', city: '伦敦', timeZone: 'Europe/London' },
  { flag: '🇫🇷', city: '巴黎', timeZone: 'Europe/Paris' },
  { flag: '🇺🇸', city: '纽约', timeZone: 'America/New_York' },
  { flag: '🇺🇸', city: '洛杉矶', timeZone: 'America/Los_Angeles' },
  { flag: '🇦🇺', city: '悉尼', timeZone: 'Australia/Sydney' }
]

/** 实时汇率（USD/CNY） */
export const exchangeRate: ExchangeRateMock = {
  rate: 7.1284,
  updatedLabel: '09:15',
  stale: false
}

/** AI 提醒 */
export const aiReminder: AiReminderMock = {
  urgent: true,
  label: '高优先级',
  count: 6,
  title: '3 项任务逾期、2 项高风险待你决策',
  detail: '建议先处理「确认苏州云锦秋季面料打样进度」，已逾期 4 天，客户 9/20 要确认首版色卡。'
}
