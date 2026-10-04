/* SVG 播放器组件：核心渲染器自 web/player.html 迁移（同一套渲染逻辑、适配浅色主题）。
   保留全部控制能力：上一步/下一步/播放/暂停/回退/跳步/速度/进度条/缩略图/JSON 导入/JSON 导出。
   快捷键在输入框聚焦时失效（计划 §2.4）。 */
"use strict";

/* ---------- 快照状态颜色（浅色主题） ---------- */
const MARK = {
  cmp: "#B08A2E", swap: "#B07A3F", pivot: "#7A5C9E", sorted: "#4E7D57",
  active: "#A5554E", frontier: "#2E8C7A", settled: "#4E7D57", visited: "#607C8A",
  discover: "#2E8C7A", discard: "#9AA69E", unknown: "#ECEAE0",
  occupied: "#D8CFC0", H: "#A5554E", T: "#2E8C7A", root: "#7A5C9E",
  R: "#A5554E", B: "#5C6B60", end: "#4E7D57", inStack: "#B08A2E",
  todo: "#ECEAE0", inProgress: "#B08A2E", done: "#4E7D57",
  inTree: "#4E7D57", joining: "#2E8C7A", improving: "#2E8C7A",
  released: "#2E8C7A"
};
function markColor(s) { return s && (MARK[String(s).split(",")[0].trim()] || "#D8CFC0"); }

const svgNS = "http://www.w3.org/2000/svg";
function el(tag, attrs, text) {
  const e = document.createElementNS(svgNS, tag);
  for (const k in (attrs || {})) e.setAttribute(k, attrs[k]);
  if (text !== undefined) e.textContent = text;
  return e;
}
function label(svg, text, x = 450, y = 26) {
  svg.appendChild(el("text", { x, y, fill: "#6E7D73", "text-anchor": "middle", "font-size": 15 }, text ?? ""));
}
function drawCell(svg, x, y, w, h, v, s) {
  const filled = s && s !== "unknown" && s !== "todo";
  svg.appendChild(el("rect", { x, y, width: w, height: h, rx: 5,
    fill: filled ? markColor(s) : "#F8F7F2", stroke: s ? markColor(s) : "#B8B4A4", "stroke-width": s ? 2 : 1 }));
  svg.appendChild(el("text", { x: x + w / 2, y: y + h / 2 + 5, "text-anchor": "middle",
    "font-size": 14, fill: filled ? "#F8F7F2" : "#303D35" }, String(v)));
}
function arrow(svg, x1, y1, x2, y2, color) {
  const id = "arr" + Math.random().toString(36).slice(2, 7);
  const defs = el("defs");
  const m = el("marker", { id, markerWidth: 8, markerHeight: 8, refX: 7, refY: 4, orient: "auto" });
  m.appendChild(el("path", { d: "M0,0 L8,4 L0,8 z", fill: color || "#6E7D73" }));
  defs.appendChild(m);
  svg.appendChild(defs);
  svg.appendChild(el("line", { x1, y1, x2: x2 - 2, y2, stroke: color || "#6E7D73", "stroke-width": 1.6, "marker-end": `url(#${id})` }));
}

