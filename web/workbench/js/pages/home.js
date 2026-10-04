/* 学习中心：进度汇总 / 继续学习 / 推荐路线 / 欢迎引导（计划 §5） */
"use strict";
import { api } from "../api.js";
import { navTo, session, setFeatureContext } from "../main.js";

const ROUTE_ORDER = ["线性表", "栈与队列", "哈希", "树", "图", "排序", "查找", "字符串", "动态规划"];

export async function render(el) {
  let home, features;
  try {
    [home, features] = await Promise.all([
      api.get("/api/home"),
      api.get("/api/features"),
    ]);
  } catch (e) {
    el.innerHTML = `<div class="empty">${e.message}</div>`;
    return;
  }
  const states = home.states || {};
  const ctx = home.continueContext || {};
  const counts = { unlearned: 0, reviewing: 0, mastered: 0, unset: 0 };
  for (const f of features.features) {
    const s = states[f.id] || "unlearned";
    counts[s] = (counts[s] || 0) + 1;
  }
  const hasActivity = (home.recentRuns || []).length > 0 || home.mistakeCount > 0
    || counts.reviewing + counts.mastered > 0;

  if (!hasActivity) {
    el.innerHTML = `
      <div class="card" style="text-align:center;padding:44px 20px">
        <h2 style="font-size:22px;margin-bottom:6px">欢迎使用 JavaDS-Lab</h2>
        <p style="color:var(--muted)">这里是你学习 Java 数据结构与算法的完整环境：理解 → 练习 → 验证 → 纠错 → 复习。</p>
        <div class="row" style="justify-content:center;margin:18px 0">
          <button class="btn primary" id="start-first">从动态数组开始</button>
          <button class="btn" id="start-notes">先浏览学习笔记</button>
        </div>
        <div class="grid2" style="max-width:640px;margin:18px auto 0;text-align:left">
          <div class="card" style="margin:0">① <b>阅读知识点</b>：在笔记区阅读原理与图解</div>
          <div class="card" style="margin:0">② <b>完成自测</b>：主动回忆，对照参考答案自评</div>
          <div class="card" style="margin:0">③ <b>用实验验证</b>：运行真实 Java 实现，回放 Trace</div>
          <div class="card" style="margin:0">④ <b>记录错题</b>：错题会进入错题本供你重做复习</div>
        </div>
      </div>`;
    el.querySelector("#start-first").onclick = async () => {
      await setFeatureContext("arraylist", "动态数组");
      navTo("notes", { note: "01" });
    };
    el.querySelector("#start-notes").onclick = () => navTo("notes");
    return;
  }

  const ctxHtml = ctx.featureId ? `
    <div class="card">
      <h2>继续学习</h2>
      <div class="row">
        <div class="grow">
          <b>${ctx.note || ctx.featureId}</b>
          <span class="hint">　上次访问：${(ctx.savedAt || "").replace("T", " ").slice(0, 16)}</span>
        </div>
        <button class="btn primary" id="btn-continue">继续学习</button>
      </div>
    </div>` : "";

  el.innerHTML = `
    <div class="grid2">
      ${ctxHtml || "<div></div>"}
      <div class="card">
        <h2>学习状态</h2>
        <div class="row" style="gap:18px">
          <span>共 <b>${home.featureCount}</b> 个知识点</span>
          <span class="badge st-unlearned">未学 ${counts.unlearned}</span>
          <span class="badge st-reviewing">复习中 ${counts.reviewing}</span>
          <span class="badge st-mastered">已掌握 ${counts.mastered}</span>
        </div>
        <div class="row" style="margin-top:8px;gap:18px">
          <span>错题本 <b>${home.mistakeCount}</b> 条</span>
          <span>练习题 <b>${home.practiceCount}</b> 道</span>
        </div>
      </div>
    </div>
    <div class="grid2">
      <div class="card">
        <h2>最近实验</h2>
        <div id="recent-runs"></div>
      </div>
      <div class="card">
        <h2>薄弱知识点（按错题数）</h2>
        <div id="weak-list"></div>
      </div>
    </div>
    <div class="card">
      <h2>推荐学习路线</h2>
      <p class="hint">按课程结构排列，点击任意知识点开始学习。</p>
      <div id="route"></div>
    </div>`;

  // 最近实验
  const runs = home.recentRuns || [];
  const runsEl = el.querySelector("#recent-runs");
  if (!runs.length) runsEl.innerHTML = `<div class="empty">暂无实验记录。去 <a href="#/lab">数据实验场</a> 运行第一个算法吧。</div>`;
  else {
    runsEl.innerHTML = runs.map(r => `
      <div class="row" style="padding:5px 0;border-bottom:1px solid var(--border)">
        <a href="#/lab?feature=${r.featureId}&run=${r.runId}">${r.featureId}</a>
        <span class="hint">${r.createdAt} · ${r.source === "user" ? "手动运行" : r.source}</span>
      </div>`).join("");
  }
  // 薄弱项（只描述记录，不改学习状态 §16）
  const weak = home.weakness || [];
  const weakEl = el.querySelector("#weak-list");
  if (!weak.length) weakEl.innerHTML = `<div class="empty">暂无错题记录，很好的开始！</div>`;
  else weakEl.innerHTML = weak.map(w => `
    <div class="row" style="padding:5px 0;border-bottom:1px solid var(--border)">
      <a href="#/mistakes?algo=${encodeURIComponent(w.algoName)}">${w.algoName}</a>
      <span class="badge warn">错题 ${w.mistakeCount}</span>
      <span class="hint">重做 ${w.redoCount} 次 · 成功 ${w.successCount} 次</span>
    </div>`).join("");
  // 推荐路线（固定课程顺序 §5.3）
  const byCat = {};
  for (const f of features.features) (byCat[f.category] = byCat[f.category] || []).push(f);
  const routeEl = el.querySelector("#route");
  routeEl.innerHTML = ROUTE_ORDER.map((cat, i) => `
    <div style="margin-bottom:10px">
      <span class="badge info">${i + 1}. ${cat}</span>
      <div class="row" style="margin-top:6px">
        ${(byCat[cat] || []).map(f => {
          const s = states[f.id] || "unlearned";
          return `<a class="btn subtle" href="#/lab?feature=${f.id}" title="${f.description}">${f.name}
            <span class="badge st-${s === "unlearned" ? "unlearned" : s}">${s === "unlearned" ? "未学" : s === "reviewing" ? "复习中" : "已掌握"}</span></a>`;
        }).join("")}
      </div>
    </div>`).join("");

  const btn = el.querySelector("#btn-continue");
  if (btn) btn.onclick = () => { location.hash = ctx.page && ctx.page.startsWith("#/") ? ctx.page : "#/lab?feature=" + ctx.featureId; };

  return null;
}
