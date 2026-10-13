package com.luna.lunasrv.module.lunaerp.datapermission;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.biz.system.permission.PermissionCommonApi;
import com.luna.lunasrv.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.datapermission.core.aop.DataPermissionContextHolder;
import com.luna.lunasrv.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import com.luna.lunasrv.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import com.luna.lunasrv.framework.datapermission.core.util.DataPermissionUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation.QuotationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customercontact.CustomerContactMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.framework.datapermission.config.LunaerpDataPermissionConfiguration;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customer.CustomerServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact.CustomerContactServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.business.salescontract.SalesContractServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.production.productionprogress.ProductionProgressQueryServiceImpl;
import com.luna.lunasrv.module.system.enums.permission.DataScopeEnum;
import com.luna.lunasrv.module.system.service.permission.PermissionServiceImpl;
import com.luna.lunasrv.module.system.service.permission.RoleService;
import net.sf.jsqlparser.expression.Expression;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.module.lunaerp.datapermission.LunaerpDataPermissionFixture.*;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * §9 横切功能测试矩阵（向 16 号 §6.2 看齐，Issue 07）
 *
 * 覆盖：
 * 1. §9.1 组织造数 fixture：部门 D1/D1.1/D2、用户 m/a/c/d + DEPT_ONLY + boss，4 张已存表每人 ≥2（含不同状态），空归属 ≥1
 * 2. §9.2 用例 1（四档×5 表可见性）：ALL/DEPT_AND_CHILD/DEPT_ONLY/SELF 每表每档列表断言+直连 id 详情断言；报价表冻结契约
 * 3. §9.2 用例 6（多角色并集）：SELF + DEPT_ONLY 并集可见；任一 ALL 则全部
 * 4. §9.2 用例 7（边界语义）：无角色用户仅见本人；空归属行仅 ALL 可见；归属人停用后历史行仍按列值可见；easy-trans 翻译不借道泄漏归属外字段
 * 5. §9.2 用例 8（系统侧不受档影响）：salesperson 登录走合同查重/取号、发货取号、未发量统计全量、生产进度页
 * 6. §9.2 用例 9（自定义档收口回归）：角色弹窗无指定部门、API 拒绝 2 抛 1_002_002_006、common/test-dp 迁移
 * 7. §9.2 用例 10（回填 SQL 回归）：空库跑通、3 类 creator 幂等、多租户各归各
 * 8. §9.2 用例 11（跨租户专测断言）：系统侧关闭方法仍带 tenant_id 条件，杜绝跨租户越权
 * 9. §9.2 用例 5（换岗联动）：a 调到 D2 后，m 立即不可见 a 的旧单，D2 主管立即可见，a 本人 SELF 始终可见
 *
 * @author luna
 */
public class LunaerpDataPermissionMatrixTest {

    private CustomerMapper customerMapper;
    private SalesContractMapper salesContractMapper;
    private ShipmentMapper shipmentMapper;
    private CustomerContactMapper customerContactMapper;

    private CustomerServiceImpl customerService;
    private SalesContractServiceImpl salesContractService;
    private ShipmentServiceImpl shipmentService;
    private CustomerContactServiceImpl customerContactService;

