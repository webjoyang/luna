package com.luna.lunasrv.module.lunaerp.portal.controller.portal.auth;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.auth.vo.PortalAuthInfoRespVO;
import com.luna.lunasrv.module.lunaerp.portal.core.PortalContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PortalAuthController 门户认证信息端点测试")
class PortalAuthControllerTest {

    private final PortalAuthController controller = new PortalAuthController();

    @AfterEach
    void tearDown() {
        PortalContext.clear();
    }

    @Test
    @DisplayName("获取当前登录门户客户身份信息成功")
    void testGetPortalAuthInfoSuccess() {
        PortalContext.set(1L, 100L, "杭州纺织实业有限公司");

        CommonResult<PortalAuthInfoRespVO> result = controller.getPortalAuthInfo();

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(100L, result.getData().getCustomerId());
        assertEquals("杭州纺织实业有限公司", result.getData().getCustomerName());
    }
}
