/* 学习笔记：统一阅读 22 篇现有 Markdown 笔记（§17）
   优先读取原文件（服务端 /api/notes 读取 docs/notes/ 原文，不复制第二份）。
   联动：去练习 / 去实验 / 查看相关错题。 */
"use strict";
import { api } from "../api.js";
import { renderMarkdown } from "../md.js";
import { navTo, setFeatureContext } from "../main.js";

export async function render(el, params) {
  let notes;
  try {
    notes = (await api.get("/api/notes")).notes;
  } catch (e) {
    el.innerHTML = `<div class="empty">${e.message}</div>`;
    return;
  }
  el.innerHTML = `
    <div class="grid2" style="grid-template-columns:250px 1fr;align-items:start">
      <div class="card" style="position:sticky;top:10px">
        <h2>笔记目录</h2>
        <input type="text" id="n-q" class="grow" placeholder="搜索…" style="width:100%" aria-label="搜索笔记">
        <div id="note-list" style="margin-top:8px;max-height:66vh;overflow:auto"></div>
      </div>
      <div>
        <div class="card" id="note-actions" style="display:none">
          <div class="row" id="na-rows"></div>
        </div>
        <div class="card md" id="note-content"><div class="empty">从左侧选择一篇笔记开始阅读。</div></div>
      </div>
    </div>`;

  let current = null;

  function drawList(kw) {
    const list = el.querySelector("#note-list");
    list.innerHTML = notes.filter(n => !kw || (n.title + n.noteId).toLowerCase().includes(kw.toLowerCase()))
      .map(n => `<a href="#/notes?note=${n.noteId}" class="row" style="padding:5px 8px;border-radius:6px;${current === n.noteId ? "background:var(--primary-weak)" : ""}">
        <span style="flex:1;font-size:13px">${n.noteId} ${esc(n.title)}</span>
        <span class="badge">${n.featureIds.length} 知识点</span>
      </a>`).join("");
  }
  el.querySelector("#n-q").addEventListener("input", e => drawList(e.target.value));

  async function open(noteId) {
    current = noteId;
    drawList(el.querySelector("#n-q").value);
    try {
      const n = await api.get("/api/notes/" + noteId);
      el.querySelector("#note-content").innerHTML = renderMarkdown(n.markdown);
      const acts = el.querySelector("#note-actions");
      acts.style.display = "";
      acts.querySelector("#na-rows").innerHTML =
        (n.featureIds.length ? `<span class="badge info">覆盖知识点：${n.featureIds.join("、")}</span>` : "")
        + n.featureIds.slice(0, 1).map(() => `<button class="btn accent" data-act="practice">去做练习</button>
          <button class="btn" data-act="lab">去实验验证</button>
          <button class="btn" data-act="mistakes">相关错题</button>`).join("");
      const firstFeature = n.featureIds[0];
      const feats = (await api.get("/api/features?category=&q=")).features;
      const fdef = feats.find(f => f.id === firstFeature);
      acts.querySelector('[data-act="practice"]').onclick = async () => {
        if (fdef) await setFeatureContext(fdef.id, fdef.name);
        navTo("practice", { featureId: firstFeature });
      };
      acts.querySelector('[data-act="lab"]').onclick = async () => {
        if (fdef) await setFeatureContext(fdef.id, fdef.name);
        navTo("lab", { feature: firstFeature });
      };
      acts.querySelector('[data-act="mistakes"]').onclick = () => navTo("mistakes", { algo: fdef ? fdef.algoName : "" });
    } catch (e) {
      el.querySelector("#note-content").innerHTML = `<div class="empty">笔记加载失败：${esc(e.message)}</div>`;
    }
  }

  drawList("");
  if (params.note) await open(params.note);
}

function esc(s) {
  return String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
