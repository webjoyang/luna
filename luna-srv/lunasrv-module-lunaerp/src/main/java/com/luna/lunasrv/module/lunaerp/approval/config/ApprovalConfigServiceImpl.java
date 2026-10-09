package com.luna.lunasrv.module.lunaerp.approval.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.ApprovalConfigNodeVO;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.ApprovalConfigPageReqVO;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.ApprovalConfigRespVO;
import com.luna.lunasrv.module.lunaerp.approval.config.vo.ApprovalConfigSaveReqVO;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceDO;
import com.luna.lunasrv.module.lunaerp.approval.instance.ApprovalInstanceMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.framework.common.util.collection.CollectionUtils.convertList;
import static com.luna.lunasrv.module.lunaerp.enums.ApprovalErrorCodeConstants.*;

/**
 * 审批流程配置 Service 实现类
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ApprovalConfigServiceImpl implements ApprovalConfigService {

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    @Resource
    private ApprovalConfigMapper approvalConfigMapper;

    @Resource
    private ApprovalConfigNodeMapper approvalConfigNodeMapper;

    @Resource
    private ApprovalInstanceMapper approvalInstanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApprovalConfig(ApprovalConfigSaveReqVO createReqVO) {
        String status = StrUtil.blankToDefault(createReqVO.getStatus(), STATUS_ENABLED);

        // 业务守卫：同 biz_type 同一时刻只允许一条启用流程
        if (STATUS_ENABLED.equalsIgnoreCase(status)) {
            validateBizTypeEnabledUnique(createReqVO.getBizType(), null);
        }

        // 插入主表
        ApprovalConfigDO configDO = ApprovalConfigDO.builder()
                .bizType(createReqVO.getBizType())
                .name(createReqVO.getName())
                .status(status)
                .remark(createReqVO.getRemark())
                .build();
        approvalConfigMapper.insert(configDO);

        // 插入节点列表
        saveNodes(configDO.getId(), createReqVO.getNodes());

        return configDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalConfig(Long id, ApprovalConfigSaveReqVO updateReqVO) {
        ApprovalConfigDO existing = validateApprovalConfigExists(id);

        String targetBizType = StrUtil.blankToDefault(updateReqVO.getBizType(), existing.getBizType());
        String targetStatus = StrUtil.blankToDefault(updateReqVO.getStatus(), existing.getStatus());

        // 业务守卫：启用时排他校验
        if (STATUS_ENABLED.equalsIgnoreCase(targetStatus)) {
            validateBizTypeEnabledUnique(targetBizType, id);
        }

        // 更新流程主表
        ApprovalConfigDO updateDO = ApprovalConfigDO.builder()
                .id(id)
                .bizType(targetBizType)
                .name(updateReqVO.getName())
                .status(targetStatus)
                .remark(updateReqVO.getRemark())
                .build();
        approvalConfigMapper.updateById(updateDO);

        // 节点整体替换（删旧插新）
        approvalConfigNodeMapper.deleteByConfigId(id);
        saveNodes(id, updateReqVO.getNodes());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApprovalConfigStatus(Long id, String status) {
        ApprovalConfigDO existing = validateApprovalConfigExists(id);

        if (STATUS_ENABLED.equalsIgnoreCase(status)) {
            validateBizTypeEnabledUnique(existing.getBizType(), id);
        }

        ApprovalConfigDO updateDO = ApprovalConfigDO.builder()
                .id(id)
                .status(status)
                .build();
        approvalConfigMapper.updateById(updateDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApprovalConfig(Long id) {
        ApprovalConfigDO existing = validateApprovalConfigExists(id);

        // 业务守卫：已产生审批实例的流程不允许物理/逻辑删除，只能停用
        if (hasInstanceReference(id, existing.getBizType())) {
            throw exception(APPROVAL_CONFIG_HAS_ACTIVE_INSTANCE);
        }

        // 软删流程定义及节点
        approvalConfigMapper.deleteById(id);
        approvalConfigNodeMapper.deleteByConfigId(id);
    }

    @Override
    public ApprovalConfigRespVO getApprovalConfig(Long id) {
        ApprovalConfigDO config = validateApprovalConfigExists(id);
        List<ApprovalConfigNodeDO> nodeDOs = approvalConfigNodeMapper.selectListByConfigId(id);

        ApprovalConfigRespVO respVO = BeanUtils.toBean(config, ApprovalConfigRespVO.class);
        respVO.setNodes(convertList(nodeDOs, node -> BeanUtils.toBean(node, ApprovalConfigNodeVO.class)));
        return respVO;
    }

    @Override
    public PageResult<ApprovalConfigRespVO> getApprovalConfigPage(ApprovalConfigPageReqVO pageReqVO) {
        PageResult<ApprovalConfigDO> pageResult = approvalConfigMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }

        // 批量查询各流程节点
        List<Long> configIds = convertList(pageResult.getList(), ApprovalConfigDO::getId);
        List<ApprovalConfigNodeDO> nodeDOList = approvalConfigNodeMapper.selectListByConfigIds(configIds);
        Map<Long, List<ApprovalConfigNodeDO>> nodeMap = nodeDOList.stream()
                .collect(Collectors.groupingBy(ApprovalConfigNodeDO::getConfigId));

        List<ApprovalConfigRespVO> respList = pageResult.getList().stream().map(config -> {
            ApprovalConfigRespVO respVO = BeanUtils.toBean(config, ApprovalConfigRespVO.class);
            List<ApprovalConfigNodeDO> nodes = nodeMap.getOrDefault(config.getId(), Collections.emptyList());
            respVO.setNodes(convertList(nodes, node -> BeanUtils.toBean(node, ApprovalConfigNodeVO.class)));
            return respVO;
        }).collect(Collectors.toList());

        return new PageResult<>(respList, pageResult.getTotal());
    }

    @Override
    public ApprovalConfigDO getEnabledConfigByBizType(String bizType) {
        if (StrUtil.isBlank(bizType)) {
            return null;
        }
        return approvalConfigMapper.selectByBizTypeAndStatus(bizType, STATUS_ENABLED);
    }

    private void saveNodes(Long configId, List<ApprovalConfigNodeVO> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return;
        }
        int defaultSort = 1;
        for (ApprovalConfigNodeVO nodeVO : nodes) {
            ApprovalConfigNodeDO nodeDO = BeanUtils.toBean(nodeVO, ApprovalConfigNodeDO.class);
            nodeDO.setId(null); // 由主键序列自增
            nodeDO.setConfigId(configId);
            if (nodeDO.getSort() == null) {
                nodeDO.setSort(defaultSort);
            }
            approvalConfigNodeMapper.insert(nodeDO);
            defaultSort++;
        }
    }

    private ApprovalConfigDO validateApprovalConfigExists(Long id) {
        if (id == null) {
            throw exception(APPROVAL_CONFIG_NOT_EXISTS);
        }
        ApprovalConfigDO config = approvalConfigMapper.selectById(id);
        if (config == null) {
            throw exception(APPROVAL_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateBizTypeEnabledUnique(String bizType, Long excludeId) {
        ApprovalConfigDO existing = approvalConfigMapper.selectByBizTypeAndStatus(bizType, STATUS_ENABLED);
        if (existing != null && (excludeId == null || !existing.getId().equals(excludeId))) {
            throw exception(APPROVAL_CONFIG_BIZ_TYPE_ALREADY_ENABLED);
        }
    }

    private boolean hasInstanceReference(Long configId, String bizType) {
        if (approvalInstanceMapper == null) {
            return false;
        }
        // 1. 检查 process_snapshot 快照中是否显式包含本 configId 引用
        Long snapshotCount = approvalInstanceMapper.selectCount(new LambdaQueryWrapperX<ApprovalInstanceDO>()
                .and(w -> w.apply("process_snapshot LIKE {0}", "%\"configId\":" + configId + "%")
                        .or().apply("process_snapshot LIKE {0}", "%\"configId\":\"" + configId + "\"%")));
        if (snapshotCount != null && snapshotCount > 0) {
            return true;
        }

        // 2. 检查是否有同 bizType 实例且未显式指定其他 configId
        Long bizCount = approvalInstanceMapper.selectCount(new LambdaQueryWrapperX<ApprovalInstanceDO>()
                .eq(ApprovalInstanceDO::getBizType, bizType)
                .notLike(ApprovalInstanceDO::getProcessSnapshot, "\"configId\":"));
        return bizCount != null && bizCount > 0;
    }

}
