package com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 生产管理-染色单颜色明细子表 Mapper
 *
 * @author luna
 */
@Mapper
public interface DyeingColorMapper extends BaseMapperX<DyeingColorDO> {

    default List<DyeingColorDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<DyeingColorDO>()
                .eq(DyeingColorDO::getDyeingOrderId, orderId)
                .orderByAsc(DyeingColorDO::getLineNo));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<DyeingColorDO>()
                .eq(DyeingColorDO::getDyeingOrderId, orderId));
    }

}
