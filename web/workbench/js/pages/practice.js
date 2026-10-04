/* 自测练习：主动回忆优先，答案默认隐藏（计划 §6）
   流程：读题 → 输入答案 → 查看参考答案（记录 revealed）→ 对照 → 自评（不会/模糊/掌握）
   联动：查看原理 → 笔记；去实验验证 → 实验场；加入错题本。草稿自动保存（§6.6）。 */
"use strict";
import { api } from "../api.js";
import { navTo, setFeatureContext } from "../main.js";

let state = { practices: [], drafts: {}, currentId: null, revealed: false, timer: null };

export async function render(el, params) {
  el.innerHTML = `
    <div class="card">
      <div class="row">
        <select id="f-feature" style="min-width:180px" aria-label="按知识点筛选"><option value="">全部知识点</option></select>
        <input type="text" id="f-q" class="grow" placeholder="搜索题目关键词…" aria-label="搜索">
        <button class="btn" id="f-search">搜索</button>
      </div>
      <div class="hint" id="f-count" style="margin-top:6px"></div>
    </div>
    <div class="grid2" style="grid-template-columns:5fr 7fr;align-items:start">
      <div class="card" id="list-panel"><div class="hint">加载中……</div></div>
      <div class="card" id="detail-panel"><div class="empty">从左侧选择一道练习题开始。<br><br>先自己作答，再对照参考答案。</div></div>
    </div>`;

  const featureSel = el.querySelector("#f-feature");
  const feats = (await api.get("/api/features")).features;
  for (const f of feats) {
    const o = document.createElement("option");
    o.value = f.id;
    o.textContent = `${f.category} / ${f.name}`;
    featureSel.appendChild(o);
  }

  const draftsResp = await api.get("/api/practice-drafts");
  state.drafts = {};
  for (const d of draftsResp.drafts) state.drafts[d.practiceId] = d;

  async function loadList() {
    const fid = featureSel.value;
    const q = el.querySelector("#f-q").value.trim();
    const qs = new URLSearchParams();
    if (fid) qs.set("featureId", fid);
    if (q) qs.set("q", q);
    const resp = await api.get("/api/practices?" + qs.toString());
    state.practices = resp.practices;
    const listEl = el.querySelector("#list-panel");
    if (!resp.total) {
      listEl.innerHTML = `<div class="empty">没有匹配的练习题。换个关键词试试。</div>`;
      return;
    }
    listEl.innerHTML = resp.practices.map(p => {
      const draft = state.drafts[p.practiceId];
      const done = draft && draft.answer ? "已作答草稿" : "";
      return `<div class="row" data-pid="${p.practiceId}" style="padding:8px 6px;border-bottom:1px solid var(--border);cursor:pointer;${p.practiceId === state.currentId ? "background:var(--primary-weak);border-radius:6px" : ""}">
        <span class="badge info">${p.noteId} 笔记 · 第 ${p.index} 题</span>
        <span class="grow" style="font-size:13.5px">${esc(p.question.length > 42 ? p.question.slice(0, 42) + "…" : p.question)}</span>
        ${done ? `<span class="badge warn">草稿</span>` : ""}
      </div>`;
    }).join("");
    el.querySelector("#f-count").textContent = `共 ${resp.total} 道题` + (fid ? "（按知识点过滤）" : "") + (q ? `（搜索"${q}"）` : "");
    listEl.querySelectorAll("[data-pid]").forEach(row => {
      row.addEventListener("click", () => openPractice(row.dataset.pid));
    });
  }

  async function openPractice(pid) {
    state.currentId = pid;
    state.revealed = false;
    const detail = await api.get("/api/practices/" + pid);
    const draft = state.drafts[pid] || { answer: "", revealed: false };
    if (draft.revealed) state.revealed = true;
    renderDetail(detail, draft.answer || "");
    loadList();
  }

  function renderDetail(p, savedAnswer) {
    const panel = el.querySelector("#detail-panel");
    panel.innerHTML = `
      <h2>练习 ${p.practiceId}</h2>
      <div class="row"><span class="badge info">来源：笔记 ${p.noteId}《${p.noteTitle}》第 ${p.index} 题</span></div>
      <div style="margin:12px 0;font-size:16px"><b>题目：</b>${esc(p.question).replace(/\n/g, "<br>")}</div>
      <div class="field">
        <label for="answer-box">你的答案（先自己写，再对照参考答案）</label>
        <textarea id="answer-box" placeholder="用自己的话写出答案/推导过程…">${esc(savedAnswer)}</textarea>
      </div>
      <div class="row">
        <button class="btn accent" id="btn-reveal" ${state.revealed ? "disabled" : ""}>查看参考答案</button>
        <button class="btn" id="btn-note">查看原理（笔记）</button>
        <button class="btn" id="btn-lab">去实验验证</button>
        <button class="btn danger" id="btn-mistake" disabled>加入错题本</button>
      </div>
      <div id="answer-area" style="display:none;margin-top:12px">
        <div class="card" style="background:var(--ok-bg);border-color:var(--ok)">
          <h3 style="color:var(--ok)">参考答案</h3>
          <div id="ref-answer" style="font-size:14px"></div>
        </div>
        <h3 style="margin-top:12px">对照后自评</h3>
        <div class="row">
          <button class="btn" data-rate="unknown">不会</button>
          <button class="btn" data-rate="fuzzy">模糊</button>
          <button class="btn primary" data-rate="mastered">掌握</button>
        </div>
        <div class="hint" style="margin-top:6px">自评会保存为一条练习记录；自评后草稿自动清除。</div>
      </div>
      <div id="mistake-area" style="display:none;margin-top:12px">
        <div class="card" style="border-color:var(--warn)">
          <h3 style="color:var(--warn)">加入错题本</h3>
          <div class="field"><label for="mk-reason">错误原因（必选）</label>
            <select id="mk-reason">
              <option value="概念混淆">概念混淆</option>
              <option value="边界条件遗漏">边界条件遗漏</option>
              <option value="时间复杂度错误">时间复杂度错误</option>
              <option value="算法步骤错误">算法步骤错误</option>
              <option value="Java 实现问题">Java 实现问题</option>
              <option value="暂未理解">暂未理解</option>
            </select>
          </div>
          <div class="row">
            <button class="btn primary" id="mk-save">保存到错题本</button>
            <button class="btn subtle" id="mk-cancel">取消</button>
          </div>
          <div class="err-text" id="mk-err"></div>
        </div>
      </div>`;

    const answerBox = panel.querySelector("#answer-box");
    answerBox.addEventListener("input", () => scheduleDraftSave(p.practiceId, answerBox.value, state.revealed));

    panel.querySelector("#btn-note").onclick = async () => {
      await setFeatureContext(p.featureId, p.noteTitle);
      navTo("notes", { note: p.noteId, back: "practice" });
    };
    panel.querySelector("#btn-lab").onclick = async () => {
      await setFeatureContext(p.featureId, p.noteTitle);
      navTo("lab", { feature: p.featureId });
    };

    panel.querySelector("#btn-reveal").onclick = async () => {
      const full = await api.get("/api/practices/" + p.practiceId); // 详情含答案
      state.revealed = true;
      panel.querySelector("#answer-area").style.display = "";
      panel.querySelector("#ref-answer").innerHTML = esc(full.answer).replace(/\n/g, "<br>");
      const rb = panel.querySelector("#btn-reveal");
      rb.disabled = true;
      scheduleDraftSave(p.practiceId, answerBox.value, true);
    };

    panel.querySelectorAll("[data-rate]").forEach(b => {
      b.onclick = async () => {
        await api.post("/api/practice-attempts", {
          practiceId: p.practiceId,
          featureId: p.featureId,
          answer: answerBox.value,
          revealed: state.revealed,
          selfRating: b.dataset.rate,
        });
        delete state.drafts[p.practiceId];
        panel.querySelector("#answer-area").insertAdjacentHTML("beforeend",
          `<div class="badge ok">已记录自评：${b.textContent}　<button class="btn subtle" id="rate-next">下一题 →</button></div>`);
        panel.querySelector("#rate-next").onclick = () => {
          const idx = state.practices.findIndex(x => x.practiceId === p.practiceId);
          const next = state.practices[idx + 1];
          if (next) openPractice(next.practiceId); else loadList();
        };
        loadList();
      };
    });

    // 加入错题本（§6.4：自动携带题目/用户答案/参考答案/知识点）
    panel.querySelector("#btn-mistake").disabled = false;
    panel.querySelector("#btn-mistake").onclick = () => {
      panel.querySelector("#mistake-area").style.display = "";
      panel.querySelector("#mk-save").onclick = async () => {
        try {
          await api.post("/api/mistakes", {
            featureId: p.featureId,
            question: p.question,
            wrongAnswer: answerBox.value || "（未作答）",
            reason: panel.querySelector("#mk-reason").value,
          });
          panel.querySelector("#mistake-area").innerHTML =
            `<div class="badge ok">已加入错题本。去 <a href="#/mistakes">错题本</a> 查看重做。</div>`;
        } catch (e) {
          panel.querySelector("#mk-err").textContent = e.message;
        }
      };
      panel.querySelector("#mk-cancel").onclick = () => {
        panel.querySelector("#mistake-area").style.display = "none";
      };
    };
  }

  function scheduleDraftSave(pid, answer, revealed) {
    clearTimeout(state.timer);
    state.timer = setTimeout(async () => {
      try {
        await api.put("/api/practice-drafts/" + pid, { answer, revealed });
        state.drafts[pid] = { practiceId: pid, answer, revealed };
      } catch { /* 草稿保存失败不打断作答 */ }
    }, 600);
  }

  featureSel.addEventListener("change", loadList);
  el.querySelector("#f-search").addEventListener("click", loadList);
  el.querySelector("#f-q").addEventListener("keydown", e => { if (e.key === "Enter") loadList(); });

  // 支持从笔记页带 practice 定位
  if (params.practice) await openPractice(params.practice);
  else await loadList();
}

function esc(s) {
  return String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
