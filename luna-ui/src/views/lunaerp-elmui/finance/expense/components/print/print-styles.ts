/**
 * 员工费用报销单 A4 纵向套打标准 CSS 规则
 * 对齐 04 号通用打印内核
 */
export const expensePrintCss = `
  @page {
    size: A4 portrait;
    margin: 15mm 15mm;
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
    color: #111;
    background: #fff;
    font-size: 12px;
    line-height: 1.5;
  }
  .expense-sheet-wrapper {
    width: 100%;
    max-width: 180mm;
    margin: 0 auto;
  }
  .print-header {
    text-align: center;
    margin-bottom: 16px;
    position: relative;
  }
  .print-header .main-title {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 4px;
    margin-bottom: 4px;
    color: #000;
  }
  .print-header .sub-title {
    font-size: 11px;
    color: #666;
    letter-spacing: 1px;
    text-transform: uppercase;
  }
  .meta-box {
    border: 1px solid #333;
    padding: 10px 14px;
    margin-bottom: 14px;
    background: #fafafa;
    font-size: 12px;
  }
  .meta-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    row-gap: 8px;
    column-gap: 16px;
  }
  .meta-item {
    display: flex;
    align-items: center;
  }
  .meta-item.span-2 {
    grid-column: span 2;
  }
  .meta-item.span-3 {
    grid-column: span 3;
  }
  .meta-item .label {
    color: #555;
    flex-shrink: 0;
  }
  .meta-item .value {
    color: #111;
    word-break: break-all;
  }
  .table-box {
    margin-bottom: 14px;
  }
  .data-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 11.5px;
  }
  .data-table th,
  .data-table td {
    border: 1px solid #333;
    padding: 6px 8px;
    vertical-align: middle;
  }
  .data-table th {
    background-color: #f0f0f0;
    font-weight: 600;
    text-align: center;
  }
  .data-table td.text-center {
    text-align: center;
  }
  .data-table td.text-right {
    text-align: right;
  }
  .total-row td {
    font-weight: bold;
    background-color: #fafafa;
  }
  .attachments-box {
    border: 1px solid #333;
    border-top: none;
    padding: 8px 12px;
    font-size: 11px;
    background-color: #fff;
    margin-top: -14px;
    margin-bottom: 14px;
  }
  .sign-box {
    margin-top: 20px;
    border: 1px solid #333;
  }
  .sign-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
  }
  .sign-col {
    border-right: 1px solid #333;
    padding: 8px;
    text-align: center;
    min-height: 70px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
  .sign-col:last-child {
    border-right: none;
  }
  .sign-col .sign-title {
    font-size: 11.5px;
    font-weight: 600;
    color: #333;
    margin-bottom: 24px;
  }
  .sign-col .sign-name {
    font-size: 12px;
    font-weight: 500;
    color: #111;
  }
`
