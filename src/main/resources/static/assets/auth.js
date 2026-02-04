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
  const loginPanel = $("loginPanel");
  const registerPanel = $("registerPanel");
  if (loginTab) loginTab.classList.toggle("active", isLogin);
  if (registerTab) registerTab.classList.toggle("active", !isLogin);
  if (loginPanel) loginPanel.classList.toggle("hidden", !isLogin);
  if (registerPanel) registerPanel.classList.toggle("hidden", isLogin);
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

async function initAuthPage() {
  const loginTab = $("loginTab");
  const registerTab = $("registerTab");
  const loginBtn = $("loginBtn");
  const registerBtn = $("registerBtn");

  if (loginTab) loginTab.addEventListener("click", () => toggleTab(true));
  if (registerTab) registerTab.addEventListener("click", () => toggleTab(false));

  if (loginBtn) {
    loginBtn.addEventListener("click", async () => {
      setMessage("");
      const username = $("loginUsername").value.trim();
      const password = $("loginPassword").value.trim();
      const resp = await postJson("/auth/login", { username, password });
      if (resp && resp.success) {
        location.href = "/index.html";
        return;
      }
      setMessage((resp && resp.message) || "登录失败", true);
    });
  }

  if (registerBtn) {
    registerBtn.addEventListener("click", async () => {
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
    });
  }
}

document.addEventListener("DOMContentLoaded", initAuthPage);
