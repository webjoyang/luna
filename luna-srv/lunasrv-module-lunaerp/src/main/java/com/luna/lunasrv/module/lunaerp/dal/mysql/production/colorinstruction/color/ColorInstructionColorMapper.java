package com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction.color;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.color.ColorInstructionColorDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产管理 - 打色指示单颜色行子表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ColorInstructionColorMapper extends BaseMapperX<ColorInstructionColorDO> {

    /**
     * 按打色单ID查询颜色行列表（按 line_no 升序）
     *
     * @param instructionId 打色单ID
     * @return 颜色行列表
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
     * 按打色单ID集合批量查询颜色行列表（防 N+1，按 instruction_id、line_no 升序）
     *
     * @param instructionIds 打色单ID集合
     * @return 颜色行列表
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
     * 按客户色号模糊匹配查找对应的打色指示单 ID 集合（用于列表过滤）
     *
     * @param customerColorNo 客户色号
     * @return 打色单ID集合
     */
    default Set<Long> selectInstructionIdsByCustomerColorNo(String customerColorNo) {
        if (!StringUtils.hasText(customerColorNo)) {
            return Collections.emptySet();
        }
        List<ColorInstructionColorDO> list = selectList(new LambdaQueryWrapperX<ColorInstructionColorDO>()
                .select(ColorInstructionColorDO::getInstructionId)
                .like(ColorInstructionColorDO::getCustomerColorNo, customerColorNo.trim()));
        return list.stream()
                .map(ColorInstructionColorDO::getInstructionId)
                .collect(Collectors.toSet());
    }

}
