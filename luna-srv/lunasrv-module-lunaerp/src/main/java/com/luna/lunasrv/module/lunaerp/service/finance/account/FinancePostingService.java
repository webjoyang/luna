package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.luna.lunasrv.module.lunaerp.service.finance.account.dto.PostingReq;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 财务统一过账服务接口（全域资金唯一写入口，ADR-0043）
 *
 * 任何业务模块（收款、付款、报销、工资、转账、其他收支）要动资金账户，
 * 必须统一经由此通道过账：
 * 校验账户存在/启用/币种一致 -> 条件更新 current_balance -> 同事务 INSERT 经营分录。
 *
 * @author luna
 */
public interface FinancePostingService {

    /**
     * 资金过账统一入口（条件更新 current_balance + INSERT 经营分录，同事务）
     *
     * @param req 过账请求
     * @return 经营分录ID
     */
    Long post(@Valid @NotNull(message = "过账请求不能为空") PostingReq req);

    /**
     * 往来分录只读追加接口（13号销售退货往来分录等预留签名，account_id 为空不碰余额，只 INSERT 分录）
     *
     * @param req 往来分录请求（accountId 必须为空）
     * @return 经营分录ID
     */
    Long appendArEntry(@Valid @NotNull(message = "往来分录请求不能为空") PostingReq req);

}
