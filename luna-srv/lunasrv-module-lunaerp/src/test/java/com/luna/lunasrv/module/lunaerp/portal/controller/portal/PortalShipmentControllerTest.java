package com.luna.lunasrv.module.lunaerp.portal.controller.portal;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.vo.PortalShipmentRespVO;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalShipmentService;
import jakarta.annotation.security.PermitAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 客户门户发货单控制器单元测试 (契约路由与免登录审查)
 */
@ExtendWith(MockitoExtension.class)
public class PortalShipmentControllerTest {

    @Mock
    private PortalShipmentService portalShipmentService;

    @InjectMocks
    private PortalShipmentController controller;

    @Test
    @DisplayName("规约审查：控制器必须标注 @PermitAll 免密访问，且映射单一标准路径 /app-api/lunaerp/portal/shipment")
    public void testControllerAnnotationsAndRoute() {
        PermitAll permitAll = PortalShipmentController.class.getAnnotation(PermitAll.class);
        assertNotNull(permitAll, "PortalShipmentController 必须标注 @PermitAll");

        RestController restController = PortalShipmentController.class.getAnnotation(RestController.class);
        assertNotNull(restController, "PortalShipmentController 必须标注 @RestController");

        Validated validated = PortalShipmentController.class.getAnnotation(Validated.class);
        assertNotNull(validated, "PortalShipmentController 必须标注 @Validated");

        RequestMapping requestMapping = PortalShipmentController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "PortalShipmentController 必须标注 @RequestMapping");
        String[] paths = requestMapping.value().length > 0 ? requestMapping.value() : requestMapping.path();
        assertEquals(1, paths.length, "必须遵循单一标准路径，禁止多重路径别名");
        assertEquals("/app-api/lunaerp/portal/shipment", paths[0]);
    }

    @Test
    @DisplayName("端点契约：/page 与 /get 必须为 GET 端点")
    public void testEndpointHttpMethods() throws Exception {
        Method pageMethod = PortalShipmentController.class.getMethod("getPortalShipmentPage", PortalShipmentPageReqVO.class);
        assertTrue(pageMethod.isAnnotationPresent(GetMapping.class));
        assertArrayEquals(new String[]{"/page"}, pageMethod.getAnnotation(GetMapping.class).value());

        Method getMethod = PortalShipmentController.class.getMethod("getPortalShipment", Long.class, String.class, String.class);
        assertTrue(getMethod.isAnnotationPresent(GetMapping.class));
        assertArrayEquals(new String[]{"/get"}, getMethod.getAnnotation(GetMapping.class).value());
    }

    @Test
    @DisplayName("控制器透传：分页接口正常透传至 service")
    public void testGetPortalShipmentPage_passthrough() {
        PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
        reqVO.setSignStatus("PENDING");
        PageResult<PortalShipmentRespVO> expected = new PageResult<>(Collections.emptyList(), 0L);
        when(portalShipmentService.getPortalShipmentPage(reqVO)).thenReturn(expected);

        CommonResult<PageResult<PortalShipmentRespVO>> result = controller.getPortalShipmentPage(reqVO);
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertSame(expected, result.getData());
        verify(portalShipmentService).getPortalShipmentPage(reqVO);
    }

    @Test
    @DisplayName("控制器透传：详情接口（id形态与?no=形态）正常透传至 service")
    public void testGetPortalShipment_passthrough() {
        PortalShipmentRespVO expected = new PortalShipmentRespVO();
        expected.setId(10L);
        expected.setShipmentNo("SH-20260927-001");

        // 1. id 形态
        when(portalShipmentService.getPortalShipment(10L, null)).thenReturn(expected);
        CommonResult<PortalShipmentRespVO> res1 = controller.getPortalShipment(10L, null, null);
        assertEquals(0, res1.getCode());
        assertSame(expected, res1.getData());

        // 2. ?no= 形态
        when(portalShipmentService.getPortalShipment(null, "SH-20260927-001")).thenReturn(expected);
        CommonResult<PortalShipmentRespVO> res2 = controller.getPortalShipment(null, "SH-20260927-001", null);
        assertEquals(0, res2.getCode());
        assertSame(expected, res2.getData());
    }
}
