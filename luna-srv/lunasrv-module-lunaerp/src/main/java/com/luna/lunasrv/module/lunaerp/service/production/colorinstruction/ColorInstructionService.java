package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;

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

    /**
     * 预览下一个打色单号（DS+yyyyMMdd+3位流水，按天取号，只读预览不占号）
     *
     * @return 下一个单号
     */
    String getNextInstructionNo();

    /**
     * 创建打色指示单
     *
     * @param createReqVO 创建请求参数
     * @return 打色指示单ID
     */
    Long createColorInstruction(ColorInstructionSaveReqVO createReqVO);

    /**
     * 软删除打色指示单（仅 PENDING 状态，级联逻辑删除颜色行，单号不释放）
     *
     * @param id 打色指示单ID
     */
    void deleteColorInstruction(Long id);

    /**
     * 更新打色指示单（差异保存颜色行，含行状态守卫与行号重排）
     *
     * @param updateReqVO 更新请求参数
     */
    void updateColorInstruction(ColorInstructionSaveReqVO updateReqVO);

}
