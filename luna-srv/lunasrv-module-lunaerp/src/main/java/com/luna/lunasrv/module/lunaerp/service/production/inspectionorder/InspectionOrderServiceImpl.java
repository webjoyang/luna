package com.luna.lunasrv.module.lunaerp.service.production.inspectionorder;

import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * 生产管理-检验单 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
public class InspectionOrderServiceImpl implements InspectionOrderService {

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Override
    public PageResult<InspectionOrderRespVO> getInspectionOrderPage(InspectionOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<InspectionOrderDO> wrapper = buildPageWrapper(reqVO);
        wrapper.orderByDesc(InspectionOrderDO::getOrderDate);
        PageResult<InspectionOrderDO> pageResult = inspectionOrderMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, InspectionOrderRespVO.class);
    }

    /**
     * 构造分页查询条件：
     * keyword 对 code / product_code / product_name / inspection_factory_name 做 OR 模糊；
     * type / status / inspection_factory_id 等值过滤；
     * order_date 区间过滤。
     */
    private LambdaQueryWrapperX<InspectionOrderDO> buildPageWrapper(InspectionOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<InspectionOrderDO> wrapper = new LambdaQueryWrapperX<>();
        // 等值过滤
        wrapper.eqIfPresent(InspectionOrderDO::getType, reqVO.getType());
        wrapper.eqIfPresent(InspectionOrderDO::getStatus, reqVO.getStatus());
        wrapper.eqIfPresent(InspectionOrderDO::getInspectionFactoryId, reqVO.getInspectionFactoryId());

        // 日期区间过滤（兼容 orderDateStart/End 与 orderDate 数组）
        LocalDate orderStart = reqVO.getOrderDateStart() != null ? reqVO.getOrderDateStart()
                : (reqVO.getOrderDate() != null && reqVO.getOrderDate().length > 0 ? reqVO.getOrderDate()[0] : null);
        LocalDate orderEnd = reqVO.getOrderDateEnd() != null ? reqVO.getOrderDateEnd()
                : (reqVO.getOrderDate() != null && reqVO.getOrderDate().length > 1 ? reqVO.getOrderDate()[1] : null);
        wrapper.betweenIfPresent(InspectionOrderDO::getOrderDate, orderStart, orderEnd);

        // keyword：命中 code / product_code / product_name / inspection_factory_name 之一
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_code) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(inspection_factory_name) LIKE LOWER({0})", like));
        }
        return wrapper;
    }

}
