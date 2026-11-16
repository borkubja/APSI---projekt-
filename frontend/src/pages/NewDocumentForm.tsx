import { useState, type FormEvent } from 'react';
import type { NewDocument, User } from '../api/client';

const EMPTY: NewDocument = { title: '', description: '', initialVersion: '0.1', file: null, viewerIds: [] };

/** UC7 Dodaj dokument: tytuł, opis, plik pierwszej wersji i wybór osób, które dokument zobaczą. */
export default function NewDocumentForm({
  users,
  meId,
  onSubmit,
}: {
  users: User[];
  meId: number;
  onSubmit: (doc: NewDocument) => Promise<void>;
}) {
  const [form, setForm] = useState<NewDocument>(EMPTY);
  const [restricted, setRestricted] = useState(false);
  const [fileKey, setFileKey] = useState(0);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const set = <K extends keyof NewDocument>(key: K, value: NewDocument[K]) => setForm({ ...form, [key]: value });
  const toggle = (id: number) =>
    set('viewerIds', form.viewerIds.includes(id) ? form.viewerIds.filter((v) => v !== id) : [...form.viewerIds, id]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit({ ...form, viewerIds: restricted ? form.viewerIds : [] });
      setForm(EMPTY);
      setRestricted(false);
      setFileKey((k) => k + 1); // czyści pole pliku
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="card" onSubmit={submit}>
      <h3>Nowy dokument</h3>
      <label>
        Tytuł
        <input required maxLength={255} value={form.title} onChange={(e) => set('title', e.target.value)} />
      </label>
      <label>
        Opis
        <textarea rows={2} value={form.description} onChange={(e) => set('description', e.target.value)} />
      </label>
      <label>
        Plik (opcjonalnie, do 25 MB)
        <input key={fileKey} type="file" onChange={(e) => set('file', e.target.files?.[0] ?? null)} />
      </label>
      <label className="narrow">
        Etykieta pierwszej wersji
        <input
          pattern="\d+(\.\d+)*"
          title="np. 0.1 lub 1.0"
          value={form.initialVersion}
          onChange={(e) => set('initialVersion', e.target.value)}
        />
      </label>

      <fieldset>
        <legend>Kto zobaczy dokument</legend>
        <label className="check">
          <input type="radio" checked={!restricted} onChange={() => setRestricted(false)} />
          Wszyscy z dostępem do folderu
        </label>
        <label className="check">
          <input type="radio" checked={restricted} onChange={() => setRestricted(true)} />
          Tylko wybrane osoby (i Ty oraz osoby z zapisem do folderu)
        </label>
        {restricted && (
          <div className="checks">
            {users
              .filter((u) => u.id !== meId)
              .map((u) => (
                <label key={u.id} className="check">
                  <input type="checkbox" checked={form.viewerIds.includes(u.id)} onChange={() => toggle(u.id)} />
                  {u.displayName}
                </label>
              ))}
          </div>
        )}
      </fieldset>

      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={saving}>
        {saving ? 'Zapisywanie…' : 'Dodaj dokument'}
      </button>
    </form>
  );
}
