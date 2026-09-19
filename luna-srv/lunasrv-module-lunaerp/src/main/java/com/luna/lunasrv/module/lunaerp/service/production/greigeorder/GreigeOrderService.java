package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;

/**
 * 生产管理-胚布单 Service 接口
 *
 * 本票（01）只打通列表分页；创建/取号（02）、查看/修改（03）、
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

}
