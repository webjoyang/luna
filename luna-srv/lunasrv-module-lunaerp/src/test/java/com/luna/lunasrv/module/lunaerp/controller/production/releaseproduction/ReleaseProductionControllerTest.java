package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseFromContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.releaseproduction.ReleaseProductionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReleaseProductionControllerTest {

    @Mock
    private ReleaseProductionService releaseProductionService;

    @InjectMocks
    private ReleaseProductionController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("getReleaseFromContract: 调用 service 并返回结果")
    void testGetReleaseFromContract() {
        Long contractId = 100L;
        ReleaseFromContractRespVO respVO = ReleaseFromContractRespVO.builder()
                .contractId(contractId)
                .contractNo("CH20260001")
                .lines(Collections.emptyList())
                .build();
        when(releaseProductionService.getReleaseFromContract(contractId)).thenReturn(respVO);

        CommonResult<ReleaseFromContractRespVO> result = controller.getReleaseFromContract(contractId);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(contractId, result.getData().getContractId());
        verify(releaseProductionService).getReleaseFromContract(contractId);
    }

    @Test
    @DisplayName("releaseProduction: 调用 service 并返回结果")
    void testReleaseProduction() {
        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(Collections.emptyList())
                .build();

        ReleaseProductionRespVO respVO = ReleaseProductionRespVO.builder().build();
        when(releaseProductionService.releaseProduction(reqVO)).thenReturn(respVO);

        CommonResult<ReleaseProductionRespVO> result = controller.releaseProduction(reqVO);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(releaseProductionService).releaseProduction(reqVO);
    }

    @Test
    @DisplayName("createFinishedInspection: 调用 service 并返回生成的检验单 ID")
    void testCreateFinishedInspection() {
        Long dyeingOrderId = 55L;
        Long expectedInspectionId = 88L;
        when(releaseProductionService.createFinishedInspection(dyeingOrderId)).thenReturn(expectedInspectionId);

        CommonResult<Long> result = controller.createFinishedInspection(dyeingOrderId);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(expectedInspectionId, result.getData());
        verify(releaseProductionService).createFinishedInspection(dyeingOrderId);
    }

}
