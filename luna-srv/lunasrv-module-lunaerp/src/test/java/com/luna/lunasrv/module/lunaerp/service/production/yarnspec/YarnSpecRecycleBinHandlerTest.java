package com.luna.lunasrv.module.lunaerp.service.production.yarnspec;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link YarnSpecRecycleBinHandler} 单元测试
 *
 * 覆盖纱线规格回收站关键业务语义：
 *  1. 回收站分页查询带 tenant_id 隔离
 *  2. 恢复校验（存在且 deleted=1，且父级材质未软删通过）
 *  3. 父子依赖阻断（父级材质仍在回收站阻断并报 1_030_031_002）
 *  4. 恢复成功（主数据恢复为 INACTIVE）
 *  5. 并发条件更新防重入：影响行数 0 即报错（抛 1_030_031_001）
 *  6. 横向越权防范（他租户 bizId 查不到或更新为 0 报错）
 *  7. 彻底删除引用守卫（存在有效引用抛 1_030_031_003 阻断）
 *  8. 彻底删除成功
 *
 * @author luna
 */
public class YarnSpecRecycleBinHandlerTest {

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @InjectMocks
    private YarnSpecRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long TENANT_ID_2 = 2L;
    private static final Long YARN_SPEC_ID = 5001L;
    private static final Long YARN_TYPE_ID = 4001L;

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
    @DisplayName("getBizType 返回 YARN_SPEC")
    void testGetBizType() {
        assertEquals("YARN_SPEC", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询已软删纱线规格：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("YARN_SPEC");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("YARN_SPEC");
            item.setBizId(YARN_SPEC_ID);
            item.setDisplayName("32S 棉");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(yarnSpecMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("32S 棉", result.getList().get(0).getDisplayName());
        verify(yarnSpecMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：记录存在且已删除，且材质正常未删除通过")
    void testValidateRestore_success() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setYarnTypeId(YARN_TYPE_ID);
        yarnSpec.setSpecName("32S 棉");

        YarnTypeDO activeType = new YarnTypeDO();
        activeType.setId(YARN_TYPE_ID);
        activeType.setName("棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        when(yarnTypeMapper.selectDeletedById(YARN_TYPE_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnTypeMapper.selectById(YARN_TYPE_ID)).thenReturn(activeType);

        assertDoesNotThrow(() -> handler.validateRestore(YARN_SPEC_ID));
        verify(yarnSpecMapper).selectDeletedById(YARN_SPEC_ID, TENANT_ID_1);
        verify(yarnTypeMapper).selectDeletedById(YARN_TYPE_ID, TENANT_ID_1);
    }

    @Test
    @DisplayName("恢复校验：记录不存在或未删除抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：ID 为空抛 1_030_031_001")
    void testValidateRestore_nullId_throws1030031001() {
        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(null));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验父子依赖阻断：所属纱线材质在回收站中，抛 1_030_031_002 并提示先恢复材质")
    void testValidateRestore_parentInRecycle_throws1030031002() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setYarnTypeId(YARN_TYPE_ID);
        yarnSpec.setSpecName("32S 棉");

        YarnTypeDO deletedType = new YarnTypeDO();
        deletedType.setId(YARN_TYPE_ID);
        deletedType.setName("棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        // 模拟父级材质在回收站中
        when(yarnTypeMapper.selectDeletedById(YARN_TYPE_ID, TENANT_ID_1)).thenReturn(deletedType);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("棉"));
    }

    @Test
    @DisplayName("恢复校验父子依赖阻断：所属纱线材质根本不存在，抛 1_030_031_002")
    void testValidateRestore_parentNotExists_throws1030031002() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setYarnTypeId(YARN_TYPE_ID);
        yarnSpec.setSpecName("32S 棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        when(yarnTypeMapper.selectDeletedById(YARN_TYPE_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnTypeMapper.selectById(YARN_TYPE_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复纱线规格：材质正常时恢复成功（主数据恢复为 INACTIVE）")
    void testRestore_success() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setYarnTypeId(YARN_TYPE_ID);
        yarnSpec.setSpecName("32S 棉");

        YarnTypeDO activeType = new YarnTypeDO();
        activeType.setId(YARN_TYPE_ID);
        activeType.setName("棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        when(yarnTypeMapper.selectDeletedById(YARN_TYPE_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnTypeMapper.selectById(YARN_TYPE_ID)).thenReturn(activeType);
        when(yarnSpecMapper.restoreYarnSpec(eq(YARN_SPEC_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);

        assertDoesNotThrow(() -> handler.restore(YARN_SPEC_ID));

        verify(yarnSpecMapper).restoreYarnSpec(eq(YARN_SPEC_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("恢复纱线规格阻断：材质在回收站中时禁止恢复，报错且不执行更新")
    void testRestore_parentInRecycle_throws1030031002() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setYarnTypeId(YARN_TYPE_ID);
        yarnSpec.setSpecName("32S 棉");

        YarnTypeDO deletedType = new YarnTypeDO();
        deletedType.setId(YARN_TYPE_ID);
        deletedType.setName("棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        when(yarnTypeMapper.selectDeletedById(YARN_TYPE_ID, TENANT_ID_1)).thenReturn(deletedType);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());

        verify(yarnSpecMapper, never()).restoreYarnSpec(any(), any(), any(), any());
    }

    @Test
    @DisplayName("恢复纱线规格：并发条件更新影响行数 0 报错")
    void testRestore_concurrentUpdate_affectedRowsZero_throws1030031001() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setYarnTypeId(YARN_TYPE_ID);
        yarnSpec.setSpecName("32S 棉");

        YarnTypeDO activeType = new YarnTypeDO();
        activeType.setId(YARN_TYPE_ID);
        activeType.setName("棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        when(yarnTypeMapper.selectDeletedById(YARN_TYPE_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnTypeMapper.selectById(YARN_TYPE_ID)).thenReturn(activeType);
        when(yarnSpecMapper.restoreYarnSpec(eq(YARN_SPEC_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("横向越权防御：租户 A 尝试恢复租户 B 的纱线规格，查不到报错")
    void testRestore_crossTenant_throws1030031001() {
        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(yarnSpecMapper, never()).restoreYarnSpec(any(), any(), any(), any());
    }

    @Test
    @DisplayName("彻底删除引用守卫：存在活跃引用（deleted=0）时阻断并报 1_030_031_003")
    void testPurge_hasActiveReference_throws1030031003() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setSpecName("32S 棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        when(yarnSpecMapper.selectActiveReferenceCount(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(yarnSpecMapper, never()).purgeYarnSpec(any(), any());
    }

    @Test
    @DisplayName("彻底删除成功：无有效引用时物理删除纱线规格")
    void testPurge_success() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setSpecName("32S 棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        when(yarnSpecMapper.selectActiveReferenceCount(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(0L);
        when(yarnSpecMapper.purgeYarnSpec(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(YARN_SPEC_ID));

        verify(yarnSpecMapper).purgeYarnSpec(YARN_SPEC_ID, TENANT_ID_1);
    }

    @Test
    @DisplayName("彻底删除并发或未找到：影响行数 0 报错")
    void testPurge_concurrentOrMissing_affectedRowsZero_throws1030031001() {
        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(YARN_SPEC_ID);
        yarnSpec.setSpecName("32S 棉");

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(yarnSpec);
        when(yarnSpecMapper.selectActiveReferenceCount(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(0L);
        when(yarnSpecMapper.purgeYarnSpec(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("横向越权防御：租户 A 尝试彻底删除租户 B 的纱线规格，查不到报错")
    void testPurge_crossTenant_throws1030031001() {
        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(YARN_SPEC_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());

        verify(yarnSpecMapper, never()).purgeYarnSpec(any(), any());
    }

}
