/**
 * 销售退货单标准 A4 纵向套打 CSS 规则
 * 保证隐藏 iframe 打印与弹窗预览时字号、边框、网格、边距及草稿水印完全一致
 */
export const salesReturnPrintCss = `
  @page {
    size: A4 portrait;
    margin: 12mm 15mm;
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
    font-size: 11px;
    line-height: 1.4;
  }
  .sales-return-doc-wrapper {
    width: 100%;
    max-width: 185mm;
    margin: 0 auto;
    position: relative;
  }
  .print-header {
    text-align: center;
    margin-bottom: 12px;
    position: relative;
  }
  .print-header .main-title {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 3px;
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
    padding: 8px 12px;
    margin-bottom: 12px;
    background: #fafafa;
    font-size: 11px;
  }
  .meta-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    row-gap: 6px;
    column-gap: 12px;
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
    white-space: nowrap;
  }
  .meta-item .value {
    color: #111;
    font-weight: 500;
  }
  .print-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 12px;
    font-size: 10.5px;
  }
  .print-table th,
  .print-table td {
    border: 1px solid #333;
    padding: 5px 6px;
  }
  .print-table th {
    background-color: #f2f2f2;
    font-weight: bold;
    text-align: center;
  }
  .summary-box {
    border: 1px solid #333;
    padding: 8px 12px;
    margin-bottom: 14px;
    background: #fafafa;
    font-size: 11px;
  }
  .sign-area {
    margin-top: 18px;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    font-size: 11px;
    row-gap: 16px;
  }
  .sign-item {
    display: flex;
    align-items: center;
  }
  .sign-line {
    display: inline-block;
    border-bottom: 1px solid #333;
    min-width: 90px;
    height: 18px;
    margin-left: 4px;
  }
  .draft-watermark {
    position: absolute;
    top: 38%;
    left: 50%;
    transform: translate(-50%, -50%) rotate(-30deg);
    font-size: 72px;
    font-weight: 900;
    color: rgba(220, 38, 38, 0.16);
    border: 5px dashed rgba(220, 38, 38, 0.28);
    padding: 10px 40px;
    border-radius: 12px;
    letter-spacing: 10px;
    user-select: none;
    pointer-events: none;
    z-index: 10;
    text-transform: uppercase;
    white-space: nowrap;
  }
  .roll-tags-wrap {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-top: 2px;
  }
  .roll-tag {
    font-size: 9.5px;
    padding: 1px 4px;
    background: #eee;
    border-radius: 2px;
    border: 1px solid #ccc;
    font-family: monospace;
  }
`
