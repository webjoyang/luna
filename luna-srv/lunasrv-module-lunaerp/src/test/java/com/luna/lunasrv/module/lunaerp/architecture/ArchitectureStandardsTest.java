package com.luna.lunasrv.module.lunaerp.architecture;

import cn.hutool.core.util.ClassUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luna.lunasrv.module.lunaerp.common.desensitize.Sensitive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
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

    @Test
    @DisplayName("规约审查：finance 域 RespVO 中 BigDecimal 金额/单价/费用字段必须标注 @Sensitive")
    public void testFinanceResponseVOsHaveSensitiveAnnotationsOnAmounts() {
        Set<Class<?>> voClasses = ClassUtil.scanPackage("com.luna.lunasrv.module.lunaerp.controller.finance");
        assertTrue(!voClasses.isEmpty(), "未扫描到 finance 域的 Controller/VO 类");

        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : voClasses) {
            if (!clazz.getSimpleName().endsWith("RespVO") || clazz.getSimpleName().contains("Print")) {
                continue;
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (BigDecimal.class.isAssignableFrom(field.getType())) {
                    String name = field.getName().toLowerCase();
                    if (name.contains("amount") || name.contains("price") || name.contains("fee") || name.contains("cost") || name.contains("balance")) {
                        if (!field.isAnnotationPresent(Sensitive.class)) {
                            violations.add(clazz.getSimpleName() + "." + field.getName() + " 缺失 @Sensitive 序列化脱敏注解");
                        }
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "检测到财务响应 VO 金额类字段缺失脱敏注解（违反敏感字段安全规约）:\n"
                        + String.join("\n", violations));
    }

    @Test
    @DisplayName("规约审查：finance 域 Service 内部批量快照方法必须遵循 batch* 前缀规范 (coding-standards.md §2)")
    public void testFinanceServiceBatchSnapshotMethodsConvention() {
        Set<Class<?>> serviceClasses = ClassUtil.scanPackage("com.luna.lunasrv.module.lunaerp.service.finance");
        assertTrue(!serviceClasses.isEmpty(), "未扫描到 finance 域的 Service 类");

        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : serviceClasses) {
            if (clazz.isInterface()) {
                continue;
            }
            for (Method method : clazz.getDeclaredMethods()) {
                // 仅约束 Service 内部非 public 批量快照辅助方法
                if (java.lang.reflect.Modifier.isPrivate(method.getModifiers())
                        && java.util.Map.class.isAssignableFrom(method.getReturnType())) {
                    boolean hasCollectionParam = false;
                    for (Class<?> pType : method.getParameterTypes()) {
                        if (java.util.Collection.class.isAssignableFrom(pType)) {
                            hasCollectionParam = true;
                            break;
                        }
                    }
                    if (hasCollectionParam && !method.getName().startsWith("batch")) {
                        violations.add(clazz.getSimpleName() + "." + method.getName() + "() 返回 Map 且接收集合参数，必须遵循 coding-standards.md §2 命名为 batch* 前缀");
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "检测到内部批量快照方法命名不符合规范（应使用 batch* 显式标识批量抽取）:\n"
                        + String.join("\n", violations));
    }

    @Test
    @DisplayName("规约审查：finance 域 Service 严禁在 LambdaQueryWrapperX 主链路上对 ID 集合使用原生 .in(...) (应使用 inOrEmpty 防全表扫描，coding-standards.md §1)")
    public void testFinanceServicesHaveNoRawInOnIdCollections() {
        java.io.File dir = new java.io.File("src/main/java/com/luna/lunasrv/module/lunaerp/service/finance");
        if (!dir.exists()) {
            dir = new java.io.File("luna-srv/lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/service/finance");
        }
        if (!dir.exists()) {
            dir = new java.io.File("lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/service/finance");
        }
        if (!dir.exists()) {
            return; // 外部打包环境跳过源码级静态扫描
        }

        List<java.io.File> javaFiles = new ArrayList<>();
        collectJavaFiles(dir, javaFiles);

        List<String> violations = new ArrayList<>();
        for (java.io.File file : javaFiles) {
            List<String> lines;
            try {
                lines = java.nio.file.Files.readAllLines(file.toPath());
            } catch (Exception e) {
                continue;
            }
            int lambdaBraceLevel = -1;
            int currentBraceLevel = 0;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
                    continue;
                }
                // 标记进入嵌套 Consumer 块（如 .and(w -> { 或 or(sub -> {）
                if (line.contains("-> {")) {
                    lambdaBraceLevel = currentBraceLevel + 1;
                }
                for (char c : line.toCharArray()) {
                    if (c == '{') {
                        currentBraceLevel++;
                    } else if (c == '}') {
                        currentBraceLevel--;
                        if (lambdaBraceLevel > 0 && currentBraceLevel < lambdaBraceLevel) {
                            lambdaBraceLevel = -1;
                        }
                    }
                }
                // 仅在主链（非嵌套 Consumer 块内部）检查 ID 集合是否使用了原生 .in(...)
                if (lambdaBraceLevel == -1) {
                    if (line.contains(".in(") && (line.contains("Id,") || line.contains("Ids,") || line.contains("Id)") || line.contains("Ids)") || line.contains("::getId") || line.contains("::getSourceRefId") || line.contains("::getRefId"))) {
                        violations.add(file.getName() + ":" + (i + 1) + " [" + line + "]");
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "检测到 finance 域 Service 主链路对 ID 集合使用了原生 .in(...)（违反 coding-standards.md §1，必须改用 .inOrEmpty(...) 保证空集 1=0 安全短路）:\n"
                        + String.join("\n", violations));
    }

    private void collectJavaFiles(java.io.File dir, List<java.io.File> result) {
        java.io.File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (java.io.File file : files) {
            if (file.isDirectory()) {
                collectJavaFiles(file, result);
            } else if (file.getName().endsWith(".java")) {
                result.add(file);
            }
        }
    }

}
