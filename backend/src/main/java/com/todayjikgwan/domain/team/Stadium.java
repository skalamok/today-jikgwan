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

    /** REQ-F-605. null 로 온 항목은 건드리지 않는다 */
    public void updateInfo(String address, Integer capacity, Integer gridNx, Integer gridNy) {
        if (address != null) this.address = address.isBlank() ? null : address.trim();
        if (capacity != null) this.capacity = capacity;
        if (gridNx != null) this.gridNx = gridNx;
        if (gridNy != null) this.gridNy = gridNy;
    }

    public boolean hasGrid() {
        return gridNx != null && gridNy != null;
    }
}
