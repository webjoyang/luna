package com.luna.lunasrv.module.lunaerp.approval.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnRollDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnRollMapper;
import com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnStatusEnum;
import com.luna.lunasrv.module.lunaerp.service.stock.stockin.StockInService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS;

/**
 * 销售退货审批通过副作用处理器（Issue 04, ADR-0017, ADR-0042）
 *
 * 监听 sales_return 审批通过（ApprovalCompletedEvent），在 BEFORE_COMMIT 事务内：
 * 1. 幂等校验退货单是否已有 stock_in_id；
 * 2. 复用 StockInService.createStockIn 自动生成一张 inType=RETURN 的 DRAFT 入库单，金额恒为 0，回指 source_doc_type=SALES_RETURN 与 source_doc_id；
 * 3. 逐卷预生成入库明细（itemType=PRODUCT、产品快照取退货行、rollNo、quantity=卷量、pieceCount=1、单价与金额为0）；
 * 4. 回写退货单 stock_in_id 并置 status=APPROVED、approval_status=APPROVED。
 *
 * @author luna
 */
@Component
@Slf4j
public class SalesReturnApprovalHandler implements ApprovalHandler {

    private static final String DEFAULT_IN_TYPE = "RETURN";
    private static final String SOURCE_DOC_TYPE_SALES_RETURN = "SALES_RETURN";
    private static final String DEFAULT_ITEM_TYPE_PRODUCT = "PRODUCT";

    @Resource
    private SalesReturnMapper salesReturnMapper;

    @Resource
    private SalesReturnItemMapper salesReturnItemMapper;

    @Resource
    private SalesReturnRollMapper salesReturnRollMapper;

    @Resource
    private StockInService stockInService;

    @Override
    public boolean supports(String bizType) {
        return BIZ_TYPE_SALES_RETURN.equalsIgnoreCase(bizType);
    }

