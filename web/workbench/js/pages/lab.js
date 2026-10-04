/* 数据实验场：调用项目真实 Java 实现（计划 §7~§14）
   - 左侧目录（分类/搜索/最近使用），数量读自功能目录 API，不硬编码（§8）；
   - 输入表单按 inputType 生成（§9），样例一键填充；
   - Result 模式全部支持；有 Trace 的功能可播放回放（§12/§13）；
   - 区分"算法耗时"与"Trace 记录耗时"（§11.3）；运行历史可重放（§14）。 */
"use strict";
import { api } from "../api.js";
import { navTo, setFeatureContext } from "../main.js";
import { createPlayer } from "../player.js";

let catalog = null;
let current = { def: null, lastInput: null, player: null, lastRunId: null };
let el = null; // 页面根元素

export async function render(elRoot, params) {
  el = elRoot;
  if (!catalog) {
    try {
      catalog = (await api.get("/api/features")).features;
    } catch (e) {
      el.innerHTML = `<div class="empty">${e.message}</div>`;
      return;
    }
  }

  el.innerHTML = `
    <div class="grid2" style="grid-template-columns:270px 1fr;align-items:start">
      <div class="card" style="position:sticky;top:10px">
        <h2>功能目录</h2>
        <div class="row"><input type="text" id="cat-q" class="grow" placeholder="搜索算法…" aria-label="搜索"></div>
        <div id="cat-list" style="margin-top:10px;max-height:64vh;overflow:auto"></div>
      </div>
      <div>
        <div class="card" id="feature-head"><div class="empty">从左侧选择一个算法或数据结构开始实验。</div></div>
        <div id="feature-body"></div>
      </div>
    </div>`;

  renderCatalog("");
  el.querySelector("#cat-q").addEventListener("input", e => renderCatalog(e.target.value));

  if (params.feature) {
    await openFeature(params.feature, params.run);
  }
}

function renderCatalog(kw) {
  const list = el.querySelector("#cat-list");
  const cats = [...new Set(catalog.map(f => f.category))];
  list.innerHTML = cats.map(cat => {
    const feats = catalog.filter(f => f.category === cat
      && (!kw || (f.name + f.id + f.description).toLowerCase().includes(kw.toLowerCase())));
    if (!feats.length) return "";
    return `<div style="margin-bottom:8px">
      <div class="group-label" style="padding:2px 4px">${cat}</div>
      ${feats.map(f => `<a href="#/lab?feature=${f.id}" class="row" style="padding:4px 8px;border-radius:6px;${current.def && current.def.id === f.id ? "background:var(--primary-weak)" : ""}">
        <span style="flex:1;font-size:13.5px">${f.name}</span>
        ${f.traceSupported ? '<span class="badge ok" title="支持 Trace 回放">T</span>' : ""}
      </a>`).join("")}
    </div>`;
  }).join("");
}

async function openFeature(featureId, runId) {
  const def = catalog.find(f => f.id === featureId);
  if (!def) { el.querySelector("#feature-head").innerHTML = `<div class="empty">未知功能：${featureId}</div>`; return; }
  current.def = def;
  current.lastRunId = runId || null;
  await setFeatureContext(def.id, def.name);
  renderCatalog(el.querySelector("#cat-q").value);
  renderHead();
  renderForm();
  if (runId) await loadRun(runId);
}

function renderHead() {
  const d = current.def;
  el.querySelector("#feature-head").innerHTML = `
    <div class="row">
      <h2 style="margin:0">${d.name}</h2>
      <span class="badge info">${d.category}</span>
      ${d.traceSupported ? '<span class="badge ok">支持 Trace 回放</span>' : '<span class="badge warn">仅结果模式</span>'}
      <span class="spacer" style="flex:1"></span>
      <a class="btn subtle" href="#/notes?note=${d.noteId}">查看原理（笔记）</a>
      <a class="btn subtle" href="#/mistakes?algo=${encodeURIComponent(d.algoName)}">相关错题</a>
    </div>
    <p class="hint" style="margin:6px 0 0">${d.description} · Java 实现：${d.javaClass}</p>`;
}

