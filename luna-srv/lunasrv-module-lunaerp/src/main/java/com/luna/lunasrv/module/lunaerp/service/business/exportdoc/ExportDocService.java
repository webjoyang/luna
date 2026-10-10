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

}
