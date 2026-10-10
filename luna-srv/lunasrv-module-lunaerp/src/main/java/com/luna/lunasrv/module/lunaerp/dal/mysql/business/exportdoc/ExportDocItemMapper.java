package com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportDocItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 业务管理-出口单证明细行表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ExportDocItemMapper extends BaseMapperX<ExportDocItemDO> {

    /**
     * 按单证套ID查询货物明细列表（按行序 line_no 升序）
     *
     * @param docId 单证套ID
     * @return 明细行列表
     */
    default List<ExportDocItemDO> selectListByDocId(Long docId) {
        return selectList(new LambdaQueryWrapperX<ExportDocItemDO>()
                .eq(ExportDocItemDO::getDocId, docId)
                .orderByAsc(ExportDocItemDO::getLineNo));
    }

    /**
     * 按单证套ID逻辑删除货物明细
     *
     * @param docId 单证套ID
     * @return 删除行数
     */
    default int deleteByDocId(Long docId) {
        return delete(new LambdaQueryWrapperX<ExportDocItemDO>()
                .eq(ExportDocItemDO::getDocId, docId));
    }

}
