package com.luna.lunasrv.module.lunaerp.service.baseinfo.customer;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;

import java.util.List;

/**
 * 基本信息-客户信息 Service 接口
 *
 * @author luna
 */
public interface CustomerService {

    /**
     * 创建客户：shortName 为空时用 companyName 回填，status 缺省 ACTIVE。
     *
     * @param createReqVO 客户
     * @return 客户ID
     */
    Long createCustomer(CustomerSaveReqVO createReqVO);

    /**
     * 获得客户分页（支持 search 智能词 + status 过滤）
     *
     * @param reqVO 分页条件
     * @return 客户分页
     */
    PageResult<CustomerRespVO> getCustomerPage(CustomerPageReqVO reqVO);

    /**
     * 获得客户详情
     *
     * @param id 客户ID
     * @return 客户
     */
    CustomerRespVO getCustomer(Long id);

    /**
     * 修改客户：shortName 为空时用 companyName 回填（与创建一致）。
     *
     * @param updateReqVO 客户
     */
    void updateCustomer(CustomerSaveReqVO updateReqVO);

    /**
     * 删除客户：先置 status='INACTIVE'，再逻辑删除（deleted=1，与 product 模块模式一致）。
     *
     * @param id 客户ID
     */
    void deleteCustomer(Long id);

    /**
     * 批量删除客户：先置 status='INACTIVE'，再逻辑删除。
     *
     * @param ids 客户ID列表
     */
    void deleteCustomerList(List<Long> ids);

    /**
     * 获取客户导入模板示例数据
     *
     * @return 示例导入数据列表
     */
    List<com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerImportVO> getImportTemplate();

    /**
     * 导入客户档案 Excel
     *
     * @param file 上传的 Excel 文件
     * @return 导入处理结果
     * @throws java.io.IOException IO 异常
     */
    com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult importCustomer(org.springframework.web.multipart.MultipartFile file) throws java.io.IOException;

}
