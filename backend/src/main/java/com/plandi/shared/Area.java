package com.plandi.shared;

/**
 * Khu vực ở TP Trà Vinh cũ (nay là 4 phường của tỉnh Vĩnh Long). Tên hiển thị theo mốc quen gọi; tọa độ tâm do Plandi đặt
 * để điện thoại tự chọn khu gần nhất mà không gửi vị trí lên server (ADR-0006). Danh sách: spec/SPEC-account.md.
 */
public enum Area {
    TRUNG_TAM("Trung tâm (chợ Trà Vinh, Vincom)", 9.9356, 106.3397),
    DAI_HOC("Khu ĐH Trà Vinh", 9.9234, 106.3469),
    HOA_THUAN("Hòa Thuận (phía Đông)", 9.9528, 106.3661),
    AO_BA_OM("Ao Bà Om, Bảo tàng Khmer", 9.9176, 106.3041),
    LONG_DUC("Long Đức, ven sông Cổ Chiên", 9.9817, 106.3450);

    private final String displayName;
    private final double lat;
    private final double lng;

    Area(String displayName, double lat, double lng) {
        this.displayName = displayName;
        this.lat = lat;
        this.lng = lng;
    }

    public String displayName() {
        return displayName;
    }

    public double lat() {
        return lat;
    }

    public double lng() {
        return lng;
    }
}
