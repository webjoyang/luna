package com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigedispatch.GreigeDispatchRecordDO;
import com.luna.lunasrv.module.lunaerp.service.production.greigedispatch.GreigeDispatchRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.luna.lunasrv.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 生产管理-发染记录 Controller
 *
 * @author luna
 */
@Tag(name = "管理后台 - 生产管理-发染记录")
@RestController
@RequestMapping("/lunaerp/production/greige-dispatch-record")
@Validated
public class GreigeDispatchRecordController {

    @Resource
    private GreigeDispatchRecordService greigeDispatchRecordService;

    @PostMapping("/create")
    @Operation(summary = "创建发染记录（同事务写 PROCESS_OUT 库存流水）")
    @PreAuthorize("@ss.hasPermission('lunaerp:greige-dispatch-record:create')")
    public CommonResult<Long> createGreigeDispatchRecord(
            @Valid @RequestBody GreigeDispatchRecordCreateReqVO reqVO) {
        return success(greigeDispatchRecordService.createGreigeDispatchRecord(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "受限编辑发染记录（仅允许改染色单/染厂/备注）")
    @PreAuthorize("@ss.hasPermission('lunaerp:greige-dispatch-record:create')")
    public CommonResult<Boolean> updateGreigeDispatchRecord(
            @Valid @RequestBody GreigeDispatchRecordUpdateReqVO reqVO) {
        greigeDispatchRecordService.updateGreigeDispatchRecord(reqVO);
        return success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除发染记录（同事务写 PROCESS_OUT_REVERSAL 红冲流水）")
    @Parameter(name = "id", description = "发染记录ID", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('lunaerp:greige-dispatch-record:delete')")
    public CommonResult<Boolean> deleteGreigeDispatchRecord(@PathVariable("id") Long id) {
        greigeDispatchRecordService.deleteGreigeDispatchRecord(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得发染记录详情")
    @Parameter(name = "id", description = "发染记录ID", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('lunaerp:greige-dispatch-record:query')")
    public CommonResult<GreigeDispatchRecordRespVO> getGreigeDispatchRecord(
            @RequestParam("id") Long id) {
        GreigeDispatchRecordDO record = greigeDispatchRecordService.getGreigeDispatchRecord(id);
        return success(BeanUtils.toBean(record, GreigeDispatchRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得发染记录分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:greige-dispatch-record:query')")
    public CommonResult<PageResult<GreigeDispatchRecordRespVO>> getGreigeDispatchRecordPage(
            @Valid GreigeDispatchRecordPageReqVO reqVO) {
        PageResult<GreigeDispatchRecordDO> pageResult =
                greigeDispatchRecordService.getGreigeDispatchRecordPage(reqVO);
        return success(BeanUtils.toBean(pageResult, GreigeDispatchRecordRespVO.class));
    }

    @GetMapping("/list-by-greige-order")
    @Operation(summary = "按胚布单ID获得发染记录列表")
    @Parameter(name = "greigeOrderId", description = "胚布单ID", required = true, example = "1001")
    @PreAuthorize("@ss.hasPermission('lunaerp:greige-dispatch-record:query')")
    public CommonResult<List<GreigeDispatchRecordRespVO>> getGreigeDispatchRecordListByGreigeOrderId(
            @RequestParam("greigeOrderId") Long greigeOrderId) {
        List<GreigeDispatchRecordDO> list =
                greigeDispatchRecordService.getGreigeDispatchRecordListByGreigeOrderId(greigeOrderId);
        return success(BeanUtils.toBean(list, GreigeDispatchRecordRespVO.class));
    }

    @GetMapping("/list-by-dyeing-order")
    @Operation(summary = "按染色单ID获得发染记录列表")
    @Parameter(name = "dyeingOrderId", description = "染色单ID", required = true, example = "2001")
    @PreAuthorize("@ss.hasPermission('lunaerp:greige-dispatch-record:query')")
    public CommonResult<List<GreigeDispatchRecordRespVO>> getGreigeDispatchRecordListByDyeingOrderId(
            @RequestParam("dyeingOrderId") Long dyeingOrderId) {
        List<GreigeDispatchRecordDO> list =
                greigeDispatchRecordService.getGreigeDispatchRecordListByDyeingOrderId(dyeingOrderId);
        return success(BeanUtils.toBean(list, GreigeDispatchRecordRespVO.class));
    }

}
