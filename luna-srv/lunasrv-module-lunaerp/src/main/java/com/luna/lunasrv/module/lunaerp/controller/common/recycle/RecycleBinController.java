package com.luna.lunasrv.module.lunaerp.controller.common.recycle;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandlerRegistry;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleRestoreReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;

@Tag(name = "管理后台 - 统一回收站")
@RestController
@RequestMapping("/lunaerp/common/recycle")
@Validated
public class RecycleBinController {

    @Resource
    private RecycleBinHandlerRegistry recycleBinHandlerRegistry;

    @GetMapping("/page")
    @Operation(summary = "获得回收站分页列表")
    @PreAuthorize("@ss.hasPermission('lunaerp:common:recycle:query')")
    public CommonResult<PageResult<RecycleItemRespVO>> getRecyclePage(@Valid RecyclePageReqVO reqVO) {
        RecycleBinHandler handler = recycleBinHandlerRegistry.getHandler(reqVO.getBizType());
        return success(handler.pageDeleted(reqVO));
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复回收站项")
    @PreAuthorize("@ss.hasPermission('lunaerp:common:recycle:restore')")
    public CommonResult<Boolean> restore(@Valid @RequestBody RecycleRestoreReqVO reqVO) {
        RecycleBinHandler handler = recycleBinHandlerRegistry.getHandler(reqVO.getBizType());
        handler.validateRestore(reqVO.getBizId());
        handler.restore(reqVO.getBizId());
        return success(true);
    }

    @DeleteMapping("/purge")
    @Operation(summary = "彻底删除回收站项")
    @Parameters({
            @Parameter(name = "bizType", description = "业务对象类型", example = "CUSTOMER"),
            @Parameter(name = "bizId", description = "业务ID", example = "1024")
    })
    @PreAuthorize("@ss.hasPermission('lunaerp:common:recycle:purge')")
    public CommonResult<Boolean> purge(@RequestParam(value = "bizType", required = false) String bizType,
                                       @RequestParam(value = "bizId", required = false) Long bizId,
                                       @RequestBody(required = false) RecycleRestoreReqVO bodyReq) {
        if (bizType == null && bodyReq != null) {
            bizType = bodyReq.getBizType();
            bizId = bodyReq.getBizId();
        }
        if (bizType == null || bizId == null) {
            throw exception(BAD_REQUEST, "业务对象类型和业务ID不能为空");
        }
        RecycleBinHandler handler = recycleBinHandlerRegistry.getHandler(bizType);
        if (handler.hasActiveReference(bizId)) {
            throw exception(RECYCLE_BIN_HAS_ACTIVE_REFERENCE);
        }
        handler.purge(bizId);
        return success(true);
    }

}
