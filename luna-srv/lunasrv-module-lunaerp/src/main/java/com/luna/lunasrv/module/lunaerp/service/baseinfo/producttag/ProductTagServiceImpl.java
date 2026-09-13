package com.luna.lunasrv.module.lunaerp.service.baseinfo.producttag;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductTagRefVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.producttag.vo.ProductTagSaveReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.producttag.ProductTagDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.producttag.ProductTagRelDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.producttag.ProductTagMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.producttag.ProductTagRelMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;

/**
 * 基本信息-产品标签 Service 实现类
 *
 * 约束移植自 oldserver/src/lib/product-tags.ts 与 services/product-tags.service.ts：
 * 1. 颜色只开放 8 个预设键，缺省 graphite；
 * 2. 标签名去空白、内部连续空白压成一个空格，按码点数计长，上限 8；
 * 3. 单个产品标签数上限 20。
 *
 * @author luna
 */
@Service
public class ProductTagServiceImpl implements ProductTagService {

    /**
     * 预设标签颜色
     */
    private static final Set<String> TAG_COLORS = new LinkedHashSet<>(Arrays.asList(
            "camel", "blue", "teal", "green", "orange", "pink", "purple", "graphite"));

    /**
     * 默认标签颜色
     */
    private static final String DEFAULT_COLOR = "graphite";

    /**
     * 标签名字数上限（按码点数）
     */
    private static final int NAME_MAX_LENGTH = 8;

    /**
     * 单个产品的标签数上限
     */
    private static final int PER_PRODUCT_MAX = 20;

    @Resource
    private ProductTagMapper productTagMapper;

    @Resource
    private ProductTagRelMapper productTagRelMapper;

