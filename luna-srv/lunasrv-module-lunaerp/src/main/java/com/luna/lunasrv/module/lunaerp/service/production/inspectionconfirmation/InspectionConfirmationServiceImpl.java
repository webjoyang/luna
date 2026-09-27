package com.luna.lunasrv.module.lunaerp.service.production.inspectionconfirmation;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.WorkerOptionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionconfirmation.InspectionConfirmationDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionconfirmation.InspectionConfirmationMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationConstants;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationStatusEnum;
import com.luna.lunasrv.module.system.api.dept.PostApi;
import com.luna.lunasrv.module.system.api.dept.dto.PostRespDTO;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

/**
 * 生产管理-质检确认 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
public class InspectionConfirmationServiceImpl implements InspectionConfirmationService {

    @Resource
    private InspectionConfirmationMapper inspectionConfirmationMapper;

    @Resource
    private PostApi postApi;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public PageResult<InspectionConfirmationRespVO> getInspectionConfirmationPage(InspectionConfirmationPageReqVO reqVO) {
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = buildPageWrapper(reqVO);
        PageResult<InspectionConfirmationDO> pageResult = inspectionConfirmationMapper.selectPage(reqVO, wrapper);
        return BeanUtils.toBean(pageResult, InspectionConfirmationRespVO.class);
    }

    @Override
    public List<WorkerOptionRespVO> getWorkerOptions() {
        PostRespDTO post = postApi.getPostByCode(InspectionConfirmationConstants.POST_CODE_LUNA_WORKER);
        if (post == null || post.getId() == null) {
            return Collections.emptyList();
        }
        List<AdminUserRespDTO> users = adminUserApi.getUserListByPostIds(Collections.singleton(post.getId()));
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyList();
        }
        return users.stream()
                .filter(u -> u.getId() != null)
                .map(u -> new WorkerOptionRespVO(u.getId(), u.getNickname()))
                .toList();
    }

    /**
     * 构造分页查询条件：
     * 1. status：缺省查询 PENDING + CONFIRMED；显式传值（如 REVOKED）则等值查询；
     * 2. source / responsibility / workerUserId / inspectionOrderId 等值过滤；
     * 3. createTime 发现日区间过滤；
     * 4. keyword 对 roll_no / vat_no / product_name / inspection_order_code 做 OR 模糊匹配；
     * 5. 默认按 create_time DESC 排序。
     */
    private LambdaQueryWrapperX<InspectionConfirmationDO> buildPageWrapper(InspectionConfirmationPageReqVO reqVO) {
        LambdaQueryWrapperX<InspectionConfirmationDO> wrapper = new LambdaQueryWrapperX<>();

        // 状态筛选：缺省只查待确认+已认定的有效行；显式传状态（如 REVOKED）才查指定行
        if (StringUtils.hasText(reqVO.getStatus())) {
            wrapper.eq(InspectionConfirmationDO::getStatus, reqVO.getStatus().trim());
        } else {
            wrapper.in(InspectionConfirmationDO::getStatus,
                    InspectionConfirmationStatusEnum.PENDING.getValue(),
                    InspectionConfirmationStatusEnum.CONFIRMED.getValue());
        }

        // 等值筛选
        wrapper.eqIfPresent(InspectionConfirmationDO::getSource, reqVO.getSource());
        wrapper.eqIfPresent(InspectionConfirmationDO::getResponsibility, reqVO.getResponsibility());
        wrapper.eqIfPresent(InspectionConfirmationDO::getWorkerUserId, reqVO.getWorkerUserId());
        wrapper.eqIfPresent(InspectionConfirmationDO::getInspectionOrderId, reqVO.getInspectionOrderId());

        // 发现日区间筛选
        if (reqVO.getCreateTime() != null && reqVO.getCreateTime().length > 0) {
            wrapper.betweenIfPresent(InspectionConfirmationDO::getCreateTime, reqVO.getCreateTime());
        }

        // keyword：OR 模糊匹配 roll_no / vat_no / product_name / inspection_order_code
        String keyword = reqVO.getKeyword() == null ? "" : reqVO.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String like = "%" + keyword + "%";
            wrapper.and(w -> w
                    .apply("LOWER(roll_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(vat_no) LIKE LOWER({0})", like)
                    .or().apply("LOWER(product_name) LIKE LOWER({0})", like)
                    .or().apply("LOWER(inspection_order_code) LIKE LOWER({0})", like));
        }

        // 默认创建时间倒序
        wrapper.orderByDesc(InspectionConfirmationDO::getCreateTime);
        return wrapper;
    }

}
