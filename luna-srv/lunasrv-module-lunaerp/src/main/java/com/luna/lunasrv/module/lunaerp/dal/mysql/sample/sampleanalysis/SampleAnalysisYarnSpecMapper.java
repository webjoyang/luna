package com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisYarnSpecDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 样品管理 - 样品分析纱线规格明细子表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SampleAnalysisYarnSpecMapper extends BaseMapperX<SampleAnalysisYarnSpecDO> {

    default List<SampleAnalysisYarnSpecDO> selectListBySampleAnalysisId(Long sampleAnalysisId) {
        return selectList(new LambdaQueryWrapperX<SampleAnalysisYarnSpecDO>()
                .eq(SampleAnalysisYarnSpecDO::getSampleAnalysisId, sampleAnalysisId)
                .orderByAsc(SampleAnalysisYarnSpecDO::getId));
    }

    default int deleteBySampleAnalysisId(Long sampleAnalysisId) {
        return delete(new LambdaQueryWrapperX<SampleAnalysisYarnSpecDO>()
                .eq(SampleAnalysisYarnSpecDO::getSampleAnalysisId, sampleAnalysisId));
    }

    default int deleteBySampleAnalysisIds(Collection<Long> sampleAnalysisIds) {
        return delete(new LambdaQueryWrapperX<SampleAnalysisYarnSpecDO>()
                .in(SampleAnalysisYarnSpecDO::getSampleAnalysisId, sampleAnalysisIds));
    }

}
