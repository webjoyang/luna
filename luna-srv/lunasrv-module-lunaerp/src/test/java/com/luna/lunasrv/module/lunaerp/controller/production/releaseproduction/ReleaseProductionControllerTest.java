package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseFromContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.releaseproduction.ReleaseProductionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("生产管理 - 下达生产编排 Controller 单测")
class ReleaseProductionControllerTest {

    @Mock
    private ReleaseProductionService releaseProductionService;

    @InjectMocks
    private ReleaseProductionController releaseProductionController;

    @Test
    @DisplayName("getReleaseFromContract: 委托 Service 并返回预填数据")
    void testGetReleaseFromContract() {
        ReleaseFromContractRespVO respVO = ReleaseFromContractRespVO.builder()
                .contractId(100L)
                .contractNo("CH20260001")
                .lines(Collections.emptyList())
                .existingColorInstructions(Collections.emptyList())
                .build();
        when(releaseProductionService.getReleaseFromContract(100L)).thenReturn(respVO);

        CommonResult<ReleaseFromContractRespVO> result = releaseProductionController.getReleaseFromContract(100L);

        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(releaseProductionService).getReleaseFromContract(100L);
    }

    @Test
    @DisplayName("releaseProduction: 委托 Service 并返回生成单据")
    void testReleaseProduction() {
        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.emptyList())
                .build();
        ReleaseProductionRespVO respVO = ReleaseProductionRespVO.builder().build();
        when(releaseProductionService.releaseProduction(reqVO)).thenReturn(respVO);

        CommonResult<ReleaseProductionRespVO> result = releaseProductionController.releaseProduction(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(respVO, result.getData());
        verify(releaseProductionService).releaseProduction(reqVO);
    }

    @Test
    @DisplayName("createFinishedInspection: 委托 Service 并返回生成的检验单ID")
    void testCreateFinishedInspection() {
        when(releaseProductionService.createFinishedInspection(1024L)).thenReturn(888L);

        CommonResult<Long> result = releaseProductionController.createFinishedInspection(1024L);

        assertTrue(result.isSuccess());
        assertEquals(888L, result.getData());
        verify(releaseProductionService).createFinishedInspection(1024L);
    }

    @Test
    @DisplayName("createFinishedInspection: 校验端点注解与权限表达式")
    void testCreateFinishedInspection_annotations() throws NoSuchMethodException {
        java.lang.reflect.Method method = ReleaseProductionController.class.getMethod("createFinishedInspection", Long.class);
        org.springframework.web.bind.annotation.PostMapping postMapping = method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
        org.junit.jupiter.api.Assertions.assertNotNull(postMapping);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"/create-finished-inspection"}, postMapping.value());

        org.springframework.security.access.prepost.PreAuthorize preAuthorize = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        org.junit.jupiter.api.Assertions.assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('lunaerp:production:release-production:release')", preAuthorize.value());
    }

}
