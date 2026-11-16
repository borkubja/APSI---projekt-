package pl.apsi.repo.repository;

/** Poziom upoważnienia do folderu. Kolejność ma znaczenie: WRITE obejmuje READ. */
public enum AccessLevel {
    NONE, READ, WRITE;

    public boolean allows(AccessLevel required) {
        return this.ordinal() >= required.ordinal();
    }

    public static AccessLevel max(AccessLevel a, AccessLevel b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
