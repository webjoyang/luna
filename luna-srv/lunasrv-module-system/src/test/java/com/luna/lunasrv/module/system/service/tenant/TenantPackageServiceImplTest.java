package com.luna.lunasrv.module.system.service.tenant;

import com.luna.lunasrv.framework.common.enums.CommonStatusEnum;
import com.luna.lunasrv.module.system.controller.admin.tenant.vo.packages.TenantPackageSaveReqVO;
import com.luna.lunasrv.module.system.dal.dataobject.tenant.TenantDO;
import com.luna.lunasrv.module.system.dal.dataobject.tenant.TenantPackageDO;
import com.luna.lunasrv.module.system.dal.mysql.tenant.TenantPackageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link TenantPackageServiceImpl} 单元测试
 * 重点覆盖：套餐菜单变更时自动同步租户管理员及租户角色权限（开通/停用模块链路）
 */
class TenantPackageServiceImplTest {

    @InjectMocks
    private TenantPackageServiceImpl tenantPackageService;

    @Mock
    private TenantPackageMapper tenantPackageMapper;

    @Mock
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateTenantPackage_syncsTenantRoleMenusWhenModuleDisabled() {
        Long packageId = 100L;
        // 原核心包包含生产模块（10200）及基础信息（10000）
        Set<Long> originalMenuIds = Set.of(1L, 10000L, 10100L, 10200L, 10210L);
        // 新菜单集移除了生产（10200、10210）
        Set<Long> newMenuIds = Set.of(1L, 10000L, 10100L);

        TenantPackageDO existingPackage = TenantPackageDO.builder()
                .id(packageId)
                .name("核心包")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .menuIds(originalMenuIds)
                .build();
        when(tenantPackageMapper.selectById(packageId)).thenReturn(existingPackage);
        when(tenantPackageMapper.selectByName("核心包")).thenReturn(null);

        // 模拟使用该核心包的租户
        TenantDO tenantA = TenantDO.builder().id(121L).packageId(packageId).build();
        TenantDO tenantB = TenantDO.builder().id(122L).packageId(packageId).build();
        when(tenantService.getTenantListByPackageId(packageId)).thenReturn(List.of(tenantA, tenantB));

        TenantPackageSaveReqVO updateReqVO = new TenantPackageSaveReqVO();
        updateReqVO.setId(packageId);
        updateReqVO.setName("核心包");
        updateReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        updateReqVO.setMenuIds(newMenuIds);

        // 执行更新套餐（停用生产模块）
        tenantPackageService.updateTenantPackage(updateReqVO);

        // 验证数据库更新
        verify(tenantPackageMapper).updateById(any(TenantPackageDO.class));

        // 验证自动触发所有关联租户的角色菜单同步
        verify(tenantService).updateTenantRoleMenu(eq(121L), eq(newMenuIds));
        verify(tenantService).updateTenantRoleMenu(eq(122L), eq(newMenuIds));
    }

    @Test
    void testUpdateTenantPackage_syncsTenantRoleMenusWhenModuleRestored() {
        Long packageId = 100L;
        // 原套餐未包含生产模块
        Set<Long> originalMenuIds = Set.of(1L, 10000L, 10100L);
        // 新菜单集加回了生产模块（10200、10210）
        Set<Long> restoredMenuIds = Set.of(1L, 10000L, 10100L, 10200L, 10210L);

        TenantPackageDO existingPackage = TenantPackageDO.builder()
                .id(packageId)
                .name("核心包")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .menuIds(originalMenuIds)
                .build();
        when(tenantPackageMapper.selectById(packageId)).thenReturn(existingPackage);
        when(tenantPackageMapper.selectByName("核心包")).thenReturn(null);

        TenantDO tenantA = TenantDO.builder().id(121L).packageId(packageId).build();
        when(tenantService.getTenantListByPackageId(packageId)).thenReturn(List.of(tenantA));

        TenantPackageSaveReqVO updateReqVO = new TenantPackageSaveReqVO();
        updateReqVO.setId(packageId);
        updateReqVO.setName("核心包");
        updateReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        updateReqVO.setMenuIds(restoredMenuIds);

        // 执行更新套餐（恢复生产模块）
        tenantPackageService.updateTenantPackage(updateReqVO);

        // 验证自动触发租户角色菜单恢复
        verify(tenantService).updateTenantRoleMenu(eq(121L), eq(restoredMenuIds));
    }

    @Test
    void testUpdateTenantPackage_noSyncWhenMenuIdsUnchanged() {
        Long packageId = 100L;
        Set<Long> menuIds = Set.of(1L, 10000L, 10100L);

        TenantPackageDO existingPackage = TenantPackageDO.builder()
                .id(packageId)
                .name("核心包")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .menuIds(menuIds)
                .build();
        when(tenantPackageMapper.selectById(packageId)).thenReturn(existingPackage);
        when(tenantPackageMapper.selectByName("核心包-重命名")).thenReturn(null);

        TenantPackageSaveReqVO updateReqVO = new TenantPackageSaveReqVO();
        updateReqVO.setId(packageId);
        updateReqVO.setName("核心包-重命名");
        updateReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        updateReqVO.setMenuIds(menuIds); // 菜单未变

        tenantPackageService.updateTenantPackage(updateReqVO);

        verify(tenantPackageMapper).updateById(any(TenantPackageDO.class));
        // 菜单未变，不应触发租户同步
        verify(tenantService, never()).getTenantListByPackageId(any());
        verify(tenantService, never()).updateTenantRoleMenu(any(), any());
    }
}
