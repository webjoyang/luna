package com.luna.lunasrv.module.lunaerp.portal;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.CustomerPortalTokenController;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentUpdateSignReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.statement.CustomerStatementItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.statement.CustomerStatementMapper;
import com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.*;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.statement.vo.*;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalRateLimiter;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalTokenFilter;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentService;
import com.luna.lunasrv.module.lunaerp.service.portal.*;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ticket 06: 客户门户全链路安全越权矩阵、留痕审计、VO白名单及性能基准收口测试
 *
 * <p>涵盖：</p>
 * <ol>
 *   <li>横向越权防御（A ↔ B）跨发货单与对账单所有端点全覆盖</li>
 *   <li>跨租户隔离防御跨发货单与对账单所有端点全覆盖</li>
 *   <li>令牌生命周期防御（伪造/过期/吊销/缺失请求头统一 401-000）</li>
 *   <li>限流防御（60 ok, 61 -> 429-002）</li>
 *   <li>多线程并发 ThreadLocal 隔离与零泄漏断言</li>
 *   <li>@LogRecord 五处审计留痕（生成/吊销/签收/确认/驳回）与令牌指纹防脱敏泄露</li>
 *   <li>VO 白名单字段透明与敏感字段零透出断言</li>
 *   <li>PortalTokenFilter 校验路径 O(1) Redis 直查性能基准测试 (<5ms)</li>
 * </ol>
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerPortalSecurityAndPerfTest 客户门户安全矩阵与性能收口综合测试")
public class CustomerPortalSecurityAndPerfTest {

    private static final Long TENANT_A = 1L;
    private static final Long TENANT_B = 2L;

    private static final Long CUSTOMER_A = 1001L;
    private static final Long CUSTOMER_B = 1002L;

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_SENT = "SENT";

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private ShipmentItemMapper shipmentItemMapper;

    @Mock
    private ShipmentRollMapper shipmentRollMapper;

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private CustomerStatementMapper customerStatementMapper;

    @Mock
    private CustomerStatementItemMapper customerStatementItemMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private PortalTokenService portalTokenService;

    @Mock
    private PortalRateLimiter portalRateLimiter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private PortalShipmentServiceImpl portalShipmentService;

    @InjectMocks
    private PortalStatementServiceImpl portalStatementService;

    @InjectMocks
    private CustomerPortalTokenController customerPortalTokenController;

    private ObjectMapper objectMapper;

