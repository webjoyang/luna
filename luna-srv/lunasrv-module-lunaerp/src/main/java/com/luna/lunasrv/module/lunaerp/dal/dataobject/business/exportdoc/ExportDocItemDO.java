package com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 业务管理-出口单证明细行表 DO
 *
 * 表：lunaerp_business_export_doc_item
 *
 * @author luna
 */
@TableName("lunaerp_business_export_doc_item")
@KeySequence("lunaerp_business_export_doc_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExportDocItemDO extends TenantBaseDO {

    /**
     * 明细行ID
     */
    private Long id;
    /**
     * 所属单证套ID
     */
    private Long docId;
    /**
     * 行序
     */
    private Integer lineNo;
    /**
     * 中文品名
     */
    private String nameCn;
    /**
     * 英文品名
     */
    private String nameEn;
    /**
     * 海关编码 HS CODE
     */
    private String hsCode;
    /**
     * 规格（成分/门幅/颜色）
     */
    private String spec;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 单位（KG/M 等）
     */
    private String unit;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 金额（quantity * unit_price，服务端重算）
     */
    private BigDecimal amount;
    /**
     * 净重 KG
     */
    private BigDecimal netWeight;
    /**
     * 毛重 KG
     */
    private BigDecimal grossWeight;
    /**
     * 箱数/件数
     */
    private Integer packages;
    /**
     * 体积 CBM
     */
    private BigDecimal volume;
    /**
     * 原产地，缺省 CHINA
     */
    private String origin;
    /**
     * 行备注
     */
    private String remark;

}
