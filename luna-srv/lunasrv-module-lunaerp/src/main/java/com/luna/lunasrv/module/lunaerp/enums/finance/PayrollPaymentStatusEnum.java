package com.luna.lunasrv.module.lunaerp.enums.finance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工资单派生发放状态（不落库，由发放批次动态派生）
 *
 * Σ 批次 original_amount vs net_amount：
 * 0: 未发放 (UNPAID)
 * 区间内: 部分发放 (PARTIAL)
 * >= net: 已发清 (PAID)
 *
 * @author luna
 */
@Getter
@AllArgsConstructor
public enum PayrollPaymentStatusEnum {

    UNPAID("UNPAID", "未发放"),
    PARTIAL("PARTIAL", "部分发放"),
    PAID("PAID", "已发清");

    private final String value;
    private final String name;

}
