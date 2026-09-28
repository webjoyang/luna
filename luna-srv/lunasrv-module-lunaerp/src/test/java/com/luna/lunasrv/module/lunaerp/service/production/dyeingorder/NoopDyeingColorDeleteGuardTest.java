package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * {@link NoopDyeingColorDeleteGuard} 单元测试。
 * 验证阶段二进度反馈表上线前，缺省留桩组件不阻断任何删除操作。
 *
 * @author luna
 */
class NoopDyeingColorDeleteGuardTest {

    private final NoopDyeingColorDeleteGuard guard = new NoopDyeingColorDeleteGuard();

    @Test
    void validateCanDeleteColors_emptyOrNonEmpty_doesNotThrow() {
        assertDoesNotThrow(() -> guard.validateCanDeleteColors(1001L, Collections.emptyList()));
        assertDoesNotThrow(() -> guard.validateCanDeleteColors(1001L, Set.of(501L, 502L)));
        assertDoesNotThrow(() -> guard.validateCanDeleteColors(null, null));
    }

}
