package com.luna.lunasrv.module.lunaerp.service.production.releaseproduction;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.*;
import java.time.LocalDate;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import com.luna.lunasrv.module.lunaerp.service.production.dyeingorder.DyeingOrderService;
import com.luna.lunasrv.module.lunaerp.service.production.greigeorder.GreigeOrderService;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ReleaseProductionErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ReleaseProductionServiceImpl} 纯 Mockito 单元测试
 *
 * 覆盖：
 * 1. from-contract：计算 remainingQty = quantity - releasedQty，返回已有 CONFIRMED/IN_PRODUCTION 打色单；合同不存在报 000
 * 2. release 正常流程：一组一行 → 累加 released_qty、创建胚布单（ACTIVE）、染色单（ACTIVE）、胚布检验单（GREIGE/DRAFT）
 * 3. release 同产品多行合并：两行同产品 → 胚布单多颜色行、染色单多颜色行、检验单 targetWeight 累加
 * 4. 超下达拦截：条件更新影响行数 0 抛 002
 * 5. 并发超下达：条件更新返回 0 抛 002
 * 6. 合同行不属该合同：抛 002
 * 7. EXISTING 打色单：CONFIRMED / IN_PRODUCTION 正常引用；PENDING 抛 003；打色单不存在抛 003
 * 8. NEW 打色单：主灯源缺失抛 004；正常创建 PENDING 打色单，胚布/染色不引用（零回写）
 * 9. NONE 打色单：不创建不引用
 * 10. 合同 CANCELLED：下达拦截抛 001
 * 11. 合同不存在：下达拦截抛 000
 *
 * @author luna
 */
class ReleaseProductionServiceImplTest {

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private SalesContractItemMapper salesContractItemMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ColorInstructionMapper colorInstructionMapper;

    @Mock
    private ColorInstructionService colorInstructionService;

    @Mock
    private GreigeOrderService greigeOrderService;

    @Mock
    private DyeingOrderService dyeingOrderService;

    @Mock
    private InspectionOrderService inspectionOrderService;

