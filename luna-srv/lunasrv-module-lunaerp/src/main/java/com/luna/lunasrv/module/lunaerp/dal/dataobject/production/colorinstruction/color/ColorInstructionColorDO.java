package com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 生产管理 - 打色指示单颜色行子表 DO
 *
 * 表：lunaerp_production_color_instruction_color
 *
 * 颜色行是打色指示单聚合的一部分：无独立 Controller/Service/权限码，
 * 随单聚合保存。颜色身份字段与染色明细同口径（customerColorNo + colorName）。
 * 保存采用差异保存 + 行状态守卫（保留 REJECTED 轮次留痕）。
 *
 * @author luna
 */
@TableName(value = "lunaerp_production_color_instruction_color", autoResultMap = true)
@KeySequence("lunaerp_production_color_instruction_color_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class ColorInstructionColorDO extends TenantBaseDO {

    /**
     * 颜色行ID
     */
    private Long id;

    /**
     * 打色指示单ID（lunaerp_production_color_instruction.id）
     */
    private Long instructionId;

    /**
     * 行号（服务端按色号分组重排，同色号多轮相邻，最新轮在前）
     */
    private Integer lineNo;

    /**
     * 客户色号（与 DyeingColorDO/ShipmentItemDO 同口径）
     */
    private String customerColorNo;

    /**
     * 颜色名称（与上述同口径）
     */
    private String colorName;

    /**
     * 厂内色号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String factoryColorNo;

    /**
     * 色值 #rrggbb（列表色块/取色器；空则灰占位）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String colorHex;

    /**
     * 主灯源（字典 lunaerp_color_light_source，必填）
     */
    private String lightPrimary;

    /**
     * 辅灯源（字典 lunaerp_color_light_source，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lightSecondary;

    /**
     * 色卡参考（自由文本，如 Pantone TPX 19-4052）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String colorCardRef;

    /**
     * 样布照片 JSON 数组 [{"name":"...","url":"..."}] 或 URL 列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> samplePhotos;

    /**
     * 份数
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer copies;

    /**
     * 打色轮次（缺省 1，驳回重打 +1）
     */
    private Integer round;

    /**
     * 颜色行状态：PENDING 待结果 / APPROVED 通过 / REJECTED 驳回（字典 lunaerp_color_line_status）
     */
    private String status;

    /**
     * 驳回/否决意见
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String rejectReason;

    /**
     * 行备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
