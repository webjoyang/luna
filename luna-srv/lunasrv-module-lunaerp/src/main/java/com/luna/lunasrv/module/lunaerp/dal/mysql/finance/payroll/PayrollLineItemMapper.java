package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollLineItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 财务管理 - 工资单款项明细行 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface PayrollLineItemMapper extends BaseMapperX<PayrollLineItemDO> {

    /**
     * 恢复款项明细行（置 deleted=0）
     *
     * @param payrollId 工资单ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE lunaerp_finance_payroll_line_item SET deleted = 0, updater = #{updater}, update_time = #{updateTime} "
            + "WHERE payroll_id = #{payrollId} AND tenant_id = #{tenantId} AND deleted = 1")
    int restorePayrollLineItems(@Param("payrollId") Long payrollId, @Param("tenantId") Long tenantId,
                                @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 物理删除款项明细行
     *
     * @param payrollId 工资单ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    @Update("DELETE FROM lunaerp_finance_payroll_line_item WHERE payroll_id = #{payrollId} AND tenant_id = #{tenantId}")
    int purgePayrollLineItems(@Param("payrollId") Long payrollId, @Param("tenantId") Long tenantId);

}
