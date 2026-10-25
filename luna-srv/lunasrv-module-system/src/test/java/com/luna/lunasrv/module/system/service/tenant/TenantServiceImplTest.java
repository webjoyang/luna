package com.luna.lunasrv.module.system.service.tenant;

import com.luna.lunasrv.framework.common.enums.CommonStatusEnum;
import com.luna.lunasrv.module.system.controller.admin.tenant.vo.tenant.TenantSaveReqVO;
import com.luna.lunasrv.module.system.dal.dataobject.tenant.TenantDO;
import com.luna.lunasrv.module.system.dal.dataobject.tenant.TenantPackageDO;
import com.luna.lunasrv.module.system.dal.mysql.tenant.TenantMapper;
import com.luna.lunasrv.module.system.service.permission.MenuService;
import com.luna.lunasrv.module.system.service.permission.PermissionService;
import com.luna.lunasrv.module.system.service.permission.RoleService;
import com.luna.lunasrv.module.system.service.user.AdminUserService;
import com.luna.lunasrv.framework.tenant.config.TenantProperties;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.system.dal.dataobject.permission.RoleDO;
import com.luna.lunasrv.module.system.enums.permission.RoleCodeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link TenantServiceImpl} 租户服务单元测试（核心包默认绑定与套餐校验）
 */
class TenantServiceImplTest {

    @InjectMocks
    private TenantServiceImpl tenantService;

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private TenantPackageService tenantPackageService;

    @Mock
    private AdminUserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private MenuService menuService;

    @Mock
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateTenant_defaultCorePackage() {
        // 准备请求：packageId 为空，模拟新租户默认未传套餐
        TenantSaveReqVO reqVO = new TenantSaveReqVO();
        reqVO.setName("新客户纺织");
        reqVO.setContactName("张经理");
        reqVO.setContactMobile("13800000000");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setAccountCount(20);
        reqVO.setExpireTime(LocalDateTime.now().plusYears(1));
        reqVO.setUsername("zhangsan");
        reqVO.setPassword("123456");
        reqVO.setPackageId(null);

        // Mock 核心包校验通过
        TenantPackageDO corePackage = TenantPackageDO.builder()
                .id(TenantDO.PACKAGE_ID_CORE)
                .name("核心包")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .menuIds(Set.of(1L, 10000L, 10100L, 10200L, 10300L, 10600L, 10700L, 10800L))
                .build();
        when(tenantPackageService.validTenantPackage(eq(TenantDO.PACKAGE_ID_CORE))).thenReturn(corePackage);
        when(tenantMapper.selectByName("新客户纺织")).thenReturn(null);

        doAnswer(invocation -> {
            TenantDO entity = invocation.getArgument(0);
            entity.setId(888L);
            return 1;
        }).when(tenantMapper).insert(any(TenantDO.class));

        when(roleService.createRole(any(), any())).thenReturn(999L);
        when(userService.createUser(any())).thenReturn(1001L);

        // 调用创建
        Long tenantId = tenantService.createTenant(reqVO);

        // 断言：默认被绑定到核心包套餐（ID: 100）
        assertEquals(888L, tenantId);
        assertEquals(TenantDO.PACKAGE_ID_CORE, reqVO.getPackageId());

        // 校验 tenantPackageService 验证的是核心包
        verify(tenantPackageService).validTenantPackage(TenantDO.PACKAGE_ID_CORE);

        // 校验落库 tenant 的 packageId 为核心包 ID
        ArgumentCaptor<TenantDO> tenantCaptor = ArgumentCaptor.forClass(TenantDO.class);
        verify(tenantMapper).insert(tenantCaptor.capture());
        assertEquals(TenantDO.PACKAGE_ID_CORE, tenantCaptor.getValue().getPackageId());
    }