/* ---------- 渲染器（9 种 kind，逻辑与 player.html 一致） ---------- */
function renderInto(svg, snapshot, hl) {
  if (!snapshot) { svg.appendChild(el("text", { x: 450, y: 240, fill: "#6E7D73", "text-anchor": "middle" }, "无快照")); return; }
  switch (snapshot.kind) {
    case "array": renderArray(svg, snapshot, hl); break;
    case "linked": renderLinked(svg, snapshot); break;
    case "hash": renderHash(svg, snapshot); break;
    case "heap": renderHeap(svg, snapshot); break;
    case "tree": renderTree(svg, snapshot); break;
    case "graph": renderGraph(svg, snapshot, hl); break;
    case "matrix": renderMatrix(svg, snapshot); break;
    case "uf": renderUF(svg, snapshot); break;
    case "monostack": renderMonoStack(svg, snapshot); break;
    default: svg.appendChild(el("text", { x: 450, y: 240, fill: "#A5554E", "text-anchor": "middle" }, "未知快照类型: " + snapshot.kind));
  }
}
function renderArray(svg, snap, hl) {
  label(svg, snap.label);
  const cells = snap.cells || [];
  const n = cells.length, w = Math.min(70, 840 / Math.max(n, 1)), h = 44;
  const total = n * w, x0 = Math.max(30, (900 - total) / 2), y0 = 210;
  cells.forEach((c, i) => {
    let s = c.s;
    if (hl && hl.cell === i) s = "active";
    drawCell(svg, x0 + i * w, y0, w - 4, h, c.v, s);
    svg.appendChild(el("text", { x: x0 + i * w + (w - 4) / 2, y: y0 + h + 16, "text-anchor": "middle", "font-size": 10, fill: "#9AA69E" }, i));
  });
}
function renderLinked(svg, snap) {
  label(svg, "链表（→ 为 next 指针）");
  const nodes = snap.nodes || [];
  const n = nodes.length, w = Math.min(90, 800 / Math.max(n, 1)), h = 44, y0 = 218;
  const x0 = Math.max(20, (900 - n * w - 60) / 2);
  nodes.forEach((c, i) => {
    drawCell(svg, x0 + i * w, y0, w - 14, h, c.v, c.s);
    if (i < n - 1) arrow(svg, x0 + i * w + (w - 14), y0 + h / 2, x0 + (i + 1) * w, y0 + h / 2);
  });
  arrow(svg, x0 + (n - 1) * w + (w - 14), y0 + h / 2, x0 + n * w - 8, y0 + h / 2);
  svg.appendChild(el("text", { x: x0 + n * w + 2, y: y0 + h / 2 + 5, "font-size": 13, fill: "#6E7D73" }, "∅"));
}
function renderHash(svg, snap) {
  label(svg, `哈希表  size=${snap.size}  capacity=${snap.capacity}`);
  const buckets = snap.buckets || [];
  const rowH = Math.min(34, 400 / buckets.length), x0 = 130, y0 = 60;
  buckets.forEach((chain, b) => {
    const y = y0 + b * rowH;
    svg.appendChild(el("rect", { x: x0, y: y + 2, width: 56, height: rowH - 5, rx: 4, fill: "#ECEAE0" }));
    svg.appendChild(el("text", { x: x0 + 28, y: y + rowH / 2, "text-anchor": "middle", "font-size": 11, fill: "#6E7D73" }, "bucket " + b));
    let cx = x0 + 66;
    (chain || []).forEach(e => {
      const txt = e.v === null || e.v === undefined ? `${e.k}` : `${e.k}:${e.v}`;
      const wBox = Math.min(150, 14 + txt.length * 9);
      svg.appendChild(el("rect", { x: cx, y: y + 2, width: wBox, height: rowH - 5, rx: 4, fill: "#F8F7F2", stroke: "#B8B4A4" }));
      svg.appendChild(el("text", { x: cx + wBox / 2, y: y + rowH / 2, "text-anchor": "middle", "font-size": 11, fill: "#303D35" }, txt));
      cx += wBox + 14;
      arrow(svg, cx - 14, y + rowH / 2, cx - 2, y + rowH / 2);
      if (cx > 860) { svg.appendChild(el("text", { x: cx, y: y + rowH / 2, "font-size": 11, fill: "#A5554E" }, "…")); }
    });
  });
}
function renderHeap(svg, snap) {
  label(svg, `堆（完全二叉树，size=${snap.size}）`);
  const cells = (snap.cells || []).slice(0, snap.size);
  const draw = (i, x, y, span) => {
    if (i >= cells.length) return;
    drawCell(svg, x - 21, y, 42, 34, cells[i].v, cells[i].s);
    const yChild = y + 62;
    if (2 * i + 1 < cells.length) {
      svg.appendChild(el("line", { x1: x, y1: y + 34, x2: x - span / 2, y2: yChild, stroke: "#B8B4A4" }));
      draw(2 * i + 1, x - span / 2, yChild, span / 2);
    }
    if (2 * i + 2 < cells.length) {
      svg.appendChild(el("line", { x1: x, y1: y + 34, x2: x + span / 2, y2: yChild, stroke: "#B8B4A4" }));
      draw(2 * i + 2, x + span / 2, yChild, span / 2);
    }
  };
  draw(0, 450, 50, 380);
}
function renderTree(svg, snap) {
  label(svg, snap.label);
  const root = snap.root;
  if (!root) { svg.appendChild(el("text", { x: 450, y: 240, "text-anchor": "middle", fill: "#6E7D73" }, "空树")); return; }
  let leafX = 0; const pos = new Map();
  (function assign(n, depth) {
    if (!n) return 0;
    const kids = n.c || [];
    let x;
    if (kids.filter(Boolean).length === 0) x = leafX++;
    else {
      const xs = kids.filter(Boolean).map(k => assign(k, depth + 1));
      x = (xs[0] + xs[xs.length - 1]) / 2;
    }
    pos.set(n, { x, depth });
    return x;
  })(root, 0);
  const W = 840, x0 = 30, maxDepth = [...pos.values()].reduce((m, p) => Math.max(m, p.depth), 0);
  const yGap = Math.min(78, 380 / (maxDepth + 1));
  const px = n => x0 + (pos.get(n).x + 0.5) * (W / Math.max(leafX, 1));
  const py = n => 46 + pos.get(n).depth * yGap;
  (function edges(n) {
    if (!n) return;
    for (const k of (n.c || [])) if (k) { svg.appendChild(el("line", { x1: px(n), y1: py(n) + 18, x2: px(k), y2: py(k) - 4, stroke: "#B8B4A4" })); edges(k); }
  })(root);
  (function nodes(n) {
    if (!n) return;
    const isRB = n.color === "R" || n.color === "B";
    const keys = (n.keys || []).join(" ");
    const wBox = Math.max(40, 16 + keys.length * 9), hBox = 26;
    const fill = isRB ? (n.color === "R" ? "#A5554E" : "#5C6B60") : (markColor(n.color) || "#F8F7F2");
    svg.appendChild(el("rect", { x: px(n) - wBox / 2, y: py(n) - 4, width: wBox, height: hBox, rx: isRB ? 4 : 10,
      fill, stroke: n.hl ? "#A5554E" : "#B8B4A4", "stroke-width": n.hl ? 3 : 1 }));
    svg.appendChild(el("text", { x: px(n), y: py(n) + 13, "text-anchor": "middle", "font-size": 12,
      fill: (isRB || (n.color && n.color !== "unknown")) ? "#F8F7F2" : "#303D35" }, keys));
    for (const k of (n.c || [])) nodes(k);
  })(root);
}
function renderGraph(svg, snap, hl) {
  label(svg, `图  n=${snap.n}  ${snap.directed ? "有向" : "无向"}`);
  const nodes = snap.nodes || [], edges = snap.edges || [];
  const edgeHl = hl && hl.edge ? (Array.isArray(hl.edge) ? hl.edge.join(",") : String(hl.edge)) : null;
  edges.forEach(e => {
    const a = nodes[e.u], b = nodes[e.v];
    if (!a || !b) return;
    const isHl = edgeHl && (edgeHl === `${e.u},${e.v}` || edgeHl === `${e.v},${e.u}`);
    if (snap.directed) arrow(svg, a.x, a.y, b.x, b.y, isHl ? "#A5554E" : "#B8B4A4");
    else svg.appendChild(el("line", { x1: a.x, y1: a.y, x2: b.x, y2: b.y, stroke: isHl ? "#A5554E" : "#B8B4A4", "stroke-width": isHl ? 3 : 1.4 }));
    if (e.w !== undefined && e.w !== 1) {
      svg.appendChild(el("text", { x: (a.x + b.x) / 2 + 6, y: (a.y + b.y) / 2 - 6, "font-size": 11, fill: isHl ? "#A5554E" : "#6E7D73" }, e.w));
    }
  });
  nodes.forEach((nd, i) => {
    const r = 20;
    const filled = nd.s && nd.s !== "unknown" && nd.s !== "todo";
    svg.appendChild(el("circle", { cx: nd.x, cy: nd.y, r, fill: filled ? markColor(nd.s) : "#F8F7F2", stroke: "#B8B4A4", "stroke-width": filled ? 2 : 1 }));
    svg.appendChild(el("text", { x: nd.x, y: nd.y + 5, "text-anchor": "middle", "font-size": 13, fill: filled ? "#F8F7F2" : "#303D35" }, nd.label));
    if (snap.dist) {
      svg.appendChild(el("text", { x: nd.x, y: nd.y - r - 6, "text-anchor": "middle", "font-size": 12, fill: "#607C8A" }, "d=" + snap.dist[i]));
    }
  });
}
function renderMatrix(svg, snap) {
  label(svg, snap.label);
  const rows = snap.rows || [];
  const cw = Math.min(58, 820 / Math.max(...rows.map(r => r.length), 1));
  const ch = Math.min(40, 380 / rows.length);
  const x0 = (900 - rows[0].length * cw) / 2, y0 = Math.max(40, (480 - rows.length * ch) / 2);
  rows.forEach((row, i) => row.forEach((c, j) => {
    drawCell(svg, x0 + j * cw, y0 + i * ch, cw - 3, ch - 3, c.v, c.s);
  }));
  svg.appendChild(el("text", { x: x0 - 10, y: y0 - 8, "font-size": 11, fill: "#9AA69E" }, "行=i 列=j"));
}
function renderUF(svg, snap) {
  label(svg, "并查集 parent 数组（含自身者为根）");
  const parent = snap.parent || [], aux = snap.aux || [];
  const n = parent.length, w = Math.min(66, 840 / n), h = 40, y0 = 170;
  const x0 = (900 - n * w) / 2;
  parent.forEach((c, i) => {
    drawCell(svg, x0 + i * w, y0, w - 4, h, c.v, c.s);
    svg.appendChild(el("text", { x: x0 + i * w + (w - 4) / 2, y: y0 - 8, "text-anchor": "middle", "font-size": 10, fill: "#9AA69E" }, i));
    if (aux[i]) svg.appendChild(el("text", { x: x0 + i * w + (w - 4) / 2, y: y0 + h + 16, "text-anchor": "middle", "font-size": 10, fill: "#607C8A" }, aux[i].v));
    if (c.v !== i && c.v >= 0 && c.v < n) {
      arrow(svg, x0 + i * w + (w - 4) / 2, y0 + h + 26, x0 + c.v * w + (w - 4) / 2, y0 + h + 26, "#B8B4A4");
    }
  });
  svg.appendChild(el("text", { x: 450, y: y0 + h + 52, "text-anchor": "middle", "font-size": 11, fill: "#9AA69E" }, "下方灰蓝为 size/rank；箭头指向父节点"));
}
function renderMonoStack(svg, snap) {
  if (snap.text !== undefined) { renderKMP(svg, snap); return; }
  label(svg, "单调栈（数组 + 栈内容 + 结果）");
  if (snap.array) { snap.array.label = "数组"; renderArray(svg, snap.array, null); }
  if (snap.res) {
    const cells = snap.res.cells || [];
    const w = Math.min(70, 840 / Math.max(cells.length, 1)), y0 = 320;
    const x0 = (900 - cells.length * w) / 2;
    svg.appendChild(el("text", { x: 60, y: y0 + 22, "font-size": 12, fill: "#6E7D73" }, "res"));
    cells.forEach((c, i) => drawCell(svg, x0 + i * w, y0, w - 4, 40, c.v, c.s));
  }
  if (snap.stack && snap.stack.length) {
    svg.appendChild(el("text", { x: 450, y: 410, "text-anchor": "middle", "font-size": 13, fill: "#B08A2E" },
      "栈（底→顶）: " + snap.stack.map(c => `[${c.v}]`).join(" ")));
  }
}
function renderKMP(svg, snap) {
  label(svg, "KMP：主串指针 i，模式串指针 j");
  const rows = [["主串", snap.text], ["模式串", snap.pattern]];
  let y = 80;
  rows.forEach(([name, arr]) => {
    svg.appendChild(el("text", { x: 90, y: y + 24, "text-anchor": "end", "font-size": 13, fill: "#6E7D73" }, name));
    (arr && arr.cells ? arr.cells : []).forEach((c, i) => drawCell(svg, 110 + i * 46, y, 42, 40, c.v, c.s));
    y += 70;
  });
  if (snap.next) {
    svg.appendChild(el("text", { x: 90, y: y + 24, "text-anchor": "end", "font-size": 13, fill: "#6E7D73" }, "next"));
    (snap.next.cells || []).forEach((c, i) => drawCell(svg, 110 + i * 46, y, 42, 32, c.v, null));
  }
  svg.appendChild(el("text", { x: 450, y: 380, "text-anchor": "middle", "font-size": 16, fill: "#607C8A" },
    `i = ${snap.i}   j = ${snap.j}`));
}

