package com.luna.lunasrv.module.lunaerp.controller.production.yarnorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 生产管理-纱线采购测算 Response VO")
@Data
public class YarnOrderCalculateRespVO {

    @Schema(description = "采购测算明细列表")
    private List<YarnOrderCalculateItemRespVO> items;

    @Schema(description = "选中胚布单展示快照列表")
    private List<YarnOrderCalculateGreigeOrderRespVO> sourceGreigeOrders;

}
