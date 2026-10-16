package com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRatePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.exchangerate.vo.ExchangeRateUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.service.finance.exchangerate.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link ExchangeRateController} 单元测试（接缝 2: 契约与权限校验）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class ExchangeRateControllerTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateController controller;

    @Test
    @DisplayName("createExchangeRate 接口：委托 Service 并返回主键ID")
    void testCreateExchangeRate() {
        ExchangeRateSaveReqVO reqVO = new ExchangeRateSaveReqVO();
        reqVO.setCurrency("USD");
        reqVO.setRate(new BigDecimal("7.12345678"));
        reqVO.setEffectiveDate(LocalDate.of(2026, 10, 16));

        when(exchangeRateService.createExchangeRate(reqVO)).thenReturn(1001L);

        CommonResult<Long> result = controller.createExchangeRate(reqVO);
        assertTrue(result.isSuccess());
        assertEquals(1001L, result.getData());
        verify(exchangeRateService).createExchangeRate(reqVO);
    }

    @Test
    @DisplayName("updateExchangeRate 接口：委托 Service 并返回成功")
    void testUpdateExchangeRate() {
        ExchangeRateUpdateReqVO reqVO = new ExchangeRateUpdateReqVO();
        reqVO.setId(1001L);
        reqVO.setStatus("DISABLE");

        CommonResult<Boolean> result = controller.updateExchangeRate(reqVO);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(exchangeRateService).updateExchangeRate(reqVO);
    }

    @Test
    @DisplayName("getExchangeRate 接口：委托 Service 并返回详情")
    void testGetExchangeRate() {
        ExchangeRateRespVO respVO = new ExchangeRateRespVO();
        respVO.setId(1001L);
        respVO.setCurrency("USD");
        when(exchangeRateService.getExchangeRate(1001L)).thenReturn(respVO);

        CommonResult<ExchangeRateRespVO> result = controller.getExchangeRate(1001L);
        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(exchangeRateService).getExchangeRate(1001L);
    }

    @Test
    @DisplayName("getExchangeRatePage 接口：委托 Service 并返回分页")
    void testGetExchangeRatePage() {
        ExchangeRatePageReqVO pageReqVO = new ExchangeRatePageReqVO();
        PageResult<ExchangeRateRespVO> pageResult = new PageResult<>(List.of(new ExchangeRateRespVO()), 1L);
        when(exchangeRateService.getExchangeRatePage(pageReqVO)).thenReturn(pageResult);

        CommonResult<PageResult<ExchangeRateRespVO>> result = controller.getExchangeRatePage(pageReqVO);
        assertTrue(result.isSuccess());
        assertEquals(pageResult, result.getData());
        verify(exchangeRateService).getExchangeRatePage(pageReqVO);
    }

    @Test
    @DisplayName("无删除端点守卫：规约确认汇率主档严禁提供 delete 端点（失效即停用）")
    void testNoDeleteEndpoint() {
        Method[] methods = ExchangeRateController.class.getDeclaredMethods();
        for (Method method : methods) {
            assertFalse(method.getName().toLowerCase().contains("delete"),
                    "ExchangeRateController 严禁声明 delete 端点，违反汇率主档失效即停用规约: " + method.getName());
        }
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码")
    void testPermissions() throws NoSuchMethodException {
        assertPermission("createExchangeRate", new Class<?>[]{ExchangeRateSaveReqVO.class}, "@ss.hasPermission('lunaerp:finance:exchangerate:create')");
        assertPermission("updateExchangeRate", new Class<?>[]{ExchangeRateUpdateReqVO.class}, "@ss.hasPermission('lunaerp:finance:exchangerate:update')");
        assertPermission("getExchangeRate", new Class<?>[]{Long.class}, "@ss.hasPermission('lunaerp:finance:exchangerate:query')");
        assertPermission("getExchangeRatePage", new Class<?>[]{ExchangeRatePageReqVO.class}, "@ss.hasPermission('lunaerp:finance:exchangerate:query')");
    }

    private void assertPermission(String methodName, Class<?>[] paramTypes, String expectedExpression) throws NoSuchMethodException {
        Method method = ExchangeRateController.class.getMethod(methodName, paramTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " 必须标注 @PreAuthorize");
        assertEquals(expectedExpression, annotation.value());
    }

}
