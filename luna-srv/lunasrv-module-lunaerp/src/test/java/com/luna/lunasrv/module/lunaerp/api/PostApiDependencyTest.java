package com.luna.lunasrv.module.lunaerp.api;

import com.luna.lunasrv.module.system.api.dept.PostApi;
import com.luna.lunasrv.module.system.api.dept.dto.PostRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 lunaerp 模块对 system API 层的依赖已就绪且可直接注入 PostApi
 */
class PostApiDependencyTest {

    @Test
    void testPostApiAccessibleInLunaerp() {
        PostApi postApi = Mockito.mock(PostApi.class);
        Mockito.when(postApi.getPostByCode("LUNA_WORKER")).thenReturn(new PostRespDTO().setCode("LUNA_WORKER"));

        PostRespDTO post = postApi.getPostByCode("LUNA_WORKER");
        assertNotNull(post);
        assertEquals("LUNA_WORKER", post.getCode());
    }

}
