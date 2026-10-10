package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.APPROVAL_INSTANCE_STATUS_ERROR;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SALES_CONTRACT_NOT_EXISTS;

/**
 * 销售合同审批业务处理器
 *
 * @author luna
 */
@Component
public class SalesContractApprovalBizHandler implements ApprovalBizHandler {

    public static final String BIZ_TYPE = "sales_contract";

    @Resource
    private SalesContractMapper salesContractMapper;

    @Override
    public String getBizType() {
        return BIZ_TYPE;
    }

    @Override
    public String getBizTypeName() {
        return "销售合同";
    }

    @Override
    public String getBizNo(Long bizId) {
        SalesContractDO contract = salesContractMapper.selectById(bizId);
        return contract != null ? contract.getContractNo() : null;
    }

    @Override
    public void validateCanSubmit(Long bizId) {
        SalesContractDO contract = salesContractMapper.selectById(bizId);
        if (contract == null) {
            throw exception(SALES_CONTRACT_NOT_EXISTS);
        }
        if ("APPROVING".equalsIgnoreCase(contract.getApprovalStatus())) {
            throw exception(APPROVAL_INSTANCE_STATUS_ERROR, "销售合同审批中，不可重复提交");
        }
    }

    @Override
    public void updateApprovalStatus(Long bizId, String approvalStatus) {
        salesContractMapper.update(null, new LambdaUpdateWrapper<SalesContractDO>()
                .eq(SalesContractDO::getId, bizId)
                .set(SalesContractDO::getApprovalStatus, approvalStatus));
    }
}
