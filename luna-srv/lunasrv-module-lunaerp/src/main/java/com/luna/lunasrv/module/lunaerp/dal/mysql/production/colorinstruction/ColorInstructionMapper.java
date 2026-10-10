package com.luna.lunasrv.module.lunaerp.dal.mysql.production.colorinstruction;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.colorinstruction.ColorInstructionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产管理 - 打色指示单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface ColorInstructionMapper extends BaseMapperX<ColorInstructionDO> {

    /**
     * 按单号精确查询打色单，【包含已逻辑删除的行】。
     *
     * ADR-0006：打色单号 (tenant_id, instruction_no) 全量唯一、软删不释放编号，
     * 查重必须覆盖已删除行。手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；
     * tenant_id 由租户拦截器自动追加。
     *
     * @param instructionNo 打色单号
     * @return 打色单（含已删除行），不存在返回 null
     */
    @Select("SELECT id, instruction_no, customer_id, client_name, contact_person, product_id, product_code, product_name, "
            + "lab_factory_id, lab_factory_name, composition, width, gsm, instruction_date, delivery_date, status, "
            + "confirm_type, customer_feedback, confirmed_by, confirmed_at, void_reason, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_color_instruction WHERE instruction_no = #{instructionNo} LIMIT 1")
    ColorInstructionDO selectByInstructionNoIncludingDeleted(@Param("instructionNo") String instructionNo);

    /**
     * 按单号前缀查询打色单，【包含已逻辑删除的行】。
     *
     * @param prefix 单号前缀，如 DS20261010
     * @return 打色单列表（含已删除行）
     */
    @Select("SELECT id, instruction_no, customer_id, client_name, contact_person, product_id, product_code, product_name, "
            + "lab_factory_id, lab_factory_name, composition, width, gsm, instruction_date, delivery_date, status, "
            + "confirm_type, customer_feedback, confirmed_by, confirmed_at, void_reason, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_color_instruction WHERE instruction_no LIKE CONCAT(#{prefix}, '%')")
    List<ColorInstructionDO> selectListByInstructionNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 回收站分页查询已软删打色单（手写 XML：显式 WHERE deleted = 1 AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的打色单记录
     */
    ColorInstructionDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复打色单（并发条件更新带 deleted=1 AND tenant_id=?）
     */
    int restoreColorInstruction(@Param("id") Long id, @Param("tenantId") Long tenantId,
                                @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除打色单（物理删除带 deleted=1 AND tenant_id=?）
     */
    int purgeColorInstruction(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查打色单是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("instructionId") Long instructionId, @Param("tenantId") Long tenantId);

}
