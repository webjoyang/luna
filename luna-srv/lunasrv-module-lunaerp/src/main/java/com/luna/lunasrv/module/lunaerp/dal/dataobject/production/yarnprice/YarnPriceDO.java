package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnprice;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 生产管理 - 纱价主档 DO
 *
 * @author luna
 */
@TableName("lunaerp_production_yarn_price")
@KeySequence("lunaerp_production_yarn_price_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YarnPriceDO extends TenantBaseDO {

    /**
     * 纱价主键ID
     */
    @TableId
    private Long id;

    /**
     * 关联纱线规格ID
     */
    private Long yarnSpecId;

    /**
     * 冗余纱线材质ID
     */
    private Long yarnTypeId;

    /**
     * 规格名称快照
     */
    private String specName;

    /**
     * 价类（PURCHASE 采购参考价 / MARKET 市场参考价）
     */
    private String priceType;

    /**
     * 单价，元/KG，必须 >0
     */
    private BigDecimal price;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 来源（HAND 手工 / EXCEL 导入 / PURCHASE_AGG 采购聚合）
     */
    private String source;

    /**
     * 备注
     */
    private String remark;

}
