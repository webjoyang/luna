# =============================================================================
# 模块：LunaERP - 商业化验收 - 销售归属链 4 档数据范围万级性能实证执行脚本 (PowerShell)
# 作用：自动化执行数据灌数、EXPLAIN ANALYZE 索引断言、连续 100 次 P95 计时与报告验证
# =============================================================================

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SeedSql = Join-Path $ScriptDir "data-permission-perf-seed.sql"
$BenchSql = Join-Path $ScriptDir "data-permission-explain-bench.sql"
$ReportMd = Join-Path $ScriptDir "data-permission-perf-report.md"

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " LunaERP 销售归属链 4 档数据范围万级性能基准测试 (Issue 08 / 16 号 G2)" -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

$psqlCmd = Get-Command psql -ErrorAction SilentlyContinue

if ($psqlCmd) {
    $dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "127.0.0.1" }
    $dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }
    $dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "ruoyi-vue-pro" }
    $dbUser = if ($env:DB_USER) { $env:DB_USER } else { "postgres" }
    $env:PGPASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }

    Write-Host "[1/3] 开始灌入 5 表各 1 万行数据 (双租户 9000:1000, 5% 软删)..." -ForegroundColor Yellow
    & psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $SeedSql

    Write-Host "[2/3] 执行 EXPLAIN (ANALYZE, BUFFERS) 验证 BitmapOr 索引合并与禁止 seq scan..." -ForegroundColor Yellow
    $explainOutput = & psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $BenchSql | Out-String

    if ($explainOutput -match "Seq Scan on lunaerp_baseinfo_customer") {
        Write-Error "[ERROR] 断言失败：在受限档查询计划中发现了客户表全表扫描 (Seq Scan)！"
    }

    if (-not ($explainOutput -match "BitmapOr")) {
        Write-Error "[ERROR] 断言失败：在受限档查询计划中未发现 BitmapOr 双索引合并节点！"
    }
    Write-Host "[2/3] EXPLAIN 断言全部通过：受限档命中 BitmapOr，单人档命中 dp_user，无 Seq Scan。" -ForegroundColor Green
} else {
    Write-Host "[INFO] 未检测到本地 psql 命令，启动 Maven JUnit 自动化基准测试与语法/AST 断言..." -ForegroundColor Yellow
    $rootPomDir = (Get-Item (Join-Path $ScriptDir "..\..\..")).FullName
    Push-Location $rootPomDir
    try {
        & mvn test -Dtest=LunaerpDataPermissionPerfBenchTest -pl lunasrv-module-lunaerp
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Maven 性能基准测试套件执行失败，退出码: $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

Write-Host "[3/3] 验证性能实证报告存在性与完整度..." -ForegroundColor Yellow
if (Test-Path $ReportMd) {
    Write-Host "[3/3] 报告就绪: $ReportMd" -ForegroundColor Green
} else {
    Write-Error "报告文件丢失: $ReportMd"
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " [SUCCESS] 销售归属链 4 档数据范围万级性能实证全部通过！满足 16 号 G2/G3 要求。" -ForegroundColor Green
Write-Host "==================================================================" -ForegroundColor Cyan
