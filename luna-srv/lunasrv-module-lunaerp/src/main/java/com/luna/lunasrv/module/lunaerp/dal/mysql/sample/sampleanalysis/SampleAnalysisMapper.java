package com.luna.lunasrv.module.lunaerp.dal.mysql.sample.sampleanalysis;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.sample.sampleanalysis.SampleAnalysisDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 样品管理-样品分析主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SampleAnalysisMapper extends BaseMapperX<SampleAnalysisDO> {

    /**
     * 按编号前缀查询样品分析，【包含已逻辑删除的行】。
     *
     * 样品编号 (tenant_id, sample_analysis_no) 全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删样品的编号会被重新分配。
     * 手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param prefix 编号前缀，如 SA2026
     * @return 样品分析列表（含已删除行）
     */
    @Select("SELECT * FROM lunaerp_sample_analysis WHERE sample_analysis_no LIKE CONCAT(#{prefix}, '%')")
    List<SampleAnalysisDO> selectListByNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按编号精确查询样品分析，【包含已逻辑删除的行】。
     *
     * 手填编号查重同样必须覆盖已删除行——软删编号已被占用，
     * 与之重复的新编号应报 SAMPLE_ANALYSIS_NO_DUPLICATE。
     * 手写 SQL 绕过逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param sampleAnalysisNo 样品编号
     * @return 样品分析（含已删除行），不存在返回 null
     */
    @Select("SELECT * FROM lunaerp_sample_analysis WHERE sample_analysis_no = #{sampleAnalysisNo} LIMIT 1")
    SampleAnalysisDO selectByNoIncludingDeleted(@Param("sampleAnalysisNo") String sampleAnalysisNo);

}
