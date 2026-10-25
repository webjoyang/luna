package com.luna.lunasrv.module.system.service.permission;

import com.luna.lunasrv.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import com.luna.lunasrv.framework.common.enums.CommonStatusEnum;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.system.dal.dataobject.permission.RoleDO;
import com.luna.lunasrv.module.system.dal.dataobject.user.AdminUserDO;
import com.luna.lunasrv.module.system.enums.permission.DataScopeEnum;
import com.luna.lunasrv.module.system.service.dept.DeptService;
import com.luna.lunasrv.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.luna.lunasrv.module.system.enums.ErrorCodeConstants.ROLE_DATA_SCOPE_CUSTOM_DISABLED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link PermissionServiceImpl} 权限服务单元测试
 * 覆盖 Issue 06: 自定义数据权限收口（assignRoleDataScope 拒绝 dataScope=2，返回 1_002_002_006）
 */
class PermissionServiceImplTest {

    @Spy
    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Mock
    private RoleService roleService;

    @Mock
    private DeptService deptService;

    @Mock
    private AdminUserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("assignRoleDataScope: 当 dataScope 为 DEPT_CUSTOM(2) 时拒绝并抛出 1_002_002_006")
    void testAssignRoleDataScope_customDisabled() {
        Long roleId = 100L;
        Integer customScope = DataScopeEnum.DEPT_CUSTOM.getScope(); // 2
        Set<Long> deptIds = Set.of(10L, 20L);

        ServiceException exception = assertThrows(ServiceException.class, () ->
                permissionService.assignRoleDataScope(roleId, customScope, deptIds));

        assertEquals(ROLE_DATA_SCOPE_CUSTOM_DISABLED.getCode(), exception.getCode());
        assertEquals(ROLE_DATA_SCOPE_CUSTOM_DISABLED.getMsg(), exception.getMessage());
        assertEquals(1_002_002_006, exception.getCode());
        assertEquals("数据范围【自定义部门】已停用，请选择全部/本部门及以下/本部门/本人", exception.getMessage());

        // 验证底座 roleService 从未被调用
        verify(roleService, never()).updateRoleDataScope(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 3, 5})
    @DisplayName("assignRoleDataScope: 四档合法数据范围放行并调用 roleService.updateRoleDataScope")
    void testAssignRoleDataScope_allowedScopesSuccess(int dataScope) {
        Long roleId = 101L;
        Set<Long> deptIds = Collections.emptySet();

        permissionService.assignRoleDataScope(roleId, dataScope, deptIds);

        verify(roleService).updateRoleDataScope(roleId, dataScope, deptIds);
    }

    @Test
    @DisplayName("getDeptDataPermission: DataScopeEnum.DEPT_CUSTOM 存量计算分支保留可用")
    void testGetDeptDataPermission_customBranchRetained() {
        Long userId = 1L;
        Long roleId = 200L;

        // Mock 启用的角色列表
        RoleDO role = new RoleDO();
        role.setId(roleId);
        role.setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope());
        role.setDataScopeDeptIds(Set.of(1001L, 1002L));
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());

        doReturn(List.of(role)).when(permissionService).getEnableUserRoleListByUserIdFromCache(userId);

        AdminUserDO user = new AdminUserDO();
        user.setId(userId);
        user.setDeptId(1003L);
        when(userService.getUser(userId)).thenReturn(user);

        DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(userId);

        assertNotNull(result);
        assertFalse(result.getAll());
        assertTrue(result.getDeptIds().contains(1001L));
        assertTrue(result.getDeptIds().contains(1002L));
        assertTrue(result.getDeptIds().contains(1003L)); // 包含自己所在的部门
    }

}
