package com.luna.lunasrv.module.lunaerp.approval.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED;

/**
 * 销售退货审批业务处理器
 *
 * 供 02 号审批引擎发起审批实例时校验单据、提取单号快照与同步 approval_status。
 *
 * @author luna
 */
@Component
public class SalesReturnApprovalBizHandler implements ApprovalBizHandler {

    @Resource
    private SalesReturnMapper salesReturnMapper;

    @Override
    public String getBizType() {
        return ApprovalHandler.BIZ_TYPE_SALES_RETURN;
    }

    @Override
    public String getBizTypeName() {
        return "销售退货";
    }

    @Override
    public String getBizNo(Long bizId) {
        SalesReturnDO salesReturn = salesReturnMapper.selectById(bizId);
        return salesReturn != null ? salesReturn.getReturnNo() : null;
    }

    @Override
    public void validateCanSubmit(Long bizId) {
        SalesReturnDO salesReturn = salesReturnMapper.selectById(bizId);
        if (salesReturn == null) {
            throw exception(SALES_RETURN_NOT_EXISTS);
        }
        if (!SalesReturnStatusEnum.DRAFT.getValue().equalsIgnoreCase(salesReturn.getStatus())) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }
        if (!"PENDING".equalsIgnoreCase(salesReturn.getApprovalStatus())
                && !"REJECTED".equalsIgnoreCase(salesReturn.getApprovalStatus())
                && !"APPROVING".equalsIgnoreCase(salesReturn.getApprovalStatus())) {
            throw exception(SALES_RETURN_STATUS_NOT_ALLOWED);
        }
    }

    @Override
    public void updateApprovalStatus(Long bizId, String approvalStatus) {
        String targetStatus = "DRAFT".equalsIgnoreCase(approvalStatus) ? "PENDING" : approvalStatus;
        salesReturnMapper.update(null, new LambdaUpdateWrapper<SalesReturnDO>()
                .eq(SalesReturnDO::getId, bizId)
                .set(SalesReturnDO::getApprovalStatus, targetStatus));
    }

}
