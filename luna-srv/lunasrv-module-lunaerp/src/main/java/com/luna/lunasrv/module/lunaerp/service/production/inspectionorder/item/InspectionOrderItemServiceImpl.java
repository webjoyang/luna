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

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.INSPECTION_ORDER_ITEM_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.INSPECTION_ORDER_ROLL_DUPLICATE;

/**
 * 生产管理-检验单录检明细 Service 实现类
 *
 * 录检明细作为独立子资源处理（ADR-0014）：
 * - 单内 (vatNo, rollNo) 唯一性由 service 层校验（create 重复报 1_030_010_004、update 排除自身）
 * - 第一卷明细保存时便利地把主表 DRAFT→ACTIVE（若主表为 DRAFT，非 DRAFT 不动）
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInspectionOrderItem(InspectionOrderItemSaveReqVO createReqVO) {
        // 1. 校验所属检验单存在
        InspectionOrderRespVO order = validateInspectionOrderExists(createReqVO.getInspectionOrderId());

        // 2. 单内 (vatNo, rollNo) 重复查重
        validateRollNoUnique(createReqVO.getInspectionOrderId(), createReqVO.getVatNo(), createReqVO.getRollNo(), null);

        // 3. 第一卷时便利把主表 DRAFT→ACTIVE
        Long count = inspectionOrderItemMapper.selectCountByInspectionOrderId(createReqVO.getInspectionOrderId());
        if (count == null || count == 0L) {
            if (STATUS_DRAFT.equals(order.getStatus())) {
                inspectionOrderService.updateInspectionOrderStatus(createReqVO.getInspectionOrderId(), STATUS_ACTIVE);
            }
        }

        // 4. 落库
        InspectionOrderItemDO item = BeanUtils.toBean(createReqVO, InspectionOrderItemDO.class);
        if (item.getPieceCount() == null) {
            item.setPieceCount(1);
        }
        inspectionOrderItemMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInspectionOrderItem(InspectionOrderItemSaveReqVO updateReqVO) {
        // 1. 校验明细存在
        InspectionOrderItemDO exist = validateInspectionOrderItemExists(updateReqVO.getId());

        // 2. 校验所属检验单存在
        validateInspectionOrderExists(updateReqVO.getInspectionOrderId());

        // 3. 单内 (vatNo, rollNo) 重复查重（排除自身）
        validateRollNoUnique(updateReqVO.getInspectionOrderId(), updateReqVO.getVatNo(), updateReqVO.getRollNo(), updateReqVO.getId());

        // 4. 更新（保留所属检验单ID，避免篡改父单据）
        InspectionOrderItemDO updateObj = BeanUtils.toBean(updateReqVO, InspectionOrderItemDO.class);
        updateObj.setInspectionOrderId(exist.getInspectionOrderId());
        inspectionOrderItemMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInspectionOrderItem(Long id) {
        // 校验存在
        validateInspectionOrderItemExists(id);
        // 逻辑删除
        inspectionOrderItemMapper.deleteById(id);
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

}
