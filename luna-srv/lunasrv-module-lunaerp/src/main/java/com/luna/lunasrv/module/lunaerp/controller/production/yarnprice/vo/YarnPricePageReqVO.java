package com.luna.lunasrv.module.lunaerp.controller.production.yarnprice.vo;

import com.luna.lunasrv.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "生产管理 - 纱价分页请求 VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class YarnPricePageReqVO extends PageParam {

    @Schema(description = "纱线规格ID", example = "10")
    private Long yarnSpecId;

    @Schema(description = "纱线材质ID", example = "1")
    private Long yarnTypeId;

    @Schema(description = "价类（PURCHASE / MARKET）", example = "PURCHASE")
    private String priceType;

    @Schema(description = "生效日期起始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDateGe;

    @Schema(description = "生效日期截止")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDateLe;

}
