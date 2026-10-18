package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveConstants;
import com.luna.lunasrv.module.lunaerp.common.desensitize.SensitiveType;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryReqVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging.CustomerAgingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerAgingController} 单元测试（契约与权限校验，Seam C）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class CustomerAgingControllerTest {

    @Mock
    private CustomerAgingService customerAgingService;

    @InjectMocks
    private CustomerAgingController controller;

    @Test
    @DisplayName("getAgingSummary 接口：委托 Service 并返回账龄汇总数据")
    void testGetAgingSummary() {
        CustomerAgingSummaryReqVO reqVO = CustomerAgingSummaryReqVO.builder()
                .agingDate(LocalDate.of(2026, 10, 18))
                .customerIds(List.of(100L))
                .salesmanId(2048L)
                .build();

        CustomerAgingSummaryRespVO respVO = CustomerAgingSummaryRespVO.builder()
                .customerId(100L)
                .customerName("客户甲")
                .totalArBalance(new BigDecimal("1000.00"))
                .build();
        when(customerAgingService.getAgingSummary(reqVO)).thenReturn(List.of(respVO));

        CommonResult<List<CustomerAgingSummaryRespVO>> result = controller.getAgingSummary(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(respVO, result.getData().get(0));
        verify(customerAgingService).getAgingSummary(reqVO);
    }

    @Test
    @DisplayName("getAgingDetail 接口：委托 Service 并返回未核销明细分录")
    void testGetAgingDetail() {
        CustomerAgingDetailReqVO reqVO = CustomerAgingDetailReqVO.builder()
                .customerId(100L)
                .agingDate(LocalDate.of(2026, 10, 18))
                .build();

        CustomerAgingDetailRespVO detailVO = CustomerAgingDetailRespVO.builder()
                .ledgerEntryId(1L)
                .refNo("SH001")
                .remainingAmount(new BigDecimal("1000.00"))
                .build();
        when(customerAgingService.getAgingDetail(reqVO)).thenReturn(List.of(detailVO));

        CommonResult<List<CustomerAgingDetailRespVO>> result = controller.getAgingDetail(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(detailVO, result.getData().get(0));
        verify(customerAgingService).getAgingDetail(reqVO);
    }

    @Test
    @DisplayName("权限注解校验：确认各端点声明了正确的权限码")
    void testPermissions() throws NoSuchMethodException {
        assertPermission("getAgingSummary", new Class<?>[]{CustomerAgingSummaryReqVO.class},
                "@ss.hasPermission('lunaerp:finance:customerrecon:query')");
        assertPermission("getAgingDetail", new Class<?>[]{CustomerAgingDetailReqVO.class},
                "@ss.hasPermission('lunaerp:finance:customerrecon:query')");
    }

    @Test
    @DisplayName("脱敏规约校验：验证汇总与明细 RespVO 的所有 BigDecimal 金额字段均标注 @Sensitive(AMOUNT)")
    void testDesensitizationAnnotations() {
        assertBigDecimalFieldsAnnotatedWithSensitiveAmount(CustomerAgingSummaryRespVO.class);
        assertBigDecimalFieldsAnnotatedWithSensitiveAmount(CustomerAgingDetailRespVO.class);
    }

    private void assertPermission(String methodName, Class<?>[] paramTypes, String expectedExpression) throws NoSuchMethodException {
        Method method = CustomerAgingController.class.getMethod(methodName, paramTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, methodName + " 必须标注 @PreAuthorize");
        assertEquals(expectedExpression, annotation.value());
    }

    private void assertBigDecimalFieldsAnnotatedWithSensitiveAmount(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (BigDecimal.class.equals(field.getType())) {
                Sensitive sensitive = field.getAnnotation(Sensitive.class);
                assertNotNull(sensitive, clazz.getSimpleName() + "." + field.getName() + " 缺失 @Sensitive 注解");
                assertEquals(SensitiveType.AMOUNT, sensitive.type(),
                        clazz.getSimpleName() + "." + field.getName() + " 的 SensitiveType 必须为 AMOUNT");
                assertEquals(SensitiveConstants.PERMISSION_AMOUNT, sensitive.seePermission(),
                        clazz.getSimpleName() + "." + field.getName() + " 的 seePermission 必须为 PERMISSION_AMOUNT");
            }
        }
    }
}
