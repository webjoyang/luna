package com.luna.lunasrv.module.lunaerp.common.notify;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.system.api.notify.NotifyMessageSendApi;
import com.luna.lunasrv.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * LunaERP 站内消息通知助手
 *
 * 核心设计决策（零新表全复用芋道 notify）：
 * 1. 统一封装底层 {@link NotifyMessageSendApi#sendSingleMessageToAdmin}，不新增表、不新增第二个角标。
 * 2. 点击跳转零 DDL：消息 templateParams 固定携带 bizType / bizId / bizNo，前端铃铛点击按 bizType 路由表跳转。
 * 3. 旁路通知容错守卫（BEFORE_COMMIT 同事务语义）：通知发送失败仅记录 error 日志，不向上抛出异常，绝不回滚业务事务。
 * 4. 会签场景支持每人各发一条；或签有人处理后其余待办在引擎端自动关闭，已发消息不撤回（铃铛自然沉淀为已读/忽略）。
 *
 * @author luna
 */
@Component
@Slf4j
public class LunaNotifyHelper {

    private static LunaNotifyHelper instance;

    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static LunaNotifyHelper getInstance() {
        return instance;
    }

    /**
     * 发送单条站内信（基础方法）
     *
     * @param userId       接收用户编号（管理员/内部用户）
     * @param templateCode 模板编码
     * @param params       模板内容占位符参数
     * @param bizJump      业务跳转上下文（携带 bizType/bizId/bizNo）
     * @return 消息 ID，发送失败或异常时返回 null
     */
    public Long send(Long userId, String templateCode, Map<String, Object> params, BizJump bizJump) {
        if (userId == null || StrUtil.isBlank(templateCode)) {
            log.warn("[send][参数不合法，跳过消息发送: userId={}, templateCode={}]", userId, templateCode);
            return null;
        }

        try {
            Map<String, Object> templateParams = new HashMap<>();
            if (params != null) {
                templateParams.putAll(params);
            }
            if (bizJump != null) {
                if (bizJump.getBizType() != null) {
                    templateParams.put("bizType", bizJump.getBizType());
                }
                if (bizJump.getBizId() != null) {
                    templateParams.put("bizId", bizJump.getBizId());
                }
                if (bizJump.getBizNo() != null) {
                    templateParams.put("bizNo", bizJump.getBizNo());
                }
            }

            NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
            reqDTO.setUserId(userId);
            reqDTO.setTemplateCode(templateCode);
            reqDTO.setTemplateParams(templateParams);

            return notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
        } catch (Throwable t) {
            // 旁路通知容错：记录 error 日志，不回滚调用方业务事务
            log.error("[send][站内消息发送失败，已容忍错误并不阻塞主业务事务: userId={}, templateCode={}, params={}, bizJump={}]",
                    userId, templateCode, params, bizJump, t);
            return null;
        }
    }

    /**
     * 发送单条站内信（模板枚举重载）
     */
    public Long send(Long userId, LunaNotifyTemplateEnum templateEnum, Map<String, Object> params, BizJump bizJump) {
        if (templateEnum == null) {
            log.warn("[send][模板枚举为空，跳过消息发送: userId={}]", userId);
            return null;
        }
        return send(userId, templateEnum.getCode(), params, bizJump);
    }

    /**
     * 批量发送站内信给多位用户
     */
    public List<Long> sendBatch(Collection<Long> userIds, String templateCode, Map<String, Object> params, BizJump bizJump) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        List<Long> messageIds = new ArrayList<>();
        for (Long userId : userIds) {
            Long msgId = send(userId, templateCode, params, bizJump);
            if (msgId != null) {
                messageIds.add(msgId);
            }
        }
        return messageIds;
    }

    /**
     * 批量发送站内信给多位用户（模板枚举重载）
     */
    public List<Long> sendBatch(Collection<Long> userIds, LunaNotifyTemplateEnum templateEnum, Map<String, Object> params, BizJump bizJump) {
        if (templateEnum == null) {
            return Collections.emptyList();
        }
        return sendBatch(userIds, templateEnum.getCode(), params, bizJump);
    }

    // ==========================================
    // 审批引擎专用便捷触达方法（契约对齐 02 号 spec）
    // ==========================================

    /**
     * 发送审批待办通知给审批人（单人 / 或签）
     *
     * @param approverUserId 审批人用户编号
     * @param bizType        业务单据类型（如 sales_contract 等，若为空则默认为 APPROVAL）
     * @param instanceId     审批实例编号
     * @param bizNo          业务单号
     * @param bizName        业务类型名称（如"销售合同"）
     * @param submitterName  提交人姓名
     * @return 消息 ID
     */
    public Long sendApprovalTodo(Long approverUserId, String bizType, Long instanceId,
                                 String bizNo, String bizName, String submitterName) {
        Map<String, Object> params = new HashMap<>();
        params.put("bizName", StrUtil.nullToDefault(bizName, "业务单据"));
        params.put("bizNo", StrUtil.nullToDefault(bizNo, ""));
        params.put("submitterName", StrUtil.nullToDefault(submitterName, "未知"));

        BizJump bizJump = BizJump.builder()
                .bizType("APPROVAL")
                .bizId(instanceId)
                .bizNo(bizNo)
                .build();

        return send(approverUserId, LunaNotifyTemplateEnum.LUNA_APPROVAL_TODO, params, bizJump);
    }

    /**
     * 批量发送审批待办通知给各审批人（会签场景：每个审批人各一条）
     *
     * @param approverUserIds 审批人用户编号集合
     * @param bizType         业务单据类型
     * @param instanceId      审批实例编号
     * @param bizNo           业务单号
     * @param bizName         业务类型名称
     * @param submitterName   提交人姓名
     * @return 成功发送的消息 ID 列表
     */
    public List<Long> sendApprovalTodoBatch(Collection<Long> approverUserIds, String bizType, Long instanceId,
                                            String bizNo, String bizName, String submitterName) {
        if (CollUtil.isEmpty(approverUserIds)) {
            return Collections.emptyList();
        }
        List<Long> msgIds = new ArrayList<>();
        for (Long approverId : approverUserIds) {
            Long id = sendApprovalTodo(approverId, bizType, instanceId, bizNo, bizName, submitterName);
            if (id != null) {
                msgIds.add(id);
            }
        }
        return msgIds;
    }

    /**
     * 发送审批通过通知给发起人
     *
     * @param submitterUserId 发起人用户编号
     * @param bizType         业务单据类型
     * @param instanceId      审批实例编号
     * @param bizNo           业务单号
     * @param bizName         业务类型名称
     * @return 消息 ID
     */
    public Long sendApprovalApproved(Long submitterUserId, String bizType, Long instanceId,
                                     String bizNo, String bizName) {
        Map<String, Object> params = new HashMap<>();
        params.put("bizName", StrUtil.nullToDefault(bizName, "业务单据"));
        params.put("bizNo", StrUtil.nullToDefault(bizNo, ""));

        BizJump bizJump = BizJump.builder()
                .bizType("APPROVAL")
                .bizId(instanceId)
                .bizNo(bizNo)
                .build();

        return send(submitterUserId, LunaNotifyTemplateEnum.LUNA_APPROVAL_APPROVED, params, bizJump);
    }

    /**
     * 发送审批驳回通知给发起人（内容携带驳回意见）
     *
     * @param submitterUserId 发起人用户编号
     * @param bizType         业务单据类型
     * @param instanceId      审批实例编号
     * @param bizNo           业务单号
     * @param bizName         业务类型名称
     * @param reason          驳回意见
     * @return 消息 ID
     */
    public Long sendApprovalRejected(Long submitterUserId, String bizType, Long instanceId,
                                     String bizNo, String bizName, String reason) {
        Map<String, Object> params = new HashMap<>();
        params.put("bizName", StrUtil.nullToDefault(bizName, "业务单据"));
        params.put("bizNo", StrUtil.nullToDefault(bizNo, ""));
        params.put("reason", StrUtil.nullToDefault(reason, "无"));

        BizJump bizJump = BizJump.builder()
                .bizType("APPROVAL")
                .bizId(instanceId)
                .bizNo(bizNo)
                .build();

        return send(submitterUserId, LunaNotifyTemplateEnum.LUNA_APPROVAL_REJECTED, params, bizJump);
    }

    /**
     * 发送对账单驳回通知给制单财务（旁路通知，失败不阻塞事务）
     *
     * @param creatorUserId 制单人用户编号
     * @param statementId   对账单ID
     * @param statementNo   对账单号
     * @param customerName  客户名称
     * @param reason        驳回原因
     * @return 消息 ID
     */
    public Long sendStatementRejected(Long creatorUserId, Long statementId, String statementNo,
                                      String customerName, String reason) {
        Map<String, Object> params = new HashMap<>();
        params.put("bizName", "客户对账单");
        params.put("bizNo", StrUtil.nullToDefault(statementNo, ""));
        params.put("customerName", StrUtil.nullToDefault(customerName, ""));
        params.put("reason", StrUtil.nullToDefault(reason, "无"));

        BizJump bizJump = BizJump.builder()
                .bizType("STATEMENT")
                .bizId(statementId)
                .bizNo(statementNo)
                .build();

        return send(creatorUserId, LunaNotifyTemplateEnum.LUNA_STATEMENT_REJECTED, params, bizJump);
    }

    // ==========================================
    // 静态便捷方法
    // ==========================================

    public static Long sendMsg(Long userId, String templateCode, Map<String, Object> params, BizJump bizJump) {
        if (instance != null) {
            return instance.send(userId, templateCode, params, bizJump);
        }
        log.warn("[sendMsg][LunaNotifyHelper 实例尚未初始化]");
        return null;
    }

    public static Long sendMsg(Long userId, LunaNotifyTemplateEnum templateEnum, Map<String, Object> params, BizJump bizJump) {
        if (instance != null) {
            return instance.send(userId, templateEnum, params, bizJump);
        }
        log.warn("[sendMsg][LunaNotifyHelper 实例尚未初始化]");
        return null;
    }
}
