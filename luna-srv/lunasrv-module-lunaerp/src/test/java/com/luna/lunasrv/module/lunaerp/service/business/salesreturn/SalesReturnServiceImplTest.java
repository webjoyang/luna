package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceService;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceRespVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceSubmitReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.vo.ApprovalInstanceWithdrawReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation.WarehouseLocationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customercontact.CustomerContactMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnRollMapper;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInItemPlacementSaveReqVO;
import com.luna.lunasrv.module.lunaerp.service.stock.stockin.StockInService;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnReasonEnum;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnStatusEnum;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.event.SalesReturnPostedEvent;
import org.springframework.context.ApplicationEventPublisher;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 销售退货业务服务单测（SalesReturnServiceImplTest）
 * 覆盖 Issue: .scratch/sales-return/issues/02-draft-crud-availability.md
 */
@ExtendWith(MockitoExtension.class)
public class SalesReturnServiceImplTest {

    @Mock
    private SalesReturnMapper salesReturnMapper;

    @Mock
    private SalesReturnItemMapper salesReturnItemMapper;

    @Mock
    private SalesReturnRollMapper salesReturnRollMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private ShipmentItemMapper shipmentItemMapper;

    @Mock
    private ShipmentRollMapper shipmentRollMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private WarehouseLocationMapper warehouseLocationMapper;

    @Mock
    private StockInService stockInService;

    @Mock
    private SalesReturnAvailabilityService availabilityService;

    @Mock
    private ApprovalInstanceService approvalInstanceService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerContactMapper customerContactMapper;

