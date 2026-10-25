/**
 * 出口单证 (PI / CI / PL) A4 标准打印 CSS 规则
 * 760px 宽度，Arial 字体，黑白外贸单证规范
 */
export const exportDocPrintCss = `
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
    font-family: Arial, "Helvetica Neue", Helvetica, sans-serif;
    color: #000;
    background: #fff;
    font-size: 11px;
    line-height: 1.4;
  }
  .export-doc-print-wrapper {
    width: 760px;
    max-width: 760px;
    margin: 0 auto;
    font-family: Arial, "Helvetica Neue", Helvetica, sans-serif;
    color: #000;
    background: #fff;
    position: relative;
  }
  .seller-header {
    text-align: center;
    margin-bottom: 8px;
  }
  .seller-name {
    font-size: 18px;
    font-weight: bold;
    text-transform: uppercase;
    letter-spacing: 1px;
  }
  .seller-address {
    font-size: 10px;
    color: #333;
    margin-top: 2px;
  }
  .doc-title {
    text-align: center;
    font-size: 16px;
    font-weight: bold;
    text-transform: uppercase;
    letter-spacing: 2px;
    padding: 6px 0;
    margin: 8px 0;
    border-top: 2px solid #000;
    border-bottom: 2px solid #000;
  }
  .header-grid {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 11px;
  }
  .buyer-box {
    width: 58%;
    border: 1px solid #000;
    padding: 6px 8px;
  }
  .meta-box {
    width: 40%;
    border: 1px solid #000;
    padding: 6px 8px;
  }
  .meta-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
  }
  .meta-label {
    font-weight: bold;
    color: #333;
  }
  .terms-box {
    border: 1px solid #000;
    padding: 6px 8px;
    margin-bottom: 12px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
    font-size: 11px;
  }
  .terms-item {
    display: flex;
    gap: 4px;
  }
  .terms-label {
    font-weight: bold;
  }
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
    border: 1px solid #000;
    padding: 4px 6px;
    line-height: 1.3;
  }
  .print-table th {
    background-color: #f2f2f2 !important;
    font-weight: bold;
    text-transform: uppercase;
  }
  .print-table .total-row td {
    font-weight: bold;
    background-color: #fafafa !important;
  }
  .text-center { text-align: center; }
  .text-left { text-align: left; }
  .text-right { text-align: right; }
  .section-box {
    border: 1px solid #000;
    padding: 6px 8px;
    margin-bottom: 10px;
    font-size: 10.5px;
  }
  .section-title {
    font-weight: bold;
    text-transform: uppercase;
    margin-bottom: 4px;
  }
  .footer-sign-qr {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-top: 16px;
    page-break-inside: avoid;
  }
  .sign-area {
    width: 60%;
  }
  .sign-title {
    font-weight: bold;
    font-size: 11px;
  }
  .sign-box {
    height: 60px;
    border-bottom: 1px solid #000;
    margin: 8px 0 4px 0;
    position: relative;
  }
  .sign-hint {
    position: absolute;
    bottom: 4px;
    left: 4px;
    font-size: 9px;
    color: #888;
  }
  .sign-date {
    font-size: 10.5px;
  }
  .qr-area {
    width: 35%;
    display: flex;
    justify-content: flex-end;
  }
`
