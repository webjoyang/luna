package com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract;

import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
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
    @Select("SELECT id, contract_no, customer_id, client_name, client_contact, client_address, client_contract_no, "
            + "salesperson_user_id, sign_date, delivery_date, unit, currency, trade_type, order_type, "
            + "total_quantity, total_amount, remarks, status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_sales_contract WHERE contract_no LIKE CONCAT(#{prefix}, '%')")
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
    @Select("SELECT id, contract_no, customer_id, client_name, client_contact, client_address, client_contract_no, "
            + "salesperson_user_id, sign_date, delivery_date, unit, currency, trade_type, order_type, "
            + "total_quantity, total_amount, remarks, status, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_business_sales_contract WHERE contract_no = #{contractNo} LIMIT 1")
    SalesContractDO selectByContractNoIncludingDeleted(@Param("contractNo") String contractNo);

    /**
     * 生产进度列表候选合同查询（只读聚合，ADR-0034）。
     *
     * <p>下推硬过滤（关键字/交期范围/状态）以减少候选集；排序、仅看预警、分页
     * 由 Service 内存完成（预警需聚合后才能判定）。逻辑删除与租户条件由框架自动追加。</p>
     *
     * @param keyword          关键字（合同号/客户名模糊），可空
     * @param deliveryDateFrom 交期起（含），可空
     * @param deliveryDateTo   交期止（含），可空
     * @param status           合同状态等值筛选，可空
     * @param includeCancelled 无 status 时是否包含已取消合同
     * @return 候选合同列表（未排序）
     */
    default List<SalesContractDO> selectListForProgress(String keyword, LocalDate deliveryDateFrom,
                                                        LocalDate deliveryDateTo, String status,
                                                        boolean includeCancelled) {
        LambdaQueryWrapperX<SalesContractDO> wrapper = new LambdaQueryWrapperX<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SalesContractDO::getContractNo, kw)
                    .or().like(SalesContractDO::getClientName, kw));
        }
        wrapper.geIfPresent(SalesContractDO::getDeliveryDate, deliveryDateFrom)
                .leIfPresent(SalesContractDO::getDeliveryDate, deliveryDateTo);
        if (StringUtils.hasText(status)) {
            wrapper.eq(SalesContractDO::getStatus, status);
        } else if (!includeCancelled) {
            wrapper.ne(SalesContractDO::getStatus, "CANCELLED");
        }
        return selectList(wrapper);
    }

}