/* ---------- 按输入类型生成表单 ---------- */
function formFields(def) {
  const t = def.inputType;
  const sample = def.sampleInput || "";
  switch (t) {
    case "array": return `
      <div class="field"><label for="in-array">整数数组（逗号或空格分隔）</label>
      <textarea id="in-array" placeholder="如：5, 1, 8, 3, 2">${esc(sample)}</textarea></div>`;
    case "array-target": {
      const arr = (sample.match(/array:\s*(.*)/) || [])[1] || "";
      const tgt = (sample.match(/target:\s*(.*)/) || [])[1] || "";
      return `
      <div class="field"><label for="in-array">升序整数数组（二分查找要求已按升序排列）</label>
      <textarea id="in-array">${esc(arr.trim())}</textarea></div>
      <div class="field"><label for="in-target">目标值</label>
      <input type="number" id="in-target" value="${esc(tgt.trim())}" style="width:140px"></div>`;
    }
    case "text-pattern": {
      const text = (sample.match(/text:\s*(.*)/) || [])[1] || "";
      const pat = (sample.match(/pattern:\s*(.*)/) || [])[1] || "";
      return `
      <div class="field"><label for="in-text">文本</label>
      <input type="text" id="in-text" value="${esc(text.trim())}"></div>
      <div class="field"><label for="in-pattern">模式串</label>
      <input type="text" id="in-pattern" value="${esc(pat.trim())}"></div>`;
    }
    case "tree-array": return `
      <div class="field"><label for="in-array">层序整数序列（按完全二叉树建树）</label>
      <textarea id="in-array">${esc(sample)}</textarea></div>`;
    case "dp-n": {
      const n = (sample.match(/n:\s*(.*)/) || [])[1] || "8";
      return `<div class="field"><label for="in-n">阶梯数 n</label>
      <input type="number" id="in-n" value="${esc(n.trim())}" style="width:140px"></div>`;
    }
    case "coins-amount": {
      const coins = (sample.match(/coins:\s*(.*)/) || [])[1] || "";
      const amt = (sample.match(/amount:\s*(.*)/) || [])[1] || "";
      return `
      <div class="field"><label for="in-coins">硬币面值数组</label>
      <textarea id="in-coins">${esc(coins.trim())}</textarea></div>
      <div class="field"><label for="in-amount">目标金额</label>
      <input type="number" id="in-amount" value="${esc(amt.trim())}" style="width:140px"></div>`;
    }
    case "two-strings": {
      const s1 = (sample.match(/s1:\s*(.*)/) || [])[1] || "";
      const s2 = (sample.match(/s2:\s*(.*)/) || [])[1] || "";
      return `
      <div class="field"><label for="in-s1">字符串 s1</label><input type="text" id="in-s1" value="${esc(s1.trim())}"></div>
      <div class="field"><label for="in-s2">字符串 s2</label><input type="text" id="in-s2" value="${esc(s2.trim())}"></div>`;
    }
    case "knapsack": {
      const w = (sample.match(/weights:\s*(.*)/) || [])[1] || "";
      const v = (sample.match(/values:\s*(.*)/) || [])[1] || "";
      const c = (sample.match(/capacity:\s*(.*)/) || [])[1] || "";
      return `
      <div class="field"><label for="in-weights">物品重量数组</label><textarea id="in-weights">${esc(w.trim())}</textarea></div>
      <div class="field"><label for="in-values">物品价值数组</label><textarea id="in-values">${esc(v.trim())}</textarea></div>
      <div class="field"><label for="in-cap">背包容量</label><input type="number" id="in-cap" value="${esc(c.trim())}" style="width:140px"></div>`;
    }
    case "graph": case "graph-neg": {
      const n = (sample.match(/n\s+(\d+)/) || [])[1] || "6";
      const dir = /directed\s+true/.test(sample);
      const start = (sample.match(/start\s+(\d+)/) || [])[1];
      const edges = (sample.split(/edges\s*\n/)[1] || "").trim();
      return `
      <div class="row">
        <div class="field"><label for="in-n">顶点数 n</label><input type="number" id="in-n" value="${esc(n)}" style="width:110px"></div>
        <div class="field"><label for="in-dir">有向图</label><input type="checkbox" id="in-dir" ${dir ? "checked" : ""} style="margin-top:8px"></div>
        ${start !== undefined ? `<div class="field"><label for="in-start">起点</label><input type="number" id="in-start" value="${esc(start)}" style="width:110px"></div>` : ""}
      </div>
      <div class="field"><label for="in-edges">边列表（每行一条：u v w${t === "graph-neg" ? "，允许负权" : "，权值非负"}）</label>
      <textarea id="in-edges">${esc(edges)}</textarea></div>`;
    }
    case "ops": {
      const capM = sample.match(/capacity\s+(\d+)/);
      const initM = sample.match(/init\s+(.*)/);
      const ops = sample.split("\n").filter(l => !/^(capacity|init|n)\s/.test(l)).join("\n");
      const needCap = /capacity \d+/.test(sample) || ["circular-queue", "hash-chaining", "hash-open-addressing"].includes(def.id);
      const needInit = ["segment-tree", "fenwick-tree"].includes(def.id);
      const needN = def.id === "union-find";
      return `
      ${needCap ? `<div class="field"><label for="in-capacity">容量（2~64）</label><input type="number" id="in-capacity" value="${capM ? esc(capM[1]) : "8"}" style="width:110px"></div>` : ""}
      ${needN ? `<div class="field"><label for="in-capacity">元素个数 n（1~1000）</label><input type="number" id="in-capacity" value="${capM ? esc(capM[1]) : "8"}" style="width:110px"></div>` : ""}
      ${needInit ? `<div class="field"><label for="in-init">初始数组</label><textarea id="in-init">${esc(initM ? initM[1].trim() : "")}</textarea></div>` : ""}
      <div class="field"><label for="in-ops">操作序列（每行一条）</label>
      <textarea id="in-ops" style="min-height:130px">${esc(ops)}</textarea>
      <div class="hint">支持的操作见占位样例；常用：${opsHint(def.id)}</div></div>`;
    }
    default: return `<div class="empty">该功能暂未配置输入表单。</div>`;
  }
}

