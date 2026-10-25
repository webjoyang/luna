package com.luna.lunasrv.module.lunaerp.service.baseinfo.product;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.framework.dict.core.DictFrameworkUtils;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportHelper;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ExcelImportResult;
import com.luna.lunasrv.module.lunaerp.common.dataimport.ImportRow;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductImageVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductImportVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductPageReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductRespVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductSaveReqVO;
import com.luna.lunasrv.module.lunaerp.controller.baseinfo.product.vo.ProductTagRefVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductImageDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductImageMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.producttag.ProductTagRelMapper;
import com.luna.lunasrv.module.lunaerp.service.baseinfo.producttag.ProductTagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PRODUCT_CODE_DUPLICATE;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;

/**
 * 基本信息-产品信息 Service 实现类
 *
 * 查询语义移植自 oldserver/src/routes/products.ts 的 GET / 列表与 GET /next-code，
 * 写操作语义参考 oldserver/src/services/product-create.service.ts。
 *
 * @author luna
 */
@Service
public class ProductServiceImpl implements ProductService {

    /**
     * 默认分页条数
     */
    private static final int DEFAULT_PAGE_SIZE = 20;
    /**
     * 分页条数上限
     */
    private static final int MAX_PAGE_SIZE = 100;
    /**
     * 产品默认状态
     */
    private static final String DEFAULT_STATUS = "ACTIVE";
    /**
     * 删除状态
     */
    private static final String STATUS_DELETED = "DELETED";
    /**
     * 草稿状态
     */
    private static final String STATUS_DRAFT = "DRAFT";
    /**
     * 默认编号前缀
     */
    private static final String DEFAULT_CODE_PREFIX = "P";
    /**
     * 编号数字部分最小补零位数
     */
    private static final int CODE_MIN_DIGITS = 4;
    /**
     * 单值克重查询的上下浮动量（g/m²）
     */
    private static final BigDecimal WEIGHT_TOLERANCE = BigDecimal.TEN;

    /**
     * 智能搜索命中的五字段（code/name/supplier_code/composition/type）
     */
    private static final String[] SEARCH_COLUMNS = {"code", "name", "supplier_code", "composition", "type"};
    /**
     * 无分词结果时的兜底四字段（不含 type）
     */
    private static final String[] SEARCH_FALLBACK_COLUMNS = {"code", "name", "supplier_code", "composition"};

    /**
     * 搜索词初始分隔符：空格、逗号、顿号、加号、斜杠
     */
    private static final Pattern SEARCH_SPLIT_PATTERN = Pattern.compile("[\\s,，、+/]+");
    /**
     * 成分分词分隔符：空白、斜杠、英文逗号、中文逗号、顿号、加号
     */
    private static final Pattern COMPOSITION_SPLIT_PATTERN = Pattern.compile("[\\s/、,，+]+");
    /**
     * 编号前缀后的数字部分
     */
    private static final Pattern CODE_SUFFIX_NUMBER_PATTERN = Pattern.compile("^\\d+");

