package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigedispatch;

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
 * 生产管理-发染记录 DO（ADR-0033）
 *
 * 表：lunaerp_production_greige_dispatch_record
 * 挂胚布单的轻量流转记录：一次发运一条，产品快照取胚布单品种；
 * 创建写 PROCESS_OUT 负流水（扣胚布库存），删除写 PROCESS_OUT_REVERSAL 正红冲；
 * 可补填指向染色单与染厂，数量/日期/匹数/仓库创建后锁定。
 *
 * @author luna
 */
@TableName("lunaerp_production_greige_dispatch_record")
@KeySequence("lunaerp_production_greige_dispatch_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class GreigeDispatchRecordDO extends TenantBaseDO {

    /**
     * 发染记录ID
     */
    private Long id;

    /**
     * 关联胚布单ID（必填）
     */
    private Long greigeOrderId;

    /**
     * 关联染色单ID（可空，可后续补填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long dyeingOrderId;

    /**
     * 承染染厂供应商ID（可空，可后续补填）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long dyeingFactoryId;

    /**
     * 染厂公司名快照
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String dyeingFactoryName;

    /**
     * 出库仓库ID（必填，仅可选启用仓）
     */
    private Long warehouseId;

    /**
     * 仓库名称快照（创建时保存）
     */
    private String warehouseName;

    /**
     * 产品ID（快照自胚布单）
     */
    private Long productId;

    /**
     * 产品编码快照（快照自胚布单）
     */
    private String productCode;

    /**
     * 产品名称快照（快照自胚布单）
     */
    private String productName;

    /**
     * 发染日期（必填，创建后锁定不可改）
     */
    private LocalDate dispatchDate;

    /**
     * 发染数量 KG（必填，创建后锁定不可改）
     */
    private BigDecimal quantity;

    /**
     * 匹数（可空，创建后锁定不可改）
     */
    private Integer pieces;

    /**
     * 关联库存流水ID（创建时写入的 PROCESS_OUT 流水）
     */
    private Long stockLedgerId;

    /**
     * 备注（可改）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
