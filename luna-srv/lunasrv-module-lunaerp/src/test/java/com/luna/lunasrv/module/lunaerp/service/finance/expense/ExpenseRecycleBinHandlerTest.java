package com.luna.lunasrv.module.lunaerp.service.finance.expense;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.expense.ExpenseMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ExpenseRecycleBinHandler} 单元测试（Seam 4）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseRecycleBinHandlerTest {

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private ExpenseItemMapper expenseItemMapper;

    @InjectMocks
    private ExpenseRecycleBinHandler handler;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getBizType 契约：返回固定值 EXPENSE")
    void testGetBizType() {
        assertEquals("EXPENSE", handler.getBizType());
    }

    @Test
    @DisplayName("pageDeleted: 正常调用 Mapper 查回收站分页")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(inv -> {
            IPage<RecycleItemRespVO> page = inv.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizId(5001L);
            item.setDisplayName("BX20261019001");
            item.setBizType("EXPENSE");
            page.setRecords(List.of(item));
            page.setTotal(1L);
            return page;
        }).when(expenseMapper).selectRecyclePage(any(), eq(TENANT_ID));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getList().size());
        assertEquals("BX20261019001", result.getList().get(0).getDisplayName());
    }

    @Test
    @DisplayName("validateRestore: 记录不存在抛出 RECYCLE_BIN_RECORD_NOT_EXISTS")
    void testValidateRestore_NotFound() {
        when(expenseMapper.selectDeletedById(5001L, TENANT_ID)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(5001L));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("restore: 正常恢复（主从置 deleted=0, 主表置 PENDING）")
    void testRestore_Success() {
        ExpenseDO deleted = new ExpenseDO();
        deleted.setId(5001L);
        when(expenseMapper.selectDeletedById(5001L, TENANT_ID)).thenReturn(deleted);
        when(expenseMapper.restoreExpense(eq(5001L), eq(TENANT_ID), any(), any())).thenReturn(1);

        handler.restore(5001L);

        verify(expenseMapper).restoreExpense(eq(5001L), eq(TENANT_ID), any(), any());
        verify(expenseItemMapper).restoreExpenseItems(eq(5001L), eq(TENANT_ID), any(), any());
    }

    @Test
    @DisplayName("purge: 正常彻底物理删除主从表记录")
    void testPurge_Success() {
        ExpenseDO deleted = new ExpenseDO();
        deleted.setId(5001L);
        when(expenseMapper.selectDeletedById(5001L, TENANT_ID)).thenReturn(deleted);
        when(expenseMapper.purgeExpense(eq(5001L), eq(TENANT_ID))).thenReturn(1);

        handler.purge(5001L);

        verify(expenseItemMapper).purgeExpenseItems(5001L, TENANT_ID);
        verify(expenseMapper).purgeExpense(5001L, TENANT_ID);
    }

}
