package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 财务管理 - 发票申请主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface InvoiceApplicationMapper extends BaseMapperX<InvoiceApplicationDO> {

    String COLUMNS = "id, invoice_no, customer_id, customer_name, currency, tax_rate, "
            + "invoice_kind, electronic, invoice_title, tax_no, address, bank_info, "
            + "status, official_invoice_no, invoice_code, invoice_date, attachment_url, "
            + "total_amount_excl_tax, total_tax, total_amount_incl_tax, issue_by, issue_time, "
            + "submit_by, submit_time, remark, creator, create_time, updater, update_time, deleted, tenant_id";

    /**
     * 按发票申请单号精确查询（含已软删除记录，用于取号防撞）
     *
     * @param invoiceNo 发票申请单号
     * @return 申请单（含已删除记录）
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_invoice_application WHERE invoice_no = #{invoiceNo} LIMIT 1")
    InvoiceApplicationDO selectByInvoiceNoIncludingDeleted(@Param("invoiceNo") String invoiceNo);

    /**
     * 按申请单号前缀查询列表（含已软删除记录，用于发号扫描）
     *
     * @param prefix 单号前缀
     * @return 申请单列表（含已删除记录）
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_invoice_application WHERE invoice_no LIKE CONCAT(#{prefix}, '%')")
    List<InvoiceApplicationDO> selectListByInvoiceNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按官方发票号码精确查询（含已软删除记录，用于唯一性校验）
     *
     * @param officialInvoiceNo 官方发票号码（20位）
     * @return 申请单（含已删除记录）
     */
    @Select("SELECT " + COLUMNS + " FROM lunaerp_finance_invoice_application WHERE official_invoice_no = #{officialInvoiceNo} LIMIT 1")
    InvoiceApplicationDO selectByOfficialInvoiceNoIncludingDeleted(@Param("officialInvoiceNo") String officialInvoiceNo);

}
