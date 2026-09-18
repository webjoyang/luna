package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 业务管理-销售合同明细子表 DO
 *
 * 表：lunaerp_business_sales_contract_item
 *
 * 明细是合同聚合的一部分（ADR-0003）：无独立 Controller/Service/权限码，
 * 写操作随合同保存整体替换，删除合同时按 contract_id 级联逻辑删除。
 * amount（行金额）与主表 total_quantity/total_amount 由服务端权威重算：
 *   行金额 = quantity × unit_price + other_fees（null 数值按 0）。
 *
 * @author luna
 */
@TableName("lunaerp_business_sales_contract_item")
@KeySequence("lunaerp_business_sales_contract_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class SalesContractItemDO extends TenantBaseDO {

    /**
     * 明细ID
     */
    private Long id;
    /**
     * 合同ID（lunaerp_business_sales_contract.id，必填）
     */
    private Long contractId;
    /**
     * 行号（服务端按提交顺序重排，从 1 开始）
     */
    private Integer lineNo;
    /**
     * 产品编号（本票手填输入，03 票接入产品库远程搜索）
     */
    private String productCode;
    /**
     * 品名
     */
    private String productName;
    /**
     * 客户色号
     */
    private String customerColor;
    /**
     * 成分
     */
    private String composition;
    /**
     * 门幅
     */
    private String width;
    /**
     * 克重
     */
    private String weight;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 其他费用
     */
    private BigDecimal otherFees;
    /**
     * 行金额（服务端重算：数量×单价+其他费用）
     */
    private BigDecimal amount;
    /**
     * 行备注
     */
    private String remark;

}
