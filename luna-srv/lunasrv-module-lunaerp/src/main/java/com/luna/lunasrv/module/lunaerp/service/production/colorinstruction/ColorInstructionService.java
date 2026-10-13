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
     * 获得打色指示单分页列表（含防 N+1 颜色行摘要 chips）
     *
     * @param pageReqVO 分页查询条件
     * @return 打色指示单分页结果
     */
    PageResult<ColorInstructionRespVO> getColorInstructionPage(ColorInstructionPageReqVO pageReqVO);

    /**
     * 获得打色指示单详情
     *
     * @param id 打色指示单ID
     * @return 打色指示单详情
     */
    ColorInstructionRespVO getColorInstruction(Long id);

}
