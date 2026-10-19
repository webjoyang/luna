package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务管理 - 工资次品扣款引用追溯子表 DO
 *
 * 表：lunaerp_finance_payroll_defect_ref
 *
 * @author luna
 */
@TableName("lunaerp_finance_payroll_defect_ref")
@KeySequence("lunaerp_finance_payroll_defect_ref_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollDefectRefDO extends TenantBaseDO {

    /**
     * 次品引用ID
     */
    private Long id;

    /**
     * 款项明细行ID（lunaerp_finance_payroll_line_item.id）
     */
    private Long lineItemId;

    /**
     * 责任工人用户ID
     */
    private Long userId;

    /**
     * 责任工人姓名快照
     */
    private String nickname;

    /**
     * 质检确认记录ID（lunaerp_production_inspection_confirmation.id）
     */
    private Long confirmationId;

    /**
     * 质检确认单号快照
     */
    private String confirmationNo;

    /**
     * 次品布卷号快照
     */
    private String rollNo;

    /**
     * 次品布卷等级快照（如 C/等外品）
     */
    private String grade;

    /**
     * 质检确认时间快照
     */
    private LocalDateTime confirmedAt;

    /**
     * 扣款单价快照（元/卷）
     */
    private BigDecimal deductPrice;

}
