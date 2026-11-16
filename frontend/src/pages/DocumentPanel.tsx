import { useEffect, useState } from 'react';
import { api, type DocumentItem, type User, type Version } from '../api/client';

const TEXT_EXT = /\.(txt|md|csv|json|xml|html?|ya?ml|log|sql|java|ts|tsx|js|css)$/i;
const isText = (v: Version) => !!v.fileName && (v.mimeType?.startsWith('text/') || TEXT_EXT.test(v.fileName));
const fmtSize = (n: number | null) => (n == null ? '' : n < 1024 ? `${n} B` : `${(n / 1024).toFixed(1)} KB`);
const fmtDate = (s: string) => new Date(s).toLocaleString('pl-PL');

/**
 * Szczegóły dokumentu: drzewo wersji z historią obiegu, odczyt/pobranie pliku, edycja (= nowa wersja),
 * zatwierdzanie przez osobę z zapisem oraz zmiana widoczności.
 */
export default function DocumentPanel({
  token,
  doc,
  canWrite,
  users,
  onChanged,
  onClose,
}: {
  token: string;
  doc: DocumentItem;
  canWrite: boolean;
  users: User[];
  onChanged: () => void;
  onClose: () => void;
}) {
  const client = api(token);
  const [versions, setVersions] = useState<Version[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // edycja tekstu / podgląd
  const [editing, setEditing] = useState<{ version: Version; text: string; readOnly: boolean } | null>(null);
  const [note, setNote] = useState('');

  // nowa wersja z pliku
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadParent, setUploadParent] = useState('');
  const [uploadNote, setUploadNote] = useState('');
  const [uploadKey, setUploadKey] = useState(0);

  // widoczność
  const [restricted, setRestricted] = useState(doc.restricted);
  const [viewers, setViewers] = useState<number[]>(doc.viewerIds);

  const fail = (e: unknown) => setError(e instanceof Error ? e.message : String(e));
  const load = () => client.versions(doc.id).then(setVersions).catch(fail);

  useEffect(() => {
    setEditing(null);
    setError(null);
    setRestricted(doc.restricted);
    setViewers(doc.viewerIds);
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [doc.id]);

  const run = async (action: () => Promise<unknown>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
      await load();
      onChanged();
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  };

  const download = (v: Version) =>
    client
      .fileBlob(v.id)
      .then((blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = v.fileName ?? `wersja-${v.versionLabel}`;
        a.click();
        URL.revokeObjectURL(url);
      })
      .catch(fail);

  const open = (v: Version, readOnly: boolean) =>
    client
      .fileBlob(v.id)
      .then((b) => b.text())
      .then((text) => {
        setNote('');
        setEditing({ version: v, text, readOnly });
      })
      .catch(fail);

  const saveEdit = () =>
    editing &&
    run(async () => {
      const v = editing.version;
      const file = new File([editing.text], v.fileName ?? 'dokument.txt', { type: v.mimeType ?? 'text/plain' });
      await client.addVersion(doc.id, file, v.id, note || `Edycja wersji ${v.versionLabel}`);
      setEditing(null);
    });

  const latestId = versions.length ? versions[versions.length - 1].id : null;
  const toggleViewer = (id: number) =>
    setViewers(viewers.includes(id) ? viewers.filter((v) => v !== id) : [...viewers, id]);
  const labelOf = (id: number | null) => versions.find((v) => v.id === id)?.versionLabel;

  return (
    <div className="card detail">
      <div className="row">
        <h3>{doc.title}</h3>
        <button className="link" onClick={onClose}>
          zamknij ✕
        </button>
      </div>
      {doc.description && <p className="muted">{doc.description}</p>}
      {error && (
        <p className="error" onClick={() => setError(null)}>
          {error}
        </p>
      )}

      <h4>Wersje</h4>
      <table>
        <thead>
          <tr>
            <th>Wersja</th>
            <th>Z wersji</th>
            <th>Stan</th>
            <th>Autor / data</th>
            <th>Plik</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {versions.map((v) => (
            <tr key={v.id} className={v.id === latestId ? 'latest' : ''}>
              <td>
                <b>{v.versionLabel}</b>
                {v.id === latestId && <span className="tag">najnowsza</span>}
                <small className="block">{v.changeNote}</small>
              </td>
              <td>{labelOf(v.parentVersionId) ?? '—'}</td>
              <td>{v.approved ? '🔒 zatwierdzona' : 'robocza'}</td>
              <td>
                {v.author}
                <small className="block">{fmtDate(v.createdAt)}</small>
              </td>
              <td>
                {v.fileName ? (
                  <>
                    {v.fileName}
                    <small className="block">{fmtSize(v.sizeBytes)}</small>
                  </>
                ) : (
                  <span className="muted">brak</span>
                )}
              </td>
              <td className="actions">
                {v.fileName && (
                  <button className="link" onClick={() => download(v)}>
                    pobierz
                  </button>
                )}
                {isText(v) && (
                  <button className="link" onClick={() => open(v, true)}>
                    czytaj
                  </button>
                )}
                {canWrite && isText(v) && (
                  <button className="link" onClick={() => open(v, false)}>
                    edytuj
                  </button>
                )}
                {canWrite && !v.approved && (
                  <button className="link ok" disabled={busy} onClick={() => run(() => client.approve(v.id, ''))}>
                    zatwierdź
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h4>Historia obiegu</h4>
      <ul className="history">
        {versions.flatMap((v) =>
          v.history.map((h, i) => (
            <li key={`${v.id}-${i}`}>
              <b>{v.versionLabel}</b> · {h.action} · {h.user} · {fmtDate(h.createdAt)}
              {h.comment && <span className="muted"> — {h.comment}</span>}
            </li>
          )),
        )}
      </ul>

      {editing && (
        <div className="editor">
          <h4>
            {editing.readOnly ? 'Podgląd' : 'Edycja'} wersji {editing.version.versionLabel} — {editing.version.fileName}
          </h4>
          <textarea
            rows={12}
            readOnly={editing.readOnly}
            value={editing.text}
            onChange={(e) => setEditing({ ...editing, text: e.target.value })}
          />
          {!editing.readOnly && (
            <>
              <input
                placeholder="Co zmieniono? (opis nowej wersji)"
                value={note}
                onChange={(e) => setNote(e.target.value)}
              />
              <p className="muted small">Zapis tworzy nową wersję; poprzednia pozostaje bez zmian.</p>
              <div className="inline">
                <button disabled={busy} onClick={saveEdit}>
                  Zapisz jako nową wersję
                </button>
                <button className="link" onClick={() => setEditing(null)}>
                  anuluj
                </button>
              </div>
            </>
          )}
          {editing.readOnly && (
            <button className="link" onClick={() => setEditing(null)}>
              zamknij podgląd
            </button>
          )}
        </div>
      )}

      {canWrite && (
        <form
          className="upload"
          onSubmit={(e) => {
            e.preventDefault();
            if (!uploadFile) return;
            run(async () => {
              await client.addVersion(doc.id, uploadFile, uploadParent ? Number(uploadParent) : null, uploadNote);
              setUploadFile(null);
              setUploadNote('');
              setUploadKey((k) => k + 1);
            });
          }}
        >
          <h4>Dodaj nową wersję z pliku</h4>
          <input key={uploadKey} type="file" required onChange={(e) => setUploadFile(e.target.files?.[0] ?? null)} />
          <select value={uploadParent} onChange={(e) => setUploadParent(e.target.value)}>
            <option value="">na bazie najnowszej wersji</option>
            {versions.map((v) => (
              <option key={v.id} value={v.id}>
                na bazie {v.versionLabel}
              </option>
            ))}
          </select>
          <input placeholder="Opis zmiany" value={uploadNote} onChange={(e) => setUploadNote(e.target.value)} />
          <button type="submit" disabled={busy || !uploadFile}>
            Dodaj wersję
          </button>
        </form>
      )}

      {canWrite && (
        <div className="visibility">
          <h4>Kto widzi ten dokument</h4>
          <label className="check">
            <input type="radio" checked={!restricted} onChange={() => setRestricted(false)} />
            Wszyscy z dostępem do folderu
          </label>
          <label className="check">
            <input type="radio" checked={restricted} onChange={() => setRestricted(true)} />
            Tylko wybrane osoby (oraz autor i osoby z zapisem do folderu)
          </label>
          {restricted && (
            <div className="checks">
              {users.map((u) => (
                <label key={u.id} className="check">
                  <input type="checkbox" checked={viewers.includes(u.id)} onChange={() => toggleViewer(u.id)} />
                  {u.displayName}
                </label>
              ))}
            </div>
          )}
          <button disabled={busy} onClick={() => run(() => client.setVisibility(doc.id, restricted, viewers))}>
            Zapisz widoczność
          </button>
        </div>
      )}
    </div>
  );
}
