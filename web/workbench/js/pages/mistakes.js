/* 错题本：与 CLI 共用 data/lab.db（§15），禁止第二套数据库
   支持：新增/编辑/删除/搜索/按知识点筛选/重做（§15.1~15.3）；薄弱项只描述记录（§16）。 */
"use strict";
import { api } from "../api.js";

export async function render(el, params) {
  el.innerHTML = `
    <div class="card">
      <div class="row">
        <input type="text" id="mk-q" class="grow" placeholder="搜索题目/原因/错误答案…" aria-label="搜索">
        <select id="mk-algo" style="min-width:170px" aria-label="按知识点筛选"><option value="">全部知识点</option></select>
        <button class="btn" id="mk-search">筛选</button>
        <button class="btn primary" id="mk-new">＋ 手动添加错题</button>
      </div>
    </div>
    <div class="grid2" style="grid-template-columns:7fr 5fr;align-items:start">
      <div id="mk-list"></div>
      <div>
        <div class="card" id="weak-panel"><h2>薄弱知识点</h2><div id="weak-body"></div></div>
        <div id="editor-panel"></div>
      </div>
    </div>`;

  const algoSel = el.querySelector("#mk-algo");
  const feats = (await api.get("/api/features")).features;
  const algoNames = [...new Set(feats.map(f => f.algoName))].sort();
  for (const n of algoNames) {
    const o = document.createElement("option");
    o.value = n;
    o.textContent = n;
    algoSel.appendChild(o);
  }
  if (params.algo) algoSel.value = params.algo;

  async function loadList() {
    const q = el.querySelector("#mk-q").value.trim();
    const algo = algoSel.value;
    const qs = new URLSearchParams();
    if (q) qs.set("q", q);
    if (algo) qs.set("algoName", algo);
    const resp = await api.get("/api/mistakes?" + qs.toString());
    const list = el.querySelector("#mk-list");
    if (!resp.mistakes.length) {
      list.innerHTML = `<div class="empty">暂无错题${q || algo ? "（没有匹配的记录）" : ""}。<br>可以在练习页作答后"加入错题本"，或点击右上角手动添加。</div>`;
    } else {
      list.innerHTML = `<div class="card"><h2>错题列表（${resp.mistakes.length} 条，按时间倒序）</h2><div id="mk-rows"></div></div>`;
      const rows = el.querySelector("#mk-rows");
      rows.innerHTML = resp.mistakes.map(m => `
        <div class="card" data-id="${m.id}" style="margin-top:10px;background:var(--panel)">
          <div class="row">
            <span class="badge info">${esc(m.algoName)}</span>
            <span class="badge warn">${esc(m.reason)}</span>
            <span class="hint">${esc(m.createdAt)}</span>
            <span class="spacer" style="flex:1"></span>
            <span class="hint">重做 ${m.redoCount} · 成功 ${m.successCount}${m.lastRedoAt ? " · 最近重做 " + esc(m.lastRedoAt) : ""}</span>
          </div>
          <div style="margin:8px 0"><b>题目：</b>${esc(m.question)}</div>
          <div style="color:var(--danger);font-size:13.5px"><b>当时错误答案：</b>${esc(m.wrongAnswer)}</div>
          <div class="row" style="margin-top:8px">
            <button class="btn primary" data-act="redo">重新做</button>
            <button class="btn subtle" data-act="detail">重做记录</button>
            <button class="btn subtle" data-act="edit">编辑</button>
            <button class="btn subtle" data-act="del">删除</button>
          </div>
          <div class="redo-zone"></div>
        </div>`).join("");
      rows.querySelectorAll(".card[data-id]").forEach(card => {
        const id = card.dataset.id;
        card.querySelector('[data-act="redo"]').onclick = () => startRedo(card, id);
        card.querySelector('[data-act="detail"]').onclick = () => showReviews(card, id);
        card.querySelector('[data-act="edit"]').onclick = () => openEditor(id);
        card.querySelector('[data-act="del"]').onclick = async () => {
          if (!confirm("确定删除这条错题吗？")) return;
          await api.del("/api/mistakes/" + id);
          loadList();
          loadWeak();
        };
      });
    }
  }

  /* ---- 重做模式（§15.3：先只显示题目与作答框，答案隐藏） ---- */
  async function startRedo(card, id) {
    const m = (await api.get("/api/mistakes/" + id));
    const zone = card.querySelector(".redo-zone");
    zone.innerHTML = `
      <div style="border-top:1px dashed var(--border);margin-top:10px;padding-top:10px">
        <label for="redo-${id}">重新作答（不看答案先写）</label>
        <textarea id="redo-${id}" placeholder="写出你的答案……"></textarea>
        <div class="row" style="margin-top:8px">
          <button class="btn accent" data-act="reveal">查看答案</button>
        </div>
        <div class="judge-zone" style="display:none;margin-top:10px">
          <div style="background:var(--ok-bg);border:1px solid var(--ok);border-radius:8px;padding:10px">
            <b style="color:var(--ok)">参考答案</b>：<span class="ref"></span>
          </div>
          <div class="row" style="margin-top:8px">
            <button class="btn danger" data-judge="fail">仍不会</button>
            <button class="btn" data-judge="partial">基本理解</button>
            <button class="btn primary" data-judge="success">重做成功</button>
          </div>
          <div class="hint">只有点击"重做成功"才累计成功次数。</div>
        </div>
      </div>`;
    zone.querySelector('[data-act="reveal"]').onclick = () => {
      zone.querySelector(".judge-zone").style.display = "";
      zone.querySelector(".ref").textContent = m.wrongAnswer ? "（本题参考答案即你最初记下的正确解法，见错题详情——原始记录不含标准答案时可对照题目笔记）" : "";
      // 错题表只存了 wrongAnswer；这里如实展示可用的参考信息
      zone.querySelector(".ref").innerHTML = esc(m.wrongAnswer) +
        `<div class="hint">提示：错题本保存的是"当时的错误答案"。请对照练习页/笔记里的参考答案自查。</div>`;
      zone.querySelector('[data-act="reveal"]').disabled = true;
    };
    zone.querySelectorAll("[data-judge]").forEach(b => {
      b.onclick = async () => {
        await api.post(`/api/mistakes/${id}/reviews`, {
          answer: zone.querySelector(`#redo-${id}`).value,
          result: b.dataset.judge,
        });
        zone.innerHTML = `<div class="badge ok" style="margin-top:8px">重做记录已保存：${b.textContent}</div>`;
        loadList();
        loadWeak();
      };
    });
  }

  async function showReviews(card, id) {
    const m = await api.get("/api/mistakes/" + id);
    const zone = card.querySelector(".redo-zone");
    const rows = (m.reviews || []).map(r => `<tr><td>${esc(r.createdAt)}</td><td>${esc(r.answer)}</td><td>${
      r.result === "success" ? '<span class="badge ok">成功</span>' : r.result === "partial" ? '<span class="badge warn">基本理解</span>' : '<span class="badge" style="background:var(--danger-bg);color:var(--danger)">仍不会</span>'
    }</td></tr>`).join("");
    zone.innerHTML = `<div style="border-top:1px dashed var(--border);margin-top:10px;padding-top:10px">
      ${rows ? `<table class="data"><thead><tr><th>时间</th><th>重做答案</th><th>结果</th></tr></thead><tbody>${rows}</tbody></table>` : '<div class="hint">还没有重做记录。</div>'}
    </div>`;
  }

  /* ---- 手动添加 / 编辑 ---- */
  function openEditor(id) {
    const panel = el.querySelector("#editor-panel");
    if (id) {
      api.get("/api/mistakes/" + id).then(m => {
        drawEditor(panel, {
          title: "编辑错题 #" + id, id,
          algoName: m.algoName, question: m.question, wrongAnswer: m.wrongAnswer, reason: m.reason,
        });
      });
    } else {
      drawEditor(panel, { title: "手动添加错题", algoName: algoSel.value, question: "", wrongAnswer: "", reason: "概念混淆" });
    }
  }

  function drawEditor(panel, e) {
    panel.innerHTML = `<div class="card">
      <h2>${esc(e.title)}</h2>
      <div class="field"><label for="ed-algo">知识点（算法名）</label>
        <select id="ed-algo">${algoNames.map(n => `<option ${n === e.algoName ? "selected" : ""}>${n}</option>`).join("")}</select></div>
      <div class="field"><label for="ed-q">题目</label><textarea id="ed-q">${esc(e.question)}</textarea></div>
      <div class="field"><label for="ed-w">当时的错误答案/现象</label><textarea id="ed-w">${esc(e.wrongAnswer)}</textarea></div>
      <div class="field"><label for="ed-r">错误原因</label>
        <select id="ed-r">${["概念混淆", "边界条件遗漏", "时间复杂度错误", "算法步骤错误", "Java 实现问题", "暂未理解"].map(x =>
          `<option ${x === e.reason ? "selected" : ""}>${x}</option>`).join("")}</select></div>
      <div class="err-text" id="ed-err"></div>
      <div class="row"><button class="btn primary" id="ed-save">保存</button><button class="btn subtle" id="ed-cancel">取消</button></div>
    </div>`;
    panel.querySelector("#ed-save").onclick = async () => {
      try {
        const body = {
          question: panel.querySelector("#ed-q").value,
          wrongAnswer: panel.querySelector("#ed-w").value,
          reason: panel.querySelector("#ed-r").value,
        };
        if (!body.question.trim() || !body.wrongAnswer.trim()) throw new Error("题目与错误答案不能为空");
        if (e.id) await api.put("/api/mistakes/" + e.id, body);
        else await api.post("/api/mistakes", { ...body, algoName: panel.querySelector("#ed-algo").value });
        panel.innerHTML = "";
        loadList();
        loadWeak();
      } catch (err) {
        panel.querySelector("#ed-err").textContent = err.message;
      }
    };
    panel.querySelector("#ed-cancel").onclick = () => { panel.innerHTML = ""; };
  }

  async function loadWeak() {
    const resp = await api.get("/api/weakness");
    const body = el.querySelector("#weak-body");
    if (!resp.weakness.length) {
      body.innerHTML = `<div class="empty">暂无错题数据。</div>`;
      return;
    }
    body.innerHTML = resp.weakness.slice(0, 8).map((w, i) => `
      <div class="row" style="padding:5px 0;border-bottom:1px solid var(--border)">
        <span class="badge info">${i + 1}</span>
        <a href="#/mistakes" onclick="document.getElementById('mk-q').focus()" style="flex:1">${esc(w.algoName)}</a>
        <span class="hint">错题 ${w.mistakeCount} · 重做 ${w.redoCount} · 最近成功 ${w.successCount}</span>
      </div>`).join("")
      + `<p class="hint">薄弱项只汇总学习记录，供优先复习参考；不会自动修改任何知识点的掌握状态。</p>`;
  }

  el.querySelector("#mk-search").onclick = loadList;
  el.querySelector("#mk-q").addEventListener("keydown", e => { if (e.key === "Enter") loadList(); });
  algoSel.addEventListener("change", loadList);
  el.querySelector("#mk-new").onclick = () => openEditor(null);

  await loadList();
  await loadWeak();
}

function esc(s) {
  return String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
