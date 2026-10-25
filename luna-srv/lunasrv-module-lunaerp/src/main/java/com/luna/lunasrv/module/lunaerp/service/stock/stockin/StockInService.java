package com.luna.lunasrv.module.lunaerp.service.stock.stockin;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInRespVO;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.StockInSaveReqVO;
import jakarta.validation.Valid;

import java.time.LocalDate;

/**
 * 库存管理 - 入库单 Service 接口
 *
 * @author luna
 */
public interface StockInService {

    /**
     * 创建入库单（聚合保存入库单主表 + 明细表）
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStockIn(@Valid StockInSaveReqVO createReqVO);

    /**
     * 更新入库单（仅草稿态可编辑，整表替换明细并重算金额）
     *
     * @param updateReqVO 更新信息
     */
    void updateStockIn(@Valid StockInSaveReqVO updateReqVO);

    /**
     * 删除入库单（仅草稿态可删除，级联逻辑删除明细）
     *
     * @param id 入库单编号
     */
    void deleteStockIn(Long id);

    /**
     * 获得入库单详情（包含明细列表）
     *
     * @param id 入库单编号
     * @return 入库单详情
     */
    StockInRespVO getStockIn(Long id);

    /**
     * 获得入库单分页列表（不含明细）
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<StockInRespVO> getStockInPage(StockInPageReqVO pageReqVO);

    /**
     * 预览下一个入库单号（只读、不占号）
     *
     * @param inDate 单据日期（可空，默认今天）
     * @return 下一个单号（IN-YYYYMMDD-NNN）
     */
    String getNextInNo(LocalDate inDate);

    /**
     * 审核入库单（DRAFT -> COMPLETED，同事务按库位分配展开写正数 STOCK_IN 流水，ADR-0016, ADR-0017）
     *
     * @param id 入库单编号
     */
    void auditStockIn(Long id);

    /**
     * 取消入库单（DRAFT -> CANCELLED 作废不写流水；COMPLETED -> CANCELLED 红冲逐条写入负数反向行，ADR-0016, ADR-0017）
     *
     * @param id 入库单编号
     */
    void cancelStockIn(Long id);

    /**
     * 作废草稿态入库单（供外部业务单据联动作废复用，仅当状态为 DRAFT 时原子更新为 CANCELLED）
     *
     * @param id 入库单编号
     * @return 是否成功作废草稿（若单据不存在或已被审核/取消则返回 false）
     */
    boolean cancelDraftStockIn(Long id);

    /**
     * 检验单生成胚布入库单草稿（COMPLETED 状态下显式生成，选仓，过滤 DEFECTIVE，幂等跳过已生成明细）
     *
     * @param reqVO 生成入库单请求
     * @return 生成结果（入库单ID、单号、生成行数、跳过行数）
     */
    com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderGenerateStockInRespVO createStockInFromInspection(
            @Valid com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderGenerateStockInReqVO reqVO);

}
