package com.plandi.account.internal;

import com.plandi.shared.Area;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AreaController {

    record AreaView(String code, String name, double lat, double lng) {}

    // Công khai: form đăng ký cần danh sách, và web tự tính khu gần nhất từ tọa độ tâm (ADR-0006).
    @GetMapping("/api/v1/areas")
    List<AreaView> areas() {
        return Arrays.stream(Area.values())
                .map(a -> new AreaView(a.name(), a.displayName(), a.lat(), a.lng()))
                .toList();
    }
}
