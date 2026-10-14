package com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color.ColorInstructionColorDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 生产管理 - 打色指示单颜色行子表 Mapper
 *
 * 颜色行作为打色指示单聚合的一部分，无独立 Service/Controller，
 * 随打色单聚合维护。
 *
 * @author luna
 */
@Mapper
public interface ColorInstructionColorMapper extends BaseMapperX<ColorInstructionColorDO> {

    /**
     * 按打色单ID查询全部颜色行（按 line_no 升序）
     */
    default List<ColorInstructionColorDO> selectListByInstructionId(Long instructionId) {
        if (instructionId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ColorInstructionColorDO>()
                .eq(ColorInstructionColorDO::getInstructionId, instructionId)
                .orderByAsc(ColorInstructionColorDO::getLineNo));
    }

    /**
     * 批量查询打色单的颜色行（按 instruction_id、line_no 升序，用于列表防 N+1 批量摘要）
     */
    default List<ColorInstructionColorDO> selectListByInstructionIds(Collection<Long> instructionIds) {
        if (instructionIds == null || instructionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ColorInstructionColorDO>()
                .in(ColorInstructionColorDO::getInstructionId, instructionIds)
                .orderByAsc(ColorInstructionColorDO::getInstructionId)
                .orderByAsc(ColorInstructionColorDO::getLineNo));
    }

    /**
     * 按打色单ID逻辑删除全部颜色行
     */
    default int deleteByInstructionId(Long instructionId) {
        return delete(new LambdaQueryWrapperX<ColorInstructionColorDO>()
                .eq(ColorInstructionColorDO::getInstructionId, instructionId));
    }

    /**
     * 级联恢复颜色明细（手写 XML：显式 SET deleted=0 WHERE instruction_id=? AND deleted=1 AND tenant_id=?）
     */
    int restoreByInstructionId(@Param("instructionId") Long instructionId,
                              @Param("tenantId") Long tenantId,
                              @Param("updater") String updater,
                              @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除颜色明细（手写 XML：显式 DELETE WHERE instruction_id=? AND deleted=1 AND tenant_id=?）
     */
    int purgeByInstructionId(@Param("instructionId") Long instructionId,
                            @Param("tenantId") Long tenantId);

}
