const AUTH_KEY = 'mq_token';
const USER_KEY = 'mq_user';

const Auth = {
  save(token, user) {
    localStorage.setItem(AUTH_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  getToken() { return localStorage.getItem(AUTH_KEY); },
  getUser() {
    const u = localStorage.getItem(USER_KEY);
    return u ? JSON.parse(u) : null;
  },
  isLoggedIn() { return !!this.getToken(); },
  isAdmin() { const u = this.getUser(); return u && u.role === 'ADMIN'; },
  logout() {
    localStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(USER_KEY);
    window.location.href = '/login.html';
  },
  requireLogin() {
    if (!this.isLoggedIn()) window.location.href = '/login.html';
  },
  requireAdmin() {
    if (!this.isAdmin()) window.location.href = '/index.html';
  }
};
