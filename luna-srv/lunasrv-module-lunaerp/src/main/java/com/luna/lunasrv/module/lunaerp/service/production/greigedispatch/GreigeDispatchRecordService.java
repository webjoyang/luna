package com.luna.lunasrv.module.lunaerp.service.production.greigedispatch;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordUpdateReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigedispatch.GreigeDispatchRecordDO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 生产管理-发染记录 Service 接口（ADR-0033）
 *
 * @author luna
 */
public interface GreigeDispatchRecordService {

    /**
     * 创建发染记录（同事务写 PROCESS_OUT 库存流水，余额不足拦截）
     *
     * @param reqVO 创建信息
     * @return 发染记录ID
     */
    Long createGreigeDispatchRecord(GreigeDispatchRecordCreateReqVO reqVO);

    /**
     * 受限编辑发染记录（仅允许改染色单/染厂/备注，数量/日期/匹数/仓库锁定）
     *
     * @param reqVO 更新信息
     */
    void updateGreigeDispatchRecord(GreigeDispatchRecordUpdateReqVO reqVO);

    /**
     * 删除发染记录（同事务写 PROCESS_OUT_REVERSAL 红冲流水，恢复库存）
     *
     * @param id 发染记录ID
     */
    void deleteGreigeDispatchRecord(Long id);

    /**
     * 获得发染记录详情
     *
     * @param id 发染记录ID
     * @return 发染记录
     */
    GreigeDispatchRecordDO getGreigeDispatchRecord(Long id);

    /**
     * 获得发染记录分页
     *
     * @param reqVO 分页与筛选参数
     * @return 发染记录分页
     */
    PageResult<GreigeDispatchRecordDO> getGreigeDispatchRecordPage(GreigeDispatchRecordPageReqVO reqVO);

    /**
     * 按胚布单ID获得发染记录列表
     *
     * @param greigeOrderId 胚布单ID
     * @return 发染记录列表
     */
    List<GreigeDispatchRecordDO> getGreigeDispatchRecordListByGreigeOrderId(Long greigeOrderId);

    /**
     * 按染色单ID获得发染记录列表
     *
     * @param dyeingOrderId 染色单ID
     * @return 发染记录列表
     */
    List<GreigeDispatchRecordDO> getGreigeDispatchRecordListByDyeingOrderId(Long dyeingOrderId);

    /**
     * 按胚布单ID集合批量统计已发染量合计（排除软删）
     *
     * @param greigeOrderIds 胚布单ID集合
     * @return 胚布单ID → 已发染量 KG
     */
    java.util.Map<Long, BigDecimal> sumDispatchedQtyByGreigeOrderIds(Collection<Long> greigeOrderIds);

}
