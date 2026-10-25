package com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPortalTokenRespVO;
import com.luna.lunasrv.module.lunaerp.portal.service.PortalTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerControllerPortalTokenTest {

    @InjectMocks
    private CustomerController customerController;

    @Mock
    private PortalTokenService portalTokenService;

    private static final Long CUSTOMER_ID = 1001L;

    @Test
    @DisplayName("GET /portal-token: 正确调用 PortalTokenService.getTokenState 并包装 CommonResult")
    void testGetCustomerPortalToken() {
        CustomerPortalTokenRespVO mockVO = CustomerPortalTokenRespVO.builder()
                .exists(true)
                .token("token123")
                .tokenFingerprint("token1")
                .url("https://portal.lunaerp.com/portal/shipments?t=token123")
                .generateTime(LocalDateTime.now())
                .remainingDays(30L)
                .build();
        when(portalTokenService.getTokenState(CUSTOMER_ID)).thenReturn(mockVO);

        CommonResult<CustomerPortalTokenRespVO> result = customerController.getCustomerPortalToken(CUSTOMER_ID);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(mockVO, result.getData());
        verify(portalTokenService).getTokenState(CUSTOMER_ID);
    }

    @Test
    @DisplayName("POST /portal-token/generate (Query Param): 正确调用 PortalTokenService.generateToken")
    void testGenerateCustomerPortalToken_queryParam() {
        CustomerPortalTokenRespVO mockVO = CustomerPortalTokenRespVO.builder()
                .exists(true)
                .token("newToken456")
                .tokenFingerprint("newTok")
                .url("https://portal.lunaerp.com/portal/shipments?t=newToken456")
                .generateTime(LocalDateTime.now())
                .remainingDays(30L)
                .build();
        when(portalTokenService.generateToken(CUSTOMER_ID)).thenReturn(mockVO);

        CommonResult<CustomerPortalTokenRespVO> result = customerController.generateCustomerPortalToken(CUSTOMER_ID, null);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(mockVO, result.getData());
        verify(portalTokenService).generateToken(CUSTOMER_ID);
    }

    @Test
    @DisplayName("POST /portal-token/generate (Request Body): 正确从 Body 读取 customerId")
    void testGenerateCustomerPortalToken_requestBody() {
        CustomerPortalTokenRespVO mockVO = CustomerPortalTokenRespVO.builder()
                .exists(true)
                .token("newToken789")
                .remainingDays(30L)
                .build();
        when(portalTokenService.generateToken(CUSTOMER_ID)).thenReturn(mockVO);

        Map<String, Object> body = Collections.singletonMap("customerId", CUSTOMER_ID);
        CommonResult<CustomerPortalTokenRespVO> result = customerController.generateCustomerPortalToken(null, body);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(mockVO, result.getData());
        verify(portalTokenService).generateToken(CUSTOMER_ID);
    }

    @Test
    @DisplayName("DELETE /portal-token: 正确调用 PortalTokenService.revokeToken 并返回 true")
    void testRevokeCustomerPortalToken() {
        CommonResult<Boolean> result = customerController.revokeCustomerPortalToken(CUSTOMER_ID);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(portalTokenService).revokeToken(CUSTOMER_ID);
    }

    @Test
    @DisplayName("权限守卫：三端点必须显式声明 @PreAuthorize('lunaerp:baseinfo:customer:portal-token')")
    void testPermissionAnnotations() throws NoSuchMethodException {
        String expectedPerm = "@ss.hasPermission('lunaerp:baseinfo:customer:portal-token')";

        Method getMethod = CustomerController.class.getMethod("getCustomerPortalToken", Long.class);
        PreAuthorize getAuth = getMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(getAuth, "getCustomerPortalToken 必须标注 @PreAuthorize");
        assertEquals(expectedPerm, getAuth.value());

        Method postMethod = CustomerController.class.getMethod("generateCustomerPortalToken", Long.class, Map.class);
        PreAuthorize postAuth = postMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(postAuth, "generateCustomerPortalToken 必须标注 @PreAuthorize");
        assertEquals(expectedPerm, postAuth.value());

        Method delMethod = CustomerController.class.getMethod("revokeCustomerPortalToken", Long.class);
        PreAuthorize delAuth = delMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(delAuth, "revokeCustomerPortalToken 必须标注 @PreAuthorize");
        assertEquals(expectedPerm, delAuth.value());
    }

}
