import { useState, type FormEvent } from 'react';

/** Małe pole „nazwa + przycisk” do tworzenia repozytorium lub folderu. */
export default function InlineCreate({
  placeholder,
  onCreate,
}: {
  placeholder: string;
  onCreate: (name: string) => Promise<void>;
}) {
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setBusy(true);
    try {
      await onCreate(name.trim());
      setName('');
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="inline" onSubmit={submit}>
      <input value={name} placeholder={placeholder} maxLength={200} onChange={(e) => setName(e.target.value)} />
      <button type="submit" disabled={busy || !name.trim()}>
        +
      </button>
    </form>
  );
}
