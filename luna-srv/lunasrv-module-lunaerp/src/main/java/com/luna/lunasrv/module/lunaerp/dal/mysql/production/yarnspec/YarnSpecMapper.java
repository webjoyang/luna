package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.yarnspec.vo.YarnSpecPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 生产管理-纱线规格 Mapper
 *
 * 唯一索引 (tenant_id, yarn_type_id, standard_name)。
 * 查重扫描覆盖已删除行，手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
 *
 * @author luna
 */
@Mapper
public interface YarnSpecMapper extends BaseMapperX<YarnSpecDO> {

    /**
     * 分页查询纱线规格
     *
     * @param reqVO 分页与过滤参数
     * @return 规格分页结果
     */
    default PageResult<YarnSpecDO> selectPage(YarnSpecPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<YarnSpecDO>()
                .eqIfPresent(YarnSpecDO::getYarnTypeId, reqVO.getYarnTypeId())
                .likeIfPresent(YarnSpecDO::getSpecName, reqVO.getSpecName())
                .likeIfPresent(YarnSpecDO::getStandardName, reqVO.getStandardName())
                .eqIfPresent(YarnSpecDO::getStatus, reqVO.getStatus())
                .orderByDesc(YarnSpecDO::getId));
    }

    /**
     * 按材质ID与标准名称查询纱线规格，【包含已逻辑删除的行】。
     *
     * 唯一索引 (tenant_id, yarn_type_id, standard_name) 包含软删行；
     * 手写 SQL 绕过逻辑删除自动过滤（deleted=0 不会追加）；
     * tenant_id 由租户拦截器自动追加，租户隔离不受影响。
     *
     * @param yarnTypeId 所属材质ID
     * @param standardName 标准名称
     * @return 规格DO（含已删除行），不存在返回 null
     */
    @Select("SELECT id, yarn_type_id, spec_name, standard_name, spin_method, luster, ref_price, remark, status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_spec WHERE yarn_type_id = #{yarnTypeId} AND standard_name = #{standardName} LIMIT 1")
    YarnSpecDO selectByYarnTypeIdAndStandardNameIncludingDeleted(@Param("yarnTypeId") Long yarnTypeId,
                                                               @Param("standardName") String standardName);

    /**
     * 分页查询已软删纱线规格（手写 XML：显式 WHERE deleted = 1 AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的纱线规格记录
     */
    YarnSpecDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复纱线规格（并发条件更新带 deleted=1 AND tenant_id=?）
     */
    int restoreYarnSpec(@Param("id") Long id,
                        @Param("tenantId") Long tenantId,
                        @Param("updater") String updater,
                        @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除纱线规格（物理删除带 deleted=1 AND tenant_id=?）
     */
    int purgeYarnSpec(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查纱线规格是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("yarnSpecId") Long yarnSpecId, @Param("tenantId") Long tenantId);

}
