package com.luna.lunasrv.module.lunaerp.service.production.releaseproduction;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseFromContractRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import com.luna.lunasrv.module.lunaerp.service.production.dyeingorder.DyeingOrderService;
import com.luna.lunasrv.module.lunaerp.service.production.greigeorder.GreigeOrderService;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderService;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingColorRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderRespVO;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.lang.reflect.Method;
import java.time.LocalDate;

import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("生产管理 - 下达生产编排服务单测")
class ReleaseProductionServiceImplTest {

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesContractDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesContractItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ColorInstructionDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), InspectionOrderDO.class);
    }

    @InjectMocks
    private ReleaseProductionServiceImpl releaseProductionService;

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private SalesContractItemMapper salesContractItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private GreigeOrderService greigeOrderService;

    @Mock
    private DyeingOrderService dyeingOrderService;

    @Mock
    private InspectionOrderService inspectionOrderService;

    @Mock
    private InspectionOrderMapper inspectionOrderMapper;

    @Mock
    private ColorInstructionService colorInstructionService;

    @Mock
    private ColorInstructionMapper colorInstructionMapper;

    @Test
    @DisplayName("from-contract: 合同不存在时报 1_030_035_000")
    void getReleaseFromContract_contractNotExists_throws1030035000() {
        when(salesContractMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.getReleaseFromContract(999L));
        assertEquals(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS.getCode(), ex.getCode());
        assertEquals(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS.getMsg(), ex.getMessage());
    }

    @Test
    @DisplayName("from-contract: 正常返回合同行剩余量与客户已有打色单")
    void getReleaseFromContract_success_calculatesRemainingQtyAndReturnsExistingColorInstructions() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setContractNo("CH20260001");
        contract.setCustomerId(200L);
        contract.setClientName("优衣纺织");
        contract.setStatus("ACTIVE");
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO line1 = new SalesContractItemDO();
        line1.setId(1L);
        line1.setContractId(100L);
        line1.setLineNo(1);
        line1.setProductCode("P001");
        line1.setProductName("全棉汗布");
        line1.setCustomerColor("C101");
        line1.setComposition("100%棉");
        line1.setWidth("180cm");
        line1.setWeight("200g");
        line1.setQuantity(new BigDecimal("1000.00"));
        line1.setReleasedQty(new BigDecimal("400.00"));

        SalesContractItemDO line2 = new SalesContractItemDO();
        line2.setId(2L);
        line2.setContractId(100L);
        line2.setLineNo(2);
        line2.setProductCode("P001");
        line2.setProductName("全棉汗布");
        line2.setCustomerColor("C102");
        line2.setQuantity(new BigDecimal("500.00"));
        line2.setReleasedQty(null); // null 缺省按 0

        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Arrays.asList(line1, line2));

        ColorInstructionDO ci = new ColorInstructionDO();
        ci.setId(5L);
        ci.setInstructionNo("DS20261007001");
        ci.setStatus("CONFIRMED");
        ci.setProductCode("P001");
        ci.setProductName("全棉汗布");
        when(colorInstructionMapper.selectList(any())).thenReturn(Collections.singletonList(ci));

        ReleaseFromContractRespVO resp = releaseProductionService.getReleaseFromContract(100L);
        assertNotNull(resp);
        assertEquals(100L, resp.getContractId());
        assertEquals("CH20260001", resp.getContractNo());
        assertEquals(200L, resp.getCustomerId());
        assertEquals("优衣纺织", resp.getClientName());

        assertEquals(2, resp.getLines().size());
        ReleaseFromContractRespVO.Line respLine1 = resp.getLines().get(0);
        assertEquals(1L, respLine1.getContractLineId());
        assertEquals(1, respLine1.getLineNo());
        assertEquals(new BigDecimal("1000.00"), respLine1.getContractQty());
        assertEquals(new BigDecimal("400.00"), respLine1.getReleasedQty());
        assertEquals(new BigDecimal("600.00"), respLine1.getRemainingQty());
        assertEquals(BigDecimal.ZERO, respLine1.getDefaultLossRate());

        ReleaseFromContractRespVO.Line respLine2 = resp.getLines().get(1);
        assertEquals(2L, respLine2.getContractLineId());
        assertEquals(2, respLine2.getLineNo());
        assertEquals(new BigDecimal("500.00"), respLine2.getContractQty());
        assertEquals(BigDecimal.ZERO, respLine2.getReleasedQty());
        assertEquals(new BigDecimal("500.00"), respLine2.getRemainingQty());

        assertEquals(1, resp.getExistingColorInstructions().size());
        ReleaseFromContractRespVO.ExistingColorInstruction respCi = resp.getExistingColorInstructions().get(0);
        assertEquals(5L, respCi.getId());
        assertEquals("DS20261007001", respCi.getInstructionNo());
        assertEquals("CONFIRMED", respCi.getStatus());
        assertEquals("P001", respCi.getProductCode());
    }

    // ==================== releaseProduction 异常分支守卫 ====================

    @Test
    @DisplayName("release: 合同不存在报 1_030_035_000")
    void releaseProduction_contractNotExists_throws1030035000() {
        when(salesContractMapper.selectById(100L)).thenReturn(null);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .releaseQty(new BigDecimal("100.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("release: 合同已取消报 1_030_035_001")
    void releaseProduction_contractCancelled_throws1030035001() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setStatus("CANCELLED");
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .releaseQty(new BigDecimal("100.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(RELEASE_PRODUCTION_CONTRACT_CANCELLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("release: 合同行不属于该合同时被拦截")
    void releaseProduction_lineNotBelongToContract_throwsException() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setStatus("ACTIVE");
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setContractId(100L);
        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Collections.singletonList(item1));

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(999L) // 不属于合同 100
                                .releaseQty(new BigDecimal("100.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("release: 并发或超下达导致条件更新影响行数为 0 报 1_030_035_002")
    void releaseProduction_overReleaseOrConcurrentConflict_throws1030035002() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setStatus("ACTIVE");
        contract.setTenantId(1L);
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setLineNo(1);
        item1.setContractId(100L);
        item1.setQuantity(new BigDecimal("100.00"));
        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Collections.singletonList(item1));

        when(salesContractItemMapper.updateReleasedQty(eq(1L), eq(new BigDecimal("60.00")), any())).thenReturn(0);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .colorInstructionMode("NONE")
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .releaseQty(new BigDecimal("60.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(RELEASE_PRODUCTION_OVER_RELEASE.getCode(), ex.getCode());
        verify(greigeOrderService, never()).createGreigeOrder(any());
        verify(dyeingOrderService, never()).createDyeingOrder(any());
        verify(inspectionOrderService, never()).createInspectionOrder(any());
    }

    @Test
    @DisplayName("release: EXISTING 模式打色单为 PENDING 时报 1_030_035_003")
    void releaseProduction_existingColorInstruction_pendingStatus_throws1030035003() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setStatus("ACTIVE");
        contract.setCustomerId(200L);
        contract.setTenantId(1L);
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setLineNo(1);
        item1.setContractId(100L);
        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Collections.singletonList(item1));
        when(salesContractItemMapper.updateReleasedQty(eq(1L), any(), any())).thenReturn(1);

        when(colorInstructionService.validateColorInstructionForDownstream(eq(5L), eq(200L), any(), eq("P001"), eq(RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED)))
                .thenThrow(new ServiceException(RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(),
                        RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getMsg()));

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .colorInstructionMode("EXISTING")
                        .existingColorInstructionId(5L)
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .releaseQty(new BigDecimal("60.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("release: NEW 模式主灯源缺失报 1_030_035_004")
    void releaseProduction_newColorInstruction_missingLightPrimary_throws1030035004() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setStatus("ACTIVE");
        contract.setCustomerId(200L);
        contract.setTenantId(1L);
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setLineNo(1);
        item1.setContractId(100L);
        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Collections.singletonList(item1));
        when(salesContractItemMapper.updateReleasedQty(eq(1L), any(), any())).thenReturn(1);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .colorInstructionMode("NEW")
                        .newColorInstruction(ReleaseProductionReqVO.NewColorInstruction.builder()
                                .colors(Collections.singletonList(ReleaseProductionReqVO.ColorLine.builder()
                                        .contractLineId(1L)
                                        .customerColorNo("C101")
                                        .lightPrimary("") // 缺失主灯源
                                        .build()))
                                .build())
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .releaseQty(new BigDecimal("60.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(RELEASE_PRODUCTION_LIGHT_PRIMARY_REQUIRED.getCode(), ex.getCode());
    }

    // ==================== releaseProduction 正常编排 ====================

    @Test
    @DisplayName("release: NONE 模式单行创建胚布单、染色单、检验单，不引用打色单")
    void releaseProduction_noneMode_singleLine_success() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setContractNo("CH20260001");
        contract.setStatus("ACTIVE");
        contract.setCustomerId(200L);
        contract.setClientName("优衣纺织");
        contract.setTenantId(1L);
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setLineNo(1);
        item1.setContractId(100L);
        item1.setProductCode("P001");
        item1.setProductName("全棉汗布");
        item1.setQuantity(new BigDecimal("1000.00"));
        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Collections.singletonList(item1));
        when(salesContractItemMapper.updateReleasedQty(eq(1L), eq(new BigDecimal("600.00")), eq(1L))).thenReturn(1);

        ProductDO productDO = new ProductDO();
        productDO.setId(101L);
        productDO.setCode("P001");
        when(productMapper.selectByCode("P001")).thenReturn(productDO);

        when(greigeOrderService.createGreigeOrder(any())).thenReturn(11L);
        GreigeOrderRespVO greigeOrderResp = new GreigeOrderRespVO();
        greigeOrderResp.setId(11L);
        greigeOrderResp.setOrderNo("PB1007001");
        when(greigeOrderService.getGreigeOrder(11L)).thenReturn(greigeOrderResp);

        when(dyeingOrderService.createDyeingOrder(any())).thenReturn(21L);

        when(inspectionOrderService.createInspectionOrder(any())).thenReturn(31L);
        InspectionOrderRespVO inspectionOrderResp = new InspectionOrderRespVO();
        inspectionOrderResp.setId(31L);
        inspectionOrderResp.setCode("INS-20261007-001");
        inspectionOrderResp.setType("GREIGE");
        when(inspectionOrderService.getInspectionOrder(31L)).thenReturn(inspectionOrderResp);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .productName("全棉汗布")
                        .composition("100%棉")
                        .width("180cm")
                        .gsm("200g")
                        .colorInstructionMode("NONE")
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .customerColor("C101")
                                .releaseQty(new BigDecimal("600.00"))
                                .lossRate(new BigDecimal("0.05"))
                                .build()))
                        .build()))
                .build();

        ReleaseProductionRespVO resp = releaseProductionService.releaseProduction(reqVO);
        assertNotNull(resp);
        assertEquals(1, resp.getGreigeOrders().size());
        assertEquals(11L, resp.getGreigeOrders().get(0).getId());
        assertEquals("PB1007001", resp.getGreigeOrders().get(0).getOrderNo());

        assertEquals(1, resp.getDyeingOrders().size());
        assertEquals(21L, resp.getDyeingOrders().get(0).getId());

        assertEquals(1, resp.getInspectionOrders().size());
        assertEquals(31L, resp.getInspectionOrders().get(0).getId());
        assertEquals("INS-20261007-001", resp.getInspectionOrders().get(0).getCode());
        assertEquals("GREIGE", resp.getInspectionOrders().get(0).getType());

        assertTrue(resp.getColorInstructions().isEmpty());

        // 验证胚布单创建参数
        ArgumentCaptor<GreigeOrderSaveReqVO> greigeCaptor = ArgumentCaptor.forClass(GreigeOrderSaveReqVO.class);
        verify(greigeOrderService).createGreigeOrder(greigeCaptor.capture());
        GreigeOrderSaveReqVO greigeReq = greigeCaptor.getValue();
        assertEquals(100L, greigeReq.getSalesContractId());
        assertEquals("CH20260001", greigeReq.getContractNo());
        assertEquals(456L, greigeReq.getGreigeFactoryId());
        assertEquals(789L, greigeReq.getDyeingFactoryId());
        assertEquals("ACTIVE", greigeReq.getStatus());
        assertNull(greigeReq.getColorInstructionId());
        assertEquals(1, greigeReq.getColors().size());
        assertEquals("C101", greigeReq.getColors().get(0).getColor());
        assertEquals(new BigDecimal("600.00"), greigeReq.getColors().get(0).getFinishedQty());
        assertEquals(1L, greigeReq.getColors().get(0).getContractLineId());
        assertEquals(1, greigeReq.getColors().get(0).getContractLineNo());

        // 验证染色单创建参数
        ArgumentCaptor<DyeingOrderSaveReqVO> dyeingCaptor = ArgumentCaptor.forClass(DyeingOrderSaveReqVO.class);
        verify(dyeingOrderService).createDyeingOrder(dyeingCaptor.capture());
        DyeingOrderSaveReqVO dyeingReq = dyeingCaptor.getValue();
        assertEquals(100L, dyeingReq.getSalesContractId());
        assertEquals("CH20260001", dyeingReq.getContractNo());
        assertEquals(200L, dyeingReq.getCustomerId());
        assertEquals("优衣纺织", dyeingReq.getClientName());
        assertEquals(789L, dyeingReq.getDyeingFactoryId());
        assertEquals("ACTIVE", dyeingReq.getStatus());
        assertNull(dyeingReq.getColorInstructionId());
        assertEquals(1, dyeingReq.getColors().size());
        assertEquals("C101", dyeingReq.getColors().get(0).getCustomerColorNo());
        assertEquals(new BigDecimal("600.00"), dyeingReq.getColors().get(0).getQuantity());
        assertEquals(new BigDecimal("1000.00"), dyeingReq.getColors().get(0).getContractQty());
        assertEquals(1L, dyeingReq.getColors().get(0).getContractLineId());
        assertEquals(1, dyeingReq.getColors().get(0).getContractLineNo());

        // 验证检验单创建参数
        ArgumentCaptor<InspectionOrderSaveReqVO> inspectionCaptor = ArgumentCaptor.forClass(InspectionOrderSaveReqVO.class);
        verify(inspectionOrderService).createInspectionOrder(inspectionCaptor.capture());
        InspectionOrderSaveReqVO inspectionReq = inspectionCaptor.getValue();
        assertEquals("GREIGE", inspectionReq.getType());
        assertEquals("DRAFT", inspectionReq.getStatus());
        assertEquals(101L, inspectionReq.getProductId());
        assertEquals(11L, inspectionReq.getGreigeOrderId());
        assertEquals(100L, inspectionReq.getSalesContractId());
        assertEquals(new BigDecimal("600.00"), inspectionReq.getTargetWeight());
    }

    @Test
    @DisplayName("release: EXISTING 模式成功引用打色单，胚布单与染色单带入引用")
    void releaseProduction_existingMode_confirmed_success() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setContractNo("CH20260001");
        contract.setStatus("ACTIVE");
        contract.setCustomerId(200L);
        contract.setTenantId(1L);
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setLineNo(1);
        item1.setContractId(100L);
        item1.setProductCode("P001");
        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Collections.singletonList(item1));
        when(salesContractItemMapper.updateReleasedQty(eq(1L), any(), any())).thenReturn(1);

        ColorInstructionDO ci = new ColorInstructionDO();
        ci.setId(5L);
        ci.setInstructionNo("DS20261007001");
        ci.setStatus("CONFIRMED");
        ci.setCustomerId(200L);
        ci.setProductCode("P001");
        when(colorInstructionService.validateColorInstructionForDownstream(eq(5L), eq(200L), any(), eq("P001"), any()))
                .thenReturn(ci);

        when(greigeOrderService.createGreigeOrder(any())).thenReturn(11L);
        when(dyeingOrderService.createDyeingOrder(any())).thenReturn(21L);
        when(inspectionOrderService.createInspectionOrder(any())).thenReturn(31L);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .colorInstructionMode("EXISTING")
                        .existingColorInstructionId(5L)
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .customerColor("C101")
                                .releaseQty(new BigDecimal("500.00"))
                                .build()))
                        .build()))
                .build();

        ReleaseProductionRespVO resp = releaseProductionService.releaseProduction(reqVO);
        assertNotNull(resp);

        ArgumentCaptor<GreigeOrderSaveReqVO> greigeCaptor = ArgumentCaptor.forClass(GreigeOrderSaveReqVO.class);
        verify(greigeOrderService).createGreigeOrder(greigeCaptor.capture());
        assertEquals(5L, greigeCaptor.getValue().getColorInstructionId());
        assertEquals("DS20261007001", greigeCaptor.getValue().getColorInstructionNo());

        ArgumentCaptor<DyeingOrderSaveReqVO> dyeingCaptor = ArgumentCaptor.forClass(DyeingOrderSaveReqVO.class);
        verify(dyeingOrderService).createDyeingOrder(dyeingCaptor.capture());
        assertEquals(5L, dyeingCaptor.getValue().getColorInstructionId());
        assertEquals("DS20261007001", dyeingCaptor.getValue().getColorInstructionNo());
    }

    @Test
    @DisplayName("release: NEW 模式创建 PENDING 打色单，胚布单与染色单不引用")
    void releaseProduction_newMode_validLightPrimary_success() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setContractNo("CH20260001");
        contract.setStatus("ACTIVE");
        contract.setCustomerId(200L);
        contract.setTenantId(1L);
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setLineNo(1);
        item1.setContractId(100L);
        item1.setProductCode("P001");
        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Collections.singletonList(item1));
        when(salesContractItemMapper.updateReleasedQty(eq(1L), any(), any())).thenReturn(1);

        ProductDO productDO = new ProductDO();
        productDO.setId(101L);
        productDO.setCode("P001");
        when(productMapper.selectByCode("P001")).thenReturn(productDO);

        when(colorInstructionService.createColorInstruction(any())).thenReturn(88L);
        ColorInstructionDO ciDO = new ColorInstructionDO();
        ciDO.setId(88L);
        ciDO.setInstructionNo("DS20261014088");
        when(colorInstructionMapper.selectById(88L)).thenReturn(ciDO);
        when(greigeOrderService.createGreigeOrder(any())).thenReturn(11L);
        when(dyeingOrderService.createDyeingOrder(any())).thenReturn(21L);
        when(inspectionOrderService.createInspectionOrder(any())).thenReturn(31L);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .colorInstructionMode("NEW")
                        .newColorInstruction(ReleaseProductionReqVO.NewColorInstruction.builder()
                                .labFactoryId(3001L)
                                .colors(Collections.singletonList(ReleaseProductionReqVO.ColorLine.builder()
                                         .contractLineId(1L)
                                        .customerColorNo("C101")
                                        .colorName("藏青")
                                        .lightPrimary("D65")
                                        .lightSecondary("TL84")
                                        .build()))
                                .build())
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .customerColor("C101")
                                .releaseQty(new BigDecimal("500.00"))
                                .build()))
                        .build()))
                .build();

        ReleaseProductionRespVO resp = releaseProductionService.releaseProduction(reqVO);
        assertNotNull(resp);
        assertEquals(1, resp.getColorInstructions().size());
        assertEquals(88L, resp.getColorInstructions().get(0).getId());
        assertEquals("DS20261014088", resp.getColorInstructions().get(0).getInstructionNo());

        // 验证打色单参数
        ArgumentCaptor<ColorInstructionSaveReqVO> ciCaptor = ArgumentCaptor.forClass(ColorInstructionSaveReqVO.class);
        verify(colorInstructionService).createColorInstruction(ciCaptor.capture());
        ColorInstructionSaveReqVO ciReq = ciCaptor.getValue();
        assertEquals(200L, ciReq.getCustomerId());
        assertEquals(101L, ciReq.getProductId());
        assertEquals(3001L, ciReq.getLabFactoryId());
        assertEquals(1, ciReq.getColors().size());
        assertEquals("C101", ciReq.getColors().get(0).getCustomerColorNo());
        assertEquals("D65", ciReq.getColors().get(0).getLightPrimary());
        assertEquals("TL84", ciReq.getColors().get(0).getLightSecondary());

        // 验证胚布单与染色单不引用新建的打色单（零回写）
        ArgumentCaptor<GreigeOrderSaveReqVO> greigeCaptor = ArgumentCaptor.forClass(GreigeOrderSaveReqVO.class);
        verify(greigeOrderService).createGreigeOrder(greigeCaptor.capture());
        assertNull(greigeCaptor.getValue().getColorInstructionId());

        ArgumentCaptor<DyeingOrderSaveReqVO> dyeingCaptor = ArgumentCaptor.forClass(DyeingOrderSaveReqVO.class);
        verify(dyeingOrderService).createDyeingOrder(dyeingCaptor.capture());
        assertNull(dyeingCaptor.getValue().getColorInstructionId());
    }

    @Test
    @DisplayName("release: 同产品多行合并到同一张胚布单与染色单（多颜色行），检验单目标重量为组内合计")
    void releaseProduction_multipleLinesSameProduct_consolidated() {
        SalesContractDO contract = new SalesContractDO();
        contract.setId(100L);
        contract.setContractNo("CH20260001");
        contract.setStatus("ACTIVE");
        contract.setCustomerId(200L);
        contract.setTenantId(1L);
        when(salesContractMapper.selectById(100L)).thenReturn(contract);

        SalesContractItemDO item1 = new SalesContractItemDO();
        item1.setId(1L);
        item1.setLineNo(1);
        item1.setContractId(100L);
        item1.setProductCode("P001");
        item1.setQuantity(new BigDecimal("1000.00"));

        SalesContractItemDO item2 = new SalesContractItemDO();
        item2.setId(2L);
        item2.setLineNo(2);
        item2.setContractId(100L);
        item2.setProductCode("P001");
        item2.setQuantity(new BigDecimal("800.00"));

        when(salesContractItemMapper.selectListByContractId(100L)).thenReturn(Arrays.asList(item1, item2));
        when(salesContractItemMapper.updateReleasedQty(eq(1L), eq(new BigDecimal("600.00")), any())).thenReturn(1);
        when(salesContractItemMapper.updateReleasedQty(eq(2L), eq(new BigDecimal("400.00")), any())).thenReturn(1);

        ProductDO productDO = new ProductDO();
        productDO.setId(101L);
        productDO.setCode("P001");
        when(productMapper.selectByCode("P001")).thenReturn(productDO);

        when(greigeOrderService.createGreigeOrder(any())).thenReturn(11L);
        when(dyeingOrderService.createDyeingOrder(any())).thenReturn(21L);
        when(inspectionOrderService.createInspectionOrder(any())).thenReturn(31L);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(100L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .productName("全棉汗布")
                        .colorInstructionMode("NONE")
                        .lines(Arrays.asList(
                                ReleaseProductionReqVO.Line.builder()
                                        .contractLineId(1L)
                                        .customerColor("C101")
                                        .releaseQty(new BigDecimal("600.00"))
                                        .lossRate(new BigDecimal("0.05"))
                                        .build(),
                                ReleaseProductionReqVO.Line.builder()
                                        .contractLineId(2L)
                                        .customerColor("C102")
                                        .releaseQty(new BigDecimal("400.00"))
                                        .lossRate(new BigDecimal("0.08"))
                                        .build()))
                        .build()))
                .build();

        ReleaseProductionRespVO resp = releaseProductionService.releaseProduction(reqVO);
        assertNotNull(resp);
        assertEquals(1, resp.getGreigeOrders().size());
        assertEquals(1, resp.getDyeingOrders().size());
        assertEquals(1, resp.getInspectionOrders().size());

        // 验证胚布单：2 个颜色行，主单 suggestedLoss = 0.08（组内最大），plannedQty = 1000
        ArgumentCaptor<GreigeOrderSaveReqVO> greigeCaptor = ArgumentCaptor.forClass(GreigeOrderSaveReqVO.class);
        verify(greigeOrderService).createGreigeOrder(greigeCaptor.capture());
        GreigeOrderSaveReqVO greigeReq = greigeCaptor.getValue();
        assertEquals(new BigDecimal("1000.00"), greigeReq.getPlannedQty());
        assertEquals(new BigDecimal("0.08"), greigeReq.getSuggestedLoss());
        assertEquals(2, greigeReq.getColors().size());
        assertEquals("C101", greigeReq.getColors().get(0).getColor());
        assertEquals(new BigDecimal("600.00"), greigeReq.getColors().get(0).getFinishedQty());
        assertEquals(1L, greigeReq.getColors().get(0).getContractLineId());
        assertEquals(1, greigeReq.getColors().get(0).getContractLineNo());
        assertEquals("C102", greigeReq.getColors().get(1).getColor());
        assertEquals(new BigDecimal("400.00"), greigeReq.getColors().get(1).getFinishedQty());
        assertEquals(2L, greigeReq.getColors().get(1).getContractLineId());
        assertEquals(2, greigeReq.getColors().get(1).getContractLineNo());

        // 验证染色单：2 个颜色行
        ArgumentCaptor<DyeingOrderSaveReqVO> dyeingCaptor = ArgumentCaptor.forClass(DyeingOrderSaveReqVO.class);
        verify(dyeingOrderService).createDyeingOrder(dyeingCaptor.capture());
        DyeingOrderSaveReqVO dyeingReq = dyeingCaptor.getValue();
        assertEquals(2, dyeingReq.getColors().size());
        assertEquals("C101", dyeingReq.getColors().get(0).getCustomerColorNo());
        assertEquals(new BigDecimal("600.00"), dyeingReq.getColors().get(0).getQuantity());
        assertEquals(1L, dyeingReq.getColors().get(0).getContractLineId());
        assertEquals("C102", dyeingReq.getColors().get(1).getCustomerColorNo());
        assertEquals(new BigDecimal("400.00"), dyeingReq.getColors().get(1).getQuantity());
        assertEquals(2L, dyeingReq.getColors().get(1).getContractLineId());

        // 验证检验单：targetWeight = 1000.00
        ArgumentCaptor<InspectionOrderSaveReqVO> inspectionCaptor = ArgumentCaptor.forClass(InspectionOrderSaveReqVO.class);
        verify(inspectionOrderService).createInspectionOrder(inspectionCaptor.capture());
        assertEquals(new BigDecimal("1000.00"), inspectionCaptor.getValue().getTargetWeight());
    }

    @Test
    @DisplayName("release: 租户隔离测试，跨租户合同访问（selectById 为 null）被拦截并报 1_030_035_000")
    void releaseProduction_tenantIsolation_crossTenantContractBlocked() {
        when(salesContractMapper.selectById(888L)).thenReturn(null);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(888L)
                .greigeFactoryId(456L)
                .dyeingFactoryId(789L)
                .groups(Collections.singletonList(ReleaseProductionReqVO.Group.builder()
                        .productCode("P001")
                        .lines(Collections.singletonList(ReleaseProductionReqVO.Line.builder()
                                .contractLineId(1L)
                                .releaseQty(new BigDecimal("100.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFinishedInspection: 染色单ID为 null 时报 1_030_007_000 (DYEING_ORDER_NOT_EXISTS)")
    void createFinishedInspection_dyeingOrderIdNull_throws1030007000() {
        when(dyeingOrderService.getDyeingOrder(null)).thenThrow(new ServiceException(DYEING_ORDER_NOT_EXISTS.getCode(), DYEING_ORDER_NOT_EXISTS.getMsg()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.createFinishedInspection(null));
        assertEquals(DYEING_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFinishedInspection: 染色单不存在时报 1_030_007_000 (DYEING_ORDER_NOT_EXISTS)")
    void createFinishedInspection_dyeingOrderNotExists_throws1030007000() {
        when(dyeingOrderService.getDyeingOrder(999L)).thenThrow(new ServiceException(DYEING_ORDER_NOT_EXISTS.getCode(), DYEING_ORDER_NOT_EXISTS.getMsg()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.createFinishedInspection(999L));
        assertEquals(DYEING_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFinishedInspection: 染色单非 COMPLETED 状态时报 1_030_007_001 (DYEING_ORDER_STATUS_NOT_ALLOWED)")
    void createFinishedInspection_dyeingOrderNotCompleted_throws1030007001() {
        for (String invalidStatus : Arrays.asList("DRAFT", "ACTIVE", "CANCELLED")) {
            DyeingOrderRespVO order = new DyeingOrderRespVO();
            order.setId(100L);
            order.setStatus(invalidStatus);
            when(dyeingOrderService.getDyeingOrder(100L)).thenReturn(order);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> releaseProductionService.createFinishedInspection(100L));
            assertEquals(DYEING_ORDER_STATUS_NOT_ALLOWED.getCode(), ex.getCode(), "非 COMPLETED 状态应拦截: " + invalidStatus);
        }
    }

    @Test
    @DisplayName("createFinishedInspection: 已存在相同染色单的成品检验单时报 1_030_035_005")
    void createFinishedInspection_alreadyExists_throws1030035005() {
        DyeingOrderRespVO order = new DyeingOrderRespVO();
        order.setId(100L);
        order.setStatus("COMPLETED");
        when(dyeingOrderService.getDyeingOrder(100L)).thenReturn(order);

        InspectionOrderDO existing = new InspectionOrderDO();
        existing.setId(501L);
        existing.setDyeingOrderId(100L);
        existing.setType("FINISHED");
        when(inspectionOrderMapper.selectOne(any())).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.createFinishedInspection(100L));
        assertEquals(RELEASE_PRODUCTION_FINISHED_INSPECTION_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("createFinishedInspection: 正常生成带出产品、合同、染色单，targetWeight 为颜色明细数量合计")
    void createFinishedInspection_success_createsFinishedInspectionWithTargetWeightAndRefs() {
        DyeingOrderRespVO order = new DyeingOrderRespVO();
        order.setId(100L);
        order.setStatus("COMPLETED");
        order.setSalesContractId(200L);
        order.setContractNo("CH20261014-001");
        order.setProductCode("P001");
        order.setProductName("全棉汗布");

        DyeingColorRespVO color1 = new DyeingColorRespVO();
        color1.setQuantity(new BigDecimal("500.25"));
        DyeingColorRespVO color2 = new DyeingColorRespVO();
        color2.setQuantity(new BigDecimal("300.75"));
        order.setColors(Arrays.asList(color1, color2));

        when(dyeingOrderService.getDyeingOrder(100L)).thenReturn(order);
        when(inspectionOrderMapper.selectOne(any())).thenReturn(null);

        ProductDO product = new ProductDO();
        product.setId(10L);
        product.setCode("P001");
        when(productMapper.selectByCode("P001")).thenReturn(product);

        when(inspectionOrderService.createInspectionOrder(any())).thenReturn(888L);
        InspectionOrderRespVO inspectionOrderResp = new InspectionOrderRespVO();
        inspectionOrderResp.setId(888L);
        inspectionOrderResp.setCode("INS-20261014-001");
        when(inspectionOrderService.getInspectionOrder(888L)).thenReturn(inspectionOrderResp);

        Long inspectionOrderId = releaseProductionService.createFinishedInspection(100L);

        assertEquals(888L, inspectionOrderId);

        ArgumentCaptor<InspectionOrderSaveReqVO> captor = ArgumentCaptor.forClass(InspectionOrderSaveReqVO.class);
        verify(inspectionOrderService).createInspectionOrder(captor.capture());
        InspectionOrderSaveReqVO captured = captor.getValue();
        assertEquals("FINISHED", captured.getType());
        assertEquals("DRAFT", captured.getStatus());
        assertEquals("ROLL", captured.getPackageMode());
        assertEquals(10L, captured.getProductId());
        assertEquals(100L, captured.getDyeingOrderId());
        assertEquals(200L, captured.getSalesContractId());
        assertEquals(new BigDecimal("801.00"), captured.getTargetWeight());
        assertEquals(LocalDate.now(), captured.getOrderDate());
    }

    @Test
    @DisplayName("createFinishedInspection: 切面注解标注 @LogRecord")
    void createFinishedInspection_hasLogRecordAnnotation() throws NoSuchMethodException {
        Method method = ReleaseProductionServiceImpl.class.getMethod("createFinishedInspection", Long.class);
        LogRecord logRecord = method.getAnnotation(LogRecord.class);
        assertNotNull(logRecord, "createFinishedInspection 必须标注 @LogRecord");
        assertEquals("PRODUCTION", logRecord.type());
        assertEquals("CREATE_FINISHED_INSPECTION", logRecord.subType());
        assertEquals("{{#dyeingOrderId}}", logRecord.bizNo());
    }

}
