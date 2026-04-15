const BASE = '/api';

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  const token = Auth.getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  });
  const text = await res.text();
  if (!text) throw new Error('服务器无响应（HTTP ' + res.status + '）');
  const data = JSON.parse(text);
  if (!data.success) throw new Error(data.message || '请求失败');
  return data.data;
}

const api = {
  login: (username, password) => request('POST', '/auth/login', { username, password }),
  register: (data) => request('POST', '/auth/register', data),
  me: () => request('GET', '/auth/me'),

  getCats: (params) => request('GET', '/cats?' + new URLSearchParams(params)),
  getCat: (id) => request('GET', `/cats/${id}`),
  submitCat: (data) => request('POST', '/cats', data),
  toggleLike: (id) => request('POST', `/cats/${id}/like`),

  getComments: (catId) => request('GET', `/cats/${catId}/comments`),
  addComment: (catId, content) => request('POST', `/cats/${catId}/comments`, { content }),

  getNews: (params) => request('GET', '/news?' + new URLSearchParams(params)),
  getNewsDetail: (id) => request('GET', `/news/${id}`),

  uploadImage: async (file) => {
    const fd = new FormData();
    fd.append('file', file);
    const headers = {};
    const token = Auth.getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const res = await fetch(BASE + '/upload/image', { method: 'POST', headers, body: fd });
    const data = await res.json();
    if (!data.success) throw new Error(data.message);
    return data.data.url;
  },

  getCatsAdmin: (page, status, name, breed, gender) => {
    const params = new URLSearchParams({ page: page || 1, size: 10 });
    if (status) params.set('status', status);
    if (name) params.set('name', name);
    if (breed) params.set('breed', breed);
    if (gender) params.set('gender', gender);
    return request('GET', `/admin/cats/pending?${params}`);
  },
  updateCatStatus: (id, status) => request('PUT', `/admin/cats/${id}/status`, { status }),
  blockComment: (id) => request('PUT', `/admin/comments/${id}/block`),
  getAllComments: (page, content, isBlocked) => {
    const params = new URLSearchParams({ page: page || 1, size: 20 });
    if (content) params.set('content', content);
    if (isBlocked !== undefined && isBlocked !== '') params.set('isBlocked', isBlocked);
    return request('GET', `/admin/comments?${params}`);
  },
  getAdminNews: (page, title, category) => {
    const params = new URLSearchParams({ page: page || 1, size: 10 });
    if (title) params.set('title', title);
    if (category) params.set('category', category);
    return request('GET', `/admin/news?${params}`);
  },
  createNews: (data) => request('POST', '/admin/news', data),
  updateNews: (id, data) => request('PUT', `/admin/news/${id}`, data),
  deleteNews: (id) => request('DELETE', `/admin/news/${id}`),
  getUsers: (page, id, username) => {
    const params = new URLSearchParams({ page: page || 1, size: 20 });
    if (id) params.set('id', id);
    if (username) params.set('username', username);
    return request('GET', `/admin/users?${params}`);
  },
  updateUserRole: (id, role) => request('PUT', `/admin/users/${id}/role`, { role }),
  updateUserStatus: (id, isActive) => request('PUT', `/admin/users/${id}/status`, { isActive }),
};
