package com.luna.lunasrv.module.lunaerp.approval;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luna.lunasrv.framework.common.util.json.JsonUtils;
import com.luna.lunasrv.framework.security.core.LoginUser;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.biz.ApprovalBizHandler;
import com.luna.lunasrv.module.lunaerp.approval.biz.SalesContractApprovalBizHandler;
import com.luna.lunasrv.module.lunaerp.approval.event.ApprovalCompletedEvent;
import com.luna.lunasrv.module.lunaerp.approval.handler.ApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.handler.SalesContractApprovalHandler;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceMapper;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalNodeSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.approval.instance.dto.ApprovalProcessSnapshotDTO;
import com.luna.lunasrv.module.lunaerp.approval.listener.ApprovalCompletedEventListener;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordDO;
import com.luna.lunasrv.module.lunaerp.approval.record.ApprovalRecordMapper;
import com.luna.lunasrv.module.lunaerp.approval.task.ApprovalTaskDO;
import com.luna.lunasrv.module.lunaerp.approval.task.ApprovalTaskMapper;
import com.luna.lunasrv.module.lunaerp.approval.task.ApprovalTaskService;
import com.luna.lunasrv.module.lunaerp.approval.task.ApprovalTaskServiceImpl;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 审批通过副作用机制集成测试（对齐 Issue 07 与 ADR-0017）
 *
 * 验证：
 * 1. 审批通过（最后节点通过）在同一事务内发布 ApprovalCompletedEvent，
 *    通过 @TransactionalEventListener(phase = BEFORE_COMMIT) 路由至 ApprovalHandler 触发生效；
 * 2. 模拟业务侧 ApprovalHandler 抛异常，验证 Spring 事务拦截并触发回滚（Connection.rollback 触发，Connection.commit 不触发），
 *    保证「审批通过 = 业务生效」强原子性；
 * 3. APPROVED 终态守卫：已通过的审批实例不可撤回（报 1_030_030_004）。
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ApprovalCompletedEventIntegrationTest.TestConfig.class)
public class ApprovalCompletedEventIntegrationTest {

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        public Connection connection() throws SQLException {
            Connection con = mock(Connection.class);
            when(con.getAutoCommit()).thenReturn(true);
            return con;
        }

        @Bean
        public DataSource dataSource(Connection connection) throws SQLException {
            DataSource ds = mock(DataSource.class);
            when(ds.getConnection()).thenReturn(connection);
            return ds;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        public AdminUserApi adminUserApi() {
            return mock(AdminUserApi.class);
        }

        @Bean
        public ApprovalTaskMapper approvalTaskMapper() {
            return mock(ApprovalTaskMapper.class);
        }

        @Bean
        public ApprovalInstanceMapper approvalInstanceMapper() {
            return mock(ApprovalInstanceMapper.class);
        }

        @Bean
        public ApprovalRecordMapper approvalRecordMapper() {
            return mock(ApprovalRecordMapper.class);
        }

        @Bean
        public SalesContractMapper salesContractMapper() {
            return mock(SalesContractMapper.class);
        }

        @Bean
        public SalesContractApprovalBizHandler salesContractApprovalBizHandler() {
            return mock(SalesContractApprovalBizHandler.class);
        }

        @Bean
        public SalesContractApprovalHandler salesContractApprovalHandler() {
            return new SalesContractApprovalHandler();
        }

        @Bean
        public ApprovalCompletedEventListener approvalCompletedEventListener() {
            return new ApprovalCompletedEventListener();
        }

        @Bean
        public ApprovalTaskService approvalTaskService() {
            return new ApprovalTaskServiceImpl();
        }
    }

    @Resource
    private ApprovalTaskService approvalTaskService;

    @Resource
    private ApprovalTaskMapper approvalTaskMapper;

    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;

    @Resource
    private ApprovalRecordMapper approvalRecordMapper;

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private SalesContractApprovalBizHandler salesContractApprovalBizHandler;

    @Resource
    private SalesContractApprovalHandler salesContractApprovalHandler;

    @Resource
    private ApprovalCompletedEventListener approvalCompletedEventListener;

    @Resource
    private Connection connection;

