package com.todayjikgwan.domain.team;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "stadium_zones")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StadiumZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public static StadiumZone create(Stadium stadium, String name, int sortOrder) {
        StadiumZone z = new StadiumZone();
        z.stadium = stadium;
        z.name = name;
        z.sortOrder = sortOrder;
        z.active = true;
        return z;
    }

    public void rename(String name) { this.name = name; }

    public void reorder(int sortOrder) { this.sortOrder = sortOrder; }

    /** 지우는 대신 비활성으로 돌린다. 과거 기록이 이 구역을 참조하기 때문이다 (REQ-F-605) */
    public void setActive(boolean active) { this.active = active; }
}