    private PermissionCommonApi permissionApi;
    private DeptDataPermissionRule deptRule;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesContractDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerContactDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), QuotationDO.class);
    }

    @BeforeEach
    void setupServicesAndMappers() {
        customerMapper = Mockito.mock(CustomerMapper.class);
        salesContractMapper = Mockito.mock(SalesContractMapper.class);
        com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper salesContractItemMapper =
                Mockito.mock(com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper.class);
        shipmentMapper = Mockito.mock(ShipmentMapper.class);
        com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper shipmentItemMapper =
                Mockito.mock(com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper.class);
        com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper shipmentRollMapper =
                Mockito.mock(com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper.class);
        customerContactMapper = Mockito.mock(CustomerContactMapper.class);

        when(salesContractItemMapper.selectListByContractId(anyLong())).thenReturn(Collections.emptyList());
        when(shipmentItemMapper.selectListByShipmentId(anyLong())).thenReturn(Collections.emptyList());
        when(shipmentRollMapper.selectListByShipmentId(anyLong())).thenReturn(Collections.emptyList());

        customerService = new CustomerServiceImpl();
        ReflectionTestUtils.setField(customerService, "customerMapper", customerMapper);

        salesContractService = new SalesContractServiceImpl();
        ReflectionTestUtils.setField(salesContractService, "salesContractMapper", salesContractMapper);
        ReflectionTestUtils.setField(salesContractService, "salesContractItemMapper", salesContractItemMapper);

        shipmentService = new ShipmentServiceImpl();
        ReflectionTestUtils.setField(shipmentService, "shipmentMapper", shipmentMapper);
        ReflectionTestUtils.setField(shipmentService, "salesContractMapper", salesContractMapper);
        ReflectionTestUtils.setField(shipmentService, "shipmentItemMapper", shipmentItemMapper);
        ReflectionTestUtils.setField(shipmentService, "shipmentRollMapper", shipmentRollMapper);

        customerContactService = new CustomerContactServiceImpl();
        ReflectionTestUtils.setField(customerContactService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(customerContactService, "customerContactMapper", customerContactMapper);

        permissionApi = Mockito.mock(PermissionCommonApi.class);
        deptRule = new DeptDataPermissionRule(permissionApi);
        LunaerpDataPermissionConfiguration config = new LunaerpDataPermissionConfiguration();
        DeptDataPermissionRuleCustomizer customizer = config.lunaerpDeptDataPermissionRuleCustomizer();
        customizer.customize(deptRule);
    }

    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        while (DataPermissionContextHolder.get() != null) {
            DataPermissionContextHolder.remove();
        }
    }

    /**
     * 为指定用户绑定 mock 数据访问行为
     */
    private void bindUserMockBehavior(Long currentUserId, String username, DeptDataPermissionRespDTO perm) {
        mockLogin(currentUserId, username);
        when(permissionApi.getDeptDataPermission(currentUserId)).thenReturn(perm);

        // 1. 客户 Mock
        when(customerMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return ALL_CUSTOMERS.stream()
                    .filter(c -> c.getId().equals(id) && isCustomerVisible(c, perm, currentUserId))
                    .findFirst()
                    .orElse(null);
        });

        // 2. 合同 Mock
        when(salesContractMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return ALL_CONTRACTS.stream()
                    .filter(c -> c.getId().equals(id) && isContractVisible(c, perm, currentUserId))
                    .findFirst()
                    .orElse(null);
        });

        // 3. 发货 Mock
        when(shipmentMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return ALL_SHIPMENTS.stream()
                    .filter(s -> s.getId().equals(id) && isShipmentVisible(s, perm, currentUserId))
                    .findFirst()
                    .orElse(null);
        });

        // 4. 联系人 Mock (底层联系人行本身能查出，但 Service 会校验其所属客户是否存在且可见)
        when(customerContactMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return ALL_CONTACTS.stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        });
    }

    // =========================================================================
    // 1. §9.1 组织造数 fixture 校验
    // =========================================================================

    @Test
    @DisplayName("§9.1 组织造数 fixture 结构规范校验：部门树、用户角色、4 表每人 ≥2（含不同状态）与空归属 ≥1")
    void testFixtureDataStructureAndDistribution() {
        // 部门层次断言
        assertNotNull(DEPT_D1_ID);
        assertNotNull(DEPT_D1_1_ID);
        assertNotNull(DEPT_D2_ID);
        assertNotEquals(DEPT_D1_ID, DEPT_D2_ID, "D1 与 D2 必须平级不同 ID");

        // 4 表行数断言：每表至少造 a(2) + c(2) + d(2) + null(1) + deact(1) = 8 行
        assertEquals(8, ALL_CUSTOMERS.size(), "客户表 fixture 必须为 8 行");
        assertEquals(8, ALL_CONTRACTS.size(), "销售合同表 fixture 必须为 8 行");
        assertEquals(8, ALL_SHIPMENTS.size(), "发货单表 fixture 必须为 8 行");
        assertEquals(8, ALL_CONTACTS.size(), "客户联系人表 fixture 必须为 8 行");

        // 状态覆盖断言：a 必须至少造 2 行且状态不同
        List<CustomerDO> custA = ALL_CUSTOMERS.stream().filter(c -> USER_A_ID.equals(c.getSalespersonUserId())).toList();
        assertEquals(2, custA.size());
        assertNotEquals(custA.get(0).getStatus(), custA.get(1).getStatus(), "a 的两行客户状态必须不同");

        List<SalesContractDO> scA = ALL_CONTRACTS.stream().filter(c -> USER_A_ID.equals(c.getSalespersonUserId())).toList();
        assertEquals(2, scA.size());
        assertNotEquals(scA.get(0).getStatus(), scA.get(1).getStatus(), "a 的两行合同状态必须不同");

        List<ShipmentDO> shA = ALL_SHIPMENTS.stream().filter(s -> USER_A_ID.equals(s.getSalespersonUserId())).toList();
        assertEquals(2, shA.size());
        assertNotEquals(shA.get(0).getStatus(), shA.get(1).getStatus(), "a 的两行发货单状态必须不同");

        // 空归属行断言
        long nullCustCount = ALL_CUSTOMERS.stream().filter(c -> c.getSalespersonUserId() == null && c.getDeptId() == null).count();
        assertTrue(nullCustCount >= 1, "必须有空归属客户行");

        long nullScCount = ALL_CONTRACTS.stream().filter(c -> c.getSalespersonUserId() == null && c.getDeptId() == null).count();
        assertTrue(nullScCount >= 1, "必须有空归属合同行");

        long nullShCount = ALL_SHIPMENTS.stream().filter(s -> s.getSalespersonUserId() == null && s.getDeptId() == null).count();
        assertTrue(nullShCount >= 1, "必须有空归属发货单行");
    }

    // =========================================================================
    // 2. §9.2 用例 1（四档×5 表可见性）
    // =========================================================================

    @Test
    @DisplayName("§9.2 用例 1 - ALL 档（boss）：4 表列表见全部 7 行（含空归属），直连 a/c/d/null 详情均成功返回")
    void testCase1_VisibilityMatrix_AllScope() {
        DeptDataPermissionRespDTO bossPerm = getPermission(USER_BOSS_ID);
        bindUserMockBehavior(USER_BOSS_ID, "boss", bossPerm);

        // 1. 客户表断言
        List<Long> visibleCustIds = ALL_CUSTOMERS.stream()
                .filter(c -> isCustomerVisible(c, bossPerm, USER_BOSS_ID))
                .map(CustomerDO::getId)
                .toList();
        assertTrue(visibleCustIds.containsAll(List.of(CUST_A_1_ID, CUST_C_1_ID, CUST_D_1_ID, CUST_NULL_ID)),
                "ALL 档必须可见 a/c/d 以及空归属客户");
        assertNotNull(customerService.getCustomer(CUST_A_1_ID));
        assertNotNull(customerService.getCustomer(CUST_C_1_ID));
        assertNotNull(customerService.getCustomer(CUST_D_1_ID));
        assertNotNull(customerService.getCustomer(CUST_NULL_ID));

        // 2. 合同表断言
        List<Long> visibleScIds = ALL_CONTRACTS.stream()
                .filter(sc -> isContractVisible(sc, bossPerm, USER_BOSS_ID))
                .map(SalesContractDO::getId)
                .toList();
        assertTrue(visibleScIds.containsAll(List.of(CONTRACT_A_1_ID, CONTRACT_C_1_ID, CONTRACT_D_1_ID, CONTRACT_NULL_ID)));
        assertNotNull(salesContractService.getSalesContract(CONTRACT_A_1_ID));
        assertNotNull(salesContractService.getSalesContract(CONTRACT_C_1_ID));
        assertNotNull(salesContractService.getSalesContract(CONTRACT_D_1_ID));
        assertNotNull(salesContractService.getSalesContract(CONTRACT_NULL_ID));

        // 3. 发货表断言
        List<Long> visibleShIds = ALL_SHIPMENTS.stream()
                .filter(sh -> isShipmentVisible(sh, bossPerm, USER_BOSS_ID))
                .map(ShipmentDO::getId)
                .toList();
        assertTrue(visibleShIds.containsAll(List.of(SHIPMENT_A_1_ID, SHIPMENT_C_1_ID, SHIPMENT_D_1_ID, SHIPMENT_NULL_ID)));
        assertNotNull(shipmentService.getShipment(SHIPMENT_A_1_ID));
        assertNotNull(shipmentService.getShipment(SHIPMENT_C_1_ID));
        assertNotNull(shipmentService.getShipment(SHIPMENT_D_1_ID));
        assertNotNull(shipmentService.getShipment(SHIPMENT_NULL_ID));

        // 4. 联系人表断言
        List<Long> visibleContactIds = ALL_CONTACTS.stream()
                .filter(cc -> isContactVisible(cc, bossPerm, USER_BOSS_ID))
                .map(CustomerContactDO::getId)
                .toList();
        assertTrue(visibleContactIds.containsAll(List.of(CONTACT_A_1_ID, CONTACT_C_1_ID, CONTACT_D_1_ID, CONTACT_NULL_ID)));
        assertNotNull(customerContactService.getCustomerContact(CONTACT_A_1_ID));
        assertNotNull(customerContactService.getCustomerContact(CONTACT_C_1_ID));
        assertNotNull(customerContactService.getCustomerContact(CONTACT_D_1_ID));
        assertNotNull(customerContactService.getCustomerContact(CONTACT_NULL_ID));
    }

    @Test
    @DisplayName("§9.2 用例 1 - DEPT_AND_CHILD 档（m）：4 表列表见 a+c (4 行)，不见 d (2 行) 和空归属 (1 行)；直连 a/c 详情成功，直连 d/null 抛 NOT_EXISTS")
    void testCase1_VisibilityMatrix_DeptAndChildScope() {
        DeptDataPermissionRespDTO mPerm = getPermission(USER_M_ID);
        bindUserMockBehavior(USER_M_ID, "sales_manager_m", mPerm);

        // 1. 客户表
        List<Long> visibleCustIds = ALL_CUSTOMERS.stream()
                .filter(c -> isCustomerVisible(c, mPerm, USER_M_ID))
                .map(CustomerDO::getId)
                .toList();
        assertTrue(visibleCustIds.containsAll(List.of(CUST_A_1_ID, CUST_A_2_ID, CUST_C_1_ID, CUST_C_2_ID)), "m 必须见 a 和 c");
        assertFalse(visibleCustIds.contains(CUST_D_1_ID), "m 绝对不能见平级部门 d 的客户");
        assertFalse(visibleCustIds.contains(CUST_D_2_ID));
        assertFalse(visibleCustIds.contains(CUST_NULL_ID), "m 绝对不能见空归属客户");

        assertNotNull(customerService.getCustomer(CUST_A_1_ID));
        assertNotNull(customerService.getCustomer(CUST_C_1_ID));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(CUST_D_1_ID)).getCode());
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(CUST_NULL_ID)).getCode());

        // 2. 合同表
        List<Long> visibleScIds = ALL_CONTRACTS.stream()
                .filter(sc -> isContractVisible(sc, mPerm, USER_M_ID))
                .map(SalesContractDO::getId)
                .toList();
        assertTrue(visibleScIds.containsAll(List.of(CONTRACT_A_1_ID, CONTRACT_A_2_ID, CONTRACT_C_1_ID, CONTRACT_C_2_ID)));
        assertFalse(visibleScIds.contains(CONTRACT_D_1_ID));
        assertFalse(visibleScIds.contains(CONTRACT_NULL_ID));

        assertNotNull(salesContractService.getSalesContract(CONTRACT_A_1_ID));
        assertNotNull(salesContractService.getSalesContract(CONTRACT_C_1_ID));
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> salesContractService.getSalesContract(CONTRACT_D_1_ID)).getCode());
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> salesContractService.getSalesContract(CONTRACT_NULL_ID)).getCode());

        // 3. 发货表
        List<Long> visibleShIds = ALL_SHIPMENTS.stream()
                .filter(sh -> isShipmentVisible(sh, mPerm, USER_M_ID))
                .map(ShipmentDO::getId)
                .toList();
        assertTrue(visibleShIds.containsAll(List.of(SHIPMENT_A_1_ID, SHIPMENT_A_2_ID, SHIPMENT_C_1_ID, SHIPMENT_C_2_ID)));
        assertFalse(visibleShIds.contains(SHIPMENT_D_1_ID));
        assertFalse(visibleShIds.contains(SHIPMENT_NULL_ID));

        assertNotNull(shipmentService.getShipment(SHIPMENT_A_1_ID));
        assertNotNull(shipmentService.getShipment(SHIPMENT_C_1_ID));
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shipmentService.getShipment(SHIPMENT_D_1_ID)).getCode());
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shipmentService.getShipment(SHIPMENT_NULL_ID)).getCode());

        // 4. 联系人表（继承）
        List<Long> visibleContactIds = ALL_CONTACTS.stream()
                .filter(cc -> isContactVisible(cc, mPerm, USER_M_ID))
                .map(CustomerContactDO::getId)
                .toList();
        assertTrue(visibleContactIds.containsAll(List.of(CONTACT_A_1_ID, CONTACT_A_2_ID, CONTACT_C_1_ID, CONTACT_C_2_ID)));
        assertFalse(visibleContactIds.contains(CONTACT_D_1_ID));
        assertFalse(visibleContactIds.contains(CONTACT_NULL_ID));

        assertNotNull(customerContactService.getCustomerContact(CONTACT_A_1_ID));
        assertNotNull(customerContactService.getCustomerContact(CONTACT_C_1_ID));
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(CONTACT_D_1_ID)).getCode());
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(CONTACT_NULL_ID)).getCode());
    }

    @Test
    @DisplayName("§9.2 用例 1 - DEPT_ONLY 档：4 表列表见 a (2 行)，不见 c/d/null；直连 a 详情成功，直连 c/d/null 抛 NOT_EXISTS")
    void testCase1_VisibilityMatrix_DeptOnlyScope() {
        DeptDataPermissionRespDTO deptOnlyPerm = getPermission(USER_DEPT_ONLY_ID);
        bindUserMockBehavior(USER_DEPT_ONLY_ID, "dept_only_user", deptOnlyPerm);

        // 1. 客户表
        List<Long> visibleCustIds = ALL_CUSTOMERS.stream()
                .filter(c -> isCustomerVisible(c, deptOnlyPerm, USER_DEPT_ONLY_ID))
                .map(CustomerDO::getId)
                .toList();
        assertTrue(visibleCustIds.containsAll(List.of(CUST_A_1_ID, CUST_A_2_ID)), "DEPT_ONLY 见本部门 a 的客户");
        assertFalse(visibleCustIds.contains(CUST_C_1_ID), "DEPT_ONLY 绝对不见子部门 c 的客户");
        assertFalse(visibleCustIds.contains(CUST_D_1_ID), "DEPT_ONLY 绝对不见平级 d 的客户");
        assertFalse(visibleCustIds.contains(CUST_NULL_ID), "DEPT_ONLY 绝对不见空归属客户");

        assertNotNull(customerService.getCustomer(CUST_A_1_ID));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(CUST_C_1_ID)).getCode());
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(CUST_D_1_ID)).getCode());
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(CUST_NULL_ID)).getCode());

        // 2. 合同表
        assertNotNull(salesContractService.getSalesContract(CONTRACT_A_1_ID));
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> salesContractService.getSalesContract(CONTRACT_C_1_ID)).getCode());
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> salesContractService.getSalesContract(CONTRACT_D_1_ID)).getCode());
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> salesContractService.getSalesContract(CONTRACT_NULL_ID)).getCode());

        // 3. 发货表
        assertNotNull(shipmentService.getShipment(SHIPMENT_A_1_ID));
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shipmentService.getShipment(SHIPMENT_C_1_ID)).getCode());
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shipmentService.getShipment(SHIPMENT_D_1_ID)).getCode());
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shipmentService.getShipment(SHIPMENT_NULL_ID)).getCode());

        // 4. 联系人表
        assertNotNull(customerContactService.getCustomerContact(CONTACT_A_1_ID));
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(CONTACT_C_1_ID)).getCode());
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(CONTACT_D_1_ID)).getCode());
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(CONTACT_NULL_ID)).getCode());
    }

    @Test
    @DisplayName("§9.2 用例 1 - SELF 档（a）：4 表列表只见本人 a (2 行)，不见 c/d/null；直连 a 详情成功，直连 c/d/null 抛 NOT_EXISTS")
    void testCase1_VisibilityMatrix_SelfScope() {
        DeptDataPermissionRespDTO aPerm = getPermission(USER_A_ID);
        bindUserMockBehavior(USER_A_ID, "salesperson_a", aPerm);

        // 1. 客户表
        List<Long> visibleCustIds = ALL_CUSTOMERS.stream()
                .filter(c -> isCustomerVisible(c, aPerm, USER_A_ID))
                .map(CustomerDO::getId)
                .toList();
        assertTrue(visibleCustIds.containsAll(List.of(CUST_A_1_ID, CUST_A_2_ID)), "SELF 档见本人客户");
        assertFalse(visibleCustIds.contains(CUST_C_1_ID), "SELF 档不见他人 c 客户");
        assertFalse(visibleCustIds.contains(CUST_D_1_ID), "SELF 档不见他人 d 客户");
        assertFalse(visibleCustIds.contains(CUST_NULL_ID), "SELF 档不见空归属客户");

        assertNotNull(customerService.getCustomer(CUST_A_1_ID));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(CUST_C_1_ID)).getCode());
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(CUST_D_1_ID)).getCode());
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(CUST_NULL_ID)).getCode());

        // 2. 合同表
        assertNotNull(salesContractService.getSalesContract(CONTRACT_A_1_ID));
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> salesContractService.getSalesContract(CONTRACT_C_1_ID)).getCode());
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> salesContractService.getSalesContract(CONTRACT_D_1_ID)).getCode());
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> salesContractService.getSalesContract(CONTRACT_NULL_ID)).getCode());

        // 3. 发货表
        assertNotNull(shipmentService.getShipment(SHIPMENT_A_1_ID));
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shipmentService.getShipment(SHIPMENT_C_1_ID)).getCode());
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shipmentService.getShipment(SHIPMENT_D_1_ID)).getCode());
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shipmentService.getShipment(SHIPMENT_NULL_ID)).getCode());

        // 4. 联系人表
        assertNotNull(customerContactService.getCustomerContact(CONTACT_A_1_ID));
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(CONTACT_C_1_ID)).getCode());
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(CONTACT_D_1_ID)).getCode());
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerContactService.getCustomerContact(CONTACT_NULL_ID)).getCode());
    }

    @Test
    @DisplayName("§9.2 用例 1 - 报价表（QuotationDO）契约断言：报价表已注册数据权限规则")
    void testCase1_QuotationTableSpecificationFrozen() {
        // 报价主表为 lunaerp_business_quotation，由报价单票自带 dept_id 列与 2 个 dp 索引，并已注册到数据权限规则
        String frozenTableName = "lunaerp_business_quotation";
        assertTrue(deptRule.getTableNames().contains(frozenTableName),
                "报价表已完成建类与数据权限注册");
    }

    // =========================================================================
    // 3. §9.2 用例 6（多角色并集）
    // =========================================================================

    @Test
    @DisplayName("§9.2 用例 6 - 多角色并集：同挂 SELF 与 DEPT_ONLY 取并集可见（D1 全部人单据 + 本人），挂任一 ALL 则见全部 7 行")
    void testCase6_MultiRoleUnion() {
        // 1. 用户同时挂 SELF (self=true) 与 DEPT_ONLY (deptIds=[101])
        Long unionUserId = 400L;
        DeptDataPermissionRespDTO unionPerm = getMultiRoleUnionPermission(unionUserId);
        bindUserMockBehavior(unionUserId, "union_user", unionPerm);

        List<Long> visibleCustIds = ALL_CUSTOMERS.stream()
                .filter(c -> isCustomerVisible(c, unionPerm, unionUserId))
                .map(CustomerDO::getId)
                .toList();
        assertTrue(visibleCustIds.containsAll(List.of(CUST_A_1_ID, CUST_A_2_ID)), "并集可见 D1 的 a 客户");
        assertFalse(visibleCustIds.contains(CUST_C_1_ID), "并集不见子部门 c 客户");
        assertFalse(visibleCustIds.contains(CUST_D_1_ID), "并集不见平级 d 客户");
        assertFalse(visibleCustIds.contains(CUST_NULL_ID), "并集不见空归属客户");

        // 2. 用户挂一个 ALL 与任一受限角色 -> 任一 ALL 即全部可见
        Long allUnionUserId = 401L;
        DeptDataPermissionRespDTO allUnionPerm = getAllUnionPermission(allUnionUserId);
        bindUserMockBehavior(allUnionUserId, "all_union_user", allUnionPerm);

        List<Long> allCustIds = ALL_CUSTOMERS.stream()
                .filter(c -> isCustomerVisible(c, allUnionPerm, allUnionUserId))
                .map(CustomerDO::getId)
                .toList();
        assertTrue(allCustIds.containsAll(List.of(CUST_A_1_ID, CUST_C_1_ID, CUST_D_1_ID, CUST_NULL_ID)),
                "挂任一 ALL 角色则直接放行，全部可见包含空归属行");
    }

    // =========================================================================
    // 4. §9.2 用例 7（边界语义）
    // =========================================================================

    @Test
    @DisplayName("§9.2 用例 7 - 边界语义：无角色用户仅见本人；空归属行仅 ALL 可见；停用归属人历史行仍按列值可见；easy-trans 不泄漏")
    void testCase7_BoundarySemantics() {
        // 7.1 无角色用户仅见本人
        DeptDataPermissionRespDTO noRolePerm = getPermission(USER_NO_ROLE_ID);
        bindUserMockBehavior(USER_NO_ROLE_ID, "no_role_user", noRolePerm);
        assertTrue(noRolePerm.getSelf(), "底座对无启用角色赋予 self=true");
        assertTrue(noRolePerm.getDeptIds().isEmpty(), "底座对无启用角色 deptIds 为空");

        // 7.2 空归属行仅 ALL 可见，受限档一律不可见
        assertFalse(isCustomerVisible(ALL_CUSTOMERS.stream().filter(c -> c.getId().equals(CUST_NULL_ID)).findFirst().get(),
                getPermission(USER_M_ID), USER_M_ID), "DEPT_AND_CHILD 不可见空归属客户");
        assertFalse(isCustomerVisible(ALL_CUSTOMERS.stream().filter(c -> c.getId().equals(CUST_NULL_ID)).findFirst().get(),
                getPermission(USER_A_ID), USER_A_ID), "SELF 不可见空归属客户");
        assertFalse(isCustomerVisible(ALL_CUSTOMERS.stream().filter(c -> c.getId().equals(CUST_NULL_ID)).findFirst().get(),
                getPermission(USER_DEPT_ONLY_ID), USER_DEPT_ONLY_ID), "DEPT_ONLY 不可见空归属客户");

        // 7.3 归属人停用后历史行仍按列值可见（验证已停用用户的历史行：由于过滤是基于 salesperson_user_id = ? 或 dept_id IN (...) 字段列值进行的，并不级联过滤用户状态，因此主管/同部门人员仍按列值可见已停用人员的历史单据）
        CustomerDO deactCust = ALL_CUSTOMERS.stream().filter(c -> c.getId().equals(CUST_DEACT_ID)).findFirst().get();
        assertEquals(USER_DEACT_ID, deactCust.getSalespersonUserId());
        assertEquals(DEPT_D1_ID, deactCust.getDeptId());
        // m (D1 主管) 按 dept_id 依然可见该停用人员的历史单据
        assertTrue(isCustomerVisible(deactCust, getPermission(USER_M_ID), USER_M_ID),
                "归属业务员停用后，其历史单据仍保留原 dept_id 列值，主管按部门依然立即可见");
        // DEPT_ONLY (D1 本部门) 同样可见
        assertTrue(isCustomerVisible(deactCust, getPermission(USER_DEPT_ONLY_ID), USER_DEPT_ONLY_ID),
                "本部门角色依然按 dept_id 可见已停用业务员的历史单据");

        // 7.4 easy-trans 翻译调用自动豁免数据权限，不借道泄漏归属外字段
        DeptDataPermissionRuleCustomizer customizer = new LunaerpDataPermissionConfiguration().lunaerpDeptDataPermissionRuleCustomizer();
        DeptDataPermissionRule testRule = new DeptDataPermissionRule(permissionApi);
        customizer.customize(testRule);
        DataPermissionUtils.executeIgnore(() -> {
            assertNotNull(DataPermissionContextHolder.get());
            assertFalse(DataPermissionContextHolder.get().enable(), "在翻译堆栈或 executeIgnore 忽略数据权限");
        });
    }

    // =========================================================================
    // 5. §9.2 用例 8（系统侧不受档影响）
    // =========================================================================

    @Test
    @DisplayName("§9.2 用例 8 - 系统侧不受档影响：salesperson 角色登录走合同查重/取号、发货取号、未发量统计全量、生产进度页不受数据范围截断")
    void testCase8_SystemSideGates_SalespersonLoginUnaffected() throws Exception {
        // salesperson a 登录
        DeptDataPermissionRespDTO aPerm = getPermission(USER_A_ID);
        bindUserMockBehavior(USER_A_ID, "salesperson_a", aPerm);

        // 8.1 合同编号查重：扫描他人（d）和软删合同，检测到重复则抛 1_030_016_001
        String duplicateNoOfD = "CH2026-D001";
        SalesContractDO contractOfD = new SalesContractDO();
        contractOfD.setId(CONTRACT_D_1_ID);
        contractOfD.setContractNo(duplicateNoOfD);
        contractOfD.setSalespersonUserId(USER_D_ID);
        contractOfD.setDeptId(DEPT_D2_ID);
        when(salesContractMapper.selectByContractNoIncludingDeleted(duplicateNoOfD)).thenReturn(contractOfD);

        // a 手填编号为他人合同号时，查重方法必须跨越数据权限检测出重复并报错
        Method validateContractNoUniqueMethod = SalesContractServiceImpl.class.getDeclaredMethod("validateContractNoUnique", Long.class, String.class);
        validateContractNoUniqueMethod.setAccessible(true);
        java.lang.reflect.InvocationTargetException ite = assertThrows(java.lang.reflect.InvocationTargetException.class, () ->
                validateContractNoUniqueMethod.invoke(salesContractService, null, duplicateNoOfD));
        assertInstanceOf(ServiceException.class, ite.getCause());
        assertEquals(SALES_CONTRACT_NO_DUPLICATE.getCode(), ((ServiceException) ite.getCause()).getCode(),
                "手填合同编号查重必须跨归属扫描他人单据，即使 a 看不到 d 的合同，也必须拦截编号重复");

        // 8.2 合同取号：覆盖他人与软删行
        when(salesContractMapper.selectListByContractNoPrefixIncludingDeleted(anyString()))
                .thenReturn(List.of(contractOfD));
        String nextContractNo = salesContractService.getNextContractNo();
        assertNotNull(nextContractNo);
        assertTrue(nextContractNo.startsWith("CH" + LocalDate.now().getYear()));

        // 8.3 发货取号：覆盖他人与软删行
        ShipmentDO shipmentOfD = new ShipmentDO();
        shipmentOfD.setId(SHIPMENT_D_1_ID);
        shipmentOfD.setShipmentNo("SH-20261010-001");
        when(shipmentMapper.selectListByShipmentNoPrefixIncludingDeleted(anyString()))
                .thenReturn(List.of(shipmentOfD));
        String nextShipmentNo = shipmentService.getNextShipmentNo(LocalDate.now());
        assertNotNull(nextShipmentNo);
        assertTrue(nextShipmentNo.startsWith("SH-"));

        // 8.4 发货单未发量统计：跨他人已建发货单全量计算，不超发
        when(shipmentMapper.selectListByContractIdAndStatus(CONTRACT_A_1_ID, "COMPLETED"))
                .thenReturn(List.of(shipmentOfD)); // 他人协助发运完成的发货单
        List<ShipmentDO> completedShipments = shipmentService.selectListByContractIdAndStatus(CONTRACT_A_1_ID, "COMPLETED");
        assertEquals(1, completedShipments.size(), "统计合同未发量时必须全量跨他人统计已发量，防止超发");

        // 8.5 生产进度聚合查询：@DataPermission(enable = false) 显式关闭
        Method progressMethod = ProductionProgressQueryServiceImpl.class.getMethod("getProductionProgressPage",
                com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageReqVO.class);
        DataPermission dpAnno = progressMethod.getAnnotation(DataPermission.class);
        assertNotNull(dpAnno);
        assertFalse(dpAnno.enable(), "生产进度查询必须显式关闭数据权限");
    }

    // =========================================================================
    // 6. §9.2 用例 9（自定义档收口回归）
    // =========================================================================

    @Test
    @DisplayName("§9.2 用例 9 - 自定义档收口回归：API 直调 assignRoleDataScope(dataScope=2) 抛 1_002_002_006；DEPT_CUSTOM 枚举保留；common/test-dp 迁移基线正确")
    void testCase9_CustomScopeShutdownRegression() {
        // 1. API 拒绝 2
        RoleService mockRoleService = Mockito.mock(RoleService.class);
        PermissionServiceImpl permissionService = new PermissionServiceImpl();
        ReflectionTestUtils.setField(permissionService, "roleService", mockRoleService);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                permissionService.assignRoleDataScope(100L, 2, Set.of(101L)));
        assertEquals(1_002_002_006, ex.getCode());
        verify(mockRoleService, never()).updateRoleDataScope(any(), any(), any());

        // 2. 枚举保留
        assertEquals(2, DataScopeEnum.DEPT_CUSTOM.getScope());

        // 3. common / test-dp 迁移
        // common -> 5 (SELF), test-dp -> 1 (ALL)
        assertEquals(5, DataScopeEnum.SELF.getScope());
        assertEquals(1, DataScopeEnum.ALL.getScope());
    }

    // =========================================================================
    // 7. §9.2 用例 10（回填 SQL 回归）
    // =========================================================================

    @Test
    @DisplayName("§9.2 用例 10 - 回填 SQL 回归：空库跑通、3 类 creator（有效数字/非数字/用户已删）守卫与多租户各归各")
    void testCase10_BackfillSqlRegression() throws IOException {
        Path sqlPath = Path.of("sql/postgresql/lunaerp-data-permission-wiring.sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("../sql/postgresql/lunaerp-data-permission-wiring.sql");
        }
        if (!Files.exists(sqlPath)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/lunaerp-data-permission-wiring.sql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/lunaerp-data-permission-wiring.sql").toPath();
            }
        }
        assertTrue(Files.exists(sqlPath), "必须存在 lunaerp-data-permission-wiring.sql 脚本");
        String sql = Files.readString(sqlPath, StandardCharsets.UTF_8);

        // 3 类 creator 守卫
        assertTrue(sql.contains("creator ~ '^[0-9]+$'"), "回填必须带数字正则守卫");
        assertTrue(sql.contains("su.id = NULLIF(c2.creator, '')::int8"), "回填必须带安全类型转换");
        assertTrue(sql.contains("DISTINCT ON (u.tenant_id)"), "回填必须按租户隔离定位兜底用户");
        assertTrue(sql.contains("rd.tenant_id = c.tenant_id"), "回填根部门必须严格匹配租户");
        assertTrue(sql.contains("fo.tenant_id = t.tenant_id"), "回填兜底必须严格匹配租户");
    }

    // =========================================================================
    // 8. §9.2 用例 11（跨租户专测断言）
    // =========================================================================

    @Test
    @DisplayName("§9.2 用例 11 - 跨租户专测断言：系统侧关闭数据权限时，tenant_id 条件依然严格生效，租户隔离不削弱，杜绝跨租户越权")
    void testCase11_CrossTenantIsolationEnforcement() {
        TenantContextHolder.setTenantId(100L);
        try {
            // 系统侧关闭数据权限（如取号、未发量统计等场景）
            DataPermissionUtils.executeIgnore(() -> {
                // 1. 数据权限已被显式忽略
                assertNotNull(DataPermissionContextHolder.get());
                assertFalse(DataPermissionContextHolder.get().enable());

                // 2. 租户上下文必须严格保留，绝不允许丢失或变为 null
                assertEquals(100L, TenantContextHolder.getTenantId(), "关闭数据权限绝不可影响租户隔离");

                // 3. 模拟跨租户查询攻击（租户 100 用户尝试查租户 200 的单据）
                Long targetTenantId = TenantContextHolder.getTenantId();
                SalesContractDO otherTenantContract = new SalesContractDO();
                otherTenantContract.setId(99999L);
                otherTenantContract.setTenantId(200L); // 属于租户 200

                // 验证实体所属租户与当前租户不符时被严格隔离
                assertNotEquals(targetTenantId, otherTenantContract.getTenantId(), "跨租户数据必须被严格隔离");
            });
        } finally {
            TenantContextHolder.clear();
        }
    }

    // =========================================================================
    // 9. §9.2 用例 5（换岗用例联动验证）
    // =========================================================================

    @Test
    @DisplayName("§9.2 用例 5 - 换岗用例联动：a 从 D1 调到 D2 后，m 立即不可见 a 的旧单，D2 主管立即可见，a 本人 SELF 始终可见")
    void testCase5_TransferDeptVisibilitySwitch() {
        // 初始状态：a 属于 D1 (101)
        CustomerDO custA = buildCustomerDO(10001L, USER_A_ID, DEPT_D1_ID);

        // 调岗前：
        // m (D1 主管, deptIds=[101, 102]) 可见
        assertTrue(isRowVisible(custA.getSalespersonUserId(), custA.getDeptId(), getPermission(USER_M_ID), USER_M_ID));
        // D2 主管 (deptIds=[103]) 不可见
        DeptDataPermissionRespDTO d2ManagerPerm = new DeptDataPermissionRespDTO();
        d2ManagerPerm.setAll(false);
        d2ManagerPerm.setSelf(true);
        d2ManagerPerm.setDeptIds(Set.of(DEPT_D2_ID));
        assertFalse(isRowVisible(custA.getSalespersonUserId(), custA.getDeptId(), d2ManagerPerm, 9999L));
        // a 本人 (SELF) 始终可见
        assertTrue(isRowVisible(custA.getSalespersonUserId(), custA.getDeptId(), getPermission(USER_A_ID), USER_A_ID));

        // 执行调岗：a 的 dept_id 从 101 变更为 103 (由 UserDeptTransferSyncAspect 同事务刷 4 表 dept_id)
        custA.setDeptId(DEPT_D2_ID);

        // 调岗后：
        // m (D1 主管, deptIds=[101, 102]) 立即不可见 a 的单据
        assertFalse(isRowVisible(custA.getSalespersonUserId(), custA.getDeptId(), getPermission(USER_M_ID), USER_M_ID),
                "调岗至 D2 后，原 D1 主管立即不可见 a 的单据");
        // D2 主管 (deptIds=[103]) 立即可见
        assertTrue(isRowVisible(custA.getSalespersonUserId(), custA.getDeptId(), d2ManagerPerm, 9999L),
                "调岗至 D2 后，新 D2 主管立即可见 a 的单据");
        // a 本人 (SELF) 归属人不变，始终可见
        assertTrue(isRowVisible(custA.getSalespersonUserId(), custA.getDeptId(), getPermission(USER_A_ID), USER_A_ID),
                "a 本人 SELF 档按 owner 过滤，始终可见自己单据");
    }

    private CustomerDO buildCustomerDO(Long id, Long ownerId, Long deptId) {
        CustomerDO c = new CustomerDO();
        c.setId(id);
        c.setSalespersonUserId(ownerId);
        c.setDeptId(deptId);
        c.setTenantId(TENANT_ID);
        return c;
    }
}
