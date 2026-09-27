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
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentRollDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
 *
 * 覆盖 Issue 02 业务行为：
 * 1. 创建：status 缺省 DRAFT；signStatus 缺省 PENDING；currency 缺省 RMB；shippingDate 缺省当天
 * 2. 编号：SH-YYYYMMDD-NNN 按天取号；手填撞号报 SHIPMENT_NO_DUPLICATE (016_001)
 * 3. 关联校验与快照回填：客户必选且 ACTIVE；发货仓库必选且 ACTIVE；合同存在；业务员存在且启用；产品必选存在；来源入库明细存在；违规报 SHIPMENT_RELATED_REF_NOT_EXISTS (016_003)
 * 4. 聚合保存与权威重算：金额 = quantity × unitPrice，整单 totalAmount/totalPieces 服务端重算；允许零明细草稿
 * 5. 更新：仅 DRAFT 可改，非草稿报 SHIPMENT_STATUS_NOT_ALLOW (016_002)；明细与卷清单整表逻辑删除后重插；金额匹数重算
 * 6. 删除：仅 DRAFT 可删，非草稿报 016_002；删除主表级联逻辑删除 items 与 rolls
 * 7. 详情查询与分页查询
 * 8. 预览下一个单号
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
    private ProductMapper productMapper;

    @Mock
    private StockInItemMapper stockInItemMapper;

    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentRollDO.class);

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

}
