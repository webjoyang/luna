package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.SupplierReconSourceItemRespVO;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 供应商对账唯一取数服务接口
 *
 * <p>唯一取数真相，禁止在他处复制取数口径：
 * 1. 加工费候选（PROCESS）：仅 status=COMPLETED 检验单，承检方=供应商，单据日期 ∈ 区间，一张单汇总一行（Σ重量/Σ米数/卷数，全等级计入，GREIGE 缺省按米，FINISHED 缺省按公斤）；
 * 2. 采购货款候选（PURCHASE）：仅主表 COMPLETED 且 in_type=PURCHASE 入库明细，按供应商与单据日期过滤，退货入库（RETURN）排除；
 * 3. 防重指纹排除：排除被 DRAFT/CONFIRMED 对账单引用的指纹，CANCELLED 对账单指纹自动释放回候选池；
 * 4. 纯内存候选装配，不落库、零回写。</p>
 *
 * @author luna
 */
public interface SupplierReconSourceService {

    /**
     * 获取指定供应商在指定对账区间内【未被有效对账单引用】的可选候选明细行列表
     *
     * @param supplierId 供应商ID
     * @param periodStart 对账区间起始日期（含端点）
     * @param periodEnd 对账区间截止日期（含端点）
     * @return 候选明细行列表（已排除有效对账单引用的指纹）
     */
    List<SupplierReconSourceItemRespVO> getAvailableCandidateItems(Long supplierId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 获取指定供应商在指定对账区间内的全部候选明细行（未过滤防重指纹）
     *
     * @param supplierId 供应商ID
     * @param periodStart 对账区间起始日期（含端点）
     * @param periodEnd 对账区间截止日期（含端点）
     * @return 全部候选明细行列表
     */
    List<SupplierReconSourceItemRespVO> getAllCandidateItems(Long supplierId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 检查给定的指纹集合中，哪些已被有效对账单（DRAFT/CONFIRMED）引用
     *
     * @param fingerprints 防重指纹集合
     * @return 已被占用的指纹 -> 占用对账单号 reconNo 映射 Map
     */
    Map<String, String> findOccupiedFingerprints(Collection<String> fingerprints);

    /**
     * 构造来源防重指纹
     *
     * @param sourceRefType 来源类型（INSPECTION / STOCK_IN_ITEM）
     * @param sourceRefId 来源单据主表ID
     * @param sourceRefItemId 来源单据明细ID（检验单汇总行可为 null）
     * @return 格式化指纹字符串
     */
    String buildFingerprint(String sourceRefType, Long sourceRefId, Long sourceRefItemId);

}
