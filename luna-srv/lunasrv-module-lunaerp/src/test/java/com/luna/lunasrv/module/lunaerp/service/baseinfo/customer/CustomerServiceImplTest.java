package com.luna.lunasrv.module.lunaerp.service.baseinfo.customer;

import cn.idev.excel.FastExcelFactory;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerImportVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CustomerServiceImpl} 的纯 Mockito 单元测试（不依赖 Spring 上下文与数据库）。
 *
 * 覆盖关键业务语义：
 *  - createCustomer：shortName 为空时用 companyName 回填；status 缺省 ACTIVE；shortName 非空时保留。
 *  - getCustomer：客户不存在时抛 CUSTOMER_NOT_EXISTS（1_030_004_000）。
 *  - updateCustomer：不存在抛 1_030_004_000；shortName 为空时同样用 companyName 回填。
 *  - deleteCustomer/deleteCustomerList：不存在抛错；删除时先置 status=INACTIVE 再逻辑删除。
 *
 * 说明：Controller 层 MockMvc 集成测试需要完整的 security filter chain + 测试数据库，
 * 本仓 lunaerp 模块当前无该基础设施（参照 product 模块亦无测试），故先以纯单元测试覆盖核心逻辑。
 * DB 中 status=INACTIVE、deleted=1 的断言以验证 updateById(INACTIVE) + deleteByIds 调用顺序代替。
 *
 * @author luna
 */
class CustomerServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private static final String COMPANY_NAME = "杭州织造有限公司";
    private static final String SHORT_NAME = "杭州织造";
    private static final String CLIENT_TYPE = "FORMAL";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 模拟当前登录用户 99L，归属部门 200L
        LoginUser loginUser = new LoginUser();
        loginUser.setId(99L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));

        AdminUserRespDTO mockUser = new AdminUserRespDTO();
        mockUser.setId(99L);
        mockUser.setDeptId(200L);
        when(adminUserApi.getUser(99L)).thenReturn(mockUser);

        // 模拟 insert 时回填主键（@KeySequence 在真实环境由 MyBatis-Plus 填充）
        when(customerMapper.insert(any(CustomerDO.class))).thenAnswer(invocation -> {
            CustomerDO dobj = invocation.getArgument(0);
            dobj.setId(1024L);
            return 1;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCustomer_shortNameBlank_backfilledFromCompany() {
        // shortName 留空、status 留空 → 应回填 companyName 与 ACTIVE
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompanyName(COMPANY_NAME);
        req.setClientType(CLIENT_TYPE);

        Long id = customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();

        assertEquals(1024L, id);
        assertEquals(COMPANY_NAME, saved.getCompanyName());
        assertEquals(COMPANY_NAME, saved.getShortName(), "shortName 为空时应用 companyName 回填");
        assertEquals("ACTIVE", saved.getStatus(), "status 缺省应为 ACTIVE");
        assertEquals(CLIENT_TYPE, saved.getClientType());
        assertEquals(99L, saved.getSalespersonUserId(), "缺省业务员应为当前登录用户");
        assertEquals(200L, saved.getDeptId(), "缺省部门应为当前登录用户的部门");
    }

    @Test
    void createCustomer_shortNameProvided_preserved() {
        // shortName 非空 → 保留；status 显式 INACTIVE → 保留
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompanyName(COMPANY_NAME);
        req.setShortName(SHORT_NAME);
        req.setClientType(CLIENT_TYPE);
        req.setStatus("INACTIVE");

        customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();

        assertEquals(SHORT_NAME, saved.getShortName(), "shortName 非空时应保留原值");
        assertEquals("INACTIVE", saved.getStatus(), "status 显式传入时应保留");
    }

    @Test
    void createAndUpdateCustomer_withEnglishFields() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompanyName(COMPANY_NAME);
        req.setClientType(CLIENT_TYPE);
        req.setNameEn("Hangzhou Weaving Co., Ltd.");
        req.setAddressEn("123 Silk Road, Hangzhou, China");

        customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();
        assertEquals("Hangzhou Weaving Co., Ltd.", saved.getNameEn());
        assertEquals("123 Silk Road, Hangzhou, China", saved.getAddressEn());

        // Update
        CustomerDO existing = new CustomerDO();
        existing.setId(1024L);
        existing.setCompanyName(COMPANY_NAME);
        existing.setNameEn("Hangzhou Weaving Co., Ltd.");
        existing.setAddressEn("123 Silk Road, Hangzhou, China");
        existing.setSalespersonUserId(99L);
        existing.setDeptId(200L);
        when(customerMapper.selectById(1024L)).thenReturn(existing);

        CustomerSaveReqVO updateReq = new CustomerSaveReqVO();
        updateReq.setId(1024L);
        updateReq.setCompanyName(COMPANY_NAME);
        updateReq.setClientType(CLIENT_TYPE);
        updateReq.setNameEn("Hangzhou Textile Ltd.");
        updateReq.setAddressEn("456 Cotton Road, Hangzhou, China");
        updateReq.setSalespersonUserId(99L);

        customerService.updateCustomer(updateReq);

        ArgumentCaptor<CustomerDO> updateCaptor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(updateCaptor.capture());
        CustomerDO updated = updateCaptor.getValue();
        assertEquals("Hangzhou Textile Ltd.", updated.getNameEn());
        assertEquals("456 Cotton Road, Hangzhou, China", updated.getAddressEn());
    }

    @Test
    void getCustomer_notExists_throwsCode1030004000() {
        when(customerMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.getCustomer(9999L));
        assertEquals(1_030_004_000, ex.getCode());
    }

    @Test
    void updateCustomer_notExists_throwsCode1030004000() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(9999L);
        req.setCompanyName(COMPANY_NAME);
        req.setClientType(CLIENT_TYPE);
        when(customerMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.updateCustomer(req));
        assertEquals(1_030_004_000, ex.getCode());
        verify(customerMapper, never()).updateById(any(CustomerDO.class));
    }

    @Test
    void updateCustomer_shortNameBlank_backfilledFromCompany() {
        // 编辑时 shortName 留空 → 仍应用 companyName 回填
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(1024L);
        req.setCompanyName(COMPANY_NAME);
        req.setClientType(CLIENT_TYPE);
        when(customerMapper.selectById(1024L)).thenReturn(new CustomerDO());

        customerService.updateCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        CustomerDO saved = captor.getValue();
        assertEquals(1024L, saved.getId());
        assertEquals(COMPANY_NAME, saved.getShortName(), "update 时 shortName 为空也应用 companyName 回填");
    }

    @Test
    void updateCustomer_shortNameProvided_preserved() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(1024L);
        req.setCompanyName(COMPANY_NAME);
        req.setShortName(SHORT_NAME);
        req.setClientType(CLIENT_TYPE);
        when(customerMapper.selectById(1024L)).thenReturn(new CustomerDO());

        customerService.updateCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        assertEquals(SHORT_NAME, captor.getValue().getShortName(), "shortName 非空时应保留原值");
    }

    @Test
    void deleteCustomer_notExists_throwsCode1030004000() {
        when(customerMapper.selectById(9999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.deleteCustomer(9999L));
        assertEquals(1_030_004_000, ex.getCode());
        verify(customerMapper, never()).deleteByIds(anyList());
    }

    @Test
    void deleteCustomer_exists_setsInactiveThenLogicalDelete() {
        CustomerDO customer = new CustomerDO();
        customer.setId(1024L);
        customer.setStatus("ACTIVE");
        when(customerMapper.selectById(1024L)).thenReturn(customer);
        when(customerMapper.selectByIds(anyList())).thenReturn(Collections.singletonList(customer));

        customerService.deleteCustomer(1024L);

        // 1. 先置 status=INACTIVE
        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        CustomerDO statusUpdate = captor.getValue();
        assertEquals(1024L, statusUpdate.getId());
        assertEquals("INACTIVE", statusUpdate.getStatus(), "删除时应先将 status 置为 INACTIVE");
        // 2. 再逻辑删除（deleted=1）
        verify(customerMapper).deleteByIds(Collections.singletonList(1024L));
    }

    @Test
    void deleteCustomerList_batch_success() {
        CustomerDO c1 = new CustomerDO();
        c1.setId(1L);
        CustomerDO c2 = new CustomerDO();
        c2.setId(2L);
        CustomerDO c3 = new CustomerDO();
        c3.setId(3L);
        when(customerMapper.selectById(1L)).thenReturn(c1);
        when(customerMapper.selectById(2L)).thenReturn(c2);
        when(customerMapper.selectById(3L)).thenReturn(c3);

        customerService.deleteCustomerList(Arrays.asList(1L, 2L, 3L));

        // 每条先置 INACTIVE（逐条断言 status 与 id），再整体逻辑删除
        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper, times(3)).updateById(captor.capture());
        assertEquals(3, captor.getAllValues().size());
        for (CustomerDO statusUpdate : captor.getAllValues()) {
            assertEquals("INACTIVE", statusUpdate.getStatus(), "批量删除时每条都应先置 INACTIVE");
        }
        verify(customerMapper).deleteByIds(Arrays.asList(1L, 2L, 3L));
    }

    @Test
    void deleteCustomerList_withInvisibleId_throwsCode1030004000_andZeroDeletions() {
        // §5.4 / §9.2 用例 4：ids 中混入不可见 id → 整体失败且零删除、禁止部分成功
        CustomerDO c1 = new CustomerDO();
        c1.setId(1L);
        when(customerMapper.selectById(1L)).thenReturn(c1);
        when(customerMapper.selectById(2L)).thenReturn(null); // 不可见（或不存在）

        ServiceException ex = assertThrows(ServiceException.class,
                () -> customerService.deleteCustomerList(Arrays.asList(1L, 2L, 3L)));
        assertEquals(1_030_004_000, ex.getCode(), "任一 id 不可见必须整体抛 CUSTOMER_NOT_EXISTS");

        // 断言写操作 DB 零变更（不得部分置 INACTIVE，不得部分删除）
        verify(customerMapper, never()).updateById(any(CustomerDO.class));
        verify(customerMapper, never()).deleteByIds(anyList());
    }

    @Test
    void createCustomer_withInvoiceFieldsAndCreditDays() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompanyName(COMPANY_NAME);
        req.setClientType(CLIENT_TYPE);
        req.setInvoiceTitle("杭州织造开票抬头");
        req.setInvoiceAddress("杭州市滨江区江南大道88号");
        req.setBankName("招商银行杭州分行");
        req.setBankAccountNo("5719000123456");
        req.setCreditDays(45);

        customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();
        assertEquals("杭州织造开票抬头", saved.getInvoiceTitle());
        assertEquals("杭州市滨江区江南大道88号", saved.getInvoiceAddress());
        assertEquals("招商银行杭州分行", saved.getBankName());
        assertEquals("5719000123456", saved.getBankAccountNo());
        assertEquals(45, saved.getCreditDays());
    }

    @Test
    void createCustomer_invoiceTitleBlank_backfilledFromCompany() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompanyName("宁波纺织有限公司");
        req.setClientType(CLIENT_TYPE);

        customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();
        assertEquals("宁波纺织有限公司", saved.getInvoiceTitle(), "invoiceTitle 为空时应回填 companyName");
    }

    @Test
    void updateCustomer_withInvoiceFieldsAndCreditDays() {
        CustomerDO existing = new CustomerDO();
        existing.setId(1024L);
        existing.setCompanyName(COMPANY_NAME);
        existing.setSalespersonUserId(99L);
        existing.setDeptId(200L);
        when(customerMapper.selectById(1024L)).thenReturn(existing);

        CustomerSaveReqVO updateReq = new CustomerSaveReqVO();
        updateReq.setId(1024L);
        updateReq.setCompanyName(COMPANY_NAME);
        updateReq.setClientType(CLIENT_TYPE);
        updateReq.setInvoiceTitle("杭州织造集团开票抬头");
        updateReq.setInvoiceAddress("杭州市上城区钱江新城1号");
        updateReq.setBankName("建设银行西湖支行");
        updateReq.setBankAccountNo("33000123456789");
        updateReq.setCreditDays(60);

        customerService.updateCustomer(updateReq);

        ArgumentCaptor<CustomerDO> updateCaptor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(updateCaptor.capture());
        CustomerDO updated = updateCaptor.getValue();
        assertEquals("杭州织造集团开票抬头", updated.getInvoiceTitle());
        assertEquals("杭州市上城区钱江新城1号", updated.getInvoiceAddress());
        assertEquals("建设银行西湖支行", updated.getBankName());
        assertEquals("33000123456789", updated.getBankAccountNo());
        assertEquals(60, updated.getCreditDays());
    }

    private static final List<String> CUSTOMER_HEADERS = Arrays.asList(
            "企业名称", "简称", "客户类型", "行业", "规模", "来源", "合作状态",
            "统一社会信用代码", "英文名称", "英文地址", "开票抬头", "开票地址", "开户银行", "开户行账号", "备注"
    );

    private byte[] createRawExcelBytes(List<String> headers, List<List<Object>> dataList) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<List<String>> headList = headers.stream()
                .map(Collections::singletonList)
                .toList();
        FastExcelFactory.write(out)
                .head(headList)
                .sheet("客户导入")
                .doWrite(dataList);
        return out.toByteArray();
    }

    @Test
    void getImportTemplate_returnsSampleList() {
        List<CustomerImportVO> template = customerService.getImportTemplate();
        assertNotNull(template);
        assertEquals(1, template.size());
        assertEquals("示例客户A有限公司", template.get(0).getCompanyName());
        assertEquals("示例客户A有限公司", template.get(0).getInvoiceTitle());
        assertEquals("浙江省杭州市余杭区临平大道100号", template.get(0).getInvoiceAddress());
        assertEquals("中国工商银行杭州临平支行", template.get(0).getBankName());
        assertEquals("1202021109900012345", template.get(0).getBankAccountNo());
    }

    @Test
    void importCustomer_success_batchInsertsWithActiveStatusAndOwnerDept() throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("杭州丝绸集团", "杭州丝绸", "", "", "", "", "", "91330100MA27XXXX01", "Silk Co", "Addr 1", "杭州丝绸开票抬头", "杭州市西湖区", "中国工商银行杭州分行", "6222021234567890", "备注1"));
        rows.add(Arrays.asList("桐乡印染纺织", "", "", "", "", "", "", "91330100MA27XXXX02", "", "", "", "", "", "", ""));

        byte[] bytes = createRawExcelBytes(CUSTOMER_HEADERS, rows);
        MockMultipartFile file = new MockMultipartFile("file", "customers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        when(customerMapper.selectListByCompanyNames(any())).thenReturn(Collections.emptyList());

        ExcelImportResult result = customerService.importCustomer(file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSuccessCount());
        assertNull(result.getErrorStream());

        ArgumentCaptor<List<CustomerDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(customerMapper).insertBatch(captor.capture());
        List<CustomerDO> inserted = captor.getValue();
        assertEquals(2, inserted.size());

        CustomerDO c1 = inserted.get(0);
        assertEquals("杭州丝绸集团", c1.getCompanyName());
        assertEquals("杭州丝绸", c1.getShortName());
        assertEquals("ACTIVE", c1.getStatus());
        assertEquals(99L, c1.getSalespersonUserId());
        assertEquals(200L, c1.getDeptId());
        assertEquals("Silk Co", c1.getNameEn());
        assertEquals("Addr 1", c1.getAddressEn());
        assertEquals("杭州丝绸开票抬头", c1.getInvoiceTitle());
        assertEquals("杭州市西湖区", c1.getInvoiceAddress());
        assertEquals("中国工商银行杭州分行", c1.getBankName());
        assertEquals("6222021234567890", c1.getBankAccountNo());

        CustomerDO c2 = inserted.get(1);
        assertEquals("桐乡印染纺织", c2.getCompanyName());
        assertEquals("桐乡印染纺织", c2.getShortName(), "简称为空时应由企业名称自动回填");
        assertEquals("桐乡印染纺织", c2.getInvoiceTitle(), "开票抬头为空时应由企业名称自动回填");
        assertEquals("ACTIVE", c2.getStatus());
        assertEquals(99L, c2.getSalespersonUserId());
        assertEquals(200L, c2.getDeptId());
    }

    @Test
    void importCustomer_fileDuplicate_generatesErrorExcelWithoutDbWrite() throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("重复企业A", "简称A", "", "", "", "", "", "91330100MA27XXXX01", "", "", "", "", "", "", ""));
        rows.add(Arrays.asList("重复企业A", "简称B", "", "", "", "", "", "91330100MA27XXXX02", "", "", "", "", "", "", ""));

        byte[] bytes = createRawExcelBytes(CUSTOMER_HEADERS, rows);
        MockMultipartFile file = new MockMultipartFile("file", "duplicate.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        when(customerMapper.selectListByCompanyNames(any())).thenReturn(Collections.emptyList());

        ExcelImportResult result = customerService.importCustomer(file);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertNotNull(result.getErrorStream());
        verify(customerMapper, never()).insertBatch(anyList());
    }

    @Test
    void importCustomer_dbDuplicate_generatesErrorExcelWithoutDbWrite() throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("已有企业", "", "", "", "", "", "", "91330100MA27XXXX01", "", "", "", "", "", "", ""));

        byte[] bytes = createRawExcelBytes(CUSTOMER_HEADERS, rows);
        MockMultipartFile file = new MockMultipartFile("file", "db_duplicate.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        CustomerDO existing = new CustomerDO();
        existing.setCompanyName("已有企业");
        when(customerMapper.selectListByCompanyNames(any())).thenReturn(Collections.singletonList(existing));

        ExcelImportResult result = customerService.importCustomer(file);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertNotNull(result.getErrorStream());
        verify(customerMapper, never()).insertBatch(anyList());
    }

    @Test
    void importCustomer_creditCodeInvalid_generatesErrorExcelWithoutDbWrite() throws IOException {
        List<List<Object>> rows = new ArrayList<>();
        // 统一社会信用代码为 10 位，不满足 18 位格式
        rows.add(Arrays.asList("某企业", "", "", "", "", "", "", "1234567890", "", "", "", "", "", "", ""));

        byte[] bytes = createRawExcelBytes(CUSTOMER_HEADERS, rows);
        MockMultipartFile file = new MockMultipartFile("file", "invalid_code.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

        when(customerMapper.selectListByCompanyNames(any())).thenReturn(Collections.emptyList());

        ExcelImportResult result = customerService.importCustomer(file);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getSuccessCount());
        assertNotNull(result.getErrorStream());
        verify(customerMapper, never()).insertBatch(anyList());
    }

    @Test
    void createCustomer_defaultMarginRatePersistenceAndNullSafety() {
        // 1. 创建携带默认毛利率 18.5% 的客户
        CustomerSaveReqVO req1 = new CustomerSaveReqVO();
        req1.setCompanyName("优选面料客商");
        req1.setClientType("FORMAL");
        req1.setDefaultMarginRate(new BigDecimal("0.185000"));

        Long id1 = customerService.createCustomer(req1);
        assertEquals(1024L, id1);

        // 2. 创建不填默认毛利率的客户，存量/留空为 null，不被视为 0
        CustomerSaveReqVO req2 = new CustomerSaveReqVO();
        req2.setCompanyName("普通客户");
        req2.setClientType("POTENTIAL");
        req2.setDefaultMarginRate(null);

        customerService.createCustomer(req2);

        org.mockito.ArgumentCaptor<CustomerDO> captor = org.mockito.ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper, times(2)).insert(captor.capture());
        CustomerDO saved1 = captor.getAllValues().get(0);
        assertEquals(new BigDecimal("0.185000"), saved1.getDefaultMarginRate());
        CustomerDO saved2 = captor.getAllValues().get(1);
        assertNull(saved2.getDefaultMarginRate());
    }

}
