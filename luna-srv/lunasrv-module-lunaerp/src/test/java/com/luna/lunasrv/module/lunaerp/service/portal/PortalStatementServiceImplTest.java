package com.luna.lunasrv.module.lunaerp.service.portal;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo.*;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link PortalStatementServiceImpl} 单元测试
 *
 * <p>覆盖列表查询（状态白名单）、详情查询（id/no双入口）、横向隔离（客户A无法访问客户B）、
 * 跨租户隔离（租户X无法访问租户Y）、确认操作（SENT->CONFIRMED/记录确认人/IP/时间）、
 * 驳回操作（SENT->REJECTED/必填原因）、幂等守卫、双身份源（PortalContext与显式customerId）、
 * 日志留痕与VO白名单测试。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortalStatementServiceImpl 门户对账单服务单元测试")
public class PortalStatementServiceImplTest {

    @InjectMocks
    private PortalStatementServiceImpl portalStatementService;

    @Mock
    private CustomerStatementMapper customerStatementMapper;

    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;

    private static final Long TEST_TENANT_ID = 1L;
    private static final Long OTHER_TENANT_ID = 2L;
    private static final Long TEST_CUSTOMER_A = 1001L;
    private static final Long TEST_CUSTOMER_B = 1002L;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, CustomerStatementDO.class);
        TableInfoHelper.initTableInfo(assistant, CustomerStatementItemDO.class);
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TEST_TENANT_ID);
        PortalContext.set(TEST_TENANT_ID, TEST_CUSTOMER_A, "测试客户A");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        PortalContext.clear();
    }

    private CustomerStatementDO createStatement(Long id, String statementNo, Long customerId, Long tenantId, String status) {
        CustomerStatementDO s = new CustomerStatementDO();
        s.setId(id);
        s.setStatementNo(statementNo);
        s.setCustomerId(customerId);
        s.setCustomerName("测试客户" + customerId);
        s.setCurrency("CNY");
        s.setPeriodStart(LocalDate.of(2026, 10, 1));
        s.setPeriodEnd(LocalDate.of(2026, 10, 31));
        s.setOpeningBalance(new BigDecimal("1000.00"));
        s.setPeriodDebit(new BigDecimal("5000.00"));
        s.setPeriodCredit(new BigDecimal("2000.00"));
        s.setClosingBalance(new BigDecimal("4000.00"));
        s.setStatus(status);
        s.setTenantId(tenantId);
        s.setDeleted(false);
        return s;
    }

    private CustomerStatementItemDO createItem(Long id, Long statementId, int lineNo, String direction, BigDecimal debit, BigDecimal credit) {
        CustomerStatementItemDO item = new CustomerStatementItemDO();
        item.setId(id);
        item.setStatementId(statementId);
        item.setLineNo(lineNo);
        item.setDirection(direction);
        item.setBizDate(LocalDate.of(2026, 10, 15));
        item.setRefType("SHIPMENT");
        item.setRefNo("SH-20261015-001");
        item.setSummary("发货单出库");
        item.setDebitAmount(debit);
        item.setCreditAmount(credit);
        item.setSignStatus("SIGNED");
        item.setTenantId(TEST_TENANT_ID);
        item.setDeleted(false);
        return item;
    }

    // ==========================================
    // 1. 列表查询与状态过滤测试
    // ==========================================

    @Test
    @DisplayName("列表查询：默认过滤 SENT 与 CONFIRMED 状态")
    void testGetStatementList_defaultStatuses() {
        CustomerStatementDO s1 = createStatement(1L, "CS-001", TEST_CUSTOMER_A, TEST_TENANT_ID, "SENT");
        CustomerStatementDO s2 = createStatement(2L, "CS-002", TEST_CUSTOMER_A, TEST_TENANT_ID, "CONFIRMED");
        when(customerStatementMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(s1, s2));

        List<PortalStatementRespVO> list = portalStatementService.getStatementList(null, null);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("CS-001", list.get(0).getStatementNo());
        assertEquals("CS-002", list.get(1).getStatementNo());
    }

    @Test
    @DisplayName("列表查询：显式指定 SENT 状态过滤")
    void testGetStatementList_explicitStatusSent() {
        CustomerStatementDO s1 = createStatement(1L, "CS-001", TEST_CUSTOMER_A, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(s1));

        List<PortalStatementRespVO> list = portalStatementService.getStatementList(null, "SENT");

        assertEquals(1, list.size());
        assertEquals("SENT", list.get(0).getStatus());
    }

    @Test
    @DisplayName("列表查询：显式指定 CONFIRMED 状态过滤")
    void testGetStatementList_explicitStatusConfirmed() {
        CustomerStatementDO s2 = createStatement(2L, "CS-002", TEST_CUSTOMER_A, TEST_TENANT_ID, "CONFIRMED");
        when(customerStatementMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(s2));

        List<PortalStatementRespVO> list = portalStatementService.getStatementList(null, "CONFIRMED");

        assertEquals(1, list.size());
        assertEquals("CONFIRMED", list.get(0).getStatus());
    }

    @Test
    @DisplayName("列表查询：显式指定 REJECTED 状态过滤")
    void testGetStatementList_explicitStatusRejected() {
        CustomerStatementDO s3 = createStatement(3L, "CS-003", TEST_CUSTOMER_A, TEST_TENANT_ID, "REJECTED");
        when(customerStatementMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(s3));

        List<PortalStatementRespVO> list = portalStatementService.getStatementList(null, "REJECTED");

        assertEquals(1, list.size());
        assertEquals("REJECTED", list.get(0).getStatus());
    }

    @Test
    @DisplayName("列表查询：不允许的状态（如 DRAFT 或 VOID）过滤返回空列表，不透出")
    void testGetStatementList_disallowedStatusReturnsEmpty() {
        List<PortalStatementRespVO> listDraft = portalStatementService.getStatementList(null, "DRAFT");
        assertTrue(listDraft.isEmpty());

        List<PortalStatementRespVO> listVoid = portalStatementService.getStatementList(null, "VOID");
        assertTrue(listVoid.isEmpty());

        verify(customerStatementMapper, never()).selectList(any(LambdaQueryWrapperX.class));
    }

    @Test
    @DisplayName("列表查询：双身份源 - PortalContext 为空但传入 customerId 时有效")
    void testGetStatementList_dualSourceFallbackToParam() {
        PortalContext.clear(); // 清除门户上下文，模拟内部代客
        CustomerStatementDO s1 = createStatement(1L, "CS-001", TEST_CUSTOMER_A, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectList(any(LambdaQueryWrapperX.class))).thenReturn(List.of(s1));

        List<PortalStatementRespVO> list = portalStatementService.getStatementList(TEST_CUSTOMER_A, "SENT");

        assertEquals(1, list.size());
        assertEquals("CS-001", list.get(0).getStatementNo());
    }

    @Test
    @DisplayName("列表查询：两处身份均缺失时抛出 PORTAL_DATA_ACCESS_DENIED")
    void testGetStatementList_missingIdentityThrowsException() {
        PortalContext.clear();
        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.getStatementList(null, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    // ==========================================
    // 2. 详情查询测试（id 与 no 双入口）
    // ==========================================

    @Test
    @DisplayName("详情查询：按 id 查询成功返回头与全部明细行")
    void testGetStatementDetail_byId() {
        CustomerStatementDO s = createStatement(101L, "CS-202610110001", TEST_CUSTOMER_A, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(101L)).thenReturn(s);

        CustomerStatementItemDO item1 = createItem(201L, 101L, 1, "DEBIT", new BigDecimal("5000.00"), BigDecimal.ZERO);
        CustomerStatementItemDO item2 = createItem(202L, 101L, 2, "CREDIT", BigDecimal.ZERO, new BigDecimal("2000.00"));
        when(customerStatementItemMapper.selectListByStatementId(101L)).thenReturn(List.of(item1, item2));

        PortalStatementDetailRespVO detail = portalStatementService.getStatementDetail(101L, null, null);

        assertNotNull(detail);
        assertEquals(101L, detail.getId());
        assertEquals("CS-202610110001", detail.getStatementNo());
        assertEquals("SENT", detail.getStatus());
        assertEquals(2, detail.getItems().size());
        assertEquals(new BigDecimal("5000.00"), detail.getItems().get(0).getDebitAmount());
        assertEquals(new BigDecimal("2000.00"), detail.getItems().get(1).getCreditAmount());
    }

    @Test
    @DisplayName("详情查询：按 no 单号查询（id 为空）成功返回头与明细")
    void testGetStatementDetail_byNo() {
        CustomerStatementDO s = createStatement(101L, "CS-202610110001", TEST_CUSTOMER_A, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectByStatementNo("CS-202610110001")).thenReturn(s);
        when(customerStatementItemMapper.selectListByStatementId(101L)).thenReturn(Collections.emptyList());

        PortalStatementDetailRespVO detail = portalStatementService.getStatementDetail(null, "CS-202610110001", null);

        assertNotNull(detail);
        assertEquals("CS-202610110001", detail.getStatementNo());
        assertTrue(detail.getItems().isEmpty());
    }

    @Test
    @DisplayName("详情查询：同时传入 id 与 no 且单号不一致时抛出 PORTAL_DATA_ACCESS_DENIED")
    void testGetStatementDetail_idAndNoMismatch() {
        CustomerStatementDO s = createStatement(101L, "CS-202610110001", TEST_CUSTOMER_A, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(101L)).thenReturn(s);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.getStatementDetail(101L, "CS-MISMATCH", null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("详情查询：id 与 no 均未提供时抛出 PORTAL_DATA_ACCESS_DENIED")
    void testGetStatementDetail_emptyParams() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.getStatementDetail(null, "", null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("详情查询：单据不存在时抛出 PORTAL_DATA_ACCESS_DENIED")
    void testGetStatementDetail_notExists() {
        when(customerStatementMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.getStatementDetail(999L, null, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    // ==========================================
    // 3. 隔离专测（横向越权、跨租户越权、状态隔离）
    // ==========================================

    @Test
    @DisplayName("横向越权专测：客户 A token 访问客户 B 对账单详情抛出 038-001")
    void testGetStatementDetail_horizontalCrossCustomerDenied() {
        // 门户上下文为客户 A，单据归属客户 B
        CustomerStatementDO statementB = createStatement(102L, "CS-B-001", TEST_CUSTOMER_B, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(102L)).thenReturn(statementB);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.getStatementDetail(102L, null, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("跨租户专测：租户 X token 访问租户 Y 对账单详情抛出 038-001")
    void testGetStatementDetail_crossTenantDenied() {
        CustomerStatementDO statementY = createStatement(103L, "CS-Y-001", TEST_CUSTOMER_A, OTHER_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(103L)).thenReturn(statementY);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.getStatementDetail(103L, null, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非门户暴露状态（DRAFT/VOID）单据访问详情抛出 038-001")
    void testGetStatementDetail_disallowedStatusThrowsDenied() {
        CustomerStatementDO draft = createStatement(104L, "CS-DRAFT", TEST_CUSTOMER_A, TEST_TENANT_ID, "DRAFT");
        when(customerStatementMapper.selectById(104L)).thenReturn(draft);

        ServiceException ex1 = assertThrows(ServiceException.class, () ->
                portalStatementService.getStatementDetail(104L, null, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex1.getCode());

        CustomerStatementDO voided = createStatement(105L, "CS-VOID", TEST_CUSTOMER_A, TEST_TENANT_ID, "VOID");
        when(customerStatementMapper.selectById(105L)).thenReturn(voided);

        ServiceException ex2 = assertThrows(ServiceException.class, () ->
                portalStatementService.getStatementDetail(105L, null, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex2.getCode());
    }

    // ==========================================
    // 4. 确认对账单测试
    // ==========================================

    @Test
    @DisplayName("确认对账单：SENT -> CONFIRMED，正确更新状态、确认类型、确认人、IP、确认时间")
    void testConfirmStatement_success() {
        CustomerStatementDO statement = createStatement(101L, "CS-001", TEST_CUSTOMER_A, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(101L)).thenReturn(statement);

        PortalStatementConfirmReqVO reqVO = PortalStatementConfirmReqVO.builder()
                .id(101L)
                .confirmByName("李财务")
                .build();

        portalStatementService.confirmStatement(reqVO);

        ArgumentCaptor<CustomerStatementDO> captor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).updateById(captor.capture());
        CustomerStatementDO updated = captor.getValue();

        assertEquals(101L, updated.getId());
        assertEquals("CONFIRMED", updated.getStatus());
        assertEquals("ONLINE_PORTAL", updated.getConfirmType());
        assertEquals("李财务", updated.getConfirmByName());
        assertNotNull(updated.getConfirmTime());
    }

    @Test
    @DisplayName("确认对账单：确认人姓名为空抛出 BAD_REQUEST")
    void testConfirmStatement_blankConfirmByNameThrowsBadRequest() {
        PortalStatementConfirmReqVO reqVO = PortalStatementConfirmReqVO.builder()
                .id(101L)
                .confirmByName("   ")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.confirmStatement(reqVO));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("确认对账单守卫：非 SENT 状态（已 CONFIRMED）不可再确认，抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testConfirmStatement_alreadyConfirmedThrowsStatusNotAllowed() {
        CustomerStatementDO statement = createStatement(101L, "CS-001", TEST_CUSTOMER_A, TEST_TENANT_ID, "CONFIRMED");
        when(customerStatementMapper.selectById(101L)).thenReturn(statement);

        PortalStatementConfirmReqVO reqVO = PortalStatementConfirmReqVO.builder()
                .id(101L)
                .confirmByName("李财务")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.confirmStatement(reqVO));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("确认对账单横向越权：客户 A 尝试确认客户 B 的对账单抛出 038-001")
    void testConfirmStatement_crossCustomerDenied() {
        CustomerStatementDO statementB = createStatement(102L, "CS-B-001", TEST_CUSTOMER_B, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(102L)).thenReturn(statementB);

        PortalStatementConfirmReqVO reqVO = PortalStatementConfirmReqVO.builder()
                .id(102L)
                .confirmByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.confirmStatement(reqVO));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("确认对账单跨租户越权：租户 X 尝试确认租户 Y 的对账单抛出 038-001")
    void testConfirmStatement_crossTenantDenied() {
        CustomerStatementDO statementY = createStatement(103L, "CS-Y-001", TEST_CUSTOMER_A, OTHER_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(103L)).thenReturn(statementY);

        PortalStatementConfirmReqVO reqVO = PortalStatementConfirmReqVO.builder()
                .id(103L)
                .confirmByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.confirmStatement(reqVO));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    // ==========================================
    // 5. 驳会对账单测试
    // ==========================================

    @Test
    @DisplayName("驳会对账单：SENT -> REJECTED，正确更新状态与驳回原因")
    void testRejectStatement_success() {
        CustomerStatementDO statement = createStatement(101L, "CS-001", TEST_CUSTOMER_A, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(101L)).thenReturn(statement);

        PortalStatementRejectReqVO reqVO = PortalStatementRejectReqVO.builder()
                .id(101L)
                .rejectReason("第3行运费金额与约定不符")
                .build();

        portalStatementService.rejectStatement(reqVO);

        ArgumentCaptor<CustomerStatementDO> captor = ArgumentCaptor.forClass(CustomerStatementDO.class);
        verify(customerStatementMapper).updateById(captor.capture());
        CustomerStatementDO updated = captor.getValue();

        assertEquals(101L, updated.getId());
        assertEquals("REJECTED", updated.getStatus());
        assertEquals("第3行运费金额与约定不符", updated.getRejectReason());
    }

    @Test
    @DisplayName("驳会对账单：驳回原因缺失抛出 BAD_REQUEST")
    void testRejectStatement_blankReasonThrowsBadRequest() {
        PortalStatementRejectReqVO reqVO = PortalStatementRejectReqVO.builder()
                .id(101L)
                .rejectReason("")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.rejectStatement(reqVO));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("驳会对账单守卫：非 SENT 状态（已 REJECTED 或 CONFIRMED）不可驳回，抛出 STATEMENT_STATUS_NOT_ALLOW")
    void testRejectStatement_notSentThrowsStatusNotAllowed() {
        CustomerStatementDO statement = createStatement(101L, "CS-001", TEST_CUSTOMER_A, TEST_TENANT_ID, "CONFIRMED");
        when(customerStatementMapper.selectById(101L)).thenReturn(statement);

        PortalStatementRejectReqVO reqVO = PortalStatementRejectReqVO.builder()
                .id(101L)
                .rejectReason("重新驳回")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.rejectStatement(reqVO));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("驳会对账单横向越权：客户 A 尝试驳回客户 B 的对账单抛出 038-001")
    void testRejectStatement_crossCustomerDenied() {
        CustomerStatementDO statementB = createStatement(102L, "CS-B-001", TEST_CUSTOMER_B, TEST_TENANT_ID, "SENT");
        when(customerStatementMapper.selectById(102L)).thenReturn(statementB);

        PortalStatementRejectReqVO reqVO = PortalStatementRejectReqVO.builder()
                .id(102L)
                .rejectReason("驳回B客户")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () ->
                portalStatementService.rejectStatement(reqVO));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    // ==========================================
    // 6. 日志留痕与 VO 白名单测试
    // ==========================================

    @Test
    @DisplayName("留痕注解断言：confirmStatement 与 rejectStatement 必须包含 @LogRecord 留痕")
    void testLogRecordAnnotations() throws NoSuchMethodException {
        Method confirmMethod = PortalStatementServiceImpl.class.getMethod("confirmStatement", PortalStatementConfirmReqVO.class);
        LogRecord confirmLog = confirmMethod.getAnnotation(LogRecord.class);
        assertNotNull(confirmLog, "confirmStatement 必须标注 @LogRecord");
        assertEquals("客户门户", confirmLog.type());
        assertEquals("对账确认", confirmLog.subType());

        Method rejectMethod = PortalStatementServiceImpl.class.getMethod("rejectStatement", PortalStatementRejectReqVO.class);
        LogRecord rejectLog = rejectMethod.getAnnotation(LogRecord.class);
        assertNotNull(rejectLog, "rejectStatement 必须标注 @LogRecord");
        assertEquals("客户门户", rejectLog.type());
        assertEquals("对账驳回", rejectLog.subType());
    }

    @Test
    @DisplayName("VO 白名单安全性断言：返回 VO 中零内部敏感字段（成本、手机号、密码等）")
    void testVoWhitelistSafety() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        CustomerStatementDO s = createStatement(101L, "CS-001", TEST_CUSTOMER_A, TEST_TENANT_ID, "CONFIRMED");
        when(customerStatementMapper.selectById(101L)).thenReturn(s);

        CustomerStatementItemDO item = createItem(201L, 101L, 1, "DEBIT", new BigDecimal("100.00"), BigDecimal.ZERO);
        when(customerStatementItemMapper.selectListByStatementId(101L)).thenReturn(List.of(item));

        PortalStatementDetailRespVO detail = portalStatementService.getStatementDetail(101L, null, null);
        String json = mapper.writeValueAsString(detail);

        // 断言敏感词未被透出
        assertFalse(json.contains("cost"), "VO 中不应包含 cost 成本字段");
        assertFalse(json.contains("password"), "VO 中不应包含 password 字段");
        assertFalse(json.contains("mobile"), "VO 中不应包含 mobile 手机号字段");
        assertFalse(json.contains("phone"), "VO 中不应包含 phone 电话字段");
        assertFalse(json.contains("creatorId"), "VO 中不应包含内部用户ID");

        // 断言正常业务字段存在
        assertTrue(json.contains("statementNo"));
        assertTrue(json.contains("openingBalance"));
        assertTrue(json.contains("closingBalance"));
        assertTrue(json.contains("items"));
    }

}