    @Override
    public Long createTag(ProductTagSaveReqVO createReqVO) {
        String name = normalizeName(createReqVO.getName());
        if (name.isEmpty()) {
            throw exception(PRODUCT_TAG_NAME_EMPTY);
        }
        // 校验同租户名称唯一
        validateNameUnique(null, name);
        // 插入标签
        ProductTagDO tag = new ProductTagDO();
        tag.setName(name);
        tag.setColor(normalizeColor(createReqVO.getColor()));
        productTagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    public void updateTag(ProductTagSaveReqVO updateReqVO) {
        // 校验存在
        validateTagExists(updateReqVO.getId());
        String name = normalizeName(updateReqVO.getName());
        if (name.isEmpty()) {
            throw exception(PRODUCT_TAG_NAME_EMPTY);
        }
        // 校验同租户名称唯一（排除自身）
        validateNameUnique(updateReqVO.getId(), name);
        // 更新标签
        ProductTagDO updateObj = new ProductTagDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setName(name);
        updateObj.setColor(normalizeColor(updateReqVO.getColor()));
        productTagMapper.updateById(updateObj);
    }

    @Override
    public void deleteTag(Long id) {
        // 校验存在
        ProductTagDO tag = validateTagExists(id);
        // 删除标签定义
        productTagMapper.deleteById(id);
        // 级联逻辑删除该标签的全部产品关联（标签定义删除时关联无意义）
        productTagRelMapper.deleteByTagIds(Collections.singletonList(String.valueOf(tag.getId())));
    }

    @Override
    public ProductTagDO getTag(Long id) {
        return productTagMapper.selectById(id);
    }

    @Override
    public List<ProductTagDO> getTagList() {
        return productTagMapper.selectListOrderByCreateTimeDesc();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductTagRefVO> coUpdateProductTags(Long productId, List<ProductTagRefVO> tags) {
        String productIdStr = String.valueOf(productId);
        // 1. 解析入参：名称归一化、颜色校验、去重、数量上限
        List<ParsedTag> wanted = parseTagInputs(tags);

        // 2. 按 (租户ID, 名称) 批量查未删除标签（租户条件由框架自动追加）
        Map<String, ProductTagDO> existingMap = new HashMap<>();
        if (CollUtil.isNotEmpty(wanted)) {
            List<String> names = wanted.stream().map(ParsedTag::getName).collect(Collectors.toList());
            List<ProductTagDO> existingTags = productTagMapper.selectListByNames(names);
            for (ProductTagDO tag : existingTags) {
                existingMap.put(tag.getName(), tag);
            }
        }

        // 3. 已存在则复用（保留原色，不用传入色覆盖），不存在则新建（用传入色，缺省 graphite）
        List<ProductTagDO> resolved = new ArrayList<>(wanted.size());
        for (ParsedTag wantedTag : wanted) {
            ProductTagDO tag = existingMap.get(wantedTag.getName());
            if (tag == null) {
                tag = new ProductTagDO();
                tag.setName(wantedTag.getName());
                tag.setColor(wantedTag.getColor());
                productTagMapper.insert(tag);
            }
            resolved.add(tag);
        }

        // 4. 删除该产品的旧关联，按列表顺序插入新关联（sortOrder=下标）
        productTagRelMapper.deleteByProductIds(Collections.singletonList(productIdStr));
        List<ProductTagRefVO> result = new ArrayList<>(resolved.size());
        if (CollUtil.isNotEmpty(resolved)) {
            List<ProductTagRelDO> rels = new ArrayList<>(resolved.size());
            for (int i = 0; i < resolved.size(); i++) {
                ProductTagDO tag = resolved.get(i);
                ProductTagRelDO rel = new ProductTagRelDO();
                rel.setProductId(productIdStr);
                rel.setTagId(String.valueOf(tag.getId()));
                rel.setSortOrder(i);
                rels.add(rel);

                ProductTagRefVO ref = new ProductTagRefVO();
                ref.setId(tag.getId());
                ref.setName(tag.getName());
                ref.setColor(tag.getColor());
                ref.setSortOrder(i);
                result.add(ref);
            }
            productTagRelMapper.insertBatch(rels);
        }
        return result;
    }

    @Override
    public List<ProductTagRefVO> getProductTags(Long productId) {
        Map<Long, List<ProductTagRefVO>> map = getProductTagMap(Collections.singletonList(productId));
        return map.getOrDefault(productId, Collections.emptyList());
    }

    @Override
    public Map<Long, List<ProductTagRefVO>> getProductTagMap(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        // 查全部关联（按 sort_order 升序）
        List<String> productIdStrs = productIds.stream().map(String::valueOf).collect(Collectors.toList());
        List<ProductTagRelDO> rels = productTagRelMapper.selectListByProductIds(productIdStrs);
        if (CollUtil.isEmpty(rels)) {
            return Collections.emptyMap();
        }
        // 查关联到的标签定义（tag_id 历史数据可能为 UUID，仅取数字主键对应的标签）
        List<Long> tagIds = rels.stream()
                .map(ProductTagRelDO::getTagId)
                .map(ProductTagServiceImpl::parseLongOrNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, ProductTagDO> tagMap = new HashMap<>();
        if (CollUtil.isNotEmpty(tagIds)) {
            for (ProductTagDO tag : productTagMapper.selectByIds(tagIds)) {
                tagMap.put(tag.getId(), tag);
            }
        }
        // 组装：产品ID → 标签引用（保持关联的 sortOrder 顺序）
        Map<Long, List<ProductTagRefVO>> result = new HashMap<>();
        for (ProductTagRelDO rel : rels) {
            Long pid = parseLongOrNull(rel.getProductId());
            Long tagId = parseLongOrNull(rel.getTagId());
            if (pid == null || tagId == null) {
                continue;
            }
            ProductTagDO tag = tagMap.get(tagId);
            if (tag == null) {
                continue;
            }
            ProductTagRefVO ref = BeanUtils.toBean(tag, ProductTagRefVO.class);
            ref.setSortOrder(rel.getSortOrder());
            result.computeIfAbsent(pid, k -> new ArrayList<>()).add(ref);
        }
        return result;
    }

    private ProductTagDO validateTagExists(Long id) {
        ProductTagDO tag = productTagMapper.selectById(id);
        if (tag == null) {
            throw exception(PRODUCT_TAG_NOT_EXISTS);
        }
        return tag;
    }

    private void validateNameUnique(Long id, String name) {
        ProductTagDO tag = productTagMapper.selectByName(name);
        if (tag != null && !tag.getId().equals(id)) {
            throw exception(PRODUCT_TAG_NAME_DUPLICATE, name);
        }
    }

    /**
     * 解析产品标签入参，移植自 oldserver parseProductTagInputs：
     * 名称去首尾空白并压缩内部连续空白；颜色缺省 graphite 且必须为预设色；
     * 同名去重（保留首次出现顺序）；数量超过 20 报错。
     */
    private List<ParsedTag> parseTagInputs(List<ProductTagRefVO> rawList) {
        if (rawList == null) {
            return Collections.emptyList();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<ParsedTag> wanted = new ArrayList<>();
        for (ProductTagRefVO item : rawList) {
            String name = item == null ? "" : normalizeName(item.getName());
            if (name.isEmpty()) {
                // VO 上已有 @NotBlank 校验，此处为 service 内被直接调用时的兜底
                throw exception(PRODUCT_TAG_NAME_EMPTY);
            }
            if (name.codePointCount(0, name.length()) > NAME_MAX_LENGTH) {
                throw exception(PRODUCT_TAG_NAME_TOO_LONG);
            }
            String color = normalizeColor(item.getColor());
            if (!seen.add(name)) {
                continue;
            }
            wanted.add(new ParsedTag(name, color));
        }
        if (wanted.size() > PER_PRODUCT_MAX) {
            throw exception(PRODUCT_TAG_LIMIT_EXCEEDED, wanted.size());
        }
        return wanted;
    }

    /**
     * 去首尾空白并把内部连续空白压成一个空格，移植自 normalizeProductTagName。
     */
    private static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    /**
     * 颜色缺省 graphite，并校验必须为预设色，移植自 isValidProductTagColor。
     */
    private static String normalizeColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return DEFAULT_COLOR;
        }
        String normalized = color.trim();
        if (!TAG_COLORS.contains(normalized)) {
            throw exception(PRODUCT_TAG_COLOR_INVALID);
        }
        return normalized;
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析后的标签入参（名称归一化、颜色归一化）
     */
    private static class ParsedTag {

        private final String name;
        private final String color;

        ParsedTag(String name, String color) {
            this.name = name;
            this.color = color;
        }

        String getName() {
            return name;
        }

        String getColor() {
            return color;
        }

    }

}
