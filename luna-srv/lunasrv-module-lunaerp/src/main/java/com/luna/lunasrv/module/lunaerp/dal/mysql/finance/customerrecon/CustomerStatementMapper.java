package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 客户对账单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface CustomerStatementMapper extends BaseMapperX<CustomerStatementDO> {

    String COLUMNS = "id, statement_no, customer_id, customer_name, currency, "
            + "period_start, period_end, opening_balance, period_debit, period_credit, closing_balance, "
            + "status, confirm_type, confirm_by_name, confirm_time, confirm_ip, confirm_method, "
            + "reject_reason, reply_remark, remark, creator, create_time, updater, update_time, deleted, tenant_id";

    /**
     * 按对账单号精确查询（含已软删除记录，用于取号防撞）
     *
     * @param statementNo 对账单号
     * @return 对账单（含已删除记录）
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_statement WHERE statement_no = #{statementNo} LIMIT 1")
    CustomerStatementDO selectByStatementNoIncludingDeleted(@Param("statementNo") String statementNo);

    /**
     * 按对账单号前缀查询列表（含已软删除记录，用于发号扫描）
     *
     * @param prefix 单号前缀
     * @return 对账单列表（含已删除记录）
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_statement WHERE statement_no LIKE CONCAT(#{prefix}, '%')")
    List<CustomerStatementDO> selectListByStatementNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 回收站分页查询已软删草稿客户对账单（手写 XML：WHERE deleted = 1 AND status = 'DRAFT' AND tenant_id = #{tenantId}）
     *
     * @param page 分页参数
     * @param tenantId 租户编号
     * @return 分页结果
     */
    com.baomidou.mybatisplus.core.metadata.IPage<com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO> selectRecyclePage(
            com.baomidou.mybatisplus.core.metadata.IPage<com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO> page,
            @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删草稿客户对账单记录
     *
     * @param id 主键
     * @param tenantId 租户编号
     * @return 客户对账单 DO
     */
    CustomerStatementDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿客户对账单（并发条件更新）
     *
     * @param id 主键
     * @param tenantId 租户编号
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restoreCustomerStatement(@Param("id") Long id, @Param("tenantId") Long tenantId,
                                 @Param("updater") String updater, @Param("now") java.time.LocalDateTime now);

    /**
     * 彻底物理删除草稿客户对账单
     *
     * @param id 主键
     * @param tenantId 租户编号
     * @return 影响行数
     */
    int purgeCustomerStatement(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查客户对账单是否存在活跃业务引用
     *
     * @param id 主键
     * @param tenantId 租户编号
     * @return 活跃引用数
     */
    Long selectActiveReferenceCount(@Param("id") Long id, @Param("tenantId") Long tenantId);

}

