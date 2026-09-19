package com.luna.lunasrv.module.lunaerp.service.baseinfo.customer;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;

/**
 * 基本信息-客户信息 Service 实现类
 *
 * @author luna
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    /**
     * 客户默认状态
     */
    private static final String DEFAULT_STATUS = "ACTIVE";
    /**
     * 停用状态（删除时先置该状态，再逻辑删除）
     */
    private static final String STATUS_INACTIVE = "INACTIVE";

    @Resource
    private CustomerMapper customerMapper;

    @Override
    public Long createCustomer(CustomerSaveReqVO createReqVO) {
        CustomerDO customer = BeanUtils.toBean(createReqVO, CustomerDO.class);
        // shortName 为空时用 companyName 回填
        backfillShortName(customer);
        // status 缺省 ACTIVE
        if (!StringUtils.hasText(customer.getStatus())) {
            customer.setStatus(DEFAULT_STATUS);
        }
        customerMapper.insert(customer);
        return customer.getId();
    }

    @Override
    public PageResult<CustomerRespVO> getCustomerPage(CustomerPageReqVO reqVO) {
        PageResult<CustomerDO> pageResult = customerMapper.selectPage(reqVO);
        return BeanUtils.toBean(pageResult, CustomerRespVO.class);
    }

    @Override
    public CustomerRespVO getCustomer(Long id) {
        CustomerDO customer = customerMapper.selectById(id);
        if (customer == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
        return BeanUtils.toBean(customer, CustomerRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(CustomerSaveReqVO updateReqVO) {
        // 校验存在
        validateCustomerExists(updateReqVO.getId());
        // 更新（shortName 为空时用 companyName 回填，与创建一致）
        CustomerDO updateObj = BeanUtils.toBean(updateReqVO, CustomerDO.class);
        backfillShortName(updateObj);
        customerMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long id) {
        // 校验存在
        validateCustomerExists(id);
        doDelete(Collections.singletonList(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerList(List<Long> ids) {
        doDelete(ids);
    }

    // ==================== 私有方法 ====================

    /**
     * shortName 为空时用 companyName 回填（create/update 共用）
     */
    private void backfillShortName(CustomerDO customer) {
        if (!StringUtils.hasText(customer.getShortName())) {
            customer.setShortName(customer.getCompanyName());
        }
    }

    /**
     * 校验客户存在，不存在抛 1_030_004_000
     */
    private void validateCustomerExists(Long id) {
        if (id == null || customerMapper.selectById(id) == null) {
            throw exception(CUSTOMER_NOT_EXISTS);
        }
    }

    /**
     * 批量逻辑删除：先置 status='INACTIVE'，再走 mapper.deleteByIds（deleted=1，与 product 模块模式一致）。
     * 仅处理当前租户下存在的客户。
     */
    private void doDelete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<CustomerDO> customers = customerMapper.selectByIds(ids);
        if (CollUtil.isEmpty(customers)) {
            return;
        }
        // 1. 先置 status='INACTIVE'
        for (CustomerDO customer : customers) {
            CustomerDO updateObj = new CustomerDO();
            updateObj.setId(customer.getId());
            updateObj.setStatus(STATUS_INACTIVE);
            customerMapper.updateById(updateObj);
        }
        // 2. 逻辑删除（deleted=1）
        customerMapper.deleteByIds(ids);
    }

}
