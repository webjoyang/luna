package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 销售合同审批通过副作用处理器（对齐 Issue 07）
 *
 * 监听 sales_contract 审批通过，把销售合同主表 status 置为 ACTIVE（生效）。
 *
 * @author luna
 */
@Component
@Slf4j
public class SalesContractApprovalHandler implements ApprovalHandler {

    @Resource
    private SalesContractMapper salesContractMapper;

    @Override
    public boolean supports(String bizType) {
        return BIZ_TYPE_SALES_CONTRACT.equalsIgnoreCase(bizType);
    }

    @Override
    public void onApproved(Long bizId) {
        log.info("[SalesContractApprovalHandler] 销售合同审批通过, 执行生效动作 (status=ACTIVE): bizId={}", bizId);
        int rows = salesContractMapper.update(null, new LambdaUpdateWrapper<SalesContractDO>()
                .eq(SalesContractDO::getId, bizId)
                .set(SalesContractDO::getStatus, "ACTIVE"));
        if (rows == 0) {
            log.warn("[SalesContractApprovalHandler] 未找到销售合同或更新行数为0: bizId={}", bizId);
        }
    }

}
