package com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentDetailRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentPageItemRespVO;
import com.luna.lunasrv.module.lunaerp.portal.controller.portal.shipment.vo.PortalShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.service.portal.PortalShipmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalShipmentController 门户发货单控制器测试")
public class PortalShipmentControllerTest {

    @InjectMocks
    private PortalShipmentController controller;

    @Mock
    private PortalShipmentService portalShipmentService;

    @Test
    @DisplayName("分页端点正常委托 Service 并返回 CommonResult")
    void testGetShipmentPage() {
        PortalShipmentPageReqVO reqVO = new PortalShipmentPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PortalShipmentPageItemRespVO item = PortalShipmentPageItemRespVO.builder()
                .id(101L)
                .shipmentNo("SH-20261011-001")
                .shippingDate(LocalDate.now())
                .signStatus("PENDING")
                .totalAmount(BigDecimal.TEN)
                .freightFee(BigDecimal.ONE)
                .build();
        PageResult<PortalShipmentPageItemRespVO> pageResult = new PageResult<>(List.of(item), 1L);
        when(portalShipmentService.getShipmentPage(reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<PortalShipmentPageItemRespVO>> result = controller.getShipmentPage(reqVO);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().getList().size());
        assertEquals("SH-20261011-001", result.getData().getList().get(0).getShipmentNo());
    }

    @Test
    @DisplayName("详情端点正常委托 Service 并返回 CommonResult")
    void testGetShipmentDetail() {
        Long id = 101L;
        String no = "SH-20261011-001";
        PortalShipmentDetailRespVO detail = PortalShipmentDetailRespVO.builder()
                .id(id)
                .shipmentNo(no)
                .signStatus("PENDING")
                .totalAmount(BigDecimal.TEN)
                .freightFee(BigDecimal.ONE)
                .items(Collections.emptyList())
                .build();
        when(portalShipmentService.getShipmentDetail(id, no)).thenReturn(detail);

        CommonResult<PortalShipmentDetailRespVO> result = controller.getShipmentDetail(id, no);

        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(id, result.getData().getId());
        assertEquals(no, result.getData().getShipmentNo());
    }

}
