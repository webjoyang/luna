package com.luna.lunasrv.module.lunaerp.service.production.yarnrecipebom;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnrecipebom.vo.YarnRecipeBomSaveReqVO;
import jakarta.validation.Valid;

/**
 * 生产管理-纱线配比BOM Service 接口
 *
 * @author luna
 */
public interface YarnRecipeBomService {

    /**
     * 创建配比BOM
     *
     * @param createReqVO 创建信息
     * @return 配比BOM编号
     */
    Long createYarnRecipeBom(@Valid YarnRecipeBomSaveReqVO createReqVO);

    /**
     * 更新配比BOM
     *
     * @param updateReqVO 更新信息
     */
    void updateYarnRecipeBom(@Valid YarnRecipeBomSaveReqVO updateReqVO);

    /**
     * 删除配比BOM（级联逻辑删除明细）
     *
     * @param id 配比BOM编号
     */
    void deleteYarnRecipeBom(Long id);

    /**
     * 获得配比BOM详情（内嵌明细）
     *
     * @param id 配比BOM编号
     * @return 配比BOM详情
     */
    YarnRecipeBomRespVO getYarnRecipeBom(Long id);

    /**
     * 获得配比BOM分页
     *
     * @param pageReqVO 分页查询条件
     * @return 配比BOM分页
     */
    PageResult<YarnRecipeBomRespVO> getYarnRecipeBomPage(YarnRecipeBomPageReqVO pageReqVO);

    /**
     * 按产品档案ID获取配比BOM（用于测算，内嵌明细）
     *
     * @param productId 产品档案ID
     * @return 配比BOM详情
     */
    YarnRecipeBomRespVO getYarnRecipeBomByProductId(Long productId);

}
