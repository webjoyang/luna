package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.materialcheck.MaterialCheckService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialCheckControllerTest {

    @Mock
    private MaterialCheckService materialCheckService;

    @InjectMocks
    private MaterialCheckController materialCheckController;

    @Test
    @DisplayName("Controller齐套试算：正确委托 MaterialCheckService 并返回成功结果")
    void testRunMaterialCheck_delegatesToService() {
        MaterialCheckReqVO reqVO = new MaterialCheckReqVO();
        MaterialCheckRespVO expectedResp = new MaterialCheckRespVO();
        when(materialCheckService.calculateMaterialCheck(reqVO)).thenReturn(expectedResp);

        CommonResult<MaterialCheckRespVO> result = materialCheckController.runMaterialCheck(reqVO);

        assertTrue(result.isSuccess());
        assertSame(expectedResp, result.getData());
        verify(materialCheckService).calculateMaterialCheck(reqVO);
    }

    @Test
    @DisplayName("Controller参数为 null 时自动兜底空对象")
    void testRunMaterialCheck_nullReqVO_defaultsToEmpty() {
        MaterialCheckRespVO expectedResp = new MaterialCheckRespVO();
        when(materialCheckService.calculateMaterialCheck(any(MaterialCheckReqVO.class))).thenReturn(expectedResp);

        CommonResult<MaterialCheckRespVO> result = materialCheckController.runMaterialCheck(null);

        assertTrue(result.isSuccess());
        assertSame(expectedResp, result.getData());
        verify(materialCheckService).calculateMaterialCheck(argThat(r -> r != null && r.getWeavingOrderIds() == null));
    }

    @Test
    @DisplayName("权限注解：runMaterialCheck 必须包含 @PreAuthorize('lunaerp:production:material-check:query')")
    void testRunMaterialCheck_permissionAnnotation() throws NoSuchMethodException {
        Method method = MaterialCheckController.class.getMethod("runMaterialCheck", MaterialCheckReqVO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "方法必须标注 @PreAuthorize 注解");
        assertTrue(preAuthorize.value().contains("lunaerp:production:material-check:query"),
                "权限表达式必须包含 lunaerp:production:material-check:query");
    }

}
