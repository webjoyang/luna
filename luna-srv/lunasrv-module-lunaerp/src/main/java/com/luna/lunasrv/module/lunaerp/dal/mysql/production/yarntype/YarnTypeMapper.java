package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarntype.vo.YarnTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

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

    /**
     * 分页查询已软删纱线材质（手写 XML：显式 WHERE deleted = 1 AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的纱线材质记录
     */
    YarnTypeDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复纱线材质（并发条件更新带 deleted=1 AND tenant_id=?）
     */
    int restoreYarnType(@Param("id") Long id,
                        @Param("tenantId") Long tenantId,
                        @Param("updater") String updater,
                        @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除纱线材质（物理删除带 deleted=1 AND tenant_id=?）
     */
    int purgeYarnType(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查纱线材质是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("yarnTypeId") Long yarnTypeId, @Param("tenantId") Long tenantId);

}
