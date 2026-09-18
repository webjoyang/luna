package com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactSaveReqVO;

/**
 * 基本信息-客户联系人 Service 接口
 *
 * 联系人作为客户的独立子资源，存在独立权限码（lunaerp:baseinfo:customer-contact:*），
 * 不与客户主表共用 Service。
 *
 * @author luna
 */
public interface CustomerContactService {

    /**
     * 创建联系人
     *
     * @param createReqVO 联系人（customerId、name 必填，由 VO 校验）
     * @return 联系人ID
     */
    Long createCustomerContact(CustomerContactSaveReqVO createReqVO);

    /**
     * 更新联系人
     *
     * @param updateReqVO 联系人（id、customerId、name 必填）
     */
    void updateCustomerContact(CustomerContactSaveReqVO updateReqVO);

    /**
     * 删除联系人（逻辑删除）
     *
     * @param id 联系人ID
     */
    void deleteCustomerContact(Long id);

    /**
     * 获得联系人详情
     *
     * @param id 联系人ID
     * @return 联系人
     */
    CustomerContactRespVO getCustomerContact(Long id);

    /**
     * 获得联系人分页（按 customerId 过滤）
     *
     * @param reqVO 分页条件（customerId 必填）
     * @return 联系人分页
     */
    PageResult<CustomerContactRespVO> getCustomerContactPage(CustomerContactPageReqVO reqVO);

}
