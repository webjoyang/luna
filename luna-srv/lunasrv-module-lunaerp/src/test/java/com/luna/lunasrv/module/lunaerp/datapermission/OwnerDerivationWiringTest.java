package com.luna.lunasrv.module.lunaerp.datapermission;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.enums.CommonStatusEnum;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.module.infra.api.config.ConfigApi;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerImportVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
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
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customer.CustomerServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.business.salescontract.SalesContractServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentServiceImpl;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 03 票：服务端 dept_id 推导与 VO 字段契约单元测试（ADR-0046）
 *
 * 覆盖验收矩阵：
 * 1. VO 不暴露 dept_id 字段反射断言（CustomerSaveReqVO / CustomerRespVO / CustomerPageReqVO / CustomerImportVO）
 * 2. 选填且可清空列 TableField(updateStrategy = FieldStrategy.ALWAYS) 反射断言
 * 3. 客户 create / update 的 owner & dept_id 推导（登录人兜底、修改重算、清空双清、不变不动）
 * 4. 合同 create / update 的 owner & dept_id 推导（登录人兜底、修改重算、清空双清、不变不动）
 * 5. 发货 create / update 的 owner & dept_id 推导（登录人兜底、修改重算、清空双清、不变不动）
 * 6. 跨归属引用存在性校验（合同引不可见客户 1_030_004_000，发货引不可见合同 1_030_016_003）
 */
class OwnerDerivationWiringTest {

    @Mock private CustomerMapper customerMapper;
    @Mock private SalesContractMapper salesContractMapper;
    @Mock private SalesContractItemMapper salesContractItemMapper;
    @Mock private ShipmentMapper shipmentMapper;
    @Mock private ShipmentItemMapper shipmentItemMapper;
    @Mock private ShipmentRollMapper shipmentRollMapper;
    @Mock private WarehouseMapper warehouseMapper;
    @Mock private ProductMapper productMapper;
    @Mock private StockInItemMapper stockInItemMapper;
    @Mock private AdminUserApi adminUserApi;
    @Mock private ConfigApi configApi;

    @InjectMocks private CustomerServiceImpl customerService;
    @InjectMocks private SalesContractServiceImpl salesContractService;
    @InjectMocks private ShipmentServiceImpl shipmentService;

