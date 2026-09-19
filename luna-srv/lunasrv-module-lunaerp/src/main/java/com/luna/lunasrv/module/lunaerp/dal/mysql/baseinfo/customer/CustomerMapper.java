package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.customer.vo.CustomerPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基本信息-客户表 Mapper
 *
 * 列表 search 智能词命中 company_name / short_name / phone / unified_social_credit_code 之一，
 * PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 OR ILIKE 语义（占位符走预编译，无注入风险）。
 * 逻辑删除（deleted=0）与租户条件由框架自动追加。
 *
 * @author luna
 */
@Mapper
public interface CustomerMapper extends BaseMapperX<CustomerDO> {

    /**
     * 客户分页查询：search 智能词 OR 四字段 ILIKE + status 等值过滤。
     *
     * @param reqVO 分页与查询条件
     * @return 客户分页
     */
    default PageResult<CustomerDO> selectPage(CustomerPageReqVO reqVO) {
        LambdaQueryWrapperX<CustomerDO> wrapper = new LambdaQueryWrapperX<>();
        // status 等值过滤（为空则不过滤，便于"全部"场景）
        wrapper.eqIfPresent(CustomerDO::getStatus, reqVO.getStatus());
        // search 智能词：命中 company_name / short_name / phone / unified_social_credit_code 之一
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (!search.isEmpty()) {
            String like = "%" + search + "%";
            wrapper.and(w -> w
                    .apply("LOWER(company_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(short_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(phone) LIKE LOWER({0})", like)
                    .or().apply("LOWER(unified_social_credit_code) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(CustomerDO::getCreateTime);
        return selectPage(reqVO, wrapper);
    }

}
