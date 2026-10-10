package com.luna.lunasrv.module.lunaerp.approval.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.module.lunaerp.approval.event.ApprovalCompletedEvent;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 审批通过完成事件监听器（对齐 ADR-0017 与 Issue 07）
 *
 * 监听 {@link ApprovalCompletedEvent}，在当前事务提交前（BEFORE_COMMIT）
 * 路由并触发对应业务单据的 {@link ApprovalHandler} 执行生效或过账副作用。
 * 与审批状态更新处于同一事务，业务侧一旦抛出异常立即触发事务回滚，
 * 确保「审批通过 = 业务生效」的强原子性。
 *
 * @author luna
 */
@Component
@Slf4j
public class ApprovalCompletedEventListener {

    @Resource
    private List<ApprovalHandler> handlers;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onApprovalCompleted(ApprovalCompletedEvent event) {
        if (event == null || StrUtil.isBlank(event.getBizType()) || event.getBizId() == null) {
            return;
        }
        log.info("[onApprovalCompleted] 监听到审批通过完成事件: bizType={}, bizId={}", event.getBizType(), event.getBizId());
        if (CollUtil.isEmpty(handlers)) {
            log.info("[onApprovalCompleted] 当前无注册的 ApprovalHandler, 忽略副作用: bizType={}", event.getBizType());
            return;
        }

        boolean matched = false;
        for (ApprovalHandler handler : handlers) {
            if (handler != null && handler.supports(event.getBizType())) {
                log.info("[onApprovalCompleted] 路由到处理器 [{}], 执行审批通过副作用: bizType={}, bizId={}",
                        handler.getClass().getSimpleName(), event.getBizType(), event.getBizId());
                handler.onApproved(event.getBizId());
                matched = true;
            }
        }

        if (!matched) {
            log.info("[onApprovalCompleted] 业务类型 [{}] 暂无注册的 ApprovalHandler（预留接入点）", event.getBizType());
        }
    }

}
