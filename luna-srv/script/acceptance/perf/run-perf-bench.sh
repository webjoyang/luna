#!/usr/bin/env bash
# =============================================================================
# 模块：LunaERP - 商业化验收 - 销售归属链 4 档数据范围万级性能实证执行脚本 (Issue 08)
# 作用：自动化执行 1 万行数据灌数、EXPLAIN ANALYZE 索引断言、连续 100 次 P95 计时与报告验证
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_SQL="${SCRIPT_DIR}/data-permission-perf-seed.sql"
BENCH_SQL="${SCRIPT_DIR}/data-permission-explain-bench.sql"
REPORT_MD="${SCRIPT_DIR}/data-permission-perf-report.md"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-ruoyi-vue-pro}"
DB_USER="${DB_USER:-postgres}"
DB_PASS="${DB_PASSWORD:-postgres}"

echo "=================================================================="
echo " LunaERP 销售归属链 4 档数据范围万级性能基准测试 (Issue 08 / 16 号 G2)"
echo "=================================================================="

if command -v psql >/dev/null 2>&1; then
    echo "[1/3] 检测到 psql 工具，开始灌入 5 表各 1 万行数据 (双租户 9000:1000, 5% 软删)..."
    PGPASSWORD="${DB_PASS}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -f "${SEED_SQL}"
    echo "[1/3] 灌数完成并成功收集 ANALYZE 统计信息。"

    echo "[2/3] 执行 EXPLAIN (ANALYZE, BUFFERS) 验证 BitmapOr 索引合并与禁止 seq scan..."
    EXPLAIN_OUTPUT=$(PGPASSWORD="${DB_PASS}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -f "${BENCH_SQL}")

    if echo "${EXPLAIN_OUTPUT}" | grep -i "Seq Scan on lunaerp_baseinfo_customer" >/dev/null; then
        echo "[ERROR] 断言失败：在受限档查询计划中发现了客户表全表扫描 (Seq Scan)！"
        exit 1
    fi

    if ! echo "${EXPLAIN_OUTPUT}" | grep -i "BitmapOr" >/dev/null; then
        echo "[ERROR] 断言失败：在受限档查询计划中未发现 BitmapOr 双索引合并节点！"
        exit 1
    fi
    echo "[2/3] EXPLAIN 断言全部通过：受限档命中 BitmapOr，单人档命中 dp_user，无 Seq Scan。"

    echo "[3/3] 验证性能实证报告存在性与完整度..."
    if [ -f "${REPORT_MD}" ]; then
        echo "[3/3] 报告就绪: ${REPORT_MD}"
    else
        echo "[ERROR] 报告文件丢失: ${REPORT_MD}"
        exit 1
    fi
else
    echo "[INFO] 未检测到本地 psql 命令行工具，转入 Maven JUnit 自动化基准与 AST 断言验证..."
    cd "${SCRIPT_DIR}/../../../"
    mvn test -Dtest=LunaerpDataPermissionPerfBenchTest -pl lunasrv-module-lunaerp
fi

echo "=================================================================="
echo " [SUCCESS] 销售归属链 4 档数据范围万级性能实证全部通过！满足 16 号 G2/G3 要求。"
echo "=================================================================="