    private static final Long LOGIN_USER_ID = 100L;
    private static final Long LOGIN_DEPT_ID = 1000L;
    private static final Long OTHER_USER_ID = 200L;
    private static final Long OTHER_DEPT_ID = 2000L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), CustomerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesContractDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ShipmentDO.class);

        // 设置当前登录用户
        LoginUser loginUser = new LoginUser();
        loginUser.setId(LOGIN_USER_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));

        // mock adminUserApi 用户查询
        AdminUserRespDTO loginUserDTO = new AdminUserRespDTO();
        loginUserDTO.setId(LOGIN_USER_ID);
        loginUserDTO.setDeptId(LOGIN_DEPT_ID);
        loginUserDTO.setNickname("登录业务员");
        loginUserDTO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(adminUserApi.getUser(LOGIN_USER_ID)).thenReturn(loginUserDTO);

        AdminUserRespDTO otherUserDTO = new AdminUserRespDTO();
        otherUserDTO.setId(OTHER_USER_ID);
        otherUserDTO.setDeptId(OTHER_DEPT_ID);
        otherUserDTO.setNickname("其他业务员");
        otherUserDTO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(adminUserApi.getUser(OTHER_USER_ID)).thenReturn(otherUserDTO);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== 1. VO 反射契约断言 ====================

    @Test
    @DisplayName("任何客户 VO 均不得暴露 dept_id 字段，客户保存与分页 VO 必须包含 salespersonUserId")
    void testCustomerVOContracts() {
        assertNull(findField(CustomerSaveReqVO.class, "deptId"), "CustomerSaveReqVO 不得暴露 deptId");
        assertNull(findField(CustomerRespVO.class, "deptId"), "CustomerRespVO 不得暴露 deptId");
        assertNull(findField(CustomerPageReqVO.class, "deptId"), "CustomerPageReqVO 不得暴露 deptId");
        assertNull(findField(CustomerImportVO.class, "deptId"), "CustomerImportVO 不得暴露 deptId");

        assertNotNull(findField(CustomerSaveReqVO.class, "salespersonUserId"), "CustomerSaveReqVO 必须包含 salespersonUserId");
        assertNotNull(findField(CustomerRespVO.class, "salespersonUserId"), "CustomerRespVO 必须包含 salespersonUserId");
        assertNotNull(findField(CustomerPageReqVO.class, "salespersonUserId"), "CustomerPageReqVO 必须包含 salespersonUserId");
    }

    @Test
    @DisplayName("CustomerDO / SalesContractDO / ShipmentDO 的归属列必须声明 updateStrategy = ALWAYS")
    void testDOUpdateStrategyContracts() {
        assertTableFieldAlways(CustomerDO.class, "salespersonUserId");
        assertTableFieldAlways(CustomerDO.class, "deptId");
        assertTableFieldAlways(SalesContractDO.class, "salespersonUserId");
        assertTableFieldAlways(SalesContractDO.class, "deptId");
        assertTableFieldAlways(ShipmentDO.class, "salespersonUserId");
        assertTableFieldAlways(ShipmentDO.class, "deptId");
    }

    // ==================== 2. 客户归属推导 ====================

    @Test
    @DisplayName("客户新建：传入业务员 -> 服务端推导该业务员部门")
    void testCustomerCreate_withExplicitSalesperson() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompanyName("客户A");
        req.setClientType("FORMAL");
        req.setSalespersonUserId(OTHER_USER_ID);

        customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();
        assertEquals(OTHER_USER_ID, saved.getSalespersonUserId());
        assertEquals(OTHER_DEPT_ID, saved.getDeptId());
    }

    @Test
    @DisplayName("客户新建：未传业务员 -> 缺省当前登录用户及其部门")
    void testCustomerCreate_defaultToLoginUser() {
        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompanyName("客户B");
        req.setClientType("FORMAL");
        req.setSalespersonUserId(null);

        customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();
        assertEquals(LOGIN_USER_ID, saved.getSalespersonUserId());
        assertEquals(LOGIN_DEPT_ID, saved.getDeptId());
    }

    @Test
    @DisplayName("客户新建：未登录且未传业务员 -> 两列皆空")
    void testCustomerCreate_noLoginUser_bothNull() {
        SecurityContextHolder.clearContext();

        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setCompanyName("客户C");
        req.setClientType("FORMAL");
        req.setSalespersonUserId(null);

        customerService.createCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).insert(captor.capture());
        CustomerDO saved = captor.getValue();
        assertNull(saved.getSalespersonUserId());
        assertNull(saved.getDeptId());
    }

    @Test
    @DisplayName("客户更新：归属人变更 -> 同事务重算 dept_id")
    void testCustomerUpdate_ownerChanged_recalculatesDeptId() {
        CustomerDO existing = new CustomerDO();
        existing.setId(1L);
        existing.setSalespersonUserId(LOGIN_USER_ID);
        existing.setDeptId(LOGIN_DEPT_ID);
        when(customerMapper.selectById(1L)).thenReturn(existing);

        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(1L);
        req.setCompanyName("客户A改名");
        req.setClientType("FORMAL");
        req.setSalespersonUserId(OTHER_USER_ID);

        customerService.updateCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        CustomerDO updated = captor.getValue();
        assertEquals(OTHER_USER_ID, updated.getSalespersonUserId());
        assertEquals(OTHER_DEPT_ID, updated.getDeptId());
    }

    @Test
    @DisplayName("客户更新：清空归属人 -> 两列清空")
    void testCustomerUpdate_ownerCleared_bothNull() {
        CustomerDO existing = new CustomerDO();
        existing.setId(1L);
        existing.setSalespersonUserId(LOGIN_USER_ID);
        existing.setDeptId(LOGIN_DEPT_ID);
        when(customerMapper.selectById(1L)).thenReturn(existing);

        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(1L);
        req.setCompanyName("客户A改名");
        req.setClientType("FORMAL");
        req.setSalespersonUserId(null);

        customerService.updateCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        CustomerDO updated = captor.getValue();
        assertNull(updated.getSalespersonUserId());
        assertNull(updated.getDeptId());
    }

    @Test
    @DisplayName("客户更新：归属人未变 -> 不动 dept_id")
    void testCustomerUpdate_ownerUnchanged_deptIdUntouched() {
        CustomerDO existing = new CustomerDO();
        existing.setId(1L);
        existing.setSalespersonUserId(OTHER_USER_ID);
        existing.setDeptId(9999L); // 假设历史部门快照为 9999
        when(customerMapper.selectById(1L)).thenReturn(existing);

        CustomerSaveReqVO req = new CustomerSaveReqVO();
        req.setId(1L);
        req.setCompanyName("客户A改名");
        req.setClientType("FORMAL");
        req.setSalespersonUserId(OTHER_USER_ID);

        customerService.updateCustomer(req);

        ArgumentCaptor<CustomerDO> captor = ArgumentCaptor.forClass(CustomerDO.class);
        verify(customerMapper).updateById(captor.capture());
        CustomerDO updated = captor.getValue();
        assertEquals(OTHER_USER_ID, updated.getSalespersonUserId());
        assertEquals(9999L, updated.getDeptId(), "归属人未变时不动 dept_id");
    }

    // ==================== 3. 销售合同归属推导 ====================

    @Test
    @DisplayName("合同新建：传入业务员 -> 服务端推导其部门")
    void testSalesContractCreate_withExplicitSalesperson() {
        mockCustomerExists(10L);

        SalesContractSaveReqVO req = new SalesContractSaveReqVO();
        req.setCustomerId(10L);
        req.setSalespersonUserId(OTHER_USER_ID);

        salesContractService.createSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        SalesContractDO saved = captor.getValue();
        assertEquals(OTHER_USER_ID, saved.getSalespersonUserId());
        assertEquals(OTHER_DEPT_ID, saved.getDeptId());
    }

    @Test
    @DisplayName("合同新建：未传业务员 -> 缺省当前登录用户及其部门")
    void testSalesContractCreate_defaultToLoginUser() {
        mockCustomerExists(10L);

        SalesContractSaveReqVO req = new SalesContractSaveReqVO();
        req.setCustomerId(10L);
        req.setSalespersonUserId(null);

        salesContractService.createSalesContract(req);

        ArgumentCaptor<SalesContractDO> captor = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).insert(captor.capture());
        SalesContractDO saved = captor.getValue();
        assertEquals(LOGIN_USER_ID, saved.getSalespersonUserId());
        assertEquals(LOGIN_DEPT_ID, saved.getDeptId());
    }

    @Test
    @DisplayName("合同更新：归属人变更 -> 重算 dept_id；清空 -> 两列清空；未变 -> 不动 dept_id")
    void testSalesContractUpdate_ownerDerivation() {
        mockCustomerExists(10L);

        // 场景 1：归属人变更
        SalesContractDO existing1 = new SalesContractDO();
        existing1.setId(100L);
        existing1.setCustomerId(10L);
        existing1.setSalespersonUserId(LOGIN_USER_ID);
        existing1.setDeptId(LOGIN_DEPT_ID);
        when(salesContractMapper.selectById(100L)).thenReturn(existing1);

        SalesContractSaveReqVO req1 = new SalesContractSaveReqVO();
        req1.setId(100L);
        req1.setCustomerId(10L);
        req1.setSalespersonUserId(OTHER_USER_ID);

        salesContractService.updateSalesContract(req1);

        ArgumentCaptor<SalesContractDO> captor1 = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper).updateById(captor1.capture());
        assertEquals(OTHER_USER_ID, captor1.getValue().getSalespersonUserId());
        assertEquals(OTHER_DEPT_ID, captor1.getValue().getDeptId());

        // 场景 2：清空归属人
        SalesContractSaveReqVO req2 = new SalesContractSaveReqVO();
        req2.setId(100L);
        req2.setCustomerId(10L);
        req2.setSalespersonUserId(null);

        salesContractService.updateSalesContract(req2);

        ArgumentCaptor<SalesContractDO> captor2 = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper, times(2)).updateById(captor2.capture());
        assertNull(captor2.getValue().getSalespersonUserId());
        assertNull(captor2.getValue().getDeptId());

        // 场景 3：归属人未变
        existing1.setSalespersonUserId(OTHER_USER_ID);
        existing1.setDeptId(8888L);
        SalesContractSaveReqVO req3 = new SalesContractSaveReqVO();
        req3.setId(100L);
        req3.setCustomerId(10L);
        req3.setSalespersonUserId(OTHER_USER_ID);

        salesContractService.updateSalesContract(req3);

        ArgumentCaptor<SalesContractDO> captor3 = ArgumentCaptor.forClass(SalesContractDO.class);
        verify(salesContractMapper, times(3)).updateById(captor3.capture());
        assertEquals(OTHER_USER_ID, captor3.getValue().getSalespersonUserId());
        assertEquals(8888L, captor3.getValue().getDeptId(), "合同归属人未变时不重算 dept_id");
    }

    // ==================== 4. 发货单归属推导 ====================

    @Test
    @DisplayName("发货单新建：传入业务员 -> 推导部门；未传 -> 缺省当前登录用户及其部门")
    void testShipmentCreate_ownerDerivation() {
        mockCustomerExists(10L);
        mockWarehouseExists(30L);

        // 显式业务员
        ShipmentSaveReqVO req1 = new ShipmentSaveReqVO();
        req1.setCustomerId(10L);
        req1.setWarehouseId(30L);
        req1.setSalespersonUserId(OTHER_USER_ID);

        shipmentService.createShipment(req1);

        ArgumentCaptor<ShipmentDO> captor1 = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).insert(captor1.capture());
        assertEquals(OTHER_USER_ID, captor1.getValue().getSalespersonUserId());
        assertEquals(OTHER_DEPT_ID, captor1.getValue().getDeptId());

        // 缺省业务员
        ShipmentSaveReqVO req2 = new ShipmentSaveReqVO();
        req2.setCustomerId(10L);
        req2.setWarehouseId(30L);
        req2.setSalespersonUserId(null);

        shipmentService.createShipment(req2);

        ArgumentCaptor<ShipmentDO> captor2 = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper, times(2)).insert(captor2.capture());
        assertEquals(LOGIN_USER_ID, captor2.getValue().getSalespersonUserId());
        assertEquals(LOGIN_DEPT_ID, captor2.getValue().getDeptId());
    }

    @Test
    @DisplayName("发货单更新：归属人变更 -> 重算 dept_id；清空 -> 两列清空；未变 -> 不动 dept_id")
    void testShipmentUpdate_ownerDerivation() {
        mockCustomerExists(10L);
        mockWarehouseExists(30L);

        ShipmentDO existing = new ShipmentDO();
        existing.setId(50L);
        existing.setStatus("DRAFT");
        existing.setSalespersonUserId(LOGIN_USER_ID);
        existing.setDeptId(LOGIN_DEPT_ID);
        when(shipmentMapper.selectById(50L)).thenReturn(existing);

        // 1. 变更
        ShipmentSaveReqVO req1 = new ShipmentSaveReqVO();
        req1.setId(50L);
        req1.setCustomerId(10L);
        req1.setWarehouseId(30L);
        req1.setSalespersonUserId(OTHER_USER_ID);

        shipmentService.updateShipment(req1);

        ArgumentCaptor<ShipmentDO> captor1 = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper).updateById(captor1.capture());
        assertEquals(OTHER_USER_ID, captor1.getValue().getSalespersonUserId());
        assertEquals(OTHER_DEPT_ID, captor1.getValue().getDeptId());

        // 2. 清空
        ShipmentSaveReqVO req2 = new ShipmentSaveReqVO();
        req2.setId(50L);
        req2.setCustomerId(10L);
        req2.setWarehouseId(30L);
        req2.setSalespersonUserId(null);

        shipmentService.updateShipment(req2);

        ArgumentCaptor<ShipmentDO> captor2 = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper, times(2)).updateById(captor2.capture());
        assertNull(captor2.getValue().getSalespersonUserId());
        assertNull(captor2.getValue().getDeptId());

        // 3. 未变
        existing.setSalespersonUserId(OTHER_USER_ID);
        existing.setDeptId(7777L);
        ShipmentSaveReqVO req3 = new ShipmentSaveReqVO();
        req3.setId(50L);
        req3.setCustomerId(10L);
        req3.setWarehouseId(30L);
        req3.setSalespersonUserId(OTHER_USER_ID);

        shipmentService.updateShipment(req3);

        ArgumentCaptor<ShipmentDO> captor3 = ArgumentCaptor.forClass(ShipmentDO.class);
        verify(shipmentMapper, times(3)).updateById(captor3.capture());
        assertEquals(OTHER_USER_ID, captor3.getValue().getSalespersonUserId());
        assertEquals(7777L, captor3.getValue().getDeptId(), "发货单归属人未变时不动 dept_id");
    }

    // ==================== 5. 跨归属引用校验断言 ====================

    @Test
    @DisplayName("合同新建引用不可见客户 -> 抛 CUSTOMER_NOT_EXISTS (1_030_004_000)")
    void testContractRefInvisibleCustomer_throwsCustomerNotExists() {
        when(customerMapper.selectById(999L)).thenReturn(null); // 受限规则下 selectById 自动查不到

        SalesContractSaveReqVO req = new SalesContractSaveReqVO();
        req.setCustomerId(999L);

        ServiceException ex = assertThrows(ServiceException.class, () -> salesContractService.createSalesContract(req));
        assertEquals(1_030_004_000, ex.getCode());
    }

    @Test
    @DisplayName("发货新建引用不可见合同 -> 抛 SHIPMENT_RELATED_REF_NOT_EXISTS (1_030_016_003)")
    void testShipmentRefInvisibleContract_throwsShipmentRelatedRefNotExists() {
        mockCustomerExists(10L);
        mockWarehouseExists(30L);
        when(salesContractMapper.selectById(888L)).thenReturn(null); // 受限规则下 selectById 自动查不到

        ShipmentSaveReqVO req = new ShipmentSaveReqVO();
        req.setCustomerId(10L);
        req.setWarehouseId(30L);
        req.setSalesContractId(888L);

        ServiceException ex = assertThrows(ServiceException.class, () -> shipmentService.createShipment(req));
        assertEquals(1_030_016_003, ex.getCode());
        assertTrue(ex.getMessage().contains("销售合同"));
    }

    // ==================== 辅助方法 ====================

    private void mockCustomerExists(Long customerId) {
        CustomerDO customer = new CustomerDO();
        customer.setId(customerId);
        customer.setCompanyName("测试客户");
        customer.setStatus("ACTIVE");
        when(customerMapper.selectById(customerId)).thenReturn(customer);
    }

    private void mockWarehouseExists(Long warehouseId) {
        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(warehouseId);
        warehouse.setName("一号成品仓");
        warehouse.setStatus("ACTIVE");
        when(warehouseMapper.selectById(warehouseId)).thenReturn(warehouse);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private void assertTableFieldAlways(Class<?> clazz, String fieldName) {
        Field field = findField(clazz, fieldName);
        assertNotNull(field, clazz.getSimpleName() + " 必须包含字段 " + fieldName);
        TableField tableField = field.getAnnotation(TableField.class);
        assertNotNull(tableField, clazz.getSimpleName() + "." + fieldName + " 必须声明 @TableField");
        assertEquals(FieldStrategy.ALWAYS, tableField.updateStrategy(),
                clazz.getSimpleName() + "." + fieldName + " 必须配置 updateStrategy = FieldStrategy.ALWAYS");
    }

}
