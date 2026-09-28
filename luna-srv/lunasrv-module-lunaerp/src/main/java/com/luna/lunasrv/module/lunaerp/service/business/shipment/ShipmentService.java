package com.luna.lunasrv.module.lunaerp.service.business.shipment;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentRespVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.business.shipment.vo.ShipmentUpdateSignReqVO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * 业务管理 - 发货单 Service 接口
 *
 * @author luna
 */
public interface ShipmentService {

    /**
     * 创建发货单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createShipment(@Valid ShipmentSaveReqVO createReqVO);

    /**
     * 更新发货单（仅草稿态可编辑，明细与卷清单整体替换）
     *
     * @param updateReqVO 更新信息
     */
    void updateShipment(@Valid ShipmentSaveReqVO updateReqVO);

    /**
     * 删除发货单（仅草稿态可删，级联逻辑删除明细与卷清单）
     *
     * @param id 编号
     */
    void deleteShipment(Long id);

    /**
     * 获得发货单详情（包含发货明细与卷清单）
     *
     * @param id 编号
     * @return 发货单详情
     */
    ShipmentRespVO getShipment(Long id);

    /**
     * 获得发货单分页
     *
     * @param reqVO 分页查询
     * @return 发货单分页
     */
    PageResult<ShipmentRespVO> getShipmentPage(ShipmentPageReqVO reqVO);

    /**
     * 预览下一个发货单号（只读、不占号）
     *
     * @param shippingDate 发货日期（为空缺省当天）
     * @return 发货单号预览
     */
    String getNextShipmentNo(LocalDate shippingDate);

    /**
     * 从销售合同明细带入发货明细集合（含实时合同未发量计算，只读不落库）
     *
     * @param contractId 销售合同ID
     * @return 预填的发货明细列表
     */
    List<ShipmentItemRespVO> getShipmentItemsFromContract(Long contractId);

    /**
     * 审核发货单（DRAFT -> COMPLETED，硬校验库存可用量后在同事务写负数出库流水）
     *
     * @param id 发货单ID
     */
    void auditShipment(Long id);

    /**
     * 取消发货单（草稿态直接作废；已发货态按原流水逐条红冲追加正数流水）
     *
     * @param id 发货单ID
     */
    void cancelShipment(Long id);

    /**
     * 登记/修改发货单签收信息（仅已发货 COMPLETED 单据可操作）
     *
     * @param reqVO 签收登记参数
     */
    void updateShipmentSign(ShipmentUpdateSignReqVO reqVO);

}
