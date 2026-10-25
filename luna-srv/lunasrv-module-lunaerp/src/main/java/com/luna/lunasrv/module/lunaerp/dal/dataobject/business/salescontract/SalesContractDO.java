package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 业务管理-销售合同主表 DO
 *
 * 表：lunaerp_business_sales_contract
 *
 * 本票（01）只有基本信息，不含商品明细；totalQuantity/totalAmount 固定为 0，
 * 由商品明细票（02）接管重算。合同编号唯一性见 ADR-0004（软删不释放编号）。
 *
 * @author luna
 */
@TableName("lunaerp_business_sales_contract")
@KeySequence("lunaerp_business_sales_contract_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
public class SalesContractDO extends TenantBaseDO {

    /**
     * 合同ID
     */
    private Long id;
    /**
     * 合同编号（CH+年份+4位顺序号，可手填，租户内全量唯一含已删除行，见 ADR-0004）
     */
    private String contractNo;
    /**
     * 客户ID（lunaerp_baseinfo_customer.id，必填）
     */
    private Long customerId;
    /**
     * 客户名称快照（选中客户时保存，必填）
     */
    private String clientName;
    /**
     * 客户联系人
     */
    private String clientContact;
    /**
     * 客户地址
     */
    private String clientAddress;
    /**
     * 客户合同号
     */
    private String clientContractNo;
    /**
     * 业务员用户ID（system_users.id，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long salespersonUserId;
    /**
     * 归属部门（数据范围 dept 列）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long deptId;
    /**
     * 签订日期（必填，创建缺省当天）
     */
    private LocalDate signDate;
    /**
     * 交货日期（可空）
     */
    private LocalDate deliveryDate;
    /**
     * 单位（字典 lunaerp_sales_contract_unit：KG/M），创建缺省 KG
     */
    private String unit;
    /**
     * 币种（字典 lunaerp_sales_contract_currency：RMB/USD/EUR/HKD），创建缺省 RMB
     */
    private String currency;
    /**
     * 贸易类型（字典 lunaerp_sales_contract_trade_type：DOMESTIC 内销 / EXPORT 外销），创建缺省 DOMESTIC
     */
    private String tradeType;
    /**
     * 订单属性（字典 lunaerp_sales_contract_order_type：BULK 大货 / SAMPLE 样布），创建缺省 BULK
     */
    private String orderType;
    /**
     * 总数量（本票固定 0，02 票按商品明细重算）
     */
    private BigDecimal totalQuantity;
    /**
     * 总金额（本票固定 0，02 票按商品明细重算）
     */
    private BigDecimal totalAmount;
    /**
     * 备注
     */
    private String remarks;
    /**
     * 合同状态（字典 lunaerp_sales_contract_status：ACTIVE 进行中 / COMPLETED 已完成 / CANCELLED 已取消），创建缺省 ACTIVE
     */
    private String status;

    /**
     * 审批状态（DRAFT 草稿 / PENDING 待提交 / APPROVING 审批中 / APPROVED 已通过 / REJECTED 已驳回 / CANCELLED 已撤回），缺省 DRAFT
     */
    private String approvalStatus;

    /**
     * 贸易术语（字典 lunaerp_incoterm：FOB/CIF/CFR/EXW，可空）
     */
    private String incoterm;

    /**
     * 起运港（可空）
     */
    private String portOfLoading;

    /**
     * 目的港（可空）
     */
    private String portOfDischarge;

    /**
     * 付款方式（字典 lunaerp_payment_term：TT/LC/DP/DA/OA，可空）
     */
    private String paymentTerm;

    /**
     * 来源报价单ID（可空）
     */
    private Long sourceQuotationId;

    /**
     * 来源报价单号快照（可空）
     */
    private String sourceQuotationNo;

}
