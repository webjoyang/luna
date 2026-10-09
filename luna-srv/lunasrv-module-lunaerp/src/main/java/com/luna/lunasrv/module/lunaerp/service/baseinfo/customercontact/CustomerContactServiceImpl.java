package com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customercontact.CustomerContactMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_CONTACT_NOT_EXISTS;

/**
 * 基本信息-客户联系人 Service 实现类
 *
 * customerId 与 name 必填由 Controller 层 @Valid 校验 VO 注解；
 * isPrimary 无单点约束，业务允许同客户多条主联系人。
 *
 * @author luna
 */
@Service
public class CustomerContactServiceImpl implements CustomerContactService {

    @Resource
    private CustomerContactMapper customerContactMapper;

    @Override
    public Long createCustomerContact(CustomerContactSaveReqVO createReqVO) {
        validateCustomerContactNotMasked(createReqVO);
        CustomerContactDO contact = BeanUtils.toBean(createReqVO, CustomerContactDO.class);
        customerContactMapper.insert(contact);
        return contact.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerContact(CustomerContactSaveReqVO updateReqVO) {
        // 校验存在
        validateCustomerContactExists(updateReqVO.getId());
        validateCustomerContactNotMasked(updateReqVO);
        // 更新
        CustomerContactDO updateObj = BeanUtils.toBean(updateReqVO, CustomerContactDO.class);
        customerContactMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerContact(Long id) {
        // 校验存在
        validateCustomerContactExists(id);
        // 逻辑删除（deleted=1）
        customerContactMapper.deleteById(id);
    }

    @Override
    public CustomerContactRespVO getCustomerContact(Long id) {
        CustomerContactDO contact = customerContactMapper.selectById(id);
        if (contact == null) {
            throw exception(CUSTOMER_CONTACT_NOT_EXISTS);
        }
        return BeanUtils.toBean(contact, CustomerContactRespVO.class);
    }

    @Override
    public PageResult<CustomerContactRespVO> getCustomerContactPage(CustomerContactPageReqVO reqVO) {
        PageResult<CustomerContactDO> pageResult = customerContactMapper.selectPage(reqVO);
        return BeanUtils.toBean(pageResult, CustomerContactRespVO.class);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验联系人存在，不存在抛 1_030_004_001
     */
    private void validateCustomerContactExists(Long id) {
        if (id == null || customerContactMapper.selectById(id) == null) {
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
