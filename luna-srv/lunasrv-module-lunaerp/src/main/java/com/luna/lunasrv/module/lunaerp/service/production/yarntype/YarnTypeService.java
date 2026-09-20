package com.luna.lunasrv.module.lunaerp.service.production.yarntype;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypeSaveReqVO;
import jakarta.validation.Valid;

/**
 * 生产管理-纱线材质 Service 接口
 *
 * @author luna
 */
public interface YarnTypeService {

    /**
     * 创建纱线材质
     *
     * @param createReqVO 创建信息
     * @return 材质编号
     */
    Long createYarnType(@Valid YarnTypeSaveReqVO createReqVO);

    /**
     * 更新纱线材质
     *
     * @param updateReqVO 更新信息
     */
    void updateYarnType(@Valid YarnTypeSaveReqVO updateReqVO);

    /**
     * 删除纱线材质（先置 INACTIVE 再逻辑删除）
     *
     * @param id 材质编号
     */
    void deleteYarnType(Long id);

    /**
     * 获得纱线材质
     *
     * @param id 材质编号
     * @return 材质信息
     */
    YarnTypeRespVO getYarnType(Long id);

    /**
     * 获得纱线材质分页
     *
     * @param pageReqVO 分页查询
     * @return 材质分页结果
     */
    PageResult<YarnTypeRespVO> getYarnTypePage(YarnTypePageReqVO pageReqVO);

}
