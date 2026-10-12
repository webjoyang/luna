package com.luna.lunasrv.module.lunaerp.dal.mysql.finance.invoice;

import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.invoice.vo.InvoicePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.invoice.InvoiceApplicationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 财务管理 - 发票申请 Mapper
 *
 * @author luna
 */
@Mapper
public interface InvoiceApplicationMapper extends BaseMapperX<InvoiceApplicationDO> {

    /**
     * 按申请单号精确查询发票申请（含已删除行，用于手填查重与自动号防撞）
     */
    @Select("SELECT id, invoice_no, customer_id, customer_name, currency, tax_rate, invoice_kind, electronic, "
            + "invoice_title, tax_no, address, bank_info, status, official_invoice_no, invoice_code, invoice_date, "
            + "attachment_url, total_amount_excl_tax, total_tax, total_amount_incl_tax, issue_by, issue_time, "
            + "submit_by, submit_time, reject_reason, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_invoice_application WHERE invoice_no = #{invoiceNo} LIMIT 1")
    InvoiceApplicationDO selectByInvoiceNoIncludingDeleted(@Param("invoiceNo") String invoiceNo);

    /**
     * 按单号前缀查询发票申请列表（含已删除行，用于按年取号扫描）
     */
    @Select("SELECT id, invoice_no, customer_id, customer_name, currency, tax_rate, invoice_kind, electronic, "
            + "invoice_title, tax_no, address, bank_info, status, official_invoice_no, invoice_code, invoice_date, "
            + "attachment_url, total_amount_excl_tax, total_tax, total_amount_incl_tax, issue_by, issue_time, "
            + "submit_by, submit_time, reject_reason, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_invoice_application WHERE invoice_no LIKE CONCAT(#{prefix}, '%')")
    List<InvoiceApplicationDO> selectListByInvoiceNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按官方发票号码精确查询发票申请（含已删除行，用于发票号唯一性校验）
     */
    @Select("SELECT id, invoice_no, customer_id, customer_name, currency, tax_rate, invoice_kind, electronic, "
            + "invoice_title, tax_no, address, bank_info, status, official_invoice_no, invoice_code, invoice_date, "
            + "attachment_url, total_amount_excl_tax, total_tax, total_amount_incl_tax, issue_by, issue_time, "
            + "submit_by, submit_time, reject_reason, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_invoice_application WHERE official_invoice_no = #{officialInvoiceNo} LIMIT 1")
    InvoiceApplicationDO selectByOfficialInvoiceNoIncludingDeleted(@Param("officialInvoiceNo") String officialInvoiceNo);

    /**
     * 发票申请分页查询
     */
    default PageResult<InvoiceApplicationDO> selectPage(InvoicePageReqVO reqVO) {
        LambdaQueryWrapperX<InvoiceApplicationDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(InvoiceApplicationDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(InvoiceApplicationDO::getStatus, reqVO.getStatus())
                .eqIfPresent(InvoiceApplicationDO::getInvoiceKind, reqVO.getInvoiceKind())
                .eqIfPresent(InvoiceApplicationDO::getCurrency, reqVO.getCurrency());
        if (ArrayUtil.isNotEmpty(reqVO.getCreateTime()) && reqVO.getCreateTime().length == 2) {
            wrapper.between(InvoiceApplicationDO::getCreateTime, reqVO.getCreateTime()[0], reqVO.getCreateTime()[1]);
        }
        if (ArrayUtil.isNotEmpty(reqVO.getInvoiceDate()) && reqVO.getInvoiceDate().length == 2) {
            wrapper.between(InvoiceApplicationDO::getInvoiceDate, reqVO.getInvoiceDate()[0], reqVO.getInvoiceDate()[1]);
        }
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InvoiceApplicationDO::getInvoiceNo, keyword)
                    .or().like(InvoiceApplicationDO::getCustomerName, keyword)
                    .or().like(InvoiceApplicationDO::getOfficialInvoiceNo, keyword)
                    .or().like(InvoiceApplicationDO::getInvoiceTitle, keyword));
        }
        wrapper.orderByDesc(InvoiceApplicationDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 回收站分页查询已软删草稿发票申请
     */
    @Select("SELECT id AS id, invoice_no AS record_no, customer_name AS display_name, "
            + "total_amount_incl_tax AS amount, updater AS operator_name, update_time AS delete_time, "
            + "'INVOICE_APPLICATION' AS biz_type "
            + "FROM lunaerp_finance_invoice_application "
            + "WHERE deleted = 1 AND status = 'DRAFT' AND tenant_id = #{tenantId} "
            + "ORDER BY update_time DESC")
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<?> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的发票申请
     */
    @Select("SELECT id, invoice_no, customer_id, customer_name, currency, tax_rate, invoice_kind, electronic, "
            + "invoice_title, tax_no, address, bank_info, status, official_invoice_no, invoice_code, invoice_date, "
            + "attachment_url, total_amount_excl_tax, total_tax, total_amount_incl_tax, issue_by, issue_time, "
            + "submit_by, submit_time, reject_reason, remark, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_finance_invoice_application WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1 LIMIT 1")
    InvoiceApplicationDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿发票申请
     */
    @Update("UPDATE lunaerp_finance_invoice_application SET deleted = 0, updater = #{updater}, update_time = #{updateTime} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1 AND status = 'DRAFT'")
    int restoreInvoiceApplication(@Param("id") Long id, @Param("tenantId") Long tenantId,
                                  @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底物理删除发票申请
     */
    @Update("DELETE FROM lunaerp_finance_invoice_application "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 1")
    int purgeInvoiceApplication(@Param("id") Long id, @Param("tenantId") Long tenantId);

}