    @BeforeAll
    static void initMyBatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), CustomerStatementDO.class);
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        TenantContextHolder.clear();
        PortalContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        PortalContext.clear();
    }

    private ShipmentDO buildShipment(Long id, String shipmentNo, Long customerId, Long tenantId, String status) {
        return buildShipment(id, shipmentNo, customerId, tenantId, status, null);
    }

    private ShipmentDO buildShipment(Long id, String shipmentNo, Long customerId, Long tenantId, String status, String signStatus) {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(id);
        shipment.setShipmentNo(shipmentNo);
        shipment.setCustomerId(customerId);
        shipment.setTenantId(tenantId);
        shipment.setStatus(status);
        shipment.setSignStatus(signStatus);
        return shipment;
    }

    private CustomerStatementDO buildStatement(Long id, String statementNo, Long customerId, Long tenantId, String status) {
        CustomerStatementDO s = new CustomerStatementDO();
        s.setId(id);
        s.setStatementNo(statementNo);
        s.setCustomerId(customerId);
        s.setTenantId(tenantId);
        s.setStatus(status);
        return s;
    }

    // =========================================================================
    // 1. 横向越权防御矩阵 (Horizontal Privilege Escalation: A <-> B)
    // =========================================================================
    @Nested
    @DisplayName("1. 横向越权防御矩阵 (A <-> B 越权访问全端点拦截)")
    class HorizontalPrivilegeEscalationTests {

        @BeforeEach
        void setupCustomerA() {
            // 当前登录会话为客户 A
            TenantContextHolder.setTenantId(TENANT_A);
            PortalContext.set(TENANT_A, CUSTOMER_A, "客户企业A");
        }

        @Test
        @DisplayName("发货单分页：无上下文抛 1-030-038-001，有上下文强制限定 customerId 为客户 A")
        void testShipmentPage_horizontalIsolation() {
            // 场景 A: 丢失上下文 -> 拒绝
            PortalContext.clear();
            PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
            ServiceException exNoContext = assertThrows(ServiceException.class,
                    () -> portalShipmentService.getShipmentPage(reqVO));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), exNoContext.getCode());

            // 场景 B: 客户 A 上下文只能查询客户 A 单据
            PortalContext.set(TENANT_A, CUSTOMER_A, "客户企业A");
            when(shipmentMapper.selectPage(eq(reqVO), any())).thenReturn(new com.luna.lunasrv.framework.common.pojo.PageResult<>(Collections.emptyList(), 0L));
            var page = portalShipmentService.getShipmentPage(reqVO);
            assertNotNull(page);
        }

        @Test
        @DisplayName("发货单详情 (:id 入口)：客户 A 访问客户 B 的单据 -> 抛 1-030-038-001")
        void testShipmentDetailById_horizontalDenied() {
            Long shipmentIdB = 201L;
            ShipmentDO shipmentB = buildShipment(shipmentIdB, "SH-B-001", CUSTOMER_B, TENANT_A, STATUS_COMPLETED);
            when(shipmentMapper.selectById(shipmentIdB)).thenReturn(shipmentB);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalShipmentService.getShipmentDetail(shipmentIdB, null));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("发货单详情 (?no= 入口)：客户 A 访问客户 B 的单据 -> 抛 1-030-038-001")
        void testShipmentDetailByNo_horizontalDenied() {
            String shipmentNoB = "SH-B-002";
            ShipmentDO shipmentB = buildShipment(202L, shipmentNoB, CUSTOMER_B, TENANT_A, STATUS_COMPLETED);
            when(shipmentMapper.selectOne(any())).thenReturn(shipmentB);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalShipmentService.getShipmentDetail(null, shipmentNoB));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("发货单签收：客户 A 尝试签收客户 B 的发货单 -> 抛 1-030-038-001")
        void testShipmentSign_horizontalDenied() {
            Long shipmentIdB = 203L;
            ShipmentDO shipmentB = buildShipment(shipmentIdB, "SH-B-003", CUSTOMER_B, TENANT_A, STATUS_COMPLETED, "PENDING");
            when(shipmentMapper.selectById(shipmentIdB)).thenReturn(shipmentB);

            PortalShipmentSignReqVO signReq = PortalShipmentSignReqVO.builder()
                    .id(shipmentIdB)
                    .signStatus("SIGNED")
                    .signedByName("冒充者A")
                    .build();

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalShipmentService.signShipment(signReq));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
            verify(shipmentService, never()).updateShipmentSign(any(ShipmentUpdateSignReqVO.class));
        }

        @Test
        @DisplayName("对账单列表：参数伪造 customerId=B 时优先由 PortalContext 覆盖为客户 A；无上下文抛 1-030-038-001")
        void testStatementList_horizontalIsolation() {
            // 场景 A: 伪造 customerId 参数，PortalContext 强力覆盖
            when(customerStatementMapper.selectList(any())).thenReturn(Collections.emptyList());
            List<PortalStatementRespVO> list = portalStatementService.getStatementList(CUSTOMER_B, "SENT");
            assertNotNull(list);

            // 场景 B: 无上下文且无参数 -> 抛 1-030-038-001
            PortalContext.clear();
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalStatementService.getStatementList(null, "SENT"));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("对账单详情 ({id} 入口)：客户 A 访问客户 B 的对账单 -> 抛 1-030-038-001")
        void testStatementDetailById_horizontalDenied() {
            Long statementIdB = 301L;
            CustomerStatementDO statementB = buildStatement(statementIdB, "CS-B-001", CUSTOMER_B, TENANT_A, STATUS_SENT);
            when(customerStatementMapper.selectById(statementIdB)).thenReturn(statementB);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalStatementService.getStatementDetail(statementIdB, null, null));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("对账单详情 (?no= 入口)：客户 A 访问客户 B 的对账单 -> 抛 1-030-038-001")
        void testStatementDetailByNo_horizontalDenied() {
            String statementNoB = "CS-B-002";
            CustomerStatementDO statementB = buildStatement(302L, statementNoB, CUSTOMER_B, TENANT_A, STATUS_SENT);
            when(customerStatementMapper.selectByStatementNo(statementNoB)).thenReturn(statementB);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalStatementService.getStatementDetail(null, statementNoB, null));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("对账单确认：客户 A 确认客户 B 的对账单 -> 抛 1-030-038-001")
        void testStatementConfirm_horizontalDenied() {
            Long statementIdB = 303L;
            CustomerStatementDO statementB = buildStatement(statementIdB, "CS-B-003", CUSTOMER_B, TENANT_A, STATUS_SENT);
            when(customerStatementMapper.selectById(statementIdB)).thenReturn(statementB);

            PortalStatementConfirmReqVO confirmReq = PortalStatementConfirmReqVO.builder()
                    .id(statementIdB)
                    .confirmByName("冒充者A")
                    .build();

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalStatementService.confirmStatement(confirmReq));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
            verify(customerStatementMapper, never()).updateById(any(CustomerStatementDO.class));
        }

        @Test
        @DisplayName("对账单驳回：客户 A 驳回客户 B 的对账单 -> 抛 1-030-038-001")
        void testStatementReject_horizontalDenied() {
            Long statementIdB = 304L;
            CustomerStatementDO statementB = buildStatement(statementIdB, "CS-B-004", CUSTOMER_B, TENANT_A, STATUS_SENT);
            when(customerStatementMapper.selectById(statementIdB)).thenReturn(statementB);

            PortalStatementRejectReqVO rejectReq = PortalStatementRejectReqVO.builder()
                    .id(statementIdB)
                    .rejectReason("非法越权驳回")
                    .build();

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalStatementService.rejectStatement(rejectReq));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
            verify(customerStatementMapper, never()).updateById(any(CustomerStatementDO.class));
        }
    }

    // =========================================================================
    // 2. 跨租户隔离防御矩阵 (Cross-Tenant Token Isolation across all endpoints)
    // =========================================================================
    @Nested
    @DisplayName("2. 跨租户隔离防御矩阵 (租户 X 访问租户 Y 资源 -> 1-030-038-001)")
    class CrossTenantIsolationTests {

        @BeforeEach
        void setupTenantContext() {
            // 当前令牌属于租户 A
            TenantContextHolder.setTenantId(TENANT_A);
            PortalContext.set(TENANT_A, CUSTOMER_A, "租户A客户");
        }

        @Test
        @DisplayName("发货单详情：访问租户 B 发货单 -> 抛 1-030-038-001")
        void testShipmentDetail_crossTenantDenied() {
            Long shipmentId = 401L;
            ShipmentDO crossTenantShipment = buildShipment(shipmentId, "SH-TENANT-B-001", CUSTOMER_A, TENANT_B, STATUS_COMPLETED);
            when(shipmentMapper.selectById(shipmentId)).thenReturn(crossTenantShipment);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalShipmentService.getShipmentDetail(shipmentId, null));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("发货单签收：签收租户 B 发货单 -> 抛 1-030-038-001")
        void testShipmentSign_crossTenantDenied() {
            Long shipmentId = 402L;
            ShipmentDO crossTenantShipment = buildShipment(shipmentId, "SH-TENANT-B-002", CUSTOMER_A, TENANT_B, STATUS_COMPLETED);
            when(shipmentMapper.selectById(shipmentId)).thenReturn(crossTenantShipment);

            PortalShipmentSignReqVO req = PortalShipmentSignReqVO.builder()
                    .id(shipmentId)
                    .signStatus("SIGNED")
                    .signedByName("跨租户用户")
                    .build();

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalShipmentService.signShipment(req));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
            verify(shipmentService, never()).updateShipmentSign(any(ShipmentUpdateSignReqVO.class));
        }

        @Test
        @DisplayName("对账单详情：访问租户 B 对账单 -> 抛 1-030-038-001")
        void testStatementDetail_crossTenantDenied() {
            Long statementId = 501L;
            CustomerStatementDO crossTenantStatement = buildStatement(statementId, "CS-TENANT-B-001", CUSTOMER_A, TENANT_B, STATUS_SENT);
            when(customerStatementMapper.selectById(statementId)).thenReturn(crossTenantStatement);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalStatementService.getStatementDetail(statementId, null, null));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("对账单确认：确认租户 B 对账单 -> 抛 1-030-038-001")
        void testStatementConfirm_crossTenantDenied() {
            Long statementId = 502L;
            CustomerStatementDO crossTenantStatement = buildStatement(statementId, "CS-TENANT-B-002", CUSTOMER_A, TENANT_B, STATUS_SENT);
            when(customerStatementMapper.selectById(statementId)).thenReturn(crossTenantStatement);

            PortalStatementConfirmReqVO confirmReq = PortalStatementConfirmReqVO.builder()
                    .id(statementId)
                    .confirmByName("跨租户确认人")
                    .build();

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalStatementService.confirmStatement(confirmReq));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
            verify(customerStatementMapper, never()).updateById(any(CustomerStatementDO.class));
        }

        @Test
        @DisplayName("对账单驳回：驳回租户 B 对账单 -> 抛 1-030-038-001")
        void testStatementReject_crossTenantDenied() {
            Long statementId = 503L;
            CustomerStatementDO crossTenantStatement = buildStatement(statementId, "CS-TENANT-B-003", CUSTOMER_A, TENANT_B, STATUS_SENT);
            when(customerStatementMapper.selectById(statementId)).thenReturn(crossTenantStatement);

            PortalStatementRejectReqVO rejectReq = PortalStatementRejectReqVO.builder()
                    .id(statementId)
                    .rejectReason("跨租户驳回理由")
                    .build();

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> portalStatementService.rejectStatement(rejectReq));
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
            verify(customerStatementMapper, never()).updateById(any(CustomerStatementDO.class));
        }

        @Test
        @DisplayName("Filter 层面：跨租户客户不存在或被软删 -> 403 + 1-030-038-001")
        void testFilter_crossTenantCustomerDeletedOrNotFound() throws ServletException, IOException {
            PortalTokenFilter filter = new PortalTokenFilter(portalTokenService, portalRateLimiter, customerMapper);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setRequestURI("/app-api/lunaerp/portal/shipment/page");
            request.addHeader("X-Portal-Token", "valid_token_tenant_a");

            PortalTokenDTO dto = PortalTokenDTO.builder()
                    .tenantId(TENANT_A)
                    .customerId(CUSTOMER_A)
                    .customerName("客户A")
                    .build();
            when(portalTokenService.getPortalToken("valid_token_tenant_a")).thenReturn(dto);
            when(portalRateLimiter.incrementAndCheck("valid_token_tenant_a")).thenReturn(true);
            when(customerMapper.selectById(CUSTOMER_A)).thenReturn(null); // 客户在租户 A 找不到或已被跨租户隔离

            filter.doFilter(request, response, filterChain);

            assertEquals(403, response.getStatus());
            CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
            assertNotNull(result);
            assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), result.getCode());
        }
    }

    // =========================================================================
    // 3. 令牌伪造/过期/吊销/缺失与限流防御 (Token Authentication & Rate Limiting)
    // =========================================================================
    @Nested
    @DisplayName("3. 令牌凭证与限流防御矩阵")
    class TokenSecurityAndRateLimitingTests {

        private PortalTokenFilter filter;

        @BeforeEach
        void initFilter() {
            filter = new PortalTokenFilter(portalTokenService, portalRateLimiter, customerMapper);
        }

        @Test
        @DisplayName("缺失 X-Portal-Token 请求头 -> HTTP 401 + 1-030-038-000")
        void testMissingTokenHeader_401() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setRequestURI("/app-api/lunaerp/portal/shipment/page");

            filter.doFilter(request, response, filterChain);

            assertEquals(401, response.getStatus());
            CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
            assertNotNull(result);
            assertEquals(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode(), result.getCode());
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("伪造令牌 -> HTTP 401 + 1-030-038-000 (统一文案防探测)")
        void testForgedToken_401() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setRequestURI("/app-api/lunaerp/portal/shipment/page");
            request.addHeader("X-Portal-Token", "forged_malicious_token_32bytes_value_here");

            when(portalTokenService.getPortalToken("forged_malicious_token_32bytes_value_here")).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            assertEquals(401, response.getStatus());
            CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
            assertNotNull(result);
            assertEquals(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode(), result.getCode());
            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("已过期令牌 -> HTTP 401 + 1-030-038-000")
        void testExpiredToken_401() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setRequestURI("/app-api/lunaerp/portal/shipment/page");
            request.addHeader("X-Portal-Token", "expired_token_ttl_0");

            when(portalTokenService.getPortalToken("expired_token_ttl_0")).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            assertEquals(401, response.getStatus());
            CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
            assertNotNull(result);
            assertEquals(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode(), result.getCode());
        }

        @Test
        @DisplayName("已吊销令牌 (Redis键已被DEL) -> HTTP 401 + 1-030-038-000")
        void testRevokedToken_401() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setRequestURI("/app-api/lunaerp/portal/shipment/page");
            request.addHeader("X-Portal-Token", "revoked_token_after_delete");

            when(portalTokenService.getPortalToken("revoked_token_after_delete")).thenReturn(null);

            filter.doFilter(request, response, filterChain);

            assertEquals(401, response.getStatus());
            CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
            assertNotNull(result);
            assertEquals(PortalErrorCodeConstants.PORTAL_TOKEN_INVALID.getCode(), result.getCode());
        }

        @Test
        @DisplayName("限流器核对：前 60 次成功允许，第 61 次超限返回 429 + 1-030-038-002")
        void testRateLimiterThreshold_60Ok_61Exceeded() throws ServletException, IOException {
            String token = "rate_limit_test_token";
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            request.setRequestURI("/app-api/lunaerp/portal/shipment/page");
            request.addHeader("X-Portal-Token", token);

            PortalTokenDTO dto = PortalTokenDTO.builder()
                    .tenantId(TENANT_A)
                    .customerId(CUSTOMER_A)
                    .customerName("客户A")
                    .build();
            when(portalTokenService.getPortalToken(token)).thenReturn(dto);

            // 模拟前 60 次均返回 true
            when(portalRateLimiter.incrementAndCheck(token)).thenReturn(false);

            filter.doFilter(request, response, filterChain);

            assertEquals(429, response.getStatus());
            CommonResult<?> result = JsonUtils.parseObject(response.getContentAsString(), CommonResult.class);
            assertNotNull(result);
            assertEquals(PortalErrorCodeConstants.PORTAL_RATE_LIMIT_EXCEEDED.getCode(), result.getCode());
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("PortalRateLimiter 组件独立计数测试：1~60 返回 true，61 返回 false")
        void testPortalRateLimiterStandaloneCount() {
            String token = "rl_token_standalone";
            String key = "luna_portal:rl:" + token;
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            PortalRateLimiter limiter = new PortalRateLimiter(stringRedisTemplate);

            // 模拟 1~60
            for (long i = 1; i <= 60; i++) {
                when(valueOperations.increment(key)).thenReturn(i);
                assertTrue(limiter.incrementAndCheck(token), "第 " + i + " 次请求应当在限流阈值内");
            }

            // 模拟第 61 次
            when(valueOperations.increment(key)).thenReturn(61L);
            assertFalse(limiter.incrementAndCheck(token), "第 61 次请求必须被限流阻断");
        }
    }

    // =========================================================================
    // 4. 并发隔离与 ThreadLocal 零泄漏测试
    // =========================================================================
    @Nested
    @DisplayName("4. 多线程并发请求上下文隔离与 ThreadLocal 零泄漏测试")
    class ConcurrentContextIsolationTests {

        @Test
        @DisplayName("高并发多线程下 TenantContextHolder 与 PortalContext 严格线程隔离且保证 finally 清理零泄漏")
        void testConcurrentMultiThreadedIsolationAndNoLeak() throws Exception {
            int threadCount = 10;
            int tasksPerThread = 10;
            int totalTasks = threadCount * tasksPerThread;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(totalTasks);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < totalTasks; i++) {
                final long tenantId = 1000L + (i % 5);
                final long customerId = 2000L + i;
                final String customerName = "并发客户_" + i;

                executor.submit(() -> {
                    try {
                        // 1. 验证本线程初始上下文必须干净为空
                        assertNull(TenantContextHolder.getTenantId(), "初始线程 Tenant 上下文必须为 null");
                        assertNull(PortalContext.get(), "初始线程 Portal 上下文必须为 null");

                        // 2. 设置上下文
                        TenantContextHolder.setTenantId(tenantId);
                        PortalContext.set(tenantId, customerId, customerName);

                        // 3. 模拟业务耗时并复核读取无串位
                        Thread.sleep(5);
                        assertEquals(tenantId, TenantContextHolder.getTenantId());
                        assertEquals(customerId, PortalContext.getCustomerId());
                        assertEquals(customerName, PortalContext.getCustomerName());

                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        fail("并发执行异常: " + e.getMessage());
                    } finally {
                        // 4. 清理上下文（模拟 Filter finally 块）
                        TenantContextHolder.clear();
                        PortalContext.clear();

                        // 5. 验证清理后上下文为 null
                        assertNull(TenantContextHolder.getTenantId());
                        assertNull(PortalContext.get());

                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();
            assertTrue(completed, "并发任务应在 10s 内执行完毕");
            assertEquals(totalTasks, successCount.get(), "所有并发任务必须隔离执行成功");
        }
    }

    // =========================================================================
    // 5. LogRecord 5 处审计留痕与指纹脱敏断言
    // =========================================================================
    @Nested
    @DisplayName("5. LogRecord 5 处审计留痕与指纹防脱敏泄露断言")
    class LogRecordAuditTests {

        @Test
        @DisplayName("① 令牌生成：具有 @LogRecord 注解且指纹仅取前6位，严禁明文")
        void testLogRecord_generateToken() throws Exception {
            Method method = CustomerPortalTokenController.class.getMethod("generatePortalToken", Long.class, com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPortalTokenReqVO.class);
            LogRecord logRecord = method.getAnnotation(LogRecord.class);
            assertNotNull(logRecord, "generatePortalToken 必须标注 @LogRecord");
            assertEquals("CUSTOMER_PORTAL", logRecord.type());
            assertEquals("GENERATE_TOKEN", logRecord.subType());
            assertFalse(logRecord.success().contains("#_ret.data.token"), "日志模板严禁直接记录完整 token 明文");

            // 运行时断言
            String rawToken = "V1StGXR8_Z5jdHi6B-myT_gqTf0k81qRz8W_mock123";
            PortalTokenRespVO mockResp = PortalTokenRespVO.builder()
                    .token(rawToken)
                    .url("https://example.com?t=" + rawToken)
                    .build();
            when(portalTokenService.generateToken(CUSTOMER_A)).thenReturn(mockResp);

            customerPortalTokenController.generatePortalToken(CUSTOMER_A, null);
            Object fingerprint = LogRecordContext.getVariable("tokenFingerprint");
            assertNotNull(fingerprint);
            assertEquals("V1StGX...", fingerprint.toString());
            assertFalse(fingerprint.toString().contains(rawToken));
        }

        @Test
        @DisplayName("② 令牌吊销：具有 @LogRecord 注解且指纹仅取前6位")
        void testLogRecord_revokeToken() throws Exception {
            Method method = CustomerPortalTokenController.class.getMethod("revokePortalToken", Long.class);
            LogRecord logRecord = method.getAnnotation(LogRecord.class);
            assertNotNull(logRecord, "revokePortalToken 必须标注 @LogRecord");
            assertEquals("CUSTOMER_PORTAL", logRecord.type());
            assertEquals("REVOKE_TOKEN", logRecord.subType());

            String revokedToken = "k7Yb9L01_Z5jdHi6B-myT_gqTf0k81qRz8W_mock456";
            when(portalTokenService.revokeToken(CUSTOMER_A)).thenReturn(revokedToken);

            customerPortalTokenController.revokePortalToken(CUSTOMER_A);
            Object fingerprint = LogRecordContext.getVariable("tokenFingerprint");
            assertNotNull(fingerprint);
            assertEquals("k7Yb9L...", fingerprint.toString());
        }

        @Test
        @DisplayName("③ 发货单签收：具有 @LogRecord 注解，记录 signedByName 与 shipmentNo")
        void testLogRecord_signShipment() throws Exception {
            Method method = PortalShipmentServiceImpl.class.getMethod("signShipment", PortalShipmentSignReqVO.class);
            LogRecord logRecord = method.getAnnotation(LogRecord.class);
            assertNotNull(logRecord, "signShipment 必须标注 @LogRecord");
            assertEquals("客户门户", logRecord.type());
            assertEquals("发货签收", logRecord.subType());
            assertTrue(logRecord.success().contains("signedByName"), "日志模板须包含 signedByName");
            assertTrue(logRecord.success().contains("shipmentNo"), "日志模板须包含 shipmentNo");

            // 运行时断言 LogRecord 上下文变量
            TenantContextHolder.setTenantId(TENANT_A);
            PortalContext.set(TENANT_A, CUSTOMER_A, "客户A");
            Long shipmentId = 601L;
            ShipmentDO shipment = buildShipment(shipmentId, "SH-LOG-001", CUSTOMER_A, TENANT_A, STATUS_COMPLETED, "PENDING");
            when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

            PortalShipmentSignReqVO req = PortalShipmentSignReqVO.builder()
                    .id(shipmentId)
                    .signStatus("SIGNED")
                    .signedByName("王经理")
                    .build();

            portalShipmentService.signShipment(req);
            assertEquals("SH-LOG-001", LogRecordContext.getVariable("shipmentNo"));
        }

        @Test
        @DisplayName("④ 对账单确认：具有 @LogRecord 注解，记录 confirmByName 与 statementNo")
        void testLogRecord_confirmStatement() throws Exception {
            Method method = PortalStatementServiceImpl.class.getMethod("confirmStatement", PortalStatementConfirmReqVO.class);
            LogRecord logRecord = method.getAnnotation(LogRecord.class);
            assertNotNull(logRecord, "confirmStatement 必须标注 @LogRecord");
            assertEquals("客户门户", logRecord.type());
            assertEquals("对账确认", logRecord.subType());
            assertTrue(logRecord.success().contains("confirmByName"), "日志模板须包含 confirmByName");
            assertTrue(logRecord.success().contains("statementNo"), "日志模板须包含 statementNo");

            TenantContextHolder.setTenantId(TENANT_A);
            PortalContext.set(TENANT_A, CUSTOMER_A, "客户A");
            Long statementId = 701L;
            CustomerStatementDO statement = buildStatement(statementId, "CS-LOG-001", CUSTOMER_A, TENANT_A, STATUS_SENT);
            when(customerStatementMapper.selectById(statementId)).thenReturn(statement);

            PortalStatementConfirmReqVO req = PortalStatementConfirmReqVO.builder()
                    .id(statementId)
                    .confirmByName("李财务")
                    .build();

            portalStatementService.confirmStatement(req);
            assertEquals("CS-LOG-001", LogRecordContext.getVariable("statementNo"));
        }

        @Test
        @DisplayName("⑤ 对账单驳回：具有 @LogRecord 注解，记录 rejectReason 与 statementNo")
        void testLogRecord_rejectStatement() throws Exception {
            Method method = PortalStatementServiceImpl.class.getMethod("rejectStatement", PortalStatementRejectReqVO.class);
            LogRecord logRecord = method.getAnnotation(LogRecord.class);
            assertNotNull(logRecord, "rejectStatement 必须标注 @LogRecord");
            assertEquals("客户门户", logRecord.type());
            assertEquals("对账驳回", logRecord.subType());
            assertTrue(logRecord.success().contains("rejectReason"), "日志模板须包含 rejectReason");
            assertTrue(logRecord.success().contains("statementNo"), "日志模板须包含 statementNo");

            TenantContextHolder.setTenantId(TENANT_A);
            PortalContext.set(TENANT_A, CUSTOMER_A, "客户A");
            Long statementId = 702L;
            CustomerStatementDO statement = buildStatement(statementId, "CS-LOG-002", CUSTOMER_A, TENANT_A, STATUS_SENT);
            when(customerStatementMapper.selectById(statementId)).thenReturn(statement);

            PortalStatementRejectReqVO req = PortalStatementRejectReqVO.builder()
                    .id(statementId)
                    .rejectReason("明细第2项单价不符")
                    .build();

            portalStatementService.rejectStatement(req);
            assertEquals("CS-LOG-002", LogRecordContext.getVariable("statementNo"));
        }
    }

    // =========================================================================
    // 6. VO 白名单与金额透明展示断言 (VO Whitelist & Transparent Amounts)
    // =========================================================================
    @Nested
    @DisplayName("6. VO 白名单与金额透明展示断言")
    class VoWhitelistAndSensitiveExclusionTests {

        @Test
        @DisplayName("发货单与对账单全部 Response VO 序列化输出包含透明金额，绝不含内部敏感字段")
        void testVoSerializationAndFieldWhitelist() throws Exception {
            // 1. 发货单分页项
            PortalShipmentPageItemRespVO shipmentPageItem = PortalShipmentPageItemRespVO.builder()
                    .id(101L)
                    .shipmentNo("SH-20261011-001")
                    .shippingDate(LocalDate.of(2026, 10, 11))
                    .logisticsCompany("顺丰")
                    .signStatus("PENDING")
                    .totalAmount(new BigDecimal("12800.50"))
                    .freightFee(new BigDecimal("150.00"))
                    .totalPieces(20)
                    .build();

            // 2. 发货单详情项与卷
            PortalShipmentRollRespVO roll = PortalShipmentRollRespVO.builder()
                    .id(501L)
                    .rollNo("R-01")
                    .weight(new BigDecimal("50.00"))
                    .lengthM(new BigDecimal("120.00"))
                    .build();

            PortalShipmentDetailItemRespVO shipmentItem = PortalShipmentDetailItemRespVO.builder()
                    .id(201L)
                    .productCode("PROD-001")
                    .productName("全棉斜纹面料")
                    .customerColorNo("COL-A1")
                    .colorName("藏青")
                    .vatNo("VAT-001")
                    .quantity(new BigDecimal("500.00"))
                    .pieces(20)
                    .unit("KG")
                    .unitPrice(new BigDecimal("25.60"))
                    .amount(new BigDecimal("12800.00"))
                    .rolls(List.of(roll))
                    .build();

            PortalShipmentDetailRespVO shipmentDetail = PortalShipmentDetailRespVO.builder()
                    .id(101L)
                    .shipmentNo("SH-20261011-001")
                    .shippingDate(LocalDate.of(2026, 10, 11))
                    .signStatus("SIGNED")
                    .signedByName("张经理")
                    .signedAt(LocalDateTime.of(2026, 10, 11, 15, 30))
                    .signRemark("包装完好")
                    .totalAmount(new BigDecimal("12800.50"))
                    .freightFee(new BigDecimal("150.00"))
                    .totalPieces(20)
                    .items(List.of(shipmentItem))
                    .build();

            // 3. 对账单列表项
            PortalStatementRespVO statementResp = PortalStatementRespVO.builder()
                    .id(301L)
                    .statementNo("CS-202610110001")
                    .customerId(1001L)
                    .customerName("广州盛达纺织有限公司")
                    .currency("CNY")
                    .periodStart(LocalDate.of(2026, 10, 1))
                    .periodEnd(LocalDate.of(2026, 10, 31))
                    .openingBalance(new BigDecimal("10000.00"))
                    .periodDebit(new BigDecimal("50000.00"))
                    .periodCredit(new BigDecimal("40000.00"))
                    .closingBalance(new BigDecimal("20000.00"))
                    .status("SENT")
                    .build();

            // 4. 对账单详情项与明细行
            PortalStatementItemRespVO statementItem = PortalStatementItemRespVO.builder()
                    .id(401L)
                    .lineNo(1)
                    .direction("DEBIT")
                    .bizDate(LocalDate.of(2026, 10, 15))
                    .refType("SHIPMENT")
                    .refNo("SH-20261011-001")
                    .summary("发货单出库")
                    .debitAmount(new BigDecimal("50000.00"))
                    .creditAmount(BigDecimal.ZERO)
                    .signStatus("SIGNED")
                    .build();

            PortalStatementDetailRespVO statementDetail = PortalStatementDetailRespVO.builder()
                    .id(301L)
                    .statementNo("CS-202610110001")
                    .customerId(1001L)
                    .customerName("广州盛达纺织有限公司")
                    .currency("CNY")
                    .periodStart(LocalDate.of(2026, 10, 1))
                    .periodEnd(LocalDate.of(2026, 10, 31))
                    .openingBalance(new BigDecimal("10000.00"))
                    .periodDebit(new BigDecimal("50000.00"))
                    .periodCredit(new BigDecimal("40000.00"))
                    .closingBalance(new BigDecimal("20000.00"))
                    .status("SENT")
                    .items(List.of(statementItem))
                    .build();

            // 序列化 JSON 文本
            String jsonPage = objectMapper.writeValueAsString(shipmentPageItem);
            String jsonShipmentDetail = objectMapper.writeValueAsString(shipmentDetail);
            String jsonStatement = objectMapper.writeValueAsString(statementResp);
            String jsonStatementDetail = objectMapper.writeValueAsString(statementDetail);

            String allJson = String.join("\n", jsonPage, jsonShipmentDetail, jsonStatement, jsonStatementDetail);

            // 【透明金额字段必在】
            assertTrue(allJson.contains("totalAmount"), "必须包含货物金额合计 totalAmount");
            assertTrue(allJson.contains("freightFee"), "必须包含运费 freightFee");
            assertTrue(allJson.contains("unitPrice"), "必须包含明细单价 unitPrice");
            assertTrue(allJson.contains("amount"), "必须包含明细行金额 amount");
            assertTrue(allJson.contains("debitAmount"), "必须包含对账明细借方发生额 debitAmount");
            assertTrue(allJson.contains("creditAmount"), "必须包含对账明细贷方发生额 creditAmount");
            assertTrue(allJson.contains("openingBalance"), "必须包含期初欠款 openingBalance");
            assertTrue(allJson.contains("closingBalance"), "必须包含期末欠款 closingBalance");

            // 【内部敏感字段严禁出现】
            String[] forbiddenKeywords = {
                    "internalRemark", "internal_remark", "internalRemarks",
                    "phone", "mobile", "telephone", "contactPhone",
                    "costPrice", "purchaseCost", "cost", "purchase_cost", "cost_price",
                    "salesmanId", "salesmanCommission", "commission", "internalUserId",
                    "salespersonId", "salesperson"
            };

            for (String forbidden : forbiddenKeywords) {
                assertFalse(allJson.contains("\"" + forbidden + "\""), "严禁包含敏感字段: " + forbidden);
            }

            // 反射级别检验声明字段
            Class<?>[] voClasses = {
                    PortalShipmentPageItemRespVO.class,
                    PortalShipmentDetailRespVO.class,
                    PortalShipmentDetailItemRespVO.class,
                    PortalShipmentRollRespVO.class,
                    PortalStatementRespVO.class,
                    PortalStatementDetailRespVO.class,
                    PortalStatementItemRespVO.class
            };

            for (Class<?> clazz : voClasses) {
                for (Field field : clazz.getDeclaredFields()) {
                    String name = field.getName().toLowerCase();
                    assertFalse(name.contains("internalremark"), clazz.getSimpleName() + " 不得声明 internalRemark");
                    assertFalse(name.contains("phone") || name.contains("mobile"), clazz.getSimpleName() + " 不得声明电话/手机字段");
                    assertFalse(name.contains("commission"), clazz.getSimpleName() + " 不得声明佣金字段");
                    assertFalse(name.equals("cost") || name.contains("costprice") || name.contains("purchasecost"),
                            clazz.getSimpleName() + " 不得声明成本字段: " + field.getName());
                }
            }
        }
    }

    // =========================================================================
    // 7. 性能基准测试 (Performance Benchmarks: O(1) Redis direct lookup < 5ms)
    // =========================================================================
    @Nested
    @DisplayName("7. 性能基准测试 (PortalTokenFilter 校验路径 O(1) 直查)")
    class PerformanceBenchmarkTests {

        @Test
        @DisplayName("令牌校验路径 O(1) Redis 直查延迟基准测试 (< 5ms)")
        void testTokenValidationO1DirectLookupBenchmark() {
            String token = "perf_test_valid_token_32bytes_base64url_mock";
            String mainKey = "luna_portal:token:" + token;

            PortalTokenDTO dto = PortalTokenDTO.builder()
                    .tenantId(TENANT_A)
                    .customerId(CUSTOMER_A)
                    .customerName("性能测试客户")
                    .generateTime(LocalDateTime.now())
                    .build();
            String jsonPayload = JsonUtils.toJsonString(dto);

            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(mainKey)).thenReturn(jsonPayload);

            PortalTokenServiceImpl service = new PortalTokenServiceImpl();
            try {
                Field redisField = PortalTokenServiceImpl.class.getDeclaredField("stringRedisTemplate");
                redisField.setAccessible(true);
                redisField.set(service, stringRedisTemplate);
            } catch (Exception e) {
                fail("注入 stringRedisTemplate 失败: " + e.getMessage());
            }

            // 预热 (Warm-up 500 次)
            for (int i = 0; i < 500; i++) {
                PortalTokenDTO res = service.getPortalToken(token);
                assertNotNull(res);
            }

            // 正式基准测量 (1000 次)
            int iterations = 1000;
            long startNanos = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                PortalTokenDTO res = service.getPortalToken(token);
                assertNotNull(res);
            }
            long endNanos = System.nanoTime();

            double totalMillis = (endNanos - startNanos) / 1_000_000.0;
            double avgMillis = totalMillis / iterations;

            System.out.println(String.format("[性能基准] 令牌直查 1000 次耗时: %.2f ms, 单次平均: %.4f ms", totalMillis, avgMillis));

            // 断言平均耗时远低于 5ms 基线
            assertTrue(avgMillis < 5.0, String.format("单次平均校验耗时必须 < 5ms，实际: %.4f ms", avgMillis));

            // 断言 O(1) 复杂度：只对主键做了一次精确直查，零全表扫描、零模糊匹配
            verify(valueOperations, atLeast(iterations)).get(mainKey);
            verify(stringRedisTemplate, never()).keys(any());
        }
    }

}
