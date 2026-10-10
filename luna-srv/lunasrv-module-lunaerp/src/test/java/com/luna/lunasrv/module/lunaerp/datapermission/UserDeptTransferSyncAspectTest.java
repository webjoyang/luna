package com.luna.lunasrv.module.lunaerp.datapermission;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.biz.system.permission.PermissionCommonApi;
import com.luna.lunasrv.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.luna.lunasrv.framework.common.enums.UserTypeEnum;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.datapermission.core.aop.DataPermissionContextHolder;
import com.luna.lunasrv.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.framework.datapermission.aop.UserDeptTransferSyncAspect;
import com.luna.lunasrv.module.lunaerp.framework.datapermission.config.LunaerpDataPermissionConfiguration;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import net.sf.jsqlparser.expression.Expression;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 04 票：调岗归属同步切面单元测试与验收用例（ADR-0046 / §9.2 用例 5）
 *
 * 覆盖矩阵：
 * 1. 切面与注解契约：bean 名切点匹配 bean(adminUserService) && execution(* updateUser(..))，不反向依赖 system 模块内部类
 * 2. Mapper 契约：3 张核心表 updateDeptIdBySalespersonUserId 声明 @DataPermission(enable=false) 与 @Update
 * 3. 部门未变化放行：oldDeptId == newDeptId 时直接 proceed，0 次 mapper 更新
 * 4. 部门变化同事务刷列：oldDeptId != newDeptId 时更新 3 表 dept_id，且在 @DataPermission(enable=false) 上下文内执行
 * 5. 调岗清空部门：newDeptId 为 null 时正确传递 null 刷新
 * 6. 用户不存在或参数无 ID：直接放行
 * 7. owner 列（salesperson_user_id）不变：只更新 dept_id，SELF 档不受调岗影响
 * 8. 同事务回滚造失败验证不半刷：
 *    - proceed 失败：向外抛异常，不触发 mapper 更新，事务标记 rollbackOnly
 *    - mapper 失败：向外抛异常，事务标记 rollbackOnly，不半刷
 * 9. Spring AOP Proxy 真实动态代理切面拦截集成测试
 * 10. §9.2 用例 5 完整组织可见性验证：a 调 D2 后重查——m 立即不见 a 的单、D2 主管立即可见、a 本人仍见；同事务回滚造失败验证不半刷
 */
