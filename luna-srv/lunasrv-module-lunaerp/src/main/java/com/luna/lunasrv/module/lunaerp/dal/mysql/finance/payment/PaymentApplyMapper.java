package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.payment;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.payment.vo.PaymentApplyPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment.PaymentApplyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 财务管理 - 付款申请单 Mapper
 *
 * @author luna
 */
@Mapper
public interface PaymentApplyMapper extends BaseMapperX<PaymentApplyDO> {

    /**
     * 按单号精确查询付款申请（含已删除行）
     */
    @Select("SELECT id, apply_no, apply_type, supplier_id, supplier_name, recon_id, recon_no, "
            + "amount, purpose, bank_name, bank_account, approval_status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment_apply WHERE apply_no = #{applyNo} LIMIT 1")
    PaymentApplyDO selectByApplyNoIncludingDeleted(@Param("applyNo") String applyNo);

    /**
     * 按单号前缀查询付款申请列表（含已删除行，用于按天取号扫描）
     */
    @Select("SELECT id, apply_no, apply_type, supplier_id, supplier_name, recon_id, recon_no, "
            + "amount, purpose, bank_name, bank_account, approval_status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_payment_apply WHERE apply_no LIKE CONCAT(#{prefix}, '%')")
    List<PaymentApplyDO> selectListByApplyNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按对账单ID查询付款申请列表
     */
    default List<PaymentApplyDO> selectListByReconId(Long reconId) {
        return selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .eq(PaymentApplyDO::getReconId, reconId));
    }

    /**
     * 按供应商ID与审批状态集合查询付款申请列表
     */
    default List<PaymentApplyDO> selectListBySupplierIdAndApprovalStatuses(Long supplierId, Collection<String> approvalStatuses) {
        if (CollUtil.isEmpty(approvalStatuses)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<PaymentApplyDO>()
                .eq(PaymentApplyDO::getSupplierId, supplierId)
                .in(PaymentApplyDO::getApprovalStatus, approvalStatuses));
    }

    /**
     * 付款申请分页查询
     */
    default PageResult<PaymentApplyDO> selectPage(PaymentApplyPageReqVO reqVO) {
        LambdaQueryWrapperX<PaymentApplyDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(PaymentApplyDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(PaymentApplyDO::getApplyType, reqVO.getApplyType())
                .eqIfPresent(PaymentApplyDO::getApprovalStatus, reqVO.getApprovalStatus());
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(PaymentApplyDO::getApplyNo, keyword)
                    .or().like(PaymentApplyDO::getSupplierName, keyword)
                    .or().like(PaymentApplyDO::getReconNo, keyword)
                    .or().like(PaymentApplyDO::getPurpose, keyword));
        }
        wrapper.orderByDesc(PaymentApplyDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 回收站分页查询已软删草稿/待提交付款申请
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的付款申请
     */
    PaymentApplyDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿/待提交付款申请
     */
    int restorePaymentApply(@Param("id") Long id, @Param("tenantId") Long tenantId,
                            @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除草稿/待提交付款申请
     */
    int purgePaymentApply(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查付款申请是否存在活跃业务引用（如 payment_ref）
     */
    Long selectActiveReferenceCount(@Param("applyId") Long applyId, @Param("tenantId") Long tenantId);

}