    @InjectMocks
    private ReleaseProductionServiceImpl releaseProductionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), ColorInstructionDO.class);
    }

    private SalesContractDO mockContract(Long id, String contractNo, String status) {
        SalesContractDO c = new SalesContractDO();
        c.setId(id);
        c.setContractNo(contractNo);
        c.setCustomerId(2001L);
        c.setClientName("优衣纺织");
        c.setStatus(status);
        c.setDeliveryDate(LocalDate.of(2026, 11, 1));
        return c;
    }

    private SalesContractItemDO mockContractItem(Long id, Long contractId, int lineNo, String productCode,
                                                 String color, BigDecimal qty, BigDecimal releasedQty) {
        SalesContractItemDO item = new SalesContractItemDO();
        item.setId(id);
        item.setContractId(contractId);
        item.setLineNo(lineNo);
        item.setProductCode(productCode);
        item.setProductName("纯棉汗布");
        item.setComposition("100%棉");
        item.setWidth("180cm");
        item.setWeight("180g");
        item.setCustomerColor(color);
        item.setQuantity(qty);
        item.setReleasedQty(releasedQty);
        return item;
    }

    // ==================== 1. getReleaseFromContract 测试 ====================

    @Test
    @DisplayName("getReleaseFromContract: 正常返回合同行与剩余量、以及已有打色单")
    void getReleaseFromContract_success() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item1 = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("1000.00"), new BigDecimal("400.00"));
        SalesContractItemDO item2 = mockContractItem(2L, contractId, 2, "P001", "C102",
                new BigDecimal("500.00"), new BigDecimal("500.00"));
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item1, item2));

        ColorInstructionDO inst = ColorInstructionDO.builder()
                .id(501L)
                .instructionNo("DS20261011001")
                .status("CONFIRMED")
                .productCode("P001")
                .productName("纯棉汗布")
                .build();
        when(colorInstructionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(inst));

        ReleaseFromContractRespVO result = releaseProductionService.getReleaseFromContract(contractId);

        assertNotNull(result);
        assertEquals(contractId, result.getContractId());
        assertEquals("CH20260001", result.getContractNo());
        assertEquals("优衣纺织", result.getClientName());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(LocalDate.of(2026, 11, 1), result.getDeliveryDate());

        assertEquals(2, result.getLines().size());
        ReleaseContractLineVO line1 = result.getLines().get(0);
        assertEquals(1L, line1.getContractLineId());
        assertEquals(new BigDecimal("1000.00"), line1.getContractQty());
        assertEquals(new BigDecimal("400.00"), line1.getReleasedQty());
        assertEquals(new BigDecimal("600.00"), line1.getRemainingQty());

        ReleaseContractLineVO line2 = result.getLines().get(1);
        assertEquals(2L, line2.getContractLineId());
        assertEquals(new BigDecimal("500.00"), line2.getContractQty());
        assertEquals(new BigDecimal("500.00"), line2.getReleasedQty());
        assertEquals(0, line2.getRemainingQty().compareTo(BigDecimal.ZERO));

        assertEquals(1, result.getExistingColorInstructions().size());
        assertEquals(501L, result.getExistingColorInstructions().get(0).getId());
        assertEquals("DS20261011001", result.getExistingColorInstructions().get(0).getInstructionNo());
        assertEquals("CONFIRMED", result.getExistingColorInstructions().get(0).getStatus());
    }

    @Test
    @DisplayName("getReleaseFromContract: 合同不存在抛 1_030_035_000")
    void getReleaseFromContract_notExists_throws000() {
        when(salesContractMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.getReleaseFromContract(999L));
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ==================== 2. releaseProduction 合同状态校验 ====================

    @Test
    @DisplayName("releaseProduction: 合同不存在抛 1_030_035_000")
    void releaseProduction_contractNotExists_throws000() {
        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(999L)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(Collections.emptyList())
                .build();
        when(salesContractMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(SALES_CONTRACT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("releaseProduction: 合同已取消抛 1_030_035_001")
    void releaseProduction_contractCancelled_throws001() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "CANCELLED");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .lines(List.of(ReleaseGroupLineVO.builder()
                                .contractLineId(1L)
                                .releaseQty(new BigDecimal("100.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(SALES_CONTRACT_CANCELLED.getCode(), ex.getCode());
    }

    // ==================== 3. releaseProduction 超下达 / 并发拦截 ====================

    @Test
    @DisplayName("releaseProduction: 条件更新影响行数 0 抛 1_030_035_002")
    void releaseProduction_updateAffectedZero_throws002() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("1000.00"), new BigDecimal("400.00"));
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));

        // 条件更新返回 0（超下达拦截或并发冲突）
        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), any(BigDecimal.class)))
                .thenReturn(0);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .lines(List.of(ReleaseGroupLineVO.builder()
                                .contractLineId(1L)
                                .releaseQty(new BigDecimal("700.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(CONTRACT_LINE_OVER_RELEASED.getCode(), ex.getCode());

        // 验证没有创建任何单据
        verify(greigeOrderService, never()).createGreigeOrder(any());
        verify(dyeingOrderService, never()).createDyeingOrder(any());
        verify(inspectionOrderService, never()).createInspectionOrder(any());
    }

    @Test
    @DisplayName("releaseProduction: 合同行不属于该合同抛 1_030_035_002")
    void releaseProduction_lineNotInContract_throws002() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("1000.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));

        // 传了属于其他合同的行 ID 99L
        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .lines(List.of(ReleaseGroupLineVO.builder()
                                .contractLineId(99L)
                                .releaseQty(new BigDecimal("100.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(CONTRACT_LINE_OVER_RELEASED.getCode(), ex.getCode());
    }

    // ==================== 4. releaseProduction 正常一组一行 ====================

    @Test
    @DisplayName("releaseProduction: 正常流程一组一行，创建胚布单、染色单、检验单并累加已下达量")
    void releaseProduction_normalFlow_oneGroupOneLine_success() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("1000.00"), new BigDecimal("400.00"));
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));

        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), eq(new BigDecimal("600.00"))))
                .thenReturn(1);

        ProductDO prod = new ProductDO();
        prod.setId(10L);
        prod.setCode("P001");
        when(productMapper.selectByCode("P001")).thenReturn(prod);

        when(greigeOrderService.createGreigeOrder(any(GreigeOrderSaveReqVO.class))).thenReturn(11L);
        GreigeOrderRespVO greigeResp = new GreigeOrderRespVO();
        greigeResp.setId(11L);
        greigeResp.setOrderNo("PB1011001");
        when(greigeOrderService.getGreigeOrder(11L)).thenReturn(greigeResp);

        when(dyeingOrderService.createDyeingOrder(any(DyeingOrderSaveReqVO.class))).thenReturn(21L);

        when(inspectionOrderService.createInspectionOrder(any(InspectionOrderSaveReqVO.class))).thenReturn(31L);
        InspectionOrderRespVO inspectResp = new InspectionOrderRespVO();
        inspectResp.setId(31L);
        inspectResp.setCode("INS-20261011-001");
        inspectResp.setType("GREIGE");
        when(inspectionOrderService.getInspectionOrder(31L)).thenReturn(inspectResp);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .productName("纯棉汗布")
                        .composition("100%棉")
                        .width("180cm")
                        .gsm("180g")
                        .colorInstructionMode("NONE")
                        .lines(List.of(ReleaseGroupLineVO.builder()
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
        assertEquals("PB1011001", resp.getGreigeOrders().get(0).getOrderNo());

        assertEquals(1, resp.getDyeingOrders().size());
        assertEquals(21L, resp.getDyeingOrders().get(0).getId());

        assertEquals(1, resp.getInspectionOrders().size());
        assertEquals(31L, resp.getInspectionOrders().get(0).getId());
        assertEquals("INS-20261011-001", resp.getInspectionOrders().get(0).getCode());
        assertEquals("GREIGE", resp.getInspectionOrders().get(0).getType());

        // 校验胚布单创建参数
        ArgumentCaptor<GreigeOrderSaveReqVO> greigeCaptor = ArgumentCaptor.forClass(GreigeOrderSaveReqVO.class);
        verify(greigeOrderService).createGreigeOrder(greigeCaptor.capture());
        GreigeOrderSaveReqVO greigeReq = greigeCaptor.getValue();
        assertEquals("ACTIVE", greigeReq.getStatus());
        assertEquals(contractId, greigeReq.getSalesContractId());
        assertEquals("CH20260001", greigeReq.getContractNo());
        assertEquals(301L, greigeReq.getGreigeFactoryId());
        assertEquals(302L, greigeReq.getDyeingFactoryId());
        assertEquals(new BigDecimal("600.00"), greigeReq.getFinishedQty());
        assertEquals(1, greigeReq.getColors().size());
        assertEquals(1L, greigeReq.getColors().get(0).getContractLineId());
        assertEquals(1, greigeReq.getColors().get(0).getContractLineNo());
        assertEquals("C101", greigeReq.getColors().get(0).getColor());
        assertEquals(new BigDecimal("600.00"), greigeReq.getColors().get(0).getFinishedQty());

        // 校验染色单创建参数
        ArgumentCaptor<DyeingOrderSaveReqVO> dyeingCaptor = ArgumentCaptor.forClass(DyeingOrderSaveReqVO.class);
        verify(dyeingOrderService).createDyeingOrder(dyeingCaptor.capture());
        DyeingOrderSaveReqVO dyeingReq = dyeingCaptor.getValue();
        assertEquals("ACTIVE", dyeingReq.getStatus());
        assertEquals(contractId, dyeingReq.getSalesContractId());
        assertEquals(302L, dyeingReq.getDyeingFactoryId());
        assertEquals(1, dyeingReq.getColors().size());
        assertEquals(1L, dyeingReq.getColors().get(0).getContractLineId());
        assertEquals(1, dyeingReq.getColors().get(0).getContractLineNo());
        assertEquals("C101", dyeingReq.getColors().get(0).getCustomerColorNo());
        assertEquals(new BigDecimal("600.00"), dyeingReq.getColors().get(0).getQuantity());

        // 校验胚布检验单创建参数
        ArgumentCaptor<InspectionOrderSaveReqVO> inspectCaptor = ArgumentCaptor.forClass(InspectionOrderSaveReqVO.class);
        verify(inspectionOrderService).createInspectionOrder(inspectCaptor.capture());
        InspectionOrderSaveReqVO inspectReq = inspectCaptor.getValue();
        assertEquals("GREIGE", inspectReq.getType());
        assertEquals("DRAFT", inspectReq.getStatus());
        assertEquals(11L, inspectReq.getGreigeOrderId());
        assertEquals(contractId, inspectReq.getSalesContractId());
        assertEquals(new BigDecimal("600.00"), inspectReq.getTargetWeight());
    }

    // ==================== 5. releaseProduction 同产品多行合并 ====================

    @Test
    @DisplayName("releaseProduction: 同产品两行合并到一张胚布单两个颜色行、一张染色单两个颜色行")
    void releaseProduction_multiLineMerge_sameProduct_success() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item1 = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("500.00"), BigDecimal.ZERO);
        SalesContractItemDO item2 = mockContractItem(2L, contractId, 2, "P001", "C102",
                new BigDecimal("300.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item1, item2));

        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), eq(new BigDecimal("500.00")))).thenReturn(1);
        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(2L), eq(new BigDecimal("300.00")))).thenReturn(1);

        when(greigeOrderService.createGreigeOrder(any(GreigeOrderSaveReqVO.class))).thenReturn(11L);
        when(dyeingOrderService.createDyeingOrder(any(DyeingOrderSaveReqVO.class))).thenReturn(21L);
        when(inspectionOrderService.createInspectionOrder(any(InspectionOrderSaveReqVO.class))).thenReturn(31L);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .productName("纯棉汗布")
                        .lines(List.of(
                                ReleaseGroupLineVO.builder()
                                        .contractLineId(1L)
                                        .customerColor("C101")
                                        .releaseQty(new BigDecimal("500.00"))
                                        .lossRate(new BigDecimal("0.04"))
                                        .build(),
                                ReleaseGroupLineVO.builder()
                                        .contractLineId(2L)
                                        .customerColor("C102")
                                        .releaseQty(new BigDecimal("300.00"))
                                        .lossRate(new BigDecimal("0.05"))
                                        .build()
                        ))
                        .build()))
                .build();

        ReleaseProductionRespVO resp = releaseProductionService.releaseProduction(reqVO);

        assertNotNull(resp);
        assertEquals(1, resp.getGreigeOrders().size());
        assertEquals(1, resp.getDyeingOrders().size());
        assertEquals(1, resp.getInspectionOrders().size());

        // 胚布单应有 2 个颜色行，suggestedLoss 为最大值 0.05，finishedQty = 800
        ArgumentCaptor<GreigeOrderSaveReqVO> greigeCaptor = ArgumentCaptor.forClass(GreigeOrderSaveReqVO.class);
        verify(greigeOrderService).createGreigeOrder(greigeCaptor.capture());
        GreigeOrderSaveReqVO greigeReq = greigeCaptor.getValue();
        assertEquals(new BigDecimal("800.00"), greigeReq.getFinishedQty());
        assertEquals(new BigDecimal("0.05"), greigeReq.getSuggestedLoss());
        assertEquals(2, greigeReq.getColors().size());
        assertEquals("C101", greigeReq.getColors().get(0).getColor());
        assertEquals(new BigDecimal("500.00"), greigeReq.getColors().get(0).getFinishedQty());
        assertEquals("C102", greigeReq.getColors().get(1).getColor());
        assertEquals(new BigDecimal("300.00"), greigeReq.getColors().get(1).getFinishedQty());

        // 染色单应有 2 个颜色明细
        ArgumentCaptor<DyeingOrderSaveReqVO> dyeingCaptor = ArgumentCaptor.forClass(DyeingOrderSaveReqVO.class);
        verify(dyeingOrderService).createDyeingOrder(dyeingCaptor.capture());
        DyeingOrderSaveReqVO dyeingReq = dyeingCaptor.getValue();
        assertEquals(2, dyeingReq.getColors().size());
        assertEquals("C101", dyeingReq.getColors().get(0).getCustomerColorNo());
        assertEquals(new BigDecimal("500.00"), dyeingReq.getColors().get(0).getQuantity());
        assertEquals("C102", dyeingReq.getColors().get(1).getCustomerColorNo());
        assertEquals(new BigDecimal("300.00"), dyeingReq.getColors().get(1).getQuantity());

        // 检验单 targetWeight 为合计 800
        ArgumentCaptor<InspectionOrderSaveReqVO> inspectCaptor = ArgumentCaptor.forClass(InspectionOrderSaveReqVO.class);
        verify(inspectionOrderService).createInspectionOrder(inspectCaptor.capture());
        assertEquals(new BigDecimal("800.00"), inspectCaptor.getValue().getTargetWeight());
    }

    // ==================== 6. EXISTING 打色单测试 ====================

    @Test
    @DisplayName("releaseProduction: EXISTING 模式 CONFIRMED 打色单成功引用")
    void releaseProduction_existingMode_confirmed_success() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("500.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));
        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), any(BigDecimal.class))).thenReturn(1);

        ColorInstructionDO instruction = ColorInstructionDO.builder()
                .id(501L)
                .instructionNo("DS20261011001")
                .status("CONFIRMED")
                .build();
        when(colorInstructionMapper.selectById(501L)).thenReturn(instruction);

        when(greigeOrderService.createGreigeOrder(any(GreigeOrderSaveReqVO.class))).thenReturn(11L);
        when(dyeingOrderService.createDyeingOrder(any(DyeingOrderSaveReqVO.class))).thenReturn(21L);
        when(inspectionOrderService.createInspectionOrder(any(InspectionOrderSaveReqVO.class))).thenReturn(31L);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .colorInstructionMode("EXISTING")
                        .existingColorInstructionId(501L)
                        .lines(List.of(ReleaseGroupLineVO.builder()
                                .contractLineId(1L)
                                .customerColor("C101")
                                .releaseQty(new BigDecimal("500.00"))
                                .build()))
                        .build()))
                .build();

        ReleaseProductionRespVO resp = releaseProductionService.releaseProduction(reqVO);
        assertNotNull(resp);

        // 校验胚布单与染色单引用了打色单
        ArgumentCaptor<GreigeOrderSaveReqVO> greigeCaptor = ArgumentCaptor.forClass(GreigeOrderSaveReqVO.class);
        verify(greigeOrderService).createGreigeOrder(greigeCaptor.capture());
        assertEquals(501L, greigeCaptor.getValue().getColorInstructionId());
        assertEquals("DS20261011001", greigeCaptor.getValue().getColorInstructionNo());

        ArgumentCaptor<DyeingOrderSaveReqVO> dyeingCaptor = ArgumentCaptor.forClass(DyeingOrderSaveReqVO.class);
        verify(dyeingOrderService).createDyeingOrder(dyeingCaptor.capture());
        assertEquals(501L, dyeingCaptor.getValue().getColorInstructionId());
        assertEquals("DS20261011001", dyeingCaptor.getValue().getColorInstructionNo());
    }

    @Test
    @DisplayName("releaseProduction: EXISTING 模式 IN_PRODUCTION 打色单成功引用")
    void releaseProduction_existingMode_inProduction_success() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("500.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));
        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), any(BigDecimal.class))).thenReturn(1);

        ColorInstructionDO instruction = ColorInstructionDO.builder()
                .id(502L)
                .instructionNo("DS20261011002")
                .status("IN_PRODUCTION")
                .build();
        when(colorInstructionMapper.selectById(502L)).thenReturn(instruction);

        when(greigeOrderService.createGreigeOrder(any(GreigeOrderSaveReqVO.class))).thenReturn(11L);
        when(dyeingOrderService.createDyeingOrder(any(DyeingOrderSaveReqVO.class))).thenReturn(21L);
        when(inspectionOrderService.createInspectionOrder(any(InspectionOrderSaveReqVO.class))).thenReturn(31L);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .colorInstructionMode("EXISTING")
                        .existingColorInstructionId(502L)
                        .lines(List.of(ReleaseGroupLineVO.builder()
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
        assertEquals(502L, greigeCaptor.getValue().getColorInstructionId());
    }

    @Test
    @DisplayName("releaseProduction: EXISTING 模式 PENDING 状态打色单被引用抛 1_030_035_003")
    void releaseProduction_existingMode_pending_throws003() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("500.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));
        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), any(BigDecimal.class))).thenReturn(1);

        ColorInstructionDO instruction = ColorInstructionDO.builder()
                .id(501L)
                .instructionNo("DS20261011001")
                .status("PENDING")
                .build();
        when(colorInstructionMapper.selectById(501L)).thenReturn(instruction);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .colorInstructionMode("EXISTING")
                        .existingColorInstructionId(501L)
                        .lines(List.of(ReleaseGroupLineVO.builder()
                                .contractLineId(1L)
                                .customerColor("C101")
                                .releaseQty(new BigDecimal("500.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("releaseProduction: EXISTING 模式打色单不存在抛 1_030_035_003")
    void releaseProduction_existingMode_notExists_throws003() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("500.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));
        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), any(BigDecimal.class))).thenReturn(1);

        when(colorInstructionMapper.selectById(999L)).thenReturn(null);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .colorInstructionMode("EXISTING")
                        .existingColorInstructionId(999L)
                        .lines(List.of(ReleaseGroupLineVO.builder()
                                .contractLineId(1L)
                                .customerColor("C101")
                                .releaseQty(new BigDecimal("500.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    // ==================== 7. NEW 打色单测试 ====================

    @Test
    @DisplayName("releaseProduction: NEW 模式主灯源缺失抛 1_030_035_004")
    void releaseProduction_newMode_missingLightPrimary_throws004() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("500.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));
        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), any(BigDecimal.class))).thenReturn(1);

        // lightPrimary 为空字符串
        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .colorInstructionMode("NEW")
                        .newColorInstruction(ReleaseNewColorInstructionVO.builder()
                                .colors(List.of(ReleaseNewColorVO.builder()
                                        .contractLineId(1L)
                                        .customerColorNo("C101")
                                        .lightPrimary("") // 空主灯源
                                        .build()))
                                .build())
                        .lines(List.of(ReleaseGroupLineVO.builder()
                                .contractLineId(1L)
                                .customerColor("C101")
                                .releaseQty(new BigDecimal("500.00"))
                                .build()))
                        .build()))
                .build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> releaseProductionService.releaseProduction(reqVO));
        assertEquals(COLOR_INSTRUCTION_LIGHT_PRIMARY_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("releaseProduction: NEW 模式创建 PENDING 打色单，胚布/染色不引用（零回写）")
    void releaseProduction_newMode_success() {
        Long contractId = 100L;
        SalesContractDO contract = mockContract(contractId, "CH20260001", "ACTIVE");
        when(salesContractMapper.selectById(contractId)).thenReturn(contract);

        SalesContractItemDO item = mockContractItem(1L, contractId, 1, "P001", "C101",
                new BigDecimal("500.00"), BigDecimal.ZERO);
        when(salesContractItemMapper.selectListByContractId(contractId)).thenReturn(List.of(item));
        when(salesContractItemMapper.updateReleasedQtyIncrement(eq(1L), any(BigDecimal.class))).thenReturn(1);

        when(colorInstructionService.createColorInstruction(any(ColorInstructionSaveReqVO.class))).thenReturn(601L);
        when(greigeOrderService.createGreigeOrder(any(GreigeOrderSaveReqVO.class))).thenReturn(11L);
        when(dyeingOrderService.createDyeingOrder(any(DyeingOrderSaveReqVO.class))).thenReturn(21L);
        when(inspectionOrderService.createInspectionOrder(any(InspectionOrderSaveReqVO.class))).thenReturn(31L);

        ReleaseProductionReqVO reqVO = ReleaseProductionReqVO.builder()
                .contractId(contractId)
                .greigeFactoryId(301L)
                .dyeingFactoryId(302L)
                .groups(List.of(ReleaseGroupVO.builder()
                        .productCode("P001")
                        .colorInstructionMode("NEW")
                        .newColorInstruction(ReleaseNewColorInstructionVO.builder()
                                .labFactoryId(401L)
                                .colors(List.of(ReleaseNewColorVO.builder()
                                        .contractLineId(1L)
                                        .customerColorNo("C101")
                                        .colorName("大红")
                                        .lightPrimary("D65")
                                        .build()))
                                .build())
                        .lines(List.of(ReleaseGroupLineVO.builder()
                                .contractLineId(1L)
                                .customerColor("C101")
                                .releaseQty(new BigDecimal("500.00"))
                                .build()))
                        .build()))
                .build();

        ReleaseProductionRespVO resp = releaseProductionService.releaseProduction(reqVO);

        assertNotNull(resp);
        assertEquals(1, resp.getColorInstructions().size());
        assertEquals(601L, resp.getColorInstructions().get(0).getId());

        // 校验打色单入参
        ArgumentCaptor<ColorInstructionSaveReqVO> colorCaptor = ArgumentCaptor.forClass(ColorInstructionSaveReqVO.class);
        verify(colorInstructionService).createColorInstruction(colorCaptor.capture());
        ColorInstructionSaveReqVO colorReq = colorCaptor.getValue();
        assertEquals(2001L, colorReq.getCustomerId());
        assertEquals(401L, colorReq.getLabFactoryId());
        assertEquals(1, colorReq.getColors().size());
        assertEquals("C101", colorReq.getColors().get(0).getCustomerColorNo());
        assertEquals("D65", colorReq.getColors().get(0).getLightPrimary());
        assertEquals("PENDING", colorReq.getColors().get(0).getStatus());

        // 校验胚布单与染色单【不引用】新建的打色单（零回写）
        ArgumentCaptor<GreigeOrderSaveReqVO> greigeCaptor = ArgumentCaptor.forClass(GreigeOrderSaveReqVO.class);
        verify(greigeOrderService).createGreigeOrder(greigeCaptor.capture());
        assertNull(greigeCaptor.getValue().getColorInstructionId());

        ArgumentCaptor<DyeingOrderSaveReqVO> dyeingCaptor = ArgumentCaptor.forClass(DyeingOrderSaveReqVO.class);
        verify(dyeingOrderService).createDyeingOrder(dyeingCaptor.capture());
        assertNull(dyeingCaptor.getValue().getColorInstructionId());
    }

}
