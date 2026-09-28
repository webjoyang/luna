package com.luna.lunasrv.module.lunaerp.service.business.shipment;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.enums.CommonStatusEnum;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.service.stock.stockledger.StockLedgerService;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ShipmentServiceImpl} 纯 Mockito 单元测试
 */
class ShipmentServiceImplTest {

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private ShipmentItemMapper shipmentItemMapper;

    @Mock
    private ShipmentRollMapper shipmentRollMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private SalesContractItemMapper salesContractItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StockInItemMapper stockInItemMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private StockLedgerService stockLedgerService;

    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentRollDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockLedgerDO.class);
        when(securityFrameworkService.hasPermission(anyString())).thenReturn(true);

        when(shipmentMapper.insert(any(ShipmentDO.class))).thenAnswer(invocation -> {
            ShipmentDO doObj = invocation.getArgument(0);
            doObj.setId(10L);
            return 1;
        });
        when(shipmentMapper.updateById(any(ShipmentDO.class))).thenReturn(1);
        when(shipmentItemMapper.insert(any(ShipmentItemDO.class))).thenAnswer(invocation -> {
            ShipmentItemDO doObj = invocation.getArgument(0);
            if (doObj.getId() == null) {
                doObj.setId(100L);
            }
            return 1;
        });
        when(shipmentRollMapper.insert(any(ShipmentRollDO.class))).thenAnswer(invocation -> {
            ShipmentRollDO doObj = invocation.getArgument(0);
            if (doObj.getId() == null) {
                doObj.setId(1000L);
            }
            return 1;
        });
    }

    private CustomerDO createCustomer(Long id, String name, String status) {
        CustomerDO c = new CustomerDO();
        c.setId(id);
        c.setCompanyName(name);
        c.setStatus(status);
        return c;
    }

    private WarehouseDO createWarehouse(Long id, String name, String status) {
        WarehouseDO w = new WarehouseDO();
        w.setId(id);
        w.setName(name);
        w.setStatus(status);
        return w;
    }

    private SalesContractDO createContract(Long id, String contractNo) {
        SalesContractDO c = new SalesContractDO();
        c.setId(id);
        c.setContractNo(contractNo);
        return c;
    }

    private AdminUserRespDTO createUser(Long id, String nickname, Integer status) {
        AdminUserRespDTO u = new AdminUserRespDTO();
        u.setId(id);
        u.setNickname(nickname);
        u.setStatus(status);
        return u;
    }

    private ProductDO createProduct(Long id, String code, String name) {
        ProductDO p = new ProductDO();
        p.setId(id);
        p.setCode(code);
        p.setName(name);
        return p;
    }

    // ==================== 1. 创建发货单测试 ====================

    @Test
    @DisplayName("创建发货单：自动生成单号、回填快照、重算金额与匹数、保存明细与卷")
    void testCreateShipment_success_withAutoNoAndSnapshotsAndRecalculate() {
        // Arrange
        CustomerDO customer = createCustomer(1L, "绍兴某某纺织有限公司", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "成品一号仓", "ACTIVE");
        SalesContractDO contract = createContract(3L, "CH20260001");
        AdminUserRespDTO user = createUser(4L, "李业务", CommonStatusEnum.ENABLE.getStatus());
        ProductDO product = createProduct(5L, "PRD-COTTON", "全棉斜纹面料");
        StockInItemDO stockInItem = new StockInItemDO();
        stockInItem.setId(6L);

        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(salesContractMapper.selectById(3L)).thenReturn(contract);
        when(adminUserApi.getUser(4L)).thenReturn(user);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(stockInItemMapper.selectById(6L)).thenReturn(stockInItem);
        when(shipmentMapper.selectListByShipmentNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());
        when(shipmentMapper.selectByShipmentNoIncludingDeleted(anyString())).thenReturn(null);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setSalesContractId(3L);
        req.setSalespersonUserId(4L);
        req.setShippingDate(LocalDate.of(2026, 9, 27));

        ShipmentItemSaveReqVO itemReq = new ShipmentItemSaveReqVO();
        itemReq.setProductId(5L);
        itemReq.setCustomerColorNo("WH-01");
        itemReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setPieces(5);
        itemReq.setUnit("KG");
        itemReq.setUnitPrice(new BigDecimal("20.00"));
        itemReq.setSourceStockInItemId(6L);

        ShipmentRollSaveReqVO rollReq = new ShipmentRollSaveReqVO();
        rollReq.setRollNo("R001");
        rollReq.setWeight(new BigDecimal("20.00"));
        rollReq.setLengthM(new BigDecimal("50.00"));
        itemReq.setRolls(List.of(rollReq));

        req.setItems(List.of(itemReq));

        // Act
        Long shipmentId = shipmentService.createShipment(req);

        // Assert
        assertEquals(10L, shipmentId);

        ArgumentCaptor<ShipmentDO> shipmentCaptor = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).insert(shipmentCaptor.capture());
        ShipmentDO savedShipment = shipmentCaptor.getValue();

        assertEquals("DRAFT", savedShipment.getStatus());
        assertEquals("PENDING", savedShipment.getSignStatus());
        assertEquals("RMB", savedShipment.getCurrency());
        assertEquals("SH-20260927-001", savedShipment.getShipmentNo());
        assertEquals("绍兴某某纺织有限公司", savedShipment.getCustomerName());
        assertEquals("成品一号仓", savedShipment.getWarehouseName());
        assertEquals("CH20260001", savedShipment.getContractNo());
        assertEquals("李业务", savedShipment.getSalespersonName());
        assertEquals(new BigDecimal("2000.00"), savedShipment.getTotalAmount());
        assertEquals(5, savedShipment.getTotalPieces());

        ArgumentCaptor<ShipmentItemDO> itemCaptor = ArgumentCaptor.forClass(ShipmentItemDO.class);
        verify(shipmentItemMapper).insert(itemCaptor.capture());
        ShipmentItemDO savedItem = itemCaptor.getValue();
        assertEquals(10L, savedItem.getShipmentId());
        assertEquals("PRD-COTTON", savedItem.getProductCode());
        assertEquals("全棉斜纹面料", savedItem.getProductName());
        assertEquals(new BigDecimal("2000.00"), savedItem.getAmount());
        assertEquals(6L, savedItem.getSourceStockInItemId());

        ArgumentCaptor<ShipmentRollDO> rollCaptor = ArgumentCaptor.forClass(ShipmentRollDO.class);
        verify(shipmentRollMapper).insert(rollCaptor.capture());
        ShipmentRollDO savedRoll = rollCaptor.getValue();
        assertEquals(10L, savedRoll.getShipmentId());
        assertEquals(100L, savedRoll.getShipmentItemId());
        assertEquals("R001", savedRoll.getRollNo());
    }

    @Test
    @DisplayName("创建发货单：允许零明细草稿，totalAmount为0，totalPieces为0")
    void testCreateShipment_success_zeroItems() {
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(shipmentMapper.selectListByShipmentNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setItems(Collections.emptyList());

        Long shipmentId = shipmentService.createShipment(req);
        assertEquals(10L, shipmentId);

        ArgumentCaptor<ShipmentDO> captor = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).insert(captor.capture());
        ShipmentDO saved = captor.getValue();
        assertEquals(BigDecimal.ZERO, saved.getTotalAmount());
        assertEquals(0, saved.getTotalPieces());
        verify(shipmentItemMapper, never()).insert(any(ShipmentItemDO.class));
        verify(shipmentRollMapper, never()).insert(any(ShipmentRollDO.class));
    }

    @Test
    @DisplayName("创建发货单：手填单号已存在（含已删除行），报 SHIPMENT_NO_DUPLICATE")
    void testCreateShipment_duplicateShipmentNo_throwsException() {
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);

        ShipmentDO existing = new ShipmentDO();
        existing.setId(99L);
        existing.setShipmentNo("SH-MANUAL-001");
        when(shipmentMapper.selectByShipmentNoIncludingDeleted("SH-MANUAL-001")).thenReturn(existing);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setShipmentNo("SH-MANUAL-001");

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_NO_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建发货单：客户不存在或停用，报 SHIPMENT_RELATED_REF_NOT_EXISTS")
    void testCreateShipment_customerNotExistsOrInactive_throwsException() {
        when(customerMapper.selectById(1L)).thenReturn(null);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex.getCode());

        CustomerDO inactiveCustomer = createCustomer(1L, "停用客户", "INACTIVE");
        when(customerMapper.selectById(1L)).thenReturn(inactiveCustomer);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("创建发货单：仓库不存在或停用，报 SHIPMENT_RELATED_REF_NOT_EXISTS")
    void testCreateShipment_warehouseNotExistsOrInactive_throwsException() {
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(null);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex.getCode());

        WarehouseDO inactiveWarehouse = createWarehouse(2L, "停用仓库", "INACTIVE");
        when(warehouseMapper.selectById(2L)).thenReturn(inactiveWarehouse);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("创建发货单：销售合同不存在，报 SHIPMENT_RELATED_REF_NOT_EXISTS")
    void testCreateShipment_salesContractNotExists_throwsException() {
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(salesContractMapper.selectById(3L)).thenReturn(null);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setSalesContractId(3L);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建发货单：业务员不存在或停用，报 SHIPMENT_RELATED_REF_NOT_EXISTS")
    void testCreateShipment_salespersonNotExistsOrDisabled_throwsException() {
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(adminUserApi.getUser(4L)).thenReturn(null);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setSalespersonUserId(4L);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex.getCode());

        AdminUserRespDTO disabledUser = createUser(4L, "停用业务员", CommonStatusEnum.DISABLE.getStatus());
        when(adminUserApi.getUser(4L)).thenReturn(disabledUser);
        ServiceException ex2 = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex2.getCode());
    }

    @Test
    @DisplayName("创建发货单：明细产品不存在，报 SHIPMENT_RELATED_REF_NOT_EXISTS")
    void testCreateShipment_productNotExists_throwsException() {
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(productMapper.selectById(5L)).thenReturn(null);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        ShipmentItemSaveReqVO itemReq = new ShipmentItemSaveReqVO();
        itemReq.setProductId(5L);
        itemReq.setQuantity(new BigDecimal("10.00"));
        itemReq.setUnit("KG");
        req.setItems(List.of(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建发货单：来源入库明细不存在，报 SHIPMENT_RELATED_REF_NOT_EXISTS")
    void testCreateShipment_sourceStockInItemNotExists_throwsException() {
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        ProductDO product = createProduct(5L, "P1", "面料1");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(stockInItemMapper.selectById(6L)).thenReturn(null);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        ShipmentItemSaveReqVO itemReq = new ShipmentItemSaveReqVO();
        itemReq.setProductId(5L);
        itemReq.setQuantity(new BigDecimal("10.00"));
        itemReq.setUnit("KG");
        itemReq.setSourceStockInItemId(6L);
        req.setItems(List.of(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ==================== 2. 更新发货单测试 ====================

    @Test
    @DisplayName("更新发货单：草稿态允许更新，替换明细与卷清单，重算金额与匹数")
    void testUpdateShipment_success() {
        ShipmentDO existing = new ShipmentDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setShipmentNo("SH-20260927-001");
        when(shipmentMapper.selectById(10L)).thenReturn(existing);

        CustomerDO customer = createCustomer(1L, "绍兴纺织", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "一号仓", "ACTIVE");
        ProductDO product = createProduct(5L, "P1", "纯棉");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(productMapper.selectById(5L)).thenReturn(product);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setId(10L);
        req.setCustomerId(1L);
        req.setWarehouseId(2L);

        ShipmentItemSaveReqVO itemReq = new ShipmentItemSaveReqVO();
        itemReq.setProductId(5L);
        itemReq.setQuantity(new BigDecimal("50.00"));
        itemReq.setUnitPrice(new BigDecimal("30.00"));
        itemReq.setPieces(3);
        itemReq.setUnit("KG");

        ShipmentRollSaveReqVO rollReq = new ShipmentRollSaveReqVO();
        rollReq.setRollNo("R002");
        rollReq.setWeight(new BigDecimal("50.00"));
        itemReq.setRolls(List.of(rollReq));

        req.setItems(List.of(itemReq));

        // Act
        shipmentService.updateShipment(req);

        // Assert
        verify(shipmentMapper).updateById(any(ShipmentDO.class));
        verify(shipmentRollMapper).deleteByShipmentId(10L);
        verify(shipmentItemMapper).deleteByShipmentId(10L);
        verify(shipmentItemMapper).insert(any(ShipmentItemDO.class));
        verify(shipmentRollMapper).insert(any(ShipmentRollDO.class));
    }

    @Test
    @DisplayName("更新发货单：非草稿态（已发货或已取消）禁止更新，报 SHIPMENT_STATUS_NOT_ALLOW")
    void testUpdateShipment_notDraft_throwsException() {
        ShipmentDO existing = new ShipmentDO();
        existing.setId(10L);
        existing.setStatus("COMPLETED");
        when(shipmentMapper.selectById(10L)).thenReturn(existing);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setId(10L);
        req.setCustomerId(1L);
        req.setWarehouseId(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.updateShipment(req));
        assertEquals(SHIPMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新发货单：修改单号与其他已有单据撞号，报 SHIPMENT_NO_DUPLICATE")
    void testUpdateShipment_duplicateShipmentNo_throwsException() {
        ShipmentDO existing = new ShipmentDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        existing.setShipmentNo("SH-20260927-001");
        when(shipmentMapper.selectById(10L)).thenReturn(existing);

        CustomerDO customer = createCustomer(1L, "绍兴纺织", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "一号仓", "ACTIVE");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);

        ShipmentDO another = new ShipmentDO();
        another.setId(20L);
        another.setShipmentNo("SH-EXISTING");
        when(shipmentMapper.selectByShipmentNoIncludingDeleted("SH-EXISTING")).thenReturn(another);

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setId(10L);
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setShipmentNo("SH-EXISTING");

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.updateShipment(req));
        assertEquals(SHIPMENT_NO_DUPLICATE.getCode(), ex.getCode());
    }

    // ==================== 3. 删除发货单测试 ====================

    @Test
    @DisplayName("删除发货单：仅草稿态允许删除，级联逻辑删除明细与卷清单")
    void testDeleteShipment_success() {
        ShipmentDO existing = new ShipmentDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        when(shipmentMapper.selectById(10L)).thenReturn(existing);

        // Act
        shipmentService.deleteShipment(10L);

        // Assert
        verify(shipmentMapper).deleteById(10L);
        verify(shipmentItemMapper).deleteByShipmentId(10L);
        verify(shipmentRollMapper).deleteByShipmentId(10L);
    }

    @Test
    @DisplayName("删除发货单：非草稿态禁止删除，报 SHIPMENT_STATUS_NOT_ALLOW")
    void testDeleteShipment_notDraft_throwsException() {
        ShipmentDO existing = new ShipmentDO();
        existing.setId(10L);
        existing.setStatus("COMPLETED");
        when(shipmentMapper.selectById(10L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.deleteShipment(10L));
        assertEquals(SHIPMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());

        verify(shipmentMapper, never()).deleteById(any());
        verify(shipmentItemMapper, never()).deleteByShipmentId(any());
        verify(shipmentRollMapper, never()).deleteByShipmentId(any());
    }

    // ==================== 4. 详情与预览测试 ====================

    @Test
    @DisplayName("获得发货单：返回包含明细及卷清单的树形结构")
    void testGetShipment_success() {
        ShipmentDO existing = new ShipmentDO();
        existing.setId(10L);
        existing.setShipmentNo("SH-20260927-001");
        existing.setStatus("DRAFT");
        when(shipmentMapper.selectById(10L)).thenReturn(existing);

        ShipmentItemDO item = new ShipmentItemDO();
        item.setId(100L);
        item.setShipmentId(10L);
        item.setProductId(5L);
        item.setQuantity(new BigDecimal("100.00"));
        when(shipmentItemMapper.selectListByShipmentId(10L)).thenReturn(List.of(item));

        ShipmentRollDO roll = new ShipmentRollDO();
        roll.setId(1000L);
        roll.setShipmentId(10L);
        roll.setShipmentItemId(100L);
        roll.setRollNo("R001");
        when(shipmentRollMapper.selectListByShipmentId(10L)).thenReturn(List.of(roll));

        ShipmentRespVO resp = shipmentService.getShipment(10L);
        assertNotNull(resp);
        assertEquals("SH-20260927-001", resp.getShipmentNo());
        assertEquals(1, resp.getItems().size());
        assertEquals(1, resp.getItems().get(0).getRolls().size());
        assertEquals("R001", resp.getItems().get(0).getRolls().get(0).getRollNo());
    }

    @Test
    @DisplayName("单号预览：当天第一个为 SH-YYYYMMDD-001，有号则顺延")
    void testGetNextShipmentNo_success() {
        LocalDate date = LocalDate.of(2026, 9, 27);
        when(shipmentMapper.selectListByShipmentNoPrefixIncludingDeleted("SH-20260927-"))
                .thenReturn(Collections.emptyList());

        String nextNo = shipmentService.getNextShipmentNo(date);
        assertEquals("SH-20260927-001", nextNo);

        ShipmentDO existing = new ShipmentDO();
        existing.setShipmentNo("SH-20260927-005");
        when(shipmentMapper.selectListByShipmentNoPrefixIncludingDeleted("SH-20260927-"))
                .thenReturn(List.of(existing));

        String nextNo2 = shipmentService.getNextShipmentNo(date);
        assertEquals("SH-20260927-006", nextNo2);
    }

    // ==================== 5. Issue 03 - 卷清单聚合保存与级联删除 ====================

    @Test
    @DisplayName("卷清单聚合保存：同一发货单两条明细各含不同数量的卷，卷 shipmentItemId 正确绑定")
    void testRollAggregation_multipleItemsMultipleRolls_bindingIsCorrect() {
        // Arrange
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        ProductDO product = createProduct(5L, "P1", "面料");

        // 两条明细的 insert 依次返回不同 ID，模拟序列
        final long[] itemIdSeq = {100L, 200L};
        final int[] insertCallCount = {0};
        when(shipmentItemMapper.insert(any(ShipmentItemDO.class))).thenAnswer(inv -> {
            ShipmentItemDO item = inv.getArgument(0);
            item.setId(itemIdSeq[insertCallCount[0]++]);
            return 1;
        });

        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(shipmentMapper.selectListByShipmentNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());
        when(shipmentMapper.selectByShipmentNoIncludingDeleted(anyString())).thenReturn(null);

        // 明细1：2 卷
        ShipmentItemSaveReqVO itemReq1 = new ShipmentItemSaveReqVO();
        itemReq1.setProductId(5L);
        itemReq1.setQuantity(new BigDecimal("50.00"));
        itemReq1.setUnit("KG");
        ShipmentRollSaveReqVO roll1a = new ShipmentRollSaveReqVO();
        roll1a.setRollNo("R001");
        roll1a.setWeight(new BigDecimal("25.00"));
        ShipmentRollSaveReqVO roll1b = new ShipmentRollSaveReqVO();
        roll1b.setRollNo("R002");
        roll1b.setWeight(new BigDecimal("25.00"));
        itemReq1.setRolls(List.of(roll1a, roll1b));

        // 明细2：1 卷
        ShipmentItemSaveReqVO itemReq2 = new ShipmentItemSaveReqVO();
        itemReq2.setProductId(5L);
        itemReq2.setQuantity(new BigDecimal("30.00"));
        itemReq2.setUnit("KG");
        ShipmentRollSaveReqVO roll2a = new ShipmentRollSaveReqVO();
        roll2a.setRollNo("R003");
        roll2a.setWeight(new BigDecimal("30.00"));
        itemReq2.setRolls(List.of(roll2a));

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setItems(List.of(itemReq1, itemReq2));

        // Act
        shipmentService.createShipment(req);

        // Assert：共 3 次卷 insert，各卷 shipmentItemId 正确
        ArgumentCaptor<ShipmentRollDO> rollCaptor = ArgumentCaptor.forClass(ShipmentRollDO.class);
        verify(shipmentRollMapper, times(3)).insert(rollCaptor.capture());

        List<ShipmentRollDO> savedRolls = rollCaptor.getAllValues();
        // 前两卷属于明细1 (id=100)
        assertEquals(100L, savedRolls.get(0).getShipmentItemId());
        assertEquals("R001", savedRolls.get(0).getRollNo());
        assertEquals(100L, savedRolls.get(1).getShipmentItemId());
        assertEquals("R002", savedRolls.get(1).getRollNo());
        // 第三卷属于明细2 (id=200)
        assertEquals(200L, savedRolls.get(2).getShipmentItemId());
        assertEquals("R003", savedRolls.get(2).getRollNo());
    }

    @Test
    @DisplayName("卷清单聚合保存：零卷清单明细行不插入任何卷记录")
    void testRollAggregation_itemWithNoRolls_noRollInserted() {
        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        ProductDO product = createProduct(5L, "P1", "面料");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(productMapper.selectById(5L)).thenReturn(product);
        when(shipmentMapper.selectListByShipmentNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());
        when(shipmentMapper.selectByShipmentNoIncludingDeleted(anyString())).thenReturn(null);

        ShipmentItemSaveReqVO itemReq = new ShipmentItemSaveReqVO();
        itemReq.setProductId(5L);
        itemReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setUnit("KG");
        itemReq.setRolls(Collections.emptyList()); // 零卷

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setItems(List.of(itemReq));

        shipmentService.createShipment(req);

        // 有明细插入，但无卷记录
        verify(shipmentItemMapper, times(1)).insert(any(ShipmentItemDO.class));
        verify(shipmentRollMapper, never()).insert(any(ShipmentRollDO.class));
    }

    @Test
    @DisplayName("卷清单整表替换：更新发货单时先 deleteByShipmentId 再重新插入新卷记录")
    void testRollAggregation_update_deleteAllThenReinsert() {
        ShipmentDO existing = new ShipmentDO();
        existing.setId(10L);
        existing.setStatus("DRAFT");
        when(shipmentMapper.selectById(10L)).thenReturn(existing);

        CustomerDO customer = createCustomer(1L, "某客户", "ACTIVE");
        WarehouseDO warehouse = createWarehouse(2L, "某仓", "ACTIVE");
        ProductDO product = createProduct(5L, "P1", "面料");
        when(customerMapper.selectById(1L)).thenReturn(customer);
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(productMapper.selectById(5L)).thenReturn(product);

        ShipmentRollSaveReqVO rollReq = new ShipmentRollSaveReqVO();
        rollReq.setRollNo("R-NEW");
        rollReq.setWeight(new BigDecimal("40.00"));

        ShipmentItemSaveReqVO itemReq = new ShipmentItemSaveReqVO();
        itemReq.setProductId(5L);
        itemReq.setQuantity(new BigDecimal("40.00"));
        itemReq.setUnit("KG");
        itemReq.setRolls(List.of(rollReq));

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setId(10L);
        req.setCustomerId(1L);
        req.setWarehouseId(2L);
        req.setItems(List.of(itemReq));

        // Act
        shipmentService.updateShipment(req);

        // Assert：先删除全量卷，再插入新卷
        verify(shipmentRollMapper).deleteByShipmentId(10L);
        verify(shipmentRollMapper).insert(any(ShipmentRollDO.class));
    }

    // ==================== 6. Issue 05 - 从合同明细带入与实时合同未发量 ====================

    private SalesContractItemDO createContractItem(Long id, Long contractId, int lineNo,
                                                  String productCode, String productName,
                                                  String customerColor, BigDecimal quantity,
                                                  BigDecimal unitPrice, String remark) {
        SalesContractItemDO item = new SalesContractItemDO();
        item.setId(id);
        item.setContractId(contractId);
        item.setLineNo(lineNo);
        item.setProductCode(productCode);
        item.setProductName(productName);
        item.setCustomerColor(customerColor);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setRemark(remark);
        return item;
    }

    @Test
    @DisplayName("从合同带入：销售合同不存在报 SHIPMENT_RELATED_REF_NOT_EXISTS (1_030_016_003)")
    void testFromContract_contractNotExists_throwsException() {
        when(salesContractMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.getShipmentItemsFromContract(999L));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("从合同带入：合同存在但明细为空，返回空列表")
    void testFromContract_emptyItems_returnsEmptyList() {
        SalesContractDO contract = createContract(3L, "CH20260001");
        when(salesContractMapper.selectById(3L)).thenReturn(contract);
        when(salesContractItemMapper.selectListByContractId(3L)).thenReturn(Collections.emptyList());

        List<ShipmentItemRespVO> result = shipmentService.getShipmentItemsFromContract(3L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("从合同带入：无已生效出库单时，未发量等于合同数量，回填产品快照与单价，且不写库")
    void testFromContract_success_unshippedQuantityEqualsContractQuantityWhenNoCompletedShipments() {
        // Arrange
        SalesContractDO contract = createContract(3L, "CH20260001");
        contract.setUnit("M");
        when(salesContractMapper.selectById(3L)).thenReturn(contract);

        SalesContractItemDO item1 = createContractItem(1L, 3L, 1, "PRD-01", "面料A", "RED",
                new BigDecimal("100.00"), new BigDecimal("25.00"), "首批交货");
        SalesContractItemDO item2 = createContractItem(2L, 3L, 2, "PRD-02", "面料B", "BLUE",
                new BigDecimal("50.00"), new BigDecimal("15.00"), null);
        when(salesContractItemMapper.selectListByContractId(3L)).thenReturn(List.of(item1, item2));

        ProductDO product1 = createProduct(501L, "PRD-01", "面料A");
        ProductDO product2 = createProduct(502L, "PRD-02", "面料B");
        when(productMapper.selectByCode("PRD-01")).thenReturn(product1);
        when(productMapper.selectByCode("PRD-02")).thenReturn(product2);

        // 无已生效出库的发货单
        when(shipmentMapper.selectListByContractIdAndStatus(3L, "COMPLETED")).thenReturn(Collections.emptyList());

        // Act
        List<ShipmentItemRespVO> result = shipmentService.getShipmentItemsFromContract(3L);

        // Assert
        assertEquals(2, result.size());

        ShipmentItemRespVO resp1 = result.get(0);
        assertEquals(501L, resp1.getProductId());
        assertEquals("PRD-01", resp1.getProductCode());
        assertEquals("面料A", resp1.getProductName());
        assertEquals("RED", resp1.getCustomerColorNo());
        assertEquals(new BigDecimal("100.00"), resp1.getQuantity());
        assertEquals(new BigDecimal("25.00"), resp1.getUnitPrice());
        assertEquals(new BigDecimal("2500.00"), resp1.getAmount());
        assertEquals("M", resp1.getUnit());
        assertEquals(1, resp1.getPieces());
        assertEquals("首批交货", resp1.getRemark());

        ShipmentItemRespVO resp2 = result.get(1);
        assertEquals(502L, resp2.getProductId());
        assertEquals("PRD-02", resp2.getProductCode());
        assertEquals("面料B", resp2.getProductName());
        assertEquals("BLUE", resp2.getCustomerColorNo());
        assertEquals(new BigDecimal("50.00"), resp2.getQuantity());
        assertEquals(new BigDecimal("15.00"), resp2.getUnitPrice());
        assertEquals(new BigDecimal("750.00"), resp2.getAmount());

        // 验证只读不落库：没有任何 insert/update/delete 调用
        verify(shipmentMapper, never()).insert(any(ShipmentDO.class));
        verify(shipmentMapper, never()).updateById(any(ShipmentDO.class));
        verify(shipmentItemMapper, never()).insert(any(ShipmentItemDO.class));
        verify(salesContractMapper, never()).updateById(any(SalesContractDO.class));
    }

    @Test
    @DisplayName("从合同带入：按 COMPLETED 已生效发货单扣减未发量，依产品+客户色号聚合")
    void testFromContract_success_unshippedQuantityDeductsCompletedShipments() {
        // Arrange
        SalesContractDO contract = createContract(3L, "CH20260001");
        contract.setUnit("KG");
        when(salesContractMapper.selectById(3L)).thenReturn(contract);

        SalesContractItemDO item1 = createContractItem(1L, 3L, 1, "PRD-01", "面料A", "RED",
                new BigDecimal("100.00"), new BigDecimal("20.00"), null);
        SalesContractItemDO item2 = createContractItem(2L, 3L, 2, "PRD-02", "面料B", "BLUE",
                new BigDecimal("80.00"), new BigDecimal("30.00"), null);
        when(salesContractItemMapper.selectListByContractId(3L)).thenReturn(List.of(item1, item2));

        ProductDO product1 = createProduct(501L, "PRD-01", "面料A");
        ProductDO product2 = createProduct(502L, "PRD-02", "面料B");
        when(productMapper.selectByCode("PRD-01")).thenReturn(product1);
        when(productMapper.selectByCode("PRD-02")).thenReturn(product2);

        // 存在两张已完成（COMPLETED）的发货单
        ShipmentDO shipment1 = new ShipmentDO();
        shipment1.setId(101L);
        ShipmentDO shipment2 = new ShipmentDO();
        shipment2.setId(102L);
        when(shipmentMapper.selectListByContractIdAndStatus(3L, "COMPLETED")).thenReturn(List.of(shipment1, shipment2));

        // 发货单1已发：PRD-01 RED 40.00
        ShipmentItemDO shipped1 = new ShipmentItemDO();
        shipped1.setShipmentId(101L);
        shipped1.setProductCode("PRD-01");
        shipped1.setCustomerColorNo("RED");
        shipped1.setQuantity(new BigDecimal("40.00"));

        // 发货单2已发：PRD-01 RED 20.00，PRD-02 BLUE 35.00
        ShipmentItemDO shipped2a = new ShipmentItemDO();
        shipped2a.setShipmentId(102L);
        shipped2a.setProductCode("PRD-01");
        shipped2a.setCustomerColorNo("RED");
        shipped2a.setQuantity(new BigDecimal("20.00"));

        ShipmentItemDO shipped2b = new ShipmentItemDO();
        shipped2b.setShipmentId(102L);
        shipped2b.setProductCode("PRD-02");
        shipped2b.setCustomerColorNo("BLUE");
        shipped2b.setQuantity(new BigDecimal("35.00"));

        when(shipmentItemMapper.selectListByShipmentIds(List.of(101L, 102L)))
                .thenReturn(List.of(shipped1, shipped2a, shipped2b));

        // Act
        List<ShipmentItemRespVO> result = shipmentService.getShipmentItemsFromContract(3L);

        // Assert
        assertEquals(2, result.size());

        // item1 (PRD-01 RED): 100 - 40 - 20 = 40
        ShipmentItemRespVO resp1 = result.get(0);
        assertEquals(new BigDecimal("40.00"), resp1.getQuantity());
        assertEquals(new BigDecimal("800.00"), resp1.getAmount());

        // item2 (PRD-02 BLUE): 80 - 35 = 45
        ShipmentItemRespVO resp2 = result.get(1);
        assertEquals(new BigDecimal("45.00"), resp2.getQuantity());
        assertEquals(new BigDecimal("1350.00"), resp2.getAmount());
    }

    @Test
    @DisplayName("从合同带入：已发数量超过合同数量时，未发量结果保底为 0，不为负数")
    void testFromContract_success_unshippedQuantityFloorAtZero() {
        SalesContractDO contract = createContract(3L, "CH20260001");
        when(salesContractMapper.selectById(3L)).thenReturn(contract);

        SalesContractItemDO item1 = createContractItem(1L, 3L, 1, "PRD-01", "面料A", "RED",
                new BigDecimal("50.00"), new BigDecimal("10.00"), null);
        when(salesContractItemMapper.selectListByContractId(3L)).thenReturn(List.of(item1));

        ProductDO product1 = createProduct(501L, "PRD-01", "面料A");
        when(productMapper.selectByCode("PRD-01")).thenReturn(product1);

        ShipmentDO shipment1 = new ShipmentDO();
        shipment1.setId(101L);
        when(shipmentMapper.selectListByContractIdAndStatus(3L, "COMPLETED")).thenReturn(List.of(shipment1));

        // 已发 60，超过合同数量 50
        ShipmentItemDO shipped1 = new ShipmentItemDO();
        shipped1.setShipmentId(101L);
        shipped1.setProductCode("PRD-01");
        shipped1.setCustomerColorNo("RED");
        shipped1.setQuantity(new BigDecimal("60.00"));
        when(shipmentItemMapper.selectListByShipmentIds(List.of(101L))).thenReturn(List.of(shipped1));

        List<ShipmentItemRespVO> result = shipmentService.getShipmentItemsFromContract(3L);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("0.00"), result.get(0).getQuantity());
        assertEquals(new BigDecimal("0.00"), result.get(0).getAmount());
    }

    @Test
    @DisplayName("从合同带入：产品档案未找到时仍带出 productCode 与 productName 快照")
    void testFromContract_success_productNotFoundStillReturnsCodeAndNameSnapshot() {
        SalesContractDO contract = createContract(3L, "CH20260001");
        when(salesContractMapper.selectById(3L)).thenReturn(contract);

        SalesContractItemDO item = createContractItem(1L, 3L, 1, "CUSTOM-CODE", "自定义品种", "BLACK",
                new BigDecimal("30.00"), new BigDecimal("18.00"), null);
        when(salesContractItemMapper.selectListByContractId(3L)).thenReturn(List.of(item));

        when(productMapper.selectByCode("CUSTOM-CODE")).thenReturn(null);
        when(shipmentMapper.selectListByContractIdAndStatus(3L, "COMPLETED")).thenReturn(Collections.emptyList());

        List<ShipmentItemRespVO> result = shipmentService.getShipmentItemsFromContract(3L);
        assertEquals(1, result.size());
        assertNull(result.get(0).getProductId());
        assertEquals("CUSTOM-CODE", result.get(0).getProductCode());
        assertEquals("自定义品种", result.get(0).getProductName());
        assertEquals("BLACK", result.get(0).getCustomerColorNo());
        assertEquals(new BigDecimal("30.00"), result.get(0).getQuantity());
    }

    // ==================== 7. Issue 04 - 审核出库过账与红冲 ====================

    @Test
    @DisplayName("审核发货单：发货单不存在报 SHIPMENT_NOT_EXISTS (1_030_016_000)")
    void testAuditShipment_notExists_throwsException() {
        when(shipmentMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.auditShipment(999L));
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审核发货单：非草稿状态报 SHIPMENT_STATUS_NOT_ALLOW (1_030_016_002)")
    void testAuditShipment_nonDraftStatus_throwsException() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("COMPLETED");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.auditShipment(10L));
        assertEquals(SHIPMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审核发货单：零明细不可审核报 SHIPMENT_AUDIT_NO_ITEMS (1_030_016_004)")
    void testAuditShipment_noItems_throwsException() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("DRAFT");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);
        when(shipmentItemMapper.selectListByShipmentId(10L)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.auditShipment(10L));
        assertEquals(SHIPMENT_AUDIT_NO_ITEMS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审核发货单：库存不足硬拦截报 SHIPMENT_STOCK_NOT_ENOUGH (1_030_016_005)")
    void testAuditShipment_stockNotEnough_throwsException() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("DRAFT");
        shipment.setWarehouseId(2L);
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        ShipmentItemDO item1 = new ShipmentItemDO();
        item1.setShipmentId(10L);
        item1.setProductId(1001L);
        item1.setProductCode("PRD-01");
        item1.setProductName("面料A");
        item1.setQuantity(new BigDecimal("50.00"));
        item1.setUnit("KG");

        ShipmentItemDO item2 = new ShipmentItemDO();
        item2.setShipmentId(10L);
        item2.setProductId(1001L);
        item2.setProductCode("PRD-01");
        item2.setProductName("面料A");
        item2.setQuantity(new BigDecimal("60.00"));
        item2.setUnit("KG");

        // 合并需求 50 + 60 = 110 KG，库存当前仅 100 KG
        when(shipmentItemMapper.selectListByShipmentId(10L)).thenReturn(List.of(item1, item2));
        when(stockLedgerService.getProductBalance(2L, 1001L, "KG"))
                .thenReturn(new BigDecimal("100.00"));

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.auditShipment(10L));
        assertEquals(SHIPMENT_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("审核发货单：库存充足审核成功，更新单据为 COMPLETED 并逐条写入 SHIPMENT_OUT 负数流水")
    void testAuditShipment_success_updatesStatusAndInsertsNegativeLedger() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setShipmentNo("SH-20260928-001");
        shipment.setStatus("DRAFT");
        shipment.setWarehouseId(2L);
        shipment.setWarehouseName("成品一仓");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        ShipmentItemDO item = new ShipmentItemDO();
        item.setShipmentId(10L);
        item.setProductId(1001L);
        item.setProductCode("PRD-01");
        item.setProductName("面料A");
        item.setVatNo("VAT-2026-001");
        item.setQuantity(new BigDecimal("50.00"));
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setAmount(new BigDecimal("1000.00"));
        item.setUnit("KG");
        item.setRemark("首批出库");

        when(shipmentItemMapper.selectListByShipmentId(10L)).thenReturn(List.of(item));
        when(stockLedgerService.getProductBalance(2L, 1001L, "KG"))
                .thenReturn(new BigDecimal("100.00"));

        // Act
        shipmentService.auditShipment(10L);

        // Assert 状态更新为 COMPLETED
        ArgumentCaptor<ShipmentDO> shipmentCaptor = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).updateById(shipmentCaptor.capture());
        assertEquals("COMPLETED", shipmentCaptor.getValue().getStatus());

        // Assert 写入一条负数出库流水
        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(ledgerCaptor.capture());
        StockLedgerDO ledger = ledgerCaptor.getValue();

        assertEquals("PRODUCT", ledger.getItemType());
        assertEquals(1001L, ledger.getProductId());
        assertEquals("PRD-01", ledger.getProductCode());
        assertEquals("面料A", ledger.getProductName());
        assertEquals(2L, ledger.getWarehouseId());
        assertEquals("成品一仓", ledger.getWarehouseName());
        assertNull(ledger.getLocationId());
        assertNull(ledger.getLocationCode());
        assertEquals("VAT-2026-001", ledger.getBatchNo());
        assertEquals("SHIPMENT_OUT", ledger.getTxnType());
        assertEquals(new BigDecimal("-50.00"), ledger.getQuantity());
        assertEquals("KG", ledger.getUnit());
        assertEquals(new BigDecimal("20.00"), ledger.getUnitPrice());
        assertEquals(new BigDecimal("-1000.00"), ledger.getAmount());
        assertEquals("SHIPMENT", ledger.getRelatedDocType());
        assertEquals(10L, ledger.getRelatedDocId());
        assertEquals("SH-20260928-001", ledger.getRelatedDocNo());
        assertEquals("首批出库", ledger.getRemark());
    }

    @Test
    @DisplayName("取消发货单：草稿态作废更新为 CANCELLED，不写库存流水")
    void testCancelShipment_draft_success_noLedgerWritten() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("DRAFT");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        // Act
        shipmentService.cancelShipment(10L);

        // Assert
        ArgumentCaptor<ShipmentDO> captor = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).updateById(captor.capture());
        assertEquals("CANCELLED", captor.getValue().getStatus());
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
    }

    @Test
    @DisplayName("取消发货单：已发货态红冲更新为 CANCELLED，反查原流水并逐条插入正数 SHIPMENT_OUT_REVERSAL 流水")
    void testCancelShipment_completed_success_insertsReversalLedgers() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setShipmentNo("SH-20260928-001");
        shipment.setStatus("COMPLETED");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        // 原流水（负数）
        StockLedgerDO orig = new StockLedgerDO();
        orig.setItemType("PRODUCT");
        orig.setProductId(1001L);
        orig.setProductCode("PRD-01");
        orig.setProductName("面料A");
        orig.setWarehouseId(2L);
        orig.setWarehouseName("成品一仓");
        orig.setBatchNo("VAT-001");
        orig.setQuantity(new BigDecimal("-50.00"));
        orig.setUnit("KG");
        orig.setUnitPrice(new BigDecimal("20.00"));
        orig.setAmount(new BigDecimal("-1000.00"));
        orig.setRelatedDocType("SHIPMENT");
        orig.setRelatedDocId(10L);
        orig.setRelatedDocNo("SH-20260928-001");

        when(stockLedgerMapper.selectListByRelatedDoc("SHIPMENT", 10L, "SHIPMENT_OUT"))
                .thenReturn(List.of(orig));

        // Act
        shipmentService.cancelShipment(10L);

        // Assert 单据转 CANCELLED
        ArgumentCaptor<ShipmentDO> captor = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).updateById(captor.capture());
        assertEquals("CANCELLED", captor.getValue().getStatus());

        // Assert 写入红冲正数流水
        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(ledgerCaptor.capture());
        StockLedgerDO reversal = ledgerCaptor.getValue();

        assertEquals("SHIPMENT_OUT_REVERSAL", reversal.getTxnType());
        assertEquals(new BigDecimal("50.00"), reversal.getQuantity());
        assertEquals(new BigDecimal("1000.00"), reversal.getAmount());
        assertEquals(1001L, reversal.getProductId());
        assertEquals("PRD-01", reversal.getProductCode());
        assertEquals("面料A", reversal.getProductName());
        assertEquals("VAT-001", reversal.getBatchNo());
        assertEquals(10L, reversal.getRelatedDocId());
        assertEquals("SH-20260928-001", reversal.getRelatedDocNo());
    }

    @Test
    @DisplayName("取消发货单：已取消终态再操作报 SHIPMENT_STATUS_NOT_ALLOW (1_030_016_002)")
    void testCancelShipment_alreadyCancelled_throwsException() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("CANCELLED");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.cancelShipment(10L));
        assertEquals(SHIPMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("签收登记：发货单不存在抛出 SHIPMENT_NOT_EXISTS (1_030_016_000)")
    void testUpdateShipmentSign_notExists_throwsException() {
        when(shipmentMapper.selectById(10L)).thenReturn(null);

        ShipmentUpdateSignReqVO req = new ShipmentUpdateSignReqVO();
        req.setId(10L);
        req.setSignStatus("SIGNED");

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.updateShipmentSign(req));
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("签收登记：草稿态单据登记报 SHIPMENT_STATUS_NOT_ALLOW (1_030_016_002)")
    void testUpdateShipmentSign_draftStatus_throwsException() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("DRAFT");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        ShipmentUpdateSignReqVO req = new ShipmentUpdateSignReqVO();
        req.setId(10L);
        req.setSignStatus("SIGNED");

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.updateShipmentSign(req));
        assertEquals(SHIPMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("签收登记：已取消态单据登记报 SHIPMENT_STATUS_NOT_ALLOW (1_030_016_002)")
    void testUpdateShipmentSign_cancelledStatus_throwsException() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("CANCELLED");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        ShipmentUpdateSignReqVO req = new ShipmentUpdateSignReqVO();
        req.setId(10L);
        req.setSignStatus("SIGNED");

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.updateShipmentSign(req));
        assertEquals(SHIPMENT_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("签收登记：非法签收状态报 SHIPMENT_SIGN_STATUS_INVALID (1_030_016_006)")
    void testUpdateShipmentSign_invalidSignStatus_throwsException() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("COMPLETED");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        ShipmentUpdateSignReqVO req = new ShipmentUpdateSignReqVO();
        req.setId(10L);
        req.setSignStatus("INVALID_STATUS");

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.updateShipmentSign(req));
        assertEquals(SHIPMENT_SIGN_STATUS_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("签收登记：已发货单据登记已签收，成功更新签收人/时间/备注，不产生库存流水")
    void testUpdateShipmentSign_signed_success_noLedgerWritten() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("COMPLETED");
        shipment.setSignStatus("PENDING");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        LocalDateTime now = LocalDateTime.of(2026, 9, 28, 14, 30, 0);
        ShipmentUpdateSignReqVO req = new ShipmentUpdateSignReqVO();
        req.setId(10L);
        req.setSignStatus("SIGNED");
        req.setSignedByName("张收货");
        req.setSignedAt(now);
        req.setSignRemark("货物外包装完好，件数一致");

        // Act
        shipmentService.updateShipmentSign(req);

        // Assert
        ArgumentCaptor<ShipmentDO> captor = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).updateById(captor.capture());
        ShipmentDO updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("SIGNED", updated.getSignStatus());
        assertEquals("张收货", updated.getSignedByName());
        assertEquals(now, updated.getSignedAt());
        assertEquals("货物外包装完好，件数一致", updated.getSignRemark());

        // 绝不触发任何库存动作
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
    }

    @Test
    @DisplayName("签收登记：已发货单据登记拒签，只登记状态，不自动红冲、不产生库存流水")
    void testUpdateShipmentSign_refused_success_noLedgerWritten() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(10L);
        shipment.setStatus("COMPLETED");
        shipment.setSignStatus("PENDING");
        when(shipmentMapper.selectById(10L)).thenReturn(shipment);

        LocalDateTime now = LocalDateTime.of(2026, 9, 28, 15, 0, 0);
        ShipmentUpdateSignReqVO req = new ShipmentUpdateSignReqVO();
        req.setId(10L);
        req.setSignStatus("REFUSED");
        req.setSignedByName("李收货");
        req.setSignedAt(now);
        req.setSignRemark("布面有油污瑕疵，客户拒绝签收");

        // Act
        shipmentService.updateShipmentSign(req);

        // Assert
        ArgumentCaptor<ShipmentDO> captor = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).updateById(captor.capture());
        ShipmentDO updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("REFUSED", updated.getSignStatus());
        assertEquals("李收货", updated.getSignedByName());
        assertEquals(now, updated.getSignedAt());
        assertEquals("布面有油污瑕疵，客户拒绝签收", updated.getSignRemark());

        // 规范明确要求：拒签只登记状态，不自动红冲库存，退货由人执行取消
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
    }
}

