package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.customerrecon.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.customerrecon.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.ledger.LedgerEntryMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.CustomerStatementConfirmType;
import com.luna.lunasrv.module.lunaerp.enums.finance.CustomerStatementStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static com.luna.lunasrv.module.lunaerp.enums.PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

/**
 * 客户门户对账端点与客户隔离专测 (Customer Portal Isolation & Statement Endpoints Test)
 * 对应 Issue 08 验收标准：
 * 1. GET /portal/finance/statement/list：按 customerId 过滤，仅返回 SENT/CONFIRMED 状态
 * 2. GET /portal/finance/statement/{id}：含全部行，校验该单 customerId 与当前客户一致
 * 3. PUT /portal/finance/statement/confirm：SENT->CONFIRMED，记录 confirm_type=ONLINE_PORTAL / confirm_by_name / confirm_time / confirm_ip
 * 4. PUT /portal/finance/statement/reject：必填 reject_reason，SENT->REJECTED
 * 5. 客户隔离专测：A客户不可见/不可操作B客户单、不可遍历ID越权
 * 6. 门户确认与代客确认落同一状态字段与 confirm_type 字典 (2278)
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class CustomerStatementPortalIsolationTest {

    @Mock
    private CustomerStatementMapper customerStatementMapper;

    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Mock
    private LedgerEntryMapper ledgerEntryMapper;

    @InjectMocks
    private CustomerStatementServiceImpl customerStatementService;

    private MockedStatic<TenantContextHolder> tenantContextMock;

    private static final Long CUSTOMER_A_ID = 1001L;
    private static final Long CUSTOMER_B_ID = 2002L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContextHolder.class);
        tenantContextMock.when(TenantContextHolder::getTenantId).thenReturn(1L);
        tenantContextMock.when(TenantContextHolder::getRequiredTenantId).thenReturn(1L);

        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerStatementItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), LedgerEntryDO.class);
    }

    @AfterEach
    void tearDown() {
        PortalContext.clear();
        if (tenantContextMock != null) {
            tenantContextMock.close();
        }
    }

    @Test
    @DisplayName("隔离专测 - 门户列表查询：严格按 customerId 过滤，仅查 SENT/CONFIRMED 态，A 客户不可见 B 客户单")
    void testPortalList_isolation_onlyReturnsCurrentCustomerSentAndConfirmed() {
        CustomerStatementDO stmtA1 = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.SENT.getValue());
        CustomerStatementDO stmtA2 = createStatementDO(11L, CUSTOMER_A_ID, "CS20260011", CustomerStatementStatus.CONFIRMED.getValue());

        when(customerStatementMapper.selectList(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            LambdaQueryWrapperX<CustomerStatementDO> wrapper = invocation.getArgument(0);
            return List.of(stmtA1, stmtA2);
        });

        List<CustomerStatementRespVO> result = customerStatementService.getPortalCustomerStatementList(CUSTOMER_A_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("CS20260010", result.get(0).getStatementNo());
        assertEquals("CS20260011", result.get(1).getStatementNo());

        // 验证传递给 Mapper 的查询条件必带当前 customerId 与 SENT/CONFIRMED 状态限制
        ArgumentCaptor<LambdaQueryWrapperX<CustomerStatementDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(customerStatementMapper).selectList(captor.capture());
        LambdaQueryWrapperX<CustomerStatementDO> capturedWrapper = captor.getValue();
        assertNotNull(capturedWrapper);
        String sqlSegment = capturedWrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("customer_id ="), "查询条件必须包含 customer_id 等值过滤");
        assertTrue(sqlSegment.contains("status IN"), "查询条件必须包含 status IN 限制");
    }

    @Test
    @DisplayName("隔离专测 - 门户详情查询成功：归属客户一致时正常返回全部明细行")
    void testPortalGet_success_returnsDetailWithAllLines() {
        CustomerStatementDO stmt = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(10L)).thenReturn(stmt);

        CustomerStatementItemDO item1 = new CustomerStatementItemDO();
        item1.setId(101L);
        item1.setStatementId(10L);
        item1.setLineNo(1);
        item1.setLedgerEntryId(501L);
        item1.setDebitAmount(BigDecimal.valueOf(100));
        item1.setCreditAmount(BigDecimal.ZERO);
        when(customerStatementItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item1));

        CustomerStatementDetailRespVO detail = customerStatementService.getPortalCustomerStatement(10L, CUSTOMER_A_ID);

        assertNotNull(detail);
        assertEquals(10L, detail.getId());
        assertEquals(CUSTOMER_A_ID, detail.getCustomerId());
        assertEquals(1, detail.getItems().size());
        assertEquals(101L, detail.getItems().get(0).getId());
    }

    @Test
    @DisplayName("隔离专测 - 门户详情查询横向越权拦截：A 客户尝试访问 B 客户的对账单报 STATEMENT_CUSTOMER_MISMATCH")
    void testPortalGet_isolation_crossCustomerMismatchThrowsException() {
        // 单据归属于客户 B
        CustomerStatementDO stmtB = createStatementDO(20L, CUSTOMER_B_ID, "CS20260020", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(20L)).thenReturn(stmtB);

        // 客户 A 尝试查看客户 B 的单据
        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatement(20L, CUSTOMER_A_ID));

        assertEquals(FinanceErrorCodeConstants.STATEMENT_CUSTOMER_MISMATCH.getCode(), ex.getCode());
        // 绝不加载任何明细行，杜绝信息泄漏
        verify(customerStatementItemMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("隔离专测 - 门户详情遍历 ID 越权防护：对账单不存在或已删除报 STATEMENT_NOT_EXISTS")
    void testPortalGet_isolation_idTraversalNotFoundThrowsException() {
        when(customerStatementMapper.selectById(99999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatement(99999L, CUSTOMER_A_ID));

        assertEquals(FinanceErrorCodeConstants.STATEMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("隔离专测 - 门户详情内部草稿防护：属于客户自身但处于 DRAFT/VOID 态报 STATEMENT_STATUS_NOT_ALLOW")
    void testPortalGet_isolation_draftOrVoidStatementDeniedInPortal() {
        CustomerStatementDO draftStmt = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.DRAFT.getValue());
        when(customerStatementMapper.selectById(10L)).thenReturn(draftStmt);

        ServiceException ex1 = assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatement(10L, CUSTOMER_A_ID));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex1.getCode());

        CustomerStatementDO voidStmt = createStatementDO(11L, CUSTOMER_A_ID, "CS20260011", CustomerStatementStatus.VOID.getValue());
        when(customerStatementMapper.selectById(11L)).thenReturn(voidStmt);

        ServiceException ex2 = assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatement(11L, CUSTOMER_A_ID));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("隔离专测 - 门户在线确认成功：SENT->CONFIRMED，落库 ONLINE_PORTAL、确认人姓名、时间与IP")
    void testPortalConfirm_success_writesOnlinePortalAndAuditFields() {
        CustomerStatementDO stmt = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(10L)).thenReturn(stmt);
        when(customerStatementMapper.update(any(CustomerStatementDO.class), any(LambdaQueryWrapper.class))).thenReturn(1);

        CustomerStatementPortalConfirmReqVO reqVO = new CustomerStatementPortalConfirmReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(CUSTOMER_A_ID);
        reqVO.setConfirmByName("王采购经理");
        reqVO.setConfirmIp("114.218.32.10");

        customerStatementService.confirmPortalCustomerStatement(reqVO);

        ArgumentCaptor<CustomerStatementDO> updateCaptor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        ArgumentCaptor<LambdaQueryWrapperX<CustomerStatementDO>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(customerStatementMapper).update(updateCaptor.capture(), wrapperCaptor.capture());

        CustomerStatementDO updated = updateCaptor.getValue();
        assertEquals(CustomerStatementStatus.CONFIRMED.getValue(), updated.getStatus());
        assertEquals(CustomerStatementConfirmType.ONLINE_PORTAL.getValue(), updated.getConfirmType());
        assertEquals("王采购经理", updated.getConfirmByName());
        assertEquals("114.218.32.10", updated.getConfirmIp());
        assertNotNull(updated.getConfirmTime());
    }

    @Test
    @DisplayName("隔离专测 - 门户在线确认横向越权拦截：A 客户尝试确认 B 客户对账单被拒绝")
    void testPortalConfirm_isolation_crossCustomerMismatchThrowsException() {
        CustomerStatementDO stmtB = createStatementDO(20L, CUSTOMER_B_ID, "CS20260020", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(20L)).thenReturn(stmtB);

        CustomerStatementPortalConfirmReqVO reqVO = new CustomerStatementPortalConfirmReqVO();
        reqVO.setId(20L);
        reqVO.setCustomerId(CUSTOMER_A_ID); // 恶意传递自己的 customerId 但 id 指向 B 客户单据
        reqVO.setConfirmByName("黑客A");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.confirmPortalCustomerStatement(reqVO));

        assertEquals(FinanceErrorCodeConstants.STATEMENT_CUSTOMER_MISMATCH.getCode(), ex.getCode());
        verify(customerStatementMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("隔离专测 - 门户在线确认状态守卫：非 SENT 状态（如已被确认）拒绝重复操作")
    void testPortalConfirm_statusNotSent_throwsException() {
        CustomerStatementDO stmtConfirmed = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.CONFIRMED.getValue());
        when(customerStatementMapper.selectById(10L)).thenReturn(stmtConfirmed);

        CustomerStatementPortalConfirmReqVO reqVO = new CustomerStatementPortalConfirmReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(CUSTOMER_A_ID);
        reqVO.setConfirmByName("王采购");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.confirmPortalCustomerStatement(reqVO));

        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("隔离专测 - 门户在线确认并发安全：乐观条件更新冲突报 STATEMENT_STATUS_NOT_ALLOW")
    void testPortalConfirm_raceConditionUpdateFails_throwsException() {
        CustomerStatementDO stmt = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(10L)).thenReturn(stmt);
        when(customerStatementMapper.update(any(), any())).thenReturn(0); // 模拟并发抢先更新，受影响行数为0

        CustomerStatementPortalConfirmReqVO reqVO = new CustomerStatementPortalConfirmReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(CUSTOMER_A_ID);
        reqVO.setConfirmByName("王采购");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.confirmPortalCustomerStatement(reqVO));

        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("隔离专测 - 门户驳回成功：必填 rejectReason，SENT->REJECTED 状态流转与条件更新")
    void testPortalReject_success_writesRejectedAndReason() {
        CustomerStatementDO stmt = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(10L)).thenReturn(stmt);
        when(customerStatementMapper.update(any(CustomerStatementDO.class), any(LambdaQueryWrapper.class))).thenReturn(1);

        CustomerStatementPortalRejectReqVO reqVO = new CustomerStatementPortalRejectReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(CUSTOMER_A_ID);
        reqVO.setRejectReason("9月20日发货单金额与合同不符");

        customerStatementService.rejectPortalCustomerStatement(reqVO);

        ArgumentCaptor<CustomerStatementDO> updateCaptor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).update(updateCaptor.capture(), any());

        CustomerStatementDO updated = updateCaptor.getValue();
        assertEquals(CustomerStatementStatus.REJECTED.getValue(), updated.getStatus());
        assertEquals("9月20日发货单金额与合同不符", updated.getRejectReason());
    }

    @Test
    @DisplayName("隔离专测 - 门户驳回横向越权拦截：A 客户尝试驳回 B 客户单据被拒绝")
    void testPortalReject_isolation_crossCustomerMismatchThrowsException() {
        CustomerStatementDO stmtB = createStatementDO(20L, CUSTOMER_B_ID, "CS20260020", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(20L)).thenReturn(stmtB);

        CustomerStatementPortalRejectReqVO reqVO = new CustomerStatementPortalRejectReqVO();
        reqVO.setId(20L);
        reqVO.setCustomerId(CUSTOMER_A_ID);
        reqVO.setRejectReason("恶意驳回");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.rejectPortalCustomerStatement(reqVO));

        assertEquals(FinanceErrorCodeConstants.STATEMENT_CUSTOMER_MISMATCH.getCode(), ex.getCode());
        verify(customerStatementMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("隔离专测 - 门户确认与代客确认落同一状态字段与 confirm_type 字典校验")
    void testPortalConfirm_and_confirmOnBehalf_writeSameStatusAndConfirmTypeFields() {
        // 1. 验证字典枚举取值符合字典 2278 定义
        assertEquals("ONLINE_PORTAL", CustomerStatementConfirmType.ONLINE_PORTAL.getValue());
        assertEquals("ON_BEHALF", CustomerStatementConfirmType.ON_BEHALF.getValue());

        // 2. 验证门户在线确认经由 Service 落库 status=CONFIRMED, confirm_type=ONLINE_PORTAL
        CustomerStatementDO portalStmt = createStatementDO(101L, CUSTOMER_A_ID, "CS20261001-0001", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(101L)).thenReturn(portalStmt);
        when(customerStatementMapper.update(any(CustomerStatementDO.class), any())).thenReturn(1);

        CustomerStatementPortalConfirmReqVO portalReq = new CustomerStatementPortalConfirmReqVO();
        portalReq.setId(101L);
        portalReq.setCustomerId(CUSTOMER_A_ID);
        portalReq.setConfirmByName("门户张三");
        portalReq.setConfirmIp("192.168.1.100");

        customerStatementService.confirmPortalCustomerStatement(portalReq);

        ArgumentCaptor<CustomerStatementDO> portalCaptor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).update(portalCaptor.capture(), any());
        assertEquals(CustomerStatementStatus.CONFIRMED.getValue(), portalCaptor.getValue().getStatus());
        assertEquals(CustomerStatementConfirmType.ONLINE_PORTAL.getValue(), portalCaptor.getValue().getConfirmType());
        assertEquals("门户张三", portalCaptor.getValue().getConfirmByName());

        // 3. 验证内部代客确认经由 Service 同样落库 status=CONFIRMED, 但 confirm_type=ON_BEHALF
        reset(customerStatementMapper);
        CustomerStatementDO behalfStmt = createStatementDO(102L, CUSTOMER_A_ID, "CS20261001-0002", CustomerStatementStatus.SENT.getValue());
        when(customerStatementMapper.selectById(102L)).thenReturn(behalfStmt);
        when(customerStatementMapper.update(any(CustomerStatementDO.class), any())).thenReturn(1);

        CustomerStatementConfirmOnBehalfReqVO behalfReq = new CustomerStatementConfirmOnBehalfReqVO();
        behalfReq.setId(102L);
        behalfReq.setConfirmMethod("微信");
        behalfReq.setRemark("客户微信确认核对无误");

        customerStatementService.confirmOnBehalf(behalfReq);

        ArgumentCaptor<CustomerStatementDO> behalfCaptor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).update(behalfCaptor.capture(), any());
        assertEquals(CustomerStatementStatus.CONFIRMED.getValue(), behalfCaptor.getValue().getStatus());
        assertEquals(CustomerStatementConfirmType.ON_BEHALF.getValue(), behalfCaptor.getValue().getConfirmType());
        assertEquals("微信", behalfCaptor.getValue().getConfirmMethod());
    }

    // ==================== Issue 05: PortalContext 身份注入与 038-001 隔离专测 ====================

    @Test
    @DisplayName("PortalContext 隔离专测 - 详情查询跨客户越权拦截：A 客户 Token 访问 B 客户单据报 038-001")
    void testPortalGet_portalContext_crossCustomerMismatchThrows038001() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        CustomerStatementDO stmtB = createStatementDO(20L, CUSTOMER_B_ID, "CS20260020", CustomerStatementStatus.SENT.getValue());
        stmtB.setTenantId(1L);
        when(customerStatementMapper.selectById(20L)).thenReturn(stmtB);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatement(20L, null));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("PortalContext 隔离专测 - 详情查询跨租户越权拦截：当前租户 Token 访问其他租户单据报 038-001")
    void testPortalGet_portalContext_crossTenantMismatchThrows038001() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        CustomerStatementDO stmtOtherTenant = createStatementDO(30L, CUSTOMER_A_ID, "CS20260030", CustomerStatementStatus.SENT.getValue());
        stmtOtherTenant.setTenantId(2L);
        when(customerStatementMapper.selectById(30L)).thenReturn(stmtOtherTenant);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatement(30L, null));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("PortalContext 隔离专测 - 详情查询不存在/遍历拦截：单据不存在报 038-001")
    void testPortalGet_portalContext_notFoundThrows038001() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        when(customerStatementMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatement(999L, null));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("PortalContext 成功路径 - 按单号 ?no= 查询自身对账单详情成功")
    void testPortalGet_portalContext_byNo_success() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        CustomerStatementDO stmtA = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.SENT.getValue());
        stmtA.setTenantId(1L);
        when(customerStatementMapper.selectOne(any())).thenReturn(stmtA);

        CustomerStatementItemDO item = new CustomerStatementItemDO();
        item.setId(101L);
        item.setStatementId(10L);
        item.setLineNo(1);
        item.setDebitAmount(BigDecimal.valueOf(100));
        item.setCreditAmount(BigDecimal.ZERO);
        when(customerStatementItemMapper.selectList(any())).thenReturn(List.of(item));

        CustomerStatementDetailRespVO detail = customerStatementService.getPortalCustomerStatement(null, "CS20260010", null);
        assertNotNull(detail);
        assertEquals("CS20260010", detail.getStatementNo());
        assertEquals(1, detail.getItems().size());
    }

    @Test
    @DisplayName("PortalContext 隔离专测 - 在线确认跨客户越权拦截：A 客户 Token 确认 B 客户单据报 038-001")
    void testPortalConfirm_portalContext_crossCustomerMismatchThrows038001() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        CustomerStatementDO stmtB = createStatementDO(20L, CUSTOMER_B_ID, "CS20260020", CustomerStatementStatus.SENT.getValue());
        stmtB.setTenantId(1L);
        when(customerStatementMapper.selectById(20L)).thenReturn(stmtB);

        CustomerStatementPortalConfirmReqVO reqVO = new CustomerStatementPortalConfirmReqVO();
        reqVO.setId(20L);
        reqVO.setConfirmByName("张主管");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.confirmPortalCustomerStatement(reqVO));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("PortalContext 隔离专测 - 在线确认跨租户越权拦截：单据属于其他租户报 038-001")
    void testPortalConfirm_portalContext_crossTenantMismatchThrows038001() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        CustomerStatementDO stmtOther = createStatementDO(30L, CUSTOMER_A_ID, "CS20260030", CustomerStatementStatus.SENT.getValue());
        stmtOther.setTenantId(2L);
        when(customerStatementMapper.selectById(30L)).thenReturn(stmtOther);

        CustomerStatementPortalConfirmReqVO reqVO = new CustomerStatementPortalConfirmReqVO();
        reqVO.setId(30L);
        reqVO.setConfirmByName("张主管");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.confirmPortalCustomerStatement(reqVO));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("PortalContext 成功路径 - 在线确认成功并自动注入 PortalContext customerId 与客户端IP")
    void testPortalConfirm_portalContext_success() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        CustomerStatementDO stmtA = createStatementDO(10L, CUSTOMER_A_ID, "CS20260010", CustomerStatementStatus.SENT.getValue());
        stmtA.setTenantId(1L);
        when(customerStatementMapper.selectById(10L)).thenReturn(stmtA);
        when(customerStatementMapper.update(any(CustomerStatementDO.class), any())).thenReturn(1);

        CustomerStatementPortalConfirmReqVO reqVO = new CustomerStatementPortalConfirmReqVO();
        reqVO.setId(10L);
        reqVO.setConfirmByName("客户财务张总");
        reqVO.setConfirmIp("192.168.1.100");

        customerStatementService.confirmPortalCustomerStatement(reqVO);

        ArgumentCaptor<CustomerStatementDO> captor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).update(captor.capture(), any());
        assertEquals(CustomerStatementStatus.CONFIRMED.getValue(), captor.getValue().getStatus());
        assertEquals(CustomerStatementConfirmType.ONLINE_PORTAL.getValue(), captor.getValue().getConfirmType());
        assertEquals("客户财务张总", captor.getValue().getConfirmByName());
        assertEquals("192.168.1.100", captor.getValue().getConfirmIp());
    }

    @Test
    @DisplayName("PortalContext 隔离专测 - 驳回跨客户越权拦截：A 客户 Token 驳回 B 客户单据报 038-001")
    void testPortalReject_portalContext_crossCustomerMismatchThrows038001() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        CustomerStatementDO stmtB = createStatementDO(20L, CUSTOMER_B_ID, "CS20260020", CustomerStatementStatus.SENT.getValue());
        stmtB.setTenantId(1L);
        when(customerStatementMapper.selectById(20L)).thenReturn(stmtB);

        CustomerStatementPortalRejectReqVO reqVO = new CustomerStatementPortalRejectReqVO();
        reqVO.setId(20L);
        reqVO.setRejectReason("金额与我司账面不符");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.rejectPortalCustomerStatement(reqVO));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("PortalContext 隔离专测 - 驳回跨租户越权拦截：单据属于其他租户报 038-001")
    void testPortalReject_portalContext_crossTenantMismatchThrows038001() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");
        CustomerStatementDO stmtOther = createStatementDO(30L, CUSTOMER_A_ID, "CS20260030", CustomerStatementStatus.SENT.getValue());
        stmtOther.setTenantId(2L);
        when(customerStatementMapper.selectById(30L)).thenReturn(stmtOther);

        CustomerStatementPortalRejectReqVO reqVO = new CustomerStatementPortalRejectReqVO();
        reqVO.setId(30L);
        reqVO.setRejectReason("金额有异议");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.rejectPortalCustomerStatement(reqVO));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("PortalContext 隔离专测 - 列表参数冲突拦截：Token 与显式 customerId 不一致报 038-001")
    void testPortalList_portalContext_crossCustomerParamMismatchThrows038001() {
        PortalContext.set(1L, CUSTOMER_A_ID, "客户A");

        ServiceException ex = assertThrows(ServiceException.class, () ->
                customerStatementService.getPortalCustomerStatementList(CUSTOMER_B_ID));
        assertEquals(PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    private CustomerStatementDO createStatementDO(Long id, Long customerId, String statementNo, String status) {
        CustomerStatementDO statement = new CustomerStatementDO();
        statement.setId(id);
        statement.setCustomerId(customerId);
        statement.setCustomerName("测试客户" + customerId);
        statement.setStatementNo(statementNo);
        statement.setCurrency("RMB");
        statement.setStatus(status);
        statement.setPeriodStart(LocalDate.of(2026, 9, 1));
        statement.setPeriodEnd(LocalDate.of(2026, 9, 30));
        statement.setOpeningBalance(BigDecimal.ZERO);
        statement.setPeriodDebit(BigDecimal.valueOf(1000));
        statement.setPeriodCredit(BigDecimal.valueOf(500));
        statement.setClosingBalance(BigDecimal.valueOf(500));
        return statement;
    }

}
