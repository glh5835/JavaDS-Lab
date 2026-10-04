/* 项目验证中心（§24/§25/§43）：
   - 历史报告区：展示项目已有文件（明确标注"历史报告"，不冒充本次结果）；
   - 本次执行区：后台任务（Maven 测试 / 数据库性能实验 / 完整验证），不阻塞页面，轮询状态与日志。 */
"use strict";
import { api } from "../api.js";
import { renderMarkdown } from "../md.js";

export async function render(el) {
  el.innerHTML = `
    <div class="card">
      <h2>本次执行（后台任务）</h2>
      <p class="hint">任务在服务端后台运行，不会阻塞页面。点击后可实时看到当前阶段与日志。</p>
      <div class="row">
        <button class="btn primary" data-task="maven-test">运行 Maven 全量测试</button>
        <button class="btn" data-task="db-experiment">数据库性能实验（独立 experiment.db）</button>
        <button class="btn accent" data-task="full-verify">完整验证（测试 + Trace 校验）</button>
      </div>
      <div id="task-status" style="margin-top:10px"></div>
    </div>
    <div class="grid2" style="align-items:start">
      <div class="card"><h2>任务历史</h2><div id="task-history" class="hint">加载中……</div></div>
      <div class="card"><h2>历史报告（项目已有文件）</h2>
        <p class="hint">以下内容来自项目已有文件，属历史实测数据，不是刚刚执行的结果。</p>
        <div id="report-panel"><button class="btn" id="load-report">查看 JDBC 基准报告（历史）</button></div>
      </div>
    </div>`;

  let pollTimer = null;

  async function loadHistory() {
    const resp = await api.get("/api/tasks");
    const his = el.querySelector("#task-history");
    if (!resp.tasks.length) {
      his.innerHTML = "还没有执行过任务。";
      return;
    }
    his.innerHTML = resp.tasks.slice().reverse().map(t => `
      <div class="row" style="padding:4px 0;border-bottom:1px solid var(--border)">
        <a href="javascript:void 0" data-tid="${t.id}">任务 #${t.id}</a>
        <span class="badge">${taskName(t.type)}</span>
        ${statusBadge(t.status)}
        <span class="hint" style="flex:1">${esc(t.summary || t.startedAt || "")}</span>
      </div>`).join("");
    his.querySelectorAll("[data-tid]").forEach(a => a.addEventListener("click", () => poll(a.dataset.tid, true)));
  }

  function taskName(t) {
    return { "maven-test": "Maven 测试", "db-experiment": "数据库实验", "full-verify": "完整验证" }[t] || t;
  }
  function statusBadge(s) {
    return s === "success" ? '<span class="badge ok">成功</span>'
      : s === "failed" ? '<span class="badge" style="background:var(--danger-bg);color:var(--danger)">失败</span>'
      : s === "running" ? '<span class="badge warn">运行中</span>'
      : s === "cancelled" ? '<span class="badge">已取消</span>'
      : '<span class="badge">排队中</span>';
  }

  async function poll(id, once) {
    const box = el.querySelector("#task-status");
    const t = await api.get("/api/tasks/" + id);
    const done = t.status === "success" || t.status === "failed" || t.status === "cancelled";
    const logs = (t.logs || "").split("\n").slice(-40).join("\n");
    box.innerHTML = `
      <div class="row"><b>任务 #${t.id}</b> <span class="badge info">${taskName(t.type)}</span> ${statusBadge(t.status)}
        <span class="hint">${esc(t.startedAt || "")} → ${esc(t.endedAt || "")}</span></div>
      ${t.summary ? `<p class="hint">${esc(t.summary)}</p>` : ""}
      <pre style="max-height:300px">${esc(logs) || "（等待日志…）"}</pre>
      ${done ? "" : '<p class="hint">任务运行中，每 2 秒自动刷新……</p>'}`;
    if (!done && !once) {
      clearTimeout(pollTimer);
      pollTimer = setTimeout(() => poll(id), 2000);
    }
    if (done) loadHistory();
  }

  el.querySelectorAll("[data-task]").forEach(b => {
    b.onclick = async () => {
      try {
        const t = await api.post("/api/tasks", { type: b.dataset.task });
        await poll(t.id);
      } catch (e) {
        el.querySelector("#task-status").innerHTML = `<div class="err-text">${esc(e.message)}</div>`;
      }
    };
  });

  el.querySelector("#load-report").onclick = async () => {
    try {
      const r = await api.get("/api/report/jdbc");
      el.querySelector("#report-panel").innerHTML =
        `<div class="badge warn">${esc(r.kind)}</div><div class="md" style="margin-top:8px;max-height:60vh;overflow:auto">${renderMarkdown(r.markdown)}</div>`;
    } catch (e) {
      el.querySelector("#report-panel").innerHTML = `<div class="err-text">${esc(e.message)}</div>`;
    }
  };

  await loadHistory();
  return () => clearTimeout(pollTimer);
}

function esc(s) {
  return String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
