function renderNav() {
  const user = Auth.getUser();
  const nav = document.getElementById('main-nav');
  if (!nav) return;
  nav.innerHTML = `
    <nav class="navbar">
      <a href="/index.html" class="nav-logo">🐱 猫友圈</a>
      <div class="nav-links">
        <a href="/index.html">首页</a>
        <a href="/news.html">新闻</a>
        ${user ? `
          <a href="/submit-cat.html">提交猫咪</a>
          ${user.role === 'ADMIN' ? '<a href="/admin/index.html">后台</a>' : ''}
          <span class="nav-user">Hi, ${user.nickname || user.role}</span>
          <a href="#" onclick="Auth.logout()">退出</a>
        ` : `
          <a href="/login.html">登录</a>
          <a href="/register.html" class="btn-primary">注册</a>
        `}
      </div>
    </nav>`;
}

function renderPagination(container, current, total, onPage) {
  if (total <= 1) { container.innerHTML = ''; return; }
  let html = '<div class="pagination">';
  if (current > 1) html += `<button onclick="${onPage}(${current - 1})">上一页</button>`;
  for (let i = Math.max(1, current - 2); i <= Math.min(total, current + 2); i++) {
    html += `<button class="${i === current ? 'active' : ''}" onclick="${onPage}(${i})">${i}</button>`;
  }
  if (current < total) html += `<button onclick="${onPage}(${current + 1})">下一页</button>`;
  html += '</div>';
  container.innerHTML = html;
}

function showToast(msg, type = 'success') {
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = msg;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 3000);
}

document.addEventListener('DOMContentLoaded', renderNav);
