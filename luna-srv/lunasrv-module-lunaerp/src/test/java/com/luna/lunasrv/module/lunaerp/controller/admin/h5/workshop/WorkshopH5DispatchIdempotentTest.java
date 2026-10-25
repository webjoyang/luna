package com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.luna.lunasrv.framework.common.pojo.CommonResult;
import com.luna.lunasrv.framework.idempotent.core.aop.IdempotentAspect;
import com.luna.lunasrv.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import com.luna.lunasrv.framework.idempotent.core.redis.IdempotentRedisDAO;
import com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop.vo.WorkshopRequisitionDispatchReqVO;
import com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants;
import com.luna.lunasrv.module.lunaerp.service.h5.workshop.WorkshopH5Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link WorkshopH5Controller} 发纱端点幂等性集成测试（Issue 18）
 *
 * 验收规则：
 * 1. 同一 requestId 重复提交（含网络超时客户端重发重试）在窗口期内被拦截，下游出库仅执行 1 次；
 * 2. 不同 requestId 允许正常二次发纱出库；
 * 3. 业务异常（如 015-311 库存不足）时自动释放幂等 Key，允许后续重试。
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("车间移动 H5 - 发纱端点 @Idempotent 幂等守卫单测")
class WorkshopH5DispatchIdempotentTest {

    @Mock
    private WorkshopH5Service workshopH5Service;

    @Mock
    private IdempotentRedisDAO idempotentRedisDAO;

    @InjectMocks
    private WorkshopH5Controller rawController;

    private WorkshopH5Controller proxyController;
    private final ConcurrentHashMap<String, Boolean> redisStorage = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        redisStorage.clear();

        // 模拟 Redis setIfAbsent 语义：若 key 已经存在则返回 false，否则写入并返回 true
        lenient().when(idempotentRedisDAO.setIfAbsent(anyString(), anyLong(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisStorage.putIfAbsent(key, true) == null;
        });

        // 模拟 Redis delete 语义：移除 key
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            redisStorage.remove(key);
            return null;
        }).when(idempotentRedisDAO).delete(anyString());

        // 使用 Spring AspectJProxyFactory 织入生产环境配置的 IdempotentAspect
        IdempotentAspect aspect = new IdempotentAspect(
                List.of(new ExpressionIdempotentKeyResolver()),
                idempotentRedisDAO
        );
        AspectJProxyFactory factory = new AspectJProxyFactory(rawController);
        factory.addAspect(aspect);
        proxyController = factory.getProxy();
    }

    @Test
    @DisplayName("发纱幂等：同 requestId 连发两次（含网络超时重试），首次成功，第二次拦截且下游只执行一次")
    void testDispatchYarn_duplicateRequestId_intercepted() {
        String requestId = "550e8400-e29b-41d4-a716-446655440001";
        WorkshopRequisitionDispatchReqVO reqVO = WorkshopRequisitionDispatchReqVO.builder()
                .itemId(1001L)
                .warehouseId(1L)
                .dispatchQty(new BigDecimal("25.00"))
                .requestId(requestId)
                .build();

        // 1. 首次请求：正常执行出库
        CommonResult<Boolean> firstResult = proxyController.dispatchYarn(reqVO);
        assertTrue(firstResult.isSuccess());
        assertTrue(firstResult.getData());
        verify(workshopH5Service, times(1)).dispatchYarn(reqVO);

        // 2. 模拟客户端超时或双击产生同 requestId 重试请求：切面拦截抛 REPEATED_REQUESTS
        ServiceException ex = assertThrows(ServiceException.class, () -> proxyController.dispatchYarn(reqVO));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        assertEquals("发纱正在处理，请勿重复提交", ex.getMessage());

        // 3. 验证下游核心 Service（库存扣减与流水）只被调用了一次
        verify(workshopH5Service, times(1)).dispatchYarn(any());
    }

    @Test
    @DisplayName("发纱幂等：不同 requestId 正常允许二次发纱")
    void testDispatchYarn_differentRequestId_allowed() {
        WorkshopRequisitionDispatchReqVO reqVO1 = WorkshopRequisitionDispatchReqVO.builder()
                .itemId(1001L)
                .warehouseId(1L)
                .dispatchQty(new BigDecimal("10.00"))
                .requestId("550e8400-e29b-41d4-a716-446655440002")
                .build();

        WorkshopRequisitionDispatchReqVO reqVO2 = WorkshopRequisitionDispatchReqVO.builder()
                .itemId(1001L)
                .warehouseId(1L)
                .dispatchQty(new BigDecimal("15.00"))
                .requestId("550e8400-e29b-41d4-a716-446655440003")
                .build();

        // 两个不同 requestId 的请求均能正常通过切面
        CommonResult<Boolean> result1 = proxyController.dispatchYarn(reqVO1);
        CommonResult<Boolean> result2 = proxyController.dispatchYarn(reqVO2);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        verify(workshopH5Service, times(1)).dispatchYarn(reqVO1);
        verify(workshopH5Service, times(1)).dispatchYarn(reqVO2);
    }

    @Test
    @DisplayName("发纱幂等：库存不足 015-311 异常时释放 Key，允许修正参数后重试")
    void testDispatchYarn_exceptionDeletesKey_allowsRetry() {
        String requestId = "550e8400-e29b-41d4-a716-446655440004";
        WorkshopRequisitionDispatchReqVO reqVO = WorkshopRequisitionDispatchReqVO.builder()
                .itemId(1001L)
                .warehouseId(1L)
                .dispatchQty(new BigDecimal("9999.00"))
                .requestId(requestId)
                .build();

        // 首次调用抛库存不足异常
        doThrow(new ServiceException(ErrorCodeConstants.YARN_REQUISITION_STOCK_NOT_ENOUGH.getCode(), "库存不足"))
                .doNothing()
                .when(workshopH5Service).dispatchYarn(reqVO);

        ServiceException ex = assertThrows(ServiceException.class, () -> proxyController.dispatchYarn(reqVO));
        assertEquals(ErrorCodeConstants.YARN_REQUISITION_STOCK_NOT_ENOUGH.getCode(), ex.getCode());

        // 确认切面捕获异常后调用了 deleteKey
        verify(idempotentRedisDAO, atLeastOnce()).delete(requestId);
        assertFalse(redisStorage.containsKey(requestId), "异常后 redis 中 key 必须被清理");

        // 再次提交同 requestId 请求，不再受限并正常执行
        CommonResult<Boolean> retryResult = proxyController.dispatchYarn(reqVO);
        assertTrue(retryResult.isSuccess());
        verify(workshopH5Service, times(2)).dispatchYarn(reqVO);
    }

}