function opsHint(id) {
  const hints = {
    "arraylist": "add v / add i v / set i v / get i / remove i / contains v / size",
    "singly-linked-list": "addFirst v / addLast v / add i v / get i / removeAt i / removeValue v / indexOf v",
    "doubly-linked-list": "addFirst v / addLast v / add i v / removeFirst / removeLast / get i",
    "stack": "push v / pop / peek / size",
    "queue": "offer v / poll / peek / size",
    "circular-queue": "offer v / poll / peek / size",
    "hash-chaining": "put k v / get k / containsKey k / remove k / size",
    "hash-open-addressing": "put k v / get k / containsKey k / remove k / size",
    "union-find": "union a b / find a / connected a b / sizeOf a / components",
    "heap": "push v / pop / peek / size",
    "trie": "insert w / search w / startsWith p / countWithPrefix p / delete w / words",
    "segment-tree": "query l r / update i v",
    "fenwick-tree": "add i v / sum l r / prefix i（下标从 1 开始）",
    "bst": "insert v / remove v / contains v / min / max / height / inorder",
    "avl": "insert v / remove v / contains v / min / max / height / inorder",
    "red-black-tree": "insert v / remove v / contains v / min / max / height / inorder",
    "btree": "insert v / remove v / contains v / inorder / size",
  };
  return hints[id] || "见样例";
}

function collectInput(def) {
  const val = id => {
    const e = el.querySelector("#" + id);
    return e ? e.value.trim() : "";
  };
  const nums = s => s.split(/[\s,，]+/).filter(Boolean).map(Number);
  const t = def.inputType;
  switch (t) {
    case "array": case "tree-array": return { array: nums(val("in-array")) };
    case "array-target": return { array: nums(val("in-array")), target: Number(val("in-target")) };
    case "text-pattern": return { text: val("in-text"), pattern: val("in-pattern") };
    case "dp-n": return { n: Number(val("in-n")) };
    case "coins-amount": return { coins: nums(val("in-coins")), amount: Number(val("in-amount")) };
    case "two-strings": return { s1: val("in-s1"), s2: val("in-s2") };
    case "knapsack": return { weights: nums(val("in-weights")), values: nums(val("in-values")), capacity: Number(val("in-cap")) };
    case "graph": case "graph-neg": {
      const input = { n: Number(val("in-n")), directed: el.querySelector("#in-dir").checked, edges: [] };
      const startEl = el.querySelector("#in-start");
      if (startEl) input.start = Number(startEl.value);
      for (const line of val("in-edges").split("\n")) {
        const parts = line.trim().split(/[\s,]+/).filter(Boolean);
        if (!parts.length) continue;
        input.edges.push(parts.length >= 3 ? [Number(parts[0]), Number(parts[1]), Number(parts[2])] : [Number(parts[0]), Number(parts[1])]);
      }
      return input;
    }
    case "ops": {
      const input = { ops: val("in-ops").split("\n").map(s => s.trim()).filter(Boolean) };
      const cap = el.querySelector("#in-capacity");
      if (cap && cap.value) input.capacity = Number(cap.value);
      const init = el.querySelector("#in-init");
      if (init) input.init = nums(init.value);
      return input;
    }
    default: return {};
  }
}

