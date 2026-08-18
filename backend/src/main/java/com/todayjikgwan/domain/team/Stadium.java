package com.todayjikgwan.domain.team;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "stadiums")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stadium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(length = 255)
    private String address;

    private Integer capacity;

    /** 기상청 단기예보 격자 좌표 (REQ-F-112) */
    @Column(name = "grid_nx")
    private Integer gridNx;

    @Column(name = "grid_ny")
    private Integer gridNy;

    public boolean hasGrid() {
        return gridNx != null && gridNy != null;
    }
}
