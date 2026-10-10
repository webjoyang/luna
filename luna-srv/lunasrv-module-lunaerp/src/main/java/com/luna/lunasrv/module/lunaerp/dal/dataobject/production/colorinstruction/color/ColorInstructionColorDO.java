package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 生产管理 - 打色指示单颜色行子表 DO
 *
 * @author luna
 */
@TableName("lunaerp_production_color_instruction_color")
@KeySequence("lunaerp_production_color_instruction_color_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColorInstructionColorDO extends TenantBaseDO {

    /**
     * 颜色行ID
     */
    @TableId
    private Long id;

    /**
     * 打色指示单ID（lunaerp_production_color_instruction.id）
     */
    private Long instructionId;

    /**
     * 行号（服务端重排，同色号多轮相邻、最新轮在前）
     */
    private Integer lineNo;

    /**
     * 客户色号（与染色明细/发货明细同口径）
     */
    private String customerColorNo;

    /**
     * 颜色名称
     */
    private String colorName;

    /**
     * 厂内色号
     */
    private String factoryColorNo;

    /**
     * 色值十六进制（如 #FF0000）
     */
    private String colorHex;

    /**
     * 主灯源（字典 lunaerp_color_light_source）
     */
    private String lightPrimary;

    /**
     * 辅灯源（字典 lunaerp_color_light_source）
     */
    private String lightSecondary;

    /**
     * 色卡参考（如 Pantone TPX 19-4052）
     */
    private String colorCardRef;

    /**
     * 样布照片 JSON 数组 [{"name":"...","url":"..."}]
     */
    private String samplePhotos;

    /**
     * 份数
     */
    private Integer copies;

    /**
     * 打色轮次（缺省 1，驳回重打 +1）
     */
    private Integer round;

    /**
     * 行状态（字典 lunaerp_color_line_status：PENDING 待结果 / APPROVED 通过 / REJECTED 驳回）
     */
    private String status;

    /**
     * 驳回意见
     */
    private String rejectReason;

    /**
     * 行备注
     */
    private String remark;

}
