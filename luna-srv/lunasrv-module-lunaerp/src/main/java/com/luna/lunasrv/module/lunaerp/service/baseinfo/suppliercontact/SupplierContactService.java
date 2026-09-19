package com.luna.lunasrv.module.lunaerp.service.baseinfo.suppliercontact;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactSaveReqVO;

/**
 * 基本信息-供应商联系人 Service 接口
 *
 * 联系人作为供应商的独立子资源，存在独立权限码（lunaerp:baseinfo:supplier-contact:*），
 * 不与供应商主表共用 Service。
 *
 * @author luna
 */
public interface SupplierContactService {

    /**
     * 创建联系人
     *
     * @param createReqVO 联系人（supplierId、name 必填，由 VO 校验）
     * @return 联系人ID
     */
    Long createSupplierContact(SupplierContactSaveReqVO createReqVO);

    /**
     * 更新联系人
     *
     * @param updateReqVO 联系人（id、supplierId、name 必填）
     */
    void updateSupplierContact(SupplierContactSaveReqVO updateReqVO);

    /**
     * 删除联系人（逻辑删除）
     *
     * @param id 联系人ID
     */
    void deleteSupplierContact(Long id);

    /**
     * 获得联系人详情
     *
     * @param id 联系人ID
     * @return 联系人
     */
    SupplierContactRespVO getSupplierContact(Long id);

    /**
     * 获得联系人分页（按 supplierId 过滤）
     *
     * @param reqVO 分页条件（supplierId 必填）
     * @return 联系人分页
     */
    PageResult<SupplierContactRespVO> getSupplierContactPage(SupplierContactPageReqVO reqVO);

}
