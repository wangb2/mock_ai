function $(id) {
  return document.getElementById(id);
}

function setMessage(text, isError) {
  const el = $("authMessage");
  if (!el) return;
  el.textContent = text || "";
  el.style.color = isError ? "#ef4444" : "#16a34a";
}

function toggleTab(isLogin) {
  const loginTab = $("loginTab");
  const registerTab = $("registerTab");
  const loginForm = $("loginForm");
  const registerForm = $("registerForm");
  if (loginTab) {
    loginTab.classList.toggle("active", isLogin);
    loginTab.setAttribute("aria-selected", isLogin ? "true" : "false");
  }
  if (registerTab) {
    registerTab.classList.toggle("active", !isLogin);
    registerTab.setAttribute("aria-selected", !isLogin ? "true" : "false");
  }
  if (loginForm) {
    loginForm.classList.toggle("hidden", !isLogin);
    loginForm.setAttribute("aria-hidden", isLogin ? "false" : "true");
  }
  if (registerForm) {
    registerForm.classList.toggle("hidden", isLogin);
    registerForm.setAttribute("aria-hidden", isLogin ? "true" : "false");
  }
  setMessage("");
}

async function postJson(url, body) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body || {}),
  });
  return res.json();
}

async function doLogin(e) {
  if (e) e.preventDefault();
  setMessage("");
  const username = $("loginUsername").value.trim();
  const password = $("loginPassword").value.trim();
  const resp = await postJson("/auth/login", { username, password });
  if (resp && resp.success) {
    try {
      localStorage.setItem("uiThemeV1", "light");
    } catch (e) {}
    location.href = "/index.html";
    return;
  }
  setMessage((resp && resp.message) || "登录失败", true);
}

async function doRegister(e) {
  if (e) e.preventDefault();
  setMessage("");
  const username = $("registerUsername").value.trim();
  const password = $("registerPassword").value.trim();
  const resp = await postJson("/auth/register", { username, password });
  if (resp && resp.success) {
    setMessage(resp.message || "注册成功，等待审批");
    toggleTab(true);
    return;
  }
  setMessage((resp && resp.message) || "注册失败", true);
}

function initAuthPage() {
  const loginTab = $("loginTab");
  const registerTab = $("registerTab");
  const loginForm = $("loginForm");
  const registerForm = $("registerForm");

  if (loginTab) loginTab.addEventListener("click", () => toggleTab(true));
  if (registerTab) registerTab.addEventListener("click", () => toggleTab(false));

  if (loginForm) loginForm.addEventListener("submit", doLogin);
  if (registerForm) registerForm.addEventListener("submit", doRegister);
}

document.addEventListener("DOMContentLoaded", initAuthPage);
