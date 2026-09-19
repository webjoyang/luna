package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.dyeingorder.vo.DyeingOrderRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 生产管理-染色单 Service 实现类
 *
 * 染色单无业务编号（ADR-0009），对外标识靠 id + 合同号快照。
 * 状态为四态 DRAFT/ACTIVE/COMPLETED/CANCELLED，缺省 DRAFT（ADR-0008）。
 * 颜色明细（04）随单聚合保存（ADR-0003）。
 *
 * @author luna
 */
@Service
public class DyeingOrderServiceImpl implements DyeingOrderService {

    @Resource
    private DyeingOrderMapper dyeingOrderMapper;

    @Override
    public PageResult<DyeingOrderRespVO> getDyeingOrderPage(DyeingOrderPageReqVO reqVO) {
        // 构造查询条件
        LambdaQueryWrapperX<DyeingOrderDO> wrapper = buildPageWrapper(reqVO);
        wrapper.orderByDesc(DyeingOrderDO::getOrderDate);
        // 执行查询
        PageResult<DyeingOrderDO> pageResult = dyeingOrderMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, DyeingOrderRespVO.class);
    }

    /**
     * 构造分页查询条件：
     * keyword 对 contract_no / client_name / product_code / product_name 做 OR 模糊
     * （PostgreSQL 用 LOWER(...) LIKE LOWER({0}) 实现 ILIKE 语义，占位符走预编译）；
     * status / customer_id / dyeing_factory_id 等值过滤；
     * order_date / delivery_date 区间过滤。
     */
    private LambdaQueryWrapperX<DyeingOrderDO> buildPageWrapper(DyeingOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<DyeingOrderDO> wrapper = new LambdaQueryWrapperX<>();
        // 等值过滤
        wrapper.eqIfPresent(DyeingOrderDO::getStatus, reqVO.getStatus());
        wrapper.eqIfPresent(DyeingOrderDO::getCustomerId, reqVO.getCustomerId());
        wrapper.eqIfPresent(DyeingOrderDO::getDyeingFactoryId, reqVO.getDyeingFactoryId());

        // 日期区间过滤（兼容 orderDateStart/End 与 orderDate 数组）
        LocalDate orderStart = reqVO.getOrderDateStart() != null ? reqVO.getOrderDateStart()
                : (reqVO.getOrderDate() != null && reqVO.getOrderDate().length > 0 ? reqVO.getOrderDate()[0] : null);
        LocalDate orderEnd = reqVO.getOrderDateEnd() != null ? reqVO.getOrderDateEnd()
                : (reqVO.getOrderDate() != null && reqVO.getOrderDate().length > 1 ? reqVO.getOrderDate()[1] : null);
        wrapper.betweenIfPresent(DyeingOrderDO::getOrderDate, orderStart, orderEnd);

        LocalDate deliveryStart = reqVO.getDeliveryDateStart() != null ? reqVO.getDeliveryDateStart()
                : (reqVO.getDeliveryDate() != null && reqVO.getDeliveryDate().length > 0 ? reqVO.getDeliveryDate()[0] : null);
        LocalDate deliveryEnd = reqVO.getDeliveryDateEnd() != null ? reqVO.getDeliveryDateEnd()
                : (reqVO.getDeliveryDate() != null && reqVO.getDeliveryDate().length > 1 ? reqVO.getDeliveryDate()[1] : null);
        wrapper.betweenIfPresent(DyeingOrderDO::getDeliveryDate, deliveryStart, deliveryEnd);

        // keyword：命中 contract_no / client_name / product_code / product_name 之一
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(contract_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(client_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like));
        }
        return wrapper;
    }

}
