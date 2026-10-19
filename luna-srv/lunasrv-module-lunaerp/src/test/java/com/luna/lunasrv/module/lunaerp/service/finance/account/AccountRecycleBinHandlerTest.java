package com.luna.lunasrv.module.lunaerp.service.finance.account;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.account.AccountDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.account.AccountMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 资金账户回收站业务处理器单元测试（接缝 3）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class AccountRecycleBinHandlerTest {

    @InjectMocks
    private AccountRecycleBinHandler handler;

    @Mock
    private AccountMapper accountMapper;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getBizType: 返回 FINANCE_ACCOUNT")
    void testGetBizType() {
        assertEquals("FINANCE_ACCOUNT", handler.getBizType());
    }

    @Test
    @DisplayName("pageDeleted: 分页检索已软删除账户列表")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        RecycleItemRespVO item = new RecycleItemRespVO();
        item.setBizId(101L);
        item.setDisplayName("已删测试户");

        doAnswer(inv -> {
            IPage<RecycleItemRespVO> page = inv.getArgument(0);
            page.setRecords(List.of(item));
            page.setTotal(1);
            return page;
        }).when(accountMapper).selectRecyclePage(any(), eq(1L));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertEquals(1, result.getTotal());
        assertEquals("已删测试户", result.getList().get(0).getDisplayName());
    }

    @Test
    @DisplayName("validateRestore: 目标不在回收站中抛出 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testValidateRestore_notInRecycle_throwsException() {
        when(accountMapper.selectDeletedById(999L, 1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(999L));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("restore: 正常恢复资金账户，保持原状态")
    void testRestore_success() {
        AccountDO deleted = new AccountDO();
        deleted.setId(101L);
        deleted.setAccountName("恢复账户");
        deleted.setStatus("ENABLE"); // 原状态为 ENABLE
        when(accountMapper.selectDeletedById(101L, 1L)).thenReturn(deleted);
        when(accountMapper.restoreAccount(eq(101L), eq(1L), any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(101L));
        verify(accountMapper).restoreAccount(eq(101L), eq(1L), any(), any());
    }

    @Test
    @DisplayName("hasActiveReference: 存在经营分录引用返回 true")
    void testHasActiveReference_hasEntries_returnsTrue() {
        when(accountMapper.selectActiveReferenceCount(101L, 1L)).thenReturn(2L);
        assertTrue(handler.hasActiveReference(101L));
    }

    @Test
    @DisplayName("purge: 存在活跃分录引用时禁止彻底删除，抛出 RECYCLE_BIN_HAS_ACTIVE_REFERENCE")
    void testPurge_hasActiveReference_throwsException() {
        AccountDO deleted = new AccountDO();
        deleted.setId(101L);
        when(accountMapper.selectDeletedById(101L, 1L)).thenReturn(deleted);
        when(accountMapper.selectActiveReferenceCount(101L, 1L)).thenReturn(3L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(101L));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
        verify(accountMapper, never()).purgeAccount(any(), any());
    }

    @Test
    @DisplayName("purge: 无活跃分录引用时彻底删除成功")
    void testPurge_noReference_success() {
        AccountDO deleted = new AccountDO();
        deleted.setId(101L);
        when(accountMapper.selectDeletedById(101L, 1L)).thenReturn(deleted);
        when(accountMapper.selectActiveReferenceCount(101L, 1L)).thenReturn(0L);
        when(accountMapper.purgeAccount(101L, 1L)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(101L));
        verify(accountMapper).purgeAccount(101L, 1L);
    }

}
