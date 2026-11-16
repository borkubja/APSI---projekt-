import { useEffect, useState } from 'react';
import { api, type AccessLevel, type Permission, type User } from '../api/client';

/** UC6: kto ma dostęp do folderu (odczyt albo zapis, dziedziczone przez podfoldery). Widoczne przy zapisie. */
export default function FolderAccessPanel({
  token,
  folderId,
  folderName,
  users,
}: {
  token: string;
  folderId: number;
  folderName: string;
  users: User[];
}) {
  const client = api(token);
  const [perms, setPerms] = useState<Permission[]>([]);
  const [userId, setUserId] = useState('');
  const [level, setLevel] = useState<AccessLevel>('READ');
  const [error, setError] = useState<string | null>(null);

  const fail = (e: unknown) => setError(e instanceof Error ? e.message : String(e));
  const load = () => client.permissions(folderId).then(setPerms).catch(fail);

  useEffect(() => {
    setError(null);
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [folderId]);

  const free = users.filter((u) => !perms.some((p) => p.userId === u.id));

  return (
    <div className="card">
      <h3>Kto ma dostęp do „{folderName}”</h3>
      <p className="muted small">Upoważnienie obowiązuje też w podfolderach.</p>
      <table>
        <tbody>
          {perms.map((p) => (
            <tr key={p.userId}>
              <td>{p.displayName}</td>
              <td>
                <select
                  value={p.level}
                  onChange={(e) =>
                    client
                      .grant(folderId, p.userId, e.target.value as AccessLevel)
                      .then(load)
                      .catch(fail)
                  }
                >
                  <option value="READ">odczyt</option>
                  <option value="WRITE">zapis</option>
                </select>
              </td>
              <td>
                <button className="link" onClick={() => client.revoke(folderId, p.userId).then(load).catch(fail)}>
                  odbierz
                </button>
              </td>
            </tr>
          ))}
          {perms.length === 0 && (
            <tr>
              <td className="muted">Brak upoważnień nadanych bezpośrednio na tym folderze.</td>
            </tr>
          )}
        </tbody>
      </table>
      <div className="inline">
        <select value={userId} onChange={(e) => setUserId(e.target.value)}>
          <option value="">— wybierz osobę —</option>
          {free.map((u) => (
            <option key={u.id} value={u.id}>
              {u.displayName}
            </option>
          ))}
        </select>
        <select value={level} onChange={(e) => setLevel(e.target.value as AccessLevel)}>
          <option value="READ">odczyt</option>
          <option value="WRITE">zapis</option>
        </select>
        <button
          disabled={!userId}
          onClick={() =>
            client
              .grant(folderId, Number(userId), level)
              .then(() => {
                setUserId('');
                return load();
              })
              .catch(fail)
          }
        >
          Nadaj
        </button>
      </div>
      {error && <p className="error">{error}</p>}
    </div>
  );
}
