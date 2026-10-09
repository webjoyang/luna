package com.luna.lunasrv.module.lunaerp.approval.config;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.*;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ApprovalConfigServiceImpl} 业务与守卫逻辑单元测试
 *
 * 覆盖 Issue: 02-config-crud-backend.md
 *  - CRUD 流程定义与节点整体替换
 *  - 业务守卫：启用冲突排他校验（1_030_030_007）
 *  - 业务守卫：实例引用阻断物理删除（1_030_030_008）
 *  - 流程不存在守卫（1_030_030_000）
 *  - 租户隔离
 */
class ApprovalConfigServiceImplTest {

    @Mock
    private ApprovalConfigMapper approvalConfigMapper;

    @Mock
    private ApprovalConfigNodeMapper approvalConfigNodeMapper;

    @Mock
    private ApprovalInstanceMapper approvalInstanceMapper;

    @InjectMocks
    private ApprovalConfigServiceImpl approvalConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("新建流程配置：成功插入流程定义及有序节点")
    void testCreateApprovalConfig_successWithNodes() {
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder()
                .bizType("sales_contract")
                .name("销售合同审批链")
                .status("ENABLED")
                .remark("测试备注")
                .nodes(List.of(
                        ApprovalConfigNodeVO.builder().sort(1).name("部门主管审批").nodeType("SEQUENTIAL").approverType("DEPT_LEADER").build(),
                        ApprovalConfigNodeVO.builder().sort(2).name("财务审核").nodeType("AND_SIGN").approverType("ROLE").approverIds("10").build()
                ))
                .build();

        when(approvalConfigMapper.selectByBizTypeAndStatus("sales_contract", "ENABLED")).thenReturn(null);
        doAnswer(invocation -> {
            ApprovalConfigDO entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        }).when(approvalConfigMapper).insert(any(ApprovalConfigDO.class));

        Long configId = approvalConfigService.createApprovalConfig(reqVO);

        assertEquals(100L, configId);
        verify(approvalConfigMapper).insert(any(ApprovalConfigDO.class));
        verify(approvalConfigNodeMapper, times(2)).insert(any(ApprovalConfigNodeDO.class));
    }

    @Test
    @DisplayName("新建流程配置：同 biz_type 已有 ENABLED 流程，抛出 1_030_030_007")
    void testCreateApprovalConfig_duplicateEnabled_throwsException() {
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder()
                .bizType("sales_contract")
                .name("重复销售合同审批链")
                .status("ENABLED")
                .build();

        ApprovalConfigDO existing = ApprovalConfigDO.builder().id(99L).bizType("sales_contract").status("ENABLED").build();
        when(approvalConfigMapper.selectByBizTypeAndStatus("sales_contract", "ENABLED")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalConfigService.createApprovalConfig(reqVO));
        assertEquals(APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED.getCode(), ex.getCode());
        verify(approvalConfigMapper, never()).insert(any(ApprovalConfigDO.class));
    }

    @Test
    @DisplayName("新建流程配置：同 biz_type 已有 ENABLED 流程但新建为 DISABLED，允许创建")
    void testCreateApprovalConfig_disabled_whenAnotherEnabledExists_success() {
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder()
                .bizType("sales_contract")
                .name("备用审批链")
                .status("DISABLED")
                .build();

        doAnswer(invocation -> {
            ApprovalConfigDO entity = invocation.getArgument(0);
            entity.setId(101L);
            return 1;
        }).when(approvalConfigMapper).insert(any(ApprovalConfigDO.class));

        Long configId = approvalConfigService.createApprovalConfig(reqVO);
        assertEquals(101L, configId);
        verify(approvalConfigMapper).insert(any(ApprovalConfigDO.class));
    }

