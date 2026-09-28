package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.ProgressFeedbackMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_COLOR_HAS_VATS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link ProgressFeedbackDyeingColorDeleteGuard} 纯 Mockito 单元测试。
 *
 * 覆盖 Issue 06 染色单颜色行删除守卫需求：
 * 1. 待删除颜色行列表为空或无缸时放行
 * 2. 颜色行下存在活跃反馈缸时硬拦截抛 1-030-017-004
 * 3. 错误提示包含颜色名称与缸数
 * 4. 颜色行名称为空时降级展示客户色号
 * 5. 颜色行下仅存在软删反馈缸时正常放行
 *
 * @author luna
 */
public class ProgressFeedbackDyeingColorDeleteGuardTest {

    @Mock
    private ProgressFeedbackMapper progressFeedbackMapper;

    @Mock
    private DyeingColorMapper dyeingColorMapper;

    @InjectMocks
    private ProgressFeedbackDyeingColorDeleteGuard guard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("待删除颜色行为空集合：不抛异常且不查库")
    void testValidateCanDeleteColors_emptyList_doesNothing() {
        assertDoesNotThrow(() -> guard.validateCanDeleteColors(1001L, Collections.emptyList()));
        assertDoesNotThrow(() -> guard.validateCanDeleteColors(1001L, null));
        verify(progressFeedbackMapper, never()).selectListByColorIds(any());
    }

    @Test
    @DisplayName("待删除颜色行无关联反馈缸：正常放行")
    void testValidateCanDeleteColors_noVats_doesNothing() {
        List<Long> colorIds = List.of(501L, 502L);
        when(progressFeedbackMapper.selectListByColorIds(colorIds)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> guard.validateCanDeleteColors(1001L, colorIds));
        verify(dyeingColorMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("待删除颜色行存在活跃反馈缸：抛 1-030-017-004 且包含颜色名称与缸数")
    void testValidateCanDeleteColors_hasActiveVats_throws004WithColorNameAndCount() {
        List<Long> colorIds = List.of(501L);

        ProgressFeedbackDO vat1 = new ProgressFeedbackDO();
        vat1.setId(101L);
        vat1.setDyeingColorId(501L);
        vat1.setDeleted(false);

        ProgressFeedbackDO vat2 = new ProgressFeedbackDO();
        vat2.setId(102L);
        vat2.setDyeingColorId(501L);
        vat2.setDeleted(false);

        when(progressFeedbackMapper.selectListByColorIds(colorIds)).thenReturn(List.of(vat1, vat2));

        DyeingColorDO color = new DyeingColorDO();
        color.setId(501L);
        color.setColorName("藏青");
        color.setCustomerColorNo("NV-001");
        when(dyeingColorMapper.selectById(501L)).thenReturn(color);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> guard.validateCanDeleteColors(1001L, colorIds));

        assertEquals(PROGRESS_FEEDBACK_COLOR_HAS_VATS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("藏青"));
        assertTrue(ex.getMessage().contains("2"));
    }

    @Test
    @DisplayName("颜色名称为空时降级展示客户色号")
    void testValidateCanDeleteColors_fallbackCustomerColorNo() {
        List<Long> colorIds = List.of(502L);

        ProgressFeedbackDO vat1 = new ProgressFeedbackDO();
        vat1.setId(103L);
        vat1.setDyeingColorId(502L);
        vat1.setDeleted(false);

        when(progressFeedbackMapper.selectListByColorIds(colorIds)).thenReturn(List.of(vat1));

        DyeingColorDO color = new DyeingColorDO();
        color.setId(502L);
        color.setColorName("");
        color.setCustomerColorNo("PAN-BLACK-88");
        when(dyeingColorMapper.selectById(502L)).thenReturn(color);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> guard.validateCanDeleteColors(1001L, colorIds));

        assertEquals(PROGRESS_FEEDBACK_COLOR_HAS_VATS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("PAN-BLACK-88"));
        assertTrue(ex.getMessage().contains("1"));
    }

    @Test
    @DisplayName("颜色行下仅存在软删反馈缸：正常放行不拦截")
    void testValidateCanDeleteColors_onlyDeletedVats_doesNothing() {
        List<Long> colorIds = List.of(503L);

        ProgressFeedbackDO vat1 = new ProgressFeedbackDO();
        vat1.setId(104L);
        vat1.setDyeingColorId(503L);
        vat1.setDeleted(true);

        when(progressFeedbackMapper.selectListByColorIds(colorIds)).thenReturn(List.of(vat1));

        assertDoesNotThrow(() -> guard.validateCanDeleteColors(1001L, colorIds));
        verify(dyeingColorMapper, never()).selectById(any());
    }

}
