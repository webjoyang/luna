package com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.payment;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 财务核销分录已核销金额聚合 DTO
 *
 * 用于 PaymentRefMapper 强类型聚合出参，避免 Untyped Map 大小写解析异味。
 *
 * @author luna
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryAllocatedSumDTO {

    /**
     * 待核销的分录ID (ref_id)
     */
    private Long entryId;

    /**
     * 累计已核销金额
     */
    private BigDecimal totalAllocated;

    /**
     * 将 DTO 列表转换为 Map<entryId, totalAllocated>
     */
    public static Map<Long, BigDecimal> toMap(Collection<EntryAllocatedSumDTO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> map = new HashMap<>();
        for (EntryAllocatedSumDTO item : list) {
            if (item != null && item.getEntryId() != null) {
                map.put(item.getEntryId(), item.getTotalAllocated() != null ? item.getTotalAllocated() : BigDecimal.ZERO);
            }
        }
        return map;
    }

}
