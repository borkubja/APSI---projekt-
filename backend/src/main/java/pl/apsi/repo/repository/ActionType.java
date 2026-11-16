package pl.apsi.repo.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Pozycja listy rozwijanej działań, np. "Zrecenzowałem", powiązana z rolą. */
@Entity
@Table(name = "action_type")
public class ActionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id")
    private RoleDefinition role;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStage stage;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ActionType() {
    }

    public ActionType(RoleDefinition role, String name, ActionStage stage, int sortOrder) {
        this.role = role;
        this.name = name;
        this.stage = stage;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public RoleDefinition getRole() { return role; }
    public String getName() { return name; }
    public ActionStage getStage() { return stage; }
    public int getSortOrder() { return sortOrder; }
}
