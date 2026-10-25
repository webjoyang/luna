package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务管理-出口单证套主表 DO
 *
 * 表：lunaerp_business_export_doc
 *
 * @author luna
 */
@TableName("lunaerp_business_export_doc")
@KeySequence("lunaerp_business_export_doc_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExportDocDO extends TenantBaseDO {

    /**
     * 单证套ID
     */
    private Long id;
    /**
     * 单证套号（ED{yyyy}{4位流水}，租户内全量唯一含软删行，见 ADR-0004）
     */
    private String docNo;
    /**
     * 单证状态（字典 lunaerp_export_doc_status：DRAFT/FINALIZED），缺省 DRAFT
     */
    private String status;
    /**
     * 来源类型（MANUAL 手工 / CONTRACT 合同 / SHIPMENT 发货单），缺省 MANUAL
     */
    private String sourceType;
    /**
     * 来源合同ID
     */
    private Long contractId;
    /**
     * 来源合同号快照
     */
    private String contractNo;
    /**
     * 来源发货单ID
     */
    private Long shipmentId;
    /**
     * 来源发货单号快照
     */
    private String shipmentNo;
    /**
     * 买方客户ID（必填）
     */
    private Long customerId;
    /**
     * 买方客户名称快照
     */
    private String customerName;
    /**
     * 卖方英文名（签章/抬头，缺省取租户卖方配置）
     */
    private String sellerName;
    /**
     * 卖方英文地址
     */
    private String sellerAddress;
    /**
     * 卖方收款银行信息
     */
    private String sellerBankInfo;
    /**
     * 买方英文名（缺省取客户档案 name_en）
     */
    private String buyerName;
    /**
     * 买方英文地址（缺省取客户档案 address_en）
     */
    private String buyerAddress;
    /**
     * 买方银行信息（随单手填）
     */
    private String buyerBankInfo;
    /**
     * 贸易术语（字典 lunaerp_incoterm：FOB/CIF/CFR/EXW）
     */
    private String incoterm;
    /**
     * 起运港
     */
    private String portOfLoading;
    /**
     * 目的港
     */
    private String portOfDischarge;
    /**
     * 目的国
     */
    private String countryOfDest;
    /**
     * 运输方式（字典 lunaerp_transport_mode：SEA/AIR/LAND/EXPRESS）
     */
    private String transportMode;
    /**
     * 付款方式（字典 lunaerp_payment_term：TT/LC/DP/DA/OA）
     */
    private String paymentTerm;
    /**
     * 币种（复用字典 lunaerp_sales_contract_currency），缺省 USD
     */
    private String currency;
    /**
     * 唛头
     */
    private String shippingMark;
    /**
     * 形式发票 PI 专属元数据 JSON
     */
    private String piMeta;
    /**
     * 商业发票 CI 专属元数据 JSON
     */
    private String ciMeta;
    /**
     * 装箱单 PL 专属元数据 JSON
     */
    private String plMeta;
    /**
     * 备注
     */
    private String remarks;

}
