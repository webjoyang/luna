package com.luna.lunasrv.module.system.api.dept;

import com.luna.lunasrv.framework.common.enums.CommonStatusEnum;
import com.luna.lunasrv.module.system.api.dept.dto.PostRespDTO;
import com.luna.lunasrv.module.system.dal.dataobject.dept.PostDO;
import com.luna.lunasrv.module.system.service.dept.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link PostApiImpl} 的单元测试
 */
class PostApiImplTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostApiImpl postApi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetPostByCode_success() {
        // 准备数据
        String code = "LUNA_WORKER";
        PostDO mockPost = new PostDO();
        mockPost.setId(100L);
        mockPost.setName("挡车工");
        mockPost.setCode(code);
        mockPost.setSort(1);
        mockPost.setStatus(CommonStatusEnum.ENABLE.getStatus());

        when(postService.getPostByCode(code)).thenReturn(mockPost);

        // 调用
        PostRespDTO result = postApi.getPostByCode(code);

        // 断言
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("挡车工", result.getName());
        assertEquals(code, result.getCode());
        assertEquals(1, result.getSort());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), result.getStatus());
        verify(postService, times(1)).getPostByCode(code);
    }

    @Test
    void testGetPostByCode_notFound() {
        // 准备数据
        String code = "NON_EXIST_CODE";
        when(postService.getPostByCode(code)).thenReturn(null);

        // 调用
        PostRespDTO result = postApi.getPostByCode(code);

        // 断言：岗位不存在时返回 null，不抛异常
        assertNull(result);
        verify(postService, times(1)).getPostByCode(code);
    }

}
