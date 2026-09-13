package com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 基本信息-胚布品种（坯型）表
 *
 * 一个坯可以染成多个颜色的成品，由「布种 + 成分 + 纱支 + 坯克重 + 坯门幅」
 * 五个识别属性唯一确定，{@link #matchKey} 是这五项按固定口径拼接的识别键。
 *
 * 注意：{@link #weightGsm} / {@link #widthCm} 是「坯」的参数，与成品的克重门幅不是一回事
 * （染整会缩率定型，成品克重门幅与坯不同）。
 *
 * 来源：oldserver/prisma/schema.prisma GreigeType(1047-1081)
 *
 * @author luna
 */
@TableName("lunaerp_baseinfo_product_greige_type")
@KeySequence("lunaerp_baseinfo_product_greige_type_seq") // 用于 PostgreSQL 的主键自增
@Data
@EqualsAndHashCode(callSuper = true)
public class GreigeTypeDO extends TenantBaseDO {

    /**
     * 状态：启用
     */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /**
     * 状态：停用（不参与自动识别与选择器列表）
     */
    public static final String STATUS_INACTIVE = "INACTIVE";

    /**
     * 胚布品种ID
     */
    private Long id;
    /**
     * 品种编号（租户内流水号，如 GT-0001，不随属性变化）
     */
    private String code;
    /**
     * 显示名称（如 30S NR罗马布 260g×160cm）
     */
    private String name;
    /**
     * 识别键（布种+成分+纱支+坯克重+坯门幅拼接，用于查重与自动匹配）
     */
    private String matchKey;
    /**
     * 布种（取字典 FABRIC_TYPE）
     */
    private String fabricType;
    /**
     * 成分（如 NR / TR / CVC）
     */
    private String composition;
    /**
     * 纱支（如 30S）
     */
    private String yarnSpec;
    /**
     * 坯克重（g/m²，注意与成品克重不同）
     */
    private BigDecimal weightGsm;
    /**
     * 坯门幅（厘米，注意与成品门幅不同）
     */
    private BigDecimal widthCm;
    /**
     * 工艺类型（选填）
     */
    private String craftType;
    /**
     * 机型（选填）
     */
    private String machineType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态
     *
     * 枚举 {@link #STATUS_ACTIVE} / {@link #STATUS_INACTIVE}
     */
    private String status;

}
