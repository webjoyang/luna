package com.luna.lunasrv.module.lunaerp.approval.handler;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnRollDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnRollMapper;
import com.luna.lunasrv.module.lunaerp.service.stock.stockin.StockInService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.SALES_RETURN_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link SalesReturnApprovalHandler} 单元测试
 *
 * 覆盖 Issue 04:
 * - supports 支持 sales_return，拒绝其他业务类型
 * - 销售退货不存在时抛出 1_030_037_000 异常
 * - 重复审批事件幂等拦截（已有 stockInId 不重建）
 * - 审批通过同事务通过 StockInService 生成 DRAFT 入库单与逐卷明细，回写 stockInId 并置 status=APPROVED / approvalStatus=APPROVED
 * - 单位为 M 时优先取卷米数作为入库明细数量
 */
class SalesReturnApprovalHandlerTest {

    @Mock
    private SalesReturnMapper salesReturnMapper;

    @Mock
    private SalesReturnItemMapper salesReturnItemMapper;

    @Mock
    private SalesReturnRollMapper salesReturnRollMapper;

    @Mock
    private StockInService stockInService;

    @InjectMocks
    private SalesReturnApprovalHandler handler;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesReturnDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesReturnItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesReturnRollDO.class);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("supports 校验：支持 sales_return（大小写不敏感）")
    void testSupports() {
        assertTrue(handler.supports("sales_return"));
        assertTrue(handler.supports("SALES_RETURN"));
        assertFalse(handler.supports("sales_contract"));
        assertFalse(handler.supports("payment"));
        assertFalse(handler.supports(null));
    }

    @Test
    @DisplayName("onApproved：退货单不存在抛出 SALES_RETURN_NOT_EXISTS")
    void testOnApproved_NotFound_ThrowsException() {
        when(salesReturnMapper.selectOneForUpdate(any(), eq(999L))).thenReturn(null);
        when(salesReturnMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.onApproved(999L));
        assertEquals(SALES_RETURN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("onApproved：已有 stockInId 幂等跳过，不重复建入库单")
    void testOnApproved_Idempotent_WhenStockInIdExists() {
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(10L);
        salesReturn.setStockInId(888L);
        salesReturn.setStatus("APPROVED");
        salesReturn.setApprovalStatus("APPROVED");

        when(salesReturnMapper.selectOneForUpdate(any(), eq(10L))).thenReturn(salesReturn);

        handler.onApproved(10L);

        verify(stockInService, never()).createStockIn(any());
        verify(salesReturnMapper, never()).updateById(any(SalesReturnDO.class));
    }

    @Test
    @DisplayName("onApproved：正常审批通过，生成 DRAFT 入库单与逐卷明细并回写退货单")
    void testOnApproved_Success_GeneratesStockInAndUpdatesSalesReturn() {
        Long returnId = 10L;
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(returnId);
        salesReturn.setReturnNo("RT-20261018-001");
        salesReturn.setStatus("DRAFT");
        salesReturn.setApprovalStatus("APPROVING");
        salesReturn.setWarehouseId(100L);
        salesReturn.setWarehouseName("成品一号仓");
        salesReturn.setSalesContractId(300L);
        salesReturn.setContractNo("CH20260001");
        salesReturn.setRemark("整单退货");

        when(salesReturnMapper.selectOneForUpdate(any(), eq(returnId))).thenReturn(salesReturn);

        // 明细行
        SalesReturnItemDO item = new SalesReturnItemDO();
        item.setId(101L);
        item.setReturnId(returnId);
        item.setProductId(201L);
        item.setProductCode("PRD-001");
        item.setProductName("高弹面料");
        item.setVatNo("VAT-2026-01");
        item.setUnit("KG");
        item.setReturnQty(new BigDecimal("50.00"));
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        // 卷明细（2卷）
        SalesReturnRollDO roll1 = new SalesReturnRollDO();
        roll1.setId(1001L);
        roll1.setReturnId(returnId);
        roll1.setReturnItemId(101L);
        roll1.setRollNo("R-01");
        roll1.setWeight(new BigDecimal("24.50"));
        roll1.setLengthM(new BigDecimal("30.00"));
        roll1.setRemark("首卷");

        SalesReturnRollDO roll2 = new SalesReturnRollDO();
        roll2.setId(1002L);
        roll2.setReturnId(returnId);
        roll2.setReturnItemId(101L);
        roll2.setRollNo("R-02");
        roll2.setWeight(new BigDecimal("25.50"));
        roll2.setLengthM(new BigDecimal("31.00"));

        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll1, roll2));
        when(stockInService.createStockIn(any(StockInSaveReqVO.class))).thenReturn(5001L);
        StockInRespVO stockInResp = new StockInRespVO();
        stockInResp.setId(5001L);
        stockInResp.setInNo("IN-20261018-001");
        when(stockInService.getStockIn(5001L)).thenReturn(stockInResp);

        handler.onApproved(returnId);

        // 1. 验证调用 StockInService.createStockIn 的参数组装
        ArgumentCaptor<StockInSaveReqVO> reqCaptor = ArgumentCaptor.forClass(StockInSaveReqVO.class);
        verify(stockInService).createStockIn(reqCaptor.capture());
        StockInSaveReqVO reqVO = reqCaptor.getValue();
        assertEquals("RETURN", reqVO.getInType());
        assertEquals("SALES_RETURN", reqVO.getSourceDocType());
        assertEquals(returnId, reqVO.getSourceDocId());
        assertEquals(100L, reqVO.getWarehouseId());
        assertEquals(300L, reqVO.getSalesContractId());

        // 2. 验证逐卷入库明细组装（每卷一条，金额恒 0）
        List<StockInItemSaveReqVO> createdItems = reqVO.getItems();
        assertNotNull(createdItems);
        assertEquals(2, createdItems.size());

        StockInItemSaveReqVO item1 = createdItems.get(0);
        assertEquals("PRODUCT", item1.getItemType());
        assertEquals(201L, item1.getProductId());
        assertEquals("VAT-2026-01", item1.getBatchNo());
        assertEquals("R-01", item1.getRollNo());
        assertEquals(1, item1.getPieceCount());
        assertEquals(new BigDecimal("24.50"), item1.getQuantity());
        assertEquals("KG", item1.getUnit());
        assertEquals(BigDecimal.ZERO, item1.getUnitPrice());
        assertEquals(BigDecimal.ZERO, item1.getAmount());
        assertEquals("首卷", item1.getRemark());

        StockInItemSaveReqVO item2 = createdItems.get(1);
        assertEquals("R-02", item2.getRollNo());
        assertEquals(new BigDecimal("25.50"), item2.getQuantity());
        assertEquals(BigDecimal.ZERO, item2.getUnitPrice());
        assertEquals(BigDecimal.ZERO, item2.getAmount());

        // 3. 验证退货单回写
        ArgumentCaptor<SalesReturnDO> returnCaptor = ArgumentCaptor.forClass(SalesReturnDO.class);
        verify(salesReturnMapper).updateById(returnCaptor.capture());
        SalesReturnDO updatedReturn = returnCaptor.getValue();
        assertEquals(5001L, updatedReturn.getStockInId());
        assertEquals("APPROVED", updatedReturn.getStatus());
        assertEquals("APPROVED", updatedReturn.getApprovalStatus());
    }

    @Test
    @DisplayName("onApproved：单位为 M 时，入库明细数量优先取卷米数 lengthM")
    void testOnApproved_WithMeterUnit_TakesLengthM() {
        Long returnId = 20L;
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(returnId);
        salesReturn.setReturnNo("RT-20261018-002");
        salesReturn.setStatus("DRAFT");
        salesReturn.setApprovalStatus("APPROVING");
        salesReturn.setWarehouseId(100L);

        when(salesReturnMapper.selectOneForUpdate(any(), eq(returnId))).thenReturn(salesReturn);

        SalesReturnItemDO item = new SalesReturnItemDO();
        item.setId(201L);
        item.setReturnId(returnId);
        item.setProductId(202L);
        item.setUnit("M");
        item.setReturnQty(new BigDecimal("100.00"));
        when(salesReturnItemMapper.selectList(any())).thenReturn(List.of(item));

        SalesReturnRollDO roll = new SalesReturnRollDO();
        roll.setId(2001L);
        roll.setReturnId(returnId);
        roll.setReturnItemId(201L);
        roll.setRollNo("R-M-01");
        roll.setWeight(new BigDecimal("15.20"));
        roll.setLengthM(new BigDecimal("98.50"));
        when(salesReturnRollMapper.selectList(any())).thenReturn(List.of(roll));

        when(stockInService.createStockIn(any(StockInSaveReqVO.class))).thenReturn(6001L);

        handler.onApproved(returnId);

        ArgumentCaptor<StockInSaveReqVO> reqCaptor = ArgumentCaptor.forClass(StockInSaveReqVO.class);
        verify(stockInService).createStockIn(reqCaptor.capture());
        StockInSaveReqVO req = reqCaptor.getValue();
        assertNotNull(req.getItems());
        assertEquals(1, req.getItems().size());
        StockInItemSaveReqVO createdItem = req.getItems().get(0);
        assertEquals("M", createdItem.getUnit());
        assertEquals(new BigDecimal("98.50"), createdItem.getQuantity());
        assertEquals(BigDecimal.ZERO, createdItem.getUnitPrice());
        assertEquals(BigDecimal.ZERO, createdItem.getAmount());
    }

    @Test
    @DisplayName("onApproved：当入库单生成服务抛出异常时，直接向外抛出以触发审批事务回滚")
    void testOnApproved_StockInServiceThrows_PropagatesExceptionForTransactionRollback() {
        Long returnId = 30L;
        SalesReturnDO salesReturn = new SalesReturnDO();
        salesReturn.setId(returnId);
        salesReturn.setReturnNo("RT-20261018-003");
        salesReturn.setStatus("DRAFT");
        salesReturn.setApprovalStatus("APPROVING");
        salesReturn.setWarehouseId(100L);

        when(salesReturnMapper.selectOneForUpdate(any(), eq(returnId))).thenReturn(salesReturn);
        when(salesReturnItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(salesReturnRollMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 模拟 stockInService 抛出运行时异常
        when(stockInService.createStockIn(any(StockInSaveReqVO.class)))
                .thenThrow(new RuntimeException("底层库存服务不可用"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> handler.onApproved(returnId));
        assertEquals("底层库存服务不可用", ex.getMessage());

        // 验证退货单主表状态绝不会被推进为 APPROVED
        verify(salesReturnMapper, never()).updateById(any(SalesReturnDO.class));
    }


    @Test
    @DisplayName("onApproved：bizId 为 null 时安全退出，不发起任何数据库或入库单调用")
    void testOnApproved_NullBizId_SafeReturn() {
        assertDoesNotThrow(() -> handler.onApproved(null));
        verifyNoInteractions(salesReturnMapper);
        verifyNoInteractions(stockInService);
    }

}

