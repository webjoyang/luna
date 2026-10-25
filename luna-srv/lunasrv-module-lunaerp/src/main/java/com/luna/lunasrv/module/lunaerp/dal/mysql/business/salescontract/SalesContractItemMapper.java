package com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 业务管理-销售合同明细子表 Mapper
 *
 * 明细作为合同聚合的一部分（ADR-0003），无独立 Service/Controller，
 * 仅由 SalesContractServiceImpl 在合同保存/删除事务内调用。
 *
 * @author luna
 */
@Mapper
public interface SalesContractItemMapper extends BaseMapperX<SalesContractItemDO> {

    /**
     * 级联恢复合同明细（手写 XML：显式 SET deleted=0 WHERE contract_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByContractId(@Param("contractId") Long contractId, @Param("tenantId") Long tenantId,
                            @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除合同明细（手写 XML：显式 DELETE WHERE contract_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByContractId(@Param("contractId") Long contractId, @Param("tenantId") Long tenantId);

    /**
     * 按合同ID查询全部明细（按 line_no 升序）。
     *
     * @param contractId 合同ID
     * @return 明细列表
     */
    default List<SalesContractItemDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<SalesContractItemDO>()
                .eq(SalesContractItemDO::getContractId, contractId)
                .orderByAsc(SalesContractItemDO::getLineNo));
    }

    /**
     * 按合同ID逻辑删除全部明细（MyBatis-Plus 逻辑删除自动追加 deleted 条件并执行 UPDATE）。
     *
     * @param contractId 合同ID
     * @return 受影响行数
     */
    default int deleteByContractId(Long contractId) {
        return delete(new LambdaQueryWrapperX<SalesContractItemDO>()
                .eq(SalesContractItemDO::getContractId, contractId));
    }

    /**
     * 按合同ID集合批量查询全部明细（生产进度 demandKg 聚合用，ADR-0034）。
     *
     * @param contractIds 合同ID集合
     * @return 明细列表（未加工，KG/M 过滤由 Service 完成）
     */
    default List<SalesContractItemDO> selectListByContractIds(Collection<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<SalesContractItemDO>()
                .in(SalesContractItemDO::getContractId, contractIds));
    }

    /**
     * 条件更新合同明细已下达数量（防并发与超下达）
     *
     * <p>仅当 released_qty + #{releaseQty} &lt;= quantity 且未软删时累加；影响行数 0 表示超下达或已被并发修改。</p>
     *
     * @param id 合同明细ID
     * @param releaseQty 本次下达数量
     * @param tenantId 租户ID
     * @return 影响行数（1 成功，0 拦截）
     */
    @Update("UPDATE lunaerp_business_sales_contract_item "
            + "SET released_qty = released_qty + #{releaseQty} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND released_qty + #{releaseQty} <= quantity")
    int updateReleasedQty(@Param("id") Long id,
                          @Param("releaseQty") BigDecimal releaseQty,
                          @Param("tenantId") Long tenantId);

}
