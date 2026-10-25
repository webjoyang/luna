package com.luna.lunasrv.module.lunaerp.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.expense.ExpenseItemDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TableInfoTestHelper 单元测试
 */
public class TableInfoTestHelperTest {

    @Test
    @DisplayName("测试：initAllEntities 能正确预热所有 DO 且支持构造 LambdaQueryWrapper")
    public void testInitAllEntities() {
        // 执行全量预热
        TableInfoTestHelper.initAllEntities();

        // 验证 TableInfo 存在
        TableInfo expenseTableInfo = TableInfoHelper.getTableInfo(ExpenseDO.class);
        assertNotNull(expenseTableInfo, "ExpenseDO TableInfo 必须预热成功");

        TableInfo itemTableInfo = TableInfoHelper.getTableInfo(ExpenseItemDO.class);
        assertNotNull(itemTableInfo, "ExpenseItemDO TableInfo 必须预热成功");

        // 验证构造 LambdaQueryWrapper 并引用方法引用不抛出异常
        assertDoesNotThrow(() -> {
            LambdaQueryWrapper<ExpenseDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExpenseDO::getId, 100L);
            wrapper.eq(ExpenseDO::getApprovalStatus, "APPROVED");
        });

        // 验证幂等调用
        assertDoesNotThrow(() -> {
            TableInfoTestHelper.initAllEntities();
            TableInfoTestHelper.initTableInfo(ExpenseDO.class, ExpenseItemDO.class, null);
        });
    }
}
