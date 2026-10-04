/* 学习工作台主入口：Hash Router + 左侧导航 + 顶部上下文栏（计划 §1.2/§2.2）
   页面：#/home #/practice #/lab #/mistakes #/notes #/review #/database #/verify
   URL 可携带上下文（如 #/lab?feature=sort-quick），刷新/前进/后退均保持（§30）。 */
"use strict";
import { api, onReconnect } from "./api.js";
import * as home from "./pages/home.js";
import * as practice from "./pages/practice.js";
import * as lab from "./pages/lab.js";
import * as mistakes from "./pages/mistakes.js";
import * as notes from "./pages/notes.js";
import * as review from "./pages/review.js";
import * as database from "./pages/database.js";
import * as verify from "./pages/verify.js";

const ROUTES = {
  home: { title: "学习中心", mod: home },
  practice: { title: "自测练习", mod: practice },
  lab: { title: "数据实验场", mod: lab },
  mistakes: { title: "错题本", mod: mistakes },
  notes: { title: "学习笔记", mod: notes },
  review: { title: "复习地图", mod: review },
  database: { title: "数据库实验", mod: database },
  verify: { title: "项目验证", mod: verify },
};
const ORDER = ["home", "practice", "lab", "mistakes", "notes", "review", "database", "verify"];
const GROUP2 = new Set(["notes", "review", "database", "verify"]);
/** 当前学习上下文（跨页面共享，如正在看的知识点）。 */
export const session = { featureId: null, featureName: null };

function buildNav() {
  const nav = document.getElementById("nav-links");
  nav.innerHTML = "";
  for (const key of ORDER) {
    if (key === "notes") {
      const lbl = document.createElement("div");
      lbl.className = "group-label";
      lbl.textContent = "辅助功能";
      nav.appendChild(lbl);
    }
    const a = document.createElement("a");
    a.href = "#/" + key;
    a.dataset.page = key;
    a.textContent = ROUTES[key].title;
    nav.appendChild(a);
  }
}

export function navTo(page, params) {
  const q = params ? Object.entries(params).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join("&") : "";
  location.hash = "#/" + page + (q ? "?" + q : "");
}

export async function setFeatureContext(featureId, featureName) {
  session.featureId = featureId;
  session.featureName = featureName || featureId;
  updateContextBar();
  // 记录"继续学习"上下文（§5.2），静默失败不影响使用
  try {
    await api.put("/api/context", { featureId, page: location.hash, note: featureName || "" });
  } catch { /* 忽略 */ }
}
function updateContextBar() {
  const el = document.getElementById("ctx-feature");
  el.textContent = session.featureId ? "当前知识点：" + session.featureName : "";
  el.style.display = session.featureId ? "" : "none";
}

let currentCleanup = null;
async function route() {
  const hash = location.hash || "#/home";
  const m = hash.match(/^#\/([a-z]+)(\?(.*))?$/);
  const page = m && ROUTES[m[1]] ? m[1] : "home";
  const params = {};
  if (m && m[3]) {
    for (const pair of m[3].split("&")) {
      const i = pair.indexOf("=");
      if (i > 0) params[decodeURIComponent(pair.slice(0, i))] = decodeURIComponent(pair.slice(i + 1));
    }
  }
  if (currentCleanup) { try { currentCleanup(); } catch { /* 忽略 */ } currentCleanup = null; }
  document.querySelectorAll("#nav-links a").forEach(a => a.classList.toggle("active", a.dataset.page === page));
  document.getElementById("module-title").textContent = ROUTES[page].title;
  const content = document.getElementById("page-content");
  content.innerHTML = "";
  const div = document.createElement("div");
  div.className = "page";
  content.appendChild(div);
  try {
    currentCleanup = await ROUTES[page].mod.render(div, params) || null;
  } catch (e) {
    div.innerHTML = `<div class="empty">页面加载失败：${e.message}<br><br><button class="btn" onclick="location.reload()">重新加载</button></div>`;
  }
  updateContextBar();
}

async function boot() {
  buildNav();
  document.getElementById("reconnect-btn").addEventListener("click", () => {
    location.reload();
  });
  onReconnect(() => {});
  window.addEventListener("hashchange", route);
  await route();
}

boot();
