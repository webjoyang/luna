package com.luna.lunasrv.module.lunaerp.service.production.scheduling;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.task.MachineTaskDO;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排产冲突计算工具类（与机台任务冲突校验硬规则同源口径，ADR-0041）
 *
 * @author luna
 */
public class SchedulingConflictUtils {

    /**
     * 占用态状态集合：仅 PENDING / IN_PROGRESS / HALTED 占用机台并参与冲突计算
     */
    public static final Set<String> OCCUPIED_STATUSES = Set.of("PENDING", "IN_PROGRESS", "HALTED");

    private SchedulingConflictUtils() {
    }

    /**
     * 判断两个任务区间（或任务与窗口）是否闭区间相交（同日即冲突）
     * 有效区间：effStart = coalesce(start, end)，effEnd = coalesce(end, start)
     * 两端皆空返回 false；相交判定：effStart1 <= effEnd2 && effEnd1 >= effStart2
     *
     * @param start1 起始日期1
     * @param end1   结束日期1
     * @param start2 起始日期2
     * @param end2   结束日期2
     * @return true 若闭区间相交
     */
    public static boolean isIntervalOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        LocalDate effStart1 = start1 != null ? start1 : end1;
        LocalDate effEnd1 = end1 != null ? end1 : start1;
        LocalDate effStart2 = start2 != null ? start2 : end2;
        LocalDate effEnd2 = end2 != null ? end2 : start2;

        if (effStart1 == null || effStart2 == null) {
            return false;
        }
        return !effStart1.isAfter(effEnd2) && !effEnd1.isBefore(effStart2);
    }

    /**
     * 从一组占用态机台任务中，按织机分组，两两比较时段相交，找出所有冲突任务的 ID 集合
     *
     * 规则：
     * 1. 仅同织机下状态为 PENDING/IN_PROGRESS/HALTED 且至少有一个日期非空的任务参与；
     * 2. COMPLETED 任务不参与，不令他任务标冲突；
     * 3. 互相重叠的存量任务两两均标记为冲突；排除自身对比。
     *
     * @param tasks 任务集合
     * @return 冲突任务ID集合
     */
    public static Set<Long> findConflictTaskIds(Collection<MachineTaskDO> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptySet();
        }

        Map<Long, List<MachineTaskDO>> loomTaskMap = tasks.stream()
                .filter(t -> t.getLoomId() != null)
                .filter(t -> OCCUPIED_STATUSES.contains(t.getStatus()))
                .filter(t -> t.getPlannedStart() != null || t.getPlannedEnd() != null)
                .collect(Collectors.groupingBy(MachineTaskDO::getLoomId));

        Set<Long> conflictIds = new HashSet<>();
        for (List<MachineTaskDO> loomTasks : loomTaskMap.values()) {
            int size = loomTasks.size();
            for (int i = 0; i < size; i++) {
                MachineTaskDO t1 = loomTasks.get(i);
                for (int j = i + 1; j < size; j++) {
                    MachineTaskDO t2 = loomTasks.get(j);
                    if (isIntervalOverlap(t1.getPlannedStart(), t1.getPlannedEnd(),
                            t2.getPlannedStart(), t2.getPlannedEnd())) {
                        conflictIds.add(t1.getId());
                        conflictIds.add(t2.getId());
                    }
                }
            }
        }
        return conflictIds;
    }

}