    @InjectMocks
    private SalesReturnServiceImpl salesReturnService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesReturnDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesReturnItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesReturnRollDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentRollDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), WarehouseDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), WarehouseLocationDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), StockInDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerContactDO.class);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private ShipmentDO mockShipment(Long id, String status) {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(id);
        shipment.setShipmentNo("SH-20261018-001");
        shipment.setStatus(status);
        shipment.setCustomerId(101L);
        shipment.setCustomerName("测试客户");
        shipment.setSalesContractId(201L);
        shipment.setContractNo("CH20260001");
        shipment.setWarehouseId(301L);
        shipment.setWarehouseName("成品仓");
        shipment.setCurrency("RMB");
        shipment.setShippingDate(LocalDate.of(2026, 10, 18));
        shipment.setTotalAmount(new BigDecimal("5000.00"));
        shipment.setTotalPieces(2);
        return shipment;
    }

    private ShipmentItemDO mockShipmentItem(Long id, Long shipmentId, BigDecimal quantity, BigDecimal unitPrice) {
        ShipmentItemDO item = new ShipmentItemDO();
        item.setId(id);
        item.setShipmentId(shipmentId);
        item.setProductId(401L);
        item.setProductCode("PRD-001");
        item.setProductName("纯棉斜纹");
        item.setCustomerColorNo("WH-01");
        item.setColorName("本白");
        item.setVatNo("LOT-01");
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setUnit("KG");
        item.setPieces(1);
        item.setAmount(quantity.multiply(unitPrice));
        return item;
    }

    private ShipmentRollDO mockShipmentRoll(Long id, Long shipmentId, Long shipmentItemId, String rollNo, BigDecimal weight, BigDecimal lengthM) {
        ShipmentRollDO roll = new ShipmentRollDO();
        roll.setId(id);
        roll.setShipmentId(shipmentId);
        roll.setShipmentItemId(shipmentItemId);
        roll.setRollNo(rollNo);
        roll.setWeight(weight);
        roll.setLengthM(lengthM);
        return roll;
    }

    private WarehouseDO mockWarehouse(Long id, String name) {
        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(id);
        warehouse.setName(name);
        warehouse.setStatus("ACTIVE");
        return warehouse;
    }

    // ==================== 1. 制单取数与候选列表 ====================

    @Test
    @DisplayName("候选发货单列表：仅返回 COMPLETED 且含实时可退量>0 行的发货单")
    void testGetShipmentCandidates_onlyCompletedAndAvailableQty() {
        ShipmentDO completedShipment1 = mockShipment(1L, "COMPLETED");
        ShipmentDO completedShipment2 = mockShipment(2L, "COMPLETED");
        completedShipment2.setShipmentNo("SH-20261018-002");

        when(shipmentMapper.selectList(any())).thenReturn(List.of(completedShipment1, completedShipment2));

        // 单 1 有可退行
        ShipmentItemDO item1 = mockShipmentItem(11L, 1L, new BigDecimal("100.00"), new BigDecimal("25.00"));
        // 单 2 已全退（可退量为 0）
        ShipmentItemDO item2 = mockShipmentItem(21L, 2L, new BigDecimal("100.00"), new BigDecimal("25.00"));

        when(shipmentItemMapper.selectList(any())).thenAnswer(inv -> {
            return List.of(item1, item2);
        });

        when(availabilityService.getOccupiedQtyMap(anyCollection(), isNull())).thenReturn(Map.of(
                11L, new BigDecimal("40.00"), // 100 - 40 = 60 > 0 (候选)
                21L, new BigDecimal("100.00") // 100 - 100 = 0 (排除)
        ));

        List<SalesReturnCandidateRespVO> candidates = salesReturnService.getShipmentCandidates(null, null);
        assertEquals(1, candidates.size());
        assertEquals(1L, candidates.get(0).getId());
        assertEquals("SH-20261018-001", candidates.get(0).getShipmentNo());
    }

    @Test
    @DisplayName("取发货详情：发货单不存在或未审核（非 COMPLETED）报 003")
    void testGetShipmentDetail_notCompleted_throws003() {
        // 场景 1: 不存在
        when(shipmentMapper.selectById(999L)).thenReturn(null);
        ServiceException ex1 = assertThrows(ServiceException.class, () -> salesReturnService.getShipmentDetail(999L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED.getCode(), ex1.getCode());

        // 场景 2: 状态为草稿 DRAFT
        ShipmentDO draftShipment = mockShipment(100L, "DRAFT");
        when(shipmentMapper.selectById(100L)).thenReturn(draftShipment);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> salesReturnService.getShipmentDetail(100L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("取发货详情：正确返回头单快照、明细行带可退量及卷清单")
    void testGetShipmentDetail_success() {
        Long shipmentId = 100L;
        ShipmentDO shipment = mockShipment(shipmentId, "COMPLETED");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);

        ShipmentItemDO item1 = mockShipmentItem(201L, shipmentId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectList(any())).thenReturn(List.of(item1));

        ShipmentRollDO roll1 = mockShipmentRoll(301L, shipmentId, 201L, "ROLL-001", new BigDecimal("50.00"), new BigDecimal("100.00"));
        when(shipmentRollMapper.selectList(any())).thenReturn(List.of(roll1));

        when(availabilityService.getOccupiedQtyMap(eq(List.of(201L)), isNull())).thenReturn(Map.of(201L, new BigDecimal("30.00")));

        SalesReturnShipmentDetailRespVO detail = salesReturnService.getShipmentDetail(shipmentId);
        assertNotNull(detail);
        assertEquals(shipmentId, detail.getShipmentId());
        assertEquals("SH-20261018-001", detail.getShipmentNo());
        assertEquals("测试客户", detail.getCustomerName());
        assertEquals("成品仓", detail.getWarehouseName());
        assertEquals(1, detail.getItems().size());

        SalesReturnShipmentItemRespVO itemResp = detail.getItems().get(0);
        assertEquals(new BigDecimal("100.00"), itemResp.getQuantity());
        assertEquals(new BigDecimal("30.00"), itemResp.getOccupiedQty());
        assertEquals(new BigDecimal("70.00"), itemResp.getAvailableQty());
        assertEquals(1, itemResp.getRolls().size());
        assertEquals("ROLL-001", itemResp.getRolls().get(0).getRollNo());
    }

    // ==================== 2. 草稿创建（Create） ====================

    @Test
    @DisplayName("创建退货草稿：发货单非 COMPLETED 报 003")
    void testCreateSalesReturn_shipmentNotCompleted_throws003() {
        SalesReturnCreateReqVO req = new SalesReturnCreateReqVO();
        req.setShipmentId(999L);
        when(shipmentMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.createSalesReturn(req));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建退货草稿：退货行不属于该发货单报 004")
    void testCreateSalesReturn_itemLineageInvalid_throws004() {
        Long shipmentId = 100L;
        ShipmentDO shipment = mockShipment(shipmentId, "COMPLETED");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        when(warehouseMapper.selectById(any())).thenReturn(mockWarehouse(301L, "成品仓"));

        // 发货单真实明细只有 201
        ShipmentItemDO itemReal = mockShipmentItem(201L, shipmentId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectList(any())).thenReturn(List.of(itemReal));

        // 前端传了属于其他单据的 999
        SalesReturnItemSaveReqVO itemReq = new SalesReturnItemSaveReqVO();
        itemReq.setShipmentItemId(999L);
        itemReq.setReturnQty(new BigDecimal("10.00"));
        itemReq.setReturnReason("QUALITY");
        itemReq.setRolls(List.of(new SalesReturnRollSaveReqVO(null, null, "ROLL-1", new BigDecimal("10.00"), null, null)));

        SalesReturnCreateReqVO req = new SalesReturnCreateReqVO();
        req.setShipmentId(shipmentId);
        req.setWarehouseId(301L);
        req.setItems(List.of(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.createSalesReturn(req));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_ITEM_LINEAGE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建退货草稿：退货行原因必填且必须合法枚举值报 015")
    void testCreateSalesReturn_returnReasonEmptyOrInvalid_throws015() {
        Long shipmentId = 100L;
        ShipmentDO shipment = mockShipment(shipmentId, "COMPLETED");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        when(warehouseMapper.selectById(any())).thenReturn(mockWarehouse(301L, "成品仓"));

        ShipmentItemDO itemReal = mockShipmentItem(201L, shipmentId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectList(any())).thenReturn(List.of(itemReal));

        // 退货原因空
        SalesReturnItemSaveReqVO itemReqEmptyReason = new SalesReturnItemSaveReqVO();
        itemReqEmptyReason.setShipmentItemId(201L);
        itemReqEmptyReason.setReturnQty(new BigDecimal("10.00"));
        itemReqEmptyReason.setReturnReason("");
        itemReqEmptyReason.setRolls(List.of(new SalesReturnRollSaveReqVO(null, null, "ROLL-1", new BigDecimal("10.00"), null, null)));

        SalesReturnCreateReqVO reqEmptyReason = new SalesReturnCreateReqVO();
        reqEmptyReason.setShipmentId(shipmentId);
        reqEmptyReason.setWarehouseId(301L);
        reqEmptyReason.setItems(List.of(itemReqEmptyReason));

        ServiceException ex1 = assertThrows(ServiceException.class, () -> salesReturnService.createSalesReturn(reqEmptyReason));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_REASON_EMPTY.getCode(), ex1.getCode());

        // 退货原因非法
        itemReqEmptyReason.setReturnReason("INVALID_REASON");
        ServiceException ex2 = assertThrows(ServiceException.class, () -> salesReturnService.createSalesReturn(reqEmptyReason));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_REASON_EMPTY.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("创建退货草稿：退货卷信息非法（卷号空、重量米数双空或卷血缘不属于该明细）报 007")
    void testCreateSalesReturn_rollInvalid_throws007() {
        Long shipmentId = 100L;
        ShipmentDO shipment = mockShipment(shipmentId, "COMPLETED");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        when(warehouseMapper.selectById(any())).thenReturn(mockWarehouse(301L, "成品仓"));

        ShipmentItemDO itemReal = mockShipmentItem(201L, shipmentId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectList(any())).thenReturn(List.of(itemReal));

        // 卷清单为空
        SalesReturnItemSaveReqVO itemReqEmptyRolls = new SalesReturnItemSaveReqVO();
        itemReqEmptyRolls.setShipmentItemId(201L);
        itemReqEmptyRolls.setReturnQty(new BigDecimal("10.00"));
        itemReqEmptyRolls.setReturnReason("QUALITY");
        itemReqEmptyRolls.setRolls(Collections.emptyList());

        SalesReturnCreateReqVO req1 = new SalesReturnCreateReqVO();
        req1.setShipmentId(shipmentId);
        req1.setWarehouseId(301L);
        req1.setItems(List.of(itemReqEmptyRolls));

        ServiceException ex1 = assertThrows(ServiceException.class, () -> salesReturnService.createSalesReturn(req1));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_ROLL_INVALID.getCode(), ex1.getCode());

        // 卷重量米数双空
        SalesReturnRollSaveReqVO rollReqEmptyWeightLength = new SalesReturnRollSaveReqVO(null, null, "ROLL-1", null, null, null);
        itemReqEmptyRolls.setRolls(List.of(rollReqEmptyWeightLength));
        ServiceException ex2 = assertThrows(ServiceException.class, () -> salesReturnService.createSalesReturn(req1));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_ROLL_INVALID.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("创建退货草稿：单号查重与按天自动取号（RT-YYYYMMDD-001）服务端权威重算成功")
    void testCreateSalesReturn_success_autoGenerateNo() {
        Long shipmentId = 100L;
        ShipmentDO shipment = mockShipment(shipmentId, "COMPLETED");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        when(warehouseMapper.selectById(301L)).thenReturn(mockWarehouse(301L, "成品仓"));

        ShipmentItemDO itemReal = mockShipmentItem(201L, shipmentId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectList(any())).thenReturn(List.of(itemReal));

        SalesReturnRollSaveReqVO rollReq = new SalesReturnRollSaveReqVO(null, null, "ROLL-1", new BigDecimal("20.00"), null, null);
        SalesReturnItemSaveReqVO itemReq = new SalesReturnItemSaveReqVO();
        itemReq.setShipmentItemId(201L);
        itemReq.setReturnQty(new BigDecimal("20.00"));
        itemReq.setUnitPrice(new BigDecimal("20.00")); // 折让单价 20
        itemReq.setPieces(1);
        itemReq.setReturnReason("QUALITY");
        itemReq.setRolls(List.of(rollReq));

        SalesReturnCreateReqVO req = new SalesReturnCreateReqVO();
        req.setShipmentId(shipmentId);
        req.setWarehouseId(301L);
        req.setReturnDate(LocalDate.of(2026, 10, 18));
        req.setItems(List.of(itemReq));

        // 模拟按天取号无冲突
        when(salesReturnMapper.selectListByReturnNoPrefixIncludingDeleted(eq(TENANT_ID), eq("RT-20261018-")))
                .thenReturn(Collections.emptyList());
        when(salesReturnMapper.selectByReturnNoIncludingDeleted(eq(TENANT_ID), eq("RT-20261018-001")))
                .thenReturn(null);

        doAnswer(inv -> {
            SalesReturnDO created = inv.getArgument(0);
            created.setId(888L);
            return 1;
        }).when(salesReturnMapper).insert(any(SalesReturnDO.class));

        Long returnId = salesReturnService.createSalesReturn(req);
        assertEquals(888L, returnId);

        ArgumentCaptor<SalesReturnDO> returnCaptor = ArgumentCaptor.forClass(SalesReturnDO.class);
        verify(salesReturnMapper).insert(returnCaptor.capture());
        SalesReturnDO savedReturn = returnCaptor.getValue();

        assertEquals("RT-20261018-001", savedReturn.getReturnNo());
        assertEquals("DRAFT", savedReturn.getStatus());
        assertEquals("PENDING", savedReturn.getApprovalStatus());
        assertEquals(new BigDecimal("400.00"), savedReturn.getTotalAmount()); // 20 * 20 = 400
        assertEquals(1, savedReturn.getTotalPieces());
        assertEquals("测试客户", savedReturn.getCustomerName());
        assertEquals("RMB", savedReturn.getCurrency());

        // 校验批量防超退聚合方法被调用（coding-standards.md §2 防 N+1 模式）
        verify(availabilityService).getOccupiedQtyMap(anySet(), isNull());
        verify(salesReturnItemMapper).insert(any(SalesReturnItemDO.class));
        verify(salesReturnRollMapper).insert(any(SalesReturnRollDO.class));
    }

    // ==================== 3. 草稿修改（Update） ====================

    @Test
    @DisplayName("修改退货单：仅 status=DRAFT 且 approvalStatus=PENDING/REJECTED 可修改，否则报 002")
    void testUpdateSalesReturn_statusNotAllowed_throws002() {
        SalesReturnDO approved = new SalesReturnDO();
        approved.setId(10L);
        approved.setStatus("APPROVED");
        approved.setApprovalStatus("APPROVED");
        when(salesReturnMapper.selectById(10L)).thenReturn(approved);

        SalesReturnUpdateReqVO req = new SalesReturnUpdateReqVO();
        req.setId(10L);
        req.setWarehouseId(301L);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.updateSalesReturn(req));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("修改退货草稿：ADR-0032 差异保存（新增、修改、删除明细行与卷）")
    void testUpdateSalesReturn_diffSave_success() {
        Long returnId = 10L;
        Long shipmentId = 100L;

        SalesReturnDO existing = new SalesReturnDO();
        existing.setId(returnId);
        existing.setReturnNo("RT-20261018-001");
        existing.setStatus("DRAFT");
        existing.setApprovalStatus("PENDING");
        existing.setShipmentId(shipmentId);
        existing.setCustomerId(101L);
        existing.setWarehouseId(301L);
        existing.setCurrency("RMB");
        when(salesReturnMapper.selectById(returnId)).thenReturn(existing);

        ShipmentDO shipment = mockShipment(shipmentId, "COMPLETED");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        when(warehouseMapper.selectById(302L)).thenReturn(mockWarehouse(302L, "成品二仓"));

        // 发货明细包含 201 与 202
        ShipmentItemDO sItem1 = mockShipmentItem(201L, shipmentId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        ShipmentItemDO sItem2 = mockShipmentItem(202L, shipmentId, new BigDecimal("50.00"), new BigDecimal("30.00"));
        when(shipmentItemMapper.selectList(any())).thenReturn(List.of(sItem1, sItem2));

        // 数据库存量退货明细：包含行 101（关联 201），行 102（关联 202，本次删除）
        SalesReturnItemDO dbItem1 = new SalesReturnItemDO();
        dbItem1.setId(101L);
        dbItem1.setReturnId(returnId);
        dbItem1.setShipmentItemId(201L);

        SalesReturnItemDO dbItem2 = new SalesReturnItemDO();
        dbItem2.setId(102L);
        dbItem2.setReturnId(returnId);
        dbItem2.setShipmentItemId(202L);
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(dbItem1, dbItem2));

        // 数据库存量卷：包含卷 1001（属于 101）
        SalesReturnRollDO dbRoll1 = new SalesReturnRollDO();
        dbRoll1.setId(1001L);
        dbRoll1.setReturnId(returnId);
        dbRoll1.setReturnItemId(101L);
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(dbRoll1));

        // 前端提交修改：保留 101 并新增卷 1002，同时移除了 102
        SalesReturnRollSaveReqVO roll1 = new SalesReturnRollSaveReqVO(1001L, null, "ROLL-1", new BigDecimal("15.00"), null, null);
        SalesReturnRollSaveReqVO roll2 = new SalesReturnRollSaveReqVO(null, null, "ROLL-2", new BigDecimal("10.00"), null, null);

        SalesReturnItemSaveReqVO itemReq = new SalesReturnItemSaveReqVO();
        itemReq.setId(101L);
        itemReq.setShipmentItemId(201L);
        itemReq.setReturnQty(new BigDecimal("25.00"));
        itemReq.setUnitPrice(new BigDecimal("25.00"));
        itemReq.setPieces(2);
        itemReq.setReturnReason("WRONG");
        itemReq.setRolls(List.of(roll1, roll2));

        SalesReturnUpdateReqVO updateReq = new SalesReturnUpdateReqVO();
        updateReq.setId(returnId);
        updateReq.setWarehouseId(302L); // 改为二仓
        updateReq.setItems(List.of(itemReq));

        salesReturnService.updateSalesReturn(updateReq);

        // 验证主表更新
        verify(salesReturnMapper).updateById(any(SalesReturnDO.class));
        // 验证删除的明细行 102 被删除
        verify(salesReturnItemMapper).deleteByIds(eq(Set.of(102L)));
        // 验证更新的明细行 101 被更新
        verify(salesReturnItemMapper).updateById(any(SalesReturnItemDO.class));
        // 验证新增的卷被插入
        verify(salesReturnRollMapper).insert(any(SalesReturnRollDO.class));
    }

    @Test
    @DisplayName("修改退货草稿：验证清空可空字段 remark 成功触发 FieldStrategy.ALWAYS (coding-standards.md §15)")
    void testUpdateSalesReturn_clearRemark_success() {
        Long returnId = 10L;
        Long shipmentId = 100L;

        SalesReturnDO existing = new SalesReturnDO();
        existing.setId(returnId);
        existing.setReturnNo("RT-20261018-001");
        existing.setStatus("DRAFT");
        existing.setApprovalStatus("PENDING");
        existing.setShipmentId(shipmentId);
        existing.setCustomerId(101L);
        existing.setWarehouseId(301L);
        existing.setCurrency("RMB");
        existing.setRemark("历史业务备注");
        when(salesReturnMapper.selectById(returnId)).thenReturn(existing);

        ShipmentDO shipment = mockShipment(shipmentId, "COMPLETED");
        when(shipmentMapper.selectById(shipmentId)).thenReturn(shipment);
        when(warehouseMapper.selectById(301L)).thenReturn(mockWarehouse(301L, "成品仓"));

        ShipmentItemDO sItem1 = mockShipmentItem(201L, shipmentId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectList(any())).thenReturn(List.of(sItem1));

        SalesReturnItemDO dbItem1 = new SalesReturnItemDO();
        dbItem1.setId(101L);
        dbItem1.setReturnId(returnId);
        dbItem1.setShipmentItemId(201L);
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(dbItem1));
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.emptyList());

        SalesReturnRollSaveReqVO roll1 = new SalesReturnRollSaveReqVO(null, null, "ROLL-1", new BigDecimal("15.00"), null, null);
        SalesReturnItemSaveReqVO itemReq = new SalesReturnItemSaveReqVO();
        itemReq.setId(101L);
        itemReq.setShipmentItemId(201L);
        itemReq.setReturnQty(new BigDecimal("15.00"));
        itemReq.setUnitPrice(new BigDecimal("25.00"));
        itemReq.setReturnReason("QUALITY");
        itemReq.setRolls(List.of(roll1));

        SalesReturnUpdateReqVO updateReq = new SalesReturnUpdateReqVO();
        updateReq.setId(returnId);
        updateReq.setWarehouseId(301L);
        updateReq.setRemark(null); // 显式置空 remark
        updateReq.setItems(List.of(itemReq));

        salesReturnService.updateSalesReturn(updateReq);

        ArgumentCaptor<SalesReturnDO> captor = ArgumentCaptor.forClass(SalesReturnDO.class);
        verify(salesReturnMapper, atLeastOnce()).updateById(captor.capture());
        assertNull(captor.getValue().getRemark(), "更新实体中 remark 属性必须为 null 以触发 ALWAYS 更新策略清空数据库");
    }

    @Test
    @DisplayName("候选发货单查询：显式指定 shipmentNo 且为非 COMPLETED 发货单报 003")
    void testGetShipmentCandidates_specificShipmentNotCompleted_throws003() {
        ShipmentDO notCompleted = new ShipmentDO();
        notCompleted.setId(99L);
        notCompleted.setShipmentNo("SH-20261018-001");
        notCompleted.setStatus("DRAFT");
        when(shipmentMapper.selectOne(any(SFunction.class), any())).thenReturn(notCompleted);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> salesReturnService.getShipmentCandidates(null, "SH-20261018-001"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED.getCode(), ex.getCode());
    }

    // ==================== 4. 草稿删除（Delete） ====================

    @Test
    @DisplayName("删除退货单：仅 status=DRAFT 且 approvalStatus=PENDING/REJECTED 可删，级联软删行与卷")
    void testDeleteSalesReturn_draftOnly_cascadeSoftDelete() {
        Long returnId = 10L;
        SalesReturnDO draft = new SalesReturnDO();
        draft.setId(returnId);
        draft.setStatus("DRAFT");
        draft.setApprovalStatus("PENDING");
        when(salesReturnMapper.selectById(returnId)).thenReturn(draft);

        salesReturnService.deleteSalesReturn(returnId);

        verify(salesReturnMapper).deleteById(returnId);
        verify(salesReturnItemMapper).delete(any());
        verify(salesReturnRollMapper).delete(any());
    }

    @Test
    @DisplayName("删除退货单：审批中（APPROVING）不可删，报 002")
    void testDeleteSalesReturn_approving_throws002() {
        Long returnId = 10L;
        SalesReturnDO approving = new SalesReturnDO();
        approving.setId(returnId);
        approving.setStatus("DRAFT");
        approving.setApprovalStatus("APPROVING");
        when(salesReturnMapper.selectById(returnId)).thenReturn(approving);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.deleteSalesReturn(returnId));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // ==================== 5. 详情与分页查询 ====================

    @Test
    @DisplayName("查询退货单详情：不存在报 000")
    void testGetSalesReturn_notFound_throws000() {
        when(salesReturnMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.getSalesReturn(999L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("查询退货单详情：组装头单快照、明细行与卷清单成功")
    void testGetSalesReturn_success() {
        Long returnId = 10L;
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(returnId);
        ret.setReturnNo("RT-20261018-001");
        ret.setStatus("DRAFT");
        ret.setApprovalStatus("PENDING");
        ret.setCustomerName("测试客户");
        ret.setWarehouseName("成品仓");
        when(salesReturnMapper.selectById(returnId)).thenReturn(ret);

        SalesReturnItemDO item = new SalesReturnItemDO();
        item.setId(101L);
        item.setReturnId(returnId);
        item.setProductName("纯棉斜纹");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        SalesReturnRollDO roll = new SalesReturnRollDO();
        roll.setId(1001L);
        roll.setReturnId(returnId);
        roll.setReturnItemId(101L);
        roll.setRollNo("ROLL-001");
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll));

        SalesReturnRespVO resp = salesReturnService.getSalesReturn(returnId);
        assertNotNull(resp);
        assertEquals(returnId, resp.getId());
        assertEquals("RT-20261018-001", resp.getReturnNo());
        assertEquals(1, resp.getItems().size());
        assertEquals("纯棉斜纹", resp.getItems().get(0).getProductName());
        assertEquals(1, resp.getItems().get(0).getRolls().size());
        assertEquals("ROLL-001", resp.getItems().get(0).getRolls().get(0).getRollNo());
    }

    // ==================== 6. 提交审批（POST /submit） ====================

    @Test
    @DisplayName("提交审批：退货单不存在抛 000")
    void testSubmitSalesReturn_notExists_throws000() {
        when(salesReturnMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(999L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：非 DRAFT 状态抛 002")
    void testSubmitSalesReturn_nonDraftStatus_throws002() {
        SalesReturnDO nonDraft = new SalesReturnDO();
        nonDraft.setId(10L);
        nonDraft.setStatus("APPROVED");
        nonDraft.setApprovalStatus("APPROVED");
        when(salesReturnMapper.selectById(10L)).thenReturn(nonDraft);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：审批中（APPROVING）不可重复提交抛 002")
    void testSubmitSalesReturn_approvingStatus_throws002() {
        SalesReturnDO approving = new SalesReturnDO();
        approving.setId(10L);
        approving.setStatus("DRAFT");
        approving.setApprovalStatus("APPROVING");
        when(salesReturnMapper.selectById(10L)).thenReturn(approving);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：明细行缺失抛参数异常")
    void testSubmitSalesReturn_emptyItems_throwsException() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
    }

    @Test
    @DisplayName("提交审批：明细行原因为空或非法抛 015")
    void testSubmitSalesReturn_emptyReason_throws015() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_REASON_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：某明细行缺少卷记录抛 007")
    void testSubmitSalesReturn_missingRolls_throws007() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_ROLL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：卷号为空或重量米数双空抛 007")
    void testSubmitSalesReturn_invalidRollData_throws007() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        SalesReturnRollDO roll = new SalesReturnRollDO();
        roll.setId(1001L);
        roll.setReturnId(10L);
        roll.setReturnItemId(101L);
        roll.setRollNo("   "); // 空卷号
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_ROLL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：发货单非 COMPLETED 状态抛 003")
    void testSubmitSalesReturn_shipmentNotCompleted_throws003() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        SalesReturnRollDO roll = createSalesReturnRoll(10L, 101L, 801L, "R-01", new BigDecimal("50.00"), null);
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll));

        when(shipmentMapper.selectById(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：卷血缘不属于该发货明细行抛 007")
    void testSubmitSalesReturn_rollLineageMismatch_throws007() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        SalesReturnRollDO roll = createSalesReturnRoll(10L, 101L, 801L, "R-01", new BigDecimal("50.00"), null);
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll));

        ShipmentDO shipment = createCompletedShipment(100L);
        when(shipmentMapper.selectById(100L)).thenReturn(shipment);

        // 原发货卷属于另一发货行 502L
        ShipmentRollDO sRoll = new ShipmentRollDO();
        sRoll.setId(801L);
        sRoll.setShipmentId(100L);
        sRoll.setShipmentItemId(502L); //  mismatch!
        when(shipmentRollMapper.selectList(any())).thenReturn(List.of(sRoll));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_ROLL_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：锁内重算退货数量超实时可退量抛 005")
    void testSubmitSalesReturn_qtyExceedAvailable_throws005() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        SalesReturnRollDO roll = createSalesReturnRoll(10L, 101L, null, "R-01", new BigDecimal("50.00"), null);
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll));

        ShipmentDO shipment = createCompletedShipment(100L);
        when(shipmentMapper.selectById(100L)).thenReturn(shipment);
        when(shipmentRollMapper.selectList(any())).thenReturn(Collections.emptyList());

        ShipmentItemDO sItem = new ShipmentItemDO();
        sItem.setId(501L);
        sItem.setShipmentId(100L);
        sItem.setQuantity(new BigDecimal("100.00"));
        sItem.setUnitPrice(new BigDecimal("2.00"));
        when(shipmentItemMapper.selectOneForUpdate(any(), any())).thenReturn(sItem);

        // 已占用 60，可用仅剩 40，退货需要 50
        when(availabilityService.getOccupiedQtyMap(eq(List.of(501L)), eq(10L)))
                .thenReturn(Map.of(501L, new BigDecimal("60.00")));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_QTY_EXCEED_AVAILABLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批：锁内重算退货金额超可退额上限抛 006")
    void testSubmitSalesReturn_amountExceedAvailable_throws006() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);

        // 退货单金额 150.00
        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("50.00"), new BigDecimal("150.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        SalesReturnRollDO roll = createSalesReturnRoll(10L, 101L, null, "R-01", new BigDecimal("50.00"), null);
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll));

        ShipmentDO shipment = createCompletedShipment(100L);
        when(shipmentMapper.selectById(100L)).thenReturn(shipment);
        when(shipmentRollMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 原发货行单价 2.00，可用量 60 -> 上限 120.00，本次金额 150.00 超额
        ShipmentItemDO sItem = new ShipmentItemDO();
        sItem.setId(501L);
        sItem.setShipmentId(100L);
        sItem.setQuantity(new BigDecimal("100.00"));
        sItem.setUnitPrice(new BigDecimal("2.00"));
        when(shipmentItemMapper.selectOneForUpdate(any(), any())).thenReturn(sItem);

        when(availabilityService.getOccupiedQtyMap(eq(List.of(501L)), eq(10L)))
                .thenReturn(Map.of(501L, new BigDecimal("40.00")));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_AMOUNT_EXCEED_AVAILABLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("提交审批成功：行锁父行、重算通过、条件更新 APPROVING 并推送到审批中心")
    void testSubmitSalesReturn_success() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("40.00"), new BigDecimal("80.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        SalesReturnRollDO roll = createSalesReturnRoll(10L, 101L, null, "R-01", new BigDecimal("40.00"), null);
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll));

        ShipmentDO shipment = createCompletedShipment(100L);
        when(shipmentMapper.selectById(100L)).thenReturn(shipment);
        when(shipmentRollMapper.selectList(any())).thenReturn(Collections.emptyList());

        ShipmentItemDO sItem = new ShipmentItemDO();
        sItem.setId(501L);
        sItem.setShipmentId(100L);
        sItem.setQuantity(new BigDecimal("100.00"));
        sItem.setUnitPrice(new BigDecimal("2.00"));
        when(shipmentItemMapper.selectOneForUpdate(any(), any())).thenReturn(sItem);

        when(availabilityService.getOccupiedQtyMap(eq(List.of(501L)), eq(10L)))
                .thenReturn(Map.of(501L, new BigDecimal("20.00")));
        when(salesReturnMapper.update(any(), any())).thenReturn(1);

        salesReturnService.submitSalesReturn(10L);

        // 验证行级悲观锁
        verify(shipmentItemMapper).selectOneForUpdate(any(), eq(501L));
        // 验证主表状态条件更新为 APPROVING
        verify(salesReturnMapper).update(any(), any());
        // 验证推送到审批中心
        ArgumentCaptor<ApprovalInstanceSubmitReqVO> captor = ArgumentCaptor.forClass(ApprovalInstanceSubmitReqVO.class);
        verify(approvalInstanceService).submitInstance(captor.capture());
        assertEquals("sales_return", captor.getValue().getBizType());
        assertEquals(10L, captor.getValue().getBizId());
    }

    @Test
    @DisplayName("并发守卫测试：两单同时提交同一发货明细行，第二单在行锁后重算超退失败（005），不出现超退占量")
    void testSubmitSalesReturn_concurrencySimulation_secondOrderExceedsAvailable_throws005() {
        // 发货明细总量 60.00
        ShipmentDO shipment = createCompletedShipment(100L);
        when(shipmentMapper.selectById(100L)).thenReturn(shipment);
        when(shipmentRollMapper.selectList(any())).thenReturn(Collections.emptyList());

        ShipmentItemDO sItem = new ShipmentItemDO();
        sItem.setId(501L);
        sItem.setShipmentId(100L);
        sItem.setQuantity(new BigDecimal("60.00"));
        sItem.setUnitPrice(new BigDecimal("5.00"));
        when(shipmentItemMapper.selectOneForUpdate(any(), any())).thenReturn(sItem);

        // --- 单据 A（50 KG）先行提交 ---
        SalesReturnDO orderA = createDraftSalesReturn(10L, 100L);
        when(salesReturnMapper.selectById(10L)).thenReturn(orderA);
        SalesReturnItemDO itemA = createSalesReturnItem(10L, 101L, 501L, "KG", new BigDecimal("50.00"), new BigDecimal("250.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(itemA));
        SalesReturnRollDO rollA = createSalesReturnRoll(10L, 101L, null, "R-01", new BigDecimal("50.00"), null);
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(rollA));

        // 单据 A 检查时尚无占用
        when(availabilityService.getOccupiedQtyMap(eq(List.of(501L)), eq(10L)))
                .thenReturn(Map.of(501L, BigDecimal.ZERO));
        when(salesReturnMapper.update(any(), any())).thenReturn(1);

        salesReturnService.submitSalesReturn(10L);
        verify(approvalInstanceService).submitInstance(any());

        // --- 单据 B（20 KG）并发提交（等待 A 事务释放锁后，重算发现已占用 50 KG，可用仅剩 10 KG） ---
        SalesReturnDO orderB = createDraftSalesReturn(20L, 100L);
        when(salesReturnMapper.selectById(20L)).thenReturn(orderB);
        SalesReturnItemDO itemB = createSalesReturnItem(20L, 201L, 501L, "KG", new BigDecimal("20.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(itemB));
        SalesReturnRollDO rollB = createSalesReturnRoll(20L, 201L, null, "R-02", new BigDecimal("20.00"), null);
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(rollB));

        // 单据 B 锁内重算时获取到单据 A 已占用的 50 KG
        when(availabilityService.getOccupiedQtyMap(eq(List.of(501L)), eq(20L)))
                .thenReturn(Map.of(501L, new BigDecimal("50.00")));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.submitSalesReturn(20L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_QTY_EXCEED_AVAILABLE.getCode(), ex.getCode());

        // 验证单据 B 并没有被推送到审批中心（未产生超退占量）
        verify(approvalInstanceService, times(1)).submitInstance(any());
    }

    // ==================== 7. 撤回审批（POST /withdraw） ====================

    @Test
    @DisplayName("撤回审批：退货单不存在抛 000")
    void testWithdrawSalesReturn_notExists_throws000() {
        when(salesReturnMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.withdrawSalesReturn(999L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤回审批：非 APPROVING 状态（如 PENDING 或 APPROVED）不可撤回报 002")
    void testWithdrawSalesReturn_notApproving_throws002() {
        SalesReturnDO pending = new SalesReturnDO();
        pending.setId(10L);
        pending.setStatus("DRAFT");
        pending.setApprovalStatus("PENDING");
        when(salesReturnMapper.selectById(10L)).thenReturn(pending);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.withdrawSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("撤回审批成功：调用审批引擎 withdraw 并将 approvalStatus 置为 PENDING 释放占量")
    void testWithdrawSalesReturn_success() {
        SalesReturnDO approving = new SalesReturnDO();
        approving.setId(10L);
        approving.setStatus("DRAFT");
        approving.setApprovalStatus("APPROVING");
        when(salesReturnMapper.selectById(10L)).thenReturn(approving);

        ApprovalInstanceRespVO instance = new ApprovalInstanceRespVO();
        instance.setId(5001L);
        instance.setStatus("APPROVING");
        when(approvalInstanceService.getLatestInstanceByBiz("sales_return", 10L)).thenReturn(instance);

        salesReturnService.withdrawSalesReturn(10L);

        // 验证调用审批引擎撤回
        verify(approvalInstanceService).withdrawInstance(eq(5001L), any(ApprovalInstanceWithdrawReqVO.class));
    }

    @Test
    @DisplayName("撤回审批兜底：若无审批中实例，直接原子更新 approvalStatus 为 PENDING")
    void testWithdrawSalesReturn_fallbackWhenNoInstance() {
        SalesReturnDO approving = new SalesReturnDO();
        approving.setId(10L);
        approving.setStatus("DRAFT");
        approving.setApprovalStatus("APPROVING");
        when(salesReturnMapper.selectById(10L)).thenReturn(approving);
        when(approvalInstanceService.getLatestInstanceByBiz("sales_return", 10L)).thenReturn(null);
        when(salesReturnMapper.update(any(), any())).thenReturn(1);

        salesReturnService.withdrawSalesReturn(10L);

        verify(salesReturnMapper).update(any(), any());
    }

    // ==================== 8. 作废草稿（POST /cancel） ====================

    @Test
    @DisplayName("作废退货单：作废原因空或空白抛 015")
    void testCancelSalesReturn_emptyReason_throws015() {
        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(10L, "  "));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_REASON_EMPTY.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废退货单：不存在抛 000")
    void testCancelSalesReturn_notExists_throws000() {
        when(salesReturnMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(999L, "作废"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废退货单：已是 CANCELLED 终态不可再动抛 002")
    void testCancelSalesReturn_alreadyCancelled_throws002() {
        SalesReturnDO cancelled = new SalesReturnDO();
        cancelled.setId(10L);
        cancelled.setStatus("CANCELLED");
        cancelled.setApprovalStatus("CANCELLED");
        when(salesReturnMapper.selectById(10L)).thenReturn(cancelled);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(10L, "原因"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废退货单：RECEIVED 或 POSTED 终态不可作废抛 002")
    void testCancelSalesReturn_receivedOrPosted_throws002() {
        SalesReturnDO posted = new SalesReturnDO();
        posted.setId(10L);
        posted.setStatus("POSTED");
        posted.setApprovalStatus("APPROVED");
        when(salesReturnMapper.selectById(10L)).thenReturn(posted);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(10L, "原因"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废退货单：APPROVING 状态必须先撤回，直接作废抛 002")
    void testCancelSalesReturn_approvingMustWithdrawFirst_throws002() {
        SalesReturnDO approving = new SalesReturnDO();
        approving.setId(10L);
        approving.setStatus("DRAFT");
        approving.setApprovalStatus("APPROVING");
        when(salesReturnMapper.selectById(10L)).thenReturn(approving);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(10L, "原因"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废草稿成功：DRAFT（PENDING/REJECTED）直接置 CANCELLED，落 cancelled_* 留痕")
    void testCancelSalesReturn_success_draftBranch() {
        SalesReturnDO draft = createDraftSalesReturn(10L, 100L);
        draft.setApprovalStatus("PENDING");
        when(salesReturnMapper.selectById(10L)).thenReturn(draft);
        when(salesReturnMapper.update(any(), any())).thenReturn(1);

        salesReturnService.cancelSalesReturn(10L, "客户取消退货需求");

        verify(salesReturnMapper).update(any(), any());
    }

    @Test
    @DisplayName("作废已审批退货单：缺失 stockInId 抛 009")
    void testCancelSalesReturn_approved_missingStockInId_throws009() {
        SalesReturnDO approved = new SalesReturnDO();
        approved.setId(10L);
        approved.setStatus("APPROVED");
        approved.setApprovalStatus("APPROVED");
        approved.setStockInId(null);
        when(salesReturnMapper.selectById(10L)).thenReturn(approved);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(10L, "原因"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STOCK_IN_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废已审批退货单：入库单不存在抛 009")
    void testCancelSalesReturn_approved_stockInNotFound_throws009() {
        SalesReturnDO approved = new SalesReturnDO();
        approved.setId(10L);
        approved.setStatus("APPROVED");
        approved.setApprovalStatus("APPROVED");
        approved.setStockInId(501L);
        when(salesReturnMapper.selectById(10L)).thenReturn(approved);
        when(stockInService.cancelDraftStockIn(501L)).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(10L, "原因"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STOCK_IN_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废已审批退货单：入库单非 DRAFT 态（如 COMPLETED / CANCELLED）抛 009")
    void testCancelSalesReturn_approved_stockInNotDraft_throws009() {
        SalesReturnDO approved = new SalesReturnDO();
        approved.setId(10L);
        approved.setStatus("APPROVED");
        approved.setApprovalStatus("APPROVED");
        approved.setStockInId(501L);
        when(salesReturnMapper.selectById(10L)).thenReturn(approved);
        when(stockInService.cancelDraftStockIn(501L)).thenReturn(false);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(10L, "原因"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STOCK_IN_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("作废已审批退货单：同事务取消 DRAFT 入库单并置退货单 CANCELLED")
    void testCancelSalesReturn_approved_success_cancelsDraftStockInAndUpdatesSalesReturn() {
        SalesReturnDO approved = new SalesReturnDO();
        approved.setId(10L);
        approved.setStatus("APPROVED");
        approved.setApprovalStatus("APPROVED");
        approved.setStockInId(501L);
        when(salesReturnMapper.selectById(10L)).thenReturn(approved);
        when(stockInService.cancelDraftStockIn(501L)).thenReturn(true);
        when(salesReturnMapper.update(isNull(), any())).thenReturn(1);

        salesReturnService.cancelSalesReturn(10L, "客户紧急取消退货");

        verify(stockInService).cancelDraftStockIn(501L);
        verify(salesReturnMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("详情回显：已审批退货单详情正确回显入库单号与状态")
    void testGetSalesReturn_populatesStockInNoAndStatus() {
        SalesReturnDO salesReturn = createDraftSalesReturn(10L, 100L);
        salesReturn.setStatus("APPROVED");
        salesReturn.setApprovalStatus("APPROVED");
        salesReturn.setStockInId(501L);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(501L);
        stockIn.setInNo("IN-20261018-001");
        stockIn.setStatus("DRAFT");
        when(stockInService.getStockIn(501L)).thenReturn(stockIn);

        SalesReturnRespVO resp = salesReturnService.getSalesReturn(10L);
        assertNotNull(resp);
        assertEquals(501L, resp.getStockInId());
        assertEquals("IN-20261018-001", resp.getStockInNo());
        assertEquals("DRAFT", resp.getStockInStatus());
    }

    @Test
    @DisplayName("分页查询：分页查询退货单")
    void testGetSalesReturnPage_success() {
        SalesReturnDO r1 = createDraftSalesReturn(10L, 100L);
        r1.setStockInId(501L);
        SalesReturnDO r2 = createDraftSalesReturn(20L, 100L);
        r2.setStockInId(502L);

        PageResult<SalesReturnDO> pageResult = new PageResult<>(List.of(r1, r2), 2L);
        when(salesReturnMapper.selectPage(any(SalesReturnPageReqVO.class), any())).thenReturn(pageResult);

        SalesReturnPageReqVO req = new SalesReturnPageReqVO();
        PageResult<SalesReturnRespVO> result = salesReturnService.getSalesReturnPage(req);
        assertEquals(2, result.getList().size());
        assertEquals(10L, result.getList().get(0).getId());
        assertEquals(20L, result.getList().get(1).getId());
    }

    // ==================== 9. 切面注解审计审查 ====================

    @Test
    @DisplayName("切面注解：submitSalesReturn / withdrawSalesReturn / cancelSalesReturn 正确标注 @LogRecord")
    void testLogRecordAnnotations() throws NoSuchMethodException {
        var submitLog = SalesReturnServiceImpl.class.getMethod("submitSalesReturn", Long.class)
                .getAnnotation(com.mzt.logapi.starter.annotation.LogRecord.class);
        assertNotNull(submitLog, "submitSalesReturn 必须标注 @LogRecord");
        assertEquals("SALES_RETURN", submitLog.type());
        assertEquals("SUBMIT", submitLog.subType());

        var withdrawLog = SalesReturnServiceImpl.class.getMethod("withdrawSalesReturn", Long.class)
                .getAnnotation(com.mzt.logapi.starter.annotation.LogRecord.class);
        assertNotNull(withdrawLog, "withdrawSalesReturn 必须标注 @LogRecord");
        assertEquals("SALES_RETURN", withdrawLog.type());
        assertEquals("WITHDRAW", withdrawLog.subType());

        var cancelLog = SalesReturnServiceImpl.class.getMethod("cancelSalesReturn", Long.class, String.class)
                .getAnnotation(com.mzt.logapi.starter.annotation.LogRecord.class);
        assertNotNull(cancelLog, "cancelSalesReturn 必须标注 @LogRecord");
        assertEquals("SALES_RETURN", cancelLog.type());
        assertEquals("CANCEL", cancelLog.subType());
    }

    // ==================== 私有辅助工厂方法 ====================

    private SalesReturnDO createDraftSalesReturn(Long id, Long shipmentId) {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(id);
        ret.setReturnNo("RT-20261018-001");
        ret.setStatus("DRAFT");
        ret.setApprovalStatus("PENDING");
        ret.setShipmentId(shipmentId);
        ret.setShipmentNo("SH-20261018-001");
        ret.setCustomerId(1L);
        ret.setCustomerName("测试客户");
        ret.setWarehouseId(10L);
        ret.setWarehouseName("成品仓");
        ret.setCurrency("RMB");
        ret.setReturnDate(LocalDate.now());
        return ret;
    }

    private ShipmentDO createCompletedShipment(Long id) {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(id);
        shipment.setShipmentNo("SH-20261018-001");
        shipment.setStatus("COMPLETED");
        shipment.setCustomerId(1L);
        shipment.setCustomerName("测试客户");
        shipment.setWarehouseId(10L);
        shipment.setWarehouseName("成品仓");
        shipment.setCurrency("RMB");
        return shipment;
    }

    private SalesReturnItemDO createSalesReturnItem(Long returnId, Long id, Long shipmentItemId, String unit, BigDecimal returnQty, BigDecimal amount, String reason) {
        SalesReturnItemDO item = new SalesReturnItemDO();
        item.setId(id);
        item.setReturnId(returnId);
        item.setShipmentItemId(shipmentItemId);
        item.setProductId(1L);
        item.setProductCode("P001");
        item.setProductName("测试面料");
        item.setUnit(unit);
        item.setReturnQty(returnQty);
        item.setPieces(1);
        item.setUnitPrice(new BigDecimal("2.00"));
        item.setAmount(amount);
        item.setReturnReason(reason);
        return item;
    }

    private SalesReturnRollDO createSalesReturnRoll(Long returnId, Long returnItemId, Long shipmentRollId, String rollNo, BigDecimal weight, BigDecimal lengthM) {
        SalesReturnRollDO roll = new SalesReturnRollDO();
        roll.setId(1001L);
        roll.setReturnId(returnId);
        roll.setReturnItemId(returnItemId);
        roll.setShipmentRollId(shipmentRollId);
        roll.setRollNo(rollNo);
        roll.setWeight(weight);
        roll.setLengthM(lengthM);
        return roll;
    }

    // ==================== Issue 05: 到货质检事务单测 ====================

    @Test
    @DisplayName("到货质检：单据不存在时抛异常")
    void receiveSalesReturn_notExists_throwsException() {
        when(salesReturnMapper.selectById(999L)).thenReturn(null);

        SalesReturnReceiveReqVO reqVO = SalesReturnReceiveReqVO.builder()
                .id(999L)
                .rolls(Collections.singletonList(
                        SalesReturnReceiveRollReqVO.builder().id(1001L).qcResult("RESTOCK").build()
                ))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.receiveSalesReturn(reqVO));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("到货质检：单据非 APPROVED 状态拒绝质检")
    void receiveSalesReturn_statusNotApproved_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.DRAFT.getValue());
        salesReturn.setApprovalStatus("PENDING");
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        SalesReturnReceiveReqVO reqVO = SalesReturnReceiveReqVO.builder()
                .id(10L)
                .rolls(Collections.singletonList(
                        SalesReturnReceiveRollReqVO.builder().id(1001L).qcResult("RESTOCK").build()
                ))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.receiveSalesReturn(reqVO));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_APPROVAL_NOT_APPROVED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("到货质检：入库单缺失或非 DRAFT 状态抛 009")
    void receiveSalesReturn_stockInMissingOrNotDraft_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.APPROVED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        salesReturn.setStockInId(500L);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        // 场景 1：入库单已被人工提前审核为 COMPLETED
        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(500L);
        stockIn.setStatus("COMPLETED");
        when(stockInService.getStockIn(500L)).thenReturn(stockIn);

        SalesReturnReceiveReqVO reqVO = SalesReturnReceiveReqVO.builder()
                .id(10L)
                .rolls(Collections.singletonList(
                        SalesReturnReceiveRollReqVO.builder().id(1001L).qcResult("RESTOCK").build()
                ))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.receiveSalesReturn(reqVO));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STOCK_IN_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("到货质检：存在未质检判定的退货卷抛 010")
    void receiveSalesReturn_unresolvedQcRoll_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.APPROVED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        salesReturn.setStockInId(500L);
        salesReturn.setWarehouseId(301L);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(500L);
        stockIn.setStatus("DRAFT");
        when(stockInService.getStockIn(500L)).thenReturn(stockIn);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 201L, "KG", new BigDecimal("100.00"), new BigDecimal("200.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        // 退货单 DB 中有两卷
        SalesReturnRollDO roll1 = createSalesReturnRoll(10L, 101L, null, "R01", new BigDecimal("50.00"), null);
        roll1.setId(1001L);
        SalesReturnRollDO roll2 = createSalesReturnRoll(10L, 101L, null, "R02", new BigDecimal("50.00"), null);
        roll2.setId(1002L);
        when(salesReturnRollMapper.selectList(any())).thenReturn(Arrays.asList(roll1, roll2));

        // 请求中缺少 roll2，或 roll2 的 qcResult 为空
        SalesReturnReceiveReqVO reqVO = SalesReturnReceiveReqVO.builder()
                .id(10L)
                .rolls(Collections.singletonList(
                        SalesReturnReceiveRollReqVO.builder().id(1001L).qcResult("RESTOCK").build()
                ))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.receiveSalesReturn(reqVO));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_ROLL_QC_UNRESOLVED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("到货质检：RESTOCK 卷对应行单位（KG/M）对应量非空校验（011）")
    void receiveSalesReturn_restockRollMissingUnitQuantity_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.APPROVED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        salesReturn.setStockInId(500L);
        salesReturn.setWarehouseId(301L);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(500L);
        stockIn.setStatus("DRAFT");
        when(stockInService.getStockIn(500L)).thenReturn(stockIn);

        // KG 行
        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 201L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        // 卷 weight 为空（只有米数）
        SalesReturnRollDO roll = createSalesReturnRoll(10L, 101L, null, "R01", null, new BigDecimal("100.00"));
        roll.setId(1001L);
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.singletonList(roll));

        SalesReturnReceiveReqVO reqVO = SalesReturnReceiveReqVO.builder()
                .id(10L)
                .rolls(Collections.singletonList(
                        SalesReturnReceiveRollReqVO.builder().id(1001L).qcResult("RESTOCK").build()
                ))
                .build();

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.receiveSalesReturn(reqVO));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_RESTOCK_QTY_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("到货质检：RESTOCK 卷库位不属于退货仓库时校验拦截")
    void receiveSalesReturn_restockRollLocationNotInWarehouse_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.APPROVED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        salesReturn.setStockInId(500L);
        salesReturn.setWarehouseId(301L);
        salesReturn.setWarehouseName("柯桥一号仓");
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(500L);
        stockIn.setStatus("DRAFT");
        when(stockInService.getStockIn(500L)).thenReturn(stockIn);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 201L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        SalesReturnRollDO roll = createSalesReturnRoll(10L, 101L, null, "R01", new BigDecimal("50.00"), null);
        roll.setId(1001L);
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.singletonList(roll));

        // 库位属于仓库 302，而非 301
        WarehouseLocationDO loc = new WarehouseLocationDO();
        loc.setId(601L);
        loc.setWarehouseId(302L);
        loc.setCode("B-01-01");
        when(warehouseLocationMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(loc));

        SalesReturnReceiveReqVO reqVO = SalesReturnReceiveReqVO.builder()
                .id(10L)
                .rolls(Collections.singletonList(
                        SalesReturnReceiveRollReqVO.builder().id(1001L).qcResult("RESTOCK").locationId(601L).build()
                ))
                .build();

        assertThrows(ServiceException.class, () -> salesReturnService.receiveSalesReturn(reqVO));
    }

    @Test
    @DisplayName("到货质检：部分可入库部分废品，重写入库单仅留 RESTOCK 卷并审核写 STOCK_IN 正流水")
    void receiveSalesReturn_success_partialRestockAndScrap() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setReturnNo("RT-20261019-001");
        salesReturn.setStatus(SalesReturnStatusEnum.APPROVED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        salesReturn.setStockInId(500L);
        salesReturn.setWarehouseId(301L);
        salesReturn.setWarehouseName("柯桥一号仓");
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(500L);
        stockIn.setInNo("IN-20261019-001");
        stockIn.setStatus("DRAFT");
        stockIn.setWarehouseId(301L);
        stockIn.setInDate(LocalDate.now());
        when(stockInService.getStockIn(500L)).thenReturn(stockIn);

        // 退货行：KG 单位，returnQty=70.00
        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 201L, "KG", new BigDecimal("70.00"), new BigDecimal("140.00"), "QUALITY");
        item.setProductId(1L);
        item.setVatNo("LOT-01");
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        // 2 卷：roll1=50KG, roll2=20KG
        SalesReturnRollDO roll1 = createSalesReturnRoll(10L, 101L, null, "R01", new BigDecimal("50.00"), null);
        roll1.setId(1001L);
        SalesReturnRollDO roll2 = createSalesReturnRoll(10L, 101L, null, "R02", new BigDecimal("20.00"), null);
        roll2.setId(1002L);
        when(salesReturnRollMapper.selectList(any())).thenReturn(Arrays.asList(roll1, roll2));

        // 库位 601 属于 301 仓
        WarehouseLocationDO loc = new WarehouseLocationDO();
        loc.setId(601L);
        loc.setWarehouseId(301L);
        loc.setCode("A-01-01");
        when(warehouseLocationMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(loc));

        // 条件更新退货单成功
        when(salesReturnMapper.update(isNull(), any())).thenReturn(1);

        SalesReturnReceiveReqVO reqVO = SalesReturnReceiveReqVO.builder()
                .id(10L)
                .rolls(Arrays.asList(
                        SalesReturnReceiveRollReqVO.builder().id(1001L).qcResult("RESTOCK").locationId(601L).qcRemark("轻微瑕疵二次入库").build(),
                        SalesReturnReceiveRollReqVO.builder().id(1002L).qcResult("SCRAP").qcRemark("严重破洞报废").build()
                ))
                .build();

        salesReturnService.receiveSalesReturn(reqVO);

        // 1. 验证各行 restock_qty 与 scrap_qty 重算
        ArgumentCaptor<SalesReturnItemDO> itemCaptor = ArgumentCaptor.forClass(SalesReturnItemDO.class);
        verify(salesReturnItemMapper).updateById(itemCaptor.capture());
        SalesReturnItemDO updatedItem = itemCaptor.getValue();
        assertEquals(new BigDecimal("50.00"), updatedItem.getRestockQty());
        assertEquals(new BigDecimal("20.00"), updatedItem.getScrapQty());

        // 2. 验证卷快照更新
        verify(salesReturnRollMapper, times(2)).updateById(any(SalesReturnRollDO.class));

        // 3. 验证入库单重写：仅留 RESTOCK 卷（roll1），且单价与金额恒为 0，带 placement
        ArgumentCaptor<StockInSaveReqVO> stockInCaptor = ArgumentCaptor.forClass(StockInSaveReqVO.class);
        verify(stockInService).updateStockIn(stockInCaptor.capture());
        StockInSaveReqVO updateStockInReq = stockInCaptor.getValue();
        assertEquals(500L, updateStockInReq.getId());
        assertEquals(1, updateStockInReq.getItems().size());
        StockInItemSaveReqVO inItem = updateStockInReq.getItems().get(0);
        assertEquals("PRODUCT", inItem.getItemType());
        assertEquals("R01", inItem.getRollNo());
        assertEquals(new BigDecimal("50.00"), inItem.getQuantity());
        assertEquals("KG", inItem.getUnit());
        assertEquals(BigDecimal.ZERO, inItem.getUnitPrice());
        assertEquals(BigDecimal.ZERO, inItem.getAmount());
        assertNotNull(inItem.getPlacements());
        assertEquals(1, inItem.getPlacements().size());
        assertEquals(601L, inItem.getPlacements().get(0).getLocationId());

        // 4. 验证调 stock-in 审核（写 STOCK_IN 正流水，非 RETURN_IN）
        verify(stockInService).auditStockIn(500L);
        verify(stockInService, never()).cancelDraftStockIn(any());
    }

    @Test
    @DisplayName("到货质检：全废分支，自动取消入库单，退货单仍进入 RECEIVED")
    void receiveSalesReturn_success_allScrap_cancelsStockInAndMarksReceived() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setReturnNo("RT-20261019-002");
        salesReturn.setStatus(SalesReturnStatusEnum.APPROVED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        salesReturn.setStockInId(500L);
        salesReturn.setWarehouseId(301L);
        salesReturn.setWarehouseName("柯桥一号仓");
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(500L);
        stockIn.setInNo("IN-20261019-002");
        stockIn.setStatus("DRAFT");
        stockIn.setWarehouseId(301L);
        when(stockInService.getStockIn(500L)).thenReturn(stockIn);

        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 201L, "KG", new BigDecimal("70.00"), new BigDecimal("140.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        SalesReturnRollDO roll1 = createSalesReturnRoll(10L, 101L, null, "R01", new BigDecimal("50.00"), null);
        roll1.setId(1001L);
        SalesReturnRollDO roll2 = createSalesReturnRoll(10L, 101L, null, "R02", new BigDecimal("20.00"), null);
        roll2.setId(1002L);
        when(salesReturnRollMapper.selectList(any())).thenReturn(Arrays.asList(roll1, roll2));

        when(salesReturnMapper.update(isNull(), any())).thenReturn(1);
        when(stockInService.cancelDraftStockIn(500L)).thenReturn(true);

        // 两卷均判 SCRAP
        SalesReturnReceiveReqVO reqVO = SalesReturnReceiveReqVO.builder()
                .id(10L)
                .rolls(Arrays.asList(
                        SalesReturnReceiveRollReqVO.builder().id(1001L).qcResult("SCRAP").qcRemark("报废1").build(),
                        SalesReturnReceiveRollReqVO.builder().id(1002L).qcResult("SCRAP").qcRemark("报废2").build()
                ))
                .build();

        salesReturnService.receiveSalesReturn(reqVO);

        // 1. 验证重算 restock_qty 为 0，scrap_qty 为 70.00
        ArgumentCaptor<SalesReturnItemDO> itemCaptor = ArgumentCaptor.forClass(SalesReturnItemDO.class);
        verify(salesReturnItemMapper).updateById(itemCaptor.capture());
        SalesReturnItemDO updatedItem = itemCaptor.getValue();
        assertEquals(BigDecimal.ZERO, updatedItem.getRestockQty());
        assertEquals(new BigDecimal("70.00"), updatedItem.getScrapQty());

        // 2. 验证全废分支：取消 DRAFT 入库单，严禁审核入库单
        verify(stockInService).cancelDraftStockIn(500L);
        verify(stockInService, never()).auditStockIn(any());

        // 3. 验证退货单状态依然进入 RECEIVED
        verify(salesReturnMapper).update(isNull(), any());
    }

    // ==================== Issue 06: 财务确认过账 + 应收冲减事件 ====================

    @Test
    @DisplayName("财务确认过账：退货单不存在抛出 000")
    void postSalesReturn_notExists_throwsException() {
        when(salesReturnMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.postSalesReturn(999L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("财务确认过账：非 RECEIVED 状态（如 APPROVED）拒绝过账抛出 002")
    void postSalesReturn_statusNotReceived_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.APPROVED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.postSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("财务确认过账：存在可入库卷但入库单未 COMPLETED 抛出 012")
    void postSalesReturn_hasRestockRoll_stockInNotCompleted_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.RECEIVED.getValue());
        salesReturn.setStockInId(500L);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        SalesReturnRollDO restockRoll = new SalesReturnRollDO();
        restockRoll.setId(1001L);
        restockRoll.setQcResult("RESTOCK");
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.singletonList(restockRoll));

        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(500L);
        stockIn.setStatus("DRAFT"); // 未完成
        when(stockInService.getStockIn(500L)).thenReturn(stockIn);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.postSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STOCK_IN_NOT_COMPLETED.getCode(), ex.getCode());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("财务确认过账：存在可入库卷但入库单缺失抛出 009")
    void postSalesReturn_hasRestockRoll_stockInMissing_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.RECEIVED.getValue());
        salesReturn.setStockInId(null);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        SalesReturnRollDO restockRoll = new SalesReturnRollDO();
        restockRoll.setId(1001L);
        restockRoll.setQcResult("RESTOCK");
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.singletonList(restockRoll));

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.postSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STOCK_IN_NOT_COMPLETED.getCode(), ex.getCode());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("财务确认过账：存在可入库卷且入库单已 COMPLETED，条件更新成功并发布 SalesReturnPostedEvent")
    void postSalesReturn_hasRestockRoll_stockInCompleted_success() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setReturnNo("RT-20261019-001");
        salesReturn.setShipmentId(88L);
        salesReturn.setShipmentNo("SH-20261016-001");
        salesReturn.setCustomerId(501L);
        salesReturn.setCurrency("USD");
        salesReturn.setReturnDate(LocalDate.of(2026, 10, 19));
        salesReturn.setStatus(SalesReturnStatusEnum.RECEIVED.getValue());
        salesReturn.setStockInId(500L);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        SalesReturnRollDO restockRoll = new SalesReturnRollDO();
        restockRoll.setId(1001L);
        restockRoll.setQcResult("RESTOCK");
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.singletonList(restockRoll));

        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(500L);
        stockIn.setStatus("COMPLETED");
        when(stockInService.getStockIn(500L)).thenReturn(stockIn);

        SalesReturnItemDO item1 = new SalesReturnItemDO();
        item1.setId(201L);
        item1.setReturnId(10L);
        item1.setShipmentItemId(1001L);
        item1.setReturnQty(new BigDecimal("50.00"));
        item1.setAmount(new BigDecimal("1200.00"));
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.singletonList(item1));

        when(salesReturnMapper.update(isNull(), any())).thenReturn(1);

        // Act
        salesReturnService.postSalesReturn(10L);

        // Assert
        verify(salesReturnMapper).update(isNull(), any());

        ArgumentCaptor<SalesReturnPostedEvent> eventCaptor = ArgumentCaptor.forClass(SalesReturnPostedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        SalesReturnPostedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(10L, publishedEvent.getReturnId());
        assertEquals("RT-20261019-001", publishedEvent.getReturnNo());
        assertEquals(88L, publishedEvent.getShipmentId());
        assertEquals("SH-20261016-001", publishedEvent.getShipmentNo());
        assertEquals(501L, publishedEvent.getCustomerId());
        assertEquals("USD", publishedEvent.getCurrency());
        assertEquals(LocalDate.of(2026, 10, 19), publishedEvent.getReturnDate());
        assertEquals(1, publishedEvent.getItems().size());
        assertEquals(1001L, publishedEvent.getItems().get(0).getShipmentItemId());
        assertEquals(new BigDecimal("50.00"), publishedEvent.getItems().get(0).getReturnQty());
        assertEquals(new BigDecimal("1200.00"), publishedEvent.getItems().get(0).getAmount());
    }

    @Test
    @DisplayName("财务确认过账：全废单（无 RESTOCK 卷）豁免入库单 COMPLETED 守卫，可正常过账")
    void postSalesReturn_allScrap_stockInCancelled_success() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setReturnNo("RT-20261019-002");
        salesReturn.setShipmentId(88L);
        salesReturn.setShipmentNo("SH-20261016-001");
        salesReturn.setCustomerId(501L);
        salesReturn.setCurrency("RMB");
        salesReturn.setReturnDate(LocalDate.of(2026, 10, 19));
        salesReturn.setStatus(SalesReturnStatusEnum.RECEIVED.getValue());
        salesReturn.setStockInId(500L);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        // 全部卷均为 SCRAP
        SalesReturnRollDO scrapRoll = new SalesReturnRollDO();
        scrapRoll.setId(1002L);
        scrapRoll.setQcResult("SCRAP");
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.singletonList(scrapRoll));

        SalesReturnItemDO item1 = new SalesReturnItemDO();
        item1.setId(202L);
        item1.setReturnId(10L);
        item1.setShipmentItemId(1002L);
        item1.setReturnQty(new BigDecimal("30.00"));
        item1.setAmount(new BigDecimal("600.00"));
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.singletonList(item1));

        when(salesReturnMapper.update(isNull(), any())).thenReturn(1);

        // Act
        salesReturnService.postSalesReturn(10L);

        // Assert - 不需要查询 stockIn 或断言其 COMPLETED
        verify(stockInService, never()).getStockIn(any());
        verify(eventPublisher).publishEvent(any(SalesReturnPostedEvent.class));
    }

    @Test
    @DisplayName("财务确认过账：并发双过账条件更新返回 0 行，抛出 002 拒绝")
    void postSalesReturn_concurrencyDoublePost_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.RECEIVED.getValue());
        salesReturn.setStockInId(500L);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 模拟并发更新失败（已被其他事务过账）
        when(salesReturnMapper.update(isNull(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.postSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("POSTED 终态保护：已过账单据拒绝 update 操作")
    void updateSalesReturn_postedStatus_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.POSTED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        SalesReturnUpdateReqVO updateReqVO = new SalesReturnUpdateReqVO();
        updateReqVO.setId(10L);
        updateReqVO.setWarehouseId(301L);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.updateSalesReturn(updateReqVO));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("POSTED 终态保护：已过账单据拒绝 delete 操作")
    void deleteSalesReturn_postedStatus_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.POSTED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.deleteSalesReturn(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("POSTED 终态保护：已过账单据拒绝 cancel 操作")
    void cancelSalesReturn_postedStatus_throwsException() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStatus(SalesReturnStatusEnum.POSTED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.cancelSalesReturn(10L, "原因"));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // ==================== Issue 07: 打印数据查询单测 ====================

    @Test
    @DisplayName("打印数据：单据不存在抛 000")
    void getSalesReturnPrintData_notExists_throwsException() {
        when(salesReturnMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.getSalesReturnPrintData(999L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("打印数据：CANCELLED 单据拒绝打印抛 002")
    void getSalesReturnPrintData_cancelled_throwsException() {
        SalesReturnDO cancelled = new SalesReturnDO();
        cancelled.setId(10L);
        cancelled.setStatus(SalesReturnStatusEnum.CANCELLED.getValue());
        when(salesReturnMapper.selectById(10L)).thenReturn(cancelled);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesReturnService.getSalesReturnPrintData(10L));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("打印数据：正常组装客户联系人、入库单、明细及卷清单")
    void getSalesReturnPrintData_success() {
        SalesReturnDO salesReturn = createDraftSalesReturn(10L, 88L);
        salesReturn.setStatus("APPROVED");
        salesReturn.setApprovalStatus("APPROVED");
        salesReturn.setCustomerId(501L);
        salesReturn.setStockInId(601L);
        salesReturn.setTotalAmount(new BigDecimal("2000.00"));
        salesReturn.setTotalPieces(2);
        when(salesReturnMapper.selectById(10L)).thenReturn(salesReturn);

        // 客户档案
        CustomerDO customer = new CustomerDO();
        customer.setId(501L);
        customer.setCompanyName("测试客户");
        customer.setPhone("0575-88888888");
        customer.setAddress("浙江省绍兴市柯桥区轻纺城大道1号");
        when(customerMapper.selectById(501L)).thenReturn(customer);

        // 客户联系人
        CustomerContactDO contact = new CustomerContactDO();
        contact.setId(1L);
        contact.setCustomerId(501L);
        contact.setName("张三");
        contact.setPhone("13800000000");
        contact.setIsPrimary(1);
        when(customerContactMapper.selectList(any())).thenReturn(Collections.singletonList(contact));

        // 入库单
        StockInRespVO stockIn = new StockInRespVO();
        stockIn.setId(601L);
        stockIn.setInNo("IN-20261019-001");
        stockIn.setStatus("DRAFT");
        when(stockInService.getStockIn(601L)).thenReturn(stockIn);

        // 退货明细与卷
        SalesReturnItemDO item = createSalesReturnItem(10L, 101L, 201L, "KG", new BigDecimal("50.00"), new BigDecimal("100.00"), "QUALITY");
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        SalesReturnRollDO roll = createSalesReturnRoll(10L, 101L, null, "R01", new BigDecimal("50.00"), null);
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.singletonList(roll));

        SalesReturnPrintRespVO printVO = salesReturnService.getSalesReturnPrintData(10L);

        assertNotNull(printVO);
        assertEquals(10L, printVO.getId());
        assertEquals("RT-20261018-001", printVO.getReturnNo());
        assertEquals("APPROVED", printVO.getStatus());
        assertEquals("测试客户", printVO.getCustomerName());
        assertEquals("张三", printVO.getContactName());
        assertEquals("13800000000", printVO.getContactPhone());
        assertEquals("浙江省绍兴市柯桥区轻纺城大道1号", printVO.getCustomerAddress());
        assertEquals("IN-20261019-001", printVO.getStockInNo());
        assertEquals("DRAFT", printVO.getStockInStatus());
        assertEquals(1, printVO.getItems().size());
        assertEquals(1, printVO.getItems().get(0).getRolls().size());
        assertEquals("R01", printVO.getItems().get(0).getRolls().get(0).getRollNo());
        assertEquals(new BigDecimal("100.00"), printVO.getItems().get(0).getAmount());
        assertNotNull(printVO.getPrintTime());
    }

}

