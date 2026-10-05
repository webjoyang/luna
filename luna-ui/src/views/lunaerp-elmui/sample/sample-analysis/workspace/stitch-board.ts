export type StitchSymbol = '' | 'v' | '-' | '+' | '='

export interface SampleAnalysisStitchArrangeVO {
  columns: number
  rows: number
  data: StitchSymbol[][]
}

export const STITCH_SYMBOLS_ORDER: StitchSymbol[] = ['', 'v', '-', '+', '=']

export const STITCH_SYMBOL_LABELS: Record<StitchSymbol, string> = {
  '': '空',
  v: '成圈 (v)',
  '-': '集圈 (-)',
  '+': '浮线 (+)',
  '=': '其它 (=)'
}

export const DEFAULT_COLUMNS = 24
export const FIXED_ROWS = 4
export const STEP_COLUMNS = 4
export const MIN_COLUMNS = 0

/**
 * 创建默认三角排列画板数据（4 行 × columns 列，全空）
 */
export function createDefaultStitchArrange(columns: number = DEFAULT_COLUMNS): SampleAnalysisStitchArrangeVO {
  const safeCols = Math.max(MIN_COLUMNS, columns)
  const data: StitchSymbol[][] = []
  for (let r = 0; r < FIXED_ROWS; r++) {
    data.push(Array(safeCols).fill(''))
  }
  return {
    columns: safeCols,
    rows: FIXED_ROWS,
    data
  }
}

/**
 * 获取五态顺序中的下一个符号：空 → v → - → + → = → 空
 */
export function getNextStitchSymbol(current: string): StitchSymbol {
  const currentIndex = STITCH_SYMBOLS_ORDER.indexOf(current as StitchSymbol)
  if (currentIndex === -1 || currentIndex === STITCH_SYMBOLS_ORDER.length - 1) {
    return STITCH_SYMBOLS_ORDER[0]
  }
  return STITCH_SYMBOLS_ORDER[currentIndex + 1]
}

/**
 * 切换指定格子符号（五态循环），返回新的不可变画板对象
 */
export function toggleStitchCell(
  board: SampleAnalysisStitchArrangeVO,
  rowIndex: number,
  colIndex: number
): SampleAnalysisStitchArrangeVO {
  const currentSymbol = board.data[rowIndex]?.[colIndex] || ''
  const nextSymbol = getNextStitchSymbol(currentSymbol)
  const newData = board.data.map((row, r) => {
    if (r !== rowIndex) return [...row]
    const newRow = [...row]
    newRow[colIndex] = nextSymbol
    return newRow
  })
  return {
    ...board,
    data: newData
  }
}

/**
 * 增加列（默认步进 4 列），保留已有列符号，新列置空
 */
export function addStitchColumns(
  board: SampleAnalysisStitchArrangeVO,
  step: number = STEP_COLUMNS
): SampleAnalysisStitchArrangeVO {
  const newCols = board.columns + step
  const newData: StitchSymbol[][] = []
  for (let r = 0; r < FIXED_ROWS; r++) {
    const prevRow = board.data && board.data[r] ? board.data[r] : []
    const newRow = [...prevRow]
    while (newRow.length < newCols) {
      newRow.push('')
    }
    newData.push(newRow)
  }
  return {
    columns: newCols,
    rows: FIXED_ROWS,
    data: newData
  }
}

/**
 * 减少列（默认步进 4 列），保留剩余列符号，减至下限（0 列）时不再减少
 */
export function removeStitchColumns(
  board: SampleAnalysisStitchArrangeVO,
  step: number = STEP_COLUMNS,
  minColumns: number = MIN_COLUMNS
): SampleAnalysisStitchArrangeVO {
  const newCols = Math.max(minColumns, (board.columns ?? DEFAULT_COLUMNS) - step)
  const newData: StitchSymbol[][] = []
  for (let r = 0; r < FIXED_ROWS; r++) {
    const prevRow = board.data && board.data[r] ? board.data[r] : []
    newData.push(prevRow.slice(0, newCols))
  }
  return {
    columns: newCols,
    rows: FIXED_ROWS,
    data: newData
  }
}

/**
 * 清空画板上的全部符号（保留行列数）
 */
export function clearStitchBoard(board: SampleAnalysisStitchArrangeVO): SampleAnalysisStitchArrangeVO {
  const cols = board.columns ?? DEFAULT_COLUMNS
  const newData: StitchSymbol[][] = []
  for (let r = 0; r < FIXED_ROWS; r++) {
    newData.push(Array(cols).fill(''))
  }
  return {
    columns: cols,
    rows: FIXED_ROWS,
    data: newData
  }
}

/**
 * 规范化可能为空或格式不全的画板数据
 */
export function normalizeStitchArrange(raw?: any): SampleAnalysisStitchArrangeVO {
  if (!raw || typeof raw !== 'object') {
    return createDefaultStitchArrange()
  }
  const rows = FIXED_ROWS
  const columns = typeof raw.columns === 'number' && raw.columns >= 0 ? raw.columns : DEFAULT_COLUMNS
  const data: StitchSymbol[][] = []
  const rawData = Array.isArray(raw.data) ? raw.data : []
  for (let r = 0; r < rows; r++) {
    const rawRow = Array.isArray(rawData[r]) ? rawData[r] : []
    const row: StitchSymbol[] = []
    for (let c = 0; c < columns; c++) {
      const val = rawRow[c]
      if (val === 'v' || val === '-' || val === '+' || val === '=') {
        row.push(val)
      } else {
        row.push('')
      }
    }
    data.push(row)
  }
  return {
    columns,
    rows,
    data
  }
}
