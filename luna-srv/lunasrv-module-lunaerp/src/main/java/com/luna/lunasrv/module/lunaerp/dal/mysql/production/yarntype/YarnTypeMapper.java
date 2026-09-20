package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 生产管理-纱线材质 Mapper
 *
 * 唯一索引 (tenant_id, name) 含软删行，软删不释放材质名（ADR-0011）。
 * 查重扫描必须覆盖已删除行，手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
 *
 * @author luna
 */
@Mapper
public interface YarnTypeMapper extends BaseMapperX<YarnTypeDO> {

    /**
     * 分页查询纱线材质
     *
     * @param reqVO 分页与过滤参数
     * @return 材质分页结果
     */
    default PageResult<YarnTypeDO> selectPage(YarnTypePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<YarnTypeDO>()
                .likeIfPresent(YarnTypeDO::getName, reqVO.getName())
                .eqIfPresent(YarnTypeDO::getStatus, reqVO.getStatus())
                .orderByAsc(YarnTypeDO::getSort)
                .orderByAsc(YarnTypeDO::getId));
    }

    /**
     * 按名称查询纱线材质，【包含已逻辑删除的行】。
     *
     * 唯一索引 (tenant_id, name) 包含软删行，软删不释放材质名；
     * 手写 SQL 绕过逻辑删除自动过滤（deleted=0 不会追加）；
     * tenant_id 由租户拦截器自动追加，租户隔离不受影响。
     *
     * @param name 材质名称
     * @return 材质DO（含已删除行），不存在返回 null
     */
    @Select("SELECT id, name, sort, status, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_type WHERE name = #{name} LIMIT 1")
    YarnTypeDO selectByNameIncludingDeleted(@Param("name") String name);

}
