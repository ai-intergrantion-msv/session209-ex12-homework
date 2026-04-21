package com.restaurant.service;

public class OrderService {
    private OrderRepository orderRepository;



    public OrderService() {

        this.orderRepository = new OrderRepository();

    }

    public String getAllOrders() {

        return orderRepository.getAllOrders();

    }



    public String getOrderById(Long id) {

        return orderRepository.getOrderById(id);

    }
}
