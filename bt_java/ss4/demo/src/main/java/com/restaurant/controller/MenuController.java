package com.restaurant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MenuController {

    @GetMapping("/bai2/menu")
    @ResponseBody
    public String getMenu(
            @RequestParam(name = "category", required = false, defaultValue = "chay") String category
    ) {
        System.out.println(category);
        if (category.equalsIgnoreCase("chay")) {
            return "Thực đơn CHAY: Đậu phụ sốt cà, Rau muống luộc, Canh chua chay.";
        } else if (category.equalsIgnoreCase("man")) {
            return "Thực đơn MẶN: Sườn xào chua ngọt, Cá kho tộ, Thịt kho tàu.";
        } else {
            return "Loại thực đơn '" + category + "' chưa cập nhật.";
        }
    }
}