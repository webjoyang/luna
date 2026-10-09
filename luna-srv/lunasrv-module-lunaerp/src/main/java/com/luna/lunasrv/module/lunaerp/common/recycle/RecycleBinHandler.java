package com.luna.lunasrv.module.lunaerp.common.recycle;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;

/**
 * 回收站业务处理器 SPI 接口（ADR-0038 / W0 Spec）
 *
 * 各业务模块实现此接口并注册为 Spring Bean，由 {@link RecycleBinHandlerRegistry} 统一按 bizType 调度。
 *
 * @author luna
 */
public interface RecycleBinHandler {

    /**
     * 业务对象类型（与 lunaerp_recycle_biz_type 字典一致，如 CUSTOMER 等）
     *
     * @return 业务类型
     */
    String getBizType();

    /**
     * 分页查询已软删行（手写 XML：显式 WHERE deleted=1 AND tenant_id=?，绕开 MP 逻辑删除插件）
     *
     * @param req 分页参数
     * @return 分页结果
     */
    PageResult<RecycleItemRespVO> pageDeleted(RecyclePageReqVO req);

    /**
     * 恢复前引用检查：引用的上游主数据仍在回收站时抛出 1_030_031_002
     *
     * @param id 业务主键
     */
    void validateRestore(Long id);

    /**
     * 恢复数据（手写 XML：显式 UPDATE ... SET deleted=0，同事务父→子复活，恢复为主数据 INACTIVE 或单据 DRAFT）
     *
     * @param id 业务主键
     */
    void restore(Long id);

    /**
     * 彻底删除前的引用守卫：检查是否存在未删除（deleted=0）的业务引用
     *
     * @param id 业务主键
     * @return true 若存在活跃引用，false 否则
     */
    boolean hasActiveReference(Long id);

    /**
     * 彻底删除（物理删除，手写 XML：DELETE FROM ...，同事务级联删除子表）
     *
     * @param id 业务主键
     */
    void purge(Long id);

}