    @Test
    void testCreateTenant_explicitPackage() {
        Long explicitPackageId = 111L;
        TenantSaveReqVO reqVO = new TenantSaveReqVO();
        reqVO.setName("定制租户");
        reqVO.setContactName("李经理");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setAccountCount(10);
        reqVO.setExpireTime(LocalDateTime.now().plusYears(1));
        reqVO.setUsername("lisi");
        reqVO.setPassword("123456");
        reqVO.setPackageId(explicitPackageId);

        TenantPackageDO customPackage = TenantPackageDO.builder()
                .id(explicitPackageId)
                .name("定制套餐")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .menuIds(Set.of(1L, 10000L))
                .build();
        when(tenantPackageService.validTenantPackage(eq(explicitPackageId))).thenReturn(customPackage);
        when(tenantMapper.selectByName("定制租户")).thenReturn(null);

        doAnswer(invocation -> {
            TenantDO entity = invocation.getArgument(0);
            entity.setId(889L);
            return 1;
        }).when(tenantMapper).insert(any(TenantDO.class));

        when(roleService.createRole(any(), any())).thenReturn(998L);
        when(userService.createUser(any())).thenReturn(1002L);

        Long tenantId = tenantService.createTenant(reqVO);

        assertEquals(889L, tenantId);
        assertEquals(explicitPackageId, reqVO.getPackageId());
        verify(tenantPackageService).validTenantPackage(explicitPackageId);
    }

    @Test
    void testUpdateTenantRoleMenu_disableAndRestoreProductionModule() {
        Long tenantId = 121L;

        // 模拟租户角色：管理员 (tenant_admin) 与 普通操作员 (operator)
        RoleDO adminRole = new RoleDO();
        adminRole.setId(10L);
        adminRole.setTenantId(tenantId);
        adminRole.setCode(RoleCodeEnum.TENANT_ADMIN.getCode());

        RoleDO operatorRole = new RoleDO();
        operatorRole.setId(20L);
        operatorRole.setTenantId(tenantId);
        operatorRole.setCode("operator");

        when(roleService.getRoleList()).thenReturn(List.of(adminRole, operatorRole));
        // 操作员原本拥有基础信息(10000)与生产(10200)权限
        when(permissionService.getRoleMenuListByRoleId(20L)).thenReturn(Set.of(10000L, 10200L));

        // 场景 1：核心包停用「生产」模块，剩余菜单集不含 10200
        Set<Long> menuIdsWithoutProduction = Set.of(1L, 10000L, 10100L);
        tenantService.updateTenantRoleMenu(tenantId, menuIdsWithoutProduction);

        // 校验：管理员直接赋值为套餐新菜单集（无 10200）
        verify(permissionService).assignRoleMenu(10L, menuIdsWithoutProduction);
        // 校验：普通角色取交集，生产(10200)被移除，仅剩 10000
        verify(permissionService).assignRoleMenu(20L, Set.of(10000L));

        // 场景 2：将「生产」重新加回核心包，菜单集恢复包含 10200
        Set<Long> restoredMenuIds = Set.of(1L, 10000L, 10100L, 10200L);
        tenantService.updateTenantRoleMenu(tenantId, restoredMenuIds);

        // 校验：管理员权限恢复包含 10200
        verify(permissionService).assignRoleMenu(10L, restoredMenuIds);
    }

    @Test
    void testHandleTenantMenu_filterUnactivatedModuleMenus() {
        Long tenantId = 121L;
        Long packageId = 100L;

        TenantProperties tenantProperties = new TenantProperties();
        tenantProperties.setEnable(true);
        ReflectionTestUtils.setField(tenantService, "tenantProperties", tenantProperties);

        TenantDO tenant = TenantDO.builder().id(tenantId).packageId(packageId).build();
        when(tenantMapper.selectById(tenantId)).thenReturn(tenant);

        // 套餐中已停用生产模块，仅包含 1, 10000(基础信息), 10100(销售)
        TenantPackageDO corePackageWithoutProduction = TenantPackageDO.builder()
                .id(packageId)
                .name("核心包")
                .menuIds(Set.of(1L, 10000L, 10100L))
                .build();
        when(tenantPackageService.getTenantPackage(packageId)).thenReturn(corePackageWithoutProduction);

        TenantContextHolder.setTenantId(tenantId);
        try {
            List<Long> currentCandidateMenus = new ArrayList<>(List.of(10000L, 10100L, 10200L, 10210L));
            tenantService.handleTenantMenu(allowedMenuIds -> {
                currentCandidateMenus.removeIf(mId -> !allowedMenuIds.contains(mId));
            });

            // 断言：未开通的生产模块（10200）及其子菜单（10210）被过滤
            assertTrue(currentCandidateMenus.contains(10000L));
            assertTrue(currentCandidateMenus.contains(10100L));
            assertFalse(currentCandidateMenus.contains(10200L));
            assertFalse(currentCandidateMenus.contains(10210L));
            assertEquals(2, currentCandidateMenus.size());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
