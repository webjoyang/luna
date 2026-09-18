package com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务管理-销售合同主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface SalesContractMapper extends BaseMapperX<SalesContractDO> {

    /**
     * 按编号前缀查询合同，【包含已逻辑删除的行】。
     *
     * ADR-0004：合同编号 (tenant_id, contract_no) 全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删合同的编号会被重新分配。
     * 因此此处用手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤（deleted=0 条件不会追加）；
     * tenant_id 由租户拦截器自动追加，租户隔离不受影响。勿"修复"为 wrapper 查询。
     *
     * @param prefix 编号前缀，如 CH2026
     * @return 合同列表（含已删除行）
     */
    @Select("SELECT * FROM lunaerp_business_sales_contract WHERE contract_no LIKE CONCAT(#{prefix}, '%')")
    List<SalesContractDO> selectListByContractNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按编号精确查询合同，【包含已逻辑删除的行】。
     *
     * ADR-0004：手填编号查重同样必须覆盖已删除行——软删编号已被占用，
     * 与之重复的新编号应报 SALES_CONTRACT_NO_DUPLICATE 而非放行。
     * 手写 SQL 绕过逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param contractNo 合同编号
     * @return 合同（含已删除行），不存在返回 null
     */
    @Select("SELECT * FROM lunaerp_business_sales_contract WHERE contract_no = #{contractNo} LIMIT 1")
    SalesContractDO selectByContractNoIncludingDeleted(@Param("contractNo") String contractNo);

}
