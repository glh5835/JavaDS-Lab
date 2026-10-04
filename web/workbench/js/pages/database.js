/* 数据库实验（§22/§23）：
   - 学习库 lab.db 只跑 10 条固定查询（真实执行，展示 SQL/参数/结果/耗时/执行计划）；
   - 独立实验库 experiment.db 允许造数/建删索引/EXPLAIN/受限自由 SQL；
   - 表关系与表结构读取 sql/ER.md、sql/schema.sql 原文（不伪造）。 */
"use strict";
import { api } from "../api.js";
import { renderMarkdown } from "../md.js";

export async function render(el) {
  el.innerHTML = `
    <div class="grid2" style="grid-template-columns:320px 1fr;align-items:start">
      <div class="card" style="position:sticky;top:10px">
        <h2>10 条已有查询</h2>
        <div id="q-list"></div>
        <hr style="border:none;border-top:1px solid var(--border);margin:12px 0">
        <h2>实验库 data/experiment.db</h2>
        <div id="exp-status">加载中……</div>
        <div class="row" style="margin-top:8px">
          <button class="btn" id="exp-seed">造数 10 万行</button>
          <button class="btn" id="exp-idx">建索引</button>
          <button class="btn" id="exp-didx">删索引</button>
          <button class="btn danger" id="exp-reset">重置实验库</button>
        </div>
        <div class="field" style="margin-top:8px"><label for="exp-sql">实验库自由 SQL（仅此库；单条；禁 ATTACH/PRAGMA）</label>
          <textarea id="exp-sql" placeholder="SELECT algorithm_id, COUNT(*) FROM runs GROUP BY algorithm_id"></textarea></div>
        <div class="row"><button class="btn accent" id="exp-run">在实验库执行</button>
        <button class="btn" id="exp-explain">查看执行计划</button></div>
      </div>
      <div>
        <div class="card" id="table-panel"><h2>表关系与表结构</h2><div class="hint">加载中……</div></div>
        <div class="card" id="query-panel"><div class="empty">从左侧选择一条查询，或使用实验库工具。</div></div>
      </div>
    </div>`;

  /* ---- 10 条查询 ---- */
  const queries = (await api.get("/api/database/queries")).queries;
  const qList = el.querySelector("#q-list");
  qList.innerHTML = queries.map(q => `
    <a href="javascript:void 0" class="row" data-qid="${q.id}" style="padding:6px 8px;border-radius:6px">
      <span class="badge info">${q.id}</span><span style="flex:1;font-size:13px">${esc(q.name)}</span>
      ${q.hasParam ? '<span class="badge warn">参数</span>' : ""}
    </a>`).join("");
  qList.querySelectorAll("[data-qid]").forEach(a => {
    a.addEventListener("click", () => openQuery(a.dataset.qid));
  });

  async function openQuery(qid) {
    const q = queries.find(x => x.id === qid);
    const panel = el.querySelector("#query-panel");
    panel.innerHTML = `
      <h2>${esc(q.name)} <span class="badge info">${q.id}</span></h2>
      <p class="hint">查询目标：${esc(q.target)}</p>
      <details open><summary style="cursor:pointer;color:var(--muted)">SQL（固定查询，参数经 PreparedStatement 绑定）</summary>
        <pre><code>${esc(q.sql)}</code></pre></details>
      ${q.hasParam ? `<div class="field"><label for="q-param">查询参数（HAVING 行数下限，整数）</label>
        <input type="number" id="q-param" value="5" style="width:140px"></div>` : ""}
      <div class="row"><button class="btn primary" id="q-run">执行查询</button><span class="hint" id="q-msg"></span></div>
      <div id="q-result" style="margin-top:10px"></div>`;
    panel.querySelector("#q-run").onclick = async () => {
      const msg = panel.querySelector("#q-msg");
      msg.textContent = "执行中……";
      try {
        const resp = await api.post("/api/database/query", { queryId: qid, param: (panel.querySelector("#q-param") || {}).value || "" });
        msg.textContent = `耗时 ${resp.elapsedMs} ms`;
        const result = el.querySelector("#q-result");
        result.innerHTML = `
          <div class="row"><span class="badge ok">${resp.rowCount} 行</span>${resp.truncated ? '<span class="badge warn">结果超 500 行已截断展示</span>' : ""}</div>
          <table class="data" style="margin-top:8px"><thead><tr>${resp.columns.map(c => `<th>${esc(c)}</th>`).join("")}</tr></thead>
          <tbody>${resp.rows.map(r => `<tr>${r.map(c => `<td>${esc(c)}</td>`).join("")}</tr>`).join("")}</tbody></table>
          <details style="margin-top:8px"><summary style="cursor:pointer;color:var(--muted)">EXPLAIN QUERY PLAN（真实执行计划）</summary>
            <pre><code>${esc(resp.plan)}</code></pre></details>`;
      } catch (e) {
        msg.textContent = "";
        el.querySelector("#q-result").innerHTML = `<div class="err-text">${esc(e.message)}</div>`;
      }
    };
  }

  /* ---- 表关系/结构（读真实 schema 文件） ---- */
  api.get("/api/database/schema").then(s => {
    el.querySelector("#table-panel").innerHTML = `
      <h2>表关系与表结构（sql/ER.md + sql/schema.sql 原文）</h2>
      <div class="row"><span class="badge info">学习库 data/lab.db</span><span class="badge">algorithms / runs / steps / mistakes${s.tables ? " / " + s.tables.filter(t => !["algorithms","runs","steps","mistakes","sqlite_sequence"].includes(t)).join(" / ") : ""}</span></div>
      <details open style="margin-top:8px"><summary style="cursor:pointer;color:var(--muted)">ER 关系说明</summary><div class="md">${renderMarkdown(s.er)}</div></details>
      <details style="margin-top:8px"><summary style="cursor:pointer;color:var(--muted)">建表 SQL</summary><pre><code>${esc(s.schema)}</code></pre></details>`;
  }).catch(() => {});

  /* ---- 实验库 ---- */
  async function loadStatus() {
    try {
      const st = await api.get("/api/database/status");
      el.querySelector("#exp-status").innerHTML = st.exists
        ? (st.seeded
          ? `<span class="badge ok">已造数</span> <span class="hint">runs=${st.runs} · 索引 ${st.indexes} 个</span>`
          : `<span class="badge warn">空库（尚未造数）</span>`)
        : `<span class="badge">文件不存在（首次造数时自动创建）</span>`;
    } catch (e) {
      el.querySelector("#exp-status").innerHTML = `<span class="err-text">${esc(e.message)}</span>`;
    }
  }
  async function expAction(op, params) {
    try {
      const resp = await api.post("/api/database/experiment", { op, params });
      showExpResult(resp.summary || "", resp);
      await loadStatus();
    } catch (e) {
      showExpResult("操作失败：" + e.message);
    }
  }
  function showExpResult(summary, resp) {
    const panel = el.querySelector("#query-panel");
    const extra = resp && resp.columns ? `<table class="data" style="margin-top:8px"><thead><tr>${resp.columns.map(c => `<th>${esc(c)}</th>`).join("")}</tr></thead>
      <tbody>${(resp.rows || []).map(r => `<tr>${r.map(c => `<td>${esc(c)}</td>`).join("")}</tr>`).join("")}</tbody></table>
      ${resp.elapsedMs !== undefined ? `<p class="hint">耗时 ${resp.elapsedMs} ms</p>` : ""}` : "";
    panel.innerHTML = `<h2>实验库操作结果</h2><p>${esc(summary)}</p>${extra}`;
  }

  el.querySelector("#exp-seed").onclick = () => expAction("seed", { rows: 100000 });
  el.querySelector("#exp-idx").onclick = () => expAction("create-index", {});
  el.querySelector("#exp-didx").onclick = () => expAction("drop-index", {});
  el.querySelector("#exp-reset").onclick = () => { if (confirm("重置实验库？（只影响 experiment.db，不动学习库）")) expAction("reset", {}); };
  el.querySelector("#exp-run").onclick = () => expAction("sql", { sql: el.querySelector("#exp-sql").value });
  el.querySelector("#exp-explain").onclick = () => expAction("explain", { sql: el.querySelector("#exp-sql").value });

  await loadStatus();
}

function esc(s) {
  return String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
