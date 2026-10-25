package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.suppliercontact;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.suppliercontact.SupplierContactDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 基本信息-供应商联系人表 Mapper
 *
 * 联系人是供应商的独立子资源，分页必须按 supplierId 过滤（VO 层 @NotNull 校验）。
 * 逻辑删除（deleted=0）与租户条件由框架自动追加。
 *
 * @author luna
 */
@Mapper
public interface SupplierContactMapper extends BaseMapperX<SupplierContactDO> {

    /**
     * 联系人分页查询：按 supplierId 等值过滤，主联系人优先、再按创建时间倒序。
     *
     * @param reqVO 分页与查询条件（supplierId 必填）
     * @return 联系人分页
     */
    default PageResult<SupplierContactDO> selectPage(SupplierContactPageReqVO reqVO) {
        LambdaQueryWrapperX<SupplierContactDO> wrapper = new LambdaQueryWrapperX<>();
        // supplierId 必填，联系人只在供应商工作台 Tab 内按所属供应商查询
        wrapper.eq(SupplierContactDO::getSupplierId, reqVO.getSupplierId());
        // 主联系人排在前面，同级别按创建时间倒序
        wrapper.orderByDesc(SupplierContactDO::getIsPrimary);
        wrapper.orderByDesc(SupplierContactDO::getCreateTime);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 级联恢复供应商联系人（手写 XML：显式 UPDATE ... SET deleted = 0 WHERE supplier_id = #{supplierId} AND deleted = 1 AND tenant_id = #{tenantId}）
     */
    int restoreContactsBySupplierId(@Param("supplierId") Long supplierId,
                                    @Param("tenantId") Long tenantId,
                                    @Param("updater") String updater,
                                    @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除供应商联系人（手写 XML：显式 DELETE WHERE supplier_id = #{supplierId} AND tenant_id = #{tenantId}）
     */
    int purgeContactsBySupplierId(@Param("supplierId") Long supplierId,
                                  @Param("tenantId") Long tenantId);

}
