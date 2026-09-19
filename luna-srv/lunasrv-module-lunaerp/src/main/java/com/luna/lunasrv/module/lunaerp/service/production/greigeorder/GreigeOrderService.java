package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;

/**
 * 生产管理-胚布单 Service 接口
 *
 * 02 票打通创建与取号，03 票追加查看与修改，04 票追加颜色投胚分配（随单聚合保存）；删除（05）由后续票据追加。
 *
 * @author luna
 */
public interface GreigeOrderService {

    /**
     * 获得胚布单分页
     *
     * @param pageReqVO 分页查询（keyword 命中单号/合同号/产品名称/产品编号，status 等值）
     * @return 胚布单分页（默认单据日期倒序）
     */
    PageResult<GreigeOrderRespVO> getGreigeOrderPage(GreigeOrderPageReqVO pageReqVO);

    /**
     * 获得胚布单详情
     *
     * @param id 胚布单ID
     * @return 胚布单详情（主表全部字段 + 颜色投胚分配行按 line_no 升序嵌套返回）
     */
    GreigeOrderRespVO getGreigeOrder(Long id);

    /**
     * 创建胚布单
     *
     * @param createReqVO 创建信息（单号留空走自动号；坯布厂必填、染厂/合同/胚布品种可空引用；
     *                    颜色行随单整体保存，投胚需求服务端权威重算，零颜色合法）
     * @return 胚布单ID
     */
    Long createGreigeOrder(GreigeOrderSaveReqVO createReqVO);

    /**
     * 更新胚布单（整体保存）
     *
     * @param updateReqVO 更新信息（含 id；编号/引用校验规则与创建一致，编号查重排除自身；
     *                    颜色行整体替换：事务内按 order_id 删旧行、插新行并重排 line_no）
     */
    void updateGreigeOrder(GreigeOrderSaveReqVO updateReqVO);

    /**
     * 预览下一个胚布单号（只读、不占号）
     *
     * @return 下一个自动编号，形如 PB0919001
     */
    String getNextGreigeOrderNo();

}
