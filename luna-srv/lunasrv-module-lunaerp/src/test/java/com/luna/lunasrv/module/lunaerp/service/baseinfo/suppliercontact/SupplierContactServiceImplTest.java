package com.luna.lunasrv.module.lunaerp.service.baseinfo.suppliercontact;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.suppliercontact.vo.SupplierContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.suppliercontact.SupplierContactDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.suppliercontact.SupplierContactMapper;
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
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SupplierContactServiceImpl} 的纯 Mockito 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * 覆盖核心语义：
 *  - getSupplierContactPage：按 supplierId 过滤（空列表 / 有数据两种）。
 *  - createSupplierContact：成功返回 id；supplierId、name 原样落库。
 *  - updateSupplierContact：不存在 id 抛 1_030_005_002；存在时成功更新。
 *  - deleteSupplierContact：不存在抛错；存在时逻辑删除（deleteById）。
 *  - VO 约束（内嵌 ReqVOValidationTest）：create/update 的 supplierId、name 必填，
 *    page 的 supplierId 缺失直接校验失败；等价覆盖 Controller @Valid 的必填报错。
 *
 * 说明：Controller 层 MockMvc 集成测试需要完整的 security filter chain + 测试数据库，
 * 本仓 lunaerp 模块当前无该基础设施，故以纯单元测试覆盖核心逻辑。
 *
 * @author luna
 */
class SupplierContactServiceImplTest {

    @Mock
    private SupplierContactMapper supplierContactMapper;

    @InjectMocks
    private SupplierContactServiceImpl supplierContactService;

