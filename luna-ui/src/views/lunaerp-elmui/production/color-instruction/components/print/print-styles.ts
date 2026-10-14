/**
 * 打色指示单标准 A4 打印 CSS 规则
 * 保证隐藏 iframe 打印时边框、网格、字号、边距与屏幕预览完全一致
 */
export const colorInstructionPrintCss = `
  @page {
    size: portrait;
    margin: 10mm;
  }
  * {
    box-sizing: border-box;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  body {
    margin: 0;
    padding: 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
    color: #000;
    background: #fff;
    font-size: 11px;
    line-height: 1.4;
  }
  .print-paper {
    width: 100%;
    margin: 0 auto;
  }
  .print-header {
    text-align: center;
    margin-bottom: 12px;
    position: relative;
  }
  .print-header .title {
    font-size: 20px;
    font-weight: bold;
    letter-spacing: 2px;
    margin-bottom: 2px;
  }
  .print-header .subtitle {
    font-size: 10px;
    color: #666;
    letter-spacing: 1px;
    text-transform: uppercase;
  }
  .meta-box {
    border: 1px solid #333;
    padding: 8px 12px;
    margin-bottom: 12px;
    background: #fafafa;
    font-size: 11px;
  }
  .grid {
    display: grid;
  }
  .grid-cols-2 { grid-template-columns: repeat(2, 1fr); }
  .grid-cols-3 { grid-template-columns: repeat(3, 1fr); }
  .grid-cols-4 { grid-template-columns: repeat(4, 1fr); }
  .col-span-2 { grid-column: span 2; }
  .col-span-3 { grid-column: span 3; }
  .col-span-4 { grid-column: span 4; }
  .gap-y-6px { row-gap: 6px; }
  .gap-x-12px { column-gap: 12px; }
  .text-gray-600 { color: #555; }
  .font-mono { font-family: monospace, Courier, sans-serif; }
  .font-bold { font-weight: bold; }
  .font-medium { font-weight: 500; }
  .print-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 10.5px;
    margin-bottom: 12px;
    page-break-inside: auto;
  }
  .print-table tr {
    page-break-inside: avoid;
    page-break-after: auto;
  }
  .print-table th, .print-table td {
    border: 1px solid #333;
    padding: 5px 6px;
    line-height: 1.3;
  }
  .print-table th {
    background-color: #f2f2f2 !important;
    font-weight: bold;
    text-align: center;
  }
  .rejected-row {
    background-color: #fff5f5 !important;
    color: #777;
  }
  .strike-text {
    text-decoration: line-through;
    color: #888;
  }
  .strike-text .el-tag {
    text-decoration: line-through;
  }
  .reject-reason-tip {
    color: #c53030;
    font-size: 10px;
    font-weight: normal;
    margin-top: 2px;
    text-decoration: none !important;
  }
  .color-badge {
    display: inline-block;
    width: 14px;
    height: 14px;
    vertical-align: middle;
    margin-right: 4px;
    border: 1px solid #aaa;
    border-radius: 2px;
  }
  .status-tag {
    display: inline-block;
    padding: 1px 4px;
    border-radius: 2px;
    font-size: 10px;
    font-weight: 500;
  }
  .status-tag.approved {
    background-color: #e6f7ed;
    color: #1e7e34;
    border: 1px solid #a3cfbb;
  }
  .status-tag.rejected {
    background-color: #fdf0f0;
    color: #dc3545;
    border: 1px solid #f5c2c7;
  }
  .status-tag.pending {
    background-color: #f4f6f8;
    color: #6c757d;
    border: 1px solid #dee2e6;
  }
  .section-box {
    border: 1px solid #333;
    padding: 8px 12px;
    margin-bottom: 12px;
    font-size: 11px;
    background: #fff;
  }
  .sign-box {
    border: 1px solid #333;
    padding: 12px;
    margin-top: 14px;
    font-size: 11px;
    background: #fafafa;
  }
  .sign-col {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .sign-line {
    border-bottom: 1px solid #333;
    display: inline-block;
    min-width: 120px;
    margin-left: 6px;
  }
  .text-center { text-align: center; }
  .text-left { text-align: left; }
  .text-right { text-align: right; }
`
