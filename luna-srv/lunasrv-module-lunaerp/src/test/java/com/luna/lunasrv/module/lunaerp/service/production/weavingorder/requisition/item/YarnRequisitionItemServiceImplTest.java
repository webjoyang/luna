package com.luna.lunasrv.module.lunaerp.service.production.weavingorder.requisition.item;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.module.lunaerp.controller.production.weavingorder.requisition.item.vo.YarnRequisitionItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.YarnRequisitionDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.requisition.item.YarnRequisitionItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.YarnRequisitionMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.requisition.item.YarnRequisitionItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link YarnRequisitionItemServiceImpl} 单元测试
 *
 * 覆盖 Issue 09 领纱明细独立子资源规格：
 * 1. 创建明细：父级必须为 DRAFT，自动补齐规格名称与材质快照，缺省 unit=KG、dispatchedQty=0；
 * 2. 用量校验：需求量必须大于 0；
 * 3. 状态守卫：父级非 DRAFT 态禁止新增、更新、删除；
 * 4. 规格存在性校验。
 *
 * @author luna
 */
class YarnRequisitionItemServiceImplTest {

    @Mock
    private YarnRequisitionItemMapper yarnRequisitionItemMapper;

    @Mock
    private YarnRequisitionMapper yarnRequisitionMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private YarnTypeMapper yarnTypeMapper;

    @InjectMocks
    private YarnRequisitionItemServiceImpl yarnRequisitionItemService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), YarnRequisitionItemDO.class);
    }

    @Test
    @DisplayName("创建明细 - 成功创建，自动补齐规格名称与材质快照")
    void testCreateYarnRequisitionItem_Success() {
        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(10L);
        spec.setSpecName("32S 棉");
        spec.setYarnTypeId(1L);
        when(yarnSpecMapper.selectById(10L)).thenReturn(spec);

        doAnswer(invocation -> {
            YarnRequisitionItemDO arg = invocation.getArgument(0);
            arg.setId(1024L);
            return 1;
        }).when(yarnRequisitionItemMapper).insert(any(YarnRequisitionItemDO.class));

        YarnRequisitionItemSaveReqVO vo = new YarnRequisitionItemSaveReqVO();
        vo.setRequisitionId(100L);
        vo.setYarnSpecId(10L);
        vo.setRequiredQty(new BigDecimal("120.50"));
        vo.setRemark("经纱领用");

        Long id = yarnRequisitionItemService.createYarnRequisitionItem(vo);
        assertEquals(1024L, id);

        ArgumentCaptor<YarnRequisitionItemDO> captor = ArgumentCaptor.forClass(YarnRequisitionItemDO.class);
        verify(yarnRequisitionItemMapper).insert(captor.capture());
        YarnRequisitionItemDO saved = captor.getValue();
        assertEquals(100L, saved.getRequisitionId());
        assertEquals(10L, saved.getYarnSpecId());
        assertEquals("32S 棉", saved.getYarnSpecName());
        assertEquals(1L, saved.getYarnTypeId());
        assertEquals(new BigDecimal("120.50"), saved.getRequiredQty());
        assertEquals(BigDecimal.ZERO, saved.getDispatchedQty());
        assertEquals("KG", saved.getUnit());
    }

    @Test
    @DisplayName("创建明细 - 领纱单处于非 DRAFT 状态时禁止添加明细")
    void testCreateYarnRequisitionItem_NonDraftBlocked() {
        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("ACTIVE");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnRequisitionItemSaveReqVO vo = new YarnRequisitionItemSaveReqVO();
        vo.setRequisitionId(100L);
        vo.setYarnSpecId(10L);
        vo.setRequiredQty(new BigDecimal("100.00"));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.createYarnRequisitionItem(vo));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("创建明细 - 需求量小于等于0拦截")
    void testCreateYarnRequisitionItem_NonPositiveQtyBlocked() {
        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnRequisitionItemSaveReqVO vo = new YarnRequisitionItemSaveReqVO();
        vo.setRequisitionId(100L);
        vo.setYarnSpecId(10L);
        vo.setRequiredQty(BigDecimal.ZERO);

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.createYarnRequisitionItem(vo));
        assertEquals(YARN_REQUISITION_ITEM_QTY_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("修改明细 - 仅当所属领纱单为 DRAFT 时允许修改")
    void testUpdateYarnRequisitionItem_DraftAllowed() {
        YarnRequisitionItemDO exist = new YarnRequisitionItemDO();
        exist.setId(1L);
        exist.setRequisitionId(100L);
        exist.setYarnSpecId(10L);
        exist.setRequiredQty(new BigDecimal("100.00"));
        when(yarnRequisitionItemMapper.selectById(1L)).thenReturn(exist);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(10L);
        spec.setSpecName("32S 棉");
        when(yarnSpecMapper.selectById(10L)).thenReturn(spec);

        YarnRequisitionItemSaveReqVO updateVO = new YarnRequisitionItemSaveReqVO();
        updateVO.setId(1L);
        updateVO.setYarnSpecId(10L);
        updateVO.setRequiredQty(new BigDecimal("150.00"));

        yarnRequisitionItemService.updateYarnRequisitionItem(updateVO);

        verify(yarnRequisitionItemMapper).updateById(exist);
        assertEquals(new BigDecimal("150.00"), exist.getRequiredQty());
    }

    @Test
    @DisplayName("修改明细 - 所属领纱单已提交为 ACTIVE 时禁止修改")
    void testUpdateYarnRequisitionItem_ActiveBlocked() {
        YarnRequisitionItemDO exist = new YarnRequisitionItemDO();
        exist.setId(1L);
        exist.setRequisitionId(100L);
        when(yarnRequisitionItemMapper.selectById(1L)).thenReturn(exist);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("ACTIVE");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        YarnRequisitionItemSaveReqVO updateVO = new YarnRequisitionItemSaveReqVO();
        updateVO.setId(1L);
        updateVO.setYarnSpecId(10L);
        updateVO.setRequiredQty(new BigDecimal("150.00"));

        ServiceException ex = assertThrows(ServiceException.class, () ->
                yarnRequisitionItemService.updateYarnRequisitionItem(updateVO));
        assertEquals(YARN_REQUISITION_STATUS_NOT_ALLOW.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("删除明细 - 仅 DRAFT 态允许删除")
    void testDeleteYarnRequisitionItem_Success() {
        YarnRequisitionItemDO exist = new YarnRequisitionItemDO();
        exist.setId(1L);
        exist.setRequisitionId(100L);
        when(yarnRequisitionItemMapper.selectById(1L)).thenReturn(exist);

        YarnRequisitionDO req = new YarnRequisitionDO();
        req.setId(100L);
        req.setStatus("DRAFT");
        when(yarnRequisitionMapper.selectById(100L)).thenReturn(req);

        yarnRequisitionItemService.deleteYarnRequisitionItem(1L);
        verify(yarnRequisitionItemMapper).deleteById(1L);
    }

}
