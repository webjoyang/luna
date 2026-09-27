package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.item;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.INSPECTION_ORDER_ITEM_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.INSPECTION_ORDER_ROLL_DUPLICATE;
import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link InspectionOrderItemServiceImpl} 的纯 Mockito 单元测试。
 *
 * 覆盖核心业务行为（04 票）：
 *  1. create：
 *     - 所属检验单不存在报 1_030_010_100
 *     - 单内 (vatNo, rollNo) 重复报 1_030_010_004
 *     - 第一卷保存便利把主表 DRAFT→ACTIVE（verify 调主表 Service 更新 status 为 ACTIVE 且仅当原为 DRAFT）
 *     - 第一卷保存若原状态非 DRAFT（如 ACTIVE），不更新主表 status
 *     - 非第一卷保存（已有历史明细），不更新主表 status
 *     - 缺省 pieceCount 为 1
 *     - 成功创建返回 id
 *  2. update：
 *     - 明细不存在报 1_030_010_100
 *     - 所属检验单不存在报 1_030_010_100
 *     - 单内 (vatNo, rollNo) 重复（排除自身）报 1_030_010_004
 *     - 单内排除自身更新成功
 *  3. delete：
 *     - 明细不存在报 1_030_010_100
 *     - 正常删除
 *  4. get：
 *     - 明细不存在报 1_030_010_100
 *     - 正常获取返回 RespVO
 *  5. page：
 *     - 按 inspectionOrderId 过滤，返回分页结果
 *  6. VO 校验：
 *     - inspectionOrderId、rollNo、vatNo 必填
 *
 * @author luna
 */
class InspectionOrderItemServiceImplTest {

    @Mock
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Mock
    private InspectionOrderService inspectionOrderService;

    @Mock
    private com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService weavingTicketService;

    @Mock
    private com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper weavingTicketMapper;

    @InjectMocks
    private InspectionOrderItemServiceImpl inspectionOrderItemService;

    private static final Long ORDER_ID = 100L;
    private static final Long ITEM_ID = 200L;
    private static final String VAT_NO = "VAT-202609-01";
    private static final String ROLL_NO = "R-001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), InspectionOrderItemDO.class);

