package com.luna.lunasrv.module.system.api.dept;

import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.system.api.dept.dto.PostRespDTO;
import com.luna.lunasrv.module.system.dal.dataobject.dept.PostDO;
import com.luna.lunasrv.module.system.service.dept.PostService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 岗位 API 实现类
 *
 * @author luna
 */
@Service
public class PostApiImpl implements PostApi {

    @Resource
    private PostService postService;

    @Override
    public void validPostList(Collection<Long> ids) {
        postService.validatePostList(ids);
    }

    @Override
    public List<PostRespDTO> getPostList(Collection<Long> ids) {
        List<PostDO> list = postService.getPostList(ids);
        return BeanUtils.toBean(list, PostRespDTO.class);
    }

    @Override
    public PostRespDTO getPostByCode(String code) {
        PostDO post = postService.getPostByCode(code);
        return BeanUtils.toBean(post, PostRespDTO.class);
    }

}
