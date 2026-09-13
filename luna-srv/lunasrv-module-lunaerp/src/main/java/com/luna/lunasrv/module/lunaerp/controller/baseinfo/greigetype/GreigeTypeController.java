package com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype;

import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypePageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypeRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypeSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.greigetype.vo.GreigeTypeSimpleRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype.GreigeTypeDO;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.greigetype.GreigeTypeService;
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
 * 胚布品种 Controller
 *
 * 注意：本类在 controller.baseinfo 包下（不在 controller.admin 包），
 * 框架不会自动补 /admin-api 前缀，因此类级路径显式写全。
 *
 * @author luna
 */
@Tag(name = "基本信息-胚布品种")
@RestController
@RequestMapping("/admin-api/lunaerp/baseinfo/greige-type")
@Validated
public class GreigeTypeController {

    @Resource
    private GreigeTypeService greigeTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建胚布品种")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:greige:create')")
    public CommonResult<Long> createGreigeType(@Valid @RequestBody GreigeTypeSaveReqVO createReqVO) {
        return success(greigeTypeService.createGreigeType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改胚布品种")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:greige:update')")
    public CommonResult<Boolean> updateGreigeType(@Valid @RequestBody GreigeTypeSaveReqVO updateReqVO) {
        greigeTypeService.updateGreigeType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除胚布品种")
    @Parameter(name = "id", description = "胚布品种ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:greige:delete')")
    public CommonResult<Boolean> deleteGreigeType(@RequestParam("id") Long id) {
        greigeTypeService.deleteGreigeType(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得胚布品种")
    @Parameter(name = "id", description = "胚布品种ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:greige:query')")
    public CommonResult<GreigeTypeRespVO> getGreigeType(@RequestParam("id") Long id) {
        GreigeTypeDO greigeType = greigeTypeService.getGreigeType(id);
        return success(BeanUtils.toBean(greigeType, GreigeTypeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得胚布品种分页")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:greige:query')")
    public CommonResult<PageResult<GreigeTypeRespVO>> getGreigeTypePage(@Validated GreigeTypePageReqVO pageReqVO) {
        PageResult<GreigeTypeDO> pageResult = greigeTypeService.getGreigeTypePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, GreigeTypeRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得启用中的胚布品种精简列表", description = "供产品表单胚布品种选择器使用，仅返回 status=ACTIVE 的品种")
    @PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:product:greige:query')")
    public CommonResult<List<GreigeTypeSimpleRespVO>> getActiveGreigeTypeSimpleList() {
        List<GreigeTypeDO> list = greigeTypeService.getActiveGreigeTypeList();
        return success(BeanUtils.toBean(list, GreigeTypeSimpleRespVO.class));
    }

}
