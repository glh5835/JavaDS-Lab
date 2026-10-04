/* 复习地图：知识点 + 前置关系 + 学习状态 + 练习/错题数量（§18）
   图谱只做导航与复习工具；掌握状态由用户主动设置（§5.4），系统不自动推断。 */
"use strict";
import { api } from "../api.js";
import { navTo, setFeatureContext } from "../main.js";

const STATE_LABEL = { unlearned: "未学", reviewing: "复习中", mastered: "已掌握" };

export async function render(el) {
  let map, notes;
  try {
    [map, notes] = await Promise.all([api.get("/api/review/map"), api.get("/api/notes")]);
  } catch (e) {
    el.innerHTML = `<div class="empty">${e.message}</div>`;
    return;
  }
  const noteTitles = {};
  for (const n of notes.notes) noteTitles[n.noteId] = n.title;

  const cats = [...new Set(map.features.map(f => f.category))];
  el.innerHTML = `
    <div class="card">
      <h2>知识点关系与复习导航</h2>
      <p class="hint">按课程分类分组；每个知识点标注前置关系、你的学习状态、练习与错题数量。点击卡片打开快捷操作。</p>
      <div class="row">
        <span class="badge st-unlearned">未学</span>
        <span class="badge st-reviewing">复习中</span>
        <span class="badge st-mastered">已掌握</span>
      </div>
    </div>
    <div id="map-body"></div>
    <div class="card" id="pop-card" style="display:none;position:sticky;bottom:8px;border-color:var(--primary)"></div>`;

  const body = el.querySelector("#map-body");
  body.innerHTML = cats.map(cat => `
    <div class="card">
      <h2>${esc(cat)}</h2>
      <div class="grid3">
        ${map.features.filter(f => f.category === cat).map(f => {
          const st = f.learningState || "unlearned";
          return `<div class="card" data-fid="${f.id}" style="margin:0;cursor:pointer;${st === "mastered" ? "border-color:var(--ok)" : st === "reviewing" ? "border-color:var(--warn)" : ""}">
            <div class="row">
              <b style="flex:1">${esc(f.name)}</b>
              <span class="badge st-${st}">${STATE_LABEL[st]}</span>
            </div>
            <div class="hint" style="margin:4px 0">前置：${f.prerequisites.length ? f.prerequisites.map(p => map.features.find(x => x.id === p)?.name || p).join("、") : "无"}</div>
            <div class="row">
              <span class="badge">练习 ${f.practiceCount} 道</span>
              ${f.mistakeCount > 0 ? `<span class="badge warn">错题 ${f.mistakeCount}</span>` : `<span class="badge">错题 0</span>`}
              <span class="badge">${(noteTitles[f.noteId] || f.noteId)}</span>
            </div>
          </div>`;
        }).join("")}
      </div>
    </div>`).join("");

  const pop = el.querySelector("#pop-card");
  body.querySelectorAll("[data-fid]").forEach(card => {
    card.addEventListener("click", () => {
      const f = map.features.find(x => x.id === card.dataset.fid);
      const noteId = f.noteId;
      pop.style.display = "";
      pop.innerHTML = `
        <div class="row">
          <h2 style="margin:0">${esc(f.name)}</h2>
          <span class="badge st-${f.learningState}">${STATE_LABEL[f.learningState || "unlearned"]}</span>
          <span class="spacer" style="flex:1"></span>
          <button class="btn subtle" id="pop-close">收起</button>
        </div>
        <div class="row" style="margin-top:8px">
          <button class="btn accent" data-go="notes">看笔记</button>
          <button class="btn" data-go="practice">做练习</button>
          <button class="btn" data-go="lab">去实验</button>
          <button class="btn" data-go="mistakes">看错题</button>
          <span style="border-left:1px solid var(--border);margin:0 6px"></span>
          <span class="hint">设置学习状态：</span>
          <button class="btn subtle" data-st="unlearned">未学</button>
          <button class="btn subtle" data-st="reviewing">复习中</button>
          <button class="btn subtle" data-st="mastered">已掌握</button>
        </div>`;
      pop.querySelector("#pop-close").onclick = () => { pop.style.display = "none"; };
      pop.querySelectorAll("[data-go]").forEach(b => {
        b.onclick = async () => {
          if (b.dataset.go === "notes") { await setFeatureContext(f.id, f.name); navTo("notes", { note: noteId }); }
          else if (b.dataset.go === "practice") { await setFeatureContext(f.id, f.name); navTo("practice", { featureId: f.id }); }
          else if (b.dataset.go === "lab") navTo("lab", { feature: f.id });
          else navTo("mistakes", { algo: f.algoName || "" });
        };
      });
      pop.querySelectorAll("[data-st]").forEach(b => {
        b.onclick = async () => {
          await api.put("/api/learning/" + encodeURIComponent(f.id), { status: b.dataset.st });
          f.learningState = b.dataset.st;
          card.querySelector(".badge").className = `badge st-${b.dataset.st}`;
          card.querySelector(".badge").textContent = STATE_LABEL[b.dataset.st];
          pop.querySelector(".badge").className = `badge st-${b.dataset.st}`;
          pop.querySelector(".badge").textContent = STATE_LABEL[b.dataset.st];
          if (b.dataset.st === "reviewing" || b.dataset.st === "mastered") {
            await api.put("/api/context", { featureId: f.id, page: "#/review", note: f.name });
          }
        };
      });
      pop.scrollIntoView({ behavior: "smooth", block: "nearest" });
    });
  });
}

function esc(s) {
  return String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
