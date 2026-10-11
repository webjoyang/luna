package com.luna.lunasrv.module.lunaerp.service.production.colorinstruction;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionRespVO;

import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionColorRejectRestrikeReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPrintRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;

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

    /**
     * 预览下一个打色单号（DS+yyyyMMdd+3位日流水，按天取号，扫描含已删除行，ADR-0006）
     *
     * @return 下一个建议单号
     */
    String getNextNo();

    /**
     * 创建打色指示单
     *
     * @param createReqVO 创建请求参数
     * @return 新增打色单ID
     */
    Long createColorInstruction(ColorInstructionSaveReqVO createReqVO);

    /**
     * 更新打色指示单（差异保存颜色行，含行状态守卫与 lineNo 服务端重排）
     *
     * @param updateReqVO 更新请求参数
     */
    void updateColorInstruction(ColorInstructionSaveReqVO updateReqVO);

    /**
     * 软删除打色指示单（仅 PENDING 状态，级联逻辑删除主表和颜色行）
     *
     * @param id 打色单ID
     */
    void deleteColorInstruction(Long id);

    /**
     * 开始打色（PENDING -> IN_PROGRESS）
     *
     * @param id 打色单ID
     */
    void startColorInstruction(Long id);

    /**
     * 通过打色颜色行（PENDING -> APPROVED，便利规则：单据 PENDING 自动转 IN_PROGRESS）
     *
     * @param lineId 颜色行ID
     */
    void approveColorLine(Long lineId);

    /**
     * 驳回重打打色颜色行（PENDING/APPROVED -> REJECTED 并克隆 round+1 新行，重排 lineNo）
     *
     * @param reqVO 驳回重打参数
     * @return 新增克隆颜色行ID
     */
    Long rejectRestrikeColorLine(ColorInstructionColorRejectRestrikeReqVO reqVO);

    /**
     * 获得打色指示单套打数据（独立 VO，无金额字段，豁免脱敏）
     *
     * @param id 打色单ID
     * @return 打色指示单套打数据
     */
    ColorInstructionPrintRespVO getColorInstructionPrintData(Long id);

}
