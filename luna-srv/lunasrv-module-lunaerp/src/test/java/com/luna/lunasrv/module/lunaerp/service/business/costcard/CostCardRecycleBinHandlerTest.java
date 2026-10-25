package com.luna.lunasrv.module.lunaerp.service.business.costcard;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.costcard.CostCardDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardFeeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.costcard.CostCardYarnMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CostCardRecycleBinHandlerTest {

    @Mock
    private CostCardMapper costCardMapper;

    @Mock
    private CostCardYarnMapper costCardYarnMapper;

    @Mock
    private CostCardFeeMapper costCardFeeMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private CostCardRecycleBinHandler handler;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("回收站分页查询")
    void testPageDeleted() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setPageNo(1);
        req.setPageSize(10);
        PageResult<RecycleItemRespVO> res = handler.pageDeleted(req);
        assertNotNull(res);
        verify(costCardMapper).selectRecyclePage(any(IPage.class), eq(1L));
    }

    @Test
    @DisplayName("恢复草稿成本卡：级联复活两子表")
    void testRestore_Success() {
        Long id = 100L;
        Long productId = 10L;
        CostCardDO card = CostCardDO.builder().id(id).productId(productId).build();
        when(costCardMapper.selectDeletedById(id, 1L)).thenReturn(card);
        when(productMapper.selectDeletedById(productId, 1L)).thenReturn(null);
        when(productMapper.selectById(productId)).thenReturn(new ProductDO());
        when(costCardMapper.restoreCostCard(eq(id), eq(1L), any(), any())).thenReturn(1);

        handler.restore(id);

        verify(costCardMapper).restoreCostCard(eq(id), eq(1L), any(), any());
        verify(costCardYarnMapper).restoreByCardId(eq(id), eq(1L), any(), any());
        verify(costCardFeeMapper).restoreByCardId(eq(id), eq(1L), any(), any());
    }

    @Test
    @DisplayName("恢复校验：关联产品在回收站中阻断并报错")
    void testValidateRestore_ProductInRecycle_Throws() {
        Long id = 100L;
        Long productId = 10L;
        CostCardDO card = CostCardDO.builder().id(id).productId(productId).build();
        when(costCardMapper.selectDeletedById(id, 1L)).thenReturn(card);

        ProductDO deletedProduct = new ProductDO();
        deletedProduct.setName("已删产品");
        when(productMapper.selectDeletedById(productId, 1L)).thenReturn(deletedProduct);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(id));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("物理删除：级联物理删除子表并删除主表")
    void testPurge_Success() {
        Long id = 100L;
        CostCardDO card = CostCardDO.builder().id(id).build();
        when(costCardMapper.selectDeletedById(id, 1L)).thenReturn(card);
        when(costCardMapper.selectActiveReferenceCount(id, 1L)).thenReturn(0L);
        when(costCardMapper.purgeCostCard(id, 1L)).thenReturn(1);

        handler.purge(id);

        verify(costCardFeeMapper).purgeByCardId(id, 1L);
        verify(costCardYarnMapper).purgeByCardId(id, 1L);
        verify(costCardMapper).purgeCostCard(id, 1L);
    }

    @Test
    @DisplayName("物理删除校验：存在活跃报价单引用阻断")
    void testValidatePurge_ActiveReference_Throws() {
        Long id = 100L;
        CostCardDO card = CostCardDO.builder().id(id).build();
        when(costCardMapper.selectDeletedById(id, 1L)).thenReturn(card);
        when(costCardMapper.selectActiveReferenceCount(id, 1L)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validatePurge(id));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());
    }

}
