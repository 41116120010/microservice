package com.daffiqtrie.order.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daffiqtrie.order.model.Order;
import com.daffiqtrie.order.repository.OrderRepository;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Integer id) {
        return orderRepository.findById(id).orElse(null);
    }

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    public void deleteOrder(Integer id) {
        orderRepository.deleteById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteOrdersByPelanggan(Integer idPelanggan) {
        orderRepository.deleteByIdPelanggan(idPelanggan);
    }

    public List<Order> getOrdersByPelanggan(Integer idPelanggan) {
        return orderRepository.findByIdPelanggan(idPelanggan);
    }
}
