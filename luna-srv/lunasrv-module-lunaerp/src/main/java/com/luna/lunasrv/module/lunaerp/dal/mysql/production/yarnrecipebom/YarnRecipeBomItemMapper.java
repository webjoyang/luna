package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 生产管理-纱线配比BOM明细表 Mapper
 *
 * 配比明细作为配比BOM聚合的一部分（ADR-0003模式），无独立 Service/Controller，
 * 仅由 YarnRecipeBomServiceImpl 在配比BOM保存/详情事务与方法内调用。
 *
 * @author luna
 */
@Mapper
public interface YarnRecipeBomItemMapper extends BaseMapperX<YarnRecipeBomItemDO> {

    /**
     * 按配比BOM ID查询全部明细行（按 id 升序）。
     *
     * @param recipeBomId 配比BOM ID
     * @return 明细行列表
     */
    default List<YarnRecipeBomItemDO> selectListByRecipeBomId(Long recipeBomId) {
        return selectList(new LambdaQueryWrapperX<YarnRecipeBomItemDO>()
                .eq(YarnRecipeBomItemDO::getRecipeBomId, recipeBomId)
                .orderByAsc(YarnRecipeBomItemDO::getId));
    }

    /**
     * 按配比BOM ID列表查询明细行。
     *
     * @param recipeBomIds 配比BOM ID列表
     * @return 明细行列表
     */
    default List<YarnRecipeBomItemDO> selectListByRecipeBomIds(java.util.Collection<Long> recipeBomIds) {
        return selectList(new LambdaQueryWrapperX<YarnRecipeBomItemDO>()
                .in(YarnRecipeBomItemDO::getRecipeBomId, recipeBomIds));
    }

    /**
     * 按配比BOM ID逻辑删除全部明细行（MyBatis-Plus 逻辑删除自动追加 deleted 条件并执行 UPDATE）。
     *
     * @param recipeBomId 配比BOM ID
     * @return 受影响行数
     */
    default int deleteByRecipeBomId(Long recipeBomId) {
        return delete(new LambdaQueryWrapperX<YarnRecipeBomItemDO>()
                .eq(YarnRecipeBomItemDO::getRecipeBomId, recipeBomId));
    }

}
