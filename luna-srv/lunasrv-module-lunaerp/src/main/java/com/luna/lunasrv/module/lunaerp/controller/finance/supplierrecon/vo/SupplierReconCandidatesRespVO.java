package com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 供应商对账候选集合 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReconCandidatesRespVO {

    @Schema(description = "全部候选明细列表（加工费与采购货款混排）")
    @Builder.Default
    private List<SupplierReconCandidateItemRespVO> items = new ArrayList<>();

    @Schema(description = "加工费候选明细列表")
    @Builder.Default
    private List<SupplierReconCandidateItemRespVO> processItems = new ArrayList<>();

    @Schema(description = "采购货款候选明细列表")
    @Builder.Default
    private List<SupplierReconCandidateItemRespVO> purchaseItems = new ArrayList<>();

}
