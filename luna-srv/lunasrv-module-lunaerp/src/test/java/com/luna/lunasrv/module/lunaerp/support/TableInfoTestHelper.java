package com.luna.lunasrv.module.lunaerp.support;

import cn.hutool.core.util.ClassUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.lang.reflect.Modifier;
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
    private static volatile boolean ALL_ENTITIES_INITIALIZED = false;

    static {
        // 类加载时自动扫描并预热 lunaerp 下全部 DO 实体
        try {
            initAllEntities();
        } catch (Throwable ignored) {
            // 避免类初始化被非预期环境异常阻断
        }
    }

    private TableInfoTestHelper() {}

    /**
     * 一键自动扫描并预热 lunaerp 模块下所有的 DO 实体类（线程安全，幂等）
     */
    public static void initAllEntities() {
        if (ALL_ENTITIES_INITIALIZED) {
            return;
        }
        synchronized (TableInfoTestHelper.class) {
            if (ALL_ENTITIES_INITIALIZED) {
                return;
            }
            Set<Class<?>> doClasses = ClassUtil.scanPackage("com.luna.lunasrv.module.lunaerp.dal.dataobject",
                    clazz -> !clazz.isInterface()
                            && !clazz.isEnum()
                            && !clazz.isAnnotation()
                            && !Modifier.isAbstract(clazz.getModifiers())
                            && (clazz.isAnnotationPresent(TableName.class) || clazz.getSimpleName().endsWith("DO")));
            for (Class<?> clazz : doClasses) {
                initSingleTableInfo(clazz);
            }
            ALL_ENTITIES_INITIALIZED = true;
        }
    }

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
            if (clazz != null) {
                initSingleTableInfo(clazz);
            }
        }
    }

    private static void initSingleTableInfo(Class<?> clazz) {
        if (INITIALIZED_CLASSES.contains(clazz)) {
            return;
        }
        synchronized (TableInfoTestHelper.class) {
            if (!INITIALIZED_CLASSES.contains(clazz)) {
                try {
                    TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
                    if (tableInfo == null) {
                        TableInfoHelper.initTableInfo(ASSISTANT, clazz);
                    }
                    INITIALIZED_CLASSES.add(clazz);
                } catch (Throwable ignored) {
                    // 忽略某些无主键或非标准实体的异常
                }
            }
        }
    }
}
