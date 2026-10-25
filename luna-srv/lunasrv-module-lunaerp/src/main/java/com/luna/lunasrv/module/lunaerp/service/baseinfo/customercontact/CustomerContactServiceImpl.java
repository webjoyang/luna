package com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customercontact.CustomerContactMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_CONTACT_NOT_EXISTS;

/**
 * 基本信息-客户联系人 Service 实现类
 *
 * customerId 与 name 必填由 Controller 层 @Valid 校验 VO 注解；
 * isPrimary 无单点约束，业务允许同客户多条主联系人。
 *
 * 数据权限与四入口一致性（ADR-0046 / §5.3 / §5.4）：
 * 联系人详情/update/delete/create/page 先校验客户存在且可见（customerMapper.selectById(customerId) 自动受规则过滤）
 * 客户真不存在与存在但不可见对外统一抛 CUSTOMER_CONTACT_NOT_EXISTS（1_030_004_001），杜绝逐 ID 探测。
 *
 * @author luna
 */
@Service
public class CustomerContactServiceImpl implements CustomerContactService {

    @Resource
    private CustomerContactMapper customerContactMapper;

    @Resource
    private CustomerMapper customerMapper;

    @Override
    public Long createCustomerContact(CustomerContactSaveReqVO createReqVO) {
        validateCustomerContactNotMasked(createReqVO);
        // 可见性校验（自动受规则过滤）：客户真不存在或存在但不可见均统一抛 CUSTOMER_CONTACT_NOT_EXISTS
        validateCustomerVisible(createReqVO != null ? createReqVO.getCustomerId() : null);
        CustomerContactDO contact = BeanUtils.toBean(createReqVO, CustomerContactDO.class);
        customerContactMapper.insert(contact);
        return contact.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerContact(CustomerContactSaveReqVO updateReqVO) {
        if (updateReqVO == null) {
            throw exception(CUSTOMER_CONTACT_NOT_EXISTS);
        }
        // 校验联系人存在及客户可见性
        CustomerContactDO existing = validateCustomerContactExistsAndVisible(updateReqVO.getId());
        validateCustomerContactNotMasked(updateReqVO);
        // 若入参修改了 customerId，新客户也必须校验可见性
        if (updateReqVO.getCustomerId() != null && !Objects.equals(existing.getCustomerId(), updateReqVO.getCustomerId())) {
            validateCustomerVisible(updateReqVO.getCustomerId());
        }
        // 更新
        CustomerContactDO updateObj = BeanUtils.toBean(updateReqVO, CustomerContactDO.class);
        customerContactMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerContact(Long id) {
        // 校验存在及客户可见性
        validateCustomerContactExistsAndVisible(id);
        // 逻辑删除（deleted=1）
        customerContactMapper.deleteById(id);
    }

    @Override
    public CustomerContactRespVO getCustomerContact(Long id) {
        CustomerContactDO contact = validateCustomerContactExistsAndVisible(id);
        return BeanUtils.toBean(contact, CustomerContactRespVO.class);
    }

    @Override
    public PageResult<CustomerContactRespVO> getCustomerContactPage(CustomerContactPageReqVO reqVO) {
        // 可见性校验（自动受规则过滤）：客户真不存在或不可见统一抛 CUSTOMER_CONTACT_NOT_EXISTS
        validateCustomerVisible(reqVO != null ? reqVO.getCustomerId() : null);
        PageResult<CustomerContactDO> pageResult = customerContactMapper.selectPage(reqVO);
        return BeanUtils.toBean(pageResult, CustomerContactRespVO.class);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验联系人存在且所属客户对当前用户可见，任一不满足均对外统一抛 CUSTOMER_CONTACT_NOT_EXISTS（1_030_004_001），杜绝逐 ID 探测
     */
    private CustomerContactDO validateCustomerContactExistsAndVisible(Long id) {
        if (id == null) {
            throw exception(CUSTOMER_CONTACT_NOT_EXISTS);
        }
        CustomerContactDO contact = customerContactMapper.selectById(id);
        if (contact == null) {
            throw exception(CUSTOMER_CONTACT_NOT_EXISTS);
        }
        validateCustomerVisible(contact.getCustomerId());
        return contact;
    }

    /**
     * 校验客户存在且对当前用户可见（自动受规则过滤），真不存在与存在但不可见对外同一回答（抛 CUSTOMER_CONTACT_NOT_EXISTS 1_030_004_001）
     */
    private void validateCustomerVisible(Long customerId) {
        if (customerId == null || customerMapper.selectById(customerId) == null) {
            throw exception(CUSTOMER_CONTACT_NOT_EXISTS);
        }
    }

    /**
     * 校验联系人字段不含脱敏占位符，写接口不接受脱敏值入库
     */
    private void validateCustomerContactNotMasked(CustomerContactSaveReqVO reqVO) {
        if (reqVO == null) {
            return;
        }
        if (reqVO.getName() != null && reqVO.getName().contains("*")) {
            throw exception(BAD_REQUEST, "联系人姓名不能包含脱敏字符");
        }
        if (reqVO.getPhone() != null && reqVO.getPhone().contains("*")) {
            throw exception(BAD_REQUEST, "联系电话不能包含脱敏字符");
        }
        if (reqVO.getEmail() != null && reqVO.getEmail().contains("*")) {
            throw exception(BAD_REQUEST, "邮箱不能包含脱敏字符");
        }
    }

}
