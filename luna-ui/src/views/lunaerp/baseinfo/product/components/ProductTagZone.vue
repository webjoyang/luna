<template>
  <div class="tag-zone">
    <!-- 已选标签：彩色胶囊 -->
    <div v-if="selected.length" class="zone-row">
      <span
        v-for="(tag, index) in selected"
        :key="tag.id ?? tag.name + index"
        class="tag-chip"
        :style="chipStyle(tag.color)"
      >
        <i class="tag-dot" :style="{ background: resolveTagSwatch(tag.color).ink }" />
        {{ tag.name }}
        <Icon
          icon="ep:close"
          :size="11"
          class="tag-remove"
          @click="removeTag(index)"
        />
      </span>
    </div>
    <div v-else class="zone-empty">暂未设置标签，可从标签库勾选或新建</div>

    <!-- 标签库：勾选已有标签 -->
    <div v-if="library.length" class="zone-row zone-library">
      <span class="zone-label">标签库</span>
      <button
        v-for="tag in library"
        :key="tag.id ?? tag.name"
        type="button"
        class="tag-chip tag-library"
        :class="{ 'is-selected': isSelected(tag.name) }"
        :style="chipStyle(tag.color)"
        :disabled="isSelected(tag.name)"
        @click="addExisting(tag)"
      >
        <i class="tag-dot" :style="{ background: resolveTagSwatch(tag.color).ink }" />
        {{ tag.name }}
        <Icon v-if="!isSelected(tag.name)" icon="ep:plus" :size="11" />
      </button>
    </div>

    <!-- 新建标签：命名 + 预设色板（默认石墨，同名锁库内颜色） -->
    <div class="zone-create">
      <el-input
        v-model="draftName"
        class="create-input"
        :maxlength="PRODUCT_TAG_NAME_MAX"
        placeholder="输入新标签名（最多 8 个字）"
        @keyup.enter="addNew"
      />
      <div class="color-swatches">
        <button
          v-for="swatch in PRODUCT_TAG_SWATCHES"
          :key="swatch.key"
          type="button"
          class="swatch"
          :class="{
            'is-active': effectiveColor === swatch.key,
            'is-locked': !!libraryMatch
          }"
          :style="{ background: swatch.ink }"
          :title="swatch.label"
          :disabled="!!libraryMatch"
          @click="draftColor = swatch.key"
        />
      </div>
      <el-button size="default" @click="addNew">
        <Icon icon="ep:plus" class="mr-3px" />新建标签
      </el-button>
    </div>
    <p v-if="libraryMatch" class="zone-hint">
      「{{ libraryMatch.name }}」已存在，将沿用标签库中的颜色
    </p>
  </div>
</template>

<script lang="ts" setup>
import * as ProductTagApi from '@/api/lunaerp/baseinfo/product-tag'
import type { ProductTagVO } from '@/api/lunaerp/baseinfo/product'
import {
  PRODUCT_TAG_NAME_MAX,
  PRODUCT_TAG_PER_PRODUCT_MAX,
  PRODUCT_TAG_PALETTE,
  PRODUCT_TAG_SWATCHES,
  PRODUCT_TAG_DEFAULT_COLOR,
  normalizeTagName,
  resolveTagSwatch,
  type ProductTagColorKey
} from '../product-tag-palette'

defineOptions({ name: 'ProductTagZone' })

type ZoneTag = {
  id?: number
  name: string
  color: string
}

