import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  calculateTargetDimensions,
  MAX_IMAGE_EDGE,
  MAX_IMAGE_SIZE_BYTES,
  compressImageFile,
  uploadPhotoFile
} from '../h5-compress'

vi.mock('@/config/axios', () => ({
  default: {
    upload: vi.fn(),
    post: vi.fn(),
    get: vi.fn()
  }
}))

vi.mock('@/api/infra/file', () => ({
  updateFile: vi.fn()
}))

import * as FileApi from '@/api/infra/file'

describe('h5-compress', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('calculateTargetDimensions', () => {
    it('keeps original dimensions when both edges are within 1920', () => {
      const result = calculateTargetDimensions(1200, 800, MAX_IMAGE_EDGE)
      expect(result).toEqual({ width: 1200, height: 800 })
    })

    it('scales down landscape image so width = 1920 and preserves aspect ratio', () => {
      const result = calculateTargetDimensions(3840, 2160, MAX_IMAGE_EDGE)
      expect(result.width).toBe(1920)
      expect(result.height).toBe(1080)
    })

    it('scales down portrait image so height = 1920 and preserves aspect ratio', () => {
      const result = calculateTargetDimensions(1080, 2160, MAX_IMAGE_EDGE)
      expect(result.width).toBe(960)
      expect(result.height).toBe(1920)
    })

    it('scales down square image > 1920 to 1920x1920', () => {
      const result = calculateTargetDimensions(3000, 3000, MAX_IMAGE_EDGE)
      expect(result).toEqual({ width: 1920, height: 1920 })
    })

    it('handles exact boundary of 1920 without scaling', () => {
      const result = calculateTargetDimensions(1920, 1080, MAX_IMAGE_EDGE)
      expect(result).toEqual({ width: 1920, height: 1080 })
    })
  })

  describe('constants', () => {
    it('defines MAX_IMAGE_EDGE as 1920', () => {
      expect(MAX_IMAGE_EDGE).toBe(1920)
    })

    it('defines MAX_IMAGE_SIZE_BYTES as 2MB (2097152 bytes)', () => {
      expect(MAX_IMAGE_SIZE_BYTES).toBe(2 * 1024 * 1024)
    })
  })

  describe('uploadPhotoFile', () => {
    it('uploads file via FileApi and returns URL when response is direct URL string', async () => {
      vi.mocked(FileApi.updateFile).mockResolvedValue('https://oss.example.com/photos/1.jpg' as any)
      const fakeFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' })

      const url = await uploadPhotoFile(fakeFile)

      expect(FileApi.updateFile).toHaveBeenCalledTimes(1)
      expect(FileApi.updateFile).toHaveBeenCalledWith(
        expect.objectContaining({ file: fakeFile }),
        expect.any(Function)
      )
      expect(url).toBe('https://oss.example.com/photos/1.jpg')
    })

    it('uploads file via FileApi and extracts URL from object response', async () => {
      vi.mocked(FileApi.updateFile).mockResolvedValue({
        code: 0,
        data: 'https://oss.example.com/photos/2.jpg'
      } as any)
      const fakeFile = new File(['content'], 'test2.jpg', { type: 'image/jpeg' })

      const url = await uploadPhotoFile(fakeFile)
      expect(url).toBe('https://oss.example.com/photos/2.jpg')
    })

    it('throws error when upload response does not contain a valid url', async () => {
      vi.mocked(FileApi.updateFile).mockResolvedValue({ code: 500 } as any)
      const fakeFile = new File(['content'], 'fail.jpg', { type: 'image/jpeg' })

      await expect(uploadPhotoFile(fakeFile)).rejects.toThrow('缺少有效的文件 URL')
    })
  })

  describe('compressImageFile', () => {
    it('returns original or converted file directly in non-DOM environment', async () => {
      const fakeFile = new File(['abc'], 'test.png', { type: 'image/png' })
      const result = await compressImageFile(fakeFile)
      expect(result).toBeDefined()
      expect(result.name).toBe('test.png')
    })
  })
})
