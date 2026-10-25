package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务管理 - 员工工资单主表 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface PayrollMapper extends BaseMapperX<PayrollDO> {

    /**
     * 按单号查询（含已删除行，用于按月取号防撞）
     *
     * @param payrollNo 单号
     * @return 工资单
     */
    @Select("SELECT * FROM lunaerp_finance_payroll WHERE payroll_no = #{payrollNo} LIMIT 1")
    PayrollDO selectByPayrollNoIncludingDeleted(@Param("payrollNo") String payrollNo);

    /**
     * 按单号前缀查询列表（含已删除行，用于按月取号扫描）
     *
     * @param prefix 单号前缀（如 GZ202610）
     * @return 工资单列表
     */
    @Select("SELECT * FROM lunaerp_finance_payroll WHERE payroll_no LIKE CONCAT(#{prefix}, '%')")
    List<PayrollDO> selectListByPayrollNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 查询已删除的工资单（回收站使用）
     *
     * @param id 工资单ID
     * @param tenantId 租户ID
     * @return 工资单
     */
    @Select("SELECT * FROM lunaerp_finance_payroll WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1 LIMIT 1")
    PayrollDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 回收站分页查询
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @return 回收站视图列表
     */
    @Select("SELECT id AS biz_id, 'PAYROLL' AS biz_type, payroll_no AS display_name, "
            + "update_time AS deleted_time, updater AS deleted_by "
            + "FROM lunaerp_finance_payroll WHERE tenant_id = #{tenantId} AND deleted = 1 "
            + "ORDER BY update_time DESC")
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 恢复工资单（置 deleted=0, approval_status='PENDING'）
     *
     * @param id 工资单ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE lunaerp_finance_payroll SET deleted = 0, approval_status = 'PENDING', updater = #{updater}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1")
    int restorePayroll(@Param("id") Long id, @Param("tenantId") Long tenantId,
                       @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 物理删除工资单主表记录
     *
     * @param id 工资单ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    @Update("DELETE FROM lunaerp_finance_payroll WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1")
    int purgePayroll(@Param("id") Long id, @Param("tenantId") Long tenantId);

}
