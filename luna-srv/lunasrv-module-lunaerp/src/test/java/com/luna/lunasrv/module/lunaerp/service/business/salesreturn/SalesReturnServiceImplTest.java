package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.business.salesreturn.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnReasonEnum;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnStatusEnum;
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
    private SalesReturnAvailabilityService availabilityService;

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

}
