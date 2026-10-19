package com.luna.lunasrv.module.lunaerp.controller.finance.account;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import com.luna.lunasrv.module.lunaerp.controller.finance.account.vo.*;
import com.luna.lunasrv.module.lunaerp.service.finance.account.OtherTxnService;
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
 * 其他收支单控制器单元测试（权限/脱敏/留痕/纵向越权 403/不可改删）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class OtherTxnControllerTest {

    @InjectMocks
    private OtherTxnController otherTxnController;

    @Mock
    private OtherTxnService otherTxnService;

    @Test
    @DisplayName("createOtherTxn: 正常委托 Service 登记其他收支单")
    void testCreateOtherTxn() {
        OtherTxnCreateReqVO req = new OtherTxnCreateReqVO();
        req.setAccountId(100L);
        req.setDirection("DEBIT");
        req.setCategory("INTEREST_IN");
        req.setOriginalAmount(new BigDecimal("150.00"));
        req.setTxnDate(LocalDate.of(2026, 10, 19));

        when(otherTxnService.createOtherTxn(req)).thenReturn(666L);

        CommonResult<Long> result = otherTxnController.createOtherTxn(req);
        assertEquals(0, result.getCode());
        assertEquals(666L, result.getData());
        verify(otherTxnService).createOtherTxn(req);
    }

    @Test
    @DisplayName("getOtherTxn & getOtherTxnPage: 正常委托 Service 查询详情与分页")
    void testGetOtherTxnAndPage() {
        OtherTxnRespVO respVO = new OtherTxnRespVO();
        respVO.setId(666L);
        respVO.setTxnNo("QT20261019001");
        respVO.setOriginalAmount(new BigDecimal("150.00"));

        when(otherTxnService.getOtherTxn(666L)).thenReturn(respVO);

        CommonResult<OtherTxnRespVO> getResult = otherTxnController.getOtherTxn(666L);
        assertEquals(0, getResult.getCode());
        assertEquals("QT20261019001", getResult.getData().getTxnNo());

        OtherTxnPageReqVO pageReq = new OtherTxnPageReqVO();
        PageResult<OtherTxnRespVO> pageResult = new PageResult<>(List.of(respVO), 1L);
        when(otherTxnService.getOtherTxnPage(pageReq)).thenReturn(pageResult);

        CommonResult<PageResult<OtherTxnRespVO>> pageResultRes = otherTxnController.getOtherTxnPage(pageReq);
        assertEquals(0, pageResultRes.getCode());
        assertEquals(1, pageResultRes.getData().getList().size());
    }

    @Test
    @DisplayName("不可改删守卫：验证 OtherTxnController 绝不暴露 update 或 delete 端点（凭证属性不可改删）")
    void testNoUpdateOrDeleteEndpoints() {
        Method[] methods = OtherTxnController.class.getDeclaredMethods();
        for (Method method : methods) {
            String name = method.getName().toLowerCase();
            assertFalse(name.contains("update"), "OtherTxnController 严禁声明 update 端点: " + method.getName());
            assertFalse(name.contains("delete"), "OtherTxnController 严禁声明 delete 端点: " + method.getName());
            assertFalse(name.contains("modify"), "OtherTxnController 严禁声明 modify 端点: " + method.getName());
            assertFalse(name.contains("cancel"), "OtherTxnController 严禁声明 cancel 端点: " + method.getName());
        }
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码（纵向越权守卫）")
    void testPermissions() throws NoSuchMethodException {
        Method createMethod = OtherTxnController.class.getMethod("createOtherTxn", OtherTxnCreateReqVO.class);
        PreAuthorize createAuth = createMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(createAuth, "createOtherTxn 必须标注 @PreAuthorize 防纵向越权");
        assertEquals("@ss.hasPermission('lunaerp:finance:account:other-txn')", createAuth.value());

        Method getMethod = OtherTxnController.class.getMethod("getOtherTxn", Long.class);
        PreAuthorize getAuth = getMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(getAuth, "getOtherTxn 必须标注 @PreAuthorize");
        assertEquals("@ss.hasAnyPermissions('lunaerp:finance:account:other-txn', 'lunaerp:finance:account:query')", getAuth.value());

        Method pageMethod = OtherTxnController.class.getMethod("getOtherTxnPage", OtherTxnPageReqVO.class);
        PreAuthorize pageAuth = pageMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(pageAuth, "getOtherTxnPage 必须标注 @PreAuthorize");
        assertEquals("@ss.hasAnyPermissions('lunaerp:finance:account:other-txn', 'lunaerp:finance:account:query')", pageAuth.value());
    }

    @Test
    @DisplayName("脱敏注解审查：OtherTxnRespVO 金额字段正确标注 @Sensitive(AMOUNT)")
    void testSensitiveAnnotationsOnAmounts() throws NoSuchFieldException {
        Field originalAmountField = OtherTxnRespVO.class.getDeclaredField("originalAmount");
        Sensitive origSens = originalAmountField.getAnnotation(Sensitive.class);
        assertNotNull(origSens, "originalAmount 必须标注 @Sensitive");
        assertEquals(SensitiveConstants.PERMISSION_AMOUNT, origSens.seePermission());
        assertEquals(SensitiveType.AMOUNT, origSens.type());

        Field baseAmountField = OtherTxnRespVO.class.getDeclaredField("baseAmount");
        Sensitive baseSens = baseAmountField.getAnnotation(Sensitive.class);
        assertNotNull(baseSens, "baseAmount 必须标注 @Sensitive");
        assertEquals(SensitiveConstants.PERMISSION_AMOUNT, baseSens.seePermission());
        assertEquals(SensitiveType.AMOUNT, baseSens.type());
    }

    @Test
    @DisplayName("留痕注解审查：OtherTxnServiceImpl.createOtherTxn 正确标注 @LogRecord")
    void testLogRecordAnnotation() throws NoSuchMethodException, ClassNotFoundException {
        Class<?> clazz = Class.forName("com.luna.lunasrv.module.lunaerp.service.finance.account.OtherTxnServiceImpl");
        Method method = clazz.getMethod("createOtherTxn", OtherTxnCreateReqVO.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "createOtherTxn 必须标注 @LogRecord");
        assertEquals("FINANCE_ACCOUNT", logRecord.type());
        assertEquals("OTHER_TXN", logRecord.subType());
    }

}
