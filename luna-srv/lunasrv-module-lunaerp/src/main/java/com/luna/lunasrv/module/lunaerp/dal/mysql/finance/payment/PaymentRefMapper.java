package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 财务管理 - 收付款核销明细 Mapper（共享表）
 *
 * @author luna
 */
@Mapper
public interface PaymentRefMapper extends BaseMapperX<PaymentRefDO> {

    /**
     * 按收付款单ID查询核销明细
     *
     * @param paymentId 收付款单ID
     * @return 核销明细列表
     */
    default List<PaymentRefDO> selectListByPaymentId(Long paymentId) {
        if (paymentId == null) {
            return Collections.emptyList();
        }
        return selectList(PaymentRefDO::getPaymentId, paymentId);
    }

    /**
     * 按关联业务单据ID查询核销明细
     *
     * @param refId 业务单据ID（如付款申请ID）
     * @return 核销明细列表
     */
    default List<PaymentRefDO> selectListByRefId(Long refId) {
        if (refId == null) {
            return Collections.emptyList();
        }
        return selectList(PaymentRefDO::getRefId, refId);
    }

    /**
     * 按关联业务单据ID集合批量查询核销明细
     *
     * @param refIds 业务单据ID集合
     * @return 核销明细列表
     */
    default List<PaymentRefDO> selectListByRefIds(Collection<Long> refIds) {
        if (CollUtil.isEmpty(refIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<PaymentRefDO>()
                .in(PaymentRefDO::getRefId, refIds));
    }

}
