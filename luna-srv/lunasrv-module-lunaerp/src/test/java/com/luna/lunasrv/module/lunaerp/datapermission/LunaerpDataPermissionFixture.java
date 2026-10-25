package com.luna.lunasrv.module.lunaerp.datapermission;

import com.luna.lunasrv.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.luna.lunasrv.framework.common.enums.UserTypeEnum;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * §9.1 组织造数可复用 Fixture（与验收 §6.2 同构）
 *
 * 组织结构：
 * - 部门：D1 (101L, 父部门 0), D1.1 (102L, 父部门 101L), D2 (103L, 平级父部门 0)
 * - 用户与角色：
 *   - boss (1000L): ALL 档 (data_scope = 1)
 *   - m (1001L): D1 主管 sales_manager, DEPT_AND_CHILD 档 (data_scope = 4)
 *   - a (1002L): D1 业务员 salesperson, SELF 档 (data_scope = 5)
 *   - c (1003L): D1.1 业务员 salesperson, SELF 档 (data_scope = 5)
 *   - d (1004L): D2 业务员 salesperson, SELF 档 (data_scope = 5)
 *   - dept_only_user (1005L): D1 仅本部门角色, DEPT_ONLY 档 (data_scope = 3)
 *   - user_deact (1006L): D1 停用业务员 (status = 1)
 *   - user_no_role (1007L): 无启用角色用户 (底座默认仅看本人)
 *
 * 4 张已存表：
 * - 客户、销售合同、发货单、联系人各造归属 a/c/d 的行各 ≥2（含不同状态），空归属行 ≥1，停用人员行 1
 * - 报价表（QuotationDO）用例文本与数据结构冻结，随 05 票同批施工补跑
 *
 * @author luna
 */
public class LunaerpDataPermissionFixture {

    public static final Long TENANT_ID = 1L;

    // 部门常量
    public static final Long DEPT_D1_ID = 101L;
    public static final Long DEPT_D1_1_ID = 102L;
    public static final Long DEPT_D2_ID = 103L;

    // 用户常量
    public static final Long USER_BOSS_ID = 1000L;
    public static final Long USER_M_ID = 1001L;
    public static final Long USER_A_ID = 1002L;
    public static final Long USER_C_ID = 1003L;
    public static final Long USER_D_ID = 1004L;
    public static final Long USER_DEPT_ONLY_ID = 1005L;
    public static final Long USER_DEACT_ID = 1006L;
    public static final Long USER_NO_ROLE_ID = 1007L;

    // 客户 ID 常量
    public static final Long CUST_A_1_ID = 10001L;
    public static final Long CUST_A_2_ID = 10002L;
    public static final Long CUST_C_1_ID = 10003L;
    public static final Long CUST_C_2_ID = 10004L;
    public static final Long CUST_D_1_ID = 10005L;
    public static final Long CUST_D_2_ID = 10006L;
    public static final Long CUST_NULL_ID = 10007L;
    public static final Long CUST_DEACT_ID = 10008L;

    // 合同 ID 常量
    public static final Long CONTRACT_A_1_ID = 20001L;
    public static final Long CONTRACT_A_2_ID = 20002L;
    public static final Long CONTRACT_C_1_ID = 20003L;
    public static final Long CONTRACT_C_2_ID = 20004L;
    public static final Long CONTRACT_D_1_ID = 20005L;
    public static final Long CONTRACT_D_2_ID = 20006L;
    public static final Long CONTRACT_NULL_ID = 20007L;
    public static final Long CONTRACT_DEACT_ID = 20008L;

    // 发货单 ID 常量
    public static final Long SHIPMENT_A_1_ID = 30001L;
    public static final Long SHIPMENT_A_2_ID = 30002L;
    public static final Long SHIPMENT_C_1_ID = 30003L;
    public static final Long SHIPMENT_C_2_ID = 30004L;
    public static final Long SHIPMENT_D_1_ID = 30005L;
    public static final Long SHIPMENT_D_2_ID = 30006L;
    public static final Long SHIPMENT_NULL_ID = 30007L;
    public static final Long SHIPMENT_DEACT_ID = 30008L;

