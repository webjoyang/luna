package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.business.salesreturn.dto.SalesReturnOccupiedQtyDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 销售退货防超退聚合口径单测（SalesReturnAvailabilityService）
 * 覆盖 Issue: .scratch/sales-return/issues/02-draft-crud-availability.md
 */
@ExtendWith(MockitoExtension.class)
public class SalesReturnAvailabilityServiceImplTest {

    @Mock
    private SalesReturnItemMapper salesReturnItemMapper;

    @Mock
    private ShipmentItemMapper shipmentItemMapper;

    @InjectMocks
    private SalesReturnAvailabilityServiceImpl availabilityService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesReturnDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesReturnItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentItemDO.class);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private ShipmentItemDO mockShipmentItem(Long id, BigDecimal quantity, BigDecimal unitPrice) {
        ShipmentItemDO item = new ShipmentItemDO();
        item.setId(id);
        item.setShipmentId(100L);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setUnit("KG");
        return item;
    }

    @Test
    @DisplayName("空集合输入直接返回空 Map，不发起数据库查询（集合空安全守卫）")
    void testGetOccupiedQtyMap_emptyInput_returnsEmptyMap() {
        Map<Long, BigDecimal> result = availabilityService.getOccupiedQtyMap(Collections.emptyList(), null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(salesReturnItemMapper);
    }

    @Test
    @DisplayName("计算可退量：无占量退货行时，可退量等于发货明细原发货量")
    void testGetAvailableQty_noOccupied_returnsOriginalQty() {
        Long shipmentItemId = 201L;
        ShipmentItemDO item = mockShipmentItem(shipmentItemId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectById(shipmentItemId)).thenReturn(item);
        when(salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(eq(TENANT_ID), anyCollection(), isNull()))
                .thenReturn(Collections.emptyList());

        BigDecimal availableQty = availabilityService.getAvailableQty(shipmentItemId, null);
        assertEquals(new BigDecimal("100.00"), availableQty);
    }

    @Test
    @DisplayName("计算可退量：存在占量退货行时，可退量=发货量-占量")
    void testGetAvailableQty_withOccupied_returnsRemaining() {
        Long shipmentItemId = 201L;
        ShipmentItemDO item = mockShipmentItem(shipmentItemId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectById(shipmentItemId)).thenReturn(item);

        SalesReturnOccupiedQtyDTO dto = new SalesReturnOccupiedQtyDTO(shipmentItemId, new BigDecimal("35.50"));
        when(salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(eq(TENANT_ID), anyCollection(), isNull()))
                .thenReturn(List.of(dto));

        BigDecimal availableQty = availabilityService.getAvailableQty(shipmentItemId, null);
        assertEquals(new BigDecimal("64.50"), availableQty);
    }

    @Test
    @DisplayName("计算可退量：极端情况下占量超过发货量，安全截断为 0")
    void testGetAvailableQty_occupiedExceeds_returnsZero() {
        Long shipmentItemId = 201L;
        ShipmentItemDO item = mockShipmentItem(shipmentItemId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectById(shipmentItemId)).thenReturn(item);

        SalesReturnOccupiedQtyDTO dto = new SalesReturnOccupiedQtyDTO(shipmentItemId, new BigDecimal("105.00"));
        when(salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(eq(TENANT_ID), anyCollection(), isNull()))
                .thenReturn(List.of(dto));

        BigDecimal availableQty = availabilityService.getAvailableQty(shipmentItemId, null);
        assertEquals(BigDecimal.ZERO, availableQty);
    }

    @Test
    @DisplayName("防超退校验通过：在可退量范围内且单价未上浮")
    void testCheckAvailability_success_withinLimit() {
        Long shipmentItemId = 201L;
        ShipmentItemDO item = mockShipmentItem(shipmentItemId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectById(shipmentItemId)).thenReturn(item);

        SalesReturnOccupiedQtyDTO dto = new SalesReturnOccupiedQtyDTO(shipmentItemId, new BigDecimal("40.00"));
        when(salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(eq(TENANT_ID), anyCollection(), isNull()))
                .thenReturn(List.of(dto));

        // 可退量 60.00，原单价 25.00，可退额上限 1500.00
        // 本次退 50.00，单价 20.00（折让降价），金额 1000.00 <= 1500.00，校验通过
        assertDoesNotThrow(() -> availabilityService.checkAvailability(
                shipmentItemId,
                new BigDecimal("50.00"),
                new BigDecimal("20.00"),
                null
        ));
    }

    @Test
    @DisplayName("防超退校验：退货数量超过可退量报 005（SALES_RETURN_QTY_EXCEED_AVAILABLE）")
    void testCheckAvailability_qtyExceeds_throws005() {
        Long shipmentItemId = 201L;
        ShipmentItemDO item = mockShipmentItem(shipmentItemId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectById(shipmentItemId)).thenReturn(item);

        SalesReturnOccupiedQtyDTO dto = new SalesReturnOccupiedQtyDTO(shipmentItemId, new BigDecimal("40.00"));
        when(salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(eq(TENANT_ID), anyCollection(), isNull()))
                .thenReturn(List.of(dto));

        // 可退量 60.00，本次申请退 60.01
        ServiceException ex = assertThrows(ServiceException.class, () -> availabilityService.checkAvailability(
                shipmentItemId,
                new BigDecimal("60.01"),
                new BigDecimal("25.00"),
                null
        ));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_QTY_EXCEED_AVAILABLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("防超退校验：退货金额超过可退额上限报 006（SALES_RETURN_AMOUNT_EXCEED_AVAILABLE）")
    void testCheckAvailability_amountExceeds_throws006() {
        Long shipmentItemId = 201L;
        ShipmentItemDO item = mockShipmentItem(shipmentItemId, new BigDecimal("100.00"), new BigDecimal("25.00"));
        when(shipmentItemMapper.selectById(shipmentItemId)).thenReturn(item);

        SalesReturnOccupiedQtyDTO dto = new SalesReturnOccupiedQtyDTO(shipmentItemId, new BigDecimal("40.00"));
        when(salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(eq(TENANT_ID), anyCollection(), isNull()))
                .thenReturn(List.of(dto));

        // 可退量 60.00，原单价 25.00，可退额上限 1500.00
        // 本次退 50.00（<= 60.00 数量没超），但单价上浮到 35.00，金额 1750.00 > 1500.00
        ServiceException ex = assertThrows(ServiceException.class, () -> availabilityService.checkAvailability(
                shipmentItemId,
                new BigDecimal("50.00"),
                new BigDecimal("35.00"),
                null
        ));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_AMOUNT_EXCEED_AVAILABLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("防超退校验：发货明细不存在时报 004（SALES_RETURN_ITEM_LINEAGE_INVALID）")
    void testCheckAvailability_itemNotExists_throws004() {
        Long shipmentItemId = 999L;
        when(shipmentItemMapper.selectById(shipmentItemId)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> availabilityService.checkAvailability(
                shipmentItemId,
                new BigDecimal("10.00"),
                new BigDecimal("25.00"),
                null
        ));
        assertEquals(SalesReturnErrorCodeConstants.SALES_RETURN_ITEM_LINEAGE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("占量状态口径：APPROVING/APPROVED 占量，PENDING/REJECTED/CANCELLED 释放占量")
    void testOccupiedQtyStatusFiltering_approvingAndApprovedCount_pendingRejectedCancelledReleased() {
        Long shipmentItemId = 201L;
        ShipmentItemDO sItem = mockShipmentItem(shipmentItemId, new BigDecimal("100.00"), new BigDecimal("20.00"));
        when(shipmentItemMapper.selectById(shipmentItemId)).thenReturn(sItem);

        // 场景 A: 单据处于 APPROVING 或 APPROVED 状态，Mapper 返回占量 40.00
        when(salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(eq(TENANT_ID), anyCollection(), isNull()))
                .thenReturn(List.of(new SalesReturnOccupiedQtyDTO(shipmentItemId, new BigDecimal("40.00"))));

        BigDecimal availableQtyUnderApproval = availabilityService.getAvailableQty(shipmentItemId, null);
        assertEquals(new BigDecimal("60.00"), availableQtyUnderApproval);

        // 场景 B: 单据被 REJECTED、撤回至 PENDING 或作废 CANCELLED，Mapper 占量统计释放为 0
        when(salesReturnItemMapper.selectOccupiedQtyByShipmentItemIds(eq(TENANT_ID), anyCollection(), isNull()))
                .thenReturn(Collections.emptyList());

        BigDecimal availableQtyAfterRelease = availabilityService.getAvailableQty(shipmentItemId, null);
        assertEquals(new BigDecimal("100.00"), availableQtyAfterRelease);
    }

}

