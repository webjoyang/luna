import type {
  ReleaseContractLineVO,
  ReleaseGroupVO,
  ReleaseLineVO,
  ReleaseProductionReqVO,
  NewColorInstructionVO,
  ColorInstructionMode
} from '@/api/lunaerp-elmui/production/release-production'

export interface ReleaseWorkbenchLine extends ReleaseContractLineVO {
  releaseQty: number
  lossRate: number
}

export interface ReleaseWorkbenchGroup {
  productId?: number
  productCode: string
  productName: string
  composition?: string
  width?: string
  gsm?: string
  colorInstructionMode: ColorInstructionMode
  existingColorInstructionId?: number
  newColorInstruction?: NewColorInstructionVO
  lines: ReleaseWorkbenchLine[]
}

export interface ReleaseSummary {
  totalGreigeQty: number
  totalDyeingQty: number
  greigeOrderCount: number
  dyeingOrderCount: number
  inspectionOrderCount: number
  colorInstructionCount: number
}

export interface FormValidationResult {
  valid: boolean
  errors: string[]
}

/**
 * 将从合同接口带出的明细行按 productCode 分组，并初始化工作台状态
 */
export const initReleaseWorkbenchState = (
  lines: ReleaseContractLineVO[],
  clearReleaseQty = false
): ReleaseWorkbenchGroup[] => {
  const groupMap = new Map<string, ReleaseWorkbenchGroup>()

  for (const line of lines) {
    const code = line.productCode || 'UNKNOWN'
    if (!groupMap.has(code)) {
      groupMap.set(code, {
        productCode: line.productCode,
        productName: line.productName,
        composition: line.composition,
        width: line.width,
        gsm: line.gsm,
        colorInstructionMode: 'NONE',
        existingColorInstructionId: undefined,
        newColorInstruction: {
          labFactoryId: null,
          colors: []
        },
        lines: []
      })
    }

    const group = groupMap.get(code)!
    const workbenchLine: ReleaseWorkbenchLine = {
      ...line,
      releaseQty: clearReleaseQty ? 0 : Number(line.remainingQty ?? 0),
      lossRate: Number(line.defaultLossRate ?? 0)
    }
    group.lines.push(workbenchLine)

    // 同步初始化打色单颜色行
    group.newColorInstruction?.colors.push({
      contractLineId: line.contractLineId,
      customerColorNo: line.customerColor || '',
      colorName: line.customerColor || '',
      lightPrimary: 'D65', // 缺省 D65 标准光源
      lightSecondary: '',
      factoryColorNo: '',
      colorCardRef: ''
    })
  }

  return Array.from(groupMap.values())
}

/**
 * 计算底部下达汇总数据（胚布量/染色量/单据数）
 */
export const calculateReleaseSummary = (groups: ReleaseWorkbenchGroup[]): ReleaseSummary => {
  let totalGreigeQty = 0
  let totalDyeingQty = 0
  let greigeOrderCount = 0
  let dyeingOrderCount = 0
  let inspectionOrderCount = 0
  let colorInstructionCount = 0

  for (const group of groups) {
    const activeLines = group.lines.filter((l) => Number(l.releaseQty) > 0)
    if (activeLines.length === 0) {
      continue
    }

    const groupReleaseQty = activeLines.reduce((sum, l) => sum + Number(l.releaseQty || 0), 0)
    totalGreigeQty += groupReleaseQty
    totalDyeingQty += groupReleaseQty

    // 每一个有效下达的产品分组生成 1 张胚布单、1 张染色单、1 张胚布检验单
    greigeOrderCount += 1
    dyeingOrderCount += 1
    inspectionOrderCount += 1

    if (group.colorInstructionMode === 'NEW') {
      colorInstructionCount += 1
    }
  }

  return {
    totalGreigeQty: Number(totalGreigeQty.toFixed(2)),
    totalDyeingQty: Number(totalDyeingQty.toFixed(2)),
    greigeOrderCount,
    dyeingOrderCount,
    inspectionOrderCount,
    colorInstructionCount
  }
}

/**
 * 前端校验下达表单
 */