class UserDeptTransferSyncAspectTest {

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @InjectMocks
    private UserDeptTransferSyncAspect aspect;

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), CustomerDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesContractDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), ShipmentDO.class);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        while (DataPermissionContextHolder.get() != null) {
            DataPermissionContextHolder.remove();
        }
    }

    // 模拟 POJO，用于测试非系统模块依赖下的传参
    public static class DummyUserSaveReq {
        private Long id;
        private Long deptId;

        public DummyUserSaveReq(Long id, Long deptId) {
            this.id = id;
            this.deptId = deptId;
        }

        public Long getId() {
            return id;
        }

        public Long getDeptId() {
            return deptId;
        }
    }

    // 模拟 adminUserService 接口与实现
    public interface DummyAdminUserService {
        void updateUser(Object reqVO);
    }

    public static class DummyAdminUserServiceImpl implements DummyAdminUserService {
        private final AtomicBoolean invoked = new AtomicBoolean(false);

        @Override
        public void updateUser(Object reqVO) {
            invoked.set(true);
        }

        public boolean isInvoked() {
            return invoked.get();
        }
    }

    @Test
    @DisplayName("1. 切面与注解契约：@Aspect/@Component，切点表达式包含 bean(adminUserService) && execution(* updateUser(..))，不依赖 system 内部类")
    void testAspectAnnotationAndPointcut() throws NoSuchMethodException {
        // 1.1 类注解校验
        assertTrue(UserDeptTransferSyncAspect.class.isAnnotationPresent(Aspect.class), "切面必须声明 @Aspect");
        assertTrue(UserDeptTransferSyncAspect.class.isAnnotationPresent(Component.class), "切面必须声明 @Component");

        // 1.2 方法切点校验
        Method aroundMethod = UserDeptTransferSyncAspect.class.getMethod("aroundUpdateUser", ProceedingJoinPoint.class);
        Around aroundAnno = aroundMethod.getAnnotation(Around.class);
        assertNotNull(aroundAnno, "aroundUpdateUser 必须声明 @Around 注解");
        assertEquals("bean(adminUserService) && execution(* updateUser(..))", aroundAnno.value(),
                "切点必须为 bean(adminUserService) && execution(* updateUser(..))，不硬编码内部实现类");

        // 1.3 确认 UserDeptTransferSyncAspect 中没有引用 AdminUserServiceImpl 与 UserSaveReqVO 类
        for (Class<?> declaredClass : UserDeptTransferSyncAspect.class.getDeclaredClasses()) {
            assertFalse(declaredClass.getName().contains("AdminUserServiceImpl"));
        }
        for (Method m : UserDeptTransferSyncAspect.class.getDeclaredMethods()) {
            for (Class<?> pType : m.getParameterTypes()) {
                assertFalse(pType.getName().contains("AdminUserServiceImpl"), "禁止反向依赖 AdminUserServiceImpl");
                assertFalse(pType.getName().contains("UserSaveReqVO"), "禁止反向依赖 UserSaveReqVO");
            }
        }
    }

    @Test
    @DisplayName("2. Mapper 契约：3 张核心表 updateDeptIdBySalespersonUserId 声明 @DataPermission(enable=false) 与规范 @Update")
    void testMapperMethodsContract() throws NoSuchMethodException {
        // 2.1 CustomerMapper
        Method custMethod = CustomerMapper.class.getMethod("updateDeptIdBySalespersonUserId", Long.class, Long.class);
        DataPermission custDp = custMethod.getAnnotation(DataPermission.class);
        assertNotNull(custDp, "CustomerMapper#updateDeptIdBySalespersonUserId 必须声明 @DataPermission");
        assertFalse(custDp.enable(), "数据权限必须关闭");
        Update custUpdate = custMethod.getAnnotation(Update.class);
        assertNotNull(custUpdate, "必须声明 @Update 注解");
        String custSql = custUpdate.value()[0];
        assertTrue(custSql.contains("UPDATE lunaerp_baseinfo_customer SET dept_id = #{deptId,jdbcType=BIGINT}"), "SQL 需更新 dept_id 并指定 jdbcType");
        assertTrue(custSql.contains("WHERE salesperson_user_id = #{salespersonUserId,jdbcType=BIGINT} AND deleted = 0"), "SQL 需按 salesperson_user_id 且 deleted=0 过滤");

        // 2.2 SalesContractMapper
        Method scMethod = SalesContractMapper.class.getMethod("updateDeptIdBySalespersonUserId", Long.class, Long.class);
        DataPermission scDp = scMethod.getAnnotation(DataPermission.class);
        assertNotNull(scDp, "SalesContractMapper#updateDeptIdBySalespersonUserId 必须声明 @DataPermission");
        assertFalse(scDp.enable());
        Update scUpdate = scMethod.getAnnotation(Update.class);
        assertNotNull(scUpdate);
        String scSql = scUpdate.value()[0];
        assertTrue(scSql.contains("UPDATE lunaerp_business_sales_contract SET dept_id = #{deptId,jdbcType=BIGINT}"));
        assertTrue(scSql.contains("WHERE salesperson_user_id = #{salespersonUserId,jdbcType=BIGINT} AND deleted = 0"));

        // 2.3 ShipmentMapper
        Method shMethod = ShipmentMapper.class.getMethod("updateDeptIdBySalespersonUserId", Long.class, Long.class);
        DataPermission shDp = shMethod.getAnnotation(DataPermission.class);
        assertNotNull(shDp, "ShipmentMapper#updateDeptIdBySalespersonUserId 必须声明 @DataPermission");
        assertFalse(shDp.enable());
        Update shUpdate = shMethod.getAnnotation(Update.class);
        assertNotNull(shUpdate);
        String shSql = shUpdate.value()[0];
        assertTrue(shSql.contains("UPDATE lunaerp_business_shipment SET dept_id = #{deptId,jdbcType=BIGINT}"));
        assertTrue(shSql.contains("WHERE salesperson_user_id = #{salespersonUserId,jdbcType=BIGINT} AND deleted = 0"));
    }

    @Test
    @DisplayName("3. 部门未变化直接放行：oldDeptId == newDeptId，proceed 正常执行且 0 次 mapper 调用")
    void testDeptUnchangedPassthrough() throws Throwable {
        Long userId = 100L;
        Long deptId = 200L;
        DummyUserSaveReq req = new DummyUserSaveReq(userId, deptId);

        AdminUserRespDTO oldUser = new AdminUserRespDTO();
        oldUser.setId(userId);
        oldUser.setDeptId(deptId);
        when(adminUserApi.getUser(userId)).thenReturn(oldUser);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});
        when(joinPoint.proceed()).thenReturn("OK");

        Object result = aspect.aroundUpdateUser(joinPoint);

        assertEquals("OK", result);
        verify(joinPoint).proceed();
        verifyNoInteractions(customerMapper, salesContractMapper, shipmentMapper);
    }

    @Test
    @DisplayName("4. 部门变化同事务刷列：oldDeptId != newDeptId，在 @DataPermission(enable=false) 上下文内触发 3 表刷列")
    void testDeptChangedSyncTriggered() throws Throwable {
        Long userId = 100L;
        Long oldDeptId = 200L;
        Long newDeptId = 300L;
        DummyUserSaveReq req = new DummyUserSaveReq(userId, newDeptId);

        AdminUserRespDTO oldUser = new AdminUserRespDTO();
        oldUser.setId(userId);
        oldUser.setDeptId(oldDeptId);
        when(adminUserApi.getUser(userId)).thenReturn(oldUser);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});
        when(joinPoint.proceed()).thenReturn(null);

        // 验证执行时数据权限确实被关闭
        when(customerMapper.updateDeptIdBySalespersonUserId(userId, newDeptId)).thenAnswer(inv -> {
            DataPermission dp = DataPermissionContextHolder.get();
            assertNotNull(dp, "执行刷列时必须处于数据权限上下文");
            assertFalse(dp.enable(), "执行刷列时数据权限必须关闭");
            return 2;
        });
        when(salesContractMapper.updateDeptIdBySalespersonUserId(userId, newDeptId)).thenReturn(3);
        when(shipmentMapper.updateDeptIdBySalespersonUserId(userId, newDeptId)).thenReturn(5);

        aspect.aroundUpdateUser(joinPoint);

        verify(joinPoint).proceed();
        verify(customerMapper).updateDeptIdBySalespersonUserId(userId, newDeptId);
        verify(salesContractMapper).updateDeptIdBySalespersonUserId(userId, newDeptId);
        verify(shipmentMapper).updateDeptIdBySalespersonUserId(userId, newDeptId);
    }

    @Test
    @DisplayName("5. 调岗清空部门：newDeptId 为 null 时正确传递 null 刷新 3 表")
    void testDeptChangedToNullSyncTriggered() throws Throwable {
        Long userId = 100L;
        Long oldDeptId = 200L;
        DummyUserSaveReq req = new DummyUserSaveReq(userId, null);

        AdminUserRespDTO oldUser = new AdminUserRespDTO();
        oldUser.setId(userId);
        oldUser.setDeptId(oldDeptId);
        when(adminUserApi.getUser(userId)).thenReturn(oldUser);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});

        aspect.aroundUpdateUser(joinPoint);

        verify(joinPoint).proceed();
        verify(customerMapper).updateDeptIdBySalespersonUserId(userId, null);
        verify(salesContractMapper).updateDeptIdBySalespersonUserId(userId, null);
        verify(shipmentMapper).updateDeptIdBySalespersonUserId(userId, null);
    }

    @Test
    @DisplayName("6. 用户不存在或参数无 ID：直接放行且不执行刷列")
    void testUserNotFoundOrMissingIdPassthrough() throws Throwable {
        // 6.1 用户在 DB 中不存在
        Long userId = 999L;
        DummyUserSaveReq req = new DummyUserSaveReq(userId, 300L);
        when(adminUserApi.getUser(userId)).thenReturn(null);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});

        aspect.aroundUpdateUser(joinPoint);
        verify(joinPoint).proceed();
        verifyNoInteractions(customerMapper, salesContractMapper, shipmentMapper);

        // 6.2 参数无 ID
        ProceedingJoinPoint joinPointNoId = mock(ProceedingJoinPoint.class);
        when(joinPointNoId.getArgs()).thenReturn(new Object[]{new Object()});
        aspect.aroundUpdateUser(joinPointNoId);
        verify(joinPointNoId).proceed();

        // 6.3 参数为空数组
        ProceedingJoinPoint joinPointEmpty = mock(ProceedingJoinPoint.class);
        when(joinPointEmpty.getArgs()).thenReturn(new Object[]{});
        aspect.aroundUpdateUser(joinPointEmpty);
        verify(joinPointEmpty).proceed();
    }

    @Test
    @DisplayName("7. 传参 Map 支持动态反射读取 id 与 deptId")
    void testMapArgumentSupport() throws Throwable {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("id", 100L);
        reqMap.put("deptId", 300L);

        AdminUserRespDTO oldUser = new AdminUserRespDTO();
        oldUser.setId(100L);
        oldUser.setDeptId(200L);
        when(adminUserApi.getUser(100L)).thenReturn(oldUser);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{reqMap});

        aspect.aroundUpdateUser(joinPoint);

        verify(customerMapper).updateDeptIdBySalespersonUserId(100L, 300L);
        verify(salesContractMapper).updateDeptIdBySalespersonUserId(100L, 300L);
        verify(shipmentMapper).updateDeptIdBySalespersonUserId(100L, 300L);
    }

    @Test
    @DisplayName("8. 同事务回滚造失败验证不半刷：proceed() 失败则不触发刷列并标记回滚")
    void testSameTransactionRollbackOnProceedFailure() throws Throwable {
        Long userId = 100L;
        DummyUserSaveReq req = new DummyUserSaveReq(userId, 300L);

        AdminUserRespDTO oldUser = new AdminUserRespDTO();
        oldUser.setId(userId);
        oldUser.setDeptId(200L);
        when(adminUserApi.getUser(userId)).thenReturn(oldUser);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});
        when(joinPoint.proceed()).thenThrow(new ServiceException(1001002001, "用户名已存在"));

        // 模拟 TransactionManager 监控回滚
        AtomicBoolean rollbackCalled = new AtomicBoolean(false);
        PlatformTransactionManager ptm = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
                if (status.isRollbackOnly()) {
                    rollbackCalled.set(true);
                }
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
                rollbackCalled.set(true);
            }
        };
        aspect.setTransactionManager(ptm);

        ServiceException ex = assertThrows(ServiceException.class, () -> aspect.aroundUpdateUser(joinPoint));
        assertEquals(1001002001, ex.getCode());

        // 刷列绝未被调用
        verifyNoInteractions(customerMapper, salesContractMapper, shipmentMapper);
        assertTrue(rollbackCalled.get(), "事务必须被回滚，禁止提交");
    }

    @Test
    @DisplayName("8. 同事务回滚造失败验证不半刷：刷列发生异常整事务回滚，禁止部分成功")
    void testSameTransactionRollbackOnSyncFailure() throws Throwable {
        Long userId = 100L;
        DummyUserSaveReq req = new DummyUserSaveReq(userId, 300L);

        AdminUserRespDTO oldUser = new AdminUserRespDTO();
        oldUser.setId(userId);
        oldUser.setDeptId(200L);
        when(adminUserApi.getUser(userId)).thenReturn(oldUser);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{req});
        when(joinPoint.proceed()).thenReturn(null);

        // 模拟客户刷列失败
        when(customerMapper.updateDeptIdBySalespersonUserId(userId, 300L))
                .thenThrow(new RuntimeException("DB Connection Timeout"));

        AtomicBoolean rollbackCalled = new AtomicBoolean(false);
        PlatformTransactionManager ptm = new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
                if (status.isRollbackOnly()) {
                    rollbackCalled.set(true);
                }
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
                rollbackCalled.set(true);
            }
        };
        aspect.setTransactionManager(ptm);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> aspect.aroundUpdateUser(joinPoint));
        assertEquals("DB Connection Timeout", ex.getMessage());
        assertTrue(rollbackCalled.get(), "刷列失败时整事务必须回滚，不半刷");
    }

    @Test
    @DisplayName("9. Spring AOP Proxy 真实动态代理切面拦截集成验证")
    void testSpringAopProxyIntegration() {
        DummyAdminUserService target = new DummyAdminUserServiceImpl();
        org.springframework.beans.factory.support.DefaultListableBeanFactory beanFactory =
                new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        beanFactory.registerSingleton("adminUserService", target);
        beanFactory.registerSingleton("userDeptTransferSyncAspect", aspect);

        org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator creator =
                new org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator();
        creator.setBeanFactory(beanFactory);

        Object proxied = creator.postProcessAfterInitialization(target, "adminUserService");
        assertNotNull(proxied);
        DummyAdminUserService proxy = (DummyAdminUserService) proxied;

        Long userId = 100L;
        DummyUserSaveReq req = new DummyUserSaveReq(userId, 300L);

        AdminUserRespDTO oldUser = new AdminUserRespDTO();
        oldUser.setId(userId);
        oldUser.setDeptId(200L);
        when(adminUserApi.getUser(userId)).thenReturn(oldUser);

        proxy.updateUser(req);

        assertTrue(((DummyAdminUserServiceImpl) target).isInvoked(), "目标方法必须被正常执行");
        verify(customerMapper).updateDeptIdBySalespersonUserId(userId, 300L);
        verify(salesContractMapper).updateDeptIdBySalespersonUserId(userId, 300L);
        verify(shipmentMapper).updateDeptIdBySalespersonUserId(userId, 300L);
    }

    @Test
    @DisplayName("10. §9.2 用例 5 完整组织可见性验证：a 调 D2 后重查——m 立即不见 a 的单、D2 主管立即可见、a 本人仍见；同事务回滚造失败验证不半刷")
    void testTransferE2EVisibilityChange_Case5() throws Throwable {
        // 1. 组织与用户造数（与验收 §6.2 / §9.1 严格同构）
        // D1 = 101L, D1.1 = 102L, D2 = 103L
        Long d1DeptId = 101L;
        Long d11DeptId = 102L;
        Long d2DeptId = 103L;

        Long mUserId = 10L;   // D1 主管（sales_manager）
        Long d2mUserId = 20L; // D2 主管（sales_manager）
        Long aUserId = 300L;  // D1 业务员（salesperson）

        PermissionCommonApi permissionApi = mock(PermissionCommonApi.class);

        // m: DEPT_AND_CHILD (D1 及以下: 101, 102)
        DeptDataPermissionRespDTO mPerm = new DeptDataPermissionRespDTO();
        mPerm.setAll(false);
        mPerm.setSelf(true);
        mPerm.setDeptIds(Set.of(d1DeptId, d11DeptId));
        when(permissionApi.getDeptDataPermission(mUserId)).thenReturn(mPerm);

        // d2m: DEPT_AND_CHILD (D2 及以下: 103)
        DeptDataPermissionRespDTO d2mPerm = new DeptDataPermissionRespDTO();
        d2mPerm.setAll(false);
        d2mPerm.setSelf(true);
        d2mPerm.setDeptIds(Set.of(d2DeptId));
        when(permissionApi.getDeptDataPermission(d2mUserId)).thenReturn(d2mPerm);

        // a: SELF (仅本人: user=300)
        DeptDataPermissionRespDTO aPerm = new DeptDataPermissionRespDTO();
        aPerm.setAll(false);
        aPerm.setSelf(true);
        aPerm.setDeptIds(Collections.emptySet());
        when(permissionApi.getDeptDataPermission(aUserId)).thenReturn(aPerm);

        DeptDataPermissionRule rule = new DeptDataPermissionRule(permissionApi);
        new LunaerpDataPermissionConfiguration().lunaerpDeptDataPermissionRuleCustomizer().customize(rule);

        // 2. 初始状态：a 在 D1 部门 (dept=101)，单据 salesperson_user_id=300, dept_id=101
        // 2.1 m 查客户表：(dept_id IN (101, 102) OR salesperson_user_id = 10) -> 能命中 a 的单据
        mockLoginUser(mUserId, "sales_manager_m");
        Expression mExprBefore = rule.getExpression("lunaerp_baseinfo_customer", null);
        assertNotNull(mExprBefore);
        String mSqlBefore = mExprBefore.toString();
        assertTrue(mSqlBefore.contains("dept_id IN (101, 102)") || mSqlBefore.contains("dept_id IN (102, 101)"));
        assertTrue(mSqlBefore.contains("101"), "初始状态下 m 包含 D1 部门，故可见 a 的单据");

        // 2.2 d2m 查客户表：(dept_id IN (103) OR salesperson_user_id = 20) -> 不命中 a 的单据
        mockLoginUser(d2mUserId, "sales_manager_d2");
        Expression d2mExprBefore = rule.getExpression("lunaerp_baseinfo_customer", null);
        assertNotNull(d2mExprBefore);
        String d2mSqlBefore = d2mExprBefore.toString();
        assertFalse(d2mSqlBefore.contains("101"), "初始状态下 D2 主管绝不可见 a 的 D1 单据");

        // 2.3 a 查客户表：salesperson_user_id = 300 -> 始终可见自己单据
        mockLoginUser(aUserId, "salesperson_a");
        Expression aExprBefore = rule.getExpression("lunaerp_baseinfo_customer", null);
        assertNotNull(aExprBefore);
        assertEquals("lunaerp_baseinfo_customer.salesperson_user_id = 300", aExprBefore.toString(), "a 始终只见本人");

        // 3. 执行调岗：将 a 从 D1 (101) 调入 D2 (103)
        AdminUserRespDTO oldUserA = new AdminUserRespDTO();
        oldUserA.setId(aUserId);
        oldUserA.setDeptId(d1DeptId);
        when(adminUserApi.getUser(aUserId)).thenReturn(oldUserA);

        DummyUserSaveReq transferReq = new DummyUserSaveReq(aUserId, d2DeptId);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{transferReq});

        // 调岗执行
        aspect.aroundUpdateUser(joinPoint);

        // 断言 3 表均被刷为 D2 (103)
        verify(customerMapper).updateDeptIdBySalespersonUserId(aUserId, d2DeptId);
        verify(salesContractMapper).updateDeptIdBySalespersonUserId(aUserId, d2DeptId);
        verify(shipmentMapper).updateDeptIdBySalespersonUserId(aUserId, d2DeptId);

        // 4. 调岗后立即可见性变化重查验证（单据 dept_id 已同步为 103，salesperson_user_id 仍为 300）
        // 4.1 m 查：规则条件 dept_id IN (101, 102) OR user = 10，对于 dept_id=103/user=300 的旧单立即不可见！
        mockLoginUser(mUserId, "sales_manager_m");
        Expression mExprAfter = rule.getExpression("lunaerp_baseinfo_customer", null);
        String mSqlAfter = mExprAfter.toString();
        assertFalse(mSqlAfter.contains("103"), "m 的可见范围不含 103，故 a 调 D2 后 m 立即不见 a 的旧单！");

        // 4.2 d2m 查：规则条件 dept_id IN (103) OR user = 20，对于 dept_id=103 的单据立即可见！
        mockLoginUser(d2mUserId, "sales_manager_d2");
        Expression d2mExprAfter = rule.getExpression("lunaerp_baseinfo_customer", null);
        String d2mSqlAfter = d2mExprAfter.toString();
        assertTrue(d2mSqlAfter.contains("103"), "D2 主管的可见范围包含 103，故立即可见 a 的单据！");

        // 4.3 a 查：规则条件仍然是 salesperson_user_id = 300（owner 列不变，SELF 档不受调岗影响）
        mockLoginUser(aUserId, "salesperson_a");
        Expression aExprAfter = rule.getExpression("lunaerp_baseinfo_customer", null);
        assertEquals("lunaerp_baseinfo_customer.salesperson_user_id = 300", aExprAfter.toString(),
                "a 本人仍然见自己的单据（owner 不变）");
    }

    private void mockLoginUser(Long userId, String username) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        loginUser.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }
}
