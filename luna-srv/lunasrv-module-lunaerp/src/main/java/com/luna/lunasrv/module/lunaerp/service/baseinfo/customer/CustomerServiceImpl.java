package com.luna.lunasrv.module.lunaerp.service.baseinfo.customer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportHelper;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ImportRow;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerImportVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
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

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public Long createCustomer(CustomerSaveReqVO createReqVO) {
        validateCustomerNotMasked(createReqVO);
        CustomerDO customer = BeanUtils.toBean(createReqVO, CustomerDO.class);
        // shortName 为空时用 companyName 回填
        backfillShortName(customer);
        // status 缺省 ACTIVE
        if (!StringUtils.hasText(customer.getStatus())) {
            customer.setStatus(DEFAULT_STATUS);
        }
        // 归属人缺省当前登录用户，归属部门服务端推导（ADR-0046）
        if (customer.getSalespersonUserId() == null) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId != null) {
                customer.setSalespersonUserId(loginUserId);
            }
        }
        if (customer.getSalespersonUserId() != null && customer.getDeptId() == null && adminUserApi != null) {
            AdminUserRespDTO user = adminUserApi.getUser(customer.getSalespersonUserId());
            if (user != null) {
                customer.setDeptId(user.getDeptId());
            }
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
        validateCustomerNotMasked(updateReqVO);
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

    /**
     * 校验联系电话不含脱敏字符，写接口不接受脱敏值入库
     */
    private void validateCustomerNotMasked(CustomerSaveReqVO reqVO) {
        if (reqVO != null && reqVO.getPhone() != null && reqVO.getPhone().contains("*")) {
            throw exception(BAD_REQUEST, "联系电话不能包含脱敏字符");
        }
    }

    @Override
    public List<CustomerImportVO> getImportTemplate() {
        return Collections.singletonList(
                CustomerImportVO.builder()
                        .companyName("示例客户A有限公司")
                        .shortName("示例客户A")
                        .clientType("FORMAL")
                        .industry("TEXTILE")
                        .scale("MEDIUM")
                        .source("SELF_DEV")
                        .cooperationStatus("COOPERATING")
                        .unifiedSocialCreditCode("91330100MA12345678")
                        .nameEn("Sample Customer A Co., Ltd.")
                        .addressEn("Yuhang District, Hangzhou, Zhejiang")
                        .remark("标准客户档案示例")
                        .build()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importCustomer(MultipartFile file) throws IOException {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        Long deptId = null;
        if (loginUserId != null && adminUserApi != null) {
            AdminUserRespDTO user = adminUserApi.getUser(loginUserId);
            if (user != null) {
                deptId = user.getDeptId();
            }
        }
        final Long finalDeptId = deptId;

        return ExcelImportHelper.importExcel(
                file,
                CustomerImportVO.class,
                this::validateCustomerImportRows,
                validList -> {
                    List<CustomerDO> doList = new ArrayList<>(validList.size());
                    for (CustomerImportVO vo : validList) {
                        CustomerDO customer = BeanUtils.toBean(vo, CustomerDO.class);
                        // 简称空则用企业名称回填
                        backfillShortName(customer);
                        // 导入行 status 默认 ACTIVE
                        customer.setStatus(DEFAULT_STATUS);
                        // 归属人=登录用户、归属部门=其当前部门（服务端推导）
                        customer.setSalespersonUserId(loginUserId);
                        customer.setDeptId(finalDeptId);
                        doList.add(customer);
                    }
                    customerMapper.insertBatch(doList);
                }
        );
    }

    /**
     * 逐行校验客户导入数据：
     * 1. 企业名称必填校验
     * 2. 统一社会信用代码 18 位格式校验
     * 3. 文件内企业名称重复排查
     * 4. 数据库内企业名称查重排查（租户内已存在）
     */
    private void validateCustomerImportRows(List<ImportRow<CustomerImportVO>> rows) {
        Set<String> seenCompanyNames = new HashSet<>();
        Set<String> fileDuplicateCompanyNames = new HashSet<>();
        List<String> queryCompanyNames = new ArrayList<>();

        for (ImportRow<CustomerImportVO> row : rows) {
            CustomerImportVO vo = row.getData();
            String name = vo.getCompanyName() != null ? vo.getCompanyName().trim() : "";
            if (StrUtil.isBlank(name)) {
                row.addError("企业名称不能为空");
            } else {
                if (!seenCompanyNames.add(name)) {
                    fileDuplicateCompanyNames.add(name);
                }
                queryCompanyNames.add(name);
            }

            // 统一社会信用代码 18 位格式校验
            String creditCode = vo.getUnifiedSocialCreditCode();
            if (StrUtil.isNotBlank(creditCode)) {
                String trimmedCode = creditCode.trim();
                if (trimmedCode.length() != 18 || !trimmedCode.matches("^[0-9A-Za-z]{18}$")) {
                    row.addError("统一社会信用代码格式不正确（必须为18位）");
                }
            }
        }

        // 标记文件中重复出现的企业名称
        if (!fileDuplicateCompanyNames.isEmpty()) {
            for (ImportRow<CustomerImportVO> row : rows) {
                String name = row.getData().getCompanyName() != null ? row.getData().getCompanyName().trim() : "";
                if (fileDuplicateCompanyNames.contains(name)) {
                    row.addError("企业名称在文件中重复");
                }
            }
        }

        // 校验数据库中已存在的企业名称（租户内重名检查）
        if (!queryCompanyNames.isEmpty()) {
            List<CustomerDO> existingCustomers = customerMapper.selectListByCompanyNames(queryCompanyNames);
            if (CollUtil.isNotEmpty(existingCustomers)) {
                Set<String> existingNames = existingCustomers.stream()
                        .map(CustomerDO::getCompanyName)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .collect(Collectors.toSet());
                for (ImportRow<CustomerImportVO> row : rows) {
                    String name = row.getData().getCompanyName() != null ? row.getData().getCompanyName().trim() : "";
                    if (existingNames.contains(name)) {
                        row.addError("企业名称已存在");
                    }
                }
            }
        }
    }

}
