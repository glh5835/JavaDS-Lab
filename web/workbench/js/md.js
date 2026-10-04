/* 极简 Markdown 渲染器：覆盖 22 篇笔记用到的语法
   （标题/列表/表格/代码块/行内代码/加粗/引用/链接/分隔线），HTML 全转义防注入。 */
"use strict";

function esc(s) {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

function inline(s) {
  let out = esc(s);
  out = out.replace(/`([^`]+)`/g, "<code>$1</code>");
  out = out.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
  out = out.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
  return out;
}

export function renderMarkdown(src) {
  const lines = src.replace(/\r\n/g, "\n").split("\n");
  const out = [];
  let i = 0;
  let para = [];

  const flushPara = () => {
    if (para.length) { out.push("<p>" + inline(para.join(" ")) + "</p>"); para = []; }
  };

  while (i < lines.length) {
    const line = lines[i];
    // 代码块
    if (line.trim().startsWith("```")) {
      flushPara();
      const buf = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith("```")) { buf.push(lines[i]); i++; }
      i++; // 跳过结尾 ```
      out.push("<pre><code>" + esc(buf.join("\n")) + "</code></pre>");
      continue;
    }
    // 表格
    if (line.includes("|") && i + 1 < lines.length && /^\s*\|?[\s:|-]+\|[\s:|-]*$/.test(lines[i + 1])) {
      flushPara();
      const cells = r => r.trim().replace(/^\||\|$/g, "").split("|").map(c => c.trim());
      const head = cells(line);
      i += 2;
      const rows = [];
      while (i < lines.length && lines[i].includes("|")) { rows.push(cells(lines[i])); i++; }
      out.push("<table><thead><tr>" + head.map(h => "<th>" + inline(h) + "</th>").join("") + "</tr></thead><tbody>"
        + rows.map(r => "<tr>" + r.map(c => "<td>" + inline(c) + "</td>").join("") + "</tr>").join("") + "</tbody></table>");
      continue;
    }
    // 标题
    const h = line.match(/^(#{1,4})\s+(.*)$/);
    if (h) {
      flushPara();
      out.push(`<h${h[1].length}>` + inline(h[2]) + `</h${h[1].length}>`);
      i++;
      continue;
    }
    // 引用
    if (line.startsWith("> ")) {
      flushPara();
      const buf = [];
      while (i < lines.length && lines[i].startsWith("> ")) { buf.push(lines[i].slice(2)); i++; }
      out.push("<blockquote>" + inline(buf.join(" ")) + "</blockquote>");
      continue;
    }
    // 无序列表
    if (/^\s*[-*]\s+/.test(line)) {
      flushPara();
      const buf = [];
      while (i < lines.length && /^\s*[-*]\s+/.test(lines[i])) { buf.push(lines[i].replace(/^\s*[-*]\s+/, "")); i++; }
      out.push("<ul>" + buf.map(x => "<li>" + inline(x) + "</li>").join("") + "</ul>");
      continue;
    }
    // 有序列表
    if (/^\s*\d+[.、]\s*/.test(line)) {
      flushPara();
      const buf = [];
      while (i < lines.length && /^\s*\d+[.、]\s*/.test(lines[i])) { buf.push(lines[i].replace(/^\s*\d+[.、]\s*/, "")); i++; }
      out.push("<ol>" + buf.map(x => "<li>" + inline(x) + "</li>").join("") + "</ol>");
      continue;
    }
    // 分隔线
    if (/^\s*---+\s*$/.test(line)) { flushPara(); out.push("<hr>"); i++; continue; }
    // 空行
    if (line.trim() === "") { flushPara(); i++; continue; }
    para.push(line.trim());
    i++;
  }
  flushPara();
  return out.join("\n");
}
