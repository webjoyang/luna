package com.luna.lunasrv.module.lunaerp.service.production.iot.credential;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.iot.vo.credential.VendorCredentialSaveReqVO;
import jakarta.validation.Valid;

/**
 * 生产管理 - IoT 厂商凭证 Service 接口
 *
 * @author luna
 */
public interface VendorCredentialService {

    /**
     * 创建厂商凭证
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createVendorCredential(@Valid VendorCredentialSaveReqVO createReqVO);

    /**
     * 更新厂商凭证
     *
     * @param updateReqVO 更新信息
     */
    void updateVendorCredential(@Valid VendorCredentialSaveReqVO updateReqVO);

    /**
     * 删除厂商凭证
     *
     * @param id 编号
     */
    void deleteVendorCredential(Long id);

    /**
     * 获得厂商凭证详情（不回显明文密码）
     *
     * @param id 编号
     * @return 厂商凭证
     */
    VendorCredentialRespVO getVendorCredential(Long id);

    /**
     * 获得厂商凭证分页
     *
     * @param pageReqVO 分页查询
     * @return 厂商凭证分页
     */
    PageResult<VendorCredentialRespVO> getVendorCredentialPage(VendorCredentialPageReqVO pageReqVO);

    /**
     * 获取解密后的密码（供采集器内部认证调用，不对外暴露端点）
     *
     * @param id 凭证编号
     * @return 解密后的密码
     */
    String getDecryptedPassword(Long id);

}