const props = defineProps({
  modelValue: {
    type: Array as PropType<ZoneTag[]>,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const message = ElMessage

const selected = ref<ZoneTag[]>([...(props.modelValue || [])])
const library = ref<ProductTagVO[]>([])
const draftName = ref('')
const draftColor = ref<ProductTagColorKey>(PRODUCT_TAG_DEFAULT_COLOR)

/** 弹窗重新打开/回显时，同步外部值 */
watch(
  () => props.modelValue,
  (val) => {
    selected.value = [...(val || [])]
  },
  { deep: true }
)

/** 挂载时获取租户全部标签 */
const loadLibrary = async () => {
  try {
    const data = await ProductTagApi.getProductTagList()
    library.value = Array.isArray(data) ? data : data?.list ?? []
  } catch {
    library.value = []
  }
}
onMounted(loadLibrary)

const emitChange = () => {
  emit('update:modelValue', [...selected.value])
}

const isSelected = (name: string) =>
  selected.value.some((tag) => tag.name === name)

/** 库里同名标签：颜色以库内为准（同名全租户同色） */
const libraryMatch = computed<ProductTagVO | null>(() => {
  const name = normalizeTagName(draftName.value)
  if (!name) return null
  return library.value.find((tag) => tag.name === name) ?? null
})
const effectiveColor = computed<string>(
  () => libraryMatch.value?.color || draftColor.value
)

const chipStyle = (color?: string) => {
  const swatch = resolveTagSwatch(color)
  return {
    color: swatch.ink,
    background: swatch.tint,
    borderColor: swatch.ring
  }
}

/** 勾选标签库中的已有标签 */
const addExisting = (tag: ProductTagVO) => {
  if (selected.value.length >= PRODUCT_TAG_PER_PRODUCT_MAX) {
    message.warning(`单个产品最多添加 ${PRODUCT_TAG_PER_PRODUCT_MAX} 个标签`)
    return
  }
  if (isSelected(tag.name)) return
  selected.value.push({ id: tag.id, name: tag.name, color: tag.color })
  emitChange()
}

/** 新建标签（后端在产品保存时负责自动建档） */
const addNew = () => {
  const name = normalizeTagName(draftName.value)
  if (!name) {
    message.warning('请输入标签名称')
    return
  }
  if (selected.value.length >= PRODUCT_TAG_PER_PRODUCT_MAX) {
    message.warning(`单个产品最多添加 ${PRODUCT_TAG_PER_PRODUCT_MAX} 个标签`)
    return
  }
  if (isSelected(name)) {
    message.warning('该标签已添加')
    return
  }
  const color = libraryMatch.value?.color || draftColor.value
  selected.value.push({
    id: libraryMatch.value?.id,
    name,
    color: PRODUCT_TAG_PALETTE[color as ProductTagColorKey]?.key ? color : PRODUCT_TAG_DEFAULT_COLOR
  })
  emitChange()
  draftName.value = ''
  draftColor.value = PRODUCT_TAG_DEFAULT_COLOR
}

const removeTag = (index: number) => {
  selected.value.splice(index, 1)
  emitChange()
}
</script>

<style lang="scss" scoped>
.tag-zone {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.zone-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.zone-empty {
  font-size: 12.5px;
  color: var(--el-text-color-secondary);
}

.zone-library {
  align-items: center;
  padding-top: 8px;
  border-top: 1px dashed var(--el-border-color-lighter);
}

.zone-label {
  flex: none;
  margin-right: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
  cursor: default;

  .tag-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  .tag-remove {
    margin-left: 2px;
    border-radius: 50%;
    cursor: pointer;
    opacity: 0.65;
    transition: all 0.2s ease;

    &:hover {
      opacity: 1;
      background: currentColor;
      color: #fff;
    }
  }
}

.tag-library {
  cursor: pointer;
  opacity: 0.75;
  transition: all 0.2s ease;

  &:hover {
    opacity: 1;
  }

  &.is-selected {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.zone-create {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.create-input {
  width: 220px;
}

.color-swatches {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.swatch {
  width: 20px;
  height: 20px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
  transition: transform 0.15s ease;

  &:hover {
    transform: scale(1.15);
  }

  &.is-active {
    border-color: var(--el-bg-color);
    box-shadow:
      0 0 0 2px var(--el-text-color-primary),
      inset 0 0 0 1px rgba(0, 0, 0, 0.08);
  }

  &.is-locked {
    cursor: not-allowed;
    opacity: 0.55;
  }
}

.zone-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-color-warning);
}
</style>