        when(inspectionOrderItemMapper.insert(any(InspectionOrderItemDO.class))).thenAnswer(invocation -> {
            InspectionOrderItemDO dobj = invocation.getArgument(0);
            dobj.setId(ITEM_ID);
            return 1;
        });
    }

    private static final String PIECE_NO = "P-001";

    private InspectionOrderRespVO buildOrder(Long id, String status) {
        return buildOrder(id, status, "ROLL");
    }

    private InspectionOrderRespVO buildOrder(Long id, String status, String packageMode) {
        InspectionOrderRespVO order = new InspectionOrderRespVO();
        order.setId(id);
        order.setStatus(status);
        order.setCode("INS-20260921-001");
        order.setPackageMode(packageMode);
        return order;
    }

    private InspectionOrderItemDO buildItem(Long id, Long orderId, String vatNo, String rollNo) {
        return buildItem(id, orderId, vatNo, rollNo, null);
    }

    private InspectionOrderItemDO buildItem(Long id, Long orderId, String vatNo, String rollNo, String pieceNo) {
        InspectionOrderItemDO item = new InspectionOrderItemDO();
        item.setId(id);
        item.setInspectionOrderId(orderId);
        item.setVatNo(vatNo);
        item.setRollNo(rollNo);
        item.setPieceNo(pieceNo);
        item.setMeasuredWeight(new BigDecimal("25.50"));
        item.setLengthM(new BigDecimal("100.00"));
        item.setGrade("A");
        item.setPieceCount(1);
        return item;
    }

    private InspectionOrderItemSaveReqVO buildSaveReqVO(Long id, Long orderId, String vatNo, String rollNo) {
        return buildSaveReqVO(id, orderId, vatNo, rollNo, null);
    }

    private InspectionOrderItemSaveReqVO buildSaveReqVO(Long id, Long orderId, String vatNo, String rollNo, String pieceNo) {
        InspectionOrderItemSaveReqVO reqVO = new InspectionOrderItemSaveReqVO();
        reqVO.setId(id);
        reqVO.setInspectionOrderId(orderId);
        reqVO.setVatNo(vatNo);
        reqVO.setRollNo(rollNo);
        reqVO.setPieceNo(pieceNo);
        reqVO.setMeasuredWeight(new BigDecimal("25.50"));
        reqVO.setLengthM(new BigDecimal("100.00"));
        reqVO.setGrade("A");
        reqVO.setPieceCount(1);
        return reqVO;
    }

    // ==================== createInspectionOrderItem ====================

    @Test
    void createInspectionOrderItem_inspectionOrderNotExists_throws1030010100() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.createInspectionOrderItem(reqVO));
        assertEquals(INSPECTION_ORDER_ITEM_NOT_EXISTS.getCode(), ex.getCode());
        verify(inspectionOrderItemMapper, never()).insert(any(InspectionOrderItemDO.class));
    }

    @Test
    void createInspectionOrderItem_inspectionOrderServiceThrows_throws1030010100() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID))
                .thenThrow(new ServiceException(1_030_010_000, "检验单不存在"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.createInspectionOrderItem(reqVO));
        assertEquals(INSPECTION_ORDER_ITEM_NOT_EXISTS.getCode(), ex.getCode());
        verify(inspectionOrderItemMapper, never()).insert(any(InspectionOrderItemDO.class));
    }

    @Test
    void createInspectionOrderItem_duplicateVatAndRollNo_throws1030010004() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "DRAFT"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(ORDER_ID, VAT_NO, ROLL_NO))
                .thenReturn(buildItem(999L, ORDER_ID, VAT_NO, ROLL_NO));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.createInspectionOrderItem(reqVO));
        assertEquals(INSPECTION_ORDER_ROLL_DUPLICATE.getCode(), ex.getCode());
        verify(inspectionOrderItemMapper, never()).insert(any(InspectionOrderItemDO.class));
        verify(inspectionOrderService, never()).updateInspectionOrderStatus(any(), any());
    }

    @Test
    void createInspectionOrderItem_firstRoll_draftStatus_transitionsToActive() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "DRAFT"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(ORDER_ID, VAT_NO, ROLL_NO))
                .thenReturn(null);
        when(inspectionOrderItemMapper.selectCountByInspectionOrderId(ORDER_ID)).thenReturn(0L);

        Long createdId = inspectionOrderItemService.createInspectionOrderItem(reqVO);

        assertEquals(ITEM_ID, createdId);
        // verify 调主表 Service 更新 status 为 ACTIVE 且仅当原为 DRAFT
        verify(inspectionOrderService).updateInspectionOrderStatus(ORDER_ID, "ACTIVE");

        ArgumentCaptor<InspectionOrderItemDO> captor = ArgumentCaptor.forClass(InspectionOrderItemDO.class);
        verify(inspectionOrderItemMapper).insert(captor.capture());
        assertEquals(ORDER_ID, captor.getValue().getInspectionOrderId());
        assertEquals(VAT_NO, captor.getValue().getVatNo());
        assertEquals(ROLL_NO, captor.getValue().getRollNo());
    }

    @Test
    void createInspectionOrderItem_firstRoll_nonDraftStatus_doesNotTransition() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(ORDER_ID, VAT_NO, ROLL_NO))
                .thenReturn(null);
        when(inspectionOrderItemMapper.selectCountByInspectionOrderId(ORDER_ID)).thenReturn(0L);

        Long createdId = inspectionOrderItemService.createInspectionOrderItem(reqVO);

        assertEquals(ITEM_ID, createdId);
        verify(inspectionOrderService, never()).updateInspectionOrderStatus(any(), any());
        verify(inspectionOrderItemMapper).insert(any(InspectionOrderItemDO.class));
    }

    @Test
    void createInspectionOrderItem_notFirstRoll_draftStatus_doesNotTransition() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "DRAFT"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(ORDER_ID, VAT_NO, ROLL_NO))
                .thenReturn(null);
        when(inspectionOrderItemMapper.selectCountByInspectionOrderId(ORDER_ID)).thenReturn(2L);

        Long createdId = inspectionOrderItemService.createInspectionOrderItem(reqVO);

        assertEquals(ITEM_ID, createdId);
        verify(inspectionOrderService, never()).updateInspectionOrderStatus(any(), any());
        verify(inspectionOrderItemMapper).insert(any(InspectionOrderItemDO.class));
    }

    @Test
    void createInspectionOrderItem_pieceCountNull_defaultsToOne() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO);
        reqVO.setPieceCount(null);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "DRAFT"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(ORDER_ID, VAT_NO, ROLL_NO))
                .thenReturn(null);
        when(inspectionOrderItemMapper.selectCountByInspectionOrderId(ORDER_ID)).thenReturn(1L);

        inspectionOrderItemService.createInspectionOrderItem(reqVO);

        ArgumentCaptor<InspectionOrderItemDO> captor = ArgumentCaptor.forClass(InspectionOrderItemDO.class);
        verify(inspectionOrderItemMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getPieceCount(), "匹数缺省应为 1");
    }

    @Test
    void createInspectionOrderItem_rollMode_missingRollNo_throws1030010002() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, "");
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "DRAFT", "ROLL"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.createInspectionOrderItem(reqVO));
        assertEquals(1_030_010_002, ex.getCode());
        assertTrue(ex.getMessage().contains("卷装模式下卷号不能为空"));
        verify(inspectionOrderItemMapper, never()).insert(any(InspectionOrderItemDO.class));
    }

    @Test
    void createInspectionOrderItem_rollMode_hasPieceNo_throws1030010002() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO, "P-01");
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "DRAFT", "ROLL"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.createInspectionOrderItem(reqVO));
        assertEquals(1_030_010_002, ex.getCode());
        assertTrue(ex.getMessage().contains("卷装模式下禁止填写匹号"));
        verify(inspectionOrderItemMapper, never()).insert(any(InspectionOrderItemDO.class));
    }

    @Test
    void createInspectionOrderItem_pieceMode_missingPieceNo_throws1030010002() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO, "");
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "DRAFT", "PIECE"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.createInspectionOrderItem(reqVO));
        assertEquals(1_030_010_002, ex.getCode());
        assertTrue(ex.getMessage().contains("匹装模式下匹号不能为空"));
        verify(inspectionOrderItemMapper, never()).insert(any(InspectionOrderItemDO.class));
    }

    @Test
    void createInspectionOrderItem_pieceMode_duplicateVatAndPieceNo_throws1030010004() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE", "PIECE"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndPieceNo(ORDER_ID, VAT_NO, PIECE_NO))
                .thenReturn(buildItem(999L, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.createInspectionOrderItem(reqVO));
        assertEquals(1_030_010_004, ex.getCode());
        assertTrue(ex.getMessage().contains("单内匹号【" + PIECE_NO + "】缸号【" + VAT_NO + "】已存在"));
        verify(inspectionOrderItemMapper, never()).insert(any(InspectionOrderItemDO.class));
    }

    @Test
    void createInspectionOrderItem_pieceMode_oneRollSplitMultiplePieces_success() {
        // 一卷拆多匹：相同 rollNo 但不同 pieceNo，允许保存且不报错
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, ROLL_NO, "P-002");
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE", "PIECE"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndPieceNo(ORDER_ID, VAT_NO, "P-002"))
                .thenReturn(null);
        when(inspectionOrderItemMapper.selectCountByInspectionOrderId(ORDER_ID)).thenReturn(1L);

        Long createdId = inspectionOrderItemService.createInspectionOrderItem(reqVO);

        assertEquals(ITEM_ID, createdId);
        ArgumentCaptor<InspectionOrderItemDO> captor = ArgumentCaptor.forClass(InspectionOrderItemDO.class);
        verify(inspectionOrderItemMapper).insert(captor.capture());
        assertEquals(ROLL_NO, captor.getValue().getRollNo());
        assertEquals("P-002", captor.getValue().getPieceNo());
        // 验证没有调用卷号查重（允许卷号重复）
        verify(inspectionOrderItemMapper, never()).selectByInspectionOrderIdAndVatNoAndRollNo(any(), any(), any());
    }

    @Test
    void createInspectionOrderItem_pieceMode_pieceCountAlwaysOne() {
        // PIECE 模式下无论输入 pieceCount 为几，落库恒为 1
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, null, PIECE_NO);
        reqVO.setPieceCount(8);
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "DRAFT", "PIECE"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndPieceNo(ORDER_ID, VAT_NO, PIECE_NO))
                .thenReturn(null);
        when(inspectionOrderItemMapper.selectCountByInspectionOrderId(ORDER_ID)).thenReturn(0L);

        inspectionOrderItemService.createInspectionOrderItem(reqVO);

        ArgumentCaptor<InspectionOrderItemDO> captor = ArgumentCaptor.forClass(InspectionOrderItemDO.class);
        verify(inspectionOrderItemMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getPieceCount(), "PIECE 模式下 pieceCount 恒为 1");
        assertNull(captor.getValue().getRollNo(), "PIECE 模式下 rollNo 允许为空");
    }

    // ==================== updateInspectionOrderItem ====================

    @Test
    void updateInspectionOrderItem_itemNotExists_throws1030010100() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.updateInspectionOrderItem(reqVO));
        assertEquals(INSPECTION_ORDER_ITEM_NOT_EXISTS.getCode(), ex.getCode());
        verify(inspectionOrderItemMapper, never()).updateById(any(InspectionOrderItemDO.class));
    }

    @Test
    void updateInspectionOrderItem_inspectionOrderNotExists_throws1030010100() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.updateInspectionOrderItem(reqVO));
        assertEquals(INSPECTION_ORDER_ITEM_NOT_EXISTS.getCode(), ex.getCode());
        verify(inspectionOrderItemMapper, never()).updateById(any(InspectionOrderItemDO.class));
    }

    @Test
    void updateInspectionOrderItem_duplicateVatAndRollNo_throws1030010004() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE"));
        // 发现另一条具有相同 (vatNo, rollNo) 的明细（id=888 != ITEM_ID）
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(ORDER_ID, VAT_NO, ROLL_NO))
                .thenReturn(buildItem(888L, ORDER_ID, VAT_NO, ROLL_NO));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.updateInspectionOrderItem(reqVO));
        assertEquals(INSPECTION_ORDER_ROLL_DUPLICATE.getCode(), ex.getCode());
        verify(inspectionOrderItemMapper, never()).updateById(any(InspectionOrderItemDO.class));
    }

    @Test
    void updateInspectionOrderItem_duplicateExcludesSelf_success() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE"));
        // 相同组合查出来是自己（id=ITEM_ID）
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(ORDER_ID, VAT_NO, ROLL_NO))
                .thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO));

        inspectionOrderItemService.updateInspectionOrderItem(reqVO);

        ArgumentCaptor<InspectionOrderItemDO> captor = ArgumentCaptor.forClass(InspectionOrderItemDO.class);
        verify(inspectionOrderItemMapper).updateById(captor.capture());
        assertEquals(ITEM_ID, captor.getValue().getId());
        assertEquals(VAT_NO, captor.getValue().getVatNo());
        assertEquals(ROLL_NO, captor.getValue().getRollNo());
    }

    @Test
    void updateInspectionOrderItem_rollMode_missingRollNo_throws1030010002() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, "");
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE", "ROLL"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.updateInspectionOrderItem(reqVO));
        assertEquals(1_030_010_002, ex.getCode());
        assertTrue(ex.getMessage().contains("卷装模式下卷号不能为空"));
        verify(inspectionOrderItemMapper, never()).updateById(any(InspectionOrderItemDO.class));
    }

    @Test
    void updateInspectionOrderItem_rollMode_hasPieceNo_throws1030010002() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO, "P-01");
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE", "ROLL"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.updateInspectionOrderItem(reqVO));
        assertEquals(1_030_010_002, ex.getCode());
        assertTrue(ex.getMessage().contains("卷装模式下禁止填写匹号"));
        verify(inspectionOrderItemMapper, never()).updateById(any(InspectionOrderItemDO.class));
    }

    @Test
    void updateInspectionOrderItem_pieceMode_missingPieceNo_throws1030010002() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO, "");
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE", "PIECE"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.updateInspectionOrderItem(reqVO));
        assertEquals(1_030_010_002, ex.getCode());
        assertTrue(ex.getMessage().contains("匹装模式下匹号不能为空"));
        verify(inspectionOrderItemMapper, never()).updateById(any(InspectionOrderItemDO.class));
    }

    @Test
    void updateInspectionOrderItem_pieceMode_duplicateVatAndPieceNo_throws1030010004() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE", "PIECE"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndPieceNo(ORDER_ID, VAT_NO, PIECE_NO))
                .thenReturn(buildItem(888L, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.updateInspectionOrderItem(reqVO));
        assertEquals(1_030_010_004, ex.getCode());
        assertTrue(ex.getMessage().contains("单内匹号【" + PIECE_NO + "】缸号【" + VAT_NO + "】已存在"));
        verify(inspectionOrderItemMapper, never()).updateById(any(InspectionOrderItemDO.class));
    }

    @Test
    void updateInspectionOrderItem_pieceMode_duplicateExcludesSelf_success() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE", "PIECE"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndPieceNo(ORDER_ID, VAT_NO, PIECE_NO))
                .thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO, PIECE_NO));

        inspectionOrderItemService.updateInspectionOrderItem(reqVO);

        ArgumentCaptor<InspectionOrderItemDO> captor = ArgumentCaptor.forClass(InspectionOrderItemDO.class);
        verify(inspectionOrderItemMapper).updateById(captor.capture());
        assertEquals(ITEM_ID, captor.getValue().getId());
        assertEquals(1, captor.getValue().getPieceCount(), "PIECE 模式下更新时 pieceCount 恒为 1");
    }

    @Test
    void updateInspectionOrderItem_pieceMode_pieceCountAlwaysOne() {
        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, null, PIECE_NO);
        reqVO.setPieceCount(12);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, null, PIECE_NO));
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(buildOrder(ORDER_ID, "ACTIVE", "PIECE"));
        when(inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndPieceNo(ORDER_ID, VAT_NO, PIECE_NO))
                .thenReturn(null);

        inspectionOrderItemService.updateInspectionOrderItem(reqVO);

        ArgumentCaptor<InspectionOrderItemDO> captor = ArgumentCaptor.forClass(InspectionOrderItemDO.class);
        verify(inspectionOrderItemMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getPieceCount(), "PIECE 模式下更新时 pieceCount 恒为 1");
    }

    // ==================== deleteInspectionOrderItem ====================

    @Test
    void deleteInspectionOrderItem_itemNotExists_throws1030010100() {
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.deleteInspectionOrderItem(ITEM_ID));
        assertEquals(INSPECTION_ORDER_ITEM_NOT_EXISTS.getCode(), ex.getCode());
        verify(inspectionOrderItemMapper, never()).deleteById(any());
    }

    @Test
    void deleteInspectionOrderItem_success() {
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO));

        inspectionOrderItemService.deleteInspectionOrderItem(ITEM_ID);

        verify(inspectionOrderItemMapper).deleteById(ITEM_ID);
    }

    // ==================== getInspectionOrderItem ====================

    @Test
    void getInspectionOrderItem_itemNotExists_throws1030010100() {
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> inspectionOrderItemService.getInspectionOrderItem(ITEM_ID));
        assertEquals(INSPECTION_ORDER_ITEM_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void getInspectionOrderItem_success() {
        InspectionOrderItemDO item = buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(item);

        InspectionOrderItemRespVO respVO = inspectionOrderItemService.getInspectionOrderItem(ITEM_ID);

        assertNotNull(respVO);
        assertEquals(ITEM_ID, respVO.getId());
        assertEquals(ORDER_ID, respVO.getInspectionOrderId());
        assertEquals(VAT_NO, respVO.getVatNo());
        assertEquals(ROLL_NO, respVO.getRollNo());
        assertEquals("A", respVO.getGrade());
    }

    // ==================== getInspectionOrderItemPage ====================

    @Test
    void getInspectionOrderItemPage_filteredByOrderId_returnsMappedResult() {
        InspectionOrderItemPageReqVO pageReqVO = new InspectionOrderItemPageReqVO();
        pageReqVO.setInspectionOrderId(ORDER_ID);

        InspectionOrderItemDO item = buildItem(ITEM_ID, ORDER_ID, VAT_NO, ROLL_NO);
        PageResult<InspectionOrderItemDO> pageResult = new PageResult<>(Collections.singletonList(item), 1L);
        when(inspectionOrderItemMapper.selectPage(pageReqVO)).thenReturn(pageResult);

        PageResult<InspectionOrderItemRespVO> result = inspectionOrderItemService.getInspectionOrderItemPage(pageReqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals(ROLL_NO, result.getList().get(0).getRollNo());
        verify(inspectionOrderItemMapper).selectPage(pageReqVO);
    }

    // ==================== 布票血缘与回写（ADR-0024、ADR-0025） ====================

    @Test
    void createInspectionOrderItem_pieceMode_autoMatchesTicket_andMarksInspected() {
        InspectionOrderRespVO order = buildOrder(ORDER_ID, "ACTIVE", "PIECE");
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(order);
        when(inspectionOrderItemMapper.selectCountByInspectionOrderId(ORDER_ID)).thenReturn(1L);

        com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO ticket =
                new com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO();
        ticket.setId(77L);
        ticket.setCode("BT-001");
        ticket.setActualMachine("LM-101");
        when(weavingTicketMapper.selectByCode("BT-001")).thenReturn(ticket);

        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(null, ORDER_ID, VAT_NO, null, "BT-001");
        reqVO.setGrade("A");

        Long id = inspectionOrderItemService.createInspectionOrderItem(reqVO);
        assertNotNull(id);

        ArgumentCaptor<InspectionOrderItemDO> captor = ArgumentCaptor.forClass(InspectionOrderItemDO.class);
        verify(inspectionOrderItemMapper).insert(captor.capture());
        InspectionOrderItemDO inserted = captor.getValue();
        assertEquals(77L, inserted.getWeavingTicketId());
        assertEquals("LM-101", inserted.getMachineNo());

        verify(weavingTicketService).markInspected(77L);
    }

    @Test
    void updateInspectionOrderItem_withGrade_marksInspected() {
        InspectionOrderRespVO order = buildOrder(ORDER_ID, "ACTIVE", "PIECE");
        when(inspectionOrderService.getInspectionOrder(ORDER_ID)).thenReturn(order);

        InspectionOrderItemDO existing = buildItem(ITEM_ID, ORDER_ID, VAT_NO, null, "BT-001");
        existing.setWeavingTicketId(77L);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(existing);

        InspectionOrderItemSaveReqVO reqVO = buildSaveReqVO(ITEM_ID, ORDER_ID, VAT_NO, null, "BT-001");
        reqVO.setGrade("B");

        inspectionOrderItemService.updateInspectionOrderItem(reqVO);

        verify(weavingTicketService).markInspected(77L);
    }

    @Test
    void deleteInspectionOrderItem_withTicket_rollsBackInspected() {
        InspectionOrderItemDO existing = buildItem(ITEM_ID, ORDER_ID, VAT_NO, null, "BT-001");
        existing.setWeavingTicketId(77L);
        when(inspectionOrderItemMapper.selectById(ITEM_ID)).thenReturn(existing);

        inspectionOrderItemService.deleteInspectionOrderItem(ITEM_ID);

        verify(inspectionOrderItemMapper).deleteById(ITEM_ID);
        verify(weavingTicketService).rollbackInspected(77L);
    }

    // ==================== VO 校验 ====================

    @Nested
    class ReqVOValidationTest {

        private Validator validator;

        @BeforeEach
        void setUpValidator() {
            validator = buildDefaultValidatorFactory().getValidator();
        }

        @Test
        void saveReqVO_missingInspectionOrderId_failsValidation() {
            InspectionOrderItemSaveReqVO vo = buildSaveReqVO(null, null, VAT_NO, ROLL_NO);
            Set<ConstraintViolation<InspectionOrderItemSaveReqVO>> violations = validator.validate(vo);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("inspectionOrderId")));
        }

        @Test
        void saveReqVO_missingVatNo_failsValidation() {
            InspectionOrderItemSaveReqVO vo = buildSaveReqVO(null, ORDER_ID, "", ROLL_NO);
            Set<ConstraintViolation<InspectionOrderItemSaveReqVO>> violations = validator.validate(vo);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("vatNo")));
        }

        @Test
        void pageReqVO_missingInspectionOrderId_failsValidation() {
            InspectionOrderItemPageReqVO vo = new InspectionOrderItemPageReqVO();
            vo.setInspectionOrderId(null);
            Set<ConstraintViolation<InspectionOrderItemPageReqVO>> violations = validator.validate(vo);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("inspectionOrderId")));
        }

    }

}
