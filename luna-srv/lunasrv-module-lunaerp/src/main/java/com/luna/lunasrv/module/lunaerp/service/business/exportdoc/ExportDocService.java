package com.luna.lunasrv.module.lunaerp.service.business.exportdoc;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocSaveReqVO;

/**
 * 业务管理-出口单证 Service 接口
 *
 * @author luna
 */
public interface ExportDocService {

    /**
     * 创建出口单证套（聚合保存主表+明细）
     *
     * @param createReqVO 创建信息
     * @return 单证套编号
     */
    Long createExportDoc(ExportDocSaveReqVO createReqVO);

    /**
     * 更新出口单证套（仅 DRAFT 态允许；聚合替换明细）
     *
     * @param updateReqVO 更新信息
     */
    void updateExportDoc(ExportDocSaveReqVO updateReqVO);

    /**
     * 删除出口单证套（仅 DRAFT 态允许；级联软删明细）
     *
     * @param id 单证套ID
     */
    void deleteExportDoc(Long id);

    /**
     * 获取出口单证套详情（含明细列表）
     *
     * @param id 单证套ID
     * @return 单证套详情
     */
    ExportDocRespVO getExportDoc(Long id);

    /**
     * 分页查询出口单证套
     *
     * @param reqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<ExportDocRespVO> getExportDocPage(ExportDocPageReqVO reqVO);

    /**
     * 只读获取下一个建议单证套号（只读预览不占号，含软删行，ADR-0004）
     *
     * @return 建议单证套号，如 ED20260001
     */
    String getNextDocNo();

    /**
     * 获取当前租户的卖方出口配置（若无配置返回空对象）
     *
     * @return 卖方出口配置
     */
    com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportSellerConfigRespVO getSellerConfig();

    /**
     * 保存/更新当前租户的卖方出口配置（upsert 幂等）
     *
     * @param saveReqVO 保存请求
     */
    void saveSellerConfig(com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportSellerConfigSaveReqVO saveReqVO);

    /**
     * 定稿出口单证套（DRAFT -> FINALIZED，校验货物明细非空）
     *
     * @param id 单证套ID
     */
    void finalizeExportDoc(Long id);

    /**
     * 撤销定稿出口单证套（FINALIZED -> DRAFT）
     *
     * @param id 单证套ID
     */
    void revokeExportDoc(Long id);

    /**
     * 从销售合同带出草稿单证套数据（不落库）
     *
     * @param contractId 合同ID
     * @return 预填的单证套草稿
     */
    ExportDocRespVO getDraftFromContract(Long contractId);

    /**
     * 从发货单带出草稿单证套数据（不落库）
     *
     * @param shipmentId 发货单ID
     * @return 预填的单证套草稿
     */
    ExportDocRespVO getDraftFromShipment(Long shipmentId);

    /**
     * 重新同步源单据数据（仅 DRAFT 且非 MANUAL 来源；只补充空白字段、不覆盖已填字段）
     *
     * @param id 单证套ID
     */
    void resyncFromSource(Long id);

    /**
     * 获取出口单证打印数据（独立 VO，豁免脱敏，含拼好的 qrUrl + 银行全量字段 + 汇总合计）
     *
     * @param id 单证套ID
     * @param template 打印模板：PI / CI / PL
     * @return 打印数据
     */
    com.luna.lunasrv.module.lunaerp.controller.business.exportdoc.vo.ExportDocPrintRespVO getExportDocPrintData(Long id, String template);

}
