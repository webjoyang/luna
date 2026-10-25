package com.luna.lunasrv.module.lunaerp.approval.config;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.ApprovalConfigPageReqVO;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.ApprovalConfigRespVO;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.ApprovalConfigSaveReqVO;
import jakarta.validation.Valid;

/**
 * 审批流程配置 Service 接口
 *
 * @author luna
 */
public interface ApprovalConfigService {

    /**
     * 新建审批流程配置（含节点）
     *
     * @param createReqVO 创建信息
     * @return 流程配置ID
     */
    Long createApprovalConfig(@Valid ApprovalConfigSaveReqVO createReqVO);

    /**
     * 更新审批流程配置（节点整体替换：删旧插新）
     *
     * @param id 配置ID
     * @param updateReqVO 更新信息
     */
    void updateApprovalConfig(Long id, @Valid ApprovalConfigSaveReqVO updateReqVO);

    /**
     * 启用/停用审批流程配置
     *
     * @param id 配置ID
     * @param status 目标状态（ENABLED / DISABLED）
     */
    void updateApprovalConfigStatus(Long id, String status);

    /**
     * 删除审批流程配置（软删，仅未被实例引用）
     *
     * @param id 配置ID
     */
    void deleteApprovalConfig(Long id);

    /**
     * 获得审批流程配置详情（含节点列表按 sort 升序）
     *
     * @param id 配置ID
     * @return 流程详情
     */
    ApprovalConfigRespVO getApprovalConfig(Long id);

    /**
     * 获得审批流程配置分页
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<ApprovalConfigRespVO> getApprovalConfigPage(ApprovalConfigPageReqVO pageReqVO);

    /**
     * 按业务单据类型获得已启用的流程配置（若无启用流程返回 null）
     *
     * @param bizType 业务单据类型
     * @return 流程配置
     */
    ApprovalConfigDO getEnabledConfigByBizType(String bizType);

}
