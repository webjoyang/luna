package com.luna.lunasrv.module.lunaerp.dal.mysql.production.loom;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.loom.vo.LoomPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.loom.LoomDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产管理 - 织机 Mapper
 *
 * 列表 search 智能词命中 loom_no / model / remark 之一，
 * PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 OR ILIKE 语义。
 * 逻辑删除（deleted=0）与租户条件由框架自动追加。
 *
 * 编号取号与查重扫描【包含已逻辑删除的行】：手写 SQL 绕过 MyBatis-Plus
 * 逻辑删除自动过滤，tenant_id 由租户拦截器自动追加。
 *
 * @author luna
 */
@Mapper
public interface LoomMapper extends BaseMapperX<LoomDO> {

    default PageResult<LoomDO> selectPage(LoomPageReqVO reqVO) {
        LambdaQueryWrapperX<LoomDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(LoomDO::getStatus, reqVO.getStatus());
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (StringUtils.hasText(search)) {
            String like = "%" + search + "%";
            wrapper.and(w -> w
                    .apply("LOWER(loom_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(model) LIKE LOWER({0})", like)
                    .or().apply("LOWER(remark) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(LoomDO::getCreateTime).orderByDesc(LoomDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 按编号前缀查询织机，【包含已逻辑删除的行】（用于按年取号扫描）。
     */
    @Select("SELECT id, loom_no, model, status, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_loom WHERE loom_no LIKE CONCAT(#{prefix}, '%')")
    List<LoomDO> selectListByLoomNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按编号精确查询织机，【包含已逻辑删除的行】（用于手填编号查重）。
     */
    @Select("SELECT id, loom_no, model, status, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_loom WHERE loom_no = #{loomNo} LIMIT 1")
    LoomDO selectByLoomNoIncludingDeleted(@Param("loomNo") String loomNo);

}
