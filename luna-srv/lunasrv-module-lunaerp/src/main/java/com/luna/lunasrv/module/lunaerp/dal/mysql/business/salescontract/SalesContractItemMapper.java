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
     * 条件累加合同行已下达生产数量（超下达拦截：released_qty + delta <= quantity）。
     *
     * @param id 合同行ID
     * @param delta 增量数量
     * @return 影响行数（0 表示超下达或行不存在/已删除）
     */
    @Update("UPDATE lunaerp_business_sales_contract_item SET released_qty = released_qty + #{delta} WHERE id = #{id} AND released_qty + #{delta} <= quantity AND deleted = 0")
    int updateReleasedQtyIncrement(@Param("id") Long id, @Param("delta") BigDecimal delta);

}
