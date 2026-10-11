package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.SupplierReconCandidatesRespVO;

import java.time.LocalDate;

/**
 * 财务管理 - 供应商对账唯一取数服务接口
 *
 * 核心真相来源：从财务/采购视角，选定供应商与对账区间后，
 * 产出一份"未被任何有效对账单引用过"的候选明细集合（内存装配、不落库）。
 *
 * 老系统 P0 教训：取数口径收敛为单一服务、禁止他处复制口径。
 *
 * @author luna
 */
public interface SupplierReconSourceService {

    /**
     * 获取指定供应商在指定对账区间内的候选明细集合
     *
     * @param supplierId 供应商ID
     * @param periodStart 对账区间开始日期（含端点）
     * @param periodEnd 对账区间结束日期（含端点）
     * @return 候选明细集合（包含加工费候选行与采购货款候选行，已排除被 DRAFT/CONFIRMED 对账单引用的指纹）
     */
    SupplierReconCandidatesRespVO getCandidateItems(Long supplierId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 生成防重复取数指纹
     *
     * @param sourceRefType 源单据类型（INSPECTION / STOCK_IN_ITEM）
     * @param sourceRefId 源单据主表ID（检验单ID / 入库单ID）
     * @param sourceRefItemId 源单据明细ID（入库明细ID，检验单为汇总行时为空）
     * @return 指纹字符串
     */
    static String buildFingerprint(String sourceRefType, Long sourceRefId, Long sourceRefItemId) {
        if (sourceRefType == null || sourceRefId == null) {
            return "";
        }
        if (sourceRefItemId != null) {
            return sourceRefType + ":" + sourceRefId + ":" + sourceRefItemId;
        }
        return sourceRefType + ":" + sourceRefId;
    }

}
