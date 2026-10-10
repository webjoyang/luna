package com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
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
 * 覆盖票 03/05 要求的核心语义：
 *  - 联系人实时继承客户归属：五种调用（列表/详情/改/删/新建）先校验客户存在及可见性，客户不可见或不存在一律抛 1_030_004_001。
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

    @Mock
    private CustomerMapper customerMapper;

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
        // 默认 CUSTOMER_ID 存在且可见
        CustomerDO mockCustomer = new CustomerDO();
        mockCustomer.setId(CUSTOMER_ID);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(mockCustomer);
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
    void getCustomerContact_exists_success() {
        CustomerContactDO contact = buildContact(CONTACT_ID, CUSTOMER_ID, NAME, 1);
        when(customerContactMapper.selectById(CONTACT_ID)).thenReturn(contact);

        CustomerContactRespVO resp = customerContactService.getCustomerContact(CONTACT_ID);

        assertNotNull(resp);
        assertEquals(CONTACT_ID, resp.getId());
        assertEquals(CUSTOMER_ID, resp.getCustomerId());
        assertEquals(NAME, resp.getName());
    }

    @Test
    void updateCustomerContact_exists_success() {
        CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
        req.setId(CONTACT_ID);
        req.setCustomerId(CUSTOMER_ID);
        req.setName(NAME);
        req.setPhone("13900000000");
        CustomerContactDO existing = buildContact(CONTACT_ID, CUSTOMER_ID, NAME, 1);
        when(customerContactMapper.selectById(CONTACT_ID)).thenReturn(existing);

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

    // ==================== ADR-0046 / §5.3 / §9.2 用例 3 可见性继承与防探测测试 ====================

    @Test
    void createCustomerContact_customerNotVisible_throwsCode1030004001() {
        Long invisibleCustomerId = 8888L;
        when(customerMapper.selectById(invisibleCustomerId)).thenReturn(null);

        CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
        req.setCustomerId(invisibleCustomerId);
        req.setName(NAME);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.createCustomerContact(req));
        assertEquals(1_030_004_001, ex.getCode(), "客户不可见时新建联系人必须统一抛 CUSTOMER_CONTACT_NOT_EXISTS");
        verify(customerContactMapper, never()).insert(any(CustomerContactDO.class));
    }

    @Test
    void getCustomerContact_customerNotVisible_throwsCode1030004001() {
        Long invisibleCustomerId = 8888L;
        when(customerMapper.selectById(invisibleCustomerId)).thenReturn(null);
        CustomerContactDO contact = buildContact(CONTACT_ID, invisibleCustomerId, NAME, 1);
        when(customerContactMapper.selectById(CONTACT_ID)).thenReturn(contact);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.getCustomerContact(CONTACT_ID));
        assertEquals(1_030_004_001, ex.getCode(), "客户不可见时获取联系人详情必须统一抛 CUSTOMER_CONTACT_NOT_EXISTS");
    }

    @Test
    void updateCustomerContact_customerNotVisible_throwsCode1030004001() {
        Long invisibleCustomerId = 8888L;
        when(customerMapper.selectById(invisibleCustomerId)).thenReturn(null);
        CustomerContactDO contact = buildContact(CONTACT_ID, invisibleCustomerId, NAME, 1);
        when(customerContactMapper.selectById(CONTACT_ID)).thenReturn(contact);

        CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
        req.setId(CONTACT_ID);
        req.setCustomerId(invisibleCustomerId);
        req.setName(NAME);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.updateCustomerContact(req));
        assertEquals(1_030_004_001, ex.getCode(), "客户不可见时更新联系人必须统一抛 CUSTOMER_CONTACT_NOT_EXISTS");
        verify(customerContactMapper, never()).updateById(any(CustomerContactDO.class));
    }

    @Test
    void updateCustomerContact_newCustomerIdNotVisible_throwsCode1030004001() {
        Long invisibleCustomerId = 8888L;
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(new CustomerDO());
        when(customerMapper.selectById(invisibleCustomerId)).thenReturn(null);
        CustomerContactDO contact = buildContact(CONTACT_ID, CUSTOMER_ID, NAME, 1);
        when(customerContactMapper.selectById(CONTACT_ID)).thenReturn(contact);

        CustomerContactSaveReqVO req = new CustomerContactSaveReqVO();
        req.setId(CONTACT_ID);
        req.setCustomerId(invisibleCustomerId); // 企图改属不可见客户
        req.setName(NAME);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.updateCustomerContact(req));
        assertEquals(1_030_004_001, ex.getCode(), "改属不可见客户时更新必须统一抛 CUSTOMER_CONTACT_NOT_EXISTS");
        verify(customerContactMapper, never()).updateById(any(CustomerContactDO.class));
    }

    @Test
    void deleteCustomerContact_customerNotVisible_throwsCode1030004001() {
        Long invisibleCustomerId = 8888L;
        when(customerMapper.selectById(invisibleCustomerId)).thenReturn(null);
        CustomerContactDO contact = buildContact(CONTACT_ID, invisibleCustomerId, NAME, 1);
        when(customerContactMapper.selectById(CONTACT_ID)).thenReturn(contact);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.deleteCustomerContact(CONTACT_ID));
        assertEquals(1_030_004_001, ex.getCode(), "客户不可见时删除联系人必须统一抛 CUSTOMER_CONTACT_NOT_EXISTS");
        verify(customerContactMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void getCustomerContactPage_customerNotVisible_throwsCode1030004001() {
        Long invisibleCustomerId = 8888L;
        when(customerMapper.selectById(invisibleCustomerId)).thenReturn(null);

        CustomerContactPageReqVO req = new CustomerContactPageReqVO();
        req.setCustomerId(invisibleCustomerId);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerContactService.getCustomerContactPage(req));
        assertEquals(1_030_004_001, ex.getCode(), "客户不可见时分页查询联系人必须统一抛 CUSTOMER_CONTACT_NOT_EXISTS");
        verify(customerContactMapper, never()).selectPage(any(CustomerContactPageReqVO.class));
    }

    @Test
    void testFiveAccessPoints_invisibleCustomer_allUniformlyThrow1030004001() {
        // §9.2 用例 3：直连他人联系人 id（列表/详情/改/删/新建五种调法）一律 CUSTOMER_CONTACT_NOT_EXISTS (1_030_004_001)
        Long othersCustomerId = 7777L;
        Long othersContactId = 9999L;
        when(customerMapper.selectById(othersCustomerId)).thenReturn(null);
        CustomerContactDO othersContact = buildContact(othersContactId, othersCustomerId, "他人联系人", 1);
        when(customerContactMapper.selectById(othersContactId)).thenReturn(othersContact);

        // 1. 列表
        CustomerContactPageReqVO pageReq = new CustomerContactPageReqVO();
        pageReq.setCustomerId(othersCustomerId);
        ServiceException exPage = assertThrows(ServiceException.class, () -> customerContactService.getCustomerContactPage(pageReq));
        assertEquals(1_030_004_001, exPage.getCode());

        // 2. 详情
        ServiceException exGet = assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(othersContactId));
        assertEquals(1_030_004_001, exGet.getCode());

        // 3. 改
        CustomerContactSaveReqVO updateReq = new CustomerContactSaveReqVO();
        updateReq.setId(othersContactId);
        updateReq.setCustomerId(othersCustomerId);
        updateReq.setName("试图修改");
        ServiceException exUpdate = assertThrows(ServiceException.class, () -> customerContactService.updateCustomerContact(updateReq));
        assertEquals(1_030_004_001, exUpdate.getCode());

        // 4. 删
        ServiceException exDelete = assertThrows(ServiceException.class, () -> customerContactService.deleteCustomerContact(othersContactId));
        assertEquals(1_030_004_001, exDelete.getCode());

        // 5. 新建
        CustomerContactSaveReqVO createReq = new CustomerContactSaveReqVO();
        createReq.setCustomerId(othersCustomerId);
        createReq.setName("试图新建");
        ServiceException exCreate = assertThrows(ServiceException.class, () -> customerContactService.createCustomerContact(createReq));
        assertEquals(1_030_004_001, exCreate.getCode());

        // 断言写操作 DB 零变更
        verify(customerContactMapper, never()).insert(any(CustomerContactDO.class));
        verify(customerContactMapper, never()).updateById(any(CustomerContactDO.class));
        verify(customerContactMapper, never()).deleteById(othersContactId);
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
