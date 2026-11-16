import { useEffect, useState } from 'react';
import { api, type DocumentItem, type Folder, type Repository, type User } from '../api/client';
import DocumentPanel from './DocumentPanel';
import FolderAccessPanel from './FolderAccessPanel';
import InlineCreate from './InlineCreate';
import NewDocumentForm from './NewDocumentForm';

/**
 * UC2 Przeglądaj repozytorium, UC3 Utwórz repozytorium lub folder, UC7 Dodaj dokument.
 * Widać tylko repozytoria i foldery, do których użytkownik ma upoważnienie;
 * formularze zapisu pojawiają się tylko przy upoważnieniu WRITE.
 */
export default function RepositoryPage({ token, meId }: { token: string; meId: number }) {
  const client = api(token);

  const [repositories, setRepositories] = useState<Repository[]>([]);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [repositoryId, setRepositoryId] = useState<number | null>(null);
  const [folderId, setFolderId] = useState<number | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [selectedDocId, setSelectedDocId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fail = (e: unknown) => setError(e instanceof Error ? e.message : String(e));
  const folder = folders.find((f) => f.id === folderId) ?? null;
  const canWrite = folder?.access === 'WRITE';
  const selectedDoc = documents.find((d) => d.id === selectedDocId) ?? null;

  const loadRepositories = () => client.repositories().then(setRepositories).catch(fail);
  const loadFolders = (id: number) => client.folders(id).then(setFolders).catch(fail);
  const loadDocuments = (id: number) => client.documents(id).then(setDocuments).catch(fail);

  useEffect(() => {
    client.users().then(setUsers).catch(fail);
    loadRepositories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  useEffect(() => {
    setFolders([]);
    setFolderId(null);
    if (repositoryId != null) loadFolders(repositoryId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [repositoryId]);

  useEffect(() => {
    setDocuments([]);
    setSelectedDocId(null);
    if (folderId != null) loadDocuments(folderId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [folderId]);

  const depth = (f: Folder): number => {
    const parent = folders.find((p) => p.id === f.parentId);
    return parent ? depth(parent) + 1 : 0;
  };

  return (
    <main className="columns">
      {error && (
        <p className="error full" onClick={() => setError(null)}>
          {error}
        </p>
      )}

      <section>
        <h2>Repozytoria</h2>
        <ul className="list">
          {repositories.map((r) => (
            <li key={r.id} className={r.id === repositoryId ? 'active' : ''} onClick={() => setRepositoryId(r.id)}>
              <b>{r.name}</b>
              <small>właściciel: {r.owner}</small>
            </li>
          ))}
          {repositories.length === 0 && <li className="muted">Brak repozytoriów z upoważnieniem.</li>}
        </ul>
        <InlineCreate
          placeholder="Nowe repozytorium"
          onCreate={async (name) => {
            const repo = await client.createRepository(name, '').catch((e) => {
              fail(e);
              throw e;
            });
            await loadRepositories();
            setRepositoryId(repo.id);
          }}
        />
      </section>

      <section>
        <h2>Foldery</h2>
        {repositoryId == null && <p className="muted">Wybierz repozytorium.</p>}
        <ul className="list">
          {folders.map((f) => (
            <li
              key={f.id}
              className={f.id === folderId ? 'active' : ''}
              onClick={() => setFolderId(f.id)}
              style={{ paddingLeft: 8 + depth(f) * 16 }}
            >
              📁 {f.name} <small className="tag">{f.access === 'WRITE' ? 'zapis' : 'odczyt'}</small>
            </li>
          ))}
        </ul>
        {folder && canWrite && (
          <InlineCreate
            placeholder={`Podfolder w „${folder.name}”`}
            onCreate={async (name) => {
              await client.createFolder(folder.id, name).catch((e) => {
                fail(e);
                throw e;
              });
              if (repositoryId != null) await loadFolders(repositoryId);
            }}
          />
        )}
      </section>

      <section className="wide">
        <h2>Dokumenty</h2>
        {folderId == null && <p className="muted">Wybierz folder.</p>}
        {folderId != null && (
          <table>
            <thead>
              <tr>
                <th>Tytuł</th>
                <th>Wersja</th>
                <th>Ostatnie działanie</th>
                <th>Stan</th>
                <th>Autor</th>
              </tr>
            </thead>
            <tbody>
              {documents.map((d) => (
                <tr
                  key={d.id}
                  className={`clickable ${d.id === selectedDocId ? 'active' : ''}`}
                  onClick={() => setSelectedDocId(d.id)}
                >
                  <td title={d.description ?? ''}>
                    {d.title}
                    {d.restricted && (
                      <span className="tag" title="widoczny tylko dla wybranych osób">
                        🔒 ograniczony
                      </span>
                    )}
                    {d.fileName && <small className="block">📎 {d.fileName}</small>}
                  </td>
                  <td>{d.latestVersion ?? '—'}</td>
                  <td>{d.lastAction ?? '—'}</td>
                  <td>{d.approved ? 'zatwierdzona' : 'robocza'}</td>
                  <td>{d.author}</td>
                </tr>
              ))}
              {documents.length === 0 && (
                <tr>
                  <td colSpan={5} className="muted">
                    Brak dokumentów w tym folderze.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}

        {selectedDoc && (
          <DocumentPanel
            token={token}
            doc={selectedDoc}
            canWrite={canWrite}
            users={users}
            onChanged={() => folderId != null && loadDocuments(folderId)}
            onClose={() => setSelectedDocId(null)}
          />
        )}

        {folderId != null && !canWrite && folder && (
          <p className="muted">Masz upoważnienie tylko do odczytu tego folderu.</p>
        )}
        {folderId != null && canWrite && (
          <NewDocumentForm
            users={users}
            meId={meId}
            onSubmit={async (doc) => {
              setError(null);
              await client.createDocument(folderId, doc);
              await loadDocuments(folderId);
            }}
          />
        )}
        {folder && canWrite && (
          <FolderAccessPanel token={token} folderId={folder.id} folderName={folder.name} users={users} />
        )}
      </section>
    </main>
  );
}
