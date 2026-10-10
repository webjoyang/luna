package com.luna.lunasrv.module.lunaerp.service.business.quotation;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo.QuotationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo.QuotationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.quotation.vo.QuotationSaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 业务管理 - 报价单 Service 接口
 *
 * @author luna
 */
public interface QuotationService {

    /**
     * 创建报价单（DRAFT，version_no=1）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createQuotation(@Valid QuotationSaveReqVO createReqVO);

    /**
     * 更新报价单（仅 DRAFT 允许修改，整体替换明细并权威重算）
     *
     * @param updateReqVO 更新信息
     */
    void updateQuotation(@Valid QuotationSaveReqVO updateReqVO);

    /**
     * 删除报价单（仅 DRAFT 允许删除，级联软删明细入回收站）
     *
     * @param id 报价单编号
     */
    void deleteQuotation(Long id);

    /**
     * 确认报价（DRAFT -> CONFIRMED，条件更新守卫）
     *
     * @param id 报价单编号
     */
    void confirmQuotation(Long id);

    /**
     * 取消报价（CONFIRMED -> CANCELLED，条件更新守卫）
     *
     * @param id 报价单编号
     */
    void cancelQuotation(Long id);

    /**
     * 获取报价单详情（含明细，按权限脱敏）
     *
     * @param id 报价单编号
     * @return 报价单详情
     */
    QuotationRespVO getQuotation(Long id);

    /**
     * 分页查询报价单列表（默认仅看最新版本，按权限脱敏）
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<QuotationRespVO> getQuotationPage(QuotationPageReqVO pageReqVO);

    /**
     * 只读预览下一个自动生成的报价单号（QT{yyyy}NNNN，扫描含删不占号）
     *
     * @return 下一个单号
     */
    String getNextQuotationNo();

    /**
     * 修订报价单（CONFIRMED -> SUPERSEDED，生成新草稿 version_no + 1）- 09 票
     *
     * @param id 待修订的已确认报价单编号
     * @return 新草稿报价单编号
     */
    Long reviseQuotation(Long id);

    /**
     * 查询指定单号的所有版本记录（按版本号降序）- 09 票
     *
     * @param quotationNo 报价单号
     * @return 版本列表
     */
    List<QuotationRespVO> getQuotationVersions(String quotationNo);

}
