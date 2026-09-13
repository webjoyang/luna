package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.greigetype;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype.GreigeTypeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 胚布品种 Mapper
 *
 * 继承 TenantBaseDO 后，租户条件由多租户插件自动拼接，无需手写 tenant_id；
 * 逻辑删除条件 deleted = 0 同样由 MyBatis-Plus 自动追加，
 * 与表上 (tenant_id, code) / (tenant_id, match_key) WHERE deleted = 0 的唯一索引口径一致。
 *
 * @author luna
 */
@Mapper
public interface GreigeTypeMapper extends BaseMapperX<GreigeTypeDO> {

    default PageResult<GreigeTypeDO> selectPage(GreigeTypePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<GreigeTypeDO>()
                .likeIfPresent(GreigeTypeDO::getCode, reqVO.getCode())
                .likeIfPresent(GreigeTypeDO::getName, reqVO.getName())
                .likeIfPresent(GreigeTypeDO::getFabricType, reqVO.getFabricType())
                .likeIfPresent(GreigeTypeDO::getComposition, reqVO.getComposition())
                .likeIfPresent(GreigeTypeDO::getYarnSpec, reqVO.getYarnSpec())
                .eqIfPresent(GreigeTypeDO::getStatus, reqVO.getStatus())
                .orderByDesc(GreigeTypeDO::getCreateTime));
    }

    default GreigeTypeDO selectByCode(String code) {
        return selectOne(GreigeTypeDO::getCode, code);
    }

    default GreigeTypeDO selectByMatchKey(String matchKey) {
        return selectOne(GreigeTypeDO::getMatchKey, matchKey);
    }

    /**
     * 精简选择器列表：仅取启用品种，排序口径与旧服务列表一致
     * （布种、纱支、坯克重顺序排列）。
     */
    default List<GreigeTypeDO> selectListByStatus(String status) {
        return selectList(new LambdaQueryWrapperX<GreigeTypeDO>()
                .eq(GreigeTypeDO::getStatus, status)
                .orderByAsc(GreigeTypeDO::getFabricType)
                .orderByAsc(GreigeTypeDO::getYarnSpec)
                .orderByAsc(GreigeTypeDO::getWeightGsm));
    }

    /**
     * 取当前租户全部品种编号，用于在内存计算 GT 流水号。
     * 只 select code 一列；品种表量级几百到几千，开销可忽略。
     */
    default List<GreigeTypeDO> selectListCode() {
        return selectList(new LambdaQueryWrapper<GreigeTypeDO>()
                .select(GreigeTypeDO::getCode));
    }

}
