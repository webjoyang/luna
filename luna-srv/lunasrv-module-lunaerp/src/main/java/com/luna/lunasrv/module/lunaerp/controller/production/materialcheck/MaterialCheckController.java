package com.luna.lunasrv.module.lunaerp.controller.production.materialcheck;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckRespVO;
import com.luna.lunasrv.module.lunaerp.service.production.materialcheck.MaterialCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

@Tag(name = "生产管理 - 齐套检查")
@RestController
@RequestMapping("/lunaerp/production/material-check")
@Validated
public class MaterialCheckController {

    @Resource
    private MaterialCheckService materialCheckService;

    @PostMapping("/run")
    @Operation(summary = "齐套检查只读试算")
    @PreAuthorize("@ss.hasPermission('lunaerp:production:material-check:query')")
    public CommonResult<MaterialCheckRespVO> runMaterialCheck(@RequestBody(required = false) MaterialCheckReqVO reqVO) {
        if (reqVO == null) {
            reqVO = new MaterialCheckReqVO();
        }
        return success(materialCheckService.calculateMaterialCheck(reqVO));
    }

}
