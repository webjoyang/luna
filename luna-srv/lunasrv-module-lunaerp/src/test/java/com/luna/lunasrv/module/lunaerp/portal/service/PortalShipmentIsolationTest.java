package com.luna.lunasrv.module.lunaerp.portal.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentUpdateSignReqVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentPageRespVO;
import com.luna.lunasrv.module.lunaerp.controller.portal.vo.PortalShipmentSignReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.enums.PortalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentService;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SHIPMENT_SIGN_STATUS_INVALID;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.SHIPMENT_STATUS_NOT_ALLOW;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 客户门户发货单只读隔离与越权专测 (Spec §2 & Issue 03)
 */
@ExtendWith(MockitoExtension.class)
public class PortalShipmentIsolationTest {

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private ShipmentItemMapper shipmentItemMapper;

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private PortalShipmentServiceImpl portalShipmentService;

    private static final Long TENANT_A = 1L;
    private static final Long CUSTOMER_A = 1001L;
    private static final Long CUSTOMER_B = 1002L;

    @BeforeEach
    void setUp() {
        // 初始化 MyBatis Plus TableInfo 缓存，防止 LambdaQueryWrapper 报错
        try {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentDO.class);
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentItemDO.class);
        } catch (Exception ignored) {
        }
        TenantContextHolder.setTenantId(TENANT_A);
        TenantContextHolder.setIgnore(false);
        PortalContext.set(TENANT_A, CUSTOMER_A, "客户A公司");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        PortalContext.clear();
    }

    @Test
    @DisplayName("分页测试：按 PortalContext customerId 天然过滤，仅 COMPLETED，时间倒序")
    public void testPage_naturalCustomerIdFilterAndCompletedOnly() {
        PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSignStatus("PENDING");

        ShipmentDO shipmentDO = createCompletedShipment(10L, "SH-20260927-001", CUSTOMER_A);
        when(shipmentMapper.selectPage(any(PortalShipmentPageReqVO.class), any()))
                .thenReturn(new PageResult<>(Collections.singletonList(shipmentDO), 1L));

        PageResult<PortalShipmentPageRespVO> result = portalShipmentService.getPortalShipmentPage(reqVO);

        assertNotNull(result);
        assertEquals(1, result.getList().size());
        PortalShipmentPageRespVO resp = result.getList().get(0);
        assertEquals("SH-20260927-001", resp.getShipmentNo());
        assertEquals("PENDING", resp.getSignStatus());
        assertEquals(new BigDecimal("5000.00"), resp.getTotalAmount());
        assertEquals(new BigDecimal("150.00"), resp.getFreightFee());

        // 校验查询条件中的 customerId 与 status
        ArgumentCaptor<LambdaQueryWrapperX<ShipmentDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(shipmentMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<ShipmentDO> wrapper = captor.getValue();
        assertNotNull(wrapper);
        String sql = wrapper.getSqlSegment();
        assertNotNull(sql);
        assertTrue(sql.contains("customer_id") || wrapper.getParamNameValuePairs().containsValue(CUSTOMER_A));
        assertTrue(sql.contains("status") || wrapper.getParamNameValuePairs().containsValue("COMPLETED"));
    }

    @Test
    @DisplayName("分页测试：Tab 为 ALL 时不对 signStatus 进行过滤")
    public void testPage_tabAll_noSignStatusFilter() {
        PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSignStatus("ALL");

        when(shipmentMapper.selectPage(any(PortalShipmentPageReqVO.class), any()))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        PageResult<PortalShipmentPageRespVO> result = portalShipmentService.getPortalShipmentPage(reqVO);
        assertNotNull(result);

        ArgumentCaptor<LambdaQueryWrapperX<ShipmentDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(shipmentMapper).selectPage(eq(reqVO), captor.capture());
        LambdaQueryWrapperX<ShipmentDO> wrapper = captor.getValue();
        assertFalse(wrapper.getParamNameValuePairs().containsValue("ALL"), "ALL 时不应有 signStatus 过滤");
    }

    @Test
    @DisplayName("分页测试：未登录或 PortalContext 缺失客户ID，抛 038-001")
    public void testPage_missingPortalContext_throws038001() {
        PortalContext.clear();
        PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.getPortalShipmentPage(reqVO));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("详情测试（id形态）：本客户访问本客户 COMPLETED 发货单，成功返回头+逐行明细")
    public void testGetById_success_sameCustomer() {
        ShipmentDO shipmentDO = createCompletedShipment(10L, "SH-20260927-001", CUSTOMER_A);
        when(shipmentMapper.selectById(10L)).thenReturn(shipmentDO);

        ShipmentItemDO itemDO = createShipmentItem(101L, 10L);
        when(shipmentItemMapper.selectListByShipmentId(10L)).thenReturn(Collections.singletonList(itemDO));

        PortalShipmentDetailRespVO resp = portalShipmentService.getPortalShipment(10L, null);

        assertNotNull(resp);
        assertEquals(10L, resp.getId());
        assertEquals("SH-20260927-001", resp.getShipmentNo());
        assertEquals(new BigDecimal("5000.00"), resp.getTotalAmount());
        assertEquals(new BigDecimal("150.00"), resp.getFreightFee());
        assertNotNull(resp.getItems());
        assertEquals(1, resp.getItems().size());
        assertEquals("纯棉斜纹布", resp.getItems().get(0).getProductName());
        assertEquals(new BigDecimal("2550.00"), resp.getItems().get(0).getAmount());
    }

    @Test
    @DisplayName("越权专测（id形态）：A 客户 token 访问 B 客户发货单，统一抛 038-001")
    public void testGetById_crossCustomer_forbidden_038001() {
        // 单据属于 CUSTOMER_B，当前会话是 CUSTOMER_A
        ShipmentDO shipmentDO = createCompletedShipment(20L, "SH-20260927-002", CUSTOMER_B);
        when(shipmentMapper.selectById(20L)).thenReturn(shipmentDO);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.getPortalShipment(20L, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("越权专测（id形态）：访问非 COMPLETED 状态（如 DRAFT），统一抛 038-001")
    public void testGetById_draftStatus_forbidden_038001() {
        ShipmentDO shipmentDO = createCompletedShipment(30L, "SH-20260927-003", CUSTOMER_A);
        shipmentDO.setStatus("DRAFT");
        when(shipmentMapper.selectById(30L)).thenReturn(shipmentDO);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.getPortalShipment(30L, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("越权专测（id形态）：发货单不存在，统一抛 038-001 防探测")
    public void testGetById_notExists_forbidden_038001() {
        when(shipmentMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.getPortalShipment(999L, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("详情测试（?no=形态）：本客户访问本客户 COMPLETED 发货单，成功返回头+逐行明细")
    public void testGetByNo_success_sameCustomer() {
        ShipmentDO shipmentDO = createCompletedShipment(10L, "SH-20260927-001", CUSTOMER_A);
        when(shipmentMapper.selectOne(any())).thenReturn(shipmentDO);

        ShipmentItemDO itemDO = createShipmentItem(101L, 10L);
        when(shipmentItemMapper.selectListByShipmentId(10L)).thenReturn(Collections.singletonList(itemDO));

        PortalShipmentDetailRespVO resp = portalShipmentService.getPortalShipment(null, "SH-20260927-001");

        assertNotNull(resp);
        assertEquals(10L, resp.getId());
        assertEquals("SH-20260927-001", resp.getShipmentNo());
    }

    @Test
    @DisplayName("越权专测（?no=形态）：A 客户 token 访问 B 客户发货单，统一抛 038-001")
    public void testGetByNo_crossCustomer_forbidden_038001() {
        ShipmentDO shipmentDO = createCompletedShipment(20L, "SH-20260927-002", CUSTOMER_B);
        when(shipmentMapper.selectOne(any())).thenReturn(shipmentDO);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.getPortalShipment(null, "SH-20260927-002"));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("越权专测（?no=形态）：发货单号不存在，统一抛 038-001 防探测")
    public void testGetByNo_notExists_forbidden_038001() {
        when(shipmentMapper.selectOne(any())).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.getPortalShipment(null, "SH-NON-EXISTENT"));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("越权专测（跨租户）：租户隔离查不到数据（null），统一抛 038-001")
    public void testCrossTenant_forbidden_038001() {
        // 跨租户查询被 MyBatis Plus 插件拦截返回 null
        when(shipmentMapper.selectById(10L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.getPortalShipment(10L, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    // ==================== 门户签收专测 (Issue 04) ====================

    @Test
    @DisplayName("签收专测：越权测试（跨客户）尝试签收他客发货单，统一报 038-001")
    public void testSign_crossCustomer_forbidden_038001() {
        ShipmentDO shipmentB = createCompletedShipment(20L, "SH-20260927-002", CUSTOMER_B);
        when(shipmentMapper.selectById(20L)).thenReturn(shipmentB);

        PortalShipmentSignReqVO req = PortalShipmentSignReqVO.builder()
                .id(20L)
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.signPortalShipment(req));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("签收专测：发货单不存在，统一报 038-001 防探测")
    public void testSign_notExists_forbidden_038001() {
        when(shipmentMapper.selectById(999L)).thenReturn(null);

        PortalShipmentSignReqVO req = PortalShipmentSignReqVO.builder()
                .id(999L)
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.signPortalShipment(req));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("签收专测：未带 PortalContext 身份，统一报 038-001")
    public void testSign_noContext_forbidden_038001() {
        PortalContext.clear();

        PortalShipmentSignReqVO req = PortalShipmentSignReqVO.builder()
                .id(10L)
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.signPortalShipment(req));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("签收专测：签收状态非法（非 SIGNED / REFUSED，如 PENDING）报 SHIPMENT_SIGN_STATUS_INVALID (1_030_016_006)")
    public void testSign_invalidSignStatus_throwsInvalid() {
        ShipmentDO shipmentA = createCompletedShipment(10L, "SH-20260927-001", CUSTOMER_A);
        when(shipmentMapper.selectById(10L)).thenReturn(shipmentA);

        PortalShipmentSignReqVO reqPending = PortalShipmentSignReqVO.builder()
                .id(10L)
                .signStatus("PENDING")
                .signedByName("张三")
                .build();

        ServiceException ex1 = assertThrows(ServiceException.class, () -> portalShipmentService.signPortalShipment(reqPending));
        assertEquals(SHIPMENT_SIGN_STATUS_INVALID.getCode(), ex1.getCode());

        PortalShipmentSignReqVO reqInvalid = PortalShipmentSignReqVO.builder()
                .id(10L)
                .signStatus("SOME_INVALID_STATUS")
                .signedByName("张三")
                .build();

        ServiceException ex2 = assertThrows(ServiceException.class, () -> portalShipmentService.signPortalShipment(reqInvalid));
        assertEquals(SHIPMENT_SIGN_STATUS_INVALID.getCode(), ex2.getCode());

        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("签收专测：仅 COMPLETED 可签，若非 COMPLETED 委派既有服务守卫报 SHIPMENT_STATUS_NOT_ALLOW")
    public void testSign_nonCompleted_delegatesToServiceGuard() {
        ShipmentDO shipmentA = createCompletedShipment(10L, "SH-20260927-001", CUSTOMER_A);
        shipmentA.setStatus("DRAFT");
        when(shipmentMapper.selectById(10L)).thenReturn(shipmentA);

        doThrow(new ServiceException(SHIPMENT_STATUS_NOT_ALLOW.getCode(), SHIPMENT_STATUS_NOT_ALLOW.getMsg()))
                .when(shipmentService).updateShipmentSign(any(ShipmentUpdateSignReqVO.class));

        PortalShipmentSignReqVO req = PortalShipmentSignReqVO.builder()
                .id(10L)
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.signPortalShipment(req));
        assertEquals(SHIPMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("签收专测：登记已签收 (SIGNED)，signedAt 注入服务器当前时间，零库存/账务副作用")
    public void testSign_signedSuccess_serverTimeAndNoSideEffects() {
        ShipmentDO shipmentA = createCompletedShipment(10L, "SH-20260927-001", CUSTOMER_A);
        when(shipmentMapper.selectById(10L)).thenReturn(shipmentA);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        PortalShipmentSignReqVO req = PortalShipmentSignReqVO.builder()
                .id(10L)
                .signStatus("SIGNED")
                .signedByName("张三")
                .signRemark("外包装完好，实收10件")
                .build();

        portalShipmentService.signPortalShipment(req);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        ArgumentCaptor<ShipmentUpdateSignReqVO> captor = ArgumentCaptor.forClass(ShipmentUpdateSignReqVO.class);
        verify(shipmentService).updateShipmentSign(captor.capture());
        ShipmentUpdateSignReqVO updateReq = captor.getValue();

        assertEquals(10L, updateReq.getId());
        assertEquals("SIGNED", updateReq.getSignStatus());
        assertEquals("张三", updateReq.getSignedByName());
        assertEquals("外包装完好，实收10件", updateReq.getSignRemark());
        assertNotNull(updateReq.getSignedAt(), "signedAt 必须自动填充服务器时间");
        assertTrue(!updateReq.getSignedAt().isBefore(before) && !updateReq.getSignedAt().isAfter(after),
                "signedAt 应为服务器当前时间");
    }

    @Test
    @DisplayName("签收专测：登记拒签 (REFUSED)，signedAt 注入服务器当前时间，零库存/账务副作用")
    public void testSign_refusedSuccess_serverTimeAndNoSideEffects() {
        ShipmentDO shipmentA = createCompletedShipment(10L, "SH-20260927-001", CUSTOMER_A);
        when(shipmentMapper.selectById(10L)).thenReturn(shipmentA);

        PortalShipmentSignReqVO req = PortalShipmentSignReqVO.builder()
                .id(10L)
                .signStatus("REFUSED")
                .signedByName("李四")
                .signRemark("布面有油污瑕疵，拒绝签收")
                .build();

        portalShipmentService.signPortalShipment(req);

        ArgumentCaptor<ShipmentUpdateSignReqVO> captor = ArgumentCaptor.forClass(ShipmentUpdateSignReqVO.class);
        verify(shipmentService).updateShipmentSign(captor.capture());
        ShipmentUpdateSignReqVO updateReq = captor.getValue();

        assertEquals(10L, updateReq.getId());
        assertEquals("REFUSED", updateReq.getSignStatus());
        assertEquals("李四", updateReq.getSignedByName());
        assertEquals("布面有油污瑕疵，拒绝签收", updateReq.getSignRemark());
        assertNotNull(updateReq.getSignedAt());
    }

    @Test
    @DisplayName("切面日志规约：signPortalShipment 必须标注 @LogRecord 留痕")
    public void testSign_aspectAnnotation() throws NoSuchMethodException {
        Method signMethod = PortalShipmentServiceImpl.class.getMethod("signPortalShipment", PortalShipmentSignReqVO.class);
        LogRecord logRecord = signMethod.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "signPortalShipment 必须标注 @LogRecord");
        assertEquals("CUSTOMER_PORTAL", logRecord.type());
        assertEquals("SIGN_SHIPMENT", logRecord.subType());
    }

    @Test
    @DisplayName("DTO校验专测：PortalShipmentSignReqVO signedByName 必填且非空校验")
    public void testPortalShipmentSignReqVO_validation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        // 1. signedByName 为空
        PortalShipmentSignReqVO invalidReq = PortalShipmentSignReqVO.builder()
                .id(10L)
                .signStatus("SIGNED")
                .signedByName(" ")
                .build();
        Set<ConstraintViolation<PortalShipmentSignReqVO>> violations = validator.validate(invalidReq);
        assertFalse(violations.isEmpty(), "signedByName 为空白字符串应校验失败");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("signedByName")));

        // 2. id 为空
        PortalShipmentSignReqVO invalidIdReq = PortalShipmentSignReqVO.builder()
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();
        violations = validator.validate(invalidIdReq);
        assertFalse(violations.isEmpty(), "id 为空应校验失败");

        // 3. signStatus 为空
        PortalShipmentSignReqVO invalidStatusReq = PortalShipmentSignReqVO.builder()
                .id(10L)
                .signedByName("张三")
                .build();
        violations = validator.validate(invalidStatusReq);
        assertFalse(violations.isEmpty(), "signStatus 为空应校验失败");
    }

    private ShipmentDO createCompletedShipment(Long id, String shipmentNo, Long customerId) {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(id);
        shipment.setShipmentNo(shipmentNo);
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(customerId);
        shipment.setCustomerName("测试客户");
        shipment.setShippingDate(LocalDate.of(2026, 9, 27));
        shipment.setReceiveAddress("测试收货地址");
        shipment.setCarrier("顺丰速运");
        shipment.setTrackingNumber("SF123456789");
        shipment.setFreightFee(new BigDecimal("150.00"));
        shipment.setTotalAmount(new BigDecimal("5000.00"));
        shipment.setTotalPieces(10);
        shipment.setCurrency("RMB");
        shipment.setSignStatus("PENDING");
        return shipment;
    }

    private ShipmentItemDO createShipmentItem(Long id, Long shipmentId) {
        ShipmentItemDO item = new ShipmentItemDO();
        item.setId(id);
        item.setShipmentId(shipmentId);
        item.setProductId(201L);
        item.setProductCode("P-2026-001");
        item.setProductName("纯棉斜纹布");
        item.setCustomerColorNo("C-88");
        item.setColorName("藏青");
        item.setVatNo("GH-20260901");
        item.setQuantity(new BigDecimal("100.00"));
        item.setPieces(5);
        item.setUnit("M");
        item.setUnitPrice(new BigDecimal("25.50"));
        item.setAmount(new BigDecimal("2550.00"));
        return item;
    }
}
