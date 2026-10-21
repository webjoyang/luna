import * as FileApi from '@/api/infra/file'

/**
 * LunaERP 车间 H5 统一拍照压缩与上传工具（ADR-0045, Ticket 08）
 * 规约：
 * 1. canvas 长边 ≤1920
 * 2. 导出 JPEG ≤2MB
 * 3. POST /admin-api/infra/file/upload multipart 携带系统同一 token
 */

export const MAX_IMAGE_EDGE = 1920
export const MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB

export interface CompressOptions {
  maxEdge?: number
  maxSize?: number
  initialQuality?: number
  minQuality?: number
  qualityStep?: number
}

export interface UploadPhotoOptions {
  directory?: string
  onProgress?: (percent: number) => void
  compress?: boolean
  compressOptions?: CompressOptions
}

/**
 * 计算保持原始长宽比下的目标缩放尺寸
 * 长边若超过 maxEdge 则等比下采样至 maxEdge；若未超过则保持原样
 */
export function calculateTargetDimensions(
  width: number,
  height: number,
  maxEdge: number = MAX_IMAGE_EDGE
): { width: number; height: number } {
  if (width <= 0 || height <= 0) {
    return { width: Math.max(0, width), height: Math.max(0, height) }
  }

  const currentMaxEdge = Math.max(width, height)
  if (currentMaxEdge <= maxEdge) {
    return { width, height }
  }

  const ratio = maxEdge / currentMaxEdge
  return {
    width: Math.round(width * ratio),
    height: Math.round(height * ratio)
  }
}

/**
 * 将 Blob/File 读入 HTMLImageElement
 */
function loadImage(file: Blob): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = (err) => {
      URL.revokeObjectURL(url)
      reject(err)
    }
    img.src = url
  })
}

/**
 * 将 Canvas 内容转为 JPEG Blob
 */
function canvasToBlob(
  canvas: HTMLCanvasElement,
  quality: number,
  type = 'image/jpeg'
): Promise<Blob | null> {
  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), type, quality)
  })
}

/**
 * 统一图片压缩工具：canvas 长边 ≤1920、JPEG ≤2MB
 */
export async function compressImageFile(
  file: File | Blob,
  options: CompressOptions = {}
): Promise<File> {
  const maxEdge = options.maxEdge ?? MAX_IMAGE_EDGE
  const maxSize = options.maxSize ?? MAX_IMAGE_SIZE_BYTES
  const initialQuality = options.initialQuality ?? 0.85
  const minQuality = options.minQuality ?? 0.3
  const qualityStep = options.qualityStep ?? 0.1

  const fileName =
    file instanceof File ? file.name.replace(/\.[^.]+$/, '') + '.jpg' : `photo_${Date.now()}.jpg`

  // Node 环境或无 DOM Canvas 环境下兜底直接返回
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    if (file instanceof File) return file
    return new File([file], fileName, { type: 'image/jpeg' })
  }

  try {
    const img = await loadImage(file)
    const { width, height } = calculateTargetDimensions(
      img.naturalWidth,
      img.naturalHeight,
      maxEdge
    )

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')

    if (!ctx) {
      if (file instanceof File) return file
      return new File([file], fileName, { type: 'image/jpeg' })
    }

    ctx.drawImage(img, 0, 0, width, height)

    // 迭代压缩质量直到 <= maxSize 或触达最低质量 minQuality
    let quality = initialQuality
    let blob = await canvasToBlob(canvas, quality, 'image/jpeg')

    while (blob && blob.size > maxSize && quality > minQuality) {
      quality = Math.max(minQuality, quality - qualityStep)
      const nextBlob = await canvasToBlob(canvas, quality, 'image/jpeg')
      if (nextBlob) {
        blob = nextBlob
      } else {
        break
      }
    }

    if (!blob) {
      if (file instanceof File) return file
      return new File([file], fileName, { type: 'image/jpeg' })
    }

    return new File([blob], fileName, {
      type: 'image/jpeg',
      lastModified: Date.now()
    })
  } catch (error) {
    console.warn('[h5-compress] Canvas 压缩失败，降级使用原文件:', error)
    if (file instanceof File) return file
    return new File([file], fileName, { type: 'image/jpeg' })
  }
}

/**
 * 统一拍照上传工具：压缩后 POST /admin-api/infra/file/upload multipart 携带系统同一 token，返回 URL
 */
export async function uploadPhotoFile(
  file: File | Blob,
  options: UploadPhotoOptions = {}
): Promise<string> {
  const shouldCompress = options.compress ?? true
  const uploadFile = shouldCompress
    ? await compressImageFile(file, options.compressOptions)
    : file instanceof File
      ? file
      : new File([file], `photo_${Date.now()}.jpg`, { type: 'image/jpeg' })

  const progressHandler = (progressEvent: any) => {
    if (options.onProgress && progressEvent) {
      const percent = progressEvent.total
        ? Math.round((progressEvent.loaded * 100) / progressEvent.total)
        : progressEvent.percent || 0
      options.onProgress(percent)
    }
  }

  const res: any = await FileApi.updateFile(
    { file: uploadFile, directory: options.directory },
    progressHandler
  )

  if (typeof res === 'string') {
    return res
  }
  if (res && typeof res.data === 'string') {
    return res.data
  }
  if (res && res.url) {
    return res.url
  }

  throw new Error('上传返回数据缺少有效的文件 URL')
}
