package com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisSupplierQuoteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 样品管理-样品分析供应商报价子表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SampleAnalysisSupplierQuoteMapper extends BaseMapperX<SampleAnalysisSupplierQuoteDO> {

    default List<SampleAnalysisSupplierQuoteDO> selectListBySampleAnalysisId(Long sampleAnalysisId) {
        return selectList(new LambdaQueryWrapperX<SampleAnalysisSupplierQuoteDO>()
                .eq(SampleAnalysisSupplierQuoteDO::getSampleAnalysisId, sampleAnalysisId)
                .orderByAsc(SampleAnalysisSupplierQuoteDO::getId));
    }

    default int deleteBySampleAnalysisId(Long sampleAnalysisId) {
        return delete(new LambdaQueryWrapperX<SampleAnalysisSupplierQuoteDO>()
                .eq(SampleAnalysisSupplierQuoteDO::getSampleAnalysisId, sampleAnalysisId));
    }

    default int deleteBySampleAnalysisIds(Collection<Long> sampleAnalysisIds) {
        return delete(new LambdaQueryWrapperX<SampleAnalysisSupplierQuoteDO>()
                .in(SampleAnalysisSupplierQuoteDO::getSampleAnalysisId, sampleAnalysisIds));
    }

}
