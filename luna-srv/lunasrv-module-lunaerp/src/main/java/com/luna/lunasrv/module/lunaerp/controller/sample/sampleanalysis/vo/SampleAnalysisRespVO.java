package com.luna.lunasrv.module.lunaerp.controller.sample.sampleanalysis.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 样品管理-样品分析 Response VO")
@Data
public class SampleAnalysisRespVO {

    @Schema(description = "样品分析ID", example = "1024")
    private Long id;

    @Schema(description = "样品编号（SA+年份+4位顺序号）", example = "SA20260001")
    private String sampleAnalysisNo;

    @Schema(description = "客户ID（可空，逻辑关联客户档案）", example = "100")
    private Long customerId;

    @Schema(description = "客户名称（快照或手填）", example = "某某纺织有限公司")
    private String clientName;

    @Schema(description = "客户联系人（自由文本）", example = "张三")
    private String clientContact;

    @Schema(description = "客户款号（自由文本）", example = "STY-2026")
    private String clientStyleNo;

    @Schema(description = "产品名称（自由文本）", example = "全棉双面汗布")
    private String productName;

    @Schema(description = "成分（自由文本）", example = "100%棉")
    private String composition;

    @Schema(description = "门幅（CM）", example = "180.00")
    private BigDecimal width;

    @Schema(description = "克重（GSM）", example = "200.00")
    private BigDecimal weight;

    @Schema(description = "米长（每公斤米数，服务端计算）", example = "3.33")
    private BigDecimal meterLength;

    @Schema(description = "工序（字典 lunaerp_sample_process）", example = "DYEING")
    private String process;

    @Schema(description = "双染工艺（字典 lunaerp_sample_dyeing_process）", example = "SINGLE_T")
    private String dyeingProcess;

    @Schema(description = "机型（字典 lunaerp_sample_machine_type）", example = "CIRCULAR_KNIT")
    private String machineType;

    @Schema(description = "实测产量（文本）", example = "120KG/天")
    private String output;

    @Schema(description = "花号", example = "H-01")
    private String flowerNo;

    @Schema(description = "白胚门幅", example = "185cm")
    private String whiteWidth;

    @Schema(description = "白胚克重", example = "190g")
    private String whiteWeight;

    @Schema(description = "花宽", example = "20cm")
    private String flowerWidth;

    @Schema(description = "花高", example = "15cm")
    private String flowerHeight;

    @Schema(description = "齿轮", example = "72T")
    private String gear;

    @Schema(description = "布架", example = "A架")
    private String clothRack;

    @Schema(description = "开机转数", example = "18rpm")
    private String startSpeed;

    @Schema(description = "送纱盘", example = "3段")
    private String feederDisk;

    @Schema(description = "理论产量", example = "150KG/天")
    private String theoreticalOutput;

    @Schema(description = "线长", example = "9.6cm/50针")
    private String stitchLength;

    @Schema(description = "照片 URL 列表（最多 9 张）")
    private List<String> photos;

    @Schema(description = "首张照片缩略图 URL（列表展示）")
    private String firstPhotoUrl;

    @Schema(description = "三角排列画板数据")
    private Object stitchArrange;

    @Schema(description = "生产工费（元/KG）", example = "4.50")
    private BigDecimal productionCost;

    @Schema(description = "染整费（元/KG）", example = "6.00")
    private BigDecimal dyeingCost;

    @Schema(description = "其他费用（元/KG）", example = "1.00")
    private BigDecimal otherCost;

    @Schema(description = "损耗率（百分比数字，如 5 表示 5%）", example = "5.00")
    private BigDecimal lossRate;

    @Schema(description = "总成本（元/KG，服务端重算）", example = "32.50")
    private BigDecimal totalCost;

    @Schema(description = "对外报价（元/米）", example = "28.50")
    private BigDecimal quotedPrice;

    @Schema(description = "报价备注", example = "含税不含运费")
    private String quotedRemark;

    @Schema(description = "月结付款状态（false 未付 / true 已付）", example = "false")
    private Boolean isPaid;

    @Schema(description = "月结单价（元/条，缺省 30）", example = "30.00")
    private BigDecimal unitPrice;

    @Schema(description = "登记人用户ID（创建时写入快照）", example = "1")
    private Long creatorId;

    @Schema(description = "登记人姓名（创建时写入快照）", example = "张三")
    private String creatorName;

    @Schema(description = "备注", example = "来样手感偏硬")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
