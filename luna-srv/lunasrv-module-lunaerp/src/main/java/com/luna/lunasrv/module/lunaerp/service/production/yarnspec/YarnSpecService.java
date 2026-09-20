package com.luna.lunasrv.module.lunaerp.service.production.yarnspec;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecCascadeOptionVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecSaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 生产管理-纱线规格 Service 接口
 *
 * @author luna
 */
public interface YarnSpecService {

    /**
     * 创建纱线规格
     *
     * @param createReqVO 创建信息
     * @return 规格编号
     */
    Long createYarnSpec(@Valid YarnSpecSaveReqVO createReqVO);

    /**
     * 更新纱线规格
     *
     * @param updateReqVO 更新信息
     */
    void updateYarnSpec(@Valid YarnSpecSaveReqVO updateReqVO);

    /**
     * 删除纱线规格（先置 INACTIVE 再逻辑删除）
     *
     * @param id 规格编号
     */
    void deleteYarnSpec(Long id);

    /**
     * 获得纱线规格
     *
     * @param id 规格编号
     * @return 规格信息
     */
    YarnSpecRespVO getYarnSpec(Long id);

    /**
     * 获得纱线规格分页
     *
     * @param pageReqVO 分页查询
     * @return 规格分页结果
     */
    PageResult<YarnSpecRespVO> getYarnSpecPage(YarnSpecPageReqVO pageReqVO);

    /**
     * 获得两级级联数据源（供采购单/配比 BOM 等使用）
     *
     * @param keyword 规格名搜索关键词（可空）
     * @return 两级级联选项列表
     */
    List<YarnSpecCascadeOptionVO> getCascadeOptions(String keyword);

}
