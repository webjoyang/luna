import fs from 'node:fs'
import path from 'node:path'

/**
 * 扫描 Vue SFC 模板中带有写操作动作的 el-button 是否具备 v-hasPermi 权限守卫
 */
const WRITE_ACTION_REGEX = /@click="(?:\(\)\s*=>\s*)?(?:handle(?:Confirm|Save|Update|Delete|Create|Advance|Rollback|Submit)[a-zA-Z0-9_]*)/

function scanFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8')
  const lines = content.split('\n')
  const issues = []

  let inButton = false
  let buttonStartLine = 0
  let buttonTagChunk = ''

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]

    if (!inButton && line.includes('<el-button')) {
      inButton = true
      buttonStartLine = i + 1
      buttonTagChunk = line
    } else if (inButton) {
      buttonTagChunk += ' ' + line
    }

    if (inButton && (line.includes('>') || line.includes('/>'))) {
      // 提取完整的 el-button 开标签内容
      const openTagMatch = buttonTagChunk.match(/<el-button\b[^>]*>/)
      if (openTagMatch) {
        const tagText = openTagMatch[0]
        if (WRITE_ACTION_REGEX.test(tagText) && !tagText.includes('v-hasPermi')) {
          issues.push({
            line: buttonStartLine,
            tag: tagText.trim().replace(/\s+/g, ' ')
          })
        }
      }
      inButton = false
      buttonTagChunk = ''
    }
  }

  return issues
}

function walkDir(dir, fileList = []) {
  if (!fs.existsSync(dir)) return fileList
  const files = fs.readdirSync(dir)
  for (const file of files) {
    const fullPath = path.join(dir, file)
    const stat = fs.statSync(fullPath)
    if (stat.isDirectory()) {
      if (file !== 'node_modules' && file !== '.git' && file !== 'dist') {
        walkDir(fullPath, fileList)
      }
    } else if (file.endsWith('.vue')) {
      fileList.push(fullPath)
    }
  }
  return fileList
}

// 支持传入一个或多个目录/文件参数，默认检测 src/views/lunaerp-elmui
const targetArgs = process.argv.slice(2)
const targets = targetArgs.length > 0 ? targetArgs : ['src/views/lunaerp-elmui']

const filesToScan = []
const scannedSet = new Set()

for (const arg of targets) {
  const targetPath = path.resolve(process.cwd(), arg)
  if (!fs.existsSync(targetPath)) continue
  const stat = fs.statSync(targetPath)
  if (stat.isFile()) {
    if (targetPath.endsWith('.vue') && !scannedSet.has(targetPath)) {
      filesToScan.push(targetPath)
      scannedSet.add(targetPath)
    }
  } else if (stat.isDirectory()) {
    for (const file of walkDir(targetPath)) {
      if (!scannedSet.has(file)) {
        filesToScan.push(file)
        scannedSet.add(file)
      }
    }
  }
}

let totalIssues = 0

for (const file of filesToScan) {
  const issues = scanFile(file)
  if (issues.length > 0) {
    const relativePath = path.relative(process.cwd(), file)
    console.warn(`\n[Missing v-hasPermi] in ${relativePath}:`)
    for (const issue of issues) {
      console.warn(`  Line ${issue.line}: ${issue.tag}`)
      totalIssues++
    }
  }
}

if (totalIssues > 0) {
  console.error(`\nFound ${totalIssues} button(s) with write actions lacking v-hasPermi directive.`)
  process.exit(1)
} else {
  console.log(`\nChecked ${filesToScan.length} vue file(s). All write buttons have v-hasPermi.`)
  process.exit(0)
}
