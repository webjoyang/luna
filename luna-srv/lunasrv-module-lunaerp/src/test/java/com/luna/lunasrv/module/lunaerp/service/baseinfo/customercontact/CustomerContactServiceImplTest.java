package com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customercontact.CustomerContactMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CustomerContactServiceImpl} 的纯 Mockito 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * 覆盖票 03 要求的核心语义：
 *  - getCustomerContactPage：按 customerId 过滤（空列表 / 有数据两种）。
 *  - createCustomerContact：成功返回 id；customerId、name 原样落库。
 *  - updateCustomerContact：不存在 id 抛 1_030_004_001；存在时成功更新。
 *  - deleteCustomerContact：不存在抛错；存在时逻辑删除（deleteById）。
 *  - VO 约束（内嵌 ReqVOValidationTest）：create/update 的 customerId、name 必填，
 *    page 的 customerId 缺失直接校验失败；等价覆盖 Controller @Valid 的必填报错。
 *
 * 说明：Controller 层 MockMvc 集成测试（含权限码缺失 403）需要完整的 security filter chain
 * + 测试数据库，本仓 lunaerp 模块当前无该基础设施（与票 02 CustomerServiceImplTest 同策略，
 * 参照 product 模块亦无测试），故先以纯单元测试覆盖核心逻辑。
 *
 * @author luna
 */
class CustomerContactServiceImplTest {

    @Mock
    private CustomerContactMapper customerContactMapper;

    @InjectMocks
    private CustomerContactServiceImpl customerContactService;

