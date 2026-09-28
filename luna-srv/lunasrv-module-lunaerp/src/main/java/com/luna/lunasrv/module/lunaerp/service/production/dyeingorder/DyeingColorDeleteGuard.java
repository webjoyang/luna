package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import java.util.Collection;

/**
 * 染色单颜色行删除守卫接口（扩展点，ADR-0032）
 *
 * 阶段二（进度反馈 ADR-0031 / ADR-0032）上线后，校验被删除的颜色行下是否存在反馈缸（错误码 1-030-017-004）。
 * 当前阶段默认空实现，不影响保存，禁止提前建反馈表。
 *
 * @author luna
 */
public interface DyeingColorDeleteGuard {

    /**
     * 校验指定染色单下待删除的颜色行是否允许删除
     *
     * @param dyeingOrderId 染色单ID
     * @param colorIds      待删除的颜色明细行 ID 集合
     */
    void validateCanDeleteColors(Long dyeingOrderId, Collection<Long> colorIds);

}