function renderForm() {
  const def = current.def;
  el.querySelector("#feature-body").innerHTML = `
    <div class="card">
      <h2>输入</h2>
      <form id="run-form" onsubmit="return false">
        ${formFields(def)}
        <div class="row">
          <button class="btn primary" id="btn-run" type="submit">执行算法（Result 模式）</button>
          ${def.traceSupported ? '<span class="hint">Trace 回放会在执行时一并生成（输入规模过大时自动跳过）</span>' : '<span class="hint">该算法当前仅支持结果模式。</span>'}
        </div>
        <div class="err-text" id="run-err"></div>
      </form>
    </div>
    <div id="result-panel"></div>
    <div id="player-panel"></div>
    <div id="history-panel"></div>`;
  el.querySelector("#btn-run").addEventListener("click", () => runAlgorithm());
  renderHistory();
}

async function runAlgorithm() {
  const def = current.def;
  const errEl = el.querySelector("#run-err");
  errEl.textContent = "";
  const input = collectInput(def);
  current.lastInput = input;
  const btn = el.querySelector("#btn-run");
  btn.disabled = true;
  btn.textContent = "执行中…";
  try {
    const resp = await api.post("/api/runs", { featureId: def.id, input: JSON.stringify(input), source: "user" });
    current.lastRunId = resp.runId;
    showResult(def, resp, input);
    renderHistory();
  } catch (e) {
    errEl.textContent = e.field ? `字段 ${e.field}：${e.message}` : e.message;
    // 执行失败保留用户输入（§32）——表单内容未重置
  } finally {
    btn.disabled = false;
    btn.textContent = "执行算法（Result 模式）";
  }
}

function showResult(def, resp, input) {
  const r = resp.result || {};
  const ns = document.getElementById("player-panel");
  ns.innerHTML = "";
  // 播放器
  if (def.traceSupported) {
    const playerBox = document.createElement("div");
    playerBox.className = "card";
    playerBox.innerHTML = `<h2>Trace 回放</h2><div id="wb-player"></div>`;
    ns.appendChild(playerBox);
    const mount = playerBox.querySelector("#wb-player");
    if (current.player) current.player.destroy();
    current.player = createPlayer(mount);
    if (resp.traceAvailable) loadTrace(resp.runId);
    else playerBox.querySelector("#wb-player").innerHTML = r.traceSkipped
      ? `<div class="empty">${esc(r.traceSkipped)}</div>`
      : `<div class="empty">该算法当前仅支持结果模式。</div>`;
  }
  // 结果面板
  const box = document.createElement("div");
  box.className = "card";
  const elapsedResult = r.elapsedResultNs != null ? (r.elapsedResultNs / 1e6).toFixed(2) + " ms" : (resp.elapsedNs / 1e6).toFixed(2) + " ms";
  box.innerHTML = `
    <h2>运行结果 <span class="badge">runId: ${resp.runId}</span></h2>
    <div class="row" style="margin-bottom:8px">
      <span class="badge info">算法执行耗时 ${elapsedResult}</span>
      ${r.elapsedTraceNs > 0 ? `<span class="badge info">Trace 记录耗时 ${(r.elapsedTraceNs / 1e6).toFixed(2)} ms</span>` : ""}
      ${r.elapsedTraceNs === 0 && r.elapsedResultNs != null && def.traceSupported && resp.traceAvailable ? "" : ""}
      <a class="btn subtle" id="btn-src">查看 Java 源码</a>
      <a class="btn subtle" href="#/lab?feature=${def.id}&run=${resp.runId}" title="可复制此链接分享本次运行">链接</a>
    </div>
    <p class="hint">注：开启 Trace 的运行包含记录开销，上表耗时不能直接当作纯算法性能对比（算法耗时与 Trace 耗时已分列）。</p>
    <div id="result-body">${esc(r.summary || "")}</div>`;
  ns.prepend(box);
  box.querySelector("#btn-src").addEventListener("click", () => showSource(def));
  // ops 结果表格
  if (Array.isArray(r.ops) && r.ops.length) {
    box.querySelector("#result-body").insertAdjacentHTML("beforeend",
      `<table class="data"><thead><tr><th>#</th><th>操作</th><th>返回</th></tr></thead><tbody>`
      + r.ops.map((o, i) => `<tr><td>${i + 1}</td><td><code>${esc(o.op)}</code></td><td>${esc(o.ret)}</td></tr>`).join("")
      + `</tbody></table>`);
  }
}

