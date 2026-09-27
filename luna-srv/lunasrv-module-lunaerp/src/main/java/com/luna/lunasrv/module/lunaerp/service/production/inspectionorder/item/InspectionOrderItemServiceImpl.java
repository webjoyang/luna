package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.item;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.item.vo.InspectionOrderItemSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 生产管理-检验单录检明细 Service 实现类
 *
 * 录检明细作为独立子资源处理（ADR-0014）：
 * - 包装模式分化校验（ADR-0025）：
 *   - ROLL 卷装：rollNo 必填、pieceNo 禁填，(vatNo, rollNo) 唯一（create 报 1_030_010_004，update 排除自身）
 *   - PIECE 匹装：pieceNo 必填、rollNo 可空可填且允许重复（一卷拆多匹），(vatNo, pieceNo) 唯一（create 报 1_030_010_004，update 排除自身），pieceCount 恒 1
 * - 第一卷/匹明细保存时便利地把主表 DRAFT→ACTIVE（若主表为 DRAFT，非 DRAFT 不动）
 * - 所属检验单不存在报 1_030_010_100
 *
 * @author luna
 */
@Service
public class InspectionOrderItemServiceImpl implements InspectionOrderItemService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";

    @Resource
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Resource
    private InspectionOrderService inspectionOrderService;

    @Resource
    private com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService weavingTicketService;

    @Resource
    private com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper weavingTicketMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInspectionOrderItem(InspectionOrderItemSaveReqVO createReqVO) {
        // 1. 校验所属检验单存在
        InspectionOrderRespVO order = validateInspectionOrderExists(createReqVO.getInspectionOrderId());

        // 2. 根据包装模式分化校验（ADR-0025）
        String packageMode = StringUtils.hasText(order.getPackageMode()) ? order.getPackageMode() : "ROLL";
        if ("PIECE".equals(packageMode)) {
            // PIECE 模式：pieceNo 必填，rollNo 可空可填且允许重复，(vatNo, pieceNo) 唯一
            if (!StringUtils.hasText(createReqVO.getPieceNo())) {
                throw exception(INSPECTION_ORDER_PIECE_NO_REQUIRED);
            }
            validatePieceNoUnique(createReqVO.getInspectionOrderId(), createReqVO.getVatNo(), createReqVO.getPieceNo(), null);
        } else {
            // ROLL 模式：rollNo 必填，pieceNo 禁填，(vatNo, rollNo) 唯一
            if (!StringUtils.hasText(createReqVO.getRollNo())) {
                throw exception(INSPECTION_ORDER_ROLL_NO_REQUIRED);
            }
            if (StringUtils.hasText(createReqVO.getPieceNo())) {
                throw exception(INSPECTION_ORDER_PIECE_NO_FORBIDDEN);
            }
            validateRollNoUnique(createReqVO.getInspectionOrderId(), createReqVO.getVatNo(), createReqVO.getRollNo(), null);
        }

        // 3. 第一卷/匹时便利把主表 DRAFT→ACTIVE
        Long count = inspectionOrderItemMapper.selectCountByInspectionOrderId(createReqVO.getInspectionOrderId());
        if (count == null || count == 0L) {
            if (STATUS_DRAFT.equals(order.getStatus())) {
                inspectionOrderService.updateInspectionOrderStatus(createReqVO.getInspectionOrderId(), STATUS_ACTIVE);
            }
        }

        // 4. 落库
        InspectionOrderItemDO item = BeanUtils.toBean(createReqVO, InspectionOrderItemDO.class);
        if ("PIECE".equals(packageMode)) {
            // PIECE 下 pieceCount 恒 1
            item.setPieceCount(1);
            // 布票血缘自动关联（若未显式传 weavingTicketId，按 pieceNo 匹配布票码）
            if (item.getWeavingTicketId() == null && StringUtils.hasText(item.getPieceNo()) && weavingTicketMapper != null) {
                com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO ticket =
                        weavingTicketMapper.selectByCode(item.getPieceNo().trim());
                if (ticket != null) {
                    item.setWeavingTicketId(ticket.getId());
                    if (!StringUtils.hasText(item.getMachineNo())) {
                        item.setMachineNo(StringUtils.hasText(ticket.getActualMachine()) ? ticket.getActualMachine() : ticket.getPlannedMachine());
                    }
                }
            }
        } else {
            if (item.getPieceCount() == null) {
                item.setPieceCount(1);
            }
        }
        inspectionOrderItemMapper.insert(item);

        // 5. 检验明细保存（有等级结果）后，后端按 weavingTicketId 将布票回写为 INSPECTED（ADR-0024、ADR-0025）
        if (item.getWeavingTicketId() != null && StringUtils.hasText(item.getGrade()) && weavingTicketService != null) {
            weavingTicketService.markInspected(item.getWeavingTicketId());
        }

        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInspectionOrderItem(InspectionOrderItemSaveReqVO updateReqVO) {
        // 1. 校验明细存在
        InspectionOrderItemDO exist = validateInspectionOrderItemExists(updateReqVO.getId());

        // 2. 校验所属检验单存在
        InspectionOrderRespVO order = validateInspectionOrderExists(updateReqVO.getInspectionOrderId());

        // 3. 根据包装模式分化校验（ADR-0025）
        String packageMode = StringUtils.hasText(order.getPackageMode()) ? order.getPackageMode() : "ROLL";
        if ("PIECE".equals(packageMode)) {
            // PIECE 模式：pieceNo 必填，(vatNo, pieceNo) 重复查重（排除自身）
            if (!StringUtils.hasText(updateReqVO.getPieceNo())) {
                throw exception(INSPECTION_ORDER_PIECE_NO_REQUIRED);
            }
            validatePieceNoUnique(updateReqVO.getInspectionOrderId(), updateReqVO.getVatNo(), updateReqVO.getPieceNo(), updateReqVO.getId());
        } else {
            // ROLL 模式：rollNo 必填，pieceNo 禁填，(vatNo, rollNo) 重复查重（排除自身）
            if (!StringUtils.hasText(updateReqVO.getRollNo())) {
                throw exception(INSPECTION_ORDER_ROLL_NO_REQUIRED);
            }
            if (StringUtils.hasText(updateReqVO.getPieceNo())) {
                throw exception(INSPECTION_ORDER_PIECE_NO_FORBIDDEN);
            }
            validateRollNoUnique(updateReqVO.getInspectionOrderId(), updateReqVO.getVatNo(), updateReqVO.getRollNo(), updateReqVO.getId());
        }

        // 4. 更新（保留所属检验单ID，避免篡改父单据）
        InspectionOrderItemDO updateObj = BeanUtils.toBean(updateReqVO, InspectionOrderItemDO.class);
        updateObj.setInspectionOrderId(exist.getInspectionOrderId());
        if ("PIECE".equals(packageMode)) {
            updateObj.setPieceCount(1);
            if (updateObj.getWeavingTicketId() == null && StringUtils.hasText(updateObj.getPieceNo()) && weavingTicketMapper != null) {
                com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO ticket =
                        weavingTicketMapper.selectByCode(updateObj.getPieceNo().trim());
                if (ticket != null) {
                    updateObj.setWeavingTicketId(ticket.getId());
                }
            }
        }
        inspectionOrderItemMapper.updateById(updateObj);

        // 5. 检验明细更新有等级结果时，回写布票为 INSPECTED（ADR-0024）
        Long ticketId = updateObj.getWeavingTicketId() != null ? updateObj.getWeavingTicketId() : exist.getWeavingTicketId();
        if (ticketId != null && StringUtils.hasText(updateObj.getGrade()) && weavingTicketService != null) {
            weavingTicketService.markInspected(ticketId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInspectionOrderItem(Long id) {
        // 校验存在
        InspectionOrderItemDO item = validateInspectionOrderItemExists(id);
        // 逻辑删除
        inspectionOrderItemMapper.deleteById(id);
        // 删除录检明细时回退布票状态为 COMPLETED 并同事务重算（ADR-0024）
        if (item.getWeavingTicketId() != null && weavingTicketService != null) {
            weavingTicketService.rollbackInspected(item.getWeavingTicketId());
        }
    }

    @Override
    public InspectionOrderItemRespVO getInspectionOrderItem(Long id) {
        InspectionOrderItemDO item = validateInspectionOrderItemExists(id);
        return BeanUtils.toBean(item, InspectionOrderItemRespVO.class);
    }

    @Override
    public PageResult<InspectionOrderItemRespVO> getInspectionOrderItemPage(InspectionOrderItemPageReqVO pageReqVO) {
        PageResult<InspectionOrderItemDO> pageResult = inspectionOrderItemMapper.selectPage(pageReqVO);
        return BeanUtils.toBean(pageResult, InspectionOrderItemRespVO.class);
    }

    // ==================== 私有方法 ====================

    private InspectionOrderItemDO validateInspectionOrderItemExists(Long id) {
        if (id == null) {
            throw exception(INSPECTION_ORDER_ITEM_NOT_EXISTS);
        }
        InspectionOrderItemDO item = inspectionOrderItemMapper.selectById(id);
        if (item == null) {
            throw exception(INSPECTION_ORDER_ITEM_NOT_EXISTS);
        }
        return item;
    }

    private InspectionOrderRespVO validateInspectionOrderExists(Long inspectionOrderId) {
        if (inspectionOrderId == null) {
            throw exception(INSPECTION_ORDER_ITEM_NOT_EXISTS);
        }
        InspectionOrderRespVO order;
        try {
            order = inspectionOrderService.getInspectionOrder(inspectionOrderId);
        } catch (ServiceException ex) {
            throw exception(INSPECTION_ORDER_ITEM_NOT_EXISTS);
        }
        if (order == null) {
            throw exception(INSPECTION_ORDER_ITEM_NOT_EXISTS);
        }
        return order;
    }

    private void validateRollNoUnique(Long inspectionOrderId, String vatNo, String rollNo, Long excludeId) {
        InspectionOrderItemDO exist = inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndRollNo(inspectionOrderId, vatNo, rollNo);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(INSPECTION_ORDER_ROLL_DUPLICATE, rollNo, vatNo);
        }
    }

    private void validatePieceNoUnique(Long inspectionOrderId, String vatNo, String pieceNo, Long excludeId) {
        InspectionOrderItemDO exist = inspectionOrderItemMapper.selectByInspectionOrderIdAndVatNoAndPieceNo(inspectionOrderId, vatNo, pieceNo);
        if (exist != null && !exist.getId().equals(excludeId)) {
            throw exception(INSPECTION_ORDER_PIECE_DUPLICATE, pieceNo, vatNo);
        }
    }

}
