import { describe, expect, it } from 'vitest'
import {
  createDefaultStitchArrange,
  getNextStitchSymbol,
  addStitchColumns,
  removeStitchColumns,
  clearStitchBoard,
  toggleStitchCell,
  STITCH_SYMBOLS_ORDER
} from '../stitch-board'

describe('StitchBoard logic (三角排列画板工艺)', () => {
  it('初始画板为 4×24 全空矩阵', () => {
    const board = createDefaultStitchArrange()
    expect(board.rows).toBe(4)
    expect(board.columns).toBe(24)
    expect(board.data.length).toBe(4)
    for (let r = 0; r < 4; r++) {
      expect(board.data[r].length).toBe(24)
      for (let c = 0; c < 24; c++) {
        expect(board.data[r][c]).toBe('')
      }
    }
  })

  it('点击格子按五态顺序循环：空 → v → - → + → = → 空，第6击回到空', () => {
    let symbol = ''
    symbol = getNextStitchSymbol(symbol)
    expect(symbol).toBe('v') // 1st click
    symbol = getNextStitchSymbol(symbol)
    expect(symbol).toBe('-') // 2nd click
    symbol = getNextStitchSymbol(symbol)
    expect(symbol).toBe('+') // 3rd click
    symbol = getNextStitchSymbol(symbol)
    expect(symbol).toBe('=') // 4th click
    symbol = getNextStitchSymbol(symbol)
    expect(symbol).toBe('')  // 5th click: returns to empty! (5-state cycle)

    // Complete cycle verification against STITCH_SYMBOLS_ORDER
    expect(STITCH_SYMBOLS_ORDER).toEqual(['', 'v', '-', '+', '='])
  })

  it('+4 按 4 列步进扩缩并保留已有格子内容', () => {
    const board = createDefaultStitchArrange(4)
    board.data[0][0] = 'v'
    board.data[1][2] = '-'
    board.data[2][3] = '+'
    board.data[3][1] = '='

    const expanded = addStitchColumns(board, 4)
    expect(expanded.columns).toBe(8)
    expect(expanded.rows).toBe(4)
    // 验证原内容保留
    expect(expanded.data[0][0]).toBe('v')
    expect(expanded.data[1][2]).toBe('-')
    expect(expanded.data[2][3]).toBe('+')
    expect(expanded.data[3][1]).toBe('=')
    // 验证新增列为全空
    for (let r = 0; r < 4; r++) {
      expect(expanded.data[r].length).toBe(8)
      expect(expanded.data[r][4]).toBe('')
      expect(expanded.data[r][7]).toBe('')
    }
  })

  it('-4 按 4 列步进缩减并保留剩余格子内容；到下限（0 列）时无法继续扣减', () => {
    const board = createDefaultStitchArrange(8)
    board.data[0][0] = 'v'
    board.data[1][2] = '-'
    board.data[2][5] = '+' // 第 5 列，将被裁掉

    const shrunk = removeStitchColumns(board, 4)
    expect(shrunk.columns).toBe(4)
    expect(shrunk.data[0][0]).toBe('v')
    expect(shrunk.data[1][2]).toBe('-')
    expect(shrunk.data[0].length).toBe(4)

    // 缩减到 0 列
    const zeroCols = removeStitchColumns(shrunk, 4)
    expect(zeroCols.columns).toBe(0)
    for (let r = 0; r < 4; r++) {
      expect(zeroCols.data[r].length).toBe(0)
    }

    // 已经到 0 列下限，不能再减
    const belowZero = removeStitchColumns(zeroCols, 4)
    expect(belowZero.columns).toBe(0)
  })

  it('清空画板将全部格子重置为全空，保留当前行列数', () => {
    const board = createDefaultStitchArrange(8)
    board.data[0][0] = 'v'
    board.data[1][3] = '+'
    board.data[2][7] = '='

    const cleared = clearStitchBoard(board)
    expect(cleared.columns).toBe(8)
    expect(cleared.rows).toBe(4)
    for (let r = 0; r < 4; r++) {
      expect(cleared.data[r].length).toBe(8)
      for (let c = 0; c < 8; c++) {
        expect(cleared.data[r][c]).toBe('')
      }
    }
  })

  it('toggleStitchCell 支持按行/列切换符号并返回不可变新对象', () => {
    const board = createDefaultStitchArrange(4)
    expect(board.data[1][2]).toBe('')

    const step1 = toggleStitchCell(board, 1, 2)
    expect(step1.data[1][2]).toBe('v')
    expect(board.data[1][2]).toBe('') // 验证不可变性

    const step2 = toggleStitchCell(step1, 1, 2)
    expect(step2.data[1][2]).toBe('-')

    const step3 = toggleStitchCell(step2, 1, 2)
    expect(step3.data[1][2]).toBe('+')

    const step4 = toggleStitchCell(step3, 1, 2)
    expect(step4.data[1][2]).toBe('=')

    const step5 = toggleStitchCell(step4, 1, 2)
    expect(step5.data[1][2]).toBe('')
  })
})
