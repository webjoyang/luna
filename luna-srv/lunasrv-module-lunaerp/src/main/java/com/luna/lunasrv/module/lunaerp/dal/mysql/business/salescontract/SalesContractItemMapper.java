package com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import org.apache.ibatis.annotations.Mapper;

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

}
