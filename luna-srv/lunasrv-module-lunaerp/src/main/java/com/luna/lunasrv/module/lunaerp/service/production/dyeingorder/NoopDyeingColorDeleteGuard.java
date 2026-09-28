package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * 染色单颜色行删除守卫缺省空实现（阶段二进度反馈未建表前的留桩）
 *
 * @author luna
 */
@Component
public class NoopDyeingColorDeleteGuard implements DyeingColorDeleteGuard {

    @Override
    public void validateCanDeleteColors(Long dyeingOrderId, Collection<Long> colorIds) {
        // TODO: 阶段二进度反馈上线后，若 colorIds 存在关联的反馈缸则抛出 1-030-017-004
    }

}
