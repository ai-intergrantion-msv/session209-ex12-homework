package com.restaurant.controller;

import com.restaurant.reponsitory.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller("legacyControllerBai1")
@RequestMapping("/bai1/orders")
public class LegacyController {

    private final OrderService orderService;

    @Autowired
    public LegacyController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @ResponseBody
    public List<String> getAllOrders() {
        return orderService.getAllOrders(); // Trả về JSON list đơn hàng
    }

    @GetMapping("/{id}")
    @ResponseBody
    public String getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping("/add")
    @ResponseBody
    public String addNewOrder(@RequestParam("detail") String detail) {
        System.out.println(detail);
        orderService.createNewOrder(detail);
        return "Đã thêm đơn hàng thành công: " + detail;
    }
}