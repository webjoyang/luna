package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customercontact;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 基本信息-客户联系人表 Mapper
 *
 * 联系人是客户的独立子资源，分页必须按 customerId 过滤（VO 层 @NotNull 校验）。
 * 逻辑删除（deleted=0）与租户条件由框架自动追加。
 *
 * @author luna
 */
@Mapper
public interface CustomerContactMapper extends BaseMapperX<CustomerContactDO> {

    /**
     * 联系人分页查询：按 customerId 等值过滤，主联系人优先、再按创建时间倒序。
     *
     * @param reqVO 分页与查询条件（customerId 必填）
     * @return 联系人分页
     */
    default PageResult<CustomerContactDO> selectPage(CustomerContactPageReqVO reqVO) {
        LambdaQueryWrapperX<CustomerContactDO> wrapper = new LambdaQueryWrapperX<>();
        // customerId 必填，联系人只在客户工作台 Tab 内按所属客户查询
        wrapper.eq(CustomerContactDO::getCustomerId, reqVO.getCustomerId());
        // 主联系人排在前面，同级别按创建时间倒序
        wrapper.orderByDesc(CustomerContactDO::getIsPrimary);
        wrapper.orderByDesc(CustomerContactDO::getCreateTime);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 级联恢复客户联系人（手写 XML：显式 UPDATE ... SET deleted = 0 WHERE customer_id = #{customerId} AND deleted = 1 AND tenant_id = #{tenantId}）
     *
     * @param customerId 客户ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param updateTime 更新时间
     * @return 影响行数
     */
    int restoreContactsByCustomerId(@Param("customerId") Long customerId,
                                    @Param("tenantId") Long tenantId,
                                    @Param("updater") String updater,
                                    @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除客户联系人（手写 XML：显式 DELETE WHERE customer_id = #{customerId} AND tenant_id = #{tenantId}）
     *
     * @param customerId 客户ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    int purgeContactsByCustomerId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

}
