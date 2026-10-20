package com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon;

import cn.hutool.core.util.ArrayUtil;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.*;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.CustomerStatementService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link CustomerStatementPortalController} 单元测试（契约路由与透传校验）
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
public class CustomerStatementPortalControllerTest {

    @Mock
    private CustomerStatementService customerStatementService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CustomerStatementPortalController controller;

    @Test
    @DisplayName("规约审查：控制器必须标注 @PermitAll 免登录注解，且映射单一标准路径 /app-api/lunaerp/portal/finance/statement")
    void testControllerClassAnnotations() {
        PermitAll permitAll = CustomerStatementPortalController.class.getAnnotation(PermitAll.class);
        assertNotNull(permitAll, "CustomerStatementPortalController 必须标注 @PermitAll 免密访问");

        RequestMapping requestMapping = CustomerStatementPortalController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "必须标注 @RequestMapping");
        String[] values = requestMapping.value();
        assertEquals(1, values.length, "必须映射单一标准路径");
        assertEquals("/app-api/lunaerp/portal/finance/statement", values[0], "必须映射标准路径 /app-api/lunaerp/portal/finance/statement");
    }

    @Test
    @DisplayName("GET /list 接口：正确委托 Service 并透传 customerId 参数")
    void testGetCustomerStatementList() {
        CustomerStatementRespVO vo = new CustomerStatementRespVO();
        vo.setId(100L);
        vo.setStatementNo("CS20260001");
        when(customerStatementService.getPortalCustomerStatementList(1001L)).thenReturn(List.of(vo));

        CommonResult<List<CustomerStatementRespVO>> result = controller.getCustomerStatementList(1001L);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(100L, result.getData().get(0).getId());
        verify(customerStatementService).getPortalCustomerStatementList(1001L);
    }

    @Test
    @DisplayName("GET /{id} 接口：通过 PathVariable 与 QueryParam 透传 id 和 customerId 校验")
    void testGetCustomerStatement() {
        CustomerStatementDetailRespVO detailVO = new CustomerStatementDetailRespVO();
        detailVO.setId(100L);
        detailVO.setCustomerId(1001L);
        when(customerStatementService.getPortalCustomerStatement(100L, 1001L)).thenReturn(detailVO);

        CommonResult<CustomerStatementDetailRespVO> res = controller.getCustomerStatement(100L, 1001L);
        assertTrue(res.isSuccess());
        assertEquals(100L, res.getData().getId());
        verify(customerStatementService).getPortalCustomerStatement(100L, 1001L);

        // ID 为空时报 STATEMENT_NOT_EXISTS
        ServiceException ex = assertThrows(ServiceException.class, () ->
                controller.getCustomerStatement(null, 1001L));
        assertEquals(FinanceErrorCodeConstants.STATEMENT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("GET /get 接口：通过 RequestParam 支持按 id 或 no 查询并委托 Service")
    void testGetCustomerStatementByParam() {
        CustomerStatementDetailRespVO detailVO = new CustomerStatementDetailRespVO();
        detailVO.setId(100L);
        detailVO.setStatementNo("CS20260001");
        detailVO.setCustomerId(1001L);
        when(customerStatementService.getPortalCustomerStatement(100L, "CS20260001", 1001L)).thenReturn(detailVO);

        CommonResult<CustomerStatementDetailRespVO> res = controller.getCustomerStatementByParam(100L, "CS20260001", null, 1001L);
        assertTrue(res.isSuccess());
        assertEquals("CS20260001", res.getData().getStatementNo());
        verify(customerStatementService).getPortalCustomerStatement(100L, "CS20260001", 1001L);
    }

    @Test
    @DisplayName("PUT /confirm 接口：正确委托 Service 执行在线确认，自动提取客户端IP")
    void testConfirmCustomerStatement() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195");

        CustomerStatementPortalConfirmReqVO reqVO = new CustomerStatementPortalConfirmReqVO();
        reqVO.setId(100L);
        reqVO.setCustomerId(1001L);
        reqVO.setConfirmByName("张主管");

        CommonResult<Boolean> result = controller.confirmCustomerStatement(reqVO, request);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        assertEquals("203.0.113.195", reqVO.getConfirmIp());
        verify(customerStatementService).confirmPortalCustomerStatement(reqVO);
    }

    @Test
    @DisplayName("PUT /reject 接口：正确委托 Service 执行驳回")
    void testRejectCustomerStatement() {
        CustomerStatementPortalRejectReqVO reqVO = new CustomerStatementPortalRejectReqVO();
        reqVO.setId(100L);
        reqVO.setCustomerId(1001L);
        reqVO.setRejectReason("运费金额有异议");

        CommonResult<Boolean> result = controller.rejectCustomerStatement(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(customerStatementService).rejectPortalCustomerStatement(reqVO);
    }

}
