package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payroll;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payroll.PayrollDefectRefDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 财务管理 - 工资次品扣款引用追溯子表 Mapper
 *
 * <p>遵循 coding-standards.md §4：保持 Mapper 纯粹性，不设 default 方法。</p>
 *
 * @author luna
 */
@Mapper
public interface PayrollDefectRefMapper extends BaseMapperX<PayrollDefectRefDO> {

    /**
     * 恢复次品引用明细（置 deleted=0）
     *
     * @param lineItemId 款项行ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Update("UPDATE lunaerp_finance_payroll_defect_ref SET deleted = 0, updater = #{updater}, update_time = #{updateTime} "
            + "WHERE line_item_id = #{lineItemId} AND tenant_id = #{tenantId} AND deleted = 1")
    int restoreDefectRefs(@Param("lineItemId") Long lineItemId, @Param("tenantId") Long tenantId,
                          @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 物理删除次品引用明细
     *
     * @param lineItemId 款项行ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    @Update("DELETE FROM lunaerp_finance_payroll_defect_ref WHERE line_item_id = #{lineItemId} AND tenant_id = #{tenantId}")
    int purgeDefectRefs(@Param("lineItemId") Long lineItemId, @Param("tenantId") Long tenantId);

}
