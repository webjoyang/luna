package com.luna.lunasrv.module.lunaerp.dal.mysql.business.quotation;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.quotation.QuotationItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务管理 - 报价单明细 Mapper
 *
 * @author luna
 */
@Mapper
public interface QuotationItemMapper extends BaseMapperX<QuotationItemDO> {

    /**
     * 按报价单ID查询全部明细（按 line_no 升序）
     */
    default List<QuotationItemDO> selectListByQuotationId(Long quotationId) {
        return selectList(new LambdaQueryWrapperX<QuotationItemDO>()
                .eq(QuotationItemDO::getQuotationId, quotationId)
                .orderByAsc(QuotationItemDO::getLineNo));
    }

    /**
     * 按报价单ID逻辑删除全部明细
     */
    default int deleteByQuotationId(Long quotationId) {
        return delete(new LambdaQueryWrapperX<QuotationItemDO>()
                .eq(QuotationItemDO::getQuotationId, quotationId));
    }

    /**
     * 级联恢复报价单明细
     */
    int restoreByQuotationId(@Param("quotationId") Long quotationId, @Param("tenantId") Long tenantId,
                             @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 物理删除报价单明细
     */
    int purgeByQuotationId(@Param("quotationId") Long quotationId, @Param("tenantId") Long tenantId);

}
