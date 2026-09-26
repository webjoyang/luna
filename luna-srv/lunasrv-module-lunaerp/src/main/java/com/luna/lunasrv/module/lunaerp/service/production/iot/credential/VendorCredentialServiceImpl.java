package com.luna.lunasrv.module.lunaerp.service.production.iot.credential;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.iot.VendorCredentialDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.iot.VendorCredentialMapper;
import com.luna.lunasrv.module.lunaerp.service.production.iot.crypto.IotCryptoHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.IOT_VENDOR_ACCOUNT_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.IOT_VENDOR_CREDENTIAL_NOT_EXISTS;

/**
 * 生产管理 - IoT 厂商凭证 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
public class VendorCredentialServiceImpl implements VendorCredentialService {

    @Resource
    private VendorCredentialMapper vendorCredentialMapper;

    @Resource
    private IotCryptoHelper iotCryptoHelper;

    @Override
    public Long createVendorCredential(VendorCredentialSaveReqVO createReqVO) {
        // 校验账号唯一（含软删）
        validateVendorAccountUnique(null, createReqVO.getVendorAccount());

        VendorCredentialDO credentialDO = BeanUtils.toBean(createReqVO, VendorCredentialDO.class);
        if (StringUtils.hasText(createReqVO.getVendorPassword())) {
            credentialDO.setVendorPasswordEnc(iotCryptoHelper.encrypt(createReqVO.getVendorPassword()));
        } else {
            credentialDO.setVendorPasswordEnc("");
        }
        if (!StringUtils.hasText(credentialDO.getStatus())) {
            credentialDO.setStatus("ACTIVE");
        }
        vendorCredentialMapper.insert(credentialDO);
        return credentialDO.getId();
    }

    @Override
    public void updateVendorCredential(VendorCredentialSaveReqVO updateReqVO) {
        // 校验存在
        VendorCredentialDO existing = validateVendorCredentialExists(updateReqVO.getId());
        // 校验账号唯一
        validateVendorAccountUnique(updateReqVO.getId(), updateReqVO.getVendorAccount());

        existing.setVendorAccount(updateReqVO.getVendorAccount());
        existing.setScope(updateReqVO.getScope());
        if (StringUtils.hasText(updateReqVO.getStatus())) {
            existing.setStatus(updateReqVO.getStatus());
        }
        // 若提供了新密码，则加密更新；留空则保持原密码不变
        if (StringUtils.hasText(updateReqVO.getVendorPassword())) {
            existing.setVendorPasswordEnc(iotCryptoHelper.encrypt(updateReqVO.getVendorPassword()));
        }
        vendorCredentialMapper.updateById(existing);
    }

    @Override
    public void deleteVendorCredential(Long id) {
        // 校验存在
        validateVendorCredentialExists(id);
        vendorCredentialMapper.deleteById(id);
    }

    @Override
    public VendorCredentialRespVO getVendorCredential(Long id) {
        VendorCredentialDO dobj = validateVendorCredentialExists(id);
        return convertToRespVO(dobj);
    }

    @Override
    public PageResult<VendorCredentialRespVO> getVendorCredentialPage(VendorCredentialPageReqVO pageReqVO) {
        PageResult<VendorCredentialDO> pageResult = vendorCredentialMapper.selectPage(pageReqVO);
        List<VendorCredentialRespVO> list = new ArrayList<>(pageResult.getList().size());
        for (VendorCredentialDO dobj : pageResult.getList()) {
            list.add(convertToRespVO(dobj));
        }
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public String getDecryptedPassword(Long id) {
        VendorCredentialDO dobj = validateVendorCredentialExists(id);
        if (!StringUtils.hasText(dobj.getVendorPasswordEnc())) {
            return "";
        }
        return iotCryptoHelper.decrypt(dobj.getVendorPasswordEnc());
    }

    private VendorCredentialDO validateVendorCredentialExists(Long id) {
        if (id == null) {
            throw exception(IOT_VENDOR_CREDENTIAL_NOT_EXISTS);
        }
        VendorCredentialDO dobj = vendorCredentialMapper.selectById(id);
        if (dobj == null) {
            throw exception(IOT_VENDOR_CREDENTIAL_NOT_EXISTS);
        }
        return dobj;
    }

    private void validateVendorAccountUnique(Long id, String vendorAccount) {
        if (!StringUtils.hasText(vendorAccount)) {
            return;
        }
        VendorCredentialDO existing = vendorCredentialMapper.selectByVendorAccountIncludingDeleted(vendorAccount.trim());
        if (existing == null) {
            return;
        }
        if (id == null || !existing.getId().equals(id)) {
            throw exception(IOT_VENDOR_ACCOUNT_DUPLICATE, vendorAccount);
        }
    }

    private VendorCredentialRespVO convertToRespVO(VendorCredentialDO dobj) {
        VendorCredentialRespVO respVO = BeanUtils.toBean(dobj, VendorCredentialRespVO.class);
        fillRespVOExtras(dobj, respVO);
        return respVO;
    }

    private void fillRespVOExtras(VendorCredentialDO dobj, VendorCredentialRespVO respVO) {
        respVO.setHasPassword(StringUtils.hasText(dobj.getVendorPasswordEnc()));
    }

}
