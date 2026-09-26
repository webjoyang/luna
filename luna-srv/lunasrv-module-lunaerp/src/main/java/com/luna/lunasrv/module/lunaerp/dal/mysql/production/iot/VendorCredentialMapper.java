package com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.VendorCredentialDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

/**
 * 生产管理 - IoT 厂商凭证 Mapper
 *
 * @author luna
 */
@Mapper
public interface VendorCredentialMapper extends BaseMapperX<VendorCredentialDO> {

    default PageResult<VendorCredentialDO> selectPage(VendorCredentialPageReqVO reqVO) {
        LambdaQueryWrapperX<VendorCredentialDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(VendorCredentialDO::getVendorAccount, reqVO.getVendorAccount());
        wrapper.eqIfPresent(VendorCredentialDO::getStatus, reqVO.getStatus());
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (StringUtils.hasText(search)) {
            String like = "%" + search + "%";
            wrapper.and(w -> w
                    .apply("LOWER(vendor_account) LIKE LOWER({0})", like)
                    .or().apply("LOWER(scope) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(VendorCredentialDO::getCreateTime).orderByDesc(VendorCredentialDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default VendorCredentialDO selectByVendorAccount(String vendorAccount) {
        return selectOne(VendorCredentialDO::getVendorAccount, vendorAccount);
    }

    @Select("SELECT id, vendor_account, vendor_password_enc, scope, status, last_sync_at, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_vendor_credential WHERE vendor_account = #{vendorAccount} LIMIT 1")
    VendorCredentialDO selectByVendorAccountIncludingDeleted(@Param("vendorAccount") String vendorAccount);

}
