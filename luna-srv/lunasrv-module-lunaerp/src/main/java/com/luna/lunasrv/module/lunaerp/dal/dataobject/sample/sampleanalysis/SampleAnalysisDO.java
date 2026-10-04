package com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.luna.lunasrv.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 样品管理-样品分析主表 DO
 *
 * 表：lunaerp_sample_analysis
 *
 * @author luna
 */
@TableName(value = "lunaerp_sample_analysis", autoResultMap = true)
@KeySequence("lunaerp_sample_analysis_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class SampleAnalysisDO extends TenantBaseDO {

    /**
     * 样品分析ID
     */
    private Long id;

    /**
     * 样品编号（SA+年份+4位顺序号，可手填，租户内全量唯一含已删除行）
     */
    private String sampleAnalysisNo;

    /**
     * 客户ID（lunaerp_baseinfo_customer.id，可空）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long customerId;

    /**
     * 客户名称快照（手填或关联带出）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String clientName;

    /**
     * 客户联系人（自由文本）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String clientContact;

    /**
     * 客户款号（自由文本）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String clientStyleNo;

    /**
     * 产品名称（唯一必填项，自由文本）
     */
    private String productName;

    /**
     * 成分（自由文本）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String composition;

    /**
     * 门幅（CM）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal width;

    /**
     * 克重（GSM）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal weight;

    /**
     * 米长（每公斤米数，服务端计算，只读）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal meterLength;

    /**
     * 工序（字典 lunaerp_sample_process）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String process;

    /**
     * 双染工艺（字典 lunaerp_sample_dyeing_process）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String dyeingProcess;

    /**
     * 机型（字典 lunaerp_sample_machine_type）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String machineType;

    /**
     * 实测产量（文本）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String output;

    /**
     * 花号
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String flowerNo;

    /**
     * 白胚门幅
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String whiteWidth;

    /**
     * 白胚克重
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String whiteWeight;

    /**
     * 花宽
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String flowerWidth;

    /**
     * 花高
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String flowerHeight;

    /**
     * 齿轮
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String gear;

    /**
     * 布架
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String clothRack;

    /**
     * 开机转数
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String startSpeed;

    /**
     * 送纱盘
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String feederDisk;

    /**
     * 理论产量
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String theoreticalOutput;

    /**
     * 线长
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String stitchLength;

    /**
     * 照片 URL 列表（JSONB 数组，最多 9 张）
     */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private List<String> photos;

    /**
     * 三角排列画板数据（JSONB，结构 {columns, rows, data}）
     */
    @TableField(typeHandler = JacksonTypeHandler.class, updateStrategy = FieldStrategy.ALWAYS)
    private Object stitchArrange;

    /**
     * 生产工费（元/KG）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal productionCost;

    /**
     * 染整费（元/KG）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal dyeingCost;

    /**
     * 其他费用（元/KG）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal otherCost;

    /**
     * 损耗率（百分比数字，如 5 表示 5%）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal lossRate;

    /**
     * 总成本（元/KG，服务端重算）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal totalCost;

    /**
     * 对外报价（元/米）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal quotedPrice;

    /**
     * 报价备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String quotedRemark;

    /**
     * 月结付款状态（false 未付 / true 已付）
     */
    private Boolean isPaid;

    /**
     * 月结单价（元/条，缺省 30）
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal unitPrice;

    /**
     * 登记人用户ID（创建时写入快照）
     */
    private Long creatorId;

    /**
     * 登记人姓名（创建时写入快照）
     */
    private String creatorName;

    /**
     * 备注
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

}