    // 联系人 ID 常量
    public static final Long CONTACT_A_1_ID = 40001L;
    public static final Long CONTACT_A_2_ID = 40002L;
    public static final Long CONTACT_C_1_ID = 40003L;
    public static final Long CONTACT_C_2_ID = 40004L;
    public static final Long CONTACT_D_1_ID = 40005L;
    public static final Long CONTACT_D_2_ID = 40006L;
    public static final Long CONTACT_NULL_ID = 40007L;
    public static final Long CONTACT_DEACT_ID = 40008L;

    // 内存数据列表
    public static final List<CustomerDO> ALL_CUSTOMERS = createCustomers();
    public static final List<SalesContractDO> ALL_CONTRACTS = createContracts();
    public static final List<ShipmentDO> ALL_SHIPMENTS = createShipments();
    public static final List<CustomerContactDO> ALL_CONTACTS = createContacts();

    public static void mockLogin(Long userId, String username) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        loginUser.setTenantId(TENANT_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    public static DeptDataPermissionRespDTO getPermission(Long userId) {
        DeptDataPermissionRespDTO dto = new DeptDataPermissionRespDTO();
        if (USER_BOSS_ID.equals(userId)) {
            // ALL 档
            dto.setAll(true);
            dto.setSelf(false);
            dto.setDeptIds(Collections.emptySet());
        } else if (USER_M_ID.equals(userId)) {
            // DEPT_AND_CHILD 档：见 D1(101) 和 D1.1(102)
            dto.setAll(false);
            dto.setSelf(true);
            dto.setDeptIds(Set.of(DEPT_D1_ID, DEPT_D1_1_ID));
        } else if (USER_A_ID.equals(userId) || USER_C_ID.equals(userId) || USER_D_ID.equals(userId)) {
            // SELF 档
            dto.setAll(false);
            dto.setSelf(true);
            dto.setDeptIds(Collections.emptySet());
        } else if (USER_DEPT_ONLY_ID.equals(userId)) {
            // DEPT_ONLY 档：仅见 D1(101)
            dto.setAll(false);
            dto.setSelf(false);
            dto.setDeptIds(Set.of(DEPT_D1_ID));
        } else if (USER_NO_ROLE_ID.equals(userId)) {
            // 无启用角色：底座默认仅看本人
            dto.setAll(false);
            dto.setSelf(true);
            dto.setDeptIds(Collections.emptySet());
        } else {
            // 默认兜底
            dto.setAll(false);
            dto.setSelf(false);
            dto.setDeptIds(Collections.emptySet());
        }
        return dto;
    }

    public static DeptDataPermissionRespDTO getMultiRoleUnionPermission(Long userId) {
        // 用户同时挂 SELF (self=true) 与 DEPT_ONLY (deptIds=[101])
        DeptDataPermissionRespDTO dto = new DeptDataPermissionRespDTO();
        dto.setAll(false);
        dto.setSelf(true);
        dto.setDeptIds(Set.of(DEPT_D1_ID));
        return dto;
    }

    public static DeptDataPermissionRespDTO getAllUnionPermission(Long userId) {
        // 用户挂一个 ALL (all=true) 与任一受限角色 -> 任一 ALL 即全部
        DeptDataPermissionRespDTO dto = new DeptDataPermissionRespDTO();
        dto.setAll(true);
        dto.setSelf(true);
        dto.setDeptIds(Set.of(DEPT_D1_ID));
        return dto;
    }

    /**
     * 判断单据行是否对当前权限上下文可见
     */
    public static boolean isRowVisible(Long ownerUserId, Long deptId, DeptDataPermissionRespDTO perm, Long currentUserId) {
        if (perm == null) {
            return false;
        }
        if (Boolean.TRUE.equals(perm.getAll())) {
            return true;
        }
        // 空归属行仅 ALL 档可见
        if (ownerUserId == null && deptId == null) {
            return false;
        }
        // 部门过滤
        boolean deptMatch = deptId != null && perm.getDeptIds() != null && perm.getDeptIds().contains(deptId);
        // 本人过滤
        boolean selfMatch = Boolean.TRUE.equals(perm.getSelf()) && ownerUserId != null && ownerUserId.equals(currentUserId);
        return deptMatch || selfMatch;
    }

    public static boolean isCustomerVisible(CustomerDO customer, DeptDataPermissionRespDTO perm, Long currentUserId) {
        return isRowVisible(customer.getSalespersonUserId(), customer.getDeptId(), perm, currentUserId);
    }

    public static boolean isContractVisible(SalesContractDO contract, DeptDataPermissionRespDTO perm, Long currentUserId) {
        return isRowVisible(contract.getSalespersonUserId(), contract.getDeptId(), perm, currentUserId);
    }

    public static boolean isShipmentVisible(ShipmentDO shipment, DeptDataPermissionRespDTO perm, Long currentUserId) {
        return isRowVisible(shipment.getSalespersonUserId(), shipment.getDeptId(), perm, currentUserId);
    }

    public static boolean isContactVisible(CustomerContactDO contact, DeptDataPermissionRespDTO perm, Long currentUserId) {
        // 联系人通过父客户的可见性继承
        CustomerDO parentCustomer = ALL_CUSTOMERS.stream()
                .filter(c -> c.getId().equals(contact.getCustomerId()))
                .findFirst()
                .orElse(null);
        if (parentCustomer == null) {
            return false;
        }
        return isCustomerVisible(parentCustomer, perm, currentUserId);
    }

    // ==========================================
    // 数据构造工厂
    // ==========================================
    private static List<CustomerDO> createCustomers() {
        return List.of(
                buildCustomer(CUST_A_1_ID, "织造集团A-1", "ACTIVE", USER_A_ID, DEPT_D1_ID),
                buildCustomer(CUST_A_2_ID, "织造集团A-2", "INACTIVE", USER_A_ID, DEPT_D1_ID),
                buildCustomer(CUST_C_1_ID, "纺织公司C-1", "ACTIVE", USER_C_ID, DEPT_D1_1_ID),
                buildCustomer(CUST_C_2_ID, "纺织公司C-2", "INACTIVE", USER_C_ID, DEPT_D1_1_ID),
                buildCustomer(CUST_D_1_ID, "印染企业D-1", "ACTIVE", USER_D_ID, DEPT_D2_ID),
                buildCustomer(CUST_D_2_ID, "印染企业D-2", "INACTIVE", USER_D_ID, DEPT_D2_ID),
                buildCustomer(CUST_NULL_ID, "历史公海客户-空归属", "ACTIVE", null, null),
                buildCustomer(CUST_DEACT_ID, "停用业务员历史客户", "ACTIVE", USER_DEACT_ID, DEPT_D1_ID)
        );
    }

    private static CustomerDO buildCustomer(Long id, String name, String status, Long salespersonUserId, Long deptId) {
        CustomerDO c = new CustomerDO();
        c.setId(id);
        c.setCompanyName(name);
        c.setShortName(name);
        c.setStatus(status);
        c.setSalespersonUserId(salespersonUserId);
        c.setDeptId(deptId);
        c.setTenantId(TENANT_ID);
        return c;
    }

    private static List<SalesContractDO> createContracts() {
        return List.of(
                buildContract(CONTRACT_A_1_ID, "CH2026-A001", CUST_A_1_ID, USER_A_ID, DEPT_D1_ID, "ACTIVE"),
                buildContract(CONTRACT_A_2_ID, "CH2026-A002", CUST_A_2_ID, USER_A_ID, DEPT_D1_ID, "COMPLETED"),
                buildContract(CONTRACT_C_1_ID, "CH2026-C001", CUST_C_1_ID, USER_C_ID, DEPT_D1_1_ID, "ACTIVE"),
                buildContract(CONTRACT_C_2_ID, "CH2026-C002", CUST_C_2_ID, USER_C_ID, DEPT_D1_1_ID, "COMPLETED"),
                buildContract(CONTRACT_D_1_ID, "CH2026-D001", CUST_D_1_ID, USER_D_ID, DEPT_D2_ID, "ACTIVE"),
                buildContract(CONTRACT_D_2_ID, "CH2026-D002", CUST_D_2_ID, USER_D_ID, DEPT_D2_ID, "COMPLETED"),
                buildContract(CONTRACT_NULL_ID, "CH2026-NULL01", CUST_NULL_ID, null, null, "ACTIVE"),
                buildContract(CONTRACT_DEACT_ID, "CH2026-DEACT01", CUST_DEACT_ID, USER_DEACT_ID, DEPT_D1_ID, "ACTIVE")
        );
    }

    private static SalesContractDO buildContract(Long id, String contractNo, Long customerId, Long salespersonUserId, Long deptId, String status) {
        SalesContractDO sc = new SalesContractDO();
        sc.setId(id);
        sc.setContractNo(contractNo);
        sc.setCustomerId(customerId);
        sc.setClientName("客户快照-" + customerId);
        sc.setSalespersonUserId(salespersonUserId);
        sc.setDeptId(deptId);
        sc.setStatus(status);
        sc.setSignDate(LocalDate.now());
        sc.setTotalQuantity(BigDecimal.ZERO);
        sc.setTotalAmount(BigDecimal.ZERO);
        sc.setTenantId(TENANT_ID);
        return sc;
    }

    private static List<ShipmentDO> createShipments() {
        return List.of(
                buildShipment(SHIPMENT_A_1_ID, "SH-20261010-A01", CUST_A_1_ID, CONTRACT_A_1_ID, USER_A_ID, DEPT_D1_ID, "DRAFT"),
                buildShipment(SHIPMENT_A_2_ID, "SH-20261010-A02", CUST_A_2_ID, CONTRACT_A_2_ID, USER_A_ID, DEPT_D1_ID, "COMPLETED"),
                buildShipment(SHIPMENT_C_1_ID, "SH-20261010-C01", CUST_C_1_ID, CONTRACT_C_1_ID, USER_C_ID, DEPT_D1_1_ID, "DRAFT"),
                buildShipment(SHIPMENT_C_2_ID, "SH-20261010-C02", CUST_C_2_ID, CONTRACT_C_2_ID, USER_C_ID, DEPT_D1_1_ID, "COMPLETED"),
                buildShipment(SHIPMENT_D_1_ID, "SH-20261010-D01", CUST_D_1_ID, CONTRACT_D_1_ID, USER_D_ID, DEPT_D2_ID, "DRAFT"),
                buildShipment(SHIPMENT_D_2_ID, "SH-20261010-D02", CUST_D_2_ID, CONTRACT_D_2_ID, USER_D_ID, DEPT_D2_ID, "COMPLETED"),
                buildShipment(SHIPMENT_NULL_ID, "SH-20261010-NULL1", CUST_NULL_ID, CONTRACT_NULL_ID, null, null, "DRAFT"),
                buildShipment(SHIPMENT_DEACT_ID, "SH-20261010-DEACT1", CUST_DEACT_ID, CONTRACT_DEACT_ID, USER_DEACT_ID, DEPT_D1_ID, "DRAFT")
        );
    }

    private static ShipmentDO buildShipment(Long id, String shipmentNo, Long customerId, Long contractId, Long salespersonUserId, Long deptId, String status) {
        ShipmentDO sh = new ShipmentDO();
        sh.setId(id);
        sh.setShipmentNo(shipmentNo);
        sh.setCustomerId(customerId);
        sh.setCustomerName("客户快照-" + customerId);
        sh.setSalesContractId(contractId);
        sh.setWarehouseId(1L);
        sh.setSalespersonUserId(salespersonUserId);
        sh.setDeptId(deptId);
        sh.setStatus(status);
        sh.setTenantId(TENANT_ID);
        return sh;
    }

    private static List<CustomerContactDO> createContacts() {
        return List.of(
                buildContact(CONTACT_A_1_ID, CUST_A_1_ID, "张总(A1联系人)"),
                buildContact(CONTACT_A_2_ID, CUST_A_2_ID, "李主管(A2联系人)"),
                buildContact(CONTACT_C_1_ID, CUST_C_1_ID, "王经理(C1联系人)"),
                buildContact(CONTACT_C_2_ID, CUST_C_2_ID, "赵专员(C2联系人)"),
                buildContact(CONTACT_D_1_ID, CUST_D_1_ID, "孙厂长(D1联系人)"),
                buildContact(CONTACT_D_2_ID, CUST_D_2_ID, "周调度(D2联系人)"),
                buildContact(CONTACT_NULL_ID, CUST_NULL_ID, "吴公海(NULL联系人)"),
                buildContact(CONTACT_DEACT_ID, CUST_DEACT_ID, "郑历史(DEACT联系人)")
        );
    }

    private static CustomerContactDO buildContact(Long id, Long customerId, String name) {
        CustomerContactDO cc = new CustomerContactDO();
        cc.setId(id);
        cc.setCustomerId(customerId);
        cc.setName(name);
        cc.setTenantId(TENANT_ID);
        return cc;
    }
}
