package com.luna.lunasrv.module.lunaerp.dal.mysql.business.exportdoc;

import com.luna.lunasrv.framework.datapermission.core.annotation.DataPermission;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.exportdoc.ExportDocDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务管理-出口单证套主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ExportDocMapper extends BaseMapperX<ExportDocDO> {

    /**
     * 按单证套号前缀查询，【包含已逻辑删除的行】。
     *
     * ADR-0004：单证套号 (tenant_id, doc_no) 全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删单证套的编号会被重新分配。
     * 手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param prefix 编号前缀，如 ED2026
     * @return 单证套列表（含已删除行）
     */
    @DataPermission(enable = false)
    @Select("SELECT id, doc_no, status, source_type, contract_id, contract_no, shipment_id, shipment_no, "
            + "customer_id, customer_name, seller_name, seller_address, seller_bank_info, "
            + "buyer_name, buyer_address, buyer_bank_info, incoterm, port_of_loading, port_of_discharge, "
            + "country_of_dest, transport_mode, payment_term, currency, shipping_mark, "
            + "pi_meta, ci_meta, pl_meta, remarks, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_export_doc WHERE doc_no LIKE CONCAT(#{prefix}, '%')")
    List<ExportDocDO> selectListByDocNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按单证套号精确查询，【包含已逻辑删除的行】。
     *
     * ADR-0004：手填编号查重同样必须覆盖已删除行。
     *
     * @param docNo 单证套号
     * @return 单证套（含已删除行），不存在返回 null
     */
    @DataPermission(enable = false)
    @Select("SELECT id, doc_no, status, source_type, contract_id, contract_no, shipment_id, shipment_no, "
            + "customer_id, customer_name, seller_name, seller_address, seller_bank_info, "
            + "buyer_name, buyer_address, buyer_bank_info, incoterm, port_of_loading, port_of_discharge, "
            + "country_of_dest, transport_mode, payment_term, currency, shipping_mark, "
            + "pi_meta, ci_meta, pl_meta, remarks, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_export_doc WHERE doc_no = #{docNo} LIMIT 1")
    ExportDocDO selectByDocNoIncludingDeleted(@Param("docNo") String docNo);

}
