package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.SupplierReconPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 财务管理 - 供应商对账单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SupplierReconMapper extends BaseMapperX<SupplierReconDO> {

    /**
     * 按单号精确查询对账单（含已删除行，用于防撞与查重）
     */
    @Select("SELECT id, recon_no, supplier_id, supplier_name, period_start, period_end, "
            + "status, total_amount, confirmed_by, confirmed_at, cancelled_by, cancelled_at, "
            + "remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_supplier_recon WHERE recon_no = #{reconNo} LIMIT 1")
    SupplierReconDO selectByReconNoIncludingDeleted(@Param("reconNo") String reconNo);

    /**
     * 按单号前缀查询对账单列表（含已删除行，用于按天取号扫描）
     */
    @Select("SELECT id, recon_no, supplier_id, supplier_name, period_start, period_end, "
            + "status, total_amount, confirmed_by, confirmed_at, cancelled_by, cancelled_at, "
            + "remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_supplier_recon WHERE recon_no LIKE CONCAT(#{prefix}, '%')")
    List<SupplierReconDO> selectListByReconNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 供应商对账单分页查询
     */
    default PageResult<SupplierReconDO> selectPage(SupplierReconPageReqVO reqVO) {
        LambdaQueryWrapperX<SupplierReconDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(SupplierReconDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(SupplierReconDO::getStatus, reqVO.getStatus())
                .geIfPresent(SupplierReconDO::getPeriodStart, reqVO.getPeriodStart())
                .leIfPresent(SupplierReconDO::getPeriodEnd, reqVO.getPeriodEnd());
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SupplierReconDO::getReconNo, keyword)
                    .or().like(SupplierReconDO::getSupplierName, keyword));
        }
        wrapper.orderByDesc(SupplierReconDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 按供应商ID与状态集合查询对账单列表
     */
    default List<SupplierReconDO> selectListBySupplierIdAndStatuses(Long supplierId, Collection<String> statuses) {
        if (CollUtil.isEmpty(statuses)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SupplierReconDO>()
                .eqIfPresent(SupplierReconDO::getSupplierId, supplierId)
                .in(SupplierReconDO::getStatus, statuses));
    }

    /**
     * 按状态集合查询对账单列表
     */
    default List<SupplierReconDO> selectListByStatuses(Collection<String> statuses) {
        if (CollUtil.isEmpty(statuses)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SupplierReconDO>()
                .in(SupplierReconDO::getStatus, statuses));
    }

    /**
     * 回收站分页查询已软删草稿对账单
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿对账单
     */
    SupplierReconDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿对账单
     */
    int restoreSupplierRecon(@Param("id") Long id, @Param("tenantId") Long tenantId,
                             @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除草稿对账单
     */
    int purgeSupplierRecon(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查对账单是否存在活跃业务引用
     */
    Long selectActiveReferenceCount(@Param("reconId") Long reconId, @Param("tenantId") Long tenantId);

    /**
     * 条件更新对账单状态为已确认（并发防重）
     */
    int updateToConfirmed(@Param("id") Long id, @Param("tenantId") Long tenantId,
                          @Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 条件更新对账单状态为已作废（并发防重）
     */
    int updateToCancelled(@Param("id") Long id, @Param("tenantId") Long tenantId,
                          @Param("userId") Long userId, @Param("now") LocalDateTime now);

}
