import { useEffect, useState } from 'react';
import { useAuth } from 'react-oidc-context';
import { api, type Me } from '../api/client';
import RepositoryPage from './RepositoryPage';

export default function App() {
  const auth = useAuth();
  const [me, setMe] = useState<Me | null>(null);
  const [error, setError] = useState<string | null>(null);
  const token = auth.user?.access_token;

  useEffect(() => {
    if (!token) return;
    api(token)
      .me()
      .then(setMe)
      .catch((e) => setError(String(e.message ?? e)));
  }, [token]);

  if (auth.isLoading) return <p className="center">Ładowanie…</p>;
  if (auth.error) return <p className="center error">Błąd logowania: {auth.error.message}</p>;

  if (!auth.isAuthenticated || !token) {
    return (
      <div className="login">
        <h1>Repozytorium dokumentów</h1>
        <p>Zaloguj się kontem firmowym.</p>
        <button onClick={() => auth.signinRedirect()}>Zaloguj się</button>
      </div>
    );
  }

  return (
    <div className="app">
      <header>
        <strong>Repozytorium dokumentów</strong>
        <span className="user">
          {me ? me.displayName : '…'}
          <button className="link" onClick={() => auth.signoutRedirect()}>
            Wyloguj
          </button>
        </span>
      </header>
      {error && <p className="error">{error}</p>}
      {me && <RepositoryPage token={token} meId={me.id} />}
    </div>
  );
}
