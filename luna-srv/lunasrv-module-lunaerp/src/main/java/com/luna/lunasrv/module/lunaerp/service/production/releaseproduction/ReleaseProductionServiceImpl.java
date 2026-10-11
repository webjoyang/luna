package com.luna.lunasrv.module.lunaerp.service.production.releaseproduction;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderColorSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.ColorInstructionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.service.production.colorinstruction.ColorInstructionService;
import com.luna.lunasrv.module.lunaerp.service.production.dyeingorder.DyeingOrderService;
import com.luna.lunasrv.module.lunaerp.service.production.greigeorder.GreigeOrderService;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderService;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.DYEING_ORDER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.DYEING_ORDER_STATUS_NOT_ALLOWED;
import static com.luna.lunasrv.module.lunaerp.enums.ReleaseProductionErrorCodeConstants.*;

/**
 * 生产管理 - 下达生产编排 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
public class ReleaseProductionServiceImpl implements ReleaseProductionService {

    @Resource
    private SalesContractMapper salesContractMapper;

    @Resource
    private SalesContractItemMapper salesContractItemMapper;

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ColorInstructionMapper colorInstructionMapper;

    @Resource
    private ColorInstructionService colorInstructionService;

    @Resource
    private GreigeOrderService greigeOrderService;

    @Resource
    private DyeingOrderService dyeingOrderService;

    @Resource
    private InspectionOrderService inspectionOrderService;

    @Resource
    private DyeingOrderMapper dyeingOrderMapper;

    @Resource
    private DyeingColorMapper dyeingColorMapper;

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Override
    public ReleaseFromContractRespVO getReleaseFromContract(Long contractId) {
        if (contractId == null) {
            throw exception(SALES_CONTRACT_NOT_EXISTS);
        }
        SalesContractDO contract = salesContractMapper.selectById(contractId);
        if (contract == null) {
            throw exception(SALES_CONTRACT_NOT_EXISTS);
        }

        List<SalesContractItemDO> items = salesContractItemMapper.selectListByContractId(contractId);
        List<ReleaseContractLineVO> lineVOs = new ArrayList<>();
        Set<String> productCodes = new HashSet<>();
        if (CollUtil.isNotEmpty(items)) {
            for (SalesContractItemDO item : items) {
                BigDecimal contractQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal releasedQty = item.getReleasedQty() != null ? item.getReleasedQty() : BigDecimal.ZERO;
                BigDecimal remainingQty = contractQty.subtract(releasedQty);
                if (remainingQty.compareTo(BigDecimal.ZERO) < 0) {
                    remainingQty = BigDecimal.ZERO;
                }

                if (StringUtils.hasText(item.getProductCode())) {
                    productCodes.add(item.getProductCode());
                }

                lineVOs.add(ReleaseContractLineVO.builder()
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

        List<ReleaseExistingColorInstructionVO> existingColorInstructions = new ArrayList<>();
        if (contract.getCustomerId() != null) {
            LambdaQueryWrapperX<ColorInstructionDO> query = new LambdaQueryWrapperX<ColorInstructionDO>()
                    .eq(ColorInstructionDO::getCustomerId, contract.getCustomerId())
                    .in(ColorInstructionDO::getStatus, List.of("CONFIRMED", "IN_PRODUCTION"));
            if (CollUtil.isNotEmpty(productCodes)) {
                query.in(ColorInstructionDO::getProductCode, productCodes);
            }
            List<ColorInstructionDO> instructions = colorInstructionMapper.selectList(query);
            if (CollUtil.isNotEmpty(instructions)) {
                for (ColorInstructionDO inst : instructions) {
                    existingColorInstructions.add(ReleaseExistingColorInstructionVO.builder()
                            .id(inst.getId())
                            .instructionNo(inst.getInstructionNo())
                            .status(inst.getStatus())
                            .productCode(inst.getProductCode())
                            .productName(inst.getProductName())
                            .build());
                }
            }
        }

        return ReleaseFromContractRespVO.builder()
                .contractId(contract.getId())
                .contractNo(contract.getContractNo())
                .clientName(contract.getClientName())
                .lines(lineVOs)
                .existingColorInstructions(existingColorInstructions)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = "生产管理", subType = "下达生产", bizNo = "{{#reqVO.contractId}}",
            success = "下达生产 合同【{{#contractNo}}】")
    public ReleaseProductionRespVO releaseProduction(ReleaseProductionReqVO reqVO) {
        // 1. 合同校验
        if (reqVO == null || reqVO.getContractId() == null) {
            throw exception(SALES_CONTRACT_NOT_EXISTS);
        }
        SalesContractDO contract = salesContractMapper.selectById(reqVO.getContractId());
        if (contract == null) {
            throw exception(SALES_CONTRACT_NOT_EXISTS);
        }
        if ("CANCELLED".equalsIgnoreCase(contract.getStatus())) {
            throw exception(SALES_CONTRACT_CANCELLED);
        }

        List<ReleaseGroupVO> groups = reqVO.getGroups();
        if (CollUtil.isEmpty(groups)) {
            return new ReleaseProductionRespVO();
        }

        // 查询该合同所有合同行用于校验归属与取快照
        List<SalesContractItemDO> contractItems = salesContractItemMapper.selectListByContractId(contract.getId());
        Map<Long, SalesContractItemDO> itemMap = contractItems.stream()
                .collect(Collectors.toMap(SalesContractItemDO::getId, Function.identity()));

        // 2. 超下达拦截：逐行条件更新 released_qty（WHERE released_qty + #{delta} <= quantity）
        for (ReleaseGroupVO group : groups) {
            if (CollUtil.isEmpty(group.getLines())) {
                continue;
            }
            for (ReleaseGroupLineVO line : group.getLines()) {
                SalesContractItemDO item = itemMap.get(line.getContractLineId());
                if (item == null) {
                    throw exception(CONTRACT_LINE_OVER_RELEASED, line.getContractLineId());
                }
                BigDecimal releaseQty = line.getReleaseQty() != null ? line.getReleaseQty() : BigDecimal.ZERO;
                int affected = salesContractItemMapper.updateReleasedQtyIncrement(line.getContractLineId(), releaseQty);
                if (affected == 0) {
                    throw exception(CONTRACT_LINE_OVER_RELEASED, line.getContractLineId());
                }
            }
        }

        ReleaseProductionRespVO resp = new ReleaseProductionRespVO();
        List<String> greigeOrderNos = new ArrayList<>();
        List<Long> dyeingOrderIds = new ArrayList<>();
        List<String> inspectionCodes = new ArrayList<>();

        // 3. 对每个 group 生成打色单（若需）、胚布单、染色单、胚布检验单
        for (ReleaseGroupVO group : groups) {
            if (CollUtil.isEmpty(group.getLines())) {
                continue;
            }

            // 产品档案ID解析
            Long productId = group.getProductId();
            if (productId == null && StringUtils.hasText(group.getProductCode())) {
                ProductDO prod = productMapper.selectByCode(group.getProductCode());
                if (prod != null) {
                    productId = prod.getId();
                }
            }

            // 处理打色单三态
            String mode = StringUtils.hasText(group.getColorInstructionMode())
                    ? group.getColorInstructionMode().trim().toUpperCase() : "NONE";
            Long colorInstructionId = null;
            String colorInstructionNo = null;

            if ("EXISTING".equals(mode)) {
                Long existingId = group.getExistingColorInstructionId();
                if (existingId == null) {
                    throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED, existingId);
                }
                ColorInstructionDO instruction = colorInstructionMapper.selectById(existingId);
                if (instruction == null || (!"CONFIRMED".equalsIgnoreCase(instruction.getStatus())
                        && !"IN_PRODUCTION".equalsIgnoreCase(instruction.getStatus()))) {
                    throw exception(COLOR_INSTRUCTION_STATUS_NOT_ALLOWED, existingId);
                }
                colorInstructionId = instruction.getId();
                colorInstructionNo = instruction.getInstructionNo();
            } else if ("NEW".equals(mode)) {
                ReleaseNewColorInstructionVO newInstVO = group.getNewColorInstruction();
                if (newInstVO == null || CollUtil.isEmpty(newInstVO.getColors())) {
                    throw exception(COLOR_INSTRUCTION_LIGHT_PRIMARY_REQUIRED);
                }
                for (ReleaseNewColorVO colorVO : newInstVO.getColors()) {
                    if (!StringUtils.hasText(colorVO.getLightPrimary())) {
                        throw exception(COLOR_INSTRUCTION_LIGHT_PRIMARY_REQUIRED);
                    }
                }

                // 组装并创建打色单
                List<ColorInstructionColorSaveReqVO> newColors = new ArrayList<>();
                for (ReleaseNewColorVO nc : newInstVO.getColors()) {
                    String colorNo = StringUtils.hasText(nc.getCustomerColorNo())
                            ? nc.getCustomerColorNo() : "C01";
                    String colorName = StringUtils.hasText(nc.getColorName())
                            ? nc.getColorName() : colorNo;
                    newColors.add(ColorInstructionColorSaveReqVO.builder()
                            .customerColorNo(colorNo)
                            .colorName(colorName)
                            .lightPrimary(nc.getLightPrimary())
                            .factoryColorNo(nc.getFactoryColorNo())
                            .colorCardRef(nc.getColorCardRef())
                            .status("PENDING")
                            .round(1)
                            .build());
                }

                ColorInstructionSaveReqVO saveReqVO = ColorInstructionSaveReqVO.builder()
                        .customerId(contract.getCustomerId())
                        .clientName(contract.getClientName())
                        .productId(productId)
                        .productCode(group.getProductCode())
                        .productName(group.getProductName())
                        .composition(group.getComposition())
                        .width(group.getWidth())
                        .gsm(group.getGsm())
                        .labFactoryId(newInstVO.getLabFactoryId())
                        .instructionDate(LocalDate.now())
                        .colors(newColors)
                        .build();

                Long newId = colorInstructionService.createColorInstruction(saveReqVO);
                resp.getColorInstructions().add(new ReleaseOrderSimpleVO(newId));
                // NEW 模式零回写，胚布/染色不引用
                colorInstructionId = null;
                colorInstructionNo = null;
            }

            // 计算组内最大损耗率与下达量合计
            BigDecimal maxLossRate = group.getLines().stream()
                    .map(ReleaseGroupLineVO::getLossRate)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal groupTotalQty = group.getLines().stream()
                    .map(ReleaseGroupLineVO::getReleaseQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 4. 创建胚布单（ACTIVE）
            GreigeOrderSaveReqVO greigeReq = new GreigeOrderSaveReqVO();
            greigeReq.setSalesContractId(contract.getId());
            greigeReq.setContractNo(contract.getContractNo());
            greigeReq.setGreigeFactoryId(reqVO.getGreigeFactoryId());
            greigeReq.setDyeingFactoryId(reqVO.getDyeingFactoryId());
            greigeReq.setOrderDate(LocalDate.now());
            greigeReq.setProductId(productId);
            greigeReq.setProductCode(group.getProductCode());
            greigeReq.setProductName(group.getProductName());
            greigeReq.setComposition(group.getComposition());
            greigeReq.setWidth(group.getWidth());
            greigeReq.setGsm(group.getGsm());
            greigeReq.setStatus("ACTIVE");
            greigeReq.setProductionMode("OUTSOURCE");
            greigeReq.setSuggestedLoss(maxLossRate);
            greigeReq.setFinishedQty(groupTotalQty);
            greigeReq.setPlannedQty(groupTotalQty);
            greigeReq.setColorInstructionId(colorInstructionId);
            greigeReq.setColorInstructionNo(colorInstructionNo);

            List<GreigeOrderColorSaveReqVO> greigeColors = new ArrayList<>();
            for (ReleaseGroupLineVO line : group.getLines()) {
                GreigeOrderColorSaveReqVO gc = new GreigeOrderColorSaveReqVO();
                gc.setColor(line.getCustomerColor());
                gc.setFinishedQty(line.getReleaseQty());
                gc.setLossRate(line.getLossRate() != null ? line.getLossRate() : maxLossRate);
                gc.setContractLineId(line.getContractLineId());
                SalesContractItemDO item = itemMap.get(line.getContractLineId());
                if (item != null) {
                    gc.setContractLineNo(item.getLineNo());
                    if (!StringUtils.hasText(gc.getColor())) {
                        gc.setColor(item.getCustomerColor());
                    }
                }
                greigeColors.add(gc);
            }
            greigeReq.setColors(greigeColors);

            Long greigeOrderId = greigeOrderService.createGreigeOrder(greigeReq);
            String greigeOrderNo = null;
            try {
                GreigeOrderRespVO greigeOrderResp = greigeOrderService.getGreigeOrder(greigeOrderId);
                if (greigeOrderResp != null) {
                    greigeOrderNo = greigeOrderResp.getOrderNo();
                }
            } catch (Exception ignored) {
            }
            resp.getGreigeOrders().add(new ReleaseOrderSimpleVO(greigeOrderId, greigeOrderNo));
            if (greigeOrderNo != null) {
                greigeOrderNos.add(greigeOrderNo);
            }

            // 5. 创建染色单（ACTIVE）
            DyeingOrderSaveReqVO dyeingReq = new DyeingOrderSaveReqVO();
            dyeingReq.setSalesContractId(contract.getId());
            dyeingReq.setContractNo(contract.getContractNo());
            dyeingReq.setCustomerId(contract.getCustomerId());
            dyeingReq.setClientName(contract.getClientName());
            dyeingReq.setDyeingFactoryId(reqVO.getDyeingFactoryId());
            dyeingReq.setOrderDate(LocalDate.now());
            dyeingReq.setProductCode(group.getProductCode());
            dyeingReq.setProductName(group.getProductName());
            dyeingReq.setComposition(group.getComposition());
            dyeingReq.setWidth(group.getWidth());
            dyeingReq.setGsm(group.getGsm());
            dyeingReq.setStatus("ACTIVE");
            dyeingReq.setColorInstructionId(colorInstructionId);
            dyeingReq.setColorInstructionNo(colorInstructionNo);

            List<DyeingColorSaveReqVO> dyeingColors = new ArrayList<>();
            for (ReleaseGroupLineVO line : group.getLines()) {
                DyeingColorSaveReqVO dc = new DyeingColorSaveReqVO();
                dc.setCustomerColorNo(line.getCustomerColor());
                dc.setColorName(line.getCustomerColor());
                dc.setQuantity(line.getReleaseQty());
                dc.setContractLineId(line.getContractLineId());
                SalesContractItemDO item = itemMap.get(line.getContractLineId());
                if (item != null) {
                    dc.setContractLineNo(item.getLineNo());
                    dc.setContractQty(item.getQuantity());
                    if (!StringUtils.hasText(dc.getCustomerColorNo())) {
                        dc.setCustomerColorNo(item.getCustomerColor());
                        dc.setColorName(item.getCustomerColor());
                    }
                }
                dyeingColors.add(dc);
            }
            dyeingReq.setColors(dyeingColors);

            Long dyeingOrderId = dyeingOrderService.createDyeingOrder(dyeingReq);
            resp.getDyeingOrders().add(new ReleaseOrderSimpleVO(dyeingOrderId));
            dyeingOrderIds.add(dyeingOrderId);

            // 6. 创建胚布检验单（type=GREIGE, status=DRAFT）
            InspectionOrderSaveReqVO inspectReq = new InspectionOrderSaveReqVO();
            inspectReq.setType("GREIGE");
            inspectReq.setStatus("DRAFT");
            inspectReq.setPackageMode("ROLL");
            inspectReq.setProductId(productId);
            inspectReq.setGreigeOrderId(greigeOrderId);
            inspectReq.setSalesContractId(contract.getId());
            inspectReq.setTargetWeight(groupTotalQty);
            inspectReq.setOrderDate(LocalDate.now());

            Long inspectionOrderId = inspectionOrderService.createInspectionOrder(inspectReq);
            String inspectCode = null;
            try {
                InspectionOrderRespVO inspectResp = inspectionOrderService.getInspectionOrder(inspectionOrderId);
                if (inspectResp != null) {
                    inspectCode = inspectResp.getCode();
                }
            } catch (Exception ignored) {
            }
            resp.getInspectionOrders().add(new ReleaseInspectionSimpleVO(inspectionOrderId, inspectCode, "GREIGE"));
            if (inspectCode != null) {
                inspectionCodes.add(inspectCode);
            }
        }

        // 记录业务日志变量
        try {
            LogRecordContext.putVariable("contractNo", contract.getContractNo());
            LogRecordContext.putVariable("greigeOrderNos", String.join(",", greigeOrderNos));
            LogRecordContext.putVariable("dyeingOrderIds", dyeingOrderIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            LogRecordContext.putVariable("inspectionCodes", String.join(",", inspectionCodes));
        } catch (Exception ignored) {
        }

        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFinishedInspection(Long dyeingOrderId) {
        // 1. 染色单校验：必须存在且状态为 COMPLETED
        if (dyeingOrderId == null) {
            throw exception(DYEING_ORDER_NOT_EXISTS);
        }
        DyeingOrderDO order = dyeingOrderMapper.selectById(dyeingOrderId);
        if (order == null) {
            throw exception(DYEING_ORDER_NOT_EXISTS);
        }
        if (!"COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw exception(DYEING_ORDER_STATUS_NOT_ALLOWED);
        }

        // 2. 查重校验：已存在同 dyeingOrderId 的 FINISHED 且未删除检验单则报错
        Long count = inspectionOrderMapper.selectCount(new LambdaQueryWrapperX<InspectionOrderDO>()
                .eq(InspectionOrderDO::getDyeingOrderId, dyeingOrderId)
                .eq(InspectionOrderDO::getType, "FINISHED"));
        if (count != null && count > 0) {
            throw exception(FINISHED_INSPECTION_ALREADY_EXISTS);
        }

        // 3. 计算目标重量：染色单所有颜色明细 quantity 合计
        List<DyeingColorDO> colors = dyeingColorMapper.selectListByOrderId(dyeingOrderId);
        BigDecimal targetWeight = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(colors)) {
            targetWeight = colors.stream()
                    .map(DyeingColorDO::getQuantity)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 4. 解析产品ID
        Long productId = null;
        if (StringUtils.hasText(order.getProductCode())) {
            ProductDO product = productMapper.selectByCode(order.getProductCode());
            if (product != null) {
                productId = product.getId();
            }
        }

        // 5. 调用 InspectionOrderService 创建成品检验单
        InspectionOrderSaveReqVO reqVO = new InspectionOrderSaveReqVO();
        reqVO.setType("FINISHED");
        reqVO.setStatus("DRAFT");
        reqVO.setPackageMode("ROLL");
        reqVO.setProductId(productId);
        reqVO.setDyeingOrderId(order.getId());
        reqVO.setSalesContractId(order.getSalesContractId());
        reqVO.setTargetWeight(targetWeight);
        reqVO.setOrderDate(LocalDate.now());

        return inspectionOrderService.createInspectionOrder(reqVO);
    }

}
