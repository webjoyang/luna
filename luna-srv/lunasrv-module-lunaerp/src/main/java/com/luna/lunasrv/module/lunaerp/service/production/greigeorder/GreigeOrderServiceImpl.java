package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.greigeorder.vo.GreigeOrderRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 生产管理-胚布单 Service 实现类
 *
 * 本票（01）只打通列表分页；创建/取号（02）、查看/修改（03）、
 * 颜色分配（04）、删除（05）由后续票据追加。
 *
 * @author luna
 */
@Service
public class GreigeOrderServiceImpl implements GreigeOrderService {

    @Resource
    private GreigeOrderMapper greigeOrderMapper;

    @Override
    public PageResult<GreigeOrderRespVO> getGreigeOrderPage(GreigeOrderPageReqVO reqVO) {
        // 构造查询条件
        LambdaQueryWrapperX<GreigeOrderDO> wrapper = buildPageWrapper(reqVO);
        wrapper.orderByDesc(GreigeOrderDO::getOrderDate);
        // 执行查询
        PageResult<GreigeOrderDO> pageResult = greigeOrderMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, GreigeOrderRespVO.class);
    }

    // ==================== 私有方法 ====================

    /**
     * 构造分页查询条件：keyword 对 order_no/contract_no/product_name/product_code 做 OR 模糊
     * （PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 ILIKE 语义，占位符走预编译），
     * status 等值过滤。
     */
    private LambdaQueryWrapperX<GreigeOrderDO> buildPageWrapper(GreigeOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<GreigeOrderDO> wrapper = new LambdaQueryWrapperX<>();
        // 等值过滤（为空则不过滤，便于"全部"场景）
        wrapper.eqIfPresent(GreigeOrderDO::getStatus, reqVO.getStatus());
        // keyword：命中 order_no / contract_no / product_name / product_code 之一
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(order_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(contract_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", like));
        }
        return wrapper;
    }

}
