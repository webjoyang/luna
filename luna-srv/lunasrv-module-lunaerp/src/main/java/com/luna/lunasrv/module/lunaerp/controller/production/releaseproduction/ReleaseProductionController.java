package com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseFromContractRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.releaseproduction.vo.ReleaseProductionRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.releaseproduction.ReleaseProductionService;
import com.mzt.logapi.starter.annotation.LogRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 下达生产编排")
@RestController
@RequestMapping("/lunaerp/production/release-production")
@Validated
public class ReleaseProductionController {

    @Resource
    private ReleaseProductionService releaseProductionService;

    @GetMapping("/from-contract")
    @Operation(summary = "从销售合同预填下达生产信息")
    @Parameter(name = "contractId", description = "销售合同ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:release-production:query')")
    public CommonResult<ReleaseFromContractRespVO> getReleaseFromContract(@RequestParam("contractId") Long contractId) {
        return success(releaseProductionService.getReleaseFromContract(contractId));
    }

    @PostMapping("/release")
    @Operation(summary = "下达生产（创建胚布单、染色单、检验单并更新合同行已下达量）")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:release-production:release')")
    @LogRecord(type = "生产管理", subType = "下达生产", bizNo = "{{#reqVO.contractId}}",
            success = "下达生产 合同【{{#reqVO.contractId}}】")
    public CommonResult<ReleaseProductionRespVO> releaseProduction(@Valid @RequestBody ReleaseProductionReqVO reqVO) {
        return success(releaseProductionService.releaseProduction(reqVO));
    }

}