    private static final Long SUPPLIER_ID = 1024L;
    private static final Long CONTACT_ID = 2048L;
    private static final String NAME = "张三";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 模拟 insert 时回填主键（@KeySequence 在真实环境由 MyBatis-Plus 填充）
        when(supplierContactMapper.insert(any(SupplierContactDO.class))).thenAnswer(invocation -> {
            SupplierContactDO dobj = invocation.getArgument(0);
            dobj.setId(CONTACT_ID);
            return 1;
        });
    }

    private SupplierContactDO buildContact(Long id, Long supplierId, String name, Integer isPrimary) {
        SupplierContactDO contact = new SupplierContactDO();
        contact.setId(id);
        contact.setSupplierId(supplierId);
        contact.setName(name);
        contact.setIsPrimary(isPrimary);
        return contact;
    }

    @Test
    void getSupplierContactPage_empty_returnsEmptyList() {
        SupplierContactPageReqVO reqVO = new SupplierContactPageReqVO();
        reqVO.setSupplierId(SUPPLIER_ID);
        when(supplierContactMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        PageResult<SupplierContactRespVO> result = supplierContactService.getSupplierContactPage(reqVO);

        assertEquals(0L, result.getTotal());
        assertTrue(result.getList().isEmpty(), "该供应商无联系人时应返回空列表");
        verify(supplierContactMapper).selectPage(reqVO);
    }

    @Test
    void getSupplierContactPage_withData_returnsMappedList() {
        SupplierContactPageReqVO reqVO = new SupplierContactPageReqVO();
        reqVO.setSupplierId(SUPPLIER_ID);
        SupplierContactDO contact = buildContact(CONTACT_ID, SUPPLIER_ID, NAME, 1);
        when(supplierContactMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(contact), 1L));

        PageResult<SupplierContactRespVO> result = supplierContactService.getSupplierContactPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        SupplierContactRespVO resp = result.getList().get(0);
        assertEquals(CONTACT_ID, resp.getId());
        assertEquals(SUPPLIER_ID, resp.getSupplierId());
        assertEquals(NAME, resp.getName());
        assertEquals(1, resp.getIsPrimary());
    }

    @Test
    void createSupplierContact_success_returnsId() {
        SupplierContactSaveReqVO req = new SupplierContactSaveReqVO();
        req.setSupplierId(SUPPLIER_ID);
        req.setName(NAME);
        req.setPosition("采购经理");
        req.setPhone("13800000000");
        req.setIsPrimary(1);

        Long id = supplierContactService.createSupplierContact(req);

        ArgumentCaptor<SupplierContactDO> captor = ArgumentCaptor.forClass(SupplierContactDO.class);
        verify(supplierContactMapper).insert(captor.capture());
        SupplierContactDO saved = captor.getValue();
        assertEquals(CONTACT_ID, id);
        assertEquals(SUPPLIER_ID, saved.getSupplierId());
        assertEquals(NAME, saved.getName());
        assertEquals("采购经理", saved.getPosition());
        assertEquals("13800000000", saved.getPhone());
        assertEquals(1, saved.getIsPrimary());
    }

    @Test
    void getSupplierContact_notExists_throwsCode1030005002() {
        when(supplierContactMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierContactService.getSupplierContact(9999L));
        assertEquals(1_030_005_002, ex.getCode());
    }

    @Test
    void updateSupplierContact_notExists_throwsCode1030005002() {
        SupplierContactSaveReqVO req = new SupplierContactSaveReqVO();
        req.setId(9999L);
        req.setSupplierId(SUPPLIER_ID);
        req.setName(NAME);
        when(supplierContactMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierContactService.updateSupplierContact(req));
        assertEquals(1_030_005_002, ex.getCode());
        verify(supplierContactMapper, never()).updateById(any(SupplierContactDO.class));
    }

    @Test
    void updateSupplierContact_exists_success() {
        SupplierContactSaveReqVO req = new SupplierContactSaveReqVO();
        req.setId(CONTACT_ID);
        req.setSupplierId(SUPPLIER_ID);
        req.setName(NAME);
        req.setPhone("13900000000");
        when(supplierContactMapper.selectById(CONTACT_ID)).thenReturn(new SupplierContactDO());

        supplierContactService.updateSupplierContact(req);

        ArgumentCaptor<SupplierContactDO> captor = ArgumentCaptor.forClass(SupplierContactDO.class);
        verify(supplierContactMapper).updateById(captor.capture());
        SupplierContactDO updated = captor.getValue();
        assertEquals(CONTACT_ID, updated.getId());
        assertEquals(SUPPLIER_ID, updated.getSupplierId());
        assertEquals("13900000000", updated.getPhone());
    }

    @Test
    void deleteSupplierContact_notExists_throwsCode1030005002() {
        when(supplierContactMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> supplierContactService.deleteSupplierContact(9999L));
        assertEquals(1_030_005_002, ex.getCode());
        verify(supplierContactMapper, never()).deleteById(9999L);
    }

    @Test
    void deleteSupplierContact_exists_logicalDelete() {
        when(supplierContactMapper.selectById(CONTACT_ID))
                .thenReturn(buildContact(CONTACT_ID, SUPPLIER_ID, NAME, 0));

        supplierContactService.deleteSupplierContact(CONTACT_ID);

        verify(supplierContactMapper).deleteById(CONTACT_ID);
    }

    /**
     * VO Bean Validation 约束测试：等价验证 Controller 层 @Valid 在
     * create/update（supplierId、name 必填、email 格式）与 page（supplierId 必填）上的拦截行为，
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
        void saveReqVO_supplierIdMissing_invalid() {
            SupplierContactSaveReqVO req = new SupplierContactSaveReqVO();
            req.setName(NAME);

            Set<String> invalid = invalidProperties(req);
            assertTrue(invalid.contains("supplierId"), "supplierId 缺失应校验失败");
        }

        @Test
        void saveReqVO_nameBlank_invalid() {
            SupplierContactSaveReqVO req = new SupplierContactSaveReqVO();
            req.setSupplierId(SUPPLIER_ID);
            req.setName("  ");

            Set<String> invalid = invalidProperties(req);
            assertTrue(invalid.contains("name"), "name 空白应校验失败");
        }

        @Test
        void saveReqVO_emailMalformed_invalid() {
            SupplierContactSaveReqVO req = new SupplierContactSaveReqVO();
            req.setSupplierId(SUPPLIER_ID);
            req.setName(NAME);
            req.setEmail("not-an-email");

            Set<String> invalid = invalidProperties(req);
            assertTrue(invalid.contains("email"), "邮箱格式非法应校验失败");
        }

        @Test
        void saveReqVO_allRequiredPresent_valid() {
            SupplierContactSaveReqVO req = new SupplierContactSaveReqVO();
            req.setSupplierId(SUPPLIER_ID);
            req.setName(NAME);
            req.setEmail("zhangsan@example.com");

            Set<ConstraintViolation<SupplierContactSaveReqVO>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "必填齐全且邮箱合法时应通过校验");
        }

        @Test
        void pageReqVO_supplierIdMissing_invalid() {
            SupplierContactPageReqVO req = new SupplierContactPageReqVO();

            Set<String> invalid = invalidProperties(req);
            assertTrue(invalid.contains("supplierId"), "GET /page 缺少 supplierId 应校验失败");
        }

        @Test
        void pageReqVO_supplierIdPresent_valid() {
            SupplierContactPageReqVO req = new SupplierContactPageReqVO();
            req.setSupplierId(SUPPLIER_ID);

            Set<ConstraintViolation<SupplierContactPageReqVO>> violations = validator.validate(req);
            assertTrue(violations.isEmpty(), "传入 supplierId 时分页参数应通过校验");
        }

    }

}
