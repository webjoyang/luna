package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderSaveReqVO;

/**
 * 生产管理-胚布单 Service 接口
 *
 * 本票（02）打通创建与取号；查看/修改（03）、
 * 颜色分配（04）、删除（05）由后续票据追加。
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
     * 创建胚布单
     *
     * @param createReqVO 创建信息（单号留空走自动号；坯布厂必填、染厂/合同/胚布品种可空引用）
     * @return 胚布单ID
     */
    Long createGreigeOrder(GreigeOrderSaveReqVO createReqVO);

    /**
     * 预览下一个胚布单号（只读、不占号）
     *
     * @return 下一个自动编号，形如 PB0919001
     */
    String getNextGreigeOrderNo();

}
