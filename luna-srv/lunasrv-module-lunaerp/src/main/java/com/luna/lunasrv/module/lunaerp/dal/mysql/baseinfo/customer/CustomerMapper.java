package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 基本信息-客户表 Mapper
 *
 * 列表 search 智能词命中 company_name / short_name / phone / unified_social_credit_code 之一，
 * PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 OR ILIKE 语义（占位符走预编译，无注入风险）。
 * 逻辑删除（deleted=0）与租户条件由框架自动追加。
 *
 * @author luna
 */
@Mapper
public interface CustomerMapper extends BaseMapperX<CustomerDO> {

    /**
     * 调岗同步归属部门（ADR-0046）
     *
     * <p>绕过数据权限规则，在当前租户下将该业务员名下所有有效客户的归属部门刷新为新部门。</p>
     *
     * @param salespersonUserId 业务员用户编号
     * @param deptId 新部门编号
     * @return 更新行数
     */
    @DataPermission(enable = false)
    @Update("UPDATE lunaerp_baseinfo_customer SET dept_id = #{deptId,jdbcType=BIGINT} "
            + "WHERE salesperson_user_id = #{salespersonUserId,jdbcType=BIGINT} AND deleted = 0")
    int updateDeptIdBySalespersonUserId(@Param("salespersonUserId") Long salespersonUserId,
                                        @Param("deptId") Long deptId);

    /**
     * 客户分页查询：search 智能词 OR 四字段 ILIKE + status 等值过滤。
     *
     * @param reqVO 分页与查询条件
     * @return 客户分页
     */
    default PageResult<CustomerDO> selectPage(CustomerPageReqVO reqVO) {
        LambdaQueryWrapperX<CustomerDO> wrapper = new LambdaQueryWrapperX<>();
        // status 等值过滤（为空则不过滤，便于"全部"场景）
        wrapper.eqIfPresent(CustomerDO::getStatus, reqVO.getStatus());
        // 归属业务员等值过滤
        wrapper.eqIfPresent(CustomerDO::getSalespersonUserId, reqVO.getSalespersonUserId());
        // search 智能词：命中 company_name / short_name / phone / unified_social_credit_code 之一
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (!search.isEmpty()) {
            String like = "%" + search + "%";
            wrapper.and(w -> w
                    .apply("LOWER(company_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(short_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(phone) LIKE LOWER({0})", like)
                    .or().apply("LOWER(unified_social_credit_code) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(CustomerDO::getCreateTime);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 按企业名称查询有效客户（当前租户且未删除）
     */
    default CustomerDO selectByCompanyName(String companyName) {
        return selectOne(new LambdaQueryWrapperX<CustomerDO>()
                .eq(CustomerDO::getCompanyName, companyName));
    }

    /**
     * 按企业名称列表批量查询有效客户（当前租户且未删除）
     */
    default List<CustomerDO> selectListByCompanyNames(java.util.Collection<String> companyNames) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(companyNames)) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CustomerDO>()
                .in(CustomerDO::getCompanyName, companyNames));
    }

    /**
     * 分页查询已软删客户（手写 XML：显式 WHERE deleted = 1 AND tenant_id = #{tenantId}）
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的客户记录（手写 XML：显式 WHERE id = #{id} AND deleted = 1 AND tenant_id = #{tenantId}）
     *
     * @param id 客户ID
     * @param tenantId 租户ID
     * @return 客户DO
     */
    CustomerDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复客户（手写 XML：显式 UPDATE ... SET deleted=0, status='INACTIVE' WHERE id = #{id} AND deleted = 1 AND tenant_id = #{tenantId}）
     *
     * @param id 客户ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param updateTime 更新时间
     * @return 影响行数（1 成功，0 失败/不存在/已被恢复）
     */
    int restoreCustomer(@Param("id") Long id,
                        @Param("tenantId") Long tenantId,
                        @Param("updater") String updater,
                        @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除客户（物理删除，手写 XML：显式 DELETE WHERE id = #{id} AND deleted = 1 AND tenant_id = #{tenantId}）
     *
     * @param id 客户ID
     * @param tenantId 租户ID
     * @return 影响行数（1 成功，0 失败/不存在）
     */
    int purgeCustomer(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查客户是否存在活跃业务引用（deleted=0）
     *
     * @param customerId 客户ID
     * @param tenantId 租户ID
     * @return 引用数量
     */
    Long selectActiveReferenceCount(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

}
