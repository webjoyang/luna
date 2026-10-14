package com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.colorinstruction.vo.ColorInstructionPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产管理 - 打色指示单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ColorInstructionMapper extends BaseMapperX<ColorInstructionDO> {

    default PageResult<ColorInstructionDO> selectPage(ColorInstructionPageReqVO reqVO) {
        return selectPage(reqVO, buildQueryWrapper(reqVO));
    }

    static LambdaQueryWrapperX<ColorInstructionDO> buildQueryWrapper(ColorInstructionPageReqVO reqVO) {
        LambdaQueryWrapperX<ColorInstructionDO> wrapper = new LambdaQueryWrapperX<>();

        // 统一关键字模糊（单号、客户名称、产品编号、产品名称）
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String kw = "%" + reqVO.getKeyword().trim() + "%";
            wrapper.and(w -> w.apply("LOWER(instruction_no) LIKE LOWER({0})", kw)
                    .or().apply("LOWER(client_name) LIKE LOWER({0})", kw)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", kw)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", kw));
        }

        // 单号模糊
        if (StringUtils.hasText(reqVO.getInstructionNo())) {
            wrapper.apply("LOWER(instruction_no) LIKE LOWER({0})", "%" + reqVO.getInstructionNo().trim() + "%");
        }

        // 客户过滤
        wrapper.eqIfPresent(ColorInstructionDO::getCustomerId, reqVO.getCustomerId());

        // 产品编号或名称模糊
        if (StringUtils.hasText(reqVO.getProductCodeOrName())) {
            String keyword = "%" + reqVO.getProductCodeOrName().trim() + "%";
            wrapper.and(w -> w.apply("LOWER(product_code) LIKE LOWER({0})", keyword)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", keyword));
        }

        // 客户色号过滤：子查询打色颜色行，列表主查询不联表（防 N+1 与行膨胀）
        if (StringUtils.hasText(reqVO.getCustomerColorNo())) {
            String colorKeyword = "%" + reqVO.getCustomerColorNo().trim() + "%";
            Long tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                wrapper.apply("id IN (SELECT instruction_id FROM lunaerp_production_color_instruction_color WHERE LOWER(customer_color_no) LIKE LOWER({0}) AND tenant_id = {1} AND deleted = false)", colorKeyword, tenantId);
            } else {
                wrapper.apply("id IN (SELECT instruction_id FROM lunaerp_production_color_instruction_color WHERE LOWER(customer_color_no) LIKE LOWER({0}) AND deleted = false)", colorKeyword);
            }
        }

        // 状态过滤（优先 statusList 多选，次选 status 单选）
        if (CollUtil.isNotEmpty(reqVO.getStatusList())) {
            wrapper.in(ColorInstructionDO::getStatus, reqVO.getStatusList());
        } else if (StringUtils.hasText(reqVO.getStatus())) {
            wrapper.eq(ColorInstructionDO::getStatus, reqVO.getStatus().trim());
        }

        // 单据日期区间过滤
        if (reqVO.getInstructionDateRange() != null && reqVO.getInstructionDateRange().length > 0) {
            if (reqVO.getInstructionDateRange().length >= 2) {
                if (reqVO.getInstructionDateRange()[0] != null && reqVO.getInstructionDateRange()[1] != null) {
                    wrapper.between(ColorInstructionDO::getInstructionDate, reqVO.getInstructionDateRange()[0], reqVO.getInstructionDateRange()[1]);
                } else if (reqVO.getInstructionDateRange()[0] != null) {
                    wrapper.ge(ColorInstructionDO::getInstructionDate, reqVO.getInstructionDateRange()[0]);
                } else if (reqVO.getInstructionDateRange()[1] != null) {
                    wrapper.le(ColorInstructionDO::getInstructionDate, reqVO.getInstructionDateRange()[1]);
                }
            } else if (reqVO.getInstructionDateRange()[0] != null) {
                wrapper.ge(ColorInstructionDO::getInstructionDate, reqVO.getInstructionDateRange()[0]);
            }
        }

        // 默认 instructionDate DESC, id DESC
        wrapper.orderByDesc(ColorInstructionDO::getInstructionDate)
                .orderByDesc(ColorInstructionDO::getId);

        return wrapper;
    }

    /**
     * 按单号前缀查询打色单，【包含已逻辑删除的行】（仿 ADR-0006）。
     */
    @Select("SELECT * FROM lunaerp_production_color_instruction WHERE instruction_no LIKE CONCAT(#{prefix}, '%')")
    List<ColorInstructionDO> selectListByNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按单号精确查询打色单，【包含已逻辑删除的行】（仿 ADR-0006）。
     */
    @Select("SELECT * FROM lunaerp_production_color_instruction WHERE instruction_no = #{instructionNo} LIMIT 1")
    ColorInstructionDO selectByInstructionNoIncludingDeleted(@Param("instructionNo") String instructionNo);

    /**
     * 回收站分页查询已软删打色指示单（手写 XML：显式 WHERE deleted = 1 AND status = 'PENDING' AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的 PENDING 打色单记录
     */
    ColorInstructionDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复打色指示单（并发条件更新带 deleted=1 AND status='PENDING' AND tenant_id=?）
     */
    int restoreColorInstruction(@Param("id") Long id, @Param("tenantId") Long tenantId,
                                @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除打色指示单（物理删除带 deleted=1 AND status='PENDING' AND tenant_id=?）
     */
    int purgeColorInstruction(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查打色指示单是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("instructionId") Long instructionId, @Param("tenantId") Long tenantId);

}
