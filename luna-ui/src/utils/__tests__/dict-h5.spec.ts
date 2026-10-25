import { describe, expect, it, vi } from 'vitest'
import {
  DICT_TYPE,
  H5_DICT_TYPE,
  H5_WEAVING_EXCEPTION_TYPE,
  H5_INSPECTION_GRADE,
  H5_WEAVING_TICKET_STATUS,
  H5_WEAVING_TASK_STATUS,
  H5_YARN_REQUISITION_STATUS,
  H5_INSPECTION_ORDER_STATUS,
  getDictOptions,
  getStrDictOptions
} from '@/utils/dict'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('车间移动 H5 字典常量与回退机制（02-infra-config-dict-frontend-deps）', () => {
  it('H5_DICT_TYPE 暴露所有 6 项车间移动所需既有字典键名', () => {
    expect(H5_DICT_TYPE.WEAVING_EXCEPTION_TYPE).toBe('lunaerp_weaving_exception_type')
    expect(H5_DICT_TYPE.INSPECTION_GRADE).toBe('lunaerp_inspection_grade')
    expect(H5_DICT_TYPE.WEAVING_TICKET_STATUS).toBe('lunaerp_weaving_ticket_status')
    expect(H5_DICT_TYPE.WEAVING_TASK_STATUS).toBe('lunaerp_weaving_task_status')
    expect(H5_DICT_TYPE.YARN_REQUISITION_STATUS).toBe('lunaerp_yarn_requisition_status')
    expect(H5_DICT_TYPE.INSPECTION_ORDER_STATUS).toBe('lunaerp_inspection_order_status')
  })

  it('独立常量导出与 DICT_TYPE 既有枚举值保持完全对齐', () => {
    expect(H5_WEAVING_EXCEPTION_TYPE).toBe(DICT_TYPE.LUNAERP_WEAVING_EXCEPTION_TYPE)
    expect(H5_INSPECTION_GRADE).toBe(DICT_TYPE.LUNAERP_INSPECTION_GRADE)
    expect(H5_WEAVING_TICKET_STATUS).toBe(DICT_TYPE.LUNAERP_WEAVING_TICKET_STATUS)
    expect(H5_WEAVING_TASK_STATUS).toBe(DICT_TYPE.LUNAERP_WEAVING_TASK_STATUS)
    expect(H5_YARN_REQUISITION_STATUS).toBe(DICT_TYPE.LUNAERP_YARN_REQUISITION_STATUS)
    expect(H5_INSPECTION_ORDER_STATUS).toBe(DICT_TYPE.LUNAERP_INSPECTION_ORDER_STATUS)
  })

  it('2244 织造异常类型提供完整回退选项（断纱/坏布/缺纱停机/换机/换人）', () => {
    const options = getDictOptions(H5_DICT_TYPE.WEAVING_EXCEPTION_TYPE)
    expect(options.length).toBe(5)
    const values = options.map((opt) => opt.value)
    expect(values).toEqual(['YARN_BREAK', 'CLOTH_DAMAGE', 'YARN_SHORTAGE', 'MACHINE_CHANGE', 'WORKER_CHANGE'])
    expect(options[0].label).toBe('断纱')
    expect(options[1].label).toBe('坏布')
  })

  it('2232 检验等级提供完整回退选项（一等品/二等品/三等品/等外品）', () => {
    const options = getDictOptions(H5_DICT_TYPE.INSPECTION_GRADE)
    expect(options.length).toBe(4)
    const values = options.map((opt) => opt.value)
    expect(values).toEqual(['A', 'B', 'C', 'DEFECTIVE'])
    expect(options[0].label).toBe('一等品')
    expect(options[3].label).toBe('等外品')
  })

  it('布票状态提供完整 7 态回退选项（GENERATED..VOID）', () => {
    const options = getStrDictOptions(H5_DICT_TYPE.WEAVING_TICKET_STATUS)
    expect(options.length).toBe(7)
    const values = options.map((opt) => opt.value)
    expect(values).toEqual(['GENERATED', 'STARTED', 'COMPLETED', 'INSPECTED', 'STOCKED', 'EXCEPTION', 'VOID'])
  })

  it('机台任务状态提供完整 4 态回退选项', () => {
    const options = getStrDictOptions(H5_DICT_TYPE.WEAVING_TASK_STATUS)
    expect(options.length).toBe(4)
    const values = options.map((opt) => opt.value)
    expect(values).toEqual(['PENDING', 'IN_PROGRESS', 'HALTED', 'COMPLETED'])
  })

  it('领纱单状态提供完整 5 态回退选项', () => {
    const options = getStrDictOptions(H5_DICT_TYPE.YARN_REQUISITION_STATUS)
    expect(options.length).toBe(5)
    const values = options.map((opt) => opt.value)
    expect(values).toEqual(['DRAFT', 'ACTIVE', 'PARTIAL', 'DISPATCHED', 'CLOSED'])
  })

  it('检验单状态提供完整 4 态回退选项', () => {
    const options = getStrDictOptions(H5_DICT_TYPE.INSPECTION_ORDER_STATUS)
    expect(options.length).toBe(4)
    const values = options.map((opt) => opt.value)
    expect(values).toEqual(['DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED'])
  })
})
