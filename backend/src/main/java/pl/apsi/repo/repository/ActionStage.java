package pl.apsi.repo.repository;

/**
 * Etap obiegu, w którym dostępne jest działanie:
 * START - utworzenie wersji (dodawane automatycznie),
 * MIDDLE - dowolna liczba razy przed zatwierdzeniem,
 * END - zatwierdzenie, blokuje wersję,
 * AFTER_END - po zatwierdzeniu (np. potwierdzenie odbioru).
 */
public enum ActionStage {
    START, MIDDLE, END, AFTER_END
}
