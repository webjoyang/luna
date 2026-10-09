package com.luna.lunasrv.module.lunaerp.service.baseinfo.product;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductImageMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.producttag.ProductTagRelMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ProductRecycleBinHandler} 单元测试
 *
 * 覆盖产品回收站关键业务语义：
 *  1. 回收站分页查询带 tenant_id 隔离
 *  2. 恢复校验（存在且 deleted=1 通过，不存在抛 1_030_031_001）
 *  3. 恢复成功并级联复活图片与标签关联（父->子批量 deleted=0）
 *  4. 并发条件更新防重入：影响行数 0 即报错（抛 1_030_031_001）
 *  5. 横向越权防范（他租户 bizId 查不到或更新为 0 报错）
 *  6. 彻底删除引用守卫（存在有效引用抛 1_030_031_003 阻断）
 *  7. 彻底删除成功并级联物理删除图片与标签关联子表
 *
 * @author luna
 */
public class ProductRecycleBinHandlerTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private ProductTagRelMapper productTagRelMapper;

    @InjectMocks
    private ProductRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long TENANT_ID_2 = 2L;
    private static final Long PRODUCT_ID = 3001L;

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
    @DisplayName("getBizType 返回 PRODUCT")
    void testGetBizType() {
        assertEquals("PRODUCT", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询已软删产品：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("PRODUCT");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("PRODUCT");
            item.setBizId(PRODUCT_ID);
            item.setDisplayName("30S NR罗马布");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(productMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("30S NR罗马布", result.getList().get(0).getDisplayName());
        verify(productMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：记录存在且已删除通过")
    void testValidateRestore_success() {
        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("30S NR罗马布");

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(product);

        assertDoesNotThrow(() -> handler.validateRestore(PRODUCT_ID));
        verify(productMapper).selectDeletedById(PRODUCT_ID, TENANT_ID_1);
    }

    @Test
    @DisplayName("恢复校验：记录不存在或未删除抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(PRODUCT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：ID 为空抛 1_030_031_001")
    void testValidateRestore_nullId_throws1030031001() {
        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(null));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复产品：主数据恢复为 INACTIVE 且级联复活图片与标签关联")
    void testRestore_success_withCascade() {
        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("30S NR罗马布");

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(product);
        when(productMapper.restoreProduct(eq(PRODUCT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(productImageMapper.restoreImagesByProductId(eq(String.valueOf(PRODUCT_ID)), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(2);
        when(productTagRelMapper.restoreTagRelsByProductId(eq(String.valueOf(PRODUCT_ID)), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(PRODUCT_ID));

        verify(productMapper).restoreProduct(eq(PRODUCT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(productImageMapper).restoreImagesByProductId(eq(String.valueOf(PRODUCT_ID)), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(productTagRelMapper).restoreTagRelsByProductId(eq(String.valueOf(PRODUCT_ID)), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("恢复产品：并发条件更新影响行数 0 报错（防重入与并发冲突）")
    void testRestore_concurrentUpdate_affectedRowsZero_throws1030031001() {
        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("30S NR罗马布");

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(product);
        when(productMapper.restoreProduct(eq(PRODUCT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(PRODUCT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(productImageMapper, never()).restoreImagesByProductId(any(), any(), any(), any());
        verify(productTagRelMapper, never()).restoreTagRelsByProductId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("横向越权防御：租户 A 尝试恢复租户 B 的产品档案，查不到报错")
    void testRestore_crossTenant_throws1030031001() {
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(PRODUCT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(productMapper, never()).restoreProduct(any(), any(), any(), any());
        verify(productImageMapper, never()).restoreImagesByProductId(any(), any(), any(), any());
        verify(productTagRelMapper, never()).restoreTagRelsByProductId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("彻底删除引用守卫：存在活跃引用（deleted=0）时阻断并报 1_030_031_003")
    void testPurge_hasActiveReference_throws1030031003() {
        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("30S NR罗马布");

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(product);
        when(productMapper.selectActiveReferenceCount(PRODUCT_ID, TENANT_ID_1)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(PRODUCT_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(productMapper, never()).purgeProduct(any(), any());
        verify(productImageMapper, never()).purgeImagesByProductId(any(), any());
        verify(productTagRelMapper, never()).purgeTagRelsByProductId(any(), any());
    }

    @Test
    @DisplayName("彻底删除成功：无有效引用时物理删除产品并级联物理删除图片与标签关联")
    void testPurge_success_withCascade() {
        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("30S NR罗马布");

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(product);
        when(productMapper.selectActiveReferenceCount(PRODUCT_ID, TENANT_ID_1)).thenReturn(0L);
        when(productMapper.purgeProduct(PRODUCT_ID, TENANT_ID_1)).thenReturn(1);
        when(productImageMapper.purgeImagesByProductId(String.valueOf(PRODUCT_ID), TENANT_ID_1)).thenReturn(2);
        when(productTagRelMapper.purgeTagRelsByProductId(String.valueOf(PRODUCT_ID), TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(PRODUCT_ID));

        verify(productMapper).purgeProduct(PRODUCT_ID, TENANT_ID_1);
        verify(productImageMapper).purgeImagesByProductId(String.valueOf(PRODUCT_ID), TENANT_ID_1);
        verify(productTagRelMapper).purgeTagRelsByProductId(String.valueOf(PRODUCT_ID), TENANT_ID_1);
    }

    @Test
    @DisplayName("彻底删除并发或未找到：影响行数 0 报错")
    void testPurge_concurrentOrMissing_affectedRowsZero_throws1030031001() {
        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("30S NR罗马布");

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(product);
        when(productMapper.selectActiveReferenceCount(PRODUCT_ID, TENANT_ID_1)).thenReturn(0L);
        when(productMapper.purgeProduct(PRODUCT_ID, TENANT_ID_1)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(PRODUCT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(productImageMapper, never()).purgeImagesByProductId(any(), any());
        verify(productTagRelMapper, never()).purgeTagRelsByProductId(any(), any());
    }

    @Test
    @DisplayName("横向越权防御：租户 A 尝试彻底删除租户 B 的产品档案，查不到报错")
    void testPurge_crossTenant_throws1030031001() {
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(PRODUCT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(productMapper, never()).purgeProduct(any(), any());
        verify(productImageMapper, never()).purgeImagesByProductId(any(), any());
        verify(productTagRelMapper, never()).purgeTagRelsByProductId(any(), any());
    }

}
