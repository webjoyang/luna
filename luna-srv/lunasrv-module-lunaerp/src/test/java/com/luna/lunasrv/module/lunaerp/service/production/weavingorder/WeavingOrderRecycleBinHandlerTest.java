package com.luna.lunasrv.module.lunaerp.service.production.weavingorder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.task.MachineTaskMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link WeavingOrderRecycleBinHandler} 单元测试
 *
 * 验证织造单全级联逆向复活与彻底删除（机台任务、布票、领纱单、领纱明细）。
 *
 * @author luna
 */
public class WeavingOrderRecycleBinHandlerTest {

    @Mock
    private WeavingOrderMapper weavingOrderMapper;

    @Mock
    private MachineTaskMapper machineTaskMapper;

    @Mock
    private WeavingTicketMapper weavingTicketMapper;

    @Mock
    private YarnRequisitionMapper yarnRequisitionMapper;

    @Mock
    private YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private WeavingOrderRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long ORDER_ID = 7001L;
    private static final Long PRODUCT_ID = 901L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContextHolder.setTenantId(TENANT_ID_1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getBizType 返回 WEAVING_ORDER")
    void testGetBizType() {
        assertEquals("WEAVING_ORDER", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿织造单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("WEAVING_ORDER");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("WEAVING_ORDER");
            item.setBizId(ORDER_ID);
            item.setDisplayName("ZZ-PB0919001-01");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(weavingOrderMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("ZZ-PB0919001-01", result.getList().get(0).getDisplayName());
        verify(weavingOrderMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：草稿存在且关联产品正常")
    void testValidateRestore_success() {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(ORDER_ID);
        order.setProductId(PRODUCT_ID);

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("提花布");

        when(weavingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        assertDoesNotThrow(() -> handler.validateRestore(ORDER_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在或非草稿抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(weavingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：关联产品在回收站抛 1_030_031_002")
    void testValidateRestore_productInRecycle_throws1030031002() {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(ORDER_ID);
        order.setProductId(PRODUCT_ID);

        ProductDO deletedProduct = new ProductDO();
        deletedProduct.setId(PRODUCT_ID);
        deletedProduct.setName("已删除提花布");

        when(weavingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(deletedProduct);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除提花布"));
    }

    @Test
    @DisplayName("全级联逆向复活：主单 -> 机台任务 -> 布票 -> 领纱单 -> 领纱明细 顺序严格保持")
    void testRestore_success_strictParentToChildOrder() {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(ORDER_ID);

        when(weavingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(weavingOrderMapper.restoreWeavingOrder(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(ORDER_ID));

        InOrder inOrder = inOrder(weavingOrderMapper, machineTaskMapper, weavingTicketMapper, yarnRequisitionMapper, yarnRequisitionItemMapper);
        inOrder.verify(weavingOrderMapper).restoreWeavingOrder(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        inOrder.verify(machineTaskMapper).restoreByWeavingOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        inOrder.verify(weavingTicketMapper).restoreByWeavingOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        inOrder.verify(yarnRequisitionMapper).restoreByWeavingOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        inOrder.verify(yarnRequisitionItemMapper).restoreByWeavingOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("全级联彻底删除：领纱明细 -> 领纱单 -> 布票 -> 机台任务 -> 主单 顺序严格保持")
    void testPurge_success_strictChildToParentOrder() {
        WeavingOrderDO order = new WeavingOrderDO();
        order.setId(ORDER_ID);

        when(weavingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(weavingOrderMapper.selectActiveReferenceCount(ORDER_ID, TENANT_ID_1)).thenReturn(0L);
        when(weavingOrderMapper.purgeWeavingOrder(ORDER_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(ORDER_ID));

        InOrder inOrder = inOrder(yarnRequisitionItemMapper, yarnRequisitionMapper, weavingTicketMapper, machineTaskMapper, weavingOrderMapper);
        inOrder.verify(yarnRequisitionItemMapper).purgeByWeavingOrderId(ORDER_ID, TENANT_ID_1);
        inOrder.verify(yarnRequisitionMapper).purgeByWeavingOrderId(ORDER_ID, TENANT_ID_1);
        inOrder.verify(weavingTicketMapper).purgeByWeavingOrderId(ORDER_ID, TENANT_ID_1);
        inOrder.verify(machineTaskMapper).purgeByWeavingOrderId(ORDER_ID, TENANT_ID_1);
        inOrder.verify(weavingOrderMapper).purgeWeavingOrder(ORDER_ID, TENANT_ID_1);
    }

}
