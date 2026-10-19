package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 财务管理 - 员工费用报销明细行 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface ExpenseItemMapper extends BaseMapperX<ExpenseItemDO> {

    /**
     * 恢复报销明细行（置 deleted=0）
     *
     * @param expenseId 报销单ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE lunaerp_finance_expense_item SET deleted = 0, updater = #{updater}, update_time = #{updateTime} "
            + "WHERE expense_id = #{expenseId} AND tenant_id = #{tenantId} AND deleted = 1")
    int restoreExpenseItems(@Param("expenseId") Long expenseId, @Param("tenantId") Long tenantId,
                            @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 物理删除报销明细行
     *
     * @param expenseId 报销单ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    @Update("DELETE FROM lunaerp_finance_expense_item WHERE expense_id = #{expenseId} AND tenant_id = #{tenantId}")
    int purgeExpenseItems(@Param("expenseId") Long expenseId, @Param("tenantId") Long tenantId);

}
