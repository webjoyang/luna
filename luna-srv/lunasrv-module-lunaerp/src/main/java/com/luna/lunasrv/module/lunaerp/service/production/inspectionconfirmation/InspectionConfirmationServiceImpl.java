package com.luna.lunasrv.module.lunaerp.service.production.inspectionconfirmation;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.framework.security.core.util.SecurityFrameworkUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationGenerateRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationManualCreateReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.InspectionConfirmationRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionconfirmation.vo.WorkerOptionRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionconfirmation.InspectionConfirmationDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionconfirmation.InspectionConfirmationMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationConstants;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationResponsibilityEnum;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationSourceEnum;
import com.luna.lunasrv.module.lunaerp.enums.production.inspectionconfirmation.InspectionConfirmationStatusEnum;
import com.luna.lunasrv.module.lunaerp.service.production.inspectionorder.InspectionOrderServiceImpl;
import com.luna.lunasrv.module.system.api.dept.PostApi;
import com.luna.lunasrv.module.system.api.dept.dto.PostRespDTO;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

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
    private InspectionOrderMapper inspectionOrderMapper;

    @Resource
    private InspectionOrderItemMapper inspectionOrderItemMapper;

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

    private static final Set<String> ALLOWED_GRADES = Set.of("A", "B", "C", "DEFECTIVE");

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long manualCreateInspectionConfirmation(InspectionConfirmationManualCreateReqVO reqVO) {
        // 1. 必填校验：rollNo、grade、responsibility
        if (reqVO == null) {
            throw exception(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS, "登记信息");
        }
        if (!StringUtils.hasText(reqVO.getRollNo())) {
            throw exception(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS, "卷号");
        }
        if (!StringUtils.hasText(reqVO.getGrade())) {
            throw exception(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS, "等级");
        }
        String grade = reqVO.getGrade().trim().toUpperCase();
        if (!ALLOWED_GRADES.contains(grade)) {
            throw exception(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS, "合法等级（A/B/C/DEFECTIVE）");
        }
        if (!StringUtils.hasText(reqVO.getResponsibility())) {
            throw exception(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS, "责任类型");
        }

        // 2. 校验责任类型合法性（三值）
        String responsibility = reqVO.getResponsibility().trim();
        if (!InspectionConfirmationResponsibilityEnum.isValid(responsibility)) {
            throw exception(INSPECTION_CONFIRMATION_RESPONSIBILITY_INVALID);
        }

        // 3. 责任类型为 OPERATOR 时 workerUserId 必填；非 OPERATOR 时置空挡车工信息
        Long workerUserId = null;
        String workerName = null;
        if (InspectionConfirmationResponsibilityEnum.OPERATOR.getValue().equals(responsibility)) {
            workerUserId = reqVO.getWorkerUserId();
            if (workerUserId == null) {
                throw exception(INSPECTION_CONFIRMATION_WORKER_REQUIRED);
            }
            adminUserApi.validateUser(workerUserId);
            AdminUserRespDTO worker = adminUserApi.getUser(workerUserId);
            workerName = worker != null ? worker.getNickname() : null;
        }

        // 4. inspectionOrderId 可选：传则校验检验单存在并快照 code（不存在报 1_030_014_002）
        String inspectionOrderCode = null;
        if (reqVO.getInspectionOrderId() != null) {
            InspectionOrderDO inspectionOrder = inspectionOrderMapper.selectById(reqVO.getInspectionOrderId());
            if (inspectionOrder == null) {
                throw exception(INSPECTION_CONFIRMATION_ORDER_NOT_EXISTS);
            }
            inspectionOrderCode = inspectionOrder.getCode();
        }

        // 5. 获取当前登录用户（id + 姓名快照）
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String confirmedByName = null;
        if (loginUserId != null) {
            AdminUserRespDTO loginUser = adminUserApi.getUser(loginUserId);
            confirmedByName = loginUser != null ? loginUser.getNickname() : SecurityFrameworkUtils.getLoginUserNickname();
        }

        // 6. 认定时间：缺省服务端当前时间、允许传过去时间补录（不允许未来时间）
        LocalDateTime confirmedAt = reqVO.getConfirmedAt() != null ? reqVO.getConfirmedAt() : LocalDateTime.now();
        if (confirmedAt.isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw exception(INSPECTION_CONFIRMATION_MANUAL_REQUIRED_FIELDS, "认定时间不能为未来时间");
        }

        // 7. 新 chain_key 与落库（source=MANUAL、status=CONFIRMED）
        String chainKey = IdUtil.fastSimpleUUID();

        InspectionConfirmationDO confirmation = new InspectionConfirmationDO();
        confirmation.setChainKey(chainKey);
        confirmation.setSource(InspectionConfirmationSourceEnum.MANUAL.getValue());
        confirmation.setStatus(InspectionConfirmationStatusEnum.CONFIRMED.getValue());
        confirmation.setInspectionOrderId(reqVO.getInspectionOrderId());
        confirmation.setInspectionOrderCode(inspectionOrderCode);
        confirmation.setInspectionOrderItemId(null);
        confirmation.setRollNo(reqVO.getRollNo().trim());
        confirmation.setVatNo(reqVO.getVatNo() != null ? reqVO.getVatNo().trim() : null);
        confirmation.setGrade(reqVO.getGrade().trim());
        confirmation.setMachineNo(reqVO.getMachineNo() != null ? reqVO.getMachineNo().trim() : null);
        confirmation.setProductCode(reqVO.getProductCode() != null ? reqVO.getProductCode().trim() : null);
        confirmation.setProductName(reqVO.getProductName() != null ? reqVO.getProductName().trim() : null);
        confirmation.setColorName(reqVO.getColorName() != null ? reqVO.getColorName().trim() : null);
        confirmation.setColorNo(reqVO.getColorNo() != null ? reqVO.getColorNo().trim() : null);
        confirmation.setStyleNo(reqVO.getStyleNo() != null ? reqVO.getStyleNo().trim() : null);
        confirmation.setMeasuredWeight(reqVO.getMeasuredWeight());
        confirmation.setLengthM(reqVO.getLengthM());
        confirmation.setResponsibility(responsibility);
        confirmation.setWorkerUserId(workerUserId);
        confirmation.setWorkerName(workerName);
        confirmation.setConfirmedByUserId(loginUserId);
        confirmation.setConfirmedByName(confirmedByName);
        confirmation.setConfirmedAt(confirmedAt);
        confirmation.setNote(reqVO.getNote() != null ? reqVO.getNote().trim() : null);

        inspectionConfirmationMapper.insert(confirmation);
        return confirmation.getId();
    }

    private static final Set<String> CONFIRMATION_GRADES = Set.of("B", "C", "DEFECTIVE");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionConfirmationGenerateRespVO generateFromOrder(InspectionConfirmationGenerateReqVO reqVO) {
        // 1. 检验单存在性校验（不存在报 1_030_014_002）
        if (reqVO == null || reqVO.getInspectionOrderId() == null) {
            throw exception(INSPECTION_CONFIRMATION_ORDER_NOT_EXISTS);
        }
        InspectionOrderDO order = inspectionOrderMapper.selectById(reqVO.getInspectionOrderId());
        if (order == null) {
            throw exception(INSPECTION_CONFIRMATION_ORDER_NOT_EXISTS);
        }

        // 2. 状态非 ACTIVE/COMPLETED（DRAFT/CANCELLED）报 1_030_014_003
        if (!InspectionOrderServiceImpl.STATUS_ACTIVE.equals(order.getStatus())
                && !InspectionOrderServiceImpl.STATUS_COMPLETED.equals(order.getStatus())) {
            throw exception(INSPECTION_CONFIRMATION_ORDER_STATUS_NOT_ALLOWED);
        }

        // 3. 查询该检验单的所有录检明细
        List<InspectionOrderItemDO> items = inspectionOrderItemMapper.selectListByInspectionOrderId(order.getId());
        if (CollUtil.isEmpty(items)) {
            return new InspectionConfirmationGenerateRespVO(0, 0);
        }

        // 4. 幂等：查本表该检验单下已有有效行（status ∈ PENDING,CONFIRMED）的 item_id 集合
        List<InspectionConfirmationDO> existingConfirmations = inspectionConfirmationMapper.selectList(
                new LambdaQueryWrapperX<InspectionConfirmationDO>()
                        .eq(InspectionConfirmationDO::getInspectionOrderId, order.getId())
                        .in(InspectionConfirmationDO::getStatus,
                                InspectionConfirmationStatusEnum.PENDING.getValue(),
                                InspectionConfirmationStatusEnum.CONFIRMED.getValue())
                        .isNotNull(InspectionConfirmationDO::getInspectionOrderItemId));
        Set<Long> existingItemIds = existingConfirmations.stream()
                .map(InspectionConfirmationDO::getInspectionOrderItemId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        // 5. 扫描录检明细：仅 B/C/DEFECTIVE 生成，A/无等级跳过，已存在有效确认行跳过
        int generatedCount = 0;
        int skippedCount = 0;

        for (InspectionOrderItemDO item : items) {
            String grade = item.getGrade() != null ? item.getGrade().trim().toUpperCase() : null;
            if (grade == null || !CONFIRMATION_GRADES.contains(grade)) {
                skippedCount++;
                continue;
            }
            if (existingItemIds.contains(item.getId())) {
                skippedCount++;
                continue;
            }

            // 新行快照并落库
            InspectionConfirmationDO confirmation = buildConfirmationFromOrderItem(order, item, grade);
            inspectionConfirmationMapper.insert(confirmation);
            existingItemIds.add(item.getId());
            generatedCount++;
        }

        return new InspectionConfirmationGenerateRespVO(generatedCount, skippedCount);
    }

    private InspectionConfirmationDO buildConfirmationFromOrderItem(InspectionOrderDO order,
                                                                   InspectionOrderItemDO item,
                                                                   String grade) {
        InspectionConfirmationDO confirmation = new InspectionConfirmationDO();
        confirmation.setChainKey(IdUtil.fastSimpleUUID());
        confirmation.setSource(InspectionConfirmationSourceEnum.INSPECTION.getValue());
        confirmation.setStatus(InspectionConfirmationStatusEnum.PENDING.getValue());
        confirmation.setInspectionOrderId(order.getId());
        confirmation.setInspectionOrderCode(order.getCode());
        confirmation.setInspectionOrderItemId(item.getId());
        confirmation.setRollNo(item.getRollNo());
        confirmation.setVatNo(item.getVatNo());
        confirmation.setGrade(grade);
        confirmation.setMachineNo(item.getMachineNo());
        confirmation.setProductCode(StringUtils.hasText(item.getProductCode()) ? item.getProductCode().trim() : order.getProductCode());
        confirmation.setProductName(StringUtils.hasText(item.getProductName()) ? item.getProductName().trim() : order.getProductName());
        confirmation.setColorName(item.getColorName());
        confirmation.setColorNo(item.getColorNo());
        confirmation.setStyleNo(item.getStyleNo());
        confirmation.setMeasuredWeight(item.getMeasuredWeight());
        confirmation.setLengthM(item.getLengthM());
        return confirmation;
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
