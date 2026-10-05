package com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisYarnArrangeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 样品管理-样品分析绕纱行子表 Mapper
 *
 * 绕纱行作为样品分析聚合的一部分（ADR-0003模式），无独立 Service/Controller，
 * 由 SampleAnalysisServiceImpl 在样品分析保存/详情/删除方法内协作调用。
 *
 * @author luna
 */
@Mapper
public interface SampleAnalysisYarnArrangeMapper extends BaseMapperX<SampleAnalysisYarnArrangeDO> {

    /**
     * 按样品分析ID查询全部绕纱行（按 id 升序，保证保存顺序展示）。
     *
     * @param sampleAnalysisId 样品分析ID
     * @return 绕纱行列表
     */
    default List<SampleAnalysisYarnArrangeDO> selectListBySampleAnalysisId(Long sampleAnalysisId) {
        return selectList(new LambdaQueryWrapperX<SampleAnalysisYarnArrangeDO>()
                .eq(SampleAnalysisYarnArrangeDO::getSampleAnalysisId, sampleAnalysisId)
                .orderByAsc(SampleAnalysisYarnArrangeDO::getId));
    }

    /**
     * 按样品分析ID逻辑删除全部绕纱行。
     *
     * @param sampleAnalysisId 样品分析ID
     * @return 受影响行数
     */
    default int deleteBySampleAnalysisId(Long sampleAnalysisId) {
        return delete(new LambdaQueryWrapperX<SampleAnalysisYarnArrangeDO>()
                .eq(SampleAnalysisYarnArrangeDO::getSampleAnalysisId, sampleAnalysisId));
    }

}
