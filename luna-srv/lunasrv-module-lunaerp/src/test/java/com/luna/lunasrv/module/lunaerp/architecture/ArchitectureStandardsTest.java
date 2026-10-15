package com.luna.lunasrv.module.lunaerp.architecture;

import cn.hutool.core.util.ClassUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 架构规范与代码纯洁性自动化测试 (Architecture Standards Test)
 * <p>
 * 遵循 docs/agents/coding-standards.md：
 * 1. DO 实体必须与数据库物理表严格镜像对齐，严禁使用 @TableField(exist = false) 放置非持久化/展示字段。
 * 2. Mapper 接口必须保持纯洁，严禁定义包含业务逻辑或防御分支的 default 方法。
 */
public class ArchitectureStandardsTest {

    @Test
    @DisplayName("规约审查：finance 域 DO 严禁包含 @TableField(exist = false) 非持久化字段")
    public void testFinanceDataObjectsHaveNoUnpersistedFields() {
        Set<Class<?>> doClasses = ClassUtil.scanPackage("com.luna.lunasrv.module.lunaerp.dal.dataobject.finance");
        assertTrue(!doClasses.isEmpty(), "未扫描到 finance 域的 DO 类");

        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : doClasses) {
            if (clazz.isInterface() || clazz.isEnum()) {
                continue;
            }
            for (Field field : clazz.getDeclaredFields()) {
                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField != null && !tableField.exist()) {
                    violations.add(clazz.getSimpleName() + "." + field.getName() + " 使用了 @TableField(exist = false)");
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "检测到 DO 包含未落库的非持久化字段（应移至 VO 或内存上下文），违反 coding-standards.md §3:\n"
                        + String.join("\n", violations));
    }

    @Test
    @DisplayName("规约审查：finance 域 Mapper 严禁定义 default 包装方法")
    public void testFinanceMappersHaveNoDefaultMethods() {
        Set<Class<?>> mapperClasses = ClassUtil.scanPackage("com.luna.lunasrv.module.lunaerp.dal.mysql.finance");
        assertTrue(!mapperClasses.isEmpty(), "未扫描到 finance 域的 Mapper 接口");

        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : mapperClasses) {
            if (!clazz.isInterface()) {
                continue;
            }
            assertTrue(BaseMapper.class.isAssignableFrom(clazz), clazz.getSimpleName() + " 必须继承 BaseMapper");

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isDefault()) {
                    violations.add(clazz.getSimpleName() + "." + method.getName() + "() 是 default 方法");
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "检测到 Mapper 接口定义了 default 方法（业务分支与防御一律下沉至 Service），违反 coding-standards.md §4:\n"
                        + String.join("\n", violations));
    }

}
