package com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.color;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.color.GreigeOrderColorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 生产管理-胚布单颜色投胚分配子表 Mapper
 *
 * 颜色行作为胚布单聚合的一部分（ADR-0003），无独立 Service/Controller，
 * 仅由 GreigeOrderServiceImpl 在胚布单保存/详情事务与方法内调用。
 *
 * @author luna
 */
@Mapper
public interface GreigeOrderColorMapper extends BaseMapperX<GreigeOrderColorDO> {

    /**
     * 按胚布单ID查询全部颜色行（按 line_no 升序）。
     *
     * @param orderId 胚布单ID
     * @return 颜色行列表
     */
    default List<GreigeOrderColorDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<GreigeOrderColorDO>()
                .eq(GreigeOrderColorDO::getOrderId, orderId)
                .orderByAsc(GreigeOrderColorDO::getLineNo));
    }

    /**
     * 按胚布单ID逻辑删除全部颜色行（MyBatis-Plus 逻辑删除自动追加 deleted 条件并执行 UPDATE）。
     *
     * @param orderId 胚布单ID
     * @return 受影响行数
     */
    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<GreigeOrderColorDO>()
                .eq(GreigeOrderColorDO::getOrderId, orderId));
    }

}