/* ---------- 播放器组件 ---------- */
export function createPlayer(container) {
  container.classList.add("player");
  container.innerHTML = `
    <div class="stage"></div>
    <div class="thumbs"></div>
    <div class="legend">
      <span style="color:#B08A2E">比较</span><span style="color:#B07A3F">交换/写入</span>
      <span style="color:#7A5C9E">基准</span><span style="color:#4E7D57">已完成/最短</span>
      <span style="color:#A5554E">当前活跃</span><span style="color:#2E8C7A">新加入</span>
    </div>
    <div class="controls">
      <button class="btn subtle" data-act="first" title="回到开头">⏮</button>
      <button class="btn" data-act="prev" title="上一步（回退）">◀ 上一步</button>
      <button class="btn primary" data-act="play">▶ 播放</button>
      <button class="btn" data-act="next">下一步 ▶</button>
      <button class="btn subtle" data-act="last" title="跳到最后">⏭</button>
      <input type="range" min="0" max="0" value="0" aria-label="进度条">
      <span class="step-label">0 / 0</span>
      <label style="margin:0">速度</label>
      <select data-role="speed" aria-label="播放速度">
        <option value="2000">0.5×</option>
        <option value="1000" selected>1×</option>
        <option value="500">2×</option>
        <option value="250">4×</option>
      </select>
      <button class="btn subtle" data-act="import">导入 JSON</button>
      <button class="btn subtle" data-act="export">导出 JSON</button>
      <input type="file" accept=".json" style="display:none" data-role="file">
    </div>
    <div class="stepinfo">尚未加载 Trace。</div>`;
  const stage = container.querySelector(".stage");
  const thumbsBar = container.querySelector(".thumbs");
  const stepInfo = container.querySelector(".stepinfo");
  const slider = container.querySelector("input[type=range]");
  const stepLabel = container.querySelector(".step-label");
  const playBtn = container.querySelector('[data-act="play"]');

  const st = { trace: null, idx: -1, timer: null, thumbs: [], onTraceLoaded: null };

  function render(snap, hl) {
    stage.innerHTML = "";
    const svg = el("svg", { viewBox: "0 0 900 480", preserveAspectRatio: "xMidYMid meet" });
    stage.appendChild(svg);
    renderInto(svg, snap, hl);
  }

  function buildThumbs() {
    thumbsBar.innerHTML = "";
    st.thumbs = [];
    if (!st.trace) return;
    const N = st.trace.steps.length, K = Math.min(10, N);
    for (let k = 0; k < K; k++) {
      const stepIdx = Math.round(k * (N - 1) / Math.max(K - 1, 1));
      const snap = stepIdx <= 0 ? st.trace.steps[0].before : st.trace.steps[stepIdx].after;
      const d = document.createElement("div");
      d.className = "thumb";
      d.title = "第 " + (stepIdx + 1) + " 步" + (st.trace.steps[stepIdx] ? "：" + st.trace.steps[stepIdx].op : "");
      const s = el("svg", { viewBox: "0 0 900 480", preserveAspectRatio: "xMidYMid meet" });
      renderInto(s, snap, null);
      d.appendChild(s);
      const lbl = document.createElement("span");
      lbl.textContent = "#" + (stepIdx + 1);
      d.appendChild(lbl);
      d.addEventListener("click", () => { stop(); goto(stepIdx); });
      thumbsBar.appendChild(d);
      st.thumbs.push({ stepIdx, el: d });
    }
    updateThumbs();
  }
  function updateThumbs() {
    let best = null;
    for (const t of st.thumbs) {
      t.el.classList.remove("active");
      if (st.idx >= 0 && (best === null || Math.abs(t.stepIdx - st.idx) < Math.abs(best.stepIdx - st.idx))) best = t;
    }
    if (best) best.el.classList.add("active");
  }

  function goto(i) {
    if (!st.trace) return;
    st.idx = Math.max(-1, Math.min(st.trace.steps.length - 1, i));
    const steps = st.trace.steps;
    const snap = st.idx < 0 ? steps[0].before : steps[st.idx].after;
    const step = st.idx < 0 ? null : steps[st.idx];
    render(snap, step ? step.hl : null);
    slider.value = st.idx;
    stepLabel.textContent = `${st.idx + 1} / ${steps.length}`;
    if (!step) {
      stepInfo.innerHTML = `<b>初始状态</b>（按"下一步"开始）${st.trace.meta && st.trace.meta.title ? " — " + st.trace.meta.title : ""}`;
    } else {
      const args = step.args ? Object.entries(step.args).map(([k, v]) => `${k}=${JSON.stringify(v)}`).join("　") : "";
      stepInfo.innerHTML = `<span class="op-tag">${step.op}</span><b>${args}</b>${step.note ? "　— " + step.note : ""}`;
    }
    updateThumbs();
  }
  function stop() {
    if (st.timer) { clearInterval(st.timer); st.timer = null; }
    playBtn.textContent = "▶ 播放";
  }
  function togglePlay() {
    if (st.timer) { stop(); return; }
    if (!st.trace) return;
    if (st.idx >= st.trace.steps.length - 1) goto(-1);
    const speed = parseInt(container.querySelector('[data-role="speed"]').value, 10);
    st.timer = setInterval(() => {
      if (st.idx >= st.trace.steps.length - 1) stop();
      else goto(st.idx + 1);
    }, speed);
    playBtn.textContent = "⏸ 暂停";
  }

  container.querySelectorAll("[data-act]").forEach(b => {
    b.addEventListener("click", async () => {
      const act = b.dataset.act;
      if (act === "first") { stop(); goto(-1); }
      else if (act === "prev") { stop(); goto(st.idx - 1); }
      else if (act === "next") { stop(); goto(st.idx + 1); }
      else if (act === "last") { stop(); goto(st.trace ? st.trace.steps.length - 1 : -1); }
      else if (act === "play") togglePlay();
      else if (act === "import") container.querySelector('[data-role="file"]').click();
      else if (act === "export") {
        if (!st.trace) return;
        const blob = new Blob([JSON.stringify(st.trace, null, 2)], { type: "application/json" });
        const a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = (st.trace.meta && st.trace.meta.title || "trace") + ".json";
        a.click();
        URL.revokeObjectURL(a.href);
      }
    });
  });
  container.querySelector('[data-role="file"]').addEventListener("change", async e => {
    const f = e.target.files[0];
    if (!f) return;
    try {
      setTrace(JSON.parse(await f.text()), f.name.replace(/\.json$/, ""));
    } catch (err) {
      stepInfo.innerHTML = `<span style="color:var(--danger)">导入失败：${err.message}</span>`;
    }
    e.target.value = "";
  });
  slider.addEventListener("input", e => { stop(); goto(parseInt(e.target.value, 10)); });
  // 快捷键：输入框聚焦时失效（§2.4）
  const keyHandler = e => {
    const tag = (document.activeElement && document.activeElement.tagName) || "";
    if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;
    if (!st.trace) return;
    if (e.key === "ArrowRight") { stop(); goto(st.idx + 1); }
    else if (e.key === "ArrowLeft") { stop(); goto(st.idx - 1); }
    else if (e.key === " ") { e.preventDefault(); togglePlay(); }
  };
  document.addEventListener("keydown", keyHandler);

  function setTrace(traceObj, name) {
    if (!traceObj || !traceObj.steps || !traceObj.steps.length) {
      stepInfo.innerHTML = '<span style="color:var(--danger)">无效的 trace（没有步骤）</span>';
      return false;
    }
    stop();
    st.trace = traceObj; st.idx = -1;
    slider.max = traceObj.steps.length - 1; slider.value = 0;
    buildThumbs();
    goto(-1);
    if (st.onTraceLoaded) st.onTraceLoaded(name, traceObj);
    return true;
  }

  return {
    setTrace,
    current: () => st.trace ? { idx: st.idx, total: st.trace.steps.length } : null,
    destroy() { document.removeEventListener("keydown", keyHandler); stop(); },
  };
}
