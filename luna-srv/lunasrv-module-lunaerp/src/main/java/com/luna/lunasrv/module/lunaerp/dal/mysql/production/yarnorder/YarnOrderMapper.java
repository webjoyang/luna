package com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.mybatis.core.mapper.BaseMapperX;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产管理-纱线采购单主表 Mapper
 *
 * @author luna
 */
@Mapper
public interface YarnOrderMapper extends BaseMapperX<YarnOrderDO> {

    /**
     * 回收站分页查询已软删草稿纱线采购单（手写 XML：显式 WHERE deleted = 1 AND status = 'DRAFT' AND tenant_id = #{tenantId}）
     */
    IPage<RecycleItemRespVO> selectRecyclePage(IPage<RecycleItemRespVO> page, @Param("tenantId") Long tenantId);

    /**
     * 查询指定租户下已软删的草稿纱线采购单
     */
    YarnOrderDO selectDeletedById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 恢复草稿纱线采购单（并发条件更新带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int restoreYarnOrder(@Param("id") Long id, @Param("tenantId") Long tenantId,
                         @Param("updater") String updater, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 彻底删除草稿纱线采购单（物理删除带 deleted=1 AND status='DRAFT' AND tenant_id=?）
     */
    int purgeYarnOrder(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 检查纱线采购单是否存在活跃业务引用（deleted=0 且 tenant_id=?）
     */
    Long selectActiveReferenceCount(@Param("yarnOrderId") Long yarnOrderId, @Param("tenantId") Long tenantId);

    /**
     * 按单号前缀查询采购单，【包含已逻辑删除的行】。
     *
     * 采购单号 (tenant_id, order_no) 全量唯一、软删不释放编号，
     * 取号扫描必须覆盖已删除行，否则软删单据的编号会被重新分配。
     * 手写 SQL 绕过 MyBatis-Plus 逻辑删除自动过滤；
     * tenant_id 由租户拦截器自动追加，租户隔离不受影响。
     *
     * @param prefix 单号前缀，如 YP20260920
     * @return 采购单列表（含已删除行）
     */
    @Select("SELECT id, order_no, source_instructions, order_date, delivery_date, loss_rate, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    List<YarnOrderDO> selectListByOrderNoPrefixIncludingDeleted(@Param("prefix") String prefix);

    /**
     * 按单号精确查询采购单，【包含已逻辑删除的行】。
     *
     * 手填单号查重同样必须覆盖已删除行——软删编号已被占用，
     * 与之重复的新单号应报 YARN_ORDER_NO_DUPLICATE 而非放行。
     * 手写 SQL 绕过逻辑删除自动过滤；tenant_id 由租户拦截器自动追加。
     *
     * @param orderNo 采购单号
     * @return 采购单（含已删除行），不存在返回 null
     */
    @Select("SELECT id, order_no, source_instructions, order_date, delivery_date, loss_rate, status, remark, "
            + "creator, create_time, updater, update_time, deleted, tenant_id "
            + "FROM lunaerp_production_yarn_order WHERE order_no = #{orderNo} LIMIT 1")
    YarnOrderDO selectByOrderNoIncludingDeleted(@Param("orderNo") String orderNo);

}
