import { describe, expect, it } from 'vitest'
import {
  checkCameraSupported,
  resolveSupportedFormats,
  formatScanError,
  processScannedInput,
  type ScannerEnv
} from '../scanner-helper'
import { Html5QrcodeSupportedFormats } from 'html5-qrcode'

describe('scanner-helper', () => {
  describe('checkCameraSupported', () => {
    it('returns supported=true for https context with mediaDevices', () => {
      const env: ScannerEnv = {
        isSecureContext: true,
        protocol: 'https:',
        hostname: 'erp.example.com',
        hasMediaDevices: true,
        hasGetUserMedia: true
      }
      const res = checkCameraSupported(env)
      expect(res.supported).toBe(true)
      expect(res.reason).toBe('supported')
    })

    it('returns supported=true for localhost over http', () => {
      const env: ScannerEnv = {
        isSecureContext: true,
        protocol: 'http:',
        hostname: 'localhost',
        hasMediaDevices: true,
        hasGetUserMedia: true
      }
      const res = checkCameraSupported(env)
      expect(res.supported).toBe(true)
      expect(res.reason).toBe('supported')
    })

    it('returns supported=true for 127.0.0.1 over http', () => {
      const env: ScannerEnv = {
        isSecureContext: true,
        protocol: 'http:',
        hostname: '127.0.0.1',
        hasMediaDevices: true,
        hasGetUserMedia: true
      }
      const res = checkCameraSupported(env)
      expect(res.supported).toBe(true)
    })

    it('returns supported=false for 192.168.* intranet http', () => {
      const env: ScannerEnv = {
        isSecureContext: false,
        protocol: 'http:',
        hostname: '192.168.1.120',
        hasMediaDevices: true,
        hasGetUserMedia: true
      }
      const res = checkCameraSupported(env)
      expect(res.supported).toBe(false)
      expect(res.reason).toBe('insecure_http')
      expect(res.message).toContain('局域网 HTTP')
    })

    it('returns supported=false when isSecureContext is explicitly false', () => {
      const env: ScannerEnv = {
        isSecureContext: false,
        protocol: 'https:',
        hostname: 'workshop.internal',
        hasMediaDevices: true,
        hasGetUserMedia: true
      }
      const res = checkCameraSupported(env)
      expect(res.supported).toBe(false)
      expect(res.reason).toBe('insecure_context')
    })

    it('returns supported=false when mediaDevices or getUserMedia is missing', () => {
      const env: ScannerEnv = {
        isSecureContext: true,
        protocol: 'https:',
        hostname: 'erp.example.com',
        hasMediaDevices: false,
        hasGetUserMedia: false
      }
      const res = checkCameraSupported(env)
      expect(res.supported).toBe(false)
      expect(res.reason).toBe('no_mediadevices')
    })
  })

  describe('resolveSupportedFormats', () => {
    it('resolves QR_CODE for qr mode', () => {
      const formats = resolveSupportedFormats('qr')
      expect(formats).toEqual([Html5QrcodeSupportedFormats.QR_CODE])
    })

    it('resolves CODE_128 for code128 mode', () => {
      const formats = resolveSupportedFormats('code128')
      expect(formats).toEqual([Html5QrcodeSupportedFormats.CODE_128])
    })

    it('resolves both QR_CODE and CODE_128 for all mode', () => {
      const formats = resolveSupportedFormats('all')
      expect(formats).toEqual([
        Html5QrcodeSupportedFormats.QR_CODE,
        Html5QrcodeSupportedFormats.CODE_128
      ])
    })
  })

  describe('formatScanError', () => {
    it('handles permission denied errors', () => {
      const err = new Error('Permission denied')
      err.name = 'NotAllowedError'
      const res = formatScanError(err)
      expect(res.isPermissionDenied).toBe(true)
      expect(res.message).toContain('权限')
    })

    it('handles device not found errors', () => {
      const err = new Error('Requested device not found')
      err.name = 'NotFoundError'
      const res = formatScanError(err)
      expect(res.isDeviceNotFound).toBe(true)
      expect(res.message).toContain('未检测到可用摄像头')
    })

    it('handles generic errors gracefully', () => {
      const res = formatScanError('Camera initialization timeout')
      expect(res.isPermissionDenied).toBe(false)
      expect(res.message).toContain('摄像头启动失败')
    })
  })

  describe('processScannedInput', () => {
    it('trims leading and trailing whitespace and CRLF from wedge scanner', () => {
      expect(processScannedInput('  WT-20261021-001\r\n  ')).toBe('WT-20261021-001')
      expect(processScannedInput('ROLL-1002\n')).toBe('ROLL-1002')
    })

    it('returns empty string for blank input', () => {
      expect(processScannedInput('   \r\n ')).toBe('')
      expect(processScannedInput('')).toBe('')
    })
  })
})
