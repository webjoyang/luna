package com.luna.lunasrv.module.lunaerp.controller.common.recycle;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandler;
import com.luna.lunasrv.module.lunaerp.common.recycle.RecycleBinHandlerRegistry;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleRestoreReqVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link RecycleBinController} 单元测试
 *
 * @author luna
 */
public class RecycleBinControllerTest {

    @Mock
    private RecycleBinHandlerRegistry registry;

    @Mock
    private RecycleBinHandler handler;

    @InjectMocks
    private RecycleBinController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(registry.getHandler("CUSTOMER")).thenReturn(handler);
        when(handler.getBizType()).thenReturn("CUSTOMER");
    }

    @Test
    @DisplayName("分页查询已软删列表")
    void testGetRecyclePage() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("CUSTOMER");
        req.setPageNo(1);
        req.setPageSize(10);

        PageResult<RecycleItemRespVO> expectedResult = new PageResult<>(Collections.emptyList(), 0L);
        when(handler.pageDeleted(req)).thenReturn(expectedResult);

        CommonResult<PageResult<RecycleItemRespVO>> result = controller.getRecyclePage(req);
        assertTrue(result.isSuccess());
        assertEquals(expectedResult, result.getData());
        verify(handler).pageDeleted(req);
    }

    @Test
    @DisplayName("恢复数据项：前置校验并执行恢复")
    void testRestore_success() {
        RecycleRestoreReqVO req = new RecycleRestoreReqVO();
        req.setBizType("CUSTOMER");
        req.setBizId(1024L);

        CommonResult<Boolean> result = controller.restore(req);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());

        verify(handler).validateRestore(1024L);
        verify(handler).restore(1024L);
    }

    @Test
    @DisplayName("彻底删除：无引用时成功执行物理删除")
    void testPurge_success() {
        when(handler.hasActiveReference(1024L)).thenReturn(false);

        CommonResult<Boolean> result = controller.purge("CUSTOMER", 1024L, null);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());

        verify(handler).hasActiveReference(1024L);
        verify(handler).purge(1024L);
    }

    @Test
    @DisplayName("彻底删除：支持通过 Body 传递参数")
    void testPurge_withBodyReq() {
        when(handler.hasActiveReference(1024L)).thenReturn(false);

        RecycleRestoreReqVO bodyReq = new RecycleRestoreReqVO();
        bodyReq.setBizType("CUSTOMER");
        bodyReq.setBizId(1024L);

        CommonResult<Boolean> result = controller.purge(null, null, bodyReq);
        assertTrue(result.isSuccess());
        assertTrue(result.getData());

        verify(handler).hasActiveReference(1024L);
        verify(handler).purge(1024L);
    }

    @Test
    @DisplayName("彻底删除：缺少参数抛 BAD_REQUEST")
    void testPurge_missingParams_throwsBadRequest() {
        ServiceException ex = assertThrows(ServiceException.class, () -> controller.purge(null, null, null));
        assertEquals(BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("彻底删除：存在活跃引用时抛 1_030_031_003 阻断")
    void testPurge_hasActiveReference_throws1030031003() {
        when(handler.hasActiveReference(1024L)).thenReturn(true);

        ServiceException ex = assertThrows(ServiceException.class, () -> controller.purge("CUSTOMER", 1024L, null));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(handler, never()).purge(any());
    }

}
