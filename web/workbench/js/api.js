/* API 封装：统一 success/data 与错误格式；服务断开时显示全局横幅（计划 §32） */
"use strict";

const banner = () => document.getElementById("conn-banner");
let reconnectHandler = null;

export function onReconnect(fn) { reconnectHandler = fn; }

export function showDisconnected() {
  const b = banner();
  if (b) b.style.display = "block";
}
export function hideDisconnected() {
  const b = banner();
  if (b) b.style.display = "none";
}

async function request(method, url, body) {
  let resp;
  try {
    resp = await fetch(url, {
      method,
      headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    showDisconnected();
    throw new Error("学习工作台服务已断开。");
  }
  hideDisconnected();
  let json;
  try { json = await resp.json(); } catch { throw new Error("服务返回了无法解析的内容（HTTP " + resp.status + "）"); }
  if (!resp.ok || json.success === false) {
    const err = json.error || {};
    const e = new Error(err.message || ("请求失败（HTTP " + resp.status + "）"));
    e.code = err.code;
    e.field = err.field;
    e.status = resp.status;
    throw e;
  }
  return json.data;
}

export const api = {
  get: (url) => request("GET", url),
  post: (url, body) => request("POST", url, body ?? {}),
  put: (url, body) => request("PUT", url, body ?? {}),
  del: (url) => request("DELETE", url),
};
