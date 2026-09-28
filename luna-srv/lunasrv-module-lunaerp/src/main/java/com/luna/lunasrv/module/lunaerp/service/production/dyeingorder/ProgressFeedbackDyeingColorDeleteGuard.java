package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.ProgressFeedbackMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PROGRESS_FEEDBACK_COLOR_HAS_VATS;

/**
 * 染色单颜色行删除守卫实现类（ADR-0031 / ADR-0032）
 *
 * 校验待删除的颜色行集合下是否存在关联的反馈缸。若存在，硬拦截并提示先删除缸反馈，杜绝孤儿反馈数据。
 *
 * @author luna
 */
@Component
@Primary
public class ProgressFeedbackDyeingColorDeleteGuard implements DyeingColorDeleteGuard {

    @Resource
    private ProgressFeedbackMapper progressFeedbackMapper;

    @Resource
    private DyeingColorMapper dyeingColorMapper;

    @Override
    public void validateCanDeleteColors(Long dyeingOrderId, Collection<Long> colorIds) {
        if (colorIds == null || colorIds.isEmpty()) {
            return;
        }

        // 1. 批量查询关联的反馈缸（过滤未删除缸）
        List<ProgressFeedbackDO> vats = progressFeedbackMapper.selectListByColorIds(colorIds);
        if (vats == null || vats.isEmpty()) {
            return;
        }

        List<ProgressFeedbackDO> activeVats = vats.stream()
                .filter(v -> v.getDeleted() == null || !v.getDeleted())
                .collect(Collectors.toList());
        if (activeVats.isEmpty()) {
            return;
        }

        // 2. 按颜色明细行分组，找出有缸的颜色行
        Map<Long, List<ProgressFeedbackDO>> vatsByColor = activeVats.stream()
                .collect(Collectors.groupingBy(ProgressFeedbackDO::getDyeingColorId));

        // 保证提示确定性：优先按传入的 colorIds 迭代顺序选取首个违规颜色
        Long targetColorId = colorIds.stream()
                .filter(vatsByColor::containsKey)
                .findFirst()
                .orElseGet(() -> vatsByColor.keySet().iterator().next());
        int vatCount = vatsByColor.get(targetColorId).size();

        // 3. 查该颜色行的标识（名称或色号）用于友好提示
        DyeingColorDO color = dyeingColorMapper.selectById(targetColorId);
        String colorIdentifier;
        if (color != null && StringUtils.isNotBlank(color.getColorName())) {
            colorIdentifier = color.getColorName();
        } else if (color != null && StringUtils.isNotBlank(color.getCustomerColorNo())) {
            colorIdentifier = color.getCustomerColorNo();
        } else {
            colorIdentifier = String.valueOf(targetColorId);
        }

        // 4. 抛出 1-030-017-004: 颜色【{}】下有 {} 条缸进度反馈，请先删除后再移除该颜色行
        throw exception(PROGRESS_FEEDBACK_COLOR_HAS_VATS, colorIdentifier, vatCount);
    }

}