export const validateReleaseForm = (params: {
  contractStatus?: string
  greigeFactoryId?: number
  dyeingFactoryId?: number
  groups: ReleaseWorkbenchGroup[]
}): FormValidationResult => {
  const errors: string[] = []

  if (params.contractStatus === 'CANCELLED') {
    errors.push('合同已取消，无法下达生产')
  }

  if (!params.greigeFactoryId) {
    errors.push('请选择承织坯布厂')
  }

  if (!params.dyeingFactoryId) {
    errors.push('请选择承染方')
  }

  let hasPositiveReleaseQty = false

  for (const group of params.groups) {
    const activeLines = group.lines.filter((l) => Number(l.releaseQty) > 0)
    if (activeLines.length > 0) {
      hasPositiveReleaseQty = true
    }

    for (const line of group.lines) {
      const releaseQty = Number(line.releaseQty || 0)
      const remainingQty = Number(line.remainingQty || 0)

      if (releaseQty < 0) {
        errors.push(`行【${line.lineNo}】本次下达量不能为负数`)
      }

      if (releaseQty > remainingQty) {
        errors.push(
          `行【${line.lineNo}】本次下达量（${releaseQty}）超出剩余可下达量（${remainingQty}）`
        )
      }
    }

    if (activeLines.length > 0) {
      if (group.colorInstructionMode === 'EXISTING' && !group.existingColorInstructionId) {
        errors.push(`产品【${group.productCode}】请选择已有打色单`)
      }

      if (group.colorInstructionMode === 'NEW') {
        const activeLineIdSet = new Set(activeLines.map((l) => l.contractLineId))
        const colors = (group.newColorInstruction?.colors || []).filter(
          (c) => !c.contractLineId || activeLineIdSet.has(c.contractLineId)
        )
        for (const c of colors) {
          if (!c.lightPrimary || !c.lightPrimary.trim()) {
            errors.push(
              `产品【${group.productCode}】打色单颜色行【${c.customerColorNo || '未命名'}】主灯源不能为空`
            )
          }
        }
      }
    }
  }

  if (!hasPositiveReleaseQty && params.groups.length > 0) {
    errors.push('至少有一行本次下达量必须大于0')
  }

  return {
    valid: errors.length === 0,
    errors
  }
}

/**
 * 组装下达生产请求 VO
 */
export const buildReleaseProductionPayload = (params: {
  contractId: number
  greigeFactoryId: number
  dyeingFactoryId: number
  groups: ReleaseWorkbenchGroup[]
}): ReleaseProductionReqVO => {
  const reqGroups: ReleaseGroupVO[] = []

  for (const group of params.groups) {
    const activeLines: ReleaseLineVO[] = group.lines
      .filter((l) => Number(l.releaseQty) > 0)
      .map((l) => ({
        contractLineId: l.contractLineId,
        customerColor: l.customerColor,
        releaseQty: Number(l.releaseQty),
        lossRate: Number(l.lossRate || 0)
      }))

    if (activeLines.length === 0) {
      continue
    }

    let newColorInstruction: NewColorInstructionVO | undefined = undefined
    if (group.colorInstructionMode === 'NEW' && group.newColorInstruction) {
      const activeLineIdSet = new Set(activeLines.map((l) => l.contractLineId))
      const colors = (group.newColorInstruction.colors || [])
        .filter((c) => !c.contractLineId || activeLineIdSet.has(c.contractLineId))
        .map((c) => ({
          contractLineId: c.contractLineId,
          customerColorNo: c.customerColorNo,
          colorName: c.colorName || c.customerColorNo,
          lightPrimary: c.lightPrimary,
          lightSecondary: c.lightSecondary,
          factoryColorNo: c.factoryColorNo,
          colorCardRef: c.colorCardRef
        }))

      newColorInstruction = {
        labFactoryId: group.newColorInstruction.labFactoryId ?? null,
        colors
      }
    }

    reqGroups.push({
      productId: group.productId,
      productCode: group.productCode,
      productName: group.productName,
      composition: group.composition,
      width: group.width,
      gsm: group.gsm,
      colorInstructionMode: group.colorInstructionMode,
      existingColorInstructionId:
        group.colorInstructionMode === 'EXISTING' ? group.existingColorInstructionId : undefined,
      newColorInstruction,
      lines: activeLines
    })
  }

  return {
    contractId: params.contractId,
    greigeFactoryId: params.greigeFactoryId,
    dyeingFactoryId: params.dyeingFactoryId,
    groups: reqGroups
  }
}
