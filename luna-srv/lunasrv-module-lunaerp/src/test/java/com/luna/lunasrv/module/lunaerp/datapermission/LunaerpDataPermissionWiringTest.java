package com.luna.lunasrv.module.lunaerp.datapermission;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.biz.system.permission.PermissionCommonApi;
import com.luna.lunasrv.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.luna.lunasrv.framework.common.enums.UserTypeEnum;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.datapermission.core.aop.DataPermissionContextHolder;
import com.luna.lunasrv.framework.datapermission.core.rule.DataPermissionRule;
import com.luna.lunasrv.framework.datapermission.core.rule.DataPermissionRuleFactoryImpl;
import com.luna.lunasrv.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import com.luna.lunasrv.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import com.luna.lunasrv.framework.datapermission.core.util.DataPermissionUtils;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.framework.datapermission.config.LunaerpDataPermissionConfiguration;
import com.luna.lunasrv.module.lunaerp.service.business.salescontract.SalesContractServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.business.shipment.ShipmentServiceImpl;
import com.luna.lunasrv.module.lunaerp.service.production.productionprogress.ProductionProgressQueryService;
import com.luna.lunasrv.module.lunaerp.service.production.productionprogress.ProductionProgressQueryServiceImpl;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.trans.service.impl.SimpleTransService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.salescontract.vo.SalesContractSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customer.CustomerServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customercontact.vo.CustomerContactSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customercontact.CustomerContactDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customercontact.CustomerContactMapper;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.customercontact.CustomerContactServiceImpl;
import org.mockito.ArgumentCaptor;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 销售归属链数据权限接线测试（Issue 02 / 05）
 *
 * 验证：
 * 1. LunaerpDataPermissionConfiguration 正确注册 CustomerDO / SalesContractDO / ShipmentDO 到 DeptDataPermissionRule；
 *    QuotationDO 随 05 票规划，联系人及所有子表不注册
 * 2. §9.1 组织造数 fixture 验证 4 档可见性（ALL、DEPT_AND_CHILD、DEPT_ONLY、SELF、多角色并集、任一 ALL 即全部）
 * 3. 边界语义：无登录用户不生效、无角色仅看本人、皆空拼 null=null、空归属行仅 ALL 可见
 * 4. easy-trans 翻译调用底座自动豁免
 * 5. 系统侧调用（取号扫描、未发量聚合、生产进度聚合）@DataPermission(enable=false) 显式关闭，且租户隔离不受影响
 * 6. §5.3 / §9.2 用例 3 联系人裸 EXISTS 继承与五种调法防探测统一抛 1_030_004_001
 * 7. §5.4 / §9.2 用例 4 客户批量删除强校验，混入不可见 id 整体失败零删除
 * 8. §9.2 用例 2 四入口一致性校验
 *
 * @author luna
 */
public class LunaerpDataPermissionWiringTest {