    @Test
    @DisplayName("更新流程配置：节点整体替换（删旧插新）")
    void testUpdateApprovalConfig_successReplacesNodes() {
        Long configId = 100L;
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder()
                .id(configId)
                .bizType("payment")
                .name("付款申请流程新版")
                .status("ENABLED")
                .nodes(List.of(
                        ApprovalConfigNodeVO.builder().sort(1).name("直属领导审批").nodeType("SEQUENTIAL").approverType("SUBMITTER_LEADER").build()
                ))
                .build();

        ApprovalConfigDO current = ApprovalConfigDO.builder().id(configId).bizType("payment").status("DISABLED").build();
        when(approvalConfigMapper.selectById(configId)).thenReturn(current);
        when(approvalConfigMapper.selectByBizTypeAndStatus("payment", "ENABLED")).thenReturn(null);

        approvalConfigService.updateApprovalConfig(configId, reqVO);

        verify(approvalConfigMapper).updateById(any(ApprovalConfigDO.class));
        verify(approvalConfigNodeMapper).deleteByConfigId(configId);
        verify(approvalConfigNodeMapper, times(1)).insert(any(ApprovalConfigNodeDO.class));
    }

    @Test
    @DisplayName("更新流程配置：不存在抛出 1_030_030_000")
    void testUpdateApprovalConfig_notExists_throwsException() {
        Long configId = 999L;
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder().id(configId).bizType("payment").name("测试").build();
        when(approvalConfigMapper.selectById(configId)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalConfigService.updateApprovalConfig(configId, reqVO));
        assertEquals(APPROVAL_CONFIG_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新流程配置：启用时同 biz_type 已有其他 ENABLED 流程，抛出 1_030_030_007")
    void testUpdateApprovalConfig_enableWhenAnotherAlreadyEnabled_throwsException() {
        Long configId = 100L;
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder()
                .id(configId)
                .bizType("payment")
                .name("尝试启用")
                .status("ENABLED")
                .build();

        ApprovalConfigDO current = ApprovalConfigDO.builder().id(configId).bizType("payment").status("DISABLED").build();
        ApprovalConfigDO anotherEnabled = ApprovalConfigDO.builder().id(200L).bizType("payment").status("ENABLED").build();

        when(approvalConfigMapper.selectById(configId)).thenReturn(current);
        when(approvalConfigMapper.selectByBizTypeAndStatus("payment", "ENABLED")).thenReturn(anotherEnabled);

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalConfigService.updateApprovalConfig(configId, reqVO));
        assertEquals(APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("更新流程状态：成功停用或启用")
    void testUpdateApprovalConfigStatus_success() {
        Long configId = 100L;
        ApprovalConfigDO current = ApprovalConfigDO.builder().id(configId).bizType("expense").status("ENABLED").build();
        when(approvalConfigMapper.selectById(configId)).thenReturn(current);

        // 停用
        approvalConfigService.updateApprovalConfigStatus(configId, "DISABLED");
        verify(approvalConfigMapper).updateById(argThat((ApprovalConfigDO entity) -> "DISABLED".equals(entity.getStatus())));

        // 重新启用
        current.setStatus("DISABLED");
        when(approvalConfigMapper.selectByBizTypeAndStatus("expense", "ENABLED")).thenReturn(null);
        approvalConfigService.updateApprovalConfigStatus(configId, "ENABLED");
        verify(approvalConfigMapper).updateById(argThat((ApprovalConfigDO entity) -> "ENABLED".equals(entity.getStatus())));
    }

    @Test
    @DisplayName("更新流程状态：启用时同 biz_type 已有 ENABLED 抛出 1_030_030_007")
    void testUpdateApprovalConfigStatus_enableWhenAnotherAlreadyEnabled_throwsException() {
        Long configId = 100L;
        ApprovalConfigDO current = ApprovalConfigDO.builder().id(configId).bizType("expense").status("DISABLED").build();
        ApprovalConfigDO another = ApprovalConfigDO.builder().id(101L).bizType("expense").status("ENABLED").build();

        when(approvalConfigMapper.selectById(configId)).thenReturn(current);
        when(approvalConfigMapper.selectByBizTypeAndStatus("expense", "ENABLED")).thenReturn(another);

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalConfigService.updateApprovalConfigStatus(configId, "ENABLED"));
        assertEquals(APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除流程配置：未被实例引用时成功软删流程及节点")
    void testDeleteApprovalConfig_successWhenNoInstances() {
        Long configId = 100L;
        ApprovalConfigDO current = ApprovalConfigDO.builder().id(configId).bizType("payroll").status("DISABLED").build();
        when(approvalConfigMapper.selectById(configId)).thenReturn(current);
        when(approvalInstanceMapper.selectCount(any())).thenReturn(0L);

        approvalConfigService.deleteApprovalConfig(configId);

        verify(approvalConfigMapper).deleteById(configId);
        verify(approvalConfigNodeMapper).deleteByConfigId(configId);
    }

    @Test
    @DisplayName("删除流程配置：被实例快照引用时抛出 1_030_030_008 阻断删除")
    void testDeleteApprovalConfig_hasActiveInstanceSnapshot_throwsException() {
        Long configId = 100L;
        ApprovalConfigDO current = ApprovalConfigDO.builder().id(configId).bizType("payroll").status("DISABLED").build();
        when(approvalConfigMapper.selectById(configId)).thenReturn(current);
        // 模拟实例表存在引用该 configId 的快照
        when(approvalInstanceMapper.selectCount(any())).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> approvalConfigService.deleteApprovalConfig(configId));
        assertEquals(APPROVAL_CONFIG_HAS_ACTIVE_INSTANCE.getCode(), ex.getCode());
        verify(approvalConfigMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("获得流程详情：节点列表按 sort 升序")
    void testGetApprovalConfig_successReturnsSortedNodes() {
        Long configId = 100L;
        ApprovalConfigDO configDO = ApprovalConfigDO.builder().id(configId).bizType("sales_return").name("退货审批").status("ENABLED").build();
        when(approvalConfigMapper.selectById(configId)).thenReturn(configDO);

        List<ApprovalConfigNodeDO> nodes = List.of(
                ApprovalConfigNodeDO.builder().id(1L).configId(configId).sort(1).name("仓管核对").nodeType("SEQUENTIAL").approverType("ROLE").build(),
                ApprovalConfigNodeDO.builder().id(2L).configId(configId).sort(2).name("财务核验").nodeType("SEQUENTIAL").approverType("USER").build()
        );
        when(approvalConfigNodeMapper.selectListByConfigId(configId)).thenReturn(nodes);

        ApprovalConfigRespVO respVO = approvalConfigService.getApprovalConfig(configId);
        assertNotNull(respVO);
        assertEquals(100L, respVO.getId());
        assertEquals(2, respVO.getNodes().size());
        assertEquals(1, respVO.getNodes().get(0).getSort());
        assertEquals(2, respVO.getNodes().get(1).getSort());
    }

    @Test
    @DisplayName("多租户隔离校验：租户2无法读取或修改租户1的流程配置")
    void testTenantIsolation_cannotAccessOtherTenantConfig() {
        Long tenant1ConfigId = 100L;

        // 模拟租户上下文为租户 2，Mapper 查不到租户 1 的数据（返回 null）
        TenantContextHolder.setTenantId(2L);
        when(approvalConfigMapper.selectById(tenant1ConfigId)).thenReturn(null);

        // 租户 2 查租户 1 的配置报不存在
        ServiceException getEx = assertThrows(ServiceException.class, () -> approvalConfigService.getApprovalConfig(tenant1ConfigId));
        assertEquals(APPROVAL_CONFIG_NOT_EXISTS.getCode(), getEx.getCode());

        // 租户 2 删租户 1 的配置报不存在
        ServiceException delEx = assertThrows(ServiceException.class, () -> approvalConfigService.deleteApprovalConfig(tenant1ConfigId));
        assertEquals(APPROVAL_CONFIG_NOT_EXISTS.getCode(), delEx.getCode());

        // 租户 2 可以在同 biz_type 下创建自己的 ENABLED 流程，不受租户 1 影响
        ApprovalConfigSaveReqVO reqVO = ApprovalConfigSaveReqVO.builder()
                .bizType("sales_contract")
                .name("租户2销售合同审批链")
                .status("ENABLED")
                .build();
        when(approvalConfigMapper.selectByBizTypeAndStatus("sales_contract", "ENABLED")).thenReturn(null);
        doAnswer(inv -> {
            ApprovalConfigDO entity = inv.getArgument(0);
            entity.setId(201L);
            return 1;
        }).when(approvalConfigMapper).insert(any(ApprovalConfigDO.class));

        Long newId = approvalConfigService.createApprovalConfig(reqVO);
        assertEquals(201L, newId);
    }

}
