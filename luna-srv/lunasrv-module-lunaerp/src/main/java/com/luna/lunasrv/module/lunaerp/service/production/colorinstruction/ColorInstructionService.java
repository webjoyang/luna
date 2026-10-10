package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;

/**
 * 生产管理 - 打色指示单 Service 接口
 *
 * @author luna
 */
public interface ColorInstructionService {

    /**
     * 获得打色指示单分页（带颜色行摘要 chips，防 N+1 批量加载）
     *
     * @param reqVO 分页查询条件
     * @return 打色指示单分页结果
     */
    PageResult<ColorInstructionRespVO> getColorInstructionPage(ColorInstructionPageReqVO reqVO);

    /**
     * 获得打色指示单详情（含颜色行完整列表）
     *
     * @param id 打色单ID
     * @return 打色指示单详情
     */
    ColorInstructionRespVO getColorInstruction(Long id);

}
