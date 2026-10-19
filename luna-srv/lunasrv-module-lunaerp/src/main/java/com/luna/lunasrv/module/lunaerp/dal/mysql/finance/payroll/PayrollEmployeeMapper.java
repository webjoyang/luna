package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollEmployeeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 财务管理 - 工资单员工汇总行 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface PayrollEmployeeMapper extends BaseMapperX<PayrollEmployeeDO> {

    /**
     * 恢复员工汇总行（置 deleted=0）
     *
     * @param payrollId 工资单ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE lunaerp_finance_payroll_employee SET deleted = 0, updater = #{updater}, update_time = #{updateTime} "
            + "WHERE payroll_id = #{payrollId} AND tenant_id = #{tenantId} AND deleted = 1")
    int restorePayrollEmployees(@Param("payrollId") Long payrollId, @Param("tenantId") Long tenantId,
                                @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 物理删除员工汇总行
     *
     * @param payrollId 工资单ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    @Update("DELETE FROM lunaerp_finance_payroll_employee WHERE payroll_id = #{payrollId} AND tenant_id = #{tenantId}")
    int purgePayrollEmployees(@Param("payrollId") Long payrollId, @Param("tenantId") Long tenantId);

}