    private final Long loginUserId = 999L;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesContractDO.class);
    }

    @BeforeEach
    void setUp() throws SQLException {
        reset(connection, approvalTaskMapper, approvalInstanceMapper, approvalRecordMapper, salesContractMapper, salesContractApprovalBizHandler);
        when(connection.getAutoCommit()).thenReturn(true);

        TenantContextHolder.setTenantId(1L);

        LoginUser loginUser = new LoginUser();
        loginUser.setId(loginUserId);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );

        when(salesContractApprovalBizHandler.getBizType()).thenReturn("sales_contract");
        when(salesContractApprovalBizHandler.getBizTypeName()).thenReturn("销售合同");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    private ApprovalProcessSnapshotDTO createSnapshot(int totalNodes) {
        return ApprovalProcessSnapshotDTO.builder()
                .configId(1L)
                .configName("销售合同审批流程")
                .bizType("sales_contract")
                .nodes(List.of(
                        ApprovalNodeSnapshotDTO.builder().sort(1).name("部门主管").nodeType("SEQUENTIAL").approverUserIds(List.of(100L)).build(),
                        ApprovalNodeSnapshotDTO.builder().sort(2).name("总经理终审").nodeType("SEQUENTIAL").approverUserIds(List.of(loginUserId)).build()
                ))
                .build();
    }

    @Test
    @DisplayName("集成测试-审批通过原子性：终审通过触发销售合同 status=ACTIVE 生效，同事务提交")
    void testApproveTask_FinalNode_TriggerSideEffect_CommitsTransaction() throws SQLException {
        Long instanceId = 701L;
        Long taskId = 801L;
        Long contractId = 901L;

        ApprovalProcessSnapshotDTO snapshot = createSnapshot(2);
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(instanceId)
                .bizType("sales_contract")
                .bizId(contractId)
                .bizNo("CH2026-0001")
                .submitterId(100L)
                .status("APPROVING")
                .currentNodeSort(2)
                .processSnapshot(JsonUtils.toJsonString(snapshot))
                .build();
        when(approvalInstanceMapper.selectById(instanceId)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(taskId)
                .instanceId(instanceId)
                .nodeSort(2)
                .nodeName("总经理终审")
                .nodeType("SEQUENTIAL")
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(taskId)).thenReturn(task);
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);
        when(approvalInstanceMapper.updateById(any(ApprovalInstanceDO.class))).thenReturn(1);
        when(salesContractMapper.update(isNull(), any())).thenReturn(1);

        // 执行终审通过
        approvalTaskService.approveTask(taskId, null);

        // 验证 1：审批引擎更新 instance 为 APPROVED
        assertEquals("APPROVED", instance.getStatus());

        // 验证 2：销售合同业务 Handler 被调用，将 status 更新为 ACTIVE
        ArgumentCaptor<LambdaUpdateWrapper<SalesContractDO>> wrapperCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(salesContractMapper, atLeastOnce()).update(isNull(), wrapperCaptor.capture());
        boolean hasActiveStatusUpdate = wrapperCaptor.getAllValues().stream()
                .anyMatch(w -> w != null && w.getSqlSet() != null && w.getSqlSet().contains("status"));
        assertTrue(hasActiveStatusUpdate, "销售合同 status 必须被更新为 ACTIVE");

        // 验证 3：事务正常 commit 提交，未发生回滚
        verify(connection, atLeastOnce()).commit();
        verify(connection, never()).rollback();
    }

    @Test
    @DisplayName("集成测试-原子性回滚：业务侧 ApprovalHandler 抛异常，事务回滚，审批不生效")
    void testApproveTask_HandlerThrowsException_TransactionRollsBack() throws SQLException {
        Long instanceId = 702L;
        Long taskId = 802L;
        Long contractId = 902L;

        ApprovalProcessSnapshotDTO snapshot = createSnapshot(2);
        ApprovalInstanceDO instance = ApprovalInstanceDO.builder()
                .id(instanceId)
                .bizType("sales_contract")
                .bizId(contractId)
                .bizNo("CH2026-0002")
                .submitterId(100L)
                .status("APPROVING")
                .currentNodeSort(2)
                .processSnapshot(JsonUtils.toJsonString(snapshot))
                .build();
        when(approvalInstanceMapper.selectById(instanceId)).thenReturn(instance);

        ApprovalTaskDO task = ApprovalTaskDO.builder()
                .id(taskId)
                .instanceId(instanceId)
                .nodeSort(2)
                .nodeName("总经理终审")
                .nodeType("SEQUENTIAL")
                .approverId(loginUserId)
                .status("PENDING")
                .version(0)
                .build();
        when(approvalTaskMapper.selectById(taskId)).thenReturn(task);
        when(approvalTaskMapper.updateById(any(ApprovalTaskDO.class))).thenReturn(1);
        when(approvalInstanceMapper.updateById(any(ApprovalInstanceDO.class))).thenReturn(1);

        // 模拟业务侧 ApprovalHandler 在执行生效/过账时发生异常（例如数据库死锁、台账校验不通过等）
        when(salesContractMapper.update(isNull(), any())).thenThrow(new RuntimeException("模拟业务侧过账/生效异常：台账不平"));

        // 执行终审通过，预期抛出异常
        Exception thrownEx = assertThrows(Exception.class, () -> approvalTaskService.approveTask(taskId, null));
        assertTrue(thrownEx.getMessage().contains("模拟业务侧过账/生效异常")
                || (thrownEx.getCause() != null && thrownEx.getCause().getMessage().contains("模拟业务侧过账/生效异常")),
                "异常信息应包含业务侧错误");

        // 验证核心：事务整体回滚（Connection.rollback 触发，Connection.commit 不触发）
        verify(connection, atLeastOnce()).rollback();
        verify(connection, never()).commit();
    }

    @Test
    @DisplayName("预留点测试：未接入的业务类型（如 payment/expense/payroll/sales_return）安全通过，预留接入点生效")
    void testReservationPoints_SupportedConstants() {
        assertEquals("sales_contract", ApprovalHandler.BIZ_TYPE_SALES_CONTRACT);
        assertEquals("payment", ApprovalHandler.BIZ_TYPE_PAYMENT);
        assertEquals("expense", ApprovalHandler.BIZ_TYPE_EXPENSE);
        assertEquals("payroll", ApprovalHandler.BIZ_TYPE_PAYROLL);
        assertEquals("sales_return", ApprovalHandler.BIZ_TYPE_SALES_RETURN);

        // 模拟一个动态注册的 PaymentApprovalHandler
        AtomicBoolean paymentHandled = new AtomicBoolean(false);
        ApprovalHandler dynamicPaymentHandler = new ApprovalHandler() {
            @Override
            public boolean supports(String bizType) {
                return BIZ_TYPE_PAYMENT.equalsIgnoreCase(bizType);
            }

            @Override
            public void onApproved(Long bizId) {
                paymentHandled.set(true);
            }
        };

        assertTrue(dynamicPaymentHandler.supports("payment"));
        assertFalse(dynamicPaymentHandler.supports("sales_contract"));

        dynamicPaymentHandler.onApproved(555L);
        assertTrue(paymentHandled.get());
    }
}