async function loadTrace(runId) {
  try {
    const resp = await api.get("/api/runs/" + runId + "/trace");
    current.player.setTrace(resp.trace, "本次运行");
  } catch (e) {
    // Trace 加载失败仍保留结果（§32）
    const mount = el.querySelector("#wb-player");
    if (mount) mount.innerHTML = `<div class="empty">Trace 加载失败：${esc(e.message)}<br>仍可查看上方算法结果。</div>`;
  }
}

async function loadRun(runId) {
  try {
    const run = await api.get("/api/runs/" + runId);
    const input = JSON.parse(run.inputJson);
    fillForm(input);
    const def = current.def;
    showResult(def, {
      runId: run.runId, featureId: run.featureId, result: JSON.parse(run.resultJson),
      traceAvailable: run.traceAvailable, elapsedNs: run.elapsedNs,
    }, input);
    renderHistory();
  } catch (e) {
    el.querySelector("#result-panel").innerHTML = `<div class="empty">运行记录加载失败：${esc(e.message)}</div>`;
  }
}

function fillForm(input) {
  const set = (id, v) => {
    const e = el.querySelector("#" + id);
    if (e && v !== undefined) e.value = Array.isArray(v) ? v.join(", ") : v;
  };
  if (!input) return;
  if (input.array !== undefined) set("in-array", input.array);
  set("in-target", input.target);
  set("in-text", input.text); set("in-pattern", input.pattern);
  set("in-n", input.n); set("in-coins", input.coins); set("in-amount", input.amount);
  set("in-s1", input.s1); set("in-s2", input.s2);
  set("in-weights", input.weights); set("in-values", input.values); set("in-cap", input.capacity);
  if (input.edges) {
    set("in-edges", input.edges.map(e => e.join(" ")).join("\n"));
    const dir = el.querySelector("#in-dir"); if (dir) dir.checked = !!input.directed;
    set("in-start", input.start);
  }
  if (input.ops) set("in-ops", input.ops.join("\n"));
  if (input.init) set("in-init", input.init);
  if (input.capacity !== undefined) { const c = el.querySelector("#in-capacity"); if (c && !c.value) c.value = input.capacity; }
}

async function showSource(def) {
  try {
    const resp = await api.get("/api/source?path=" + encodeURIComponent("src/main/java/com/javadslab/" + def.sourcePath));
    const box = document.createElement("div");
    box.className = "card";
    box.innerHTML = `<h2>Java 源码：${esc(def.sourcePath)}</h2><pre style="max-height:480px"><code>${esc(resp.content)}</code></pre>
      <div class="row"><button class="btn" id="src-close">收起源码</button></div>`;
    el.querySelector("#result-panel").prepend(box);
    box.querySelector("#src-close").onclick = () => box.remove();
    box.scrollIntoView({ behavior: "smooth" });
  } catch (e) {
    alert("源码加载失败：" + e.message);
  }
}

async function renderHistory() {
  const def = current.def;
  const panel = el.querySelector("#history-panel");
  if (!panel) return;
  try {
    const resp = await api.get("/api/runs?featureId=" + encodeURIComponent(def.id) + "&limit=8");
    if (!resp.runs.length) {
      panel.innerHTML = `<div class="card"><h2>实验历史</h2><div class="empty">该知识点还没有运行记录。执行一次算法后这里会保存输入与结果，可随时重放。</div></div>`;
      return;
    }
    panel.innerHTML = `<div class="card"><h2>实验历史（最近 ${resp.runs.length} 次）</h2>
      <table class="data"><thead><tr><th>时间</th><th>输入</th><th>结果摘要</th><th>Trace</th><th>来源</th><th></th></tr></thead><tbody>
      ${resp.runs.map(r => `<tr>
        <td>${esc(r.createdAt)}</td>
        <td><code style="max-width:220px;display:inline-block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;vertical-align:bottom">${esc(r.inputJson)}</code></td>
        <td>${esc((JSON.parse(r.resultJson).summary || "").slice(0, 40))}</td>
        <td>${r.traceAvailable ? '<span class="badge ok">有</span>' : '<span class="badge">无</span>'}</td>
        <td><span class="badge">${esc(r.source)}</span></td>
        <td><button class="btn subtle" data-run="${r.runId}">重新打开</button></td>
      </tr>`).join("")}
      </tbody></table></div>`;
    panel.querySelectorAll("[data-run]").forEach(b => b.addEventListener("click", () => loadRun(b.dataset.run)));
  } catch (e) {
    panel.innerHTML = "";
  }
}

function esc(s) {
  return String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
