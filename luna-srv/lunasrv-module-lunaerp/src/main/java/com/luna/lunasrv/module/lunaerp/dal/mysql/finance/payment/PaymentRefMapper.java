package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentRefDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务管理 - 收付款核销明细 Mapper（共享表）
 *
 * <p>遵循 docs/agents/coding-standards.md 第 4 节规约：保持 Mapper 纯粹性，不设冗余 default 包装方法。</p>
 *
 * @author luna
 */
@Mapper
public interface PaymentRefMapper extends BaseMapperX<PaymentRefDO> {
}
