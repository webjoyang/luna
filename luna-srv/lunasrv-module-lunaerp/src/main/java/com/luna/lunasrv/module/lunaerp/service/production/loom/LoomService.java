package com.luna.lunasrv.module.lunaerp.service.production.loom;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomSaveReqVO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 生产管理 - 织机 Service 接口
 *
 * @author luna
 */
public interface LoomService {

    /**
     * 创建织机
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLoom(@Valid LoomSaveReqVO createReqVO);

    /**
     * 更新织机
     *
     * @param updateReqVO 更新信息
     */
    void updateLoom(@Valid LoomSaveReqVO updateReqVO);

    /**
     * 删除织机（不提供物理删除；被机台任务引用时禁删并引导停用）
     *
     * @param id 织机ID
     */
    void deleteLoom(Long id);

    /**
     * 获得织机
     *
     * @param id 织机ID
     * @return 织机
     */
    LoomRespVO getLoom(Long id);

    /**
     * 获得织机分页
     *
     * @param pageReqVO 分页查询
     * @return 织机分页
     */
    PageResult<LoomRespVO> getLoomPage(LoomPageReqVO pageReqVO);

    /**
     * 获得所有织机简表（供下拉选择使用，默认 ACTIVE）
     *
     * @param status 状态（可空，默认查 ACTIVE）
     * @return 织机列表
     */
    List<LoomRespVO> getLoomListAll(String status);

    /**
     * 预览下一个织机机台号（只读、不占号）
     *
     * @return 机台号
     */
    String getNextLoomNo();

}
