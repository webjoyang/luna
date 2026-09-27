package com.luna.lunasrv.module.system.api.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.luna.lunasrv.framework.common.util.collection.CollectionUtils;
import com.luna.lunasrv.module.system.api.dept.dto.PostRespDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 岗位 API 接口
 *
 * @author luna
 */
public interface PostApi {

    /**
     * 校验岗位们是否有效。如下情况，视为无效：
     * 1. 岗位编号不存在
     * 2. 岗位被禁用
     *
     * @param ids 岗位编号数组
     */
    void validPostList(Collection<Long> ids);

    List<PostRespDTO> getPostList(Collection<Long> ids);

    /**
     * 根据岗位编码获得岗位信息
     *
     * @param code 岗位编码
     * @return 岗位信息（不存在返回 null）
     */
    PostRespDTO getPostByCode(String code);

    default Map<Long, PostRespDTO> getPostMap(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return MapUtil.empty();
        }

        List<PostRespDTO> list = getPostList(ids);
        return CollectionUtils.convertMap(list, PostRespDTO::getId);
    }

}
