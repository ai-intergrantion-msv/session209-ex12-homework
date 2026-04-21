package com.restaurant.reponsitory;

import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {
    private List<String> orders = new ArrayList<>();

    public OrderRepository() {
        orders.add("Đơn hàng 01: Phở bò");
        orders.add("Đơn hàng 02: Bún chả");
    }

    public List<String> getAllOrders() {
        return orders;
    }

    public String getOrderById(Long id) {
        if (id >= 0 && id < orders.size()) {
            return orders.get(id.intValue());
        }
        return "Không tìm thấy đơn hàng ID: " + id;
    }

    public void addOrder(String detail) {
        orders.add(detail);
    }
}