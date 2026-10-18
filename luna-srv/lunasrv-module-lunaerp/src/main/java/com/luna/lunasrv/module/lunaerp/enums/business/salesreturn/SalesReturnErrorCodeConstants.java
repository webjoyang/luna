package com.luna.lunasrv.module.lunaerp.enums.business.salesreturn;

import com.luna.lunasrv.framework.common.exception.ErrorCode;

/**
 * 销售退货模块错误码常量（1-030-037-xxx 段，ADR-0042）
 *
 * @author luna
 */
public interface SalesReturnErrorCodeConstants {

    ErrorCode SALES_RETURN_NOT_EXISTS = new ErrorCode(1_030_037_000, "退货单不存在");
    ErrorCode SALES_RETURN_NO_DUPLICATE = new ErrorCode(1_030_037_001, "退货单号【{}】已存在");
    ErrorCode SALES_RETURN_STATUS_NOT_ALLOWED = new ErrorCode(1_030_037_002, "退货单当前状态不允许该操作");
    ErrorCode SALES_RETURN_SHIPMENT_NOT_COMPLETED = new ErrorCode(1_030_037_003, "发货单不存在或未审核");
    ErrorCode SALES_RETURN_ITEM_LINEAGE_INVALID = new ErrorCode(1_030_037_004, "退货行未挂有效发货明细或不属于该发货单");
    ErrorCode SALES_RETURN_QTY_EXCEED_AVAILABLE = new ErrorCode(1_030_037_005, "退货数量超过可退量：需求【{}】，可用【{}】");
    ErrorCode SALES_RETURN_AMOUNT_EXCEED_AVAILABLE = new ErrorCode(1_030_037_006, "退货金额超过可退额上限");
    ErrorCode SALES_RETURN_ROLL_INVALID = new ErrorCode(1_030_037_007, "退货卷信息非法：卷号缺失、重量米数双空或卷血缘不属于该发货单");
    ErrorCode SALES_RETURN_APPROVAL_NOT_APPROVED = new ErrorCode(1_030_037_008, "审批未通过不可进行到货质检或财务过账");
    ErrorCode SALES_RETURN_STOCK_IN_STATUS_INVALID = new ErrorCode(1_030_037_009, "退货入库单缺失或状态异常");
    ErrorCode SALES_RETURN_ROLL_QC_UNRESOLVED = new ErrorCode(1_030_037_010, "存在未质检判定的退货卷");
    ErrorCode SALES_RETURN_RESTOCK_QTY_MISMATCH = new ErrorCode(1_030_037_011, "可入库卷缺行单位对应数量或入库汇总不一致");
    ErrorCode SALES_RETURN_STOCK_IN_NOT_COMPLETED = new ErrorCode(1_030_037_012, "存在可入库卷但入库单未审核完成，不可财务确认");
    ErrorCode SALES_RETURN_CURRENCY_MISMATCH = new ErrorCode(1_030_037_013, "退货币种与发货单不一致");
    ErrorCode SALES_RETURN_EXCHANGE_RATE_MISSING = new ErrorCode(1_030_037_014, "原发货入账汇率缺失，无法执行退货冲减");

}
