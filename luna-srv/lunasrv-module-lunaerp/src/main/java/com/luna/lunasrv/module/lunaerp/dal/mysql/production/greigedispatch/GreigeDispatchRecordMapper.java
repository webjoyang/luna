package com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigedispatch;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigedispatch.GreigeDispatchRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 生产管理-发染记录 Mapper
 *
 * @author luna
 */
@Mapper
public interface GreigeDispatchRecordMapper extends BaseMapperX<GreigeDispatchRecordDO> {

    default PageResult<GreigeDispatchRecordDO> selectPage(GreigeDispatchRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<GreigeDispatchRecordDO>()
                .eqIfPresent(GreigeDispatchRecordDO::getGreigeOrderId, reqVO.getGreigeOrderId())
                .eqIfPresent(GreigeDispatchRecordDO::getDyeingOrderId, reqVO.getDyeingOrderId())
                .orderByDesc(GreigeDispatchRecordDO::getDispatchDate)
                .orderByDesc(GreigeDispatchRecordDO::getId));
    }

    default List<GreigeDispatchRecordDO> selectListByGreigeOrderId(Long greigeOrderId) {
        if (greigeOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<GreigeDispatchRecordDO>()
                .eq(GreigeDispatchRecordDO::getGreigeOrderId, greigeOrderId)
                .orderByDesc(GreigeDispatchRecordDO::getDispatchDate)
                .orderByDesc(GreigeDispatchRecordDO::getId));
    }

    default List<GreigeDispatchRecordDO> selectListByDyeingOrderId(Long dyeingOrderId) {
        if (dyeingOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<GreigeDispatchRecordDO>()
                .eq(GreigeDispatchRecordDO::getDyeingOrderId, dyeingOrderId)
                .orderByDesc(GreigeDispatchRecordDO::getDispatchDate)
                .orderByDesc(GreigeDispatchRecordDO::getId));
    }

    default List<GreigeDispatchRecordDO> selectListByGreigeOrderIds(Collection<Long> greigeOrderIds) {
        if (greigeOrderIds == null || greigeOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<GreigeDispatchRecordDO>()
                .in(GreigeDispatchRecordDO::getGreigeOrderId, greigeOrderIds));
    }

}
