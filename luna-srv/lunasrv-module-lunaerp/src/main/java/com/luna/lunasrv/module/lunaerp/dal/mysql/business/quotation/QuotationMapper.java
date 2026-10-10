package com.luna.lunasrv.module.lunaerp.dal.mysql.business.quotation;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation.QuotationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务管理 - 报价单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface QuotationMapper extends BaseMapperX<QuotationDO> {

    /**
     * 按单号+版本号精确查询，【包含已逻辑删除的行】（ADR-0004）。
     */
    @DataPermission(enable = false)
    @Select("SELECT * FROM lunaerp_business_quotation WHERE quotation_no = #{quotationNo} AND version_no = #{versionNo} LIMIT 1")
    QuotationDO selectByNoAndVersionIncludingDeleted(@Param("quotationNo") String quotationNo, @Param("versionNo") Integer versionNo);

    /**
     * 按单号前缀查询报价单，【包含已逻辑删除的行】（ADR-0004）。
     */
    @DataPermission(enable = false)
    @Select("SELECT * FROM lunaerp_business_quotation WHERE quotation_no LIKE CONCAT(#{prefix}, '%')")
    List<QuotationDO> selectListByQuotationNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 查询同单号的草稿记录（同号至多一草稿）
     */
    @Select("SELECT * FROM lunaerp_business_quotation WHERE quotation_no = #{quotationNo} AND status = 'DRAFT' AND deleted = 0 LIMIT 1")
    QuotationDO selectDraftByQuotationNo(@Param("quotationNo") String quotationNo);

    /**
     * 查询指定单号的最大版本号
     */
    @Select("SELECT COALESCE(MAX(version_no), 0) FROM lunaerp_business_quotation WHERE quotation_no = #{quotationNo} AND deleted = 0")
    Integer selectMaxVersionNoByQuotationNo(@Param("quotationNo") String quotationNo);

    /**
     * 查询指定单号的所有版本记录（按版本号降序）
     */
    @Select("SELECT * FROM lunaerp_business_quotation WHERE quotation_no = #{quotationNo} AND deleted = 0 ORDER BY version_no DESC")
    List<QuotationDO> selectListByQuotationNo(@Param("quotationNo") String quotationNo);

    /**
     * 确认报价（仅 DRAFT -> CONFIRMED 条件更新守卫）
     */
    @Update("UPDATE lunaerp_business_quotation SET status = 'CONFIRMED', updater = #{updater}, update_time = #{now} "
            + "WHERE id = #{id} AND status = 'DRAFT' AND deleted = 0")
    int confirmQuotation(@Param("id") Long id,
                         @Param("updater") String updater,
                         @Param("now") LocalDateTime now);

    /**
     * 取消报价（仅 CONFIRMED -> CANCELLED 条件更新守卫）
     */
    @Update("UPDATE lunaerp_business_quotation SET status = 'CANCELLED', updater = #{updater}, update_time = #{now} "
            + "WHERE id = #{id} AND status = 'CONFIRMED' AND deleted = 0")
    int cancelQuotation(@Param("id") Long id,
                        @Param("updater") String updater,
                        @Param("now") LocalDateTime now);

    /**
     * 修订使旧版本失效（仅 CONFIRMED -> SUPERSEDED 条件更新守卫）
     */
    @Update("UPDATE lunaerp_business_quotation SET status = 'SUPERSEDED', updater = #{updater}, update_time = #{now} "
            + "WHERE id = #{id} AND status = 'CONFIRMED' AND deleted = 0")
    int supersedeQuotation(@Param("id") Long id,
                           @Param("updater") String updater,
                           @Param("now") LocalDateTime now);

    /**
     * 回收站分页查询已软删草稿报价单
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿报价单
     */
    QuotationDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿报价单
     */
    int restoreQuotation(@Param("id") Long id, @Param("tenantId") Long tenantId,
                         @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 物理删除草稿报价单
     */
    int purgeQuotation(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查报价单是否存在活跃业务引用（如转合同等）
     */
    Long selectActiveReferenceCount(@Param("quotationId") Long quotationId, @Param("tenantId") Long tenantId);

}
