package com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.supplier.vo.SupplierPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 基本信息-供应商表 Mapper
 *
 * 列表 search 智能词命中 supplier_no / company_name / short_name / phone 之一，
 * PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 OR ILIKE 语义（占位符走预编译，无注入风险）。
 * 逻辑删除（deleted=0）与租户条件由框架自动追加。
 *
 * 编号取号与查重扫描【包含已逻辑删除的行】（ADR-0005）：手写 SQL 绕过 MyBatis-Plus
 * 逻辑删除自动过滤，tenant_id 由租户拦截器自动追加，租户隔离不受影响。
 *
 * @author luna
 */
@Mapper
public interface SupplierMapper extends BaseMapperX<SupplierDO> {

    /**
     * 供应商分页查询：search 智能词 OR 四字段 ILIKE + supplierType/status 等值过滤。
     *
     * @param reqVO 分页与查询条件
     * @return 供应商分页
     */
    default PageResult<SupplierDO> selectPage(SupplierPageReqVO reqVO) {
        LambdaQueryWrapperX<SupplierDO> wrapper = new LambdaQueryWrapperX<>();
        // supplierType / status 等值过滤（为空则不过滤，便于"全部"场景）
        wrapper.eqIfPresent(SupplierDO::getSupplierType, reqVO.getSupplierType());
        wrapper.eqIfPresent(SupplierDO::getStatus, reqVO.getStatus());
        // search 智能词：命中 supplier_no / company_name / short_name / phone 之一
        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (!search.isEmpty()) {
            String like = "%" + search + "%";
            wrapper.and(w -> w
                    .apply("LOWER(supplier_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(company_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(short_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(phone) LIKE LOWER({0})", like));
        }
        wrapper.orderByDesc(SupplierDO::getCreateTime);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 按编号前缀查询供应商，【包含已逻辑删除的行】。
     *
     * ADR-0005：供应商编号 (tenant_id, supplier_no) 全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删供应商的编号会被重新分配。
     * 因此此处用手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤（deleted=0 条件不会追加）；
     * tenant_id 由租户拦截器自动追加，租户隔离不受影响。勿"修复"为 wrapper 查询。
     *
     * @param prefix 编号前缀，如 SUP2026
     * @return 供应商列表（含已删除行）
     */
    @Select("SELECT id, supplier_no, company_name, short_name, phone, address, supplier_type, machine_type, "
            + "remark, status, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_baseinfo_supplier WHERE supplier_no LIKE CONCAT(#{prefix}, '%')")
    List<SupplierDO> selectListBySupplierNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按编号精确查询供应商，【包含已逻辑删除的行】。
     *
     * ADR-0005：手填编号查重同样必须覆盖已删除行——软删编号已被占用，
     * 与之重复的新编号应报 SUPPLIER_NO_DUPLICATE 而非放行。
     * 手写 SQL 绕过逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param supplierNo 供应商编号
     * @return 供应商（含已删除行），不存在返回 null
     */
    @Select("SELECT id, supplier_no, company_name, short_name, phone, address, supplier_type, machine_type, "
            + "remark, status, creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_baseinfo_supplier WHERE supplier_no = #{supplierNo} LIMIT 1")
    SupplierDO selectBySupplierNoIncludingDeleted(@Param("supplierNo") String supplierNo);

}
