package com.luna.lunasrv.module.lunaerp.controller.finance.account;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.account.TransferService;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 账户间转账控制器单元测试（权限/脱敏/留痕/纵向越权 403/不可改删）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class TransferControllerTest {

    @InjectMocks
    private TransferController transferController;

    @Mock
    private TransferService transferService;

    @Test
    @DisplayName("createTransfer: 正常委托 Service 登记转账单")
    void testCreateTransfer() {
        TransferCreateReqVO req = new TransferCreateReqVO();
        req.setFromAccountId(100L);
        req.setToAccountId(200L);
        req.setCurrency("RMB");
        req.setAmount(new BigDecimal("1000.00"));
        req.setTransferDate(LocalDate.of(2026, 10, 19));

        when(transferService.createTransfer(req)).thenReturn(888L);

        CommonResult<Long> result = transferController.createTransfer(req);
        assertEquals(0, result.getCode());
        assertEquals(888L, result.getData());
        verify(transferService).createTransfer(req);
    }

    @Test
    @DisplayName("getTransfer & getTransferPage: 正常委托 Service 查询详情与分页")
    void testGetTransferAndPage() {
        TransferRespVO respVO = new TransferRespVO();
        respVO.setId(888L);
        respVO.setTransferNo("ZZ20261019001");
        respVO.setAmount(new BigDecimal("1000.00"));

        when(transferService.getTransfer(888L)).thenReturn(respVO);

        CommonResult<TransferRespVO> getResult = transferController.getTransfer(888L);
        assertEquals(0, getResult.getCode());
        assertEquals("ZZ20261019001", getResult.getData().getTransferNo());

        TransferPageReqVO pageReq = new TransferPageReqVO();
        PageResult<TransferRespVO> pageResult = new PageResult<>(List.of(respVO), 1L);
        when(transferService.getTransferPage(pageReq)).thenReturn(pageResult);

        CommonResult<PageResult<TransferRespVO>> pageResultRes = transferController.getTransferPage(pageReq);
        assertEquals(0, pageResultRes.getCode());
        assertEquals(1, pageResultRes.getData().getList().size());
    }

    @Test
    @DisplayName("不可改删守卫：验证 TransferController 绝不暴露 update 或 delete 端点（登记即生效不可改删）")
    void testNoUpdateOrDeleteEndpoints() {
        Method[] methods = TransferController.class.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName().toLowerCase();
            assertFalse(name.contains("update"), "TransferController 严禁声明 update 端点: " + method.getName());
            assertFalse(name.contains("delete"), "TransferController 严禁声明 delete 端点: " + method.getName());
            assertFalse(name.contains("modify"), "TransferController 严禁声明 modify 端点: " + method.getName());
            assertFalse(name.contains("cancel"), "TransferController 严禁声明 cancel 端点: " + method.getName());
        }
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码（纵向越权守卫）")
    void testPermissions() throws NoSuchMethodException {
        Method createMethod = TransferController.class.getMethod("createTransfer", TransferCreateReqVO.class);
        PreAuthorize createAuth = createMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(createAuth, "createTransfer 必须标注 @PreAuthorize 防纵向越权");
        assertEquals("@ss.hasPermission('lunaerp:finance:account:transfer')", createAuth.value());

        Method getMethod = TransferController.class.getMethod("getTransfer", Long.class);
        PreAuthorize getAuth = getMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(getAuth, "getTransfer 必须标注 @PreAuthorize");
        assertEquals("@ss.hasAnyPermissions('lunaerp:finance:account:transfer', 'lunaerp:finance:account:query')", getAuth.value());

        Method pageMethod = TransferController.class.getMethod("getTransferPage", TransferPageReqVO.class);
        PreAuthorize pageAuth = pageMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(pageAuth, "getTransferPage 必须标注 @PreAuthorize");
        assertEquals("@ss.hasAnyPermissions('lunaerp:finance:account:transfer', 'lunaerp:finance:account:query')", pageAuth.value());
    }

    @Test
    @DisplayName("脱敏注解审查：TransferRespVO 金额字段正确标注 @Sensitive(AMOUNT)")
    void testSensitiveAnnotationOnAmount() throws NoSuchFieldException {
        Field amountField = TransferRespVO.class.getDeclaredField("amount");
        Sensitive sens = amountField.getAnnotation(Sensitive.class);
        assertNotNull(sens, "amount 字段必须标注 @Sensitive");
        assertEquals(SensitiveConstants.PERMISSION_AMOUNT, sens.seePermission());
        assertEquals(SensitiveType.AMOUNT, sens.type());
    }

    @Test
    @DisplayName("留痕注解审查：TransferServiceImpl.createTransfer 正确标注 @LogRecord")
    void testLogRecordAnnotation() throws NoSuchMethodException, ClassNotFoundException {
        Class<?> clazz = Class.forName("com.luna.lunasrv.module.lunaerp.service.finance.account.TransferServiceImpl");
        Method method = clazz.getMethod("createTransfer", TransferCreateReqVO.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "createTransfer 必须标注 @LogRecord");
        assertEquals("FINANCE_ACCOUNT", logRecord.type());
        assertEquals("TRANSFER", logRecord.subType());
    }

}
