// Cienka warstwa nad fetch: dokleja token i zamienia błędy HTTP na wyjątki.

export type Me = { id: number; username: string; displayName: string; email: string };
export type Repository = { id: number; name: string; description: string | null; owner: string };
export type AccessLevel = 'READ' | 'WRITE';
export type Folder = { id: number; name: string; parentId: number | null; access: AccessLevel };
export type DocumentItem = {
  id: number;
  title: string;
  description: string | null;
  author: string;
  createdAt: string;
  latestVersion: string | null;
  approved: boolean;
  lastAction: string | null;
  latestVersionId: number | null;
  fileName: string | null;
  restricted: boolean;
  viewerIds: number[];
};
export type NewDocument = {
  title: string;
  description: string;
  initialVersion: string;
  file: File | null;
  /** Puste = dokument widzą wszyscy z dostępem do folderu. */
  viewerIds: number[];
};
export type User = { id: number; username: string; displayName: string };
export type HistoryEvent = { action: string; stage: string; user: string; comment: string | null; createdAt: string };
export type Version = {
  id: number;
  versionLabel: string;
  parentVersionId: number | null;
  approved: boolean;
  changeNote: string | null;
  author: string;
  createdAt: string;
  fileName: string | null;
  sizeBytes: number | null;
  mimeType: string | null;
  history: HistoryEvent[];
};
export type Permission = { userId: number; displayName: string; username: string; level: AccessLevel };

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

async function request<T>(token: string, path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      Authorization: `Bearer ${token}`,
      // Dla FormData przeglądarka sama ustawia Content-Type z granicą multipart.
      ...(init.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(init.headers ?? {}),
    },
  });
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const body = await res.json();
      detail = body.detail ?? body.title ?? detail;
    } catch {
      /* odpowiedź bez JSON */
    }
    throw new ApiError(res.status, detail);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

const post = (body: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(body) });

export const api = (token: string) => ({
  me: () => request<Me>(token, '/api/me'),
  repositories: () => request<Repository[]>(token, '/api/repositories'),
  createRepository: (name: string, description: string) =>
    request<Repository>(token, '/api/repositories', post({ name, description })),
  folders: (repositoryId: number) => request<Folder[]>(token, `/api/repositories/${repositoryId}/folders`),
  createFolder: (parentId: number, name: string) =>
    request<Folder>(token, `/api/folders/${parentId}/folders`, post({ name })),
  documents: (folderId: number) => request<DocumentItem[]>(token, `/api/folders/${folderId}/documents`),
  createDocument: (folderId: number, doc: NewDocument) => {
    const form = new FormData();
    form.append('title', doc.title);
    form.append('description', doc.description);
    if (doc.initialVersion) form.append('initialVersion', doc.initialVersion);
    if (doc.file) form.append('file', doc.file);
    doc.viewerIds.forEach((id) => form.append('viewerIds', String(id)));
    return request<DocumentItem>(token, `/api/folders/${folderId}/documents`, { method: 'POST', body: form });
  },
  users: () => request<User[]>(token, '/api/users'),
  versions: (documentId: number) => request<Version[]>(token, `/api/documents/${documentId}/versions`),
  addVersion: (documentId: number, file: File, parentVersionId: number | null, changeNote: string) => {
    const form = new FormData();
    form.append('file', file);
    form.append('changeNote', changeNote);
    if (parentVersionId != null) form.append('parentVersionId', String(parentVersionId));
    return request<Version>(token, `/api/documents/${documentId}/versions`, { method: 'POST', body: form });
  },
  approve: (versionId: number, comment: string) =>
    request<Version>(token, `/api/versions/${versionId}/approve`, post({ comment })),
  setVisibility: (documentId: number, restricted: boolean, viewerIds: number[]) =>
    request<DocumentItem>(token, `/api/documents/${documentId}/visibility`, {
      method: 'PUT',
      body: JSON.stringify({ restricted, viewerIds }),
    }),
  /** Pobiera plik wersji jako Blob (fetch z tokenem; zwykły link nie niesie nagłówka Authorization). */
  fileBlob: async (versionId: number): Promise<Blob> => {
    const res = await fetch(`/api/versions/${versionId}/file`, { headers: { Authorization: `Bearer ${token}` } });
    if (!res.ok) throw new ApiError(res.status, res.status === 404 ? 'Ta wersja nie ma pliku' : res.statusText);
    return res.blob();
  },
  permissions: (folderId: number) => request<Permission[]>(token, `/api/folders/${folderId}/permissions`),
  grant: (folderId: number, userId: number, level: AccessLevel) =>
    request<Permission>(token, `/api/folders/${folderId}/permissions`, {
      method: 'PUT',
      body: JSON.stringify({ userId, level }),
    }),
  revoke: (folderId: number, userId: number) =>
    request<void>(token, `/api/folders/${folderId}/permissions/${userId}`, { method: 'DELETE' }),
});
