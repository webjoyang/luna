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
 * 数据权限与导出契约（ADR-0046 / §5.3）：
 * 1. 联系人实时继承客户归属：列表/分页通过裸 EXISTS 子查询受客户数据权限规则改写；
 * 2. 详情/更新/删除/新建均先校验客户存在及可见性，客户不存在或不可见统一抛 CUSTOMER_CONTACT_NOT_EXISTS，杜绝逐 ID 探测；
 * 3. 导出契约：当前 5 表均无 export 端点；未来若新增导出，必须复用同一带数据权限过滤的查询方法（如 selectPage / 带 EXISTS 的 Mapper 方法），严禁另开无过滤查询，确保四入口（列表、详情、写操作、导出）一致性。
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
