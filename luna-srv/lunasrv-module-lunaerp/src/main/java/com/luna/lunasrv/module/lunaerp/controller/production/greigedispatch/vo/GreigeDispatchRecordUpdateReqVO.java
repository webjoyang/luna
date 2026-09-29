package com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生产管理 - 发染记录更新 Request VO（受限编辑：仅允许改染色单/染厂/备注）
 *
 * @author luna
 */
@Schema(description = "生产管理 - 发染记录更新 Request VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreigeDispatchRecordUpdateReqVO {

    @Schema(description = "发染记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "发染记录ID不能为空")
    private Long id;

    @Schema(description = "染色单ID（可空，补填或修改）", example = "2001")
    private Long dyeingOrderId;

    @Schema(description = "承染染厂供应商ID（可空）", example = "3001")
    private Long dyeingFactoryId;

    @Schema(description = "染厂公司名快照", example = "XX染厂")
    private String dyeingFactoryName;

    @Schema(description = "备注", example = "已确认交期")
    @Size(max = 1000, message = "备注长度不能超过 1000 个字符")
    private String remark;

}