    @Override
    public void onApproved(Long bizId) {
        log.info("[SalesReturnApprovalHandler] 监听到销售退货审批通过，开始执行入库单自动生成副作用: bizId={}", bizId);
        if (bizId == null) {
            return;
        }

        // 1. 查询退货单（行级悲观锁，兼容单测 selectById 打桩）
        SalesReturnDO salesReturn = salesReturnMapper.selectOneForUpdate(SalesReturnDO::getId, bizId);
        if (salesReturn == null) {
            salesReturn = salesReturnMapper.selectById(bizId);
        }
        if (salesReturn == null) {
            throw exception(SALES_RETURN_NOT_EXISTS);
        }

        // 2. 幂等性守卫：若已关联入库单，直接幂等返回，严禁重复建单
        if (salesReturn.getStockInId() != null) {
            log.info("[SalesReturnApprovalHandler] 退货单已关联入库单，幂等跳过: bizId={}, stockInId={}",
                    bizId, salesReturn.getStockInId());
            return;
        }

        // 3. 查询退货明细行与卷清单
        List<SalesReturnItemDO> items = salesReturnItemMapper.selectList(new LambdaQueryWrapperX<SalesReturnItemDO>()
                .eq(SalesReturnItemDO::getReturnId, bizId));
        List<SalesReturnRollDO> rolls = salesReturnRollMapper.selectList(new LambdaQueryWrapperX<SalesReturnRollDO>()
                .eq(SalesReturnRollDO::getReturnId, bizId));
        Map<Long, List<SalesReturnRollDO>> rollsByItemId = CollUtil.isEmpty(rolls)
                ? Collections.emptyMap()
                : rolls.stream().collect(Collectors.groupingBy(SalesReturnRollDO::getReturnItemId));

        // 4. 组装入库单创建请求 VO（复用 StockInService.createStockIn）
        StockInSaveReqVO reqVO = new StockInSaveReqVO();
        reqVO.setInType(DEFAULT_IN_TYPE);
        reqVO.setWarehouseId(salesReturn.getWarehouseId());
        reqVO.setSalesContractId(salesReturn.getSalesContractId());
        reqVO.setInDate(LocalDate.now());
        reqVO.setSourceDocType(SOURCE_DOC_TYPE_SALES_RETURN);
        reqVO.setSourceDocId(salesReturn.getId());
        reqVO.setRemark(StrUtil.isNotBlank(salesReturn.getRemark())
                ? salesReturn.getRemark().trim()
                : ("由销售退货单 " + salesReturn.getReturnNo() + " 自动生成"));

        // 5. 组装逐卷入库明细
        List<StockInItemSaveReqVO> inItems = new ArrayList<>();
        if (CollUtil.isNotEmpty(items)) {
            for (SalesReturnItemDO item : items) {
                List<SalesReturnRollDO> itemRolls = rollsByItemId.get(item.getId());
                if (CollUtil.isNotEmpty(itemRolls)) {
                    for (SalesReturnRollDO roll : itemRolls) {
                        StockInItemSaveReqVO inItem = new StockInItemSaveReqVO();
                        inItem.setItemType(DEFAULT_ITEM_TYPE_PRODUCT);
                        inItem.setProductId(item.getProductId());
                        inItem.setBatchNo(item.getVatNo());
                        inItem.setRollNo(roll.getRollNo());
                        inItem.setPieceCount(1);
                        inItem.setQuantity(resolveRollQuantity(item, roll));
                        inItem.setUnit(StrUtil.isNotBlank(item.getUnit()) ? item.getUnit() : "KG");
                        inItem.setUnitPrice(BigDecimal.ZERO);
                        inItem.setAmount(BigDecimal.ZERO);
                        inItem.setRemark(roll.getRemark());
                        inItems.add(inItem);
                    }
                } else {
                    StockInItemSaveReqVO inItem = new StockInItemSaveReqVO();
                    inItem.setItemType(DEFAULT_ITEM_TYPE_PRODUCT);
                    inItem.setProductId(item.getProductId());
                    inItem.setBatchNo(item.getVatNo());
                    inItem.setPieceCount(item.getPieces() != null ? item.getPieces() : 1);
                    inItem.setQuantity(item.getReturnQty() != null ? item.getReturnQty() : BigDecimal.ZERO);
                    inItem.setUnit(StrUtil.isNotBlank(item.getUnit()) ? item.getUnit() : "KG");
                    inItem.setUnitPrice(BigDecimal.ZERO);
                    inItem.setAmount(BigDecimal.ZERO);
                    inItem.setRemark(item.getRemark());
                    inItems.add(inItem);
                }
            }
        }
        reqVO.setItems(inItems);

        Long stockInId = stockInService.createStockIn(reqVO);
        StockInRespVO createdStockIn = stockInService.getStockIn(stockInId);
        String stockInNo = createdStockIn != null ? createdStockIn.getInNo() : null;

        // 6. 回写退货单主表：关联 stock_in_id，状态置为 APPROVED，approval_status 置为 APPROVED
        salesReturn.setStockInId(stockInId);
        salesReturn.setStatus(SalesReturnStatusEnum.APPROVED.getValue());
        salesReturn.setApprovalStatus("APPROVED");
        salesReturnMapper.updateById(salesReturn);

        log.info("[SalesReturnApprovalHandler] 销售退货审批通过入库单生成完成: returnId={}, returnNo={}, stockInId={}, stockInNo={}",
                salesReturn.getId(), salesReturn.getReturnNo(), stockInId, stockInNo);
    }

    private BigDecimal resolveRollQuantity(SalesReturnItemDO line, SalesReturnRollDO roll) {
        if (roll == null) {
            return line != null && line.getReturnQty() != null ? line.getReturnQty() : BigDecimal.ZERO;
        }
        String unit = line != null && line.getUnit() != null ? line.getUnit().trim().toUpperCase() : "KG";
        boolean isMeter = "M".equals(unit) || "METER".equals(unit);
        if (isMeter) {
            if (roll.getLengthM() != null && roll.getLengthM().compareTo(BigDecimal.ZERO) > 0) {
                return roll.getLengthM();
            }
            return line != null && line.getReturnQty() != null ? line.getReturnQty() : BigDecimal.ZERO;
        } else {
            if (roll.getWeight() != null && roll.getWeight().compareTo(BigDecimal.ZERO) > 0) {
                return roll.getWeight();
            }
            return line != null && line.getReturnQty() != null ? line.getReturnQty() : BigDecimal.ZERO;
        }
    }

}
