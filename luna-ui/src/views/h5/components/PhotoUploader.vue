<template>
  <div class="h5-photo-uploader">
    <div class="flex items-center justify-between mb-2">
      <span class="text-13px font-medium text-gray-700">现场照片</span>
      <span class="text-12px text-gray-400">
        {{ successCount }}/{{ maxCount }} (长边≤1920, JPEG≤2MB)
      </span>
    </div>

    <van-uploader
      v-model="fileList"
      :max-count="maxCount"
      :disabled="disabled || isReadonly"
      :deletable="!disabled && !isReadonly"
      :capture="capture"
      :after-read="handleAfterRead"
      :before-delete="handleBeforeDelete"
      :reupload="true"
      multiple
      accept="image/*"
      upload-icon="photograph"
      :upload-text="uploadText"
      @click-reupload="handleClickReupload"
    />

    <div v-if="hasFailedItems" class="mt-2 text-11px text-amber-600 flex items-center">
      <van-icon name="warning-o" class="mr-1" />
      <span>存在上传失败的照片，点击标记项可重试上传，不影响其他单据内容</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue'
import type { UploaderFileListItem } from 'vant'
import { showToast } from 'vant'
import {
  compressImageFile,
  uploadPhotoFile,
  MAX_IMAGE_EDGE,
  MAX_IMAGE_SIZE_BYTES
} from '@/utils/h5-compress'

defineOptions({ name: 'H5PhotoUploader' })

interface Props {
  modelValue?: string[]
  maxCount?: number
  disabled?: boolean
  readonly?: boolean
  capture?: string
  uploadText?: string
  directory?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  maxCount: 6,
  disabled: false,
  readonly: false,
  capture: 'environment',
  uploadText: '拍照/上传',
  directory: 'workshop/photos'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
  (e: 'change', value: string[]): void
}>()

const isReadonly = computed(() => props.readonly)

interface InternalPhotoItem extends UploaderFileListItem {
  rawFile?: File
}

const fileList = ref<InternalPhotoItem[]>([])

// 计算当前已上传成功的图片数量
const successCount = computed(() => {
  return fileList.value.filter((item) => item.status === 'done' && item.url).length
})

// 是否存在失败的照片项
const hasFailedItems = computed(() => {
  return fileList.value.some((item) => item.status === 'failed')
})

// 将已成功上传的 URL 同步向父组件发出
const emitCurrentUrls = () => {
  const urls = fileList.value
    .filter((item) => item.status === 'done' && Boolean(item.url))
    .map((item) => item.url as string)

  emit('update:modelValue', urls)
  emit('change', urls)
}

// 监听外部 modelValue 变更（如从草稿恢复），增量同步至 fileList
watch(
  () => props.modelValue,
  (newUrls) => {
    const urls = newUrls || []
    const currentDoneUrls = fileList.value
      .filter((item) => item.status === 'done' && Boolean(item.url))
      .map((item) => item.url as string)

    // 如果集合内容完全相同则不重复重建，保留当前正在上传或失败项
    if (
      urls.length === currentDoneUrls.length &&
      urls.every((u, idx) => u === currentDoneUrls[idx])
    ) {
      return
    }

    const inFlightOrFailedItems = fileList.value.filter(
      (item) => item.status === 'uploading' || item.status === 'failed'
    )

    const restoredItems: InternalPhotoItem[] = urls.map((url) => ({
      url,
      status: 'done',
      isImage: true,
      deletable: !props.disabled && !isReadonly.value
    }))

    fileList.value = [...restoredItems, ...inFlightOrFailedItems]
  },
  { immediate: true, deep: true }
)

/**
 * 执行单张照片压缩并上传
 */
const uploadSingleItem = async (item: InternalPhotoItem) => {
  const originalFile = item.rawFile || item.file
  if (!originalFile) {
    item.status = 'failed'
    item.message = '无有效文件'
    return
  }

  item.status = 'uploading'
  item.message = '压缩上传中...'

  try {
    // 1. 统一压缩：canvas 长边 ≤1920、JPEG ≤2MB
    const compressed = await compressImageFile(originalFile, {
      maxEdge: MAX_IMAGE_EDGE,
      maxSize: MAX_IMAGE_SIZE_BYTES
    })

    // 2. POST /admin-api/infra/file/upload 携带同一 token 上传
    const url = await uploadPhotoFile(compressed, {
      directory: props.directory
    })

    item.url = url
    item.status = 'done'
    item.message = ''
    item.reupload = false

    emitCurrentUrls()
  } catch (error) {
    console.warn('[PhotoUploader] 照片上传失败:', error)
    item.status = 'failed'
    item.message = '上传失败，点击重试'
    item.reupload = true
    showToast({
      message: '照片上传失败，点击红标可重试',
      type: 'fail',
      duration: 2000
    })
  }
}

/**
 * 文件选定/拍照后的回调
 */
const handleAfterRead = async (items: UploaderFileListItem | UploaderFileListItem[]) => {
  const list = Array.isArray(items) ? items : [items]

  for (const item of list) {
    const internalItem = item as InternalPhotoItem
    internalItem.rawFile = item.file
    await uploadSingleItem(internalItem)
  }
}

/**
 * 点击上传失败项触发独立重试，不影响表单其他数据
 */
const handleClickReupload = async (_item: UploaderFileListItem, detail: { index: number }) => {
  const target = fileList.value[detail.index]
  if (target && target.status === 'failed') {
    await uploadSingleItem(target)
  }
}

/**
 * 删除照片前拦截与同步
 */
const handleBeforeDelete = (item: UploaderFileListItem, detail: { index: number }) => {
  if (props.disabled || isReadonly.value) {
    return false
  }

  fileList.value.splice(detail.index, 1)
  emitCurrentUrls()
  return true
}
</script>

<style scoped>
.h5-photo-uploader :deep(.van-uploader__preview-image) {
  border-radius: 6px;
  object-fit: cover;
}
.h5-photo-uploader :deep(.van-uploader__upload) {
  border-radius: 6px;
  background-color: #f7f8fa;
  border: 1px dashed #dcdee0;
}
</style>
