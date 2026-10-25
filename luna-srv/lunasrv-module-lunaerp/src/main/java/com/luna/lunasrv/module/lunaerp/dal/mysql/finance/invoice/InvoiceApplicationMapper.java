package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
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

    /**
     * 回收站分页查询已软删草稿发票申请
     *
     * @param page 分页参数
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿发票申请记录
     *
     * @param id 发票申请ID
     * @param tenantId 租户ID
     * @return 发票申请DO
     */
    InvoiceApplicationDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿发票申请
     *
     * @param id 发票申请ID
     * @param tenantId 租户ID
     * @param updater 更新人
     * @param now 更新时间
     * @return 影响行数
     */
    int restoreInvoiceApplication(@Param("id") Long id, @Param("tenantId") Long tenantId,
                                  @Param("updater") String updater, @Param("now") LocalDateTime now);

    /**
     * 彻底物理删除草稿发票申请
     *
     * @param id 发票申请ID
     * @param tenantId 租户ID
     * @return 影响行数
     */
    int purgeInvoiceApplication(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查发票申请是否存在活跃业务引用
     *
     * @param id 发票申请ID
     * @param tenantId 租户ID
     * @return 活跃引用数量
     */
    Long selectActiveReferenceCount(@Param("id") Long id, @Param("tenantId") Long tenantId);

}
