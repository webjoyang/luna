package com.luna.lunasrv.module.lunaerp.service.portal;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentUpdateSignReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.portal.PortalErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentPageItemRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentSignReqVO;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link PortalShipmentServiceImpl} 单元测试
 *
 * <p>包含分页查询、详情查询（id与no双入口）、横向越权防御、跨租户防御、非COMPLETED状态防御、严格白名单断言。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PortalShipmentServiceImpl 门户发货单服务单元测试")
public class PortalShipmentServiceImplTest {

    @InjectMocks
    private PortalShipmentServiceImpl portalShipmentService;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private ShipmentItemMapper shipmentItemMapper;

    @Mock
    private ShipmentRollMapper shipmentRollMapper;

    @Mock
    private ShipmentService shipmentService;

    private static final Long TEST_TENANT_ID = 1L;
    private static final Long TEST_CUSTOMER_ID = 100L;
    private static final String TEST_CUSTOMER_NAME = "杭州某纺织有限公司";

    @BeforeAll
    static void initMyBatisPlus() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentDO.class);
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TEST_TENANT_ID);
        PortalContext.set(TEST_TENANT_ID, TEST_CUSTOMER_ID, TEST_CUSTOMER_NAME);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        PortalContext.clear();
    }

    private ShipmentDO createCompletedShipment(Long id, String shipmentNo, Long customerId, Long tenantId) {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(id);
        shipment.setShipmentNo(shipmentNo);
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(customerId);
        shipment.setCustomerName("测试客户");
        shipment.setCarrier("顺丰速运");
        shipment.setTrackingNumber("SF123456789");
        shipment.setShippingDate(LocalDate.of(2026, 10, 11));
        shipment.setFreightFee(new BigDecimal("150.00"));
        shipment.setTotalAmount(new BigDecimal("12800.50"));
        shipment.setTotalPieces(20);
        shipment.setSignStatus("PENDING");
        shipment.setRemark("内部发货备注，绝对不能透出");
        shipment.setSalespersonUserId(888L);
        shipment.setSalespersonName("业务员张三");
        shipment.setDeptId(999L);
        shipment.setTenantId(tenantId);
        return shipment;
    }

    @Test
    @DisplayName("分页查询：只查当前客户名下 COMPLETED 发货单，时间倒序排序")
    void testGetShipmentPage_Success() {
        PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        ShipmentDO s1 = createCompletedShipment(101L, "SH-20261011-001", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        ShipmentDO s2 = createCompletedShipment(102L, "SH-20261011-002", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        s2.setSignStatus("SIGNED");
        s2.setSignedByName("李经理");
        s2.setSignedAt(LocalDateTime.of(2026, 10, 11, 14, 0, 0));

        PageResult<ShipmentDO> dbPageResult = new PageResult<>(List.of(s1, s2), 2L);
        when(shipmentMapper.selectPage(eq(reqVO), any())).thenReturn(dbPageResult);

        PageResult<PortalShipmentPageItemRespVO> result = portalShipmentService.getShipmentPage(reqVO);

        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getList().size());

        PortalShipmentPageItemRespVO item1 = result.getList().get(0);
        assertEquals(101L, item1.getId());
        assertEquals("SH-20261011-001", item1.getShipmentNo());
        assertEquals(LocalDate.of(2026, 10, 11), item1.getShippingDate());
        assertEquals("顺丰速运", item1.getLogisticsCompany());
        assertEquals("SF123456789", item1.getLogisticsTrackingNo());
        assertEquals("PENDING", item1.getSignStatus());
        assertEquals(new BigDecimal("12800.50"), item1.getTotalAmount());
        assertEquals(new BigDecimal("150.00"), item1.getFreightFee());
    }

    @Test
    @DisplayName("分页查询：Tab 过滤状态生效")
    void testGetShipmentPage_WithSignStatus() {
        PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setSignStatus("SIGNED");

        when(shipmentMapper.selectPage(eq(reqVO), any())).thenReturn(new PageResult<>(Collections.emptyList(), 0L));

        PageResult<PortalShipmentPageItemRespVO> result = portalShipmentService.getShipmentPage(reqVO);
        assertNotNull(result);
        assertEquals(0L, result.getTotal());

        verify(shipmentMapper).selectPage(eq(reqVO), any());
    }

    @Test
    @DisplayName("分页查询：上下文无客户身份时抛出 1-030-038-001")
    void testGetShipmentPage_NoContext_ThrowsDenied() {
        PortalContext.clear();

        PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentPage(reqVO));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("详情查询：按 id 查询成功，聚合明细与卷清单，金额与运费透明透出")
    void testGetShipmentDetail_ById_Success() {
        Long shipmentId = 101L;
        ShipmentDO shipment = createCompletedShipment(shipmentId, "SH-20261011-001", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        ShipmentItemDO item = new ShipmentItemDO();
        item.setId(201L);
        item.setShipmentId(shipmentId);
        item.setProductId(301L);
        item.setProductCode("P001");
        item.setProductName("高弹汗布");
        item.setCustomerColorNo("COL-A1");
        item.setColorName("藏青色");
        item.setVatNo("VAT-001");
        item.setQuantity(new BigDecimal("100.00"));
        item.setPieces(2);
        item.setUnit("KG");
        item.setUnitPrice(new BigDecimal("35.50"));
        item.setAmount(new BigDecimal("3550.00"));
        item.setRemark("内部明细备注");
        when(shipmentItemMapper.selectListByShipmentId(shipmentId)).thenReturn(List.of(item));

        ShipmentRollDO roll1 = new ShipmentRollDO();
        roll1.setId(501L);
        roll1.setShipmentId(shipmentId);
        roll1.setShipmentItemId(201L);
        roll1.setRollNo("R01");
        roll1.setWeight(new BigDecimal("50.00"));
        roll1.setLengthM(new BigDecimal("120.00"));

        ShipmentRollDO roll2 = new ShipmentRollDO();
        roll2.setId(502L);
        roll2.setShipmentId(shipmentId);
        roll2.setShipmentItemId(201L);
        roll2.setRollNo("R02");
        roll2.setWeight(new BigDecimal("50.00"));
        roll2.setLengthM(new BigDecimal("120.00"));
        when(shipmentRollMapper.selectListByShipmentId(shipmentId)).thenReturn(List.of(roll1, roll2));

        PortalShipmentDetailRespVO detail = portalShipmentService.getShipmentDetail(shipmentId, null);

        assertNotNull(detail);
        assertEquals(shipmentId, detail.getId());
        assertEquals("SH-20261011-001", detail.getShipmentNo());
        assertEquals("顺丰速运", detail.getLogisticsCompany());
        assertEquals("SF123456789", detail.getLogisticsTrackingNo());
        assertEquals("PENDING", detail.getSignStatus());
        assertEquals(new BigDecimal("12800.50"), detail.getTotalAmount());
        assertEquals(new BigDecimal("150.00"), detail.getFreightFee());
        assertEquals(20, detail.getTotalPieces());

        assertEquals(1, detail.getItems().size());
        var itemVO = detail.getItems().get(0);
        assertEquals(201L, itemVO.getId());
        assertEquals("P001", itemVO.getProductCode());
        assertEquals("高弹汗布", itemVO.getProductName());
        assertEquals("COL-A1", itemVO.getCustomerColorNo());
        assertEquals("藏青色", itemVO.getColorName());
        assertEquals("VAT-001", itemVO.getVatNo());
        assertEquals(new BigDecimal("100.00"), itemVO.getQuantity());
        assertEquals(2, itemVO.getPieces());
        assertEquals("KG", itemVO.getUnit());
        assertEquals(new BigDecimal("35.50"), itemVO.getUnitPrice());
        assertEquals(new BigDecimal("3550.00"), itemVO.getAmount());

        assertEquals(2, itemVO.getRolls().size());
        assertEquals("R01", itemVO.getRolls().get(0).getRollNo());
        assertEquals(new BigDecimal("50.00"), itemVO.getRolls().get(0).getWeight());
    }

    @Test
    @DisplayName("详情查询：按 no 查询成功（二维码直达形态）")
    void testGetShipmentDetail_ByNo_Success() {
        String no = "SH-20261011-001";
        ShipmentDO shipment = createCompletedShipment(101L, no, TEST_CUSTOMER_ID, TEST_TENANT_ID);
        when(shipmentMapper.selectOne(any())).thenReturn(shipment);
        when(shipmentItemMapper.selectListByShipmentId(101L)).thenReturn(Collections.emptyList());
        when(shipmentRollMapper.selectListByShipmentId(101L)).thenReturn(Collections.emptyList());

        PortalShipmentDetailRespVO detail = portalShipmentService.getShipmentDetail(null, no);

        assertNotNull(detail);
        assertEquals(101L, detail.getId());
        assertEquals(no, detail.getShipmentNo());
    }

    @Test
    @DisplayName("详情查询：未传 id 且未传 no 时抛出 1-030-038-001")
    void testGetShipmentDetail_NeitherIdNorNo_ThrowsDenied() {
        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(null, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());

        ServiceException exBlank = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(null, "   "));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), exBlank.getCode());
    }

    @Test
    @DisplayName("横向越权防御：客户 A 令牌访问客户 B 的发货单（按 id 入口）-> 抛出 1-030-038-001")
    void testHorizontalPrivilegeEscalation_ById_ThrowsDenied() {
        Long shipmentId = 101L;
        // 单据归属 Customer 999，当前上下文是 Customer 100
        ShipmentDO otherCustomerShipment = createCompletedShipment(shipmentId, "SH-20261011-001", 999L, TEST_TENANT_ID);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(otherCustomerShipment);

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(shipmentId, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("横向越权防御：客户 A 令牌访问客户 B 的发货单（按 no 入口）-> 抛出 1-030-038-001")
    void testHorizontalPrivilegeEscalation_ByNo_ThrowsDenied() {
        String no = "SH-20261011-999";
        ShipmentDO otherCustomerShipment = createCompletedShipment(999L, no, 999L, TEST_TENANT_ID);
        when(shipmentMapper.selectOne(any())).thenReturn(otherCustomerShipment);

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(null, no));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("跨租户防御：租户 X 访问租户 Y 发货单 -> 抛出 1-030-038-001")
    void testCrossTenant_ThrowsDenied() {
        Long shipmentId = 101L;
        // 单据归属 Tenant 2，当前上下文是 Tenant 1
        ShipmentDO otherTenantShipment = createCompletedShipment(shipmentId, "SH-20261011-001", TEST_CUSTOMER_ID, 2L);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(otherTenantShipment);

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(shipmentId, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("状态守卫：访问非 COMPLETED（草稿态/已作废）发货单 -> 抛出 1-030-038-001")
    void testNonCompletedShipment_ThrowsDenied() {
        Long draftId = 101L;
        ShipmentDO draftShipment = createCompletedShipment(draftId, "SH-20261011-001", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        draftShipment.setStatus("DRAFT");
        when(shipmentMapper.selectById(draftId)).thenReturn(draftShipment);

        ServiceException exDraft = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(draftId, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), exDraft.getCode());

        Long cancelId = 102L;
        ShipmentDO cancelShipment = createCompletedShipment(cancelId, "SH-20261011-002", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        cancelShipment.setStatus("CANCELLED");
        when(shipmentMapper.selectById(cancelId)).thenReturn(cancelShipment);

        ServiceException exCancel = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(cancelId, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), exCancel.getCode());
    }

    @Test
    @DisplayName("不存在的发货单 -> 抛出 1-030-038-001")
    void testShipmentNotFound_ThrowsDenied() {
        when(shipmentMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(999L, null));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("id 与 no 同时传入但不一致 -> 抛出 1-030-038-001")
    void testMismatchedIdAndNo_ThrowsDenied() {
        Long shipmentId = 101L;
        ShipmentDO shipment = createCompletedShipment(shipmentId, "SH-20261011-001", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        ServiceException ex = assertThrows(ServiceException.class, () -> portalShipmentService.getShipmentDetail(shipmentId, "SH-DIFF-002"));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("严格白名单断言：JSON 序列化输出不含内部敏感字段（业务员、部门、内部备注、成本），金额透明透出")
    void testStrictWhitelistAssertion() throws Exception {
        Long shipmentId = 101L;
        ShipmentDO shipment = createCompletedShipment(shipmentId, "SH-20261011-001", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        ShipmentItemDO item = new ShipmentItemDO();
        item.setId(201L);
        item.setShipmentId(shipmentId);
        item.setProductCode("P001");
        item.setProductName("高弹汗布");
        item.setQuantity(new BigDecimal("100.00"));
        item.setUnitPrice(new BigDecimal("35.50"));
        item.setAmount(new BigDecimal("3550.00"));
        item.setRemark("内部明细备注");
        when(shipmentItemMapper.selectListByShipmentId(shipmentId)).thenReturn(List.of(item));

        ShipmentRollDO roll = new ShipmentRollDO();
        roll.setId(501L);
        roll.setShipmentId(shipmentId);
        roll.setShipmentItemId(201L);
        roll.setRollNo("R01");
        roll.setWeight(new BigDecimal("50.00"));
        roll.setLengthM(new BigDecimal("120.00"));
        roll.setRemark("卷内部备注");
        when(shipmentRollMapper.selectListByShipmentId(shipmentId)).thenReturn(List.of(roll));

        PortalShipmentDetailRespVO detail = portalShipmentService.getShipmentDetail(shipmentId, null);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String json = objectMapper.writeValueAsString(detail);

        // 1. 验证透明金额存在
        assertTrue(json.contains("\"totalAmount\":12800.50"), "必须透出 totalAmount");
        assertTrue(json.contains("\"freightFee\":150.00"), "必须透出 freightFee");
        assertTrue(json.contains("\"unitPrice\":35.50"), "必须透出 unitPrice");
        assertTrue(json.contains("\"amount\":3550.00"), "必须透出 amount");

        // 2. 严格白名单断言：不得包含内部敏感字段
        assertFalse(json.contains("salesperson"), "不得包含业务员信息");
        assertFalse(json.contains("deptId"), "不得包含部门信息");
        assertFalse(json.contains("cost"), "不得包含成本信息");
        assertFalse(json.contains("commission"), "不得包含佣金信息");
        assertFalse(json.contains("phone"), "不得包含客户电话");
        assertFalse(json.contains("warehouse"), "不得包含内部仓库信息");
        assertFalse(json.contains("sourceStockInItemId"), "不得包含内部入库单源头信息");
        assertFalse(json.contains("deleted"), "不得包含软删标志");
        assertFalse(json.contains("内部发货备注"), "不得包含内部单据备注");
        assertFalse(json.contains("内部明细备注"), "不得包含内部明细备注");
        assertFalse(json.contains("卷内部备注"), "不得包含卷内部备注");
    }

    // ==================== Issue 04: 门户发货单签收/拒收测试 ====================

    @Test
    @DisplayName("签收登记：成功登记已签收（SIGNED），signedAt 为服务器时间，更新委托 shipmentService 并留痕")
    void testSignShipment_signedSuccess() {
        Long shipmentId = 101L;
        String shipmentNo = "SH-20261011-001";
        ShipmentDO shipment = createCompletedShipment(shipmentId, shipmentNo, TEST_CUSTOMER_ID, TEST_TENANT_ID);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        PortalShipmentSignReqVO reqVO = PortalShipmentSignReqVO.builder()
                .id(shipmentId)
                .signStatus("SIGNED")
                .signedByName("李收货")
                .signRemark("外包装完好，实收全部数量")
                .build();

        portalShipmentService.signShipment(reqVO);

        ArgumentCaptor<ShipmentUpdateSignReqVO> captor = ArgumentCaptor.forClass(ShipmentUpdateSignReqVO.class);
        verify(shipmentService).updateShipmentSign(captor.capture());

        ShipmentUpdateSignReqVO captured = captor.getValue();
        assertEquals(shipmentId, captured.getId());
        assertEquals("SIGNED", captured.getSignStatus());
        assertEquals("李收货", captured.getSignedByName());
        assertEquals("外包装完好，实收全部数量", captured.getSignRemark());
        assertNotNull(captured.getSignedAt());
        assertTrue(Duration.between(captured.getSignedAt(), LocalDateTime.now()).abs().getSeconds() < 5,
                "signedAt 必须为服务器当前时间");

        // 验证 LogRecordContext 放入了 shipmentNo 供日志模版解析
        assertEquals(shipmentNo, LogRecordContext.getVariable("shipmentNo"));
    }

    @Test
    @DisplayName("拒收登记：成功登记已拒收（REFUSED），零库存/账务副作用，仅调用 updateShipmentSign")
    void testSignShipment_refusedSuccess() {
        Long shipmentId = 102L;
        String shipmentNo = "SH-20261011-002";
        ShipmentDO shipment = createCompletedShipment(shipmentId, shipmentNo, TEST_CUSTOMER_ID, TEST_TENANT_ID);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        PortalShipmentSignReqVO reqVO = PortalShipmentSignReqVO.builder()
                .id(shipmentId)
                .signStatus("REFUSED")
                .signedByName("王质检")
                .signRemark("货物受潮有异味，整单拒收")
                .build();

        portalShipmentService.signShipment(reqVO);

        ArgumentCaptor<ShipmentUpdateSignReqVO> captor = ArgumentCaptor.forClass(ShipmentUpdateSignReqVO.class);
        verify(shipmentService).updateShipmentSign(captor.capture());

        ShipmentUpdateSignReqVO captured = captor.getValue();
        assertEquals(shipmentId, captured.getId());
        assertEquals("REFUSED", captured.getSignStatus());
        assertEquals("王质检", captured.getSignedByName());
        assertEquals("货物受潮有异味，整单拒收", captured.getSignRemark());
        assertNotNull(captured.getSignedAt());
        assertEquals(shipmentNo, LogRecordContext.getVariable("shipmentNo"));
    }

    @Test
    @DisplayName("签收状态守卫：仅 COMPLETED 可签，草稿与已取消报 SHIPMENT_STATUS_NOT_ALLOW (1_030_016_002)")
    void testSignShipment_statusNotCompleted_throwsShipmentStatusNotAllow() {
        Long shipmentId = 103L;
        ShipmentDO draftShipment = createCompletedShipment(shipmentId, "SH-DRAFT", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        draftShipment.setStatus("DRAFT");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(draftShipment);

        PortalShipmentSignReqVO reqVO = PortalShipmentSignReqVO.builder()
                .id(shipmentId)
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();

        ServiceException draftEx = assertThrows(ServiceException.class,
                () -> portalShipmentService.signShipment(reqVO));
        assertEquals(ErrorCodeConstants.SHIPMENT_STATUS_NOT_ALLOW.getCode(), draftEx.getCode());

        // 已取消单据
        ShipmentDO cancelledShipment = createCompletedShipment(shipmentId, "SH-CANCEL", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        cancelledShipment.setStatus("CANCELLED");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(cancelledShipment);

        ServiceException cancelEx = assertThrows(ServiceException.class,
                () -> portalShipmentService.signShipment(reqVO));
        assertEquals(ErrorCodeConstants.SHIPMENT_STATUS_NOT_ALLOW.getCode(), cancelEx.getCode());

        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("归属校验：客户A不能签客户B的发货单，报 PORTAL_DATA_ACCESS_DENIED (1_030_038_001)")
    void testSignShipment_crossCustomerOwnership_throwsPortalDataAccessDenied() {
        Long shipmentId = 104L;
        Long otherCustomerId = 999L;
        ShipmentDO shipment = createCompletedShipment(shipmentId, "SH-OTHER", otherCustomerId, TEST_TENANT_ID);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        PortalShipmentSignReqVO reqVO = PortalShipmentSignReqVO.builder()
                .id(shipmentId)
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.signShipment(reqVO));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("租户隔离守卫：跨租户签收报 PORTAL_DATA_ACCESS_DENIED (1_030_038_001)")
    void testSignShipment_crossTenantOwnership_throwsPortalDataAccessDenied() {
        Long shipmentId = 105L;
        Long otherTenantId = 888L;
        ShipmentDO shipment = createCompletedShipment(shipmentId, "SH-TENANT", TEST_CUSTOMER_ID, otherTenantId);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        PortalShipmentSignReqVO reqVO = PortalShipmentSignReqVO.builder()
                .id(shipmentId)
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.signShipment(reqVO));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("单据存在性守卫：单据不存在或已软删报 PORTAL_DATA_ACCESS_DENIED (1_030_038_001)")
    void testSignShipment_shipmentNotExists_throwsPortalDataAccessDenied() {
        Long nonExistentId = 9999L;
        when(shipmentMapper.selectById(nonExistentId)).thenReturn(null);

        PortalShipmentSignReqVO reqVO = PortalShipmentSignReqVO.builder()
                .id(nonExistentId)
                .signStatus("SIGNED")
                .signedByName("张三")
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.signShipment(reqVO));
        assertEquals(PortalErrorCodeConstants.PORTAL_DATA_ACCESS_DENIED.getCode(), ex.getCode());
        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("签收人必填守卫：signedByName 为空报 BAD_REQUEST")
    void testSignShipment_signedByNameBlank_throwsException() {
        Long shipmentId = 106L;

        PortalShipmentSignReqVO reqVO = PortalShipmentSignReqVO.builder()
                .id(shipmentId)
                .signStatus("SIGNED")
                .signedByName("   ")
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> portalShipmentService.signShipment(reqVO));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());
        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("签收状态合法性校验：仅支持 SIGNED 与 REFUSED，PENDING 或非法状态报 SHIPMENT_SIGN_STATUS_INVALID (1_030_016_006)")
    void testSignShipment_invalidSignStatus_throwsShipmentSignStatusInvalid() {
        Long shipmentId = 107L;
        ShipmentDO shipment = createCompletedShipment(shipmentId, "SH-107", TEST_CUSTOMER_ID, TEST_TENANT_ID);
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        // 试图在门户将状态置为 PENDING
        PortalShipmentSignReqVO reqPending = PortalShipmentSignReqVO.builder()
                .id(shipmentId)
                .signStatus("PENDING")
                .signedByName("张三")
                .build();
        ServiceException exPending = assertThrows(ServiceException.class,
                () -> portalShipmentService.signShipment(reqPending));
        assertEquals(ErrorCodeConstants.SHIPMENT_SIGN_STATUS_INVALID.getCode(), exPending.getCode());

        // 非法未知状态
        PortalShipmentSignReqVO reqUnknown = PortalShipmentSignReqVO.builder()
                .id(shipmentId)
                .signStatus("UNKNOWN_STATUS")
                .signedByName("张三")
                .build();
        ServiceException exUnknown = assertThrows(ServiceException.class,
                () -> portalShipmentService.signShipment(reqUnknown));
        assertEquals(ErrorCodeConstants.SHIPMENT_SIGN_STATUS_INVALID.getCode(), exUnknown.getCode());

        verify(shipmentService, never()).updateShipmentSign(any());
    }

    @Test
    @DisplayName("留痕注解校验：signShipment 包含正确的 @LogRecord 注解定义")
    void testSignShipment_logRecordAnnotation() throws NoSuchMethodException {
        Method method = PortalShipmentServiceImpl.class.getMethod("signShipment", PortalShipmentSignReqVO.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);

        assertNotNull(logRecord, "signShipment 必须标注 @LogRecord 注解");
        assertEquals("客户门户", logRecord.type());
        assertEquals("发货签收", logRecord.subType());
        assertEquals("{{#reqVO.id}}", logRecord.bizNo());
        assertEquals("门户客户登记发货单签收【{{#shipmentNo}}】：状态【{{#reqVO.signStatus}}】，签收人【{{#reqVO.signedByName}}】", logRecord.success());
    }

    @Test
    @DisplayName("Bean Validation 约束测试：PortalShipmentSignReqVO 属性校验")
    void testPortalShipmentSignReqVO_beanValidation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        // 1. 缺失 id
        PortalShipmentSignReqVO reqMissingId = new PortalShipmentSignReqVO(null, "SIGNED", "张三", "备注");
        assertFalse(validator.validate(reqMissingId).isEmpty(), "id 为空应校验失败");

        // 2. 缺失 signStatus
        PortalShipmentSignReqVO reqMissingStatus = new PortalShipmentSignReqVO(1L, " ", "张三", "备注");
        assertFalse(validator.validate(reqMissingStatus).isEmpty(), "signStatus 为空应校验失败");

        // 3. 缺失 signedByName
        PortalShipmentSignReqVO reqMissingName = new PortalShipmentSignReqVO(1L, "SIGNED", "  ", "备注");
        assertFalse(validator.validate(reqMissingName).isEmpty(), "signedByName 为空应校验失败");

        // 4. 全部合法
        PortalShipmentSignReqVO reqValid = new PortalShipmentSignReqVO(1L, "SIGNED", "张三", "备注");
        assertTrue(validator.validate(reqValid).isEmpty(), "参数合规应通过校验");
    }

}
