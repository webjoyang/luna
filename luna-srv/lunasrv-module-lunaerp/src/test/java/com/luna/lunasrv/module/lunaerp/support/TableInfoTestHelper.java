package com.luna.lunasrv.module.lunaerp.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 纯 Mockito 单元测试下的 MyBatis-Plus TableInfo / Lambda 缓存预热辅助工具
 * <p>
 * 遵循 docs/agents/coding-standards.md §10：
 * 在未启动 Spring 容器与 MyBatis-Plus 自动扫描的纯 Mockito 单元测试中，
 * 凡被测类构造了 new LambdaQueryWrapper<DO>() 或引用了实体方法（如 AccountDO::getId），
 * 必须注入 TableInfo 缓存，否则会抛出 MybatisPlusException: can not find lambda cache for this entity。
 *
 * @author luna
 */
public final class TableInfoTestHelper {

    private static final Set<Class<?>> INITIALIZED_CLASSES = ConcurrentHashMap.newKeySet();
    private static final MapperBuilderAssistant ASSISTANT = new MapperBuilderAssistant(new MybatisConfiguration(), "");

    private TableInfoTestHelper() {}

    /**
     * 为指定实体类预热注入 TableInfo 缓存（线程安全，幂等）
     *
     * @param entityClasses 待预热的 MyBatis-Plus 实体类
     */
    public static void initTableInfo(Class<?>... entityClasses) {
        if (entityClasses == null) {
            return;
        }
        for (Class<?> clazz : entityClasses) {
            if (clazz != null && !INITIALIZED_CLASSES.contains(clazz)) {
                synchronized (TableInfoTestHelper.class) {
                    if (!INITIALIZED_CLASSES.contains(clazz)) {
                        TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
                        if (tableInfo == null) {
                            TableInfoHelper.initTableInfo(ASSISTANT, clazz);
                        }
                        INITIALIZED_CLASSES.add(clazz);
                    }
                }
            }
        }
    }
}
