package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务管理 - 付款申请单 Mapper
 *
 * <p>遵循 docs/agents/coding-standards.md 第 4 节规约：保持 Mapper 纯粹性，不设冗余 default 包装方法。</p>
 *
 * @author luna
 */
@Mapper
public interface PaymentApplyMapper extends BaseMapperX<PaymentApplyDO> {

    /**
     * 按申请单号精确查询（含已删除行，用于按天取号防撞）
     *
     * @param applyNo 申请单号
     * @return 申请单（含已删除行）
     */
    @Select("SELECT id, apply_no, apply_type, supplier_id, supplier_name, recon_id, recon_no, amount, "
            + "purpose, bank_name, bank_account, approval_status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment_apply WHERE apply_no = #{applyNo} LIMIT 1")
    PaymentApplyDO selectByApplyNoIncludingDeleted(@Param("applyNo") String applyNo);

    /**
     * 按申请单号前缀查询列表（含已删除行，用于按天取号扫描）
     *
     * @param prefix 单号前缀（如 FK20261015）
     * @return 申请单列表（含已删除行）
     */
    @Select("SELECT id, apply_no, apply_type, supplier_id, supplier_name, recon_id, recon_no, amount, "
            + "purpose, bank_name, bank_account, approval_status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment_apply WHERE apply_no LIKE CONCAT(#{prefix}, '%')")
    List<PaymentApplyDO> selectListByApplyNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 回收站分页查询已软删草稿付款申请单
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿付款申请单
     *
     * @param id 申请单ID
     * @param tenantId 租户ID
     * @return 申请单
     */
    PaymentApplyDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿付款申请单（并发条件更新）
     *
     * @param id 申请单ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restorePaymentApply(@Param("id") Long id, @Param("tenantId") Long tenantId,
                            @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 彻底物理删除草稿付款申请单
     *
     * @param id 申请单ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    int purgePaymentApply(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查付款申请单是否存在活跃业务引用（如已被收付款明细核销）
     *
     * @param id 申请单ID
     * @param tenantId 租户ID
     * @return 活跃引用数量
     */
    Long selectActiveReferenceCount(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 按供应商统计特定类型付款申请被核销分配的总额（allocate_amount 累计）
     *
     * @param supplierId 供应商ID
     * @param applyType 申请类型（如 PREPAY）
     * @param tenantId 租户编号
     * @return 核销分配总额
     */
    BigDecimal selectSumAllocatedAmountBySupplierAndType(@Param("supplierId") Long supplierId,
                                                        @Param("applyType") String applyType,
                                                        @Param("tenantId") Long tenantId);

}
