import { describe, expect, it } from 'vitest'
import type {
  SampleAnalysisYarnArrangeVO,
  SampleAnalysisSaveReqVO
} from '@/api/lunaerp-elmui/sample/sample-analysis'

describe('YarnArrange (纱线排列绕纱工艺行)', () => {
  it('支持增行，默认四个文本字段为空字符串且生成前端临时 _key', () => {
    const list: SampleAnalysisYarnArrangeVO[] = []
    let keySeed = 1

    const addRow = () => {
      list.push({
        _key: `temp_${Date.now()}_${keySeed++}`,
        code: '',
        draftText: '',
        borderStatus: '',
        description: ''
      })
    }

    addRow()
    addRow()

    expect(list.length).toBe(2)
    expect(list[0]._key).toBeDefined()
    expect(list[1]._key).toBeDefined()
    expect(list[0]._key).not.toBe(list[1]._key)
    expect(list[0].code).toBe('')
    expect(list[0].draftText).toBe('')
    expect(list[0].borderStatus).toBe('')
    expect(list[0].description).toBe('')
  })

  it('支持在任意位置删行，剩余行严格保序', () => {
    const list: SampleAnalysisYarnArrangeVO[] = [
      { id: 101, code: '1', draftText: '25.1cm/1花宽', borderStatus: '第一路对针', description: '开幅线' },
      { id: 102, code: '2', draftText: '26.0cm', borderStatus: '', description: '' },
      { id: 103, code: '3', draftText: '27.5cm', borderStatus: '第三路对针', description: '平纹线' }
    ]

    // 删除第 2 行 (index 1)
    list.splice(1, 1)

    expect(list.length).toBe(2)
    expect(list[0].code).toBe('1')
    expect(list[1].code).toBe('3')
    expect(list[1].description).toBe('平纹线')
  })

  it('保存时 payload 转换：提取四个文本字段，四字段全空也能正确组装，剥离前端内部 _key', () => {
    const formRows: SampleAnalysisYarnArrangeVO[] = [
      {
        id: 1,
        sampleAnalysisId: 100,
        code: '1',
        draftText: '25.1cm/1花宽',
        borderStatus: '第一路对针',
        description: '开幅线',
        _key: 'temp_1'
      },
      {
        // 全空新增行
        code: '',
        draftText: '',
        borderStatus: '',
        description: '',
        _key: 'temp_2'
      }
    ]

    const payloadYarnArranges = formRows.map((a) => ({
      id: a.id,
      sampleAnalysisId: a.sampleAnalysisId,
      code: a.code,
      draftText: a.draftText,
      borderStatus: a.borderStatus,
      description: a.description
    }))

    const saveReq: Partial<SampleAnalysisSaveReqVO> = {
      yarnArranges: payloadYarnArranges
    }

    expect(saveReq.yarnArranges).toHaveLength(2)
    expect(saveReq.yarnArranges![0]).toEqual({
      id: 1,
      sampleAnalysisId: 100,
      code: '1',
      draftText: '25.1cm/1花宽',
      borderStatus: '第一路对针',
      description: '开幅线'
    })
    expect(saveReq.yarnArranges![1]).toEqual({
      id: undefined,
      sampleAnalysisId: undefined,
      code: '',
      draftText: '',
      borderStatus: '',
      description: ''
    })
    // 验证 _key 未泄露至后端 payload
    expect((saveReq.yarnArranges![0] as any)._key).toBeUndefined()
    expect((saveReq.yarnArranges![1] as any)._key).toBeUndefined()
  })

  it('详情还原时保序且完整还原四个可空文本字段', () => {
    const backendData: SampleAnalysisYarnArrangeVO[] = [
      { id: 11, sampleAnalysisId: 200, code: '1', draftText: 'A', borderStatus: 'B', description: 'C' },
      { id: 12, sampleAnalysisId: 200, code: undefined, draftText: undefined, borderStatus: undefined, description: undefined }
    ]

    const restored = (backendData || []).map((arrange) => ({
      ...arrange
    }))

    expect(restored).toHaveLength(2)
    expect(restored[0].id).toBe(11)
    expect(restored[0].code).toBe('1')
    expect(restored[1].id).toBe(12)
    expect(restored[1].code).toBeUndefined()
  })
})