    /**
     * 面料核心词（作为连粘搜索词的二次切分边界），顺序与 oldserver tokenizeSearchQuery 保持一致
     */
    private static final List<String> TEXTILE_KEYWORDS = Arrays.asList(
            "莫代尔", "modal", "天丝", "tencel", "粘胶", "人棉", "人丝",
            "涤纶", "涤", "聚酯", "锦纶", "尼龙", "棉", "羊毛", "蚕丝", "真丝", "麻", "亚麻",
            "氨纶", "莱卡", "spandex", "lycra", "腈纶", "腈",
            "汗布", "双面", "螺纹", "罗纹", "毛圈", "卫衣", "平纹", "提花", "网眼", "珠地", "空气层",
            "单面", "双面布");
    private static final Pattern KEYWORD_SPLIT_PATTERN = Pattern.compile(
            "(" + TEXTILE_KEYWORDS.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductImageMapper productImageMapper;

    @Resource
    private ProductTagRelMapper productTagRelMapper;

    @Resource
    private ProductTagService productTagService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductSaveReqVO createReqVO) {
        validateProductNotMasked(createReqVO);
        // 校验同租户未删除产品编号唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 插入产品
        ProductDO product = BeanUtils.toBean(createReqVO, ProductDO.class);
        if (!StringUtils.hasText(product.getStatus())) {
            product.setStatus(DEFAULT_STATUS);
        }
        productMapper.insert(product);
        // 批量插入图片（回填 productId）
        saveProductImages(product.getId(), createReqVO.getImages());
        // 标签 co-update（与产品写操作同事务）
        if (createReqVO.getTags() != null) {
            productTagService.coUpdateProductTags(product.getId(), createReqVO.getTags());
        }
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductSaveReqVO updateReqVO) {
        // 校验存在
        validateProductExists(updateReqVO.getId());
        validateProductNotMasked(updateReqVO);
        // 校验编号唯一（排除自身）
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 更新主表
        ProductDO updateObj = BeanUtils.toBean(updateReqVO, ProductDO.class);
        productMapper.updateById(updateObj);
        // 图片整体替换：先按 productId 删旧再插新
        productImageMapper.deleteByProductIds(Collections.singletonList(String.valueOf(updateReqVO.getId())));
        saveProductImages(updateReqVO.getId(), updateReqVO.getImages());
        // 标签整体替换（入参未带 tags 时保持原样，兼容只更新产品主信息的调用）
        if (updateReqVO.getTags() != null) {
            productTagService.coUpdateProductTags(updateReqVO.getId(), updateReqVO.getTags());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long id) {
        // 校验存在
        validateProductExists(id);
        doDelete(Collections.singletonList(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductList(List<Long> ids) {
        doDelete(ids);
    }

    @Override
    public PageResult<ProductRespVO> getProductPage(ProductPageReqVO reqVO) {
        // pageSize 默认 20、上限 100
        clampPageSize(reqVO);
        // 构造查询条件
        LambdaQueryWrapperX<ProductDO> wrapper = buildPageWrapper(reqVO);
        wrapper.orderByDesc(ProductDO::getCreateTime);
        // 执行查询
        PageResult<ProductDO> pageResult = productMapper.selectPage(reqVO, wrapper);
        PageResult<ProductRespVO> respPage = BeanUtils.toBean(pageResult, ProductRespVO.class);
        enrichProducts(respPage.getList());
        return respPage;
    }

    @Override
    public ProductRespVO getProduct(Long id) {
        ProductDO product = productMapper.selectById(id);
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        ProductRespVO respVO = BeanUtils.toBean(product, ProductRespVO.class);
        enrichProducts(Collections.singletonList(respVO));
        return respVO;
    }

    @Override
    public ProductRespVO getProductByCode(String code) {
        ProductDO product = productMapper.selectByCode(code);
        if (product == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
        ProductRespVO respVO = BeanUtils.toBean(product, ProductRespVO.class);
        enrichProducts(Collections.singletonList(respVO));
        return respVO;
    }

    @Override
    public String getNextCode(String prefix) {
        String codePrefix = StringUtils.hasText(prefix) ? prefix.trim() : DEFAULT_CODE_PREFIX;
        // 当前租户同前缀、状态非 DELETED 的产品（逻辑删除与租户条件由框架自动追加）
        List<ProductDO> candidates = productMapper.selectListByCodePrefixAndNotDeleted(codePrefix);
        // 字符串排序会取错最大值（如 P9 > P10），按前缀后的数字部分取 max
        long maxNum = 0L;
        int maxDigits = CODE_MIN_DIGITS;
        for (ProductDO candidate : candidates) {
            String code = candidate.getCode();
            if (code == null || code.length() <= codePrefix.length()) {
                continue;
            }
            String suffix = code.substring(codePrefix.length());
            Matcher matcher = CODE_SUFFIX_NUMBER_PATTERN.matcher(suffix);
            if (!matcher.find()) {
                continue;
            }
            String digits = matcher.group();
            long num = Long.parseLong(digits);
            if (num > maxNum) {
                maxNum = num;
                maxDigits = Math.max(CODE_MIN_DIGITS, digits.length());
            }
        }
        if (maxNum == 0L) {
            return codePrefix + "0001";
        }
        return codePrefix + String.format("%0" + maxDigits + "d", maxNum + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ProductTagRefVO> updateProductTags(Long id, List<ProductTagRefVO> tags) {
        // 校验存在
        validateProductExists(id);
        return productTagService.coUpdateProductTags(id, tags);
    }

    // ==================== 私有方法 ====================

    private void doDelete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 仅处理当前租户下存在的产品
        List<ProductDO> products = productMapper.selectByIds(ids);
        if (CollUtil.isEmpty(products)) {
            return;
        }
        // 1. 先置 status='DELETED'
        //    必须基于已查出的完整实体更新：ProductDO 有若干列声明了 FieldStrategy.ALWAYS，
        //    若用只带 id/status 的局部 DO，这些列会被一并刷成 null
        for (ProductDO product : products) {
            product.setStatus(STATUS_DELETED);
            productMapper.updateById(product);
        }
        // 2. 逻辑删除产品
        productMapper.deleteByIds(ids);
        // 3. 级联逻辑删除图片与标签关联（标签定义保留）
        List<String> productIdStrs = products.stream()
                .map(product -> String.valueOf(product.getId())).collect(Collectors.toList());
        productImageMapper.deleteByProductIds(productIdStrs);
        productTagRelMapper.deleteByProductIds(productIdStrs);
    }

    /**
     * 批量保存产品图片（回填 productId，sortOrder 为空时置 0）
     */
    private void saveProductImages(Long productId, List<ProductImageVO> images) {
        if (CollUtil.isEmpty(images)) {
            return;
        }
        String productIdStr = String.valueOf(productId);
        List<ProductImageDO> imageDOList = new ArrayList<>(images.size());
        for (ProductImageVO image : images) {
            ProductImageDO imageDO = new ProductImageDO();
            imageDO.setProductId(productIdStr);
            imageDO.setUrl(image.getUrl());
            imageDO.setSortOrder(image.getSortOrder() != null ? image.getSortOrder() : 0);
            imageDOList.add(imageDO);
        }
        productImageMapper.insertBatch(imageDOList);
    }

    private void validateProductExists(Long id) {
        if (id == null || productMapper.selectById(id) == null) {
            throw exception(PRODUCT_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        ProductDO product = productMapper.selectByCode(code);
        if (product != null && !product.getId().equals(id)) {
            throw exception(PRODUCT_CODE_DUPLICATE);
        }
    }

    private void clampPageSize(ProductPageReqVO reqVO) {
        Integer pageSize = reqVO.getPageSize();
        if (pageSize == null || pageSize < 1) {
            reqVO.setPageSize(DEFAULT_PAGE_SIZE);
        } else if (pageSize > MAX_PAGE_SIZE) {
            reqVO.setPageSize(MAX_PAGE_SIZE);
        }
    }

    /**
     * 构造产品分页查询条件，语义移植自 oldserver GET /api/products：
     * 1. status=DRAFT 只查草稿，其余值一律查 ACTIVE；
     * 2. 有 search：分词后每个 token AND(OR 五字段 ILIKE)；无分词结果时退化为整词 OR 四字段；
     * 3. 无 search：code/name/type 单字段 ILIKE，composition 分词后每词 AND ILIKE，
     *    克重区间（minWeight/maxWeight；只传 weightGsm 时按 ±10）。
     */
    private LambdaQueryWrapperX<ProductDO> buildPageWrapper(ProductPageReqVO reqVO) {
        LambdaQueryWrapperX<ProductDO> wrapper = new LambdaQueryWrapperX<>();
        // 状态：草稿入口只允许查 DRAFT，其他值一律按 ACTIVE 处理（不允许查 DELETED/ARCHIVED）
        wrapper.eq(ProductDO::getStatus, STATUS_DRAFT.equals(reqVO.getStatus()) ? STATUS_DRAFT : DEFAULT_STATUS);

        String search = reqVO.getSearch() == null ? "" : reqVO.getSearch().trim();
        if (!search.isEmpty()) {
            List<String> tokens = tokenizeSearchQuery(search);
            if (!tokens.isEmpty()) {
                for (String token : tokens) {
                    wrapper.and(w -> appendIlikeOr(w, SEARCH_COLUMNS, token));
                }
            } else {
                wrapper.and(w -> appendIlikeOr(w, SEARCH_FALLBACK_COLUMNS, search));
            }
            return wrapper;
        }

        // 单字段模糊（PostgreSQL LIKE 区分大小写，统一用 LOWER(...) LIKE LOWER(...) 实现 ILIKE 语义）
        appendIlikeIfPresent(wrapper, "code", reqVO.getCode());
        appendIlikeIfPresent(wrapper, "name", reqVO.getName());
        appendIlikeIfPresent(wrapper, "type", reqVO.getType());
        // 成分：按分隔符分词后，每个词 AND 模糊
        if (StringUtils.hasText(reqVO.getComposition())) {
            String[] compTokens = COMPOSITION_SPLIT_PATTERN.split(reqVO.getComposition().trim());
            for (String compToken : compTokens) {
                if (StringUtils.hasText(compToken)) {
                    wrapper.apply("LOWER(composition) LIKE LOWER({0})", "%" + compToken.trim() + "%");
                }
            }
        }
        // 克重区间：minWeight/maxWeight 优先；只传 weightGsm 时按 ±10
        if (reqVO.getMinWeight() != null || reqVO.getMaxWeight() != null) {
            wrapper.geIfPresent(ProductDO::getWeightGsm, reqVO.getMinWeight());
            wrapper.leIfPresent(ProductDO::getWeightGsm, reqVO.getMaxWeight());
        } else if (reqVO.getWeightGsm() != null) {
            wrapper.ge(ProductDO::getWeightGsm, reqVO.getWeightGsm().subtract(WEIGHT_TOLERANCE));
            wrapper.le(ProductDO::getWeightGsm, reqVO.getWeightGsm().add(WEIGHT_TOLERANCE));
        }
        return wrapper;
    }

    /**
     * 拼接 (col1 ILIKE ? OR col2 ILIKE ? ...)。使用 {0} 占位符，参数走预编译，无注入风险。
     */
    private void appendIlikeOr(LambdaQueryWrapper<ProductDO> wrapper, String[] columns, String token) {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                wrapper.or();
            }
            wrapper.apply("LOWER(" + columns[i] + ") LIKE LOWER({0})", "%" + token + "%");
        }
    }

    private void appendIlikeIfPresent(LambdaQueryWrapperX<ProductDO> wrapper, String column, String value) {
        if (StringUtils.hasText(value)) {
            wrapper.apply("LOWER(" + column + ") LIKE LOWER({0})", "%" + value.trim() + "%");
        }
    }

    /**
     * 智能分词：先按空格/逗号/顿号/加号/斜杠切分，再以面料核心词作为边界二次切分，去重。
     * 移植自 oldserver/src/routes/products.ts tokenizeSearchQuery。
     */
    private static List<String> tokenizeSearchQuery(String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> finalTokens = new LinkedHashSet<>();
        for (String token : SEARCH_SPLIT_PATTERN.split(trimmed)) {
            if (token.isEmpty()) {
                continue;
            }
            // 以核心词切分（捕获组会保留核心词本身）
            for (String part : KEYWORD_SPLIT_PATTERN.split(token)) {
                if (part == null) {
                    continue;
                }
                String trimmedPart = part.trim();
                if (!trimmedPart.isEmpty()) {
                    finalTokens.add(trimmedPart);
                }
            }
        }
        return new ArrayList<>(finalTokens);
    }

    /**
     * 为产品列表填充图片（sort_order 升序）、标签（关联 sortOrder）、supplierCompany。
     */
    private void enrichProducts(List<ProductRespVO> products) {
        if (CollUtil.isEmpty(products)) {
            return;
        }
        List<Long> productIds = products.stream().map(ProductRespVO::getId).collect(Collectors.toList());
        List<String> productIdStrs = productIds.stream().map(String::valueOf).collect(Collectors.toList());

        // 图片：一次批量查询后按产品分组（SQL 已按 sort_order 升序，分组保持顺序）
        Map<String, List<ProductImageDO>> imageMap = new LinkedHashMap<>();
        List<ProductImageDO> images = productImageMapper.selectListByProductIds(productIdStrs);
        for (ProductImageDO image : images) {
            imageMap.computeIfAbsent(image.getProductId(), k -> new ArrayList<>()).add(image);
        }
        // 标签：通过 tag_rel 关联标签定义
        Map<Long, List<ProductTagRefVO>> tagMap = productTagService.getProductTagMap(productIds);

        for (ProductRespVO product : products) {
            String productIdStr = String.valueOf(product.getId());
            List<ProductImageDO> productImages = imageMap.getOrDefault(productIdStr, Collections.emptyList());
            product.setImages(BeanUtils.toBean(productImages, ProductImageVO.class));
            product.setTags(tagMap.getOrDefault(product.getId(), Collections.emptyList()));
            // TODO 供应商模块迁移后接入：按 supplierId 查询供应商公司名称
            product.setSupplierCompany(null);
        }
    }

    /**
     * 校验产品字段不含脱敏占位符，写接口不接受脱敏值入库
     */
    private void validateProductNotMasked(ProductSaveReqVO reqVO) {
        if (reqVO != null && reqVO.getCostItems() != null && reqVO.getCostItems().contains("****")) {
            throw exception(BAD_REQUEST, "成本项明细不能包含脱敏占位符");
        }
    }

    @Override
    public List<ProductImportVO> getImportTemplate() {
        return Collections.singletonList(
                ProductImportVO.builder()
                        .code("P0001")
                        .name("莫代尔汗布")
                        .type("FINISHED")
                        .fabricType("KNIT")
                        .fabricConstruction("KNIT")
                        .composition("95%莫代尔 5%氨纶")
                        .widthCm(new BigDecimal("160"))
                        .weightGsm(new BigDecimal("220"))
                        .color("黑色")
                        .craftType("丝光")
                        .machineType("大圆机")
                        .supplierCode("SUP-M01")
                        .standardLength("100")
                        .status("ACTIVE")
                        .remark("标准产品档案示例")
                        .build()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExcelImportResult importProduct(MultipartFile file) throws IOException {
        return ExcelImportHelper.importExcel(
                file,
                ProductImportVO.class,
                this::validateProductImportRows,
                validList -> {
                    List<ProductDO> doList = new ArrayList<>(validList.size());
                    for (ProductImportVO vo : validList) {
                        ProductDO product = BeanUtils.toBean(vo, ProductDO.class);
                        if (!StringUtils.hasText(product.getStatus())) {
                            product.setStatus(DEFAULT_STATUS);
                        }
                        if (StringUtils.hasText(product.getType())) {
                            product.setTypeLabel(safeParseDictLabel("lunaerp_product_category", product.getType()));
                        }
                        if (StringUtils.hasText(product.getFabricType())) {
                            product.setFabricTypeLabel(safeParseDictLabel("lunaerp_fabric_type", product.getFabricType()));
                        }
                        doList.add(product);
                    }
                    productMapper.insertBatch(doList);
                }
        );
    }

    /**
     * 逐行校验产品导入数据：
     * 1. 产品编号必填
     * 2. 产品名称必填
     * 3. 产品类别必填
     * 4. 成分必填
     * 5. 成品门幅必填
     * 6. 成品克重必填
     * 7. 文件内产品编号重复排查
     * 8. 数据库内产品编号查重（1_030_001_001 口径）
     */
    private void validateProductImportRows(List<ImportRow<ProductImportVO>> rows) {
        Set<String> seenCodes = new HashSet<>();
        Set<String> fileDuplicateCodes = new HashSet<>();
        List<String> queryCodes = new ArrayList<>();

        for (ImportRow<ProductImportVO> row : rows) {
            ProductImportVO vo = row.getData();
            String code = vo.getCode() != null ? vo.getCode().trim() : "";
            if (StrUtil.isBlank(code)) {
                row.addError("产品编号不能为空");
            } else {
                if (!seenCodes.add(code)) {
                    fileDuplicateCodes.add(code);
                }
                queryCodes.add(code);
            }

            String name = vo.getName() != null ? vo.getName().trim() : "";
            if (StrUtil.isBlank(name)) {
                row.addError("产品名称不能为空");
            }

            String type = vo.getType() != null ? vo.getType().trim() : "";
            if (StrUtil.isBlank(type)) {
                row.addError("产品类别不能为空");
            }

            String composition = vo.getComposition() != null ? vo.getComposition().trim() : "";
            if (StrUtil.isBlank(composition)) {
                row.addError("成分不能为空");
            }

            if (vo.getWidthCm() == null) {
                row.addError("成品门幅不能为空");
            }

            if (vo.getWeightGsm() == null) {
                row.addError("成品克重不能为空");
            }
        }

        // 文件内编号重复检查
        if (!fileDuplicateCodes.isEmpty()) {
            for (ImportRow<ProductImportVO> row : rows) {
                String code = row.getData().getCode() != null ? row.getData().getCode().trim() : "";
                if (fileDuplicateCodes.contains(code)) {
                    row.addError("产品编号在文件中重复");
                }
            }
        }

        // 数据库内编号查重（1_030_001_001 口径）
        if (!queryCodes.isEmpty()) {
            List<ProductDO> existingProducts = productMapper.selectListByCodes(queryCodes);
            if (CollUtil.isNotEmpty(existingProducts)) {
                Set<String> existingCodes = existingProducts.stream()
                        .map(ProductDO::getCode)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .collect(Collectors.toSet());
                for (ImportRow<ProductImportVO> row : rows) {
                    String code = row.getData().getCode() != null ? row.getData().getCode().trim() : "";
                    if (existingCodes.contains(code)) {
                        row.addError(PRODUCT_CODE_DUPLICATE.getMsg());
                    }
                }
            }
        }
    }

    private String safeParseDictLabel(String dictType, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return DictFrameworkUtils.parseDictDataLabel(dictType, value);
        } catch (Exception e) {
            return null;
        }
    }

}
