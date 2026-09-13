<template>
  <div v-loading="loading" class="product-grid-wrap">
    <div v-if="list.length > 0" class="product-grid">
      <motion.div
        v-for="(row, index) in list"
        :key="row.id"
        class="product-card"
        :initial="{ opacity: 0, y: 18 }"
        :animate="{ opacity: 1, y: 0 }"
        :transition="{
          duration: 0.38,
          delay: Math.min(index, 8) * 0.04,
          ease: EASE
        }"
      >
        <!-- 封面 -->
        <div class="card-cover">
          <img v-if="resolveCover(row)" :src="resolveCover(row)" :alt="row.name" />
          <div v-else class="cover-empty">
            <Icon icon="ep:picture" :size="42" />
          </div>

          <!-- 悬浮操作按钮 -->
          <div class="cover-actions">
            <el-button
              circle
              size="small"
              title="编辑"
              v-hasPermi="['lunaerp:baseinfo:product:update']"
              @click="emit('edit', row)"
            >
              <Icon icon="ep:edit" :size="14" />
            </el-button>
            <el-button
              circle
              size="small"
              type="danger"
              title="删除"
              v-hasPermi="['lunaerp:baseinfo:product:delete']"
              @click="emit('delete', row)"
            >
              <Icon icon="ep:delete" :size="14" />
            </el-button>
          </div>

          <!-- 编号浮层 -->
          <div class="cover-code">{{ row.code }}</div>
          <!-- 草稿角标 -->
          <div v-if="row.status === 'DRAFT'" class="cover-draft">草稿</div>
        </div>

        <!-- 信息区 -->
        <div class="card-body">
          <div class="card-name" :title="row.name">{{ row.name || '—' }}</div>
          <div class="card-sub">
            <span v-if="row.type" class="sub-type">{{ row.type }}</span>
            <span class="sub-composition" :title="row.composition">
              {{ row.composition || '—' }}
            </span>
          </div>
          <div class="card-specs">
            <span class="spec-item">
              <em>克重</em>{{ row.weightGsm ?? '—' }}<i>GSM</i>
            </span>
            <span class="spec-item">
              <em>门幅</em>{{ row.widthCm ?? '—' }}<i>CM</i>
            </span>
          </div>
          <!-- 彩色标签 chips -->
          <div v-if="row.tags && row.tags.length" class="card-tags">
            <span
              v-for="(tag, tagIndex) in row.tags.slice(0, 6)"
              :key="tag.id ?? tag.name + tagIndex"
              class="tag-chip"
              :style="chipStyle(tag.color)"
            >
              <i class="tag-dot" :style="{ background: resolveTagSwatch(tag.color).ink }" />
              {{ tag.name }}
            </span>
            <span v-if="row.tags.length > 6" class="tag-chip tag-more">
              +{{ row.tags.length - 6 }}
            </span>
          </div>
        </div>
      </motion.div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else-if="!loading" description="暂无产品数据，点击右上角「新建产品」开始录入" />
  </div>
</template>

<script lang="ts" setup>
import { motion } from 'motion-v'
import type { ProductVO } from '@/api/lunaerp/baseinfo/product'
import { resolveTagSwatch } from '../product-tag-palette'

defineOptions({ name: 'ProductGrid' })

const EASE: [number, number, number, number] = [0.16, 1, 0.3, 1]

defineProps({
  list: {
    type: Array as PropType<ProductVO[]>,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['edit', 'delete'])

/** 封面：coverImageUrl 优先，其次多图首张 */
const resolveCover = (row: ProductVO): string => {
  return row.coverImageUrl || row.images?.[0]?.url || ''
}

/** 标签胶囊配色（同色系浅染底 + 描边） */
const chipStyle = (color?: string) => {
  const swatch = resolveTagSwatch(color)
  return {
    color: swatch.ink,
    background: swatch.tint,
    borderColor: swatch.ring
  }
}
</script>

<style lang="scss" scoped>
.product-grid-wrap {
  min-height: 200px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.product-card {
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  transition: box-shadow 0.3s ease;

  &:hover {
    box-shadow: var(--el-box-shadow);

    .cover-actions {
      opacity: 1;
      transform: translateY(0);
    }

    .cover-code {
      opacity: 0;
    }

    .card-name {
      color: var(--el-color-primary);
    }
  }
}

.card-cover {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--el-fill-color-light);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
  }

  &:hover img {
    transform: scale(1.05);
  }
}

.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--el-text-color-disabled);
}

.cover-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateY(-6px);
  transition: all 0.3s ease;
}

.cover-code {
  position: absolute;
  left: 12px;
  bottom: 10px;
  z-index: 1;
  max-width: calc(100% - 24px);
  overflow: hidden;
  border-radius: 6px;
  padding: 2px 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  font-style: italic;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: opacity 0.3s ease;
}

.cover-draft {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 1;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--el-color-info);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px 14px;
}

.card-name {
  overflow: hidden;
  font-size: 14.5px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.25s ease;
}

.card-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  font-size: 12px;
  color: var(--el-text-color-secondary);

  .sub-type {
    flex: none;
    padding: 1px 7px;
    border-radius: 6px;
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-weight: 600;
  }

  .sub-composition {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.card-specs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;

  .spec-item {
    padding: 6px 10px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 9px;
    background: var(--el-fill-color-blank);
    font-size: 13px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;

    em {
      margin-right: 6px;
      font-style: normal;
      font-size: 10.5px;
      font-weight: 500;
      color: var(--el-text-color-secondary);
    }

    i {
      margin-left: 3px;
      font-style: normal;
      font-size: 10px;
      font-weight: 500;
      color: var(--el-text-color-secondary);
    }
  }
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 2px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1.6;

  .tag-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  &.tag-more {
    border-color: var(--el-border-color);
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-light);
  }
}
</style>