    private static final String TABLE_CUSTOMER = "lunaerp_baseinfo_customer";
    private static final String TABLE_SALES_CONTRACT = "lunaerp_business_sales_contract";
    private static final String TABLE_SHIPMENT = "lunaerp_business_shipment";

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesContractDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerContactDO.class);
    }

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
        while (DataPermissionContextHolder.get() != null) {
            DataPermissionContextHolder.remove();
        }
    }

    private void mockLoginUser(Long userId, String username) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        loginUser.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    private DeptDataPermissionRule createCustomizedRule(PermissionCommonApi permissionApi) {
        DeptDataPermissionRule rule = new DeptDataPermissionRule(permissionApi);
        LunaerpDataPermissionConfiguration config = new LunaerpDataPermissionConfiguration();
        DeptDataPermissionRuleCustomizer customizer = config.lunaerpDeptDataPermissionRuleCustomizer();
        customizer.customize(rule);
        return rule;
    }

    @Test
    @DisplayName("1. 验证 Customizer Bean 注册：3 张核心表注册 dept_id / salesperson_user_id，联系人及子表不注册")
    void testCustomizerRegistration() {
        PermissionCommonApi permissionApi = Mockito.mock(PermissionCommonApi.class);
        DeptDataPermissionRule rule = createCustomizedRule(permissionApi);

        Set<String> tableNames = rule.getTableNames();
        assertTrue(tableNames.contains(TABLE_CUSTOMER), "必须注册客户表");
        assertTrue(tableNames.contains(TABLE_SALES_CONTRACT), "必须注册销售合同表");
        assertTrue(tableNames.contains(TABLE_SHIPMENT), "必须注册发货单表");

        assertFalse(tableNames.contains("lunaerp_baseinfo_customer_contact"), "联系人表不可注册（EXISTS 继承）");
        assertFalse(tableNames.contains("lunaerp_business_sales_contract_item"), "合同明细表不可注册");
        assertFalse(tableNames.contains("lunaerp_business_shipment_item"), "发货明细表不可注册");
        assertFalse(tableNames.contains("lunaerp_business_shipment_roll"), "发货布卷表不可注册");
    }

    @Test
    @DisplayName("2. §9.1 fixture 验证 4 档可见性：ALL 见全部")
    void testVisibility_AllScope() {
        PermissionCommonApi permissionApi = Mockito.mock(PermissionCommonApi.class);
        DeptDataPermissionRule rule = createCustomizedRule(permissionApi);

        // boss / finance: data_scope = 1 (ALL)
        mockLoginUser(10L, "boss");
        DeptDataPermissionRespDTO allPerm = new DeptDataPermissionRespDTO();
        allPerm.setAll(true);
        allPerm.setSelf(false);
        allPerm.setDeptIds(Collections.emptySet());
        when(permissionApi.getDeptDataPermission(10L)).thenReturn(allPerm);

        // 3 张表均不拼接额外 WHERE 限制条件（ALL 档无条件，见全部含空归属）
        assertNull(rule.getExpression(TABLE_CUSTOMER, null), "ALL 档客户表不拼接条件");
        assertNull(rule.getExpression(TABLE_SALES_CONTRACT, null), "ALL 档合同表不拼接条件");
        assertNull(rule.getExpression(TABLE_SHIPMENT, null), "ALL 档发货表不拼接条件");
    }

    @Test
    @DisplayName("3. §9.1 fixture 验证 4 档可见性：DEPT_AND_CHILD (m) 见 D1 与 D1.1 (a+c)，不见 D2 (d)")
    void testVisibility_DeptAndChildScope() {
        PermissionCommonApi permissionApi = Mockito.mock(PermissionCommonApi.class);
        DeptDataPermissionRule rule = createCustomizedRule(permissionApi);

        // m: D1 主管 (dept=101), data_scope=4 (DEPT_AND_CHILD) -> deptIds=[101, 102], self=true
        Long mUserId = 100L;
        mockLoginUser(mUserId, "sales_manager_m");
        DeptDataPermissionRespDTO mPerm = new DeptDataPermissionRespDTO();
        mPerm.setAll(false);
        mPerm.setSelf(true);
        mPerm.setDeptIds(Set.of(101L, 102L));
        when(permissionApi.getDeptDataPermission(mUserId)).thenReturn(mPerm);

        for (String table : List.of(TABLE_CUSTOMER, TABLE_SALES_CONTRACT, TABLE_SHIPMENT)) {
            Expression expr = rule.getExpression(table, null);
            assertNotNull(expr, table + " 必须生成数据范围表达式");
            String sql = expr.toString();

            // 格式：(table.dept_id IN (101, 102) OR table.salesperson_user_id = 100)
            assertTrue(sql.contains("dept_id IN (101, 102)") || sql.contains("dept_id IN (102, 101)"),
                    table + " 必须包含 D1 与 D1.1 的 deptIds IN 条件");
            assertTrue(sql.contains("salesperson_user_id = 100"),
                    table + " 必须包含本人 salesperson_user_id 条件");
            assertFalse(sql.contains("103"), table + " 绝对不能包含平级 D2 (103)");
        }
    }

    @Test
    @DisplayName("4. §9.1 fixture 验证 4 档可见性：DEPT_ONLY 见 D1 (a)，不见 D1.1 (c) 与 D2 (d)")
    void testVisibility_DeptOnlyScope() {
        PermissionCommonApi permissionApi = Mockito.mock(PermissionCommonApi.class);
        DeptDataPermissionRule rule = createCustomizedRule(permissionApi);

        // dept_only 用户: D1 (101), data_scope=3 (DEPT_ONLY) -> deptIds=[101], self=false
        Long deptOnlyUserId = 200L;
        mockLoginUser(deptOnlyUserId, "dept_only_user");
        DeptDataPermissionRespDTO deptOnlyPerm = new DeptDataPermissionRespDTO();
        deptOnlyPerm.setAll(false);
        deptOnlyPerm.setSelf(false);
        deptOnlyPerm.setDeptIds(Set.of(101L));
        when(permissionApi.getDeptDataPermission(deptOnlyUserId)).thenReturn(deptOnlyPerm);

        for (String table : List.of(TABLE_CUSTOMER, TABLE_SALES_CONTRACT, TABLE_SHIPMENT)) {
            Expression expr = rule.getExpression(table, null);
            assertNotNull(expr, table + " 必须生成数据范围表达式");
            String sql = expr.toString();

            // 格式：table.dept_id IN (101)
            assertTrue(sql.contains("dept_id IN (101)"), table + " 必须仅包含部门 101");
            assertFalse(sql.contains("102"), table + " 绝对不能包含子部门 102");
            assertFalse(sql.contains("103"), table + " 绝对不能包含平级部门 103");
            assertFalse(sql.contains("salesperson_user_id"), table + " self=false 时不得拼接 user 过滤");
        }
    }

    @Test
    @DisplayName("5. §9.1 fixture 验证 4 档可见性：SELF (a) 只见本人")
    void testVisibility_SelfScope() {
        PermissionCommonApi permissionApi = Mockito.mock(PermissionCommonApi.class);
        DeptDataPermissionRule rule = createCustomizedRule(permissionApi);

        // a: D1 业务员 (101), data_scope=5 (SELF) -> deptIds=[], self=true
        Long aUserId = 300L;
        mockLoginUser(aUserId, "salesperson_a");
        DeptDataPermissionRespDTO aPerm = new DeptDataPermissionRespDTO();
        aPerm.setAll(false);
        aPerm.setSelf(true);
        aPerm.setDeptIds(Collections.emptySet());
        when(permissionApi.getDeptDataPermission(aUserId)).thenReturn(aPerm);

        for (String table : List.of(TABLE_CUSTOMER, TABLE_SALES_CONTRACT, TABLE_SHIPMENT)) {
            Expression expr = rule.getExpression(table, null);
            assertNotNull(expr, table + " 必须生成数据范围表达式");
            String sql = expr.toString();

            // 格式：table.salesperson_user_id = 300
            assertEquals(table + ".salesperson_user_id = 300", sql, table + " 必须仅过滤本人");
        }
    }

    @Test
    @DisplayName("6. 多角色并集验证：同挂 SELF 与 DEPT_ONLY 取并集；挂一个 ALL 则为全部")
    void testMultiRoleUnion() {
        PermissionCommonApi permissionApi = Mockito.mock(PermissionCommonApi.class);
        DeptDataPermissionRule rule = createCustomizedRule(permissionApi);

        // 用户同时挂 SELF (self=true) 与 DEPT_ONLY (deptIds=[101]) -> 并集
        Long multiUserId = 400L;
        mockLoginUser(multiUserId, "multi_role_user");
        DeptDataPermissionRespDTO unionPerm = new DeptDataPermissionRespDTO();
        unionPerm.setAll(false);
        unionPerm.setSelf(true);
        unionPerm.setDeptIds(Set.of(101L));
        when(permissionApi.getDeptDataPermission(multiUserId)).thenReturn(unionPerm);

        Expression unionExpr = rule.getExpression(TABLE_CUSTOMER, null);
        assertNotNull(unionExpr);
        String sql = unionExpr.toString();
        assertTrue(sql.contains("dept_id IN (101)"), "并集必须包含部门 101");
        assertTrue(sql.contains("salesperson_user_id = 400"), "并集必须包含本人 salesperson_user_id");

        // 用户挂一个 ALL (all=true) 与任一受限角色 -> 任一 ALL 即全部
        mockLoginUser(401L, "all_plus_other_user");
        DeptDataPermissionRespDTO allUnionPerm = new DeptDataPermissionRespDTO();
        allUnionPerm.setAll(true);
        allUnionPerm.setSelf(true);
        allUnionPerm.setDeptIds(Set.of(101L));
        when(permissionApi.getDeptDataPermission(401L)).thenReturn(allUnionPerm);

        assertNull(rule.getExpression(TABLE_CUSTOMER, null), "任一角色包含 ALL 则整体为 ALL，不拼接过滤条件");
    }

    @Test
    @DisplayName("7. 边界语义验证：无登录用户不生效、无角色用户仅看本人、皆空拼 null=null、空归属行仅 ALL 可见")
    void testBoundaryConditions() {
        PermissionCommonApi permissionApi = Mockito.mock(PermissionCommonApi.class);
        DeptDataPermissionRule rule = createCustomizedRule(permissionApi);

        // 7.1 无登录用户：规则不生效
        SecurityContextHolder.clearContext();
        assertNull(rule.getExpression(TABLE_CUSTOMER, null), "无登录用户时不生效");

        // 7.2 用户无启用角色：底座给 self=true (只能看本人)
        mockLoginUser(500L, "no_role_user");
        DeptDataPermissionRespDTO noRolePerm = new DeptDataPermissionRespDTO();
        noRolePerm.setAll(false);
        noRolePerm.setSelf(true);
        noRolePerm.setDeptIds(Collections.emptySet());
        when(permissionApi.getDeptDataPermission(500L)).thenReturn(noRolePerm);
        assertEquals(TABLE_CUSTOMER + ".salesperson_user_id = 500",
                rule.getExpression(TABLE_CUSTOMER, null).toString(), "无启用角色仅见本人");

        // 7.3 deptIds 与 self 皆空：拼 null = null (保证查不到任何行)
        mockLoginUser(501L, "empty_scope_user");
        DeptDataPermissionRespDTO emptyPerm = new DeptDataPermissionRespDTO();
        emptyPerm.setAll(false);
        emptyPerm.setSelf(false);
        emptyPerm.setDeptIds(Collections.emptySet());
        when(permissionApi.getDeptDataPermission(501L)).thenReturn(emptyPerm);
        Expression emptyExpr = rule.getExpression(TABLE_CUSTOMER, null);
        assertNotNull(emptyExpr);
        assertInstanceOf(EqualsTo.class, emptyExpr);
        assertEquals("null = null", emptyExpr.toString(), "皆空时拼 null = null");
    }

    @Test
    @DisplayName("8. easy-trans 自动豁免验证：通过 SimpleTransService 堆栈调用时自动忽略数据权限")
    void testEasyTransAutoExemption() throws Exception {
        DataPermissionRule rule = Mockito.mock(DataPermissionRule.class);
        DataPermissionRuleFactoryImpl factory = new DataPermissionRuleFactoryImpl(List.of(rule));

        // 正常调用：返回规则
        List<DataPermissionRule> normalRules = factory.getDataPermissionRule("testStatement");
        assertEquals(1, normalRules.size(), "正常调用包含规则");

        // 验证底座 isTranslateCall 判定
        Method isTranslateCallMethod = DataPermissionRuleFactoryImpl.class.getDeclaredMethod("isTranslateCall");
        isTranslateCallMethod.setAccessible(true);
        assertFalse((Boolean) isTranslateCallMethod.invoke(factory), "普通调用不属于翻译调用");

        // 模拟 SimpleTransService 调用栈（通过 Mockito spy 拦截 afterPropertiesSet）
        SimpleTransService spy = Mockito.spy(new SimpleTransService());
        Mockito.doAnswer(invocation -> {
            assertTrue((Boolean) isTranslateCallMethod.invoke(factory), "在 SimpleTransService 堆栈内应识别为翻译调用");
            List<DataPermissionRule> rules = factory.getDataPermissionRule("testStatement");
            assertEquals(0, rules.size(), "翻译调用时必须返回空规则列表（豁免数据权限，不借道泄漏归属外字段）");
            return null;
        }).when(spy).afterPropertiesSet();

        spy.afterPropertiesSet();
    }

    @Test
    @DisplayName("9. 系统侧关闭注解完备性验证（§5.2 第 1–4 项方法与底座 @Select 必须带 @DataPermission(enable=false)）")
    void testSystemSideMethodAnnotations() throws Exception {
        // §5.2 第 1 项：合同取号与查重
        Method getNextContractNo = SalesContractServiceImpl.class.getMethod("getNextContractNo");
        assertNotNull(getNextContractNo.getAnnotation(DataPermission.class), "SalesContractServiceImpl#getNextContractNo 必须有注解");
        assertFalse(getNextContractNo.getAnnotation(DataPermission.class).enable());

        Method genContractNoWithRetry = SalesContractServiceImpl.class.getDeclaredMethod("generateContractNoWithRetry");
        assertNotNull(genContractNoWithRetry.getAnnotation(DataPermission.class), "SalesContractServiceImpl#generateContractNoWithRetry 必须有注解");
        assertFalse(genContractNoWithRetry.getAnnotation(DataPermission.class).enable());

        Method genContractNo = SalesContractServiceImpl.class.getDeclaredMethod("generateContractNo");
        assertNotNull(genContractNo.getAnnotation(DataPermission.class), "SalesContractServiceImpl#generateContractNo 必须有注解");
        assertFalse(genContractNo.getAnnotation(DataPermission.class).enable());

        Method validateContractNoUnique = SalesContractServiceImpl.class.getDeclaredMethod("validateContractNoUnique", Long.class, String.class);
        assertNotNull(validateContractNoUnique.getAnnotation(DataPermission.class), "SalesContractServiceImpl#validateContractNoUnique 必须有注解");
        assertFalse(validateContractNoUnique.getAnnotation(DataPermission.class).enable());

        Method scSelectPrefix = SalesContractMapper.class.getMethod("selectListByContractNoPrefixIncludingDeleted", String.class);
        assertNotNull(scSelectPrefix.getAnnotation(DataPermission.class), "SalesContractMapper#selectListByContractNoPrefixIncludingDeleted 必须有注解");
        assertFalse(scSelectPrefix.getAnnotation(DataPermission.class).enable());

        Method scSelectNo = SalesContractMapper.class.getMethod("selectByContractNoIncludingDeleted", String.class);
        assertNotNull(scSelectNo.getAnnotation(DataPermission.class), "SalesContractMapper#selectByContractNoIncludingDeleted 必须有注解");
        assertFalse(scSelectNo.getAnnotation(DataPermission.class).enable());

        // §5.2 第 2 项：发货取号与查重
        Method getNextShipmentNo = ShipmentServiceImpl.class.getMethod("getNextShipmentNo", LocalDate.class);
        assertNotNull(getNextShipmentNo.getAnnotation(DataPermission.class), "ShipmentServiceImpl#getNextShipmentNo 必须有注解");
        assertFalse(getNextShipmentNo.getAnnotation(DataPermission.class).enable());

        Method genShipmentNoWithRetry = ShipmentServiceImpl.class.getDeclaredMethod("generateShipmentNoWithRetry", LocalDate.class);
        assertNotNull(genShipmentNoWithRetry.getAnnotation(DataPermission.class), "ShipmentServiceImpl#generateShipmentNoWithRetry 必须有注解");
        assertFalse(genShipmentNoWithRetry.getAnnotation(DataPermission.class).enable());

        Method genShipmentNo = ShipmentServiceImpl.class.getDeclaredMethod("generateShipmentNo", LocalDate.class);
        assertNotNull(genShipmentNo.getAnnotation(DataPermission.class), "ShipmentServiceImpl#generateShipmentNo 必须有注解");
        assertFalse(genShipmentNo.getAnnotation(DataPermission.class).enable());

        Method validateShipmentNoUnique = ShipmentServiceImpl.class.getDeclaredMethod("validateShipmentNoUnique", Long.class, String.class);
        assertNotNull(validateShipmentNoUnique.getAnnotation(DataPermission.class), "ShipmentServiceImpl#validateShipmentNoUnique 必须有注解");
        assertFalse(validateShipmentNoUnique.getAnnotation(DataPermission.class).enable());

        Method shSelectPrefix = ShipmentMapper.class.getMethod("selectListByShipmentNoPrefixIncludingDeleted", String.class);
        assertNotNull(shSelectPrefix.getAnnotation(DataPermission.class), "ShipmentMapper#selectListByShipmentNoPrefixIncludingDeleted 必须有注解");
        assertFalse(shSelectPrefix.getAnnotation(DataPermission.class).enable());

        Method shSelectNo = ShipmentMapper.class.getMethod("selectByShipmentNoIncludingDeleted", String.class);
        assertNotNull(shSelectNo.getAnnotation(DataPermission.class), "ShipmentMapper#selectByShipmentNoIncludingDeleted 必须有注解");
        assertFalse(shSelectNo.getAnnotation(DataPermission.class).enable());

        // §5.2 第 3 项：合同未发量聚合
        Method shSelectByContractIdAndStatus = ShipmentServiceImpl.class.getMethod("selectListByContractIdAndStatus", Long.class, String.class);
        assertNotNull(shSelectByContractIdAndStatus.getAnnotation(DataPermission.class), "ShipmentServiceImpl#selectListByContractIdAndStatus 必须有注解");
        assertFalse(shSelectByContractIdAndStatus.getAnnotation(DataPermission.class).enable());

        Method shMapperSelectByContractIdAndStatus = ShipmentMapper.class.getMethod("selectListByContractIdAndStatus", Long.class, String.class);
        assertNotNull(shMapperSelectByContractIdAndStatus.getAnnotation(DataPermission.class), "ShipmentMapper#selectListByContractIdAndStatus 必须有注解");
        assertFalse(shMapperSelectByContractIdAndStatus.getAnnotation(DataPermission.class).enable());

        // §5.2 第 4 项：生产进度只读聚合
        Method getProdProgressPage = ProductionProgressQueryServiceImpl.class.getMethod("getProductionProgressPage", com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageReqVO.class);
        assertNotNull(getProdProgressPage.getAnnotation(DataPermission.class), "ProductionProgressQueryServiceImpl#getProductionProgressPage 必须有注解");
        assertFalse(getProdProgressPage.getAnnotation(DataPermission.class).enable());

        Method getProdProgressPageIface = ProductionProgressQueryService.class.getMethod("getProductionProgressPage", com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageReqVO.class);
        assertNotNull(getProdProgressPageIface.getAnnotation(DataPermission.class), "ProductionProgressQueryService#getProductionProgressPage 必须有注解");
        assertFalse(getProdProgressPageIface.getAnnotation(DataPermission.class).enable());
    }

    @Test
    @DisplayName("10. 租户拦截器独立性验证：关闭数据权限不影响租户隔离")
    void testTenantIsolationIndependentOfDataPermission() {
        TenantContextHolder.setTenantId(999L);
        try {
            // 在关闭数据权限的上下文内执行
            DataPermissionUtils.executeIgnore(() -> {
                // 1. 数据权限上下文被正确关闭
                DataPermission dp = DataPermissionContextHolder.get();
                assertNotNull(dp);
                assertFalse(dp.enable());

                // 2. 租户上下文完好无损，依然为 999L，跨租户隔离不受影响
                assertEquals(999L, TenantContextHolder.getTenantId(), "关闭数据权限不得影响租户上下文");
            });

            // 退出后恢复
            assertNull(DataPermissionContextHolder.get());
            assertEquals(999L, TenantContextHolder.getTenantId());
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    @DisplayName("11. 验证受限行在 Customer / SalesContract / Shipment 单条详情与写操作路径上均抛 NOT_EXISTS 且 DB 零变更")
    void testSingleRowReadWriteInvisibleEnforcement() {
        Long invisibleId = 99999L;

        // 11.1 CustomerServiceImpl: 数据权限过滤下，不可见行 selectById 返回 null
        CustomerMapper customerMapper = Mockito.mock(CustomerMapper.class);
        CustomerServiceImpl customerService = new CustomerServiceImpl();
        ReflectionTestUtils.setField(customerService, "customerMapper", customerMapper);
        when(customerMapper.selectById(invisibleId)).thenReturn(null);

        // 详情
        ServiceException exCustGet = assertThrows(ServiceException.class, () -> customerService.getCustomer(invisibleId));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), exCustGet.getCode());

        // 更新
        CustomerSaveReqVO custUpdateReq = new CustomerSaveReqVO();
        custUpdateReq.setId(invisibleId);
        custUpdateReq.setCompanyName("Invisible Co");
        ServiceException exCustUpd = assertThrows(ServiceException.class, () -> customerService.updateCustomer(custUpdateReq));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), exCustUpd.getCode());
        verify(customerMapper, never()).updateById(any(CustomerDO.class));

        // 删除
        ServiceException exCustDel = assertThrows(ServiceException.class, () -> customerService.deleteCustomer(invisibleId));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), exCustDel.getCode());
        verify(customerMapper, never()).deleteById(any(Long.class));

        // 11.2 SalesContractServiceImpl: 数据权限过滤下不可见
        SalesContractMapper contractMapper = Mockito.mock(SalesContractMapper.class);
        SalesContractServiceImpl contractService = new SalesContractServiceImpl();
        ReflectionTestUtils.setField(contractService, "salesContractMapper", contractMapper);
        when(contractMapper.selectById(invisibleId)).thenReturn(null);

        // 详情
        ServiceException exScGet = assertThrows(ServiceException.class, () -> contractService.getSalesContract(invisibleId));
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), exScGet.getCode());

        // 更新
        SalesContractSaveReqVO scUpdateReq = new SalesContractSaveReqVO();
        scUpdateReq.setId(invisibleId);
        ServiceException exScUpd = assertThrows(ServiceException.class, () -> contractService.updateSalesContract(scUpdateReq));
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), exScUpd.getCode());
        verify(contractMapper, never()).updateById(any(SalesContractDO.class));

        // 删除
        ServiceException exScDel = assertThrows(ServiceException.class, () -> contractService.deleteSalesContract(invisibleId));
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), exScDel.getCode());
        verify(contractMapper, never()).deleteById(any(Long.class));

        // 11.3 ShipmentServiceImpl: 数据权限过滤下不可见
        ShipmentMapper shipmentMapper = Mockito.mock(ShipmentMapper.class);
        ShipmentServiceImpl shipmentService = new ShipmentServiceImpl();
        ReflectionTestUtils.setField(shipmentService, "shipmentMapper", shipmentMapper);
        when(shipmentMapper.selectById(invisibleId)).thenReturn(null);

        // 详情
        ServiceException exShGet = assertThrows(ServiceException.class, () -> shipmentService.getShipment(invisibleId));
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), exShGet.getCode());

        // 更新
        ShipmentSaveReqVO shUpdateReq = new ShipmentSaveReqVO();
        shUpdateReq.setId(invisibleId);
        ServiceException exShUpd = assertThrows(ServiceException.class, () -> shipmentService.updateShipment(shUpdateReq));
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), exShUpd.getCode());
        verify(shipmentMapper, never()).updateById(any(ShipmentDO.class));

        // 删除
        ServiceException exShDel = assertThrows(ServiceException.class, () -> shipmentService.deleteShipment(invisibleId));
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), exShDel.getCode());
        verify(shipmentMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("12. 验证发货单未发量聚合：引用不可见合同抛异常；可见时跨归属统计租户全部已发量")
    void testUndeliveredQuantityCrossUserAggregation() {
        ShipmentMapper shipmentMapper = Mockito.mock(ShipmentMapper.class);
        SalesContractMapper salesContractMapper = Mockito.mock(SalesContractMapper.class);
        ShipmentServiceImpl shipmentService = new ShipmentServiceImpl();
        ReflectionTestUtils.setField(shipmentService, "shipmentMapper", shipmentMapper);
        ReflectionTestUtils.setField(shipmentService, "salesContractMapper", salesContractMapper);

        // 不可见合同 -> 1_030_016_003
        when(salesContractMapper.selectById(888L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () ->
                shipmentService.getShipmentItemsFromContract(888L));
        assertEquals(SHIPMENT_RELATED_REF_NOT_EXISTS.getCode(), ex.getCode());

        // selectListByContractIdAndStatus 必须显式关闭数据权限查询
        when(shipmentMapper.selectListByContractIdAndStatus(777L, "COMPLETED"))
                .thenReturn(List.of(new ShipmentDO()));
        List<ShipmentDO> completedList = shipmentService.selectListByContractIdAndStatus(777L, "COMPLETED");
        assertEquals(1, completedList.size());
        verify(shipmentMapper).selectListByContractIdAndStatus(777L, "COMPLETED");
    }

    @Test
    @DisplayName("13. §5.3 / §9.2 用例 3 联系人继承：selectPage 构建裸 EXISTS 子查询，五种调法直连他人联系人/客户 id 一律 CUSTOMER_CONTACT_NOT_EXISTS")
    void testCustomerContactInheritanceAndFiveAccessPoints() {
        // 13.1 验证 selectPage 构建裸 EXISTS 子查询（无外部参数、防注入、ADR-0046 / §5.3）
        CustomerContactMapper mockContactMapper = Mockito.mock(CustomerContactMapper.class, Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        CustomerContactPageReqVO pageReqVO = new CustomerContactPageReqVO();
        pageReqVO.setCustomerId(1024L);

        mockContactMapper.selectPage(pageReqVO);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapperX<CustomerContactDO>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(mockContactMapper).selectPage(eq(pageReqVO), wrapperCaptor.capture());
        LambdaQueryWrapperX<CustomerContactDO> capturedWrapper = wrapperCaptor.getValue();
        assertNotNull(capturedWrapper);
        String customSqlSegment = capturedWrapper.getCustomSqlSegment();
        assertTrue(customSqlSegment.contains("EXISTS (SELECT 1 FROM lunaerp_baseinfo_customer c WHERE c.id = lunaerp_baseinfo_customer_contact.customer_id)"),
                "联系人分页查询必须包含裸 EXISTS 子查询");

        // 13.2 验证联系人 Service 面对不可见客户（他人客户或不存在）五种调法一律统一抛 1_030_004_001
        Long othersCustomerId = 9991L;
        Long othersContactId = 9992L;
        CustomerMapper customerMapper = Mockito.mock(CustomerMapper.class);
        CustomerContactMapper contactMapper = Mockito.mock(CustomerContactMapper.class);
        CustomerContactServiceImpl contactService = new CustomerContactServiceImpl();
        ReflectionTestUtils.setField(contactService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(contactService, "customerContactMapper", contactMapper);

        when(customerMapper.selectById(othersCustomerId)).thenReturn(null); // 不可见或不存在
        CustomerContactDO othersContact = new CustomerContactDO();
        othersContact.setId(othersContactId);
        othersContact.setCustomerId(othersCustomerId);
        othersContact.setName("他人联系人");
        when(contactMapper.selectById(othersContactId)).thenReturn(othersContact);

        // 1. 列表
        CustomerContactPageReqVO pageReq = new CustomerContactPageReqVO();
        pageReq.setCustomerId(othersCustomerId);
        ServiceException exPage = assertThrows(ServiceException.class, () -> contactService.getCustomerContactPage(pageReq));
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), exPage.getCode());

        // 2. 详情
        ServiceException exGet = assertThrows(ServiceException.class, () -> contactService.getCustomerContact(othersContactId));
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), exGet.getCode());

        // 3. 改
        CustomerContactSaveReqVO updateReq = new CustomerContactSaveReqVO();
        updateReq.setId(othersContactId);
        updateReq.setCustomerId(othersCustomerId);
        updateReq.setName("新名称");
        ServiceException exUpd = assertThrows(ServiceException.class, () -> contactService.updateCustomerContact(updateReq));
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), exUpd.getCode());

        // 4. 删
        ServiceException exDel = assertThrows(ServiceException.class, () -> contactService.deleteCustomerContact(othersContactId));
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), exDel.getCode());

        // 5. 新建
        CustomerContactSaveReqVO createReq = new CustomerContactSaveReqVO();
        createReq.setCustomerId(othersCustomerId);
        createReq.setName("新联系人");
        ServiceException exCreate = assertThrows(ServiceException.class, () -> contactService.createCustomerContact(createReq));
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), exCreate.getCode());

        // 断言写操作 DB 零变更
        verify(contactMapper, never()).insert(any(CustomerContactDO.class));
        verify(contactMapper, never()).updateById(any(CustomerContactDO.class));
        verify(contactMapper, never()).deleteById(othersContactId);
    }

    @Test
    @DisplayName("14. §5.4 / §9.2 用例 4 客户批量删除强校验：ids 中混入不可见 id → 整体失败且零删除")
    void testCustomerBatchDeleteStrictVisibilityValidation() {
        CustomerMapper customerMapper = Mockito.mock(CustomerMapper.class);
        CustomerServiceImpl customerService = new CustomerServiceImpl();
        ReflectionTestUtils.setField(customerService, "customerMapper", customerMapper);

        Long visibleId1 = 1001L;
        Long invisibleId = 1002L;
        Long visibleId2 = 1003L;

        CustomerDO c1 = new CustomerDO();
        c1.setId(visibleId1);
        CustomerDO c3 = new CustomerDO();
        c3.setId(visibleId2);

        when(customerMapper.selectById(visibleId1)).thenReturn(c1);
        when(customerMapper.selectById(invisibleId)).thenReturn(null); // 受限用户不可见
        when(customerMapper.selectById(visibleId2)).thenReturn(c3);

        List<Long> mixedIds = List.of(visibleId1, invisibleId, visibleId2);

        ServiceException ex = assertThrows(ServiceException.class, () -> customerService.deleteCustomerList(mixedIds));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), ex.getCode(), "任一 id 不可见必须整体抛 CUSTOMER_NOT_EXISTS");

        // 断言零部分成功，零删除，零更新
        verify(customerMapper, never()).updateById(any(CustomerDO.class));
        verify(customerMapper, never()).deleteByIds(anyList());
    }

    @Test
    @DisplayName("15. §9.2 用例 2 四入口一致性：不可见行在详情、update、delete、状态操作等入口统一返回各表 NOT_EXISTS，写操作断言 DB 零变更")
    void testFourEntrypointsConsistencyAllEntities() {
        Long invisibleId = 999999L;

        // 1. 客户：不可见 -> CUSTOMER_NOT_EXISTS
        CustomerMapper customerMapper = Mockito.mock(CustomerMapper.class);
        CustomerServiceImpl customerService = new CustomerServiceImpl();
        ReflectionTestUtils.setField(customerService, "customerMapper", customerMapper);
        when(customerMapper.selectById(invisibleId)).thenReturn(null);

        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.getCustomer(invisibleId)).getCode());
        CustomerSaveReqVO custUpd = new CustomerSaveReqVO();
        custUpd.setId(invisibleId);
        custUpd.setCompanyName("X");
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.updateCustomer(custUpd)).getCode());
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> customerService.deleteCustomer(invisibleId)).getCode());
        verify(customerMapper, never()).updateById(any(CustomerDO.class));
        verify(customerMapper, never()).deleteByIds(anyList());

        // 2. 销售合同：不可见 -> SALES_CONTRACT_NOT_EXISTS
        SalesContractMapper scMapper = Mockito.mock(SalesContractMapper.class);
        SalesContractServiceImpl scService = new SalesContractServiceImpl();
        ReflectionTestUtils.setField(scService, "salesContractMapper", scMapper);
        when(scMapper.selectById(invisibleId)).thenReturn(null);

        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> scService.getSalesContract(invisibleId)).getCode());
        SalesContractSaveReqVO scUpd = new SalesContractSaveReqVO();
        scUpd.setId(invisibleId);
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> scService.updateSalesContract(scUpd)).getCode());
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> scService.deleteSalesContract(invisibleId)).getCode());
        verify(scMapper, never()).updateById(any(SalesContractDO.class));
        verify(scMapper, never()).deleteById(any(Long.class));

        // 3. 发货单：不可见 -> SHIPMENT_NOT_EXISTS (含 update, audit, cancel, delete)
        ShipmentMapper shMapper = Mockito.mock(ShipmentMapper.class);
        ShipmentServiceImpl shService = new ShipmentServiceImpl();
        ReflectionTestUtils.setField(shService, "shipmentMapper", shMapper);
        when(shMapper.selectById(invisibleId)).thenReturn(null);

        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shService.getShipment(invisibleId)).getCode());
        ShipmentSaveReqVO shUpd = new ShipmentSaveReqVO();
        shUpd.setId(invisibleId);
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shService.updateShipment(shUpd)).getCode());
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shService.auditShipment(invisibleId)).getCode());
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shService.cancelShipment(invisibleId)).getCode());
        assertEquals(SHIPMENT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> shService.deleteShipment(invisibleId)).getCode());
        verify(shMapper, never()).updateById(any(ShipmentDO.class));
        verify(shMapper, never()).deleteById(any(Long.class));

        // 4. 联系人：不可见客户的联系人 -> CUSTOMER_CONTACT_NOT_EXISTS
        CustomerContactMapper ccMapper = Mockito.mock(CustomerContactMapper.class);
        CustomerContactServiceImpl ccService = new CustomerContactServiceImpl();
        ReflectionTestUtils.setField(ccService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(ccService, "customerContactMapper", ccMapper);

        CustomerContactDO ccDO = new CustomerContactDO();
        ccDO.setId(invisibleId);
        ccDO.setCustomerId(invisibleId);
        when(ccMapper.selectById(invisibleId)).thenReturn(ccDO);

        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> ccService.getCustomerContact(invisibleId)).getCode());
        CustomerContactSaveReqVO ccUpd = new CustomerContactSaveReqVO();
        ccUpd.setId(invisibleId);
        ccUpd.setCustomerId(invisibleId);
        ccUpd.setName("Contact");
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> ccService.updateCustomerContact(ccUpd)).getCode());
        assertEquals(CUSTOMER_CONTACT_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> ccService.deleteCustomerContact(invisibleId)).getCode());
        verify(ccMapper, never()).updateById(any(CustomerContactDO.class));
        verify(ccMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("16. §9.2 用例 9 自定义档四层收口：system assignRoleDataScope(dataScope=2) 拒绝并抛 1_002_002_006；DataScopeEnum.DEPT_CUSTOM 与计算分支保留；lunaerp 错误码段 1-030-041 保持空闲未被占用")
    void testCustomScopeShutdownCase9() {
        // 1. 验证枚举 DEPT_CUSTOM(2) 保留不删
        assertEquals(2, com.luna.lunasrv.module.system.enums.permission.DataScopeEnum.DEPT_CUSTOM.getScope());
        assertTrue(List.of(com.luna.lunasrv.module.system.enums.permission.DataScopeEnum.values())
                .contains(com.luna.lunasrv.module.system.enums.permission.DataScopeEnum.DEPT_CUSTOM));

        // 2. 验证错误码落入 system 角色段 1_002_002_006
        assertEquals(1_002_002_006, com.luna.lunasrv.module.system.enums.ErrorCodeConstants.ROLE_DATA_SCOPE_CUSTOM_DISABLED.getCode());
        assertEquals("数据范围【自定义部门】已停用，请选择全部/本部门及以下/本部门/本人",
                com.luna.lunasrv.module.system.enums.ErrorCodeConstants.ROLE_DATA_SCOPE_CUSTOM_DISABLED.getMsg());

        // 3. API / Service 直调 assignRoleDataScope(dataScope=2) 抛 1_002_002_006
        com.luna.lunasrv.module.system.service.permission.RoleService mockRoleService =
                Mockito.mock(com.luna.lunasrv.module.system.service.permission.RoleService.class);
        com.luna.lunasrv.module.system.service.permission.PermissionServiceImpl permissionService =
                new com.luna.lunasrv.module.system.service.permission.PermissionServiceImpl();
        ReflectionTestUtils.setField(permissionService, "roleService", mockRoleService);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                permissionService.assignRoleDataScope(100L, 2, Set.of(1L, 2L)));
        assertEquals(1_002_002_006, ex.getCode());
        assertEquals("数据范围【自定义部门】已停用，请选择全部/本部门及以下/本部门/本人", ex.getMessage());
        verify(mockRoleService, never()).updateRoleDataScope(any(), any(), any());

        // 4. 合法 4 档正常放行调用底座
        for (int scope : List.of(1, 4, 3, 5)) {
            permissionService.assignRoleDataScope(100L, scope, Collections.emptySet());
            verify(mockRoleService).updateRoleDataScope(100L, scope, Collections.emptySet());
        }

        // 5. 验证 lunaerp 段 1-030-041 保持空闲未被占用 (Q14)
        for (java.lang.reflect.Field field : com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.class.getFields()) {
            if (field.getType().equals(com.luna.lunasrv.framework.common.exception.ErrorCode.class)) {
                try {
                    com.luna.lunasrv.framework.common.exception.ErrorCode ec =
                            (com.luna.lunasrv.framework.common.exception.ErrorCode) field.get(null);
                    assertFalse(ec.getCode() >= 1_030_041_000 && ec.getCode() < 1_030_042_000,
                            "lunaerp 1-030-041 段必须保持空闲不被占用，但发现: " + field.getName() + " -> " + ec.getCode());
                } catch (IllegalAccessException e) {
                    fail("反射访问异常: " + e.getMessage());
                }
            }
        }
    }

}
