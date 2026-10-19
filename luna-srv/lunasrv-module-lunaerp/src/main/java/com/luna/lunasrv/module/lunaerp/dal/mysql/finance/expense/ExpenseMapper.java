package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务管理 - 员工费用报销主表 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface ExpenseMapper extends BaseMapperX<ExpenseDO> {

    /**
     * 按单号查询（含已删除行，用于按天取号防撞）
     *
     * @param expenseNo 单号
     * @return 报销单
     */
    @Select("SELECT * FROM lunaerp_finance_expense WHERE expense_no = #{expenseNo} LIMIT 1")
    ExpenseDO selectByExpenseNoIncludingDeleted(@Param("expenseNo") String expenseNo);

    /**
     * 按单号前缀查询列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀（如 BX20261019）
     * @return 报销单列表
     */
    @Select("SELECT * FROM lunaerp_finance_expense WHERE expense_no LIKE CONCAT(#{prefix}, '%')")
    List<ExpenseDO> selectListByExpenseNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 查询已删除的报销单（回收站使用）
     *
     * @param id 报销单ID
     * @param tenantId 租户ID
     * @return 报销单
     */
    @Select("SELECT * FROM lunaerp_finance_expense WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1 LIMIT 1")
    ExpenseDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 回收站分页查询
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @return 回收站视图列表
     */
    @Select("SELECT id AS biz_id, 'EXPENSE' AS biz_type, expense_no AS display_name, "
            + "update_time AS deleted_time, updater AS deleted_by "
            + "FROM lunaerp_finance_expense WHERE tenant_id = #{tenantId} AND deleted = 1 "
            + "ORDER BY update_time DESC")
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 恢复报销单（置 deleted=0, approval_status='PENDING'）
     *
     * @param id 报销单ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE lunaerp_finance_expense SET deleted = 0, approval_status = 'PENDING', updater = #{updater}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1")
    int restoreExpense(@Param("id") Long id, @Param("tenantId") Long tenantId,
                       @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 物理删除报销单主表记录
     *
     * @param id 报销单ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    @Update("DELETE FROM lunaerp_finance_expense WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1")
    int purgeExpense(@Param("id") Long id, @Param("tenantId") Long tenantId);

}