    private static final Long CUSTOMER_ID = 1024L;
    private static final Long CONTACT_ID = 2048L;
    private static final String NAME = "张三";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 模拟 insert 时回填主键（@KeySequence 在真实环境由 MyBatis-Plus 填充）
        when(customerContactMapper.insert(any(CustomerContactDO.class))).thenAnswer(invocation -> {
            CustomerContactDO dobj = invocation.getArgument(0);
            dobj.setId(CONTACT_ID);
            return 1;
        });
    }

    @Test
    void getCustomerContactPage_empty_returnsEmptyList() {
        CustomerContactPageReqVO reqVO = new CustomerContactPageReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        when(customerContactMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        PageResult<CustomerContactRespVO> result = customerContactService.getCustomerContactPage(reqVO);

        assertEquals(0L, result.getTotal());
        assertTrue(result.getList().isEmpty(), "该客户无联系人时应返回空列表");
        // 确认确实以 customerId 维度查询（子资源不允许跨客户拉全量）
        verify(customerContactMapper).selectPage(reqVO);
    }

    @Test
    void getCustomerContactPage_withData_returnsMappedList() {
        CustomerContactPageReqVO reqVO = new CustomerContactPageReqVO();
        reqVO.setCustomerId(CUSTOMER_ID);
        CustomerContactDO contact = buildContact(CONTACT_ID, CUSTOMER_ID, NAME, 1);
        when(customerContactMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(contact), 1L));

        PageResult<CustomerContactRespVO> result = customerContactService.getCustomerContactPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        CustomerContactRespVO resp = result.getList().get(0);
        assertEquals(CONTACT_ID, resp.getId());
        assertEquals(CUSTOMER_ID, resp.getCustomerId());
        assertEquals(NAME, resp.getName());
        assertEquals(1, resp.getIsPrimary());
    }

    @Test
    void createCustomerContact_success_returnsId() {
        CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
        req.setCustomerId(CUSTOMER_ID);
        req.setName(NAME);
        req.setPosition("采购经理");
        req.setPhone("13800000000");
        req.setIsPrimary(1);

        Long id = customerContactService.createCustomerContact(req);

        ArgumentCaptor<CustomerContactDO> captor = ArgumentCaptor.forClass(CustomerContactDO.class);
        verify(customerContactMapper).insert(captor.capture());
        CustomerContactDO saved = captor.getValue();
        assertEquals(CONTACT_ID, id);
        assertEquals(CUSTOMER_ID, saved.getCustomerId());
        assertEquals(NAME, saved.getName());
        assertEquals("采购经理", saved.getPosition());
        assertEquals("13800000000", saved.getPhone());
        assertEquals(1, saved.getIsPrimary());
    }

    @Test
    void getCustomerContact_notExists_throwsCode1030004001() {
        when(customerContactMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.getCustomerContact(9999L));
        assertEquals(1_030_004_001, ex.getCode());
    }

    @Test
    void updateCustomerContact_notExists_throwsCode1030004001() {
        CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
        req.setId(9999L);
        req.setCustomerId(CUSTOMER_ID);
        req.setName(NAME);
        when(customerContactMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.updateCustomerContact(req));
        assertEquals(1_030_004_001, ex.getCode());
        verify(customerContactMapper, never()).updateById(any(CustomerContactDO.class));
    }

    @Test
    void updateCustomerContact_exists_success() {
        CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
        req.setId(CONTACT_ID);
        req.setCustomerId(CUSTOMER_ID);
        req.setName(NAME);
        req.setPhone("13900000000");
        when(customerContactMapper.selectById(CONTACT_ID)).thenReturn(new CustomerContactDO());

        customerContactService.updateCustomerContact(req);

        ArgumentCaptor<CustomerContactDO> captor = ArgumentCaptor.forClass(CustomerContactDO.class);
        verify(customerContactMapper).updateById(captor.capture());
        CustomerContactDO updated = captor.getValue();
        assertEquals(CONTACT_ID, updated.getId());
        assertEquals(CUSTOMER_ID, updated.getCustomerId());
        assertEquals("13900000000", updated.getPhone());
    }

    @Test
    void deleteCustomerContact_notExists_throwsCode1030004001() {
        when(customerContactMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.deleteCustomerContact(9999L));
        assertEquals(1_030_004_001, ex.getCode());
        verify(customerContactMapper, never()).deleteById(9999L);
    }

    @Test
    void deleteCustomerContact_exists_logicalDelete() {
        when(customerContactMapper.selectById(CONTACT_ID)).thenReturn(buildContact(CONTACT_ID, CUSTOMER_ID, NAME, 0));

        customerContactService.deleteCustomerContact(CONTACT_ID);

        // 走 MyBatis-Plus 逻辑删除（deleted=1），仅删除一次
        verify(customerContactMapper).deleteById(CONTACT_ID);
    }

    private CustomerContactDO buildContact(Long id, Long customerId, String name, Integer isPrimary) {
        CustomerContactDO contact = new CustomerContactDO();
        contact.setId(id);
        contact.setCustomerId(customerId);
        contact.setName(name);
        contact.setIsPrimary(isPrimary);
        return contact;
    }

    /**
     * VO Bean Validation 约束测试：等价验证 Controller 层 @Valid 在
     * create/update（customerId、name 必填、email 格式）与 page（customerId 必填）上的拦截行为，
     * 无需启动 Spring 上下文。
     */
    @Nested
    class ReqVOValidationTest {

        private final Validator validator = buildDefaultValidatorFactory().getValidator();

        private Set<String> invalidProperties(Object bean) {
            return validator.validate(bean).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());
        }

        @Test
        void saveReqVO_customerIdMissing_invalid() {
            CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
            req.setName(NAME);

            Set<String> invalid = invalidProperties(req);
            assertTrue(invalid.contains("customerId"), "customerId 缺失应校验失败");
        }

        @Test
        void saveReqVO_nameBlank_invalid() {
            CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
            req.setCustomerId(CUSTOMER_ID);
            req.setName("  ");

            Set<String> invalid = invalidProperties(req);
            assertTrue(invalid.contains("name"), "name 空白应校验失败");
        }

        @Test
        void saveReqVO_emailMalformed_invalid() {
            CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
            req.setCustomerId(CUSTOMER_ID);
            req.setName(NAME);
            req.setEmail("not-an-email");

            Set<String> invalid = invalidProperties(req);
            assertTrue(invalid.contains("email"), "邮箱格式非法应校验失败");
        }

        @Test
        void saveReqVO_allRequiredPresent_valid() {
            CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
            req.setCustomerId(CUSTOMER_ID);
            req.setName(NAME);
            req.setEmail("zhangsan@example.com");

            Set<ConstraintViolation<CustomerContactSaveReqVO>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "必填齐全且邮箱合法时应通过校验");
        }

        @Test
        void pageReqVO_customerIdMissing_invalid() {
            CustomerContactPageReqVO req = new CustomerContactPageReqVO();

            Set<String> invalid = invalidProperties(req);
            assertTrue(invalid.contains("customerId"), "GET /page 缺少 customerId 应校验失败");
        }

        @Test
        void pageReqVO_customerIdPresent_valid() {
            CustomerContactPageReqVO req = new CustomerContactPageReqVO();
            req.setCustomerId(CUSTOMER_ID);

            Set<ConstraintViolation<CustomerContactPageReqVO>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "传入 customerId 时分页参数应通过校验");
        }

    }

}
