package com.luna.lunasrv.module.lunaerp.service.production.releaseproduction;

import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseFromContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import com.luna.lunasrv.module.lunaerp.service.production.dyeingorder.DyeingOrderService;
import com.luna.lunasrv.module.lunaerp.service.production.greigeorder.GreigeOrderService;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionColorSaveVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.enums.production.releaseproduction.ReleaseColorInstructionModeEnum;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class ReleaseProductionServiceImpl implements ReleaseProductionService {

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private SalesContractItemMapper salesContractItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private GreigeOrderService greigeOrderService;

    @Resource
    private DyeingOrderService dyeingOrderService;

    @Resource
    private InspectionOrderService inspectionOrderService;

    @Resource
    private ColorInstructionService colorInstructionService;

    @Resource
    private ColorInstructionMapper colorInstructionMapper;

    @Override
    public ReleaseFromContractRespVO getReleaseFromContract(Long contractId) {
        if (contractId == null) {
            throw exception(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS);
        }
        SalesContractDO contract = salesContractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS);
        }

        List<SalesContractItemDO> items = salesContractItemMapper.selectListByContractId(contractId);
        List<ReleaseFromContractRespVO.Line> lines = new ArrayList<>();
        if (CollUtil.isNotEmpty(items)) {
            for (SalesContractItemDO item : items) {
                BigDecimal contractQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal releasedQty = item.getReleasedQty() != null ? item.getReleasedQty() : BigDecimal.ZERO;
                BigDecimal remainingQty = contractQty.subtract(releasedQty);

                lines.add(ReleaseFromContractRespVO.Line.builder()
                        .contractLineId(item.getId())
                        .lineNo(item.getLineNo())
                        .productCode(item.getProductCode())
                        .productName(item.getProductName())
                        .composition(item.getComposition())
                        .width(item.getWidth())
                        .gsm(item.getWeight())
                        .customerColor(item.getCustomerColor())
                        .contractQty(contractQty)
                        .releasedQty(releasedQty)
                        .remainingQty(remainingQty)
                        .defaultLossRate(BigDecimal.ZERO)
                        .build());
            }
        }

        List<ReleaseFromContractRespVO.ExistingColorInstruction> existingColorInstructions = new ArrayList<>();
        if (contract.getCustomerId() != null && CollUtil.isNotEmpty(items)) {
            Set<String> contractProductCodes = items.stream()
                    .map(SalesContractItemDO::getProductCode)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());

            if (!contractProductCodes.isEmpty()) {
                List<ColorInstructionDO> cis = colorInstructionMapper.selectList(new LambdaQueryWrapperX<ColorInstructionDO>()
                        .eq(ColorInstructionDO::getCustomerId, contract.getCustomerId())
                        .in(ColorInstructionDO::getStatus, Arrays.asList("CONFIRMED", "IN_PRODUCTION"))
                        .in(ColorInstructionDO::getProductCode, contractProductCodes)
                        .orderByDesc(ColorInstructionDO::getId));
                if (CollUtil.isNotEmpty(cis)) {
                    for (ColorInstructionDO ci : cis) {
                        existingColorInstructions.add(ReleaseFromContractRespVO.ExistingColorInstruction.builder()
                                .id(ci.getId())
                                .instructionNo(ci.getInstructionNo())
                                .status(ci.getStatus())
                                .productCode(ci.getProductCode())
                                .productName(ci.getProductName())
                                .build());
                    }
                }
            }
        }

        return ReleaseFromContractRespVO.builder()
                .contractId(contract.getId())
                .contractNo(contract.getContractNo())
                .customerId(contract.getCustomerId())
                .clientName(contract.getClientName())
                .lines(lines)
                .existingColorInstructions(existingColorInstructions)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "PRODUCTION", subType = "RELEASE", bizNo = "{{#reqVO.contractId}}",
            success = "下达生产 合同【{{#contractNo}}】生成胚布单【{{#greigeOrderNos}}】染色单【{{#dyeingOrderIds}}】检验单【{{#inspectionOrderCodes}}】")
    public ReleaseProductionRespVO releaseProduction(ReleaseProductionReqVO reqVO) {
        // 1. 合同校验：存在且非 CANCELLED
        if (reqVO == null || reqVO.getContractId() == null) {
            throw exception(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS);
        }
        SalesContractDO contract = salesContractMapper.selectById(reqVO.getContractId());
        if (contract == null) {
            throw exception(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS);
        }
        if ("CANCELLED".equals(contract.getStatus())) {
            throw exception(RELEASE_PRODUCTION_CONTRACT_CANCELLED);
        }

        // 合同行映射与从属校验
        List<SalesContractItemDO> contractLines = salesContractItemMapper.selectListByContractId(contract.getId());
        Map<Long, SalesContractItemDO> contractItemMap = CollUtil.emptyIfNull(contractLines).stream()
                .collect(Collectors.toMap(SalesContractItemDO::getId, Function.identity(), (a, b) -> a));

        if (CollUtil.isNotEmpty(reqVO.getGroups())) {
            for (ReleaseProductionReqVO.Group group : reqVO.getGroups()) {
                if (CollUtil.isNotEmpty(group.getLines())) {
                    for (ReleaseProductionReqVO.Line line : group.getLines()) {
                        SalesContractItemDO item = contractItemMap.get(line.getContractLineId());
                        if (item == null) {
                            throw exception(RELEASE_PRODUCTION_CONTRACT_NOT_EXISTS);
                        }
                    }
                }
            }
        }

        // 2. 超下达拦截：逐行条件更新 released_qty（WHERE released_qty + delta <= quantity），影响行数 0 则报错回滚
        if (CollUtil.isNotEmpty(reqVO.getGroups())) {
            for (ReleaseProductionReqVO.Group group : reqVO.getGroups()) {
                if (CollUtil.isNotEmpty(group.getLines())) {
                    for (ReleaseProductionReqVO.Line line : group.getLines()) {
                        int updated = salesContractItemMapper.updateReleasedQty(
                                line.getContractLineId(),
                                line.getReleaseQty(),
                                contract.getTenantId());
                        if (updated <= 0) {
                            SalesContractItemDO item = contractItemMap.get(line.getContractLineId());
                            String identifier = item != null && item.getLineNo() != null
                                    ? String.valueOf(item.getLineNo())
                                    : String.valueOf(line.getContractLineId());
                            throw exception(RELEASE_PRODUCTION_OVER_RELEASE, identifier);
                        }
                    }
                }
            }
        }

        ReleaseProductionRespVO respVO = ReleaseProductionRespVO.builder()
                .greigeOrders(new ArrayList<>())
                .dyeingOrders(new ArrayList<>())
                .inspectionOrders(new ArrayList<>())
                .colorInstructions(new ArrayList<>())
                .build();

        List<String> greigeOrderNos = new ArrayList<>();
        List<Long> dyeingOrderIds = new ArrayList<>();
        List<String> inspectionOrderCodes = new ArrayList<>();

        // 3. 按产品分组编排下游单据
        if (CollUtil.isNotEmpty(reqVO.getGroups())) {
            for (ReleaseProductionReqVO.Group group : reqVO.getGroups()) {
                if (CollUtil.isEmpty(group.getLines())) {
                    continue;
                }

                // 解析关联产品ID
                Long productId = group.getProductId();
                if (productId == null && StringUtils.hasText(group.getProductCode())) {
                    ProductDO product = productMapper.selectByCode(group.getProductCode());
                    if (product != null) {
                        productId = product.getId();
                    }
                }

                // 3.1 打色单处理（EXISTING / NEW / NONE）
                Long colorInstructionId = null;
                String colorInstructionNo = null;
                ReleaseColorInstructionModeEnum mode = ReleaseColorInstructionModeEnum.of(group.getColorInstructionMode());

                if (ReleaseColorInstructionModeEnum.EXISTING == mode) {
                    Long existingCiId = group.getExistingColorInstructionId();
                    if (existingCiId == null) {
                        throw exception(RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED, "");
                    }
                    ColorInstructionDO ci;
                    try {
                        ci = colorInstructionService.validateColorInstructionForDownstream(
                                existingCiId,
                                contract.getCustomerId(),
                                productId,
                                group.getProductCode(),
                                RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED);
                    } catch (ServiceException e) {
                        if (RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED.getCode().equals(e.getCode())) {
                            throw exception(RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED, existingCiId);
                        }
                        throw e;
                    }
                    if (ci == null) {
                        throw exception(RELEASE_PRODUCTION_COLOR_INSTRUCTION_STATUS_NOT_ALLOWED, existingCiId);
                    }
                    colorInstructionId = ci.getId();
                    colorInstructionNo = ci.getInstructionNo();
                } else if (ReleaseColorInstructionModeEnum.NEW == mode) {
                    // NEW 模式：主灯源必填校验
                    ReleaseProductionReqVO.NewColorInstruction newCiConfig = group.getNewColorInstruction();
                    if (newCiConfig == null || CollUtil.isEmpty(newCiConfig.getColors())) {
                        throw exception(RELEASE_PRODUCTION_LIGHT_PRIMARY_REQUIRED);
                    }
                    for (ReleaseProductionReqVO.ColorLine cl : newCiConfig.getColors()) {
                        if (!StringUtils.hasText(cl.getLightPrimary())) {
                            throw exception(RELEASE_PRODUCTION_LIGHT_PRIMARY_REQUIRED);
                        }
                    }

                    ColorInstructionSaveReqVO ciSaveReq = new ColorInstructionSaveReqVO();
                    ciSaveReq.setCustomerId(contract.getCustomerId());
                    ciSaveReq.setProductId(productId);
                    ciSaveReq.setLabFactoryId(newCiConfig.getLabFactoryId());
                    ciSaveReq.setComposition(group.getComposition());
                    ciSaveReq.setWidth(group.getWidth());
                    ciSaveReq.setGsm(group.getGsm());
                    ciSaveReq.setInstructionDate(LocalDate.now());

                    List<ColorInstructionColorSaveVO> ciColors = new ArrayList<>();
                    for (ReleaseProductionReqVO.ColorLine cl : newCiConfig.getColors()) {
                        ColorInstructionColorSaveVO colorVO = new ColorInstructionColorSaveVO();
                        colorVO.setCustomerColorNo(cl.getCustomerColorNo());
                        colorVO.setColorName(StringUtils.hasText(cl.getColorName()) ? cl.getColorName() : cl.getCustomerColorNo());
                        colorVO.setLightPrimary(cl.getLightPrimary());
                        colorVO.setLightSecondary(cl.getLightSecondary());
                        colorVO.setFactoryColorNo(cl.getFactoryColorNo());
                        colorVO.setColorCardRef(cl.getColorCardRef());
                        colorVO.setStatus("PENDING");
                        ciColors.add(colorVO);
                    }
                    ciSaveReq.setColors(ciColors);

                    Long newCiId = colorInstructionService.createColorInstruction(ciSaveReq);
                    String newCiNo = getInstructionNoSilently(newCiId);
                    respVO.getColorInstructions().add(new ReleaseProductionRespVO.ColorInstructionSimpleVO(newCiId, newCiNo));
                    // 规范：新建 PENDING 打色单不可被下游引用，零回写
                    colorInstructionId = null;
                    colorInstructionNo = null;
                }

                // 组内数量汇总（使用 Group 内聚方法）
                BigDecimal groupTotalReleaseQty = group.calculateTotalReleaseQty();
                BigDecimal maxLoss = group.calculateMaxLossRate();

                // 3.2 创建胚布单（ACTIVE）
                GreigeOrderSaveReqVO greigeReq = new GreigeOrderSaveReqVO();
                greigeReq.setSalesContractId(contract.getId());
                greigeReq.setContractNo(contract.getContractNo());
                greigeReq.setGreigeFactoryId(reqVO.getGreigeFactoryId());
                greigeReq.setDyeingFactoryId(reqVO.getDyeingFactoryId());
                greigeReq.setProductId(productId);
                greigeReq.setProductCode(group.getProductCode());
                greigeReq.setProductName(group.getProductName());
                greigeReq.setComposition(group.getComposition());
                greigeReq.setWidth(group.getWidth());
                greigeReq.setGsm(group.getGsm());
                greigeReq.setPlannedQty(groupTotalReleaseQty);
                greigeReq.setFinishedQty(groupTotalReleaseQty);
                greigeReq.setSuggestedLoss(maxLoss);
                greigeReq.setStatus("ACTIVE");
                greigeReq.setProductionMode("OUTSOURCE");
                greigeReq.setOrderDate(LocalDate.now());
                greigeReq.setColorInstructionId(colorInstructionId);
                greigeReq.setColorInstructionNo(colorInstructionNo);

                List<GreigeOrderColorSaveReqVO> greigeColors = new ArrayList<>();
                for (ReleaseProductionReqVO.Line line : group.getLines()) {
                    SalesContractItemDO item = contractItemMap.get(line.getContractLineId());
                    GreigeOrderColorSaveReqVO colorVO = new GreigeOrderColorSaveReqVO();
                    colorVO.setColor(line.getCustomerColor());
                    colorVO.setFinishedQty(line.getReleaseQty());
                    colorVO.setLossRate(line.getLossRate() != null ? line.getLossRate() : BigDecimal.ZERO);
                    colorVO.setContractLineId(line.getContractLineId());
                    colorVO.setContractLineNo(item != null ? item.getLineNo() : null);
                    greigeColors.add(colorVO);
                }
                greigeReq.setColors(greigeColors);

                Long greigeOrderId = greigeOrderService.createGreigeOrder(greigeReq);
                String greigeOrderNo = getGreigeOrderNoSilently(greigeOrderId);
                respVO.getGreigeOrders().add(new ReleaseProductionRespVO.GreigeOrderSimpleVO(greigeOrderId, greigeOrderNo));
                if (StringUtils.hasText(greigeOrderNo)) {
                    greigeOrderNos.add(greigeOrderNo);
                } else {
                    greigeOrderNos.add(String.valueOf(greigeOrderId));
                }

                // 3.3 创建染色单（ACTIVE）
                DyeingOrderSaveReqVO dyeingReq = new DyeingOrderSaveReqVO();
                dyeingReq.setSalesContractId(contract.getId());
                dyeingReq.setContractNo(contract.getContractNo());
                dyeingReq.setCustomerId(contract.getCustomerId());
                dyeingReq.setClientName(contract.getClientName());
                dyeingReq.setDyeingFactoryId(reqVO.getDyeingFactoryId());
                dyeingReq.setProductCode(group.getProductCode());
                dyeingReq.setProductName(group.getProductName());
                dyeingReq.setComposition(group.getComposition());
                dyeingReq.setWidth(group.getWidth());
                dyeingReq.setGsm(group.getGsm());
                dyeingReq.setStatus("ACTIVE");
                dyeingReq.setOrderDate(LocalDate.now());
                dyeingReq.setColorInstructionId(colorInstructionId);
                dyeingReq.setColorInstructionNo(colorInstructionNo);

                List<DyeingColorSaveReqVO> dyeingColors = new ArrayList<>();
                for (ReleaseProductionReqVO.Line line : group.getLines()) {
                    SalesContractItemDO item = contractItemMap.get(line.getContractLineId());
                    DyeingColorSaveReqVO colorVO = new DyeingColorSaveReqVO();
                    colorVO.setCustomerColorNo(line.getCustomerColor());
                    colorVO.setColorName(line.getCustomerColor());
                    colorVO.setQuantity(line.getReleaseQty());
                    colorVO.setContractQty(item != null ? item.getQuantity() : null);
                    colorVO.setContractLineId(line.getContractLineId());
                    colorVO.setContractLineNo(item != null ? item.getLineNo() : null);
                    dyeingColors.add(colorVO);
                }
                dyeingReq.setColors(dyeingColors);

                Long dyeingOrderId = dyeingOrderService.createDyeingOrder(dyeingReq);
                respVO.getDyeingOrders().add(new ReleaseProductionRespVO.DyeingOrderSimpleVO(dyeingOrderId));
                dyeingOrderIds.add(dyeingOrderId);

                // 3.4 创建胚布检验单（type=GREIGE, status=DRAFT, targetWeight=组内下达量合计）
                InspectionOrderSaveReqVO inspectionReq = new InspectionOrderSaveReqVO();
                inspectionReq.setType("GREIGE");
                inspectionReq.setStatus("DRAFT");
                inspectionReq.setProductId(productId);
                inspectionReq.setGreigeOrderId(greigeOrderId);
                inspectionReq.setSalesContractId(contract.getId());
                inspectionReq.setTargetWeight(groupTotalReleaseQty);
                inspectionReq.setOrderDate(LocalDate.now());

                Long inspectionOrderId = inspectionOrderService.createInspectionOrder(inspectionReq);
                String inspectionCode = getInspectionOrderCodeSilently(inspectionOrderId);
                respVO.getInspectionOrders().add(new ReleaseProductionRespVO.InspectionOrderSimpleVO(inspectionOrderId, inspectionCode, "GREIGE"));
                if (StringUtils.hasText(inspectionCode)) {
                    inspectionOrderCodes.add(inspectionCode);
                } else {
                    inspectionOrderCodes.add(String.valueOf(inspectionOrderId));
                }
            }
        }

        LogRecordContext.putVariable("contractNo", contract.getContractNo());
        LogRecordContext.putVariable("greigeOrderNos", String.join(",", greigeOrderNos));
        LogRecordContext.putVariable("dyeingOrderIds", dyeingOrderIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        LogRecordContext.putVariable("inspectionOrderCodes", String.join(",", inspectionOrderCodes));

        return respVO;
    }

    private String getInstructionNoSilently(Long colorInstructionId) {
        if (colorInstructionId == null) {
            return null;
        }
        try {
            ColorInstructionDO ci = colorInstructionMapper.selectById(colorInstructionId);
            return ci != null ? ci.getInstructionNo() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getGreigeOrderNoSilently(Long greigeOrderId) {
        if (greigeOrderId == null) {
            return null;
        }
        try {
            GreigeOrderRespVO greigeResp = greigeOrderService.getGreigeOrder(greigeOrderId);
            return greigeResp != null ? greigeResp.getOrderNo() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getInspectionOrderCodeSilently(Long inspectionOrderId) {
        if (inspectionOrderId == null) {
            return null;
        }
        try {
            InspectionOrderRespVO inspectionResp = inspectionOrderService.getInspectionOrder(inspectionOrderId);
            return inspectionResp != null ? inspectionResp.getCode() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

}
