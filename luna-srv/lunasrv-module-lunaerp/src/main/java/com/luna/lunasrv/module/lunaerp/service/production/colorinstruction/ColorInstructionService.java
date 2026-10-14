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

    /**
     * 开始打色（PENDING -> IN_PROGRESS，条件更新防并发）
     *
     * @param id 打色指示单ID
     */
    void startColorInstruction(Long id);

    /**
     * 通过打色颜色行（PENDING -> APPROVED，便利规则级联主单 PENDING -> IN_PROGRESS）
     *
     * @param lineId 颜色行ID
     */
    void approveColorLine(Long lineId);

    /**
     * 驳回重打颜色行（PENDING/APPROVED -> REJECTED + 克隆新行 round+1，样布照片不继承，重排 lineNo）
     *
     * @param lineId 颜色行ID
     * @param rejectReason 驳回意见
     */
    void rejectRestrikeColorLine(Long lineId, String rejectReason);

    /**
     * 确认打色指示单（三道守卫：IN_PROGRESS态、无PENDING行、至少1行APPROVED；写入确认信息与防并发更新）
     *
     * @param reqVO 确认请求参数
     */
    void confirmColorInstruction(com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionConfirmReqVO reqVO);

    /**
     * 校验打色指示单是否可供下游单据（胚布单/染色单）引用
     * <p>
     * 校验规则（下沉领域守卫，避免下游 Feature Envy 与逻辑重复）：
     * 1. 存在且未软删
     * 2. 属于当前租户
     * 3. 状态 ∈ {CONFIRMED, IN_PRODUCTION}
     * 4. 客户口径一致（若下游单据指定了客户）
     * 5. 产品口径一致（若下游单据指定了产品ID或产品编码）
     *
     * @param colorInstructionId 打色指示单ID（允许为null，为null直接返回null）
     * @param customerId 预期客户ID（可空）
     * @param productId 预期产品ID（可空）
     * @param productCode 预期产品编码（可空）
     * @param notFoundErrorCode 校验失败时抛出的下游错误码
     * @return 校验通过的打色指示单DO，若colorInstructionId为null则返回null
     */
    com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO validateColorInstructionForDownstream(
            Long colorInstructionId,
            Long customerId,
            Long productId,
            String productCode,
            com.luna.lunasrv.framework.common.exception.ErrorCode notFoundErrorCode);

    /**
     * 自动标记打色指示单为已投产（CONFIRMED -> IN_PRODUCTION）
     * <p>
     * 条件更新：WHERE id=? AND tenant_id=? AND status='CONFIRMED' AND deleted=0
     * 影响行数 0 幂等静默不报错（已投产、已作废、已回退或单据不存在均安全静默）
     *
     * @param id 打色指示单ID
     */
    void markInProduction(Long id);

    /**
     * 检查打色指示单是否存在有效下游单据引用（胚布单 / 染色单）
     * <p>
     * 规则：查胚布单 + 染色单 status ≠ 'CANCELLED' 且 deleted = 0 且 color_instruction_id = id 的行数 > 0
     *
     * @param id 打色指示单ID
     * @return 是否存在有效下游引用
     */
    boolean hasActiveDownstreamRef(Long id);

    /**
     * 回退打色指示单（CONFIRMED -> IN_PROGRESS）
     * <p>
     * 守卫：
     * 1. 单据存在且状态为 CONFIRMED
     * 2. 无任何下游有效引用（status ≠ CANCELLED 且未删除）
     * 3. 条件更新防并发
     * 4. 颜色行状态保持不回退
     *
     * @param id 打色指示单ID
     */
    void rollbackColorInstruction(Long id);

    /**
     * 作废打色指示单（PENDING / IN_PROGRESS / CONFIRMED -> VOID）
     * <p>
     * 守卫：
     * 1. 单据存在且状态 ∈ {PENDING, IN_PROGRESS, CONFIRMED}
     * 2. IN_PRODUCTION 禁作废（报 1_030_032_002）
     * 3. 无任何下游有效引用（status ≠ CANCELLED 且未删除），存在则报 1_030_032_005
     * 4. 写入 voidReason，条件更新防并发
     *
     * @param reqVO 作废请求参数
     */
    void voidColorInstruction(com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionVoidReqVO reqVO);

}



