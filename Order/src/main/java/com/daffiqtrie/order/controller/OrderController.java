package com.daffiqtrie.order.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daffiqtrie.order.model.Order;
import com.daffiqtrie.order.service.OrderService;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Integer id) {
        Order order = orderService.getOrderById(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Integer id, @RequestBody Order order) {
        Order existingOrder = orderService.getOrderById(id);
        if (existingOrder != null) {
            order.setId(id);
            return ResponseEntity.ok(orderService.updateOrder(order));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Integer id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pelanggan/{idPelanggan}")
    public ResponseEntity<Void> deleteOrdersByPelanggan(@PathVariable Integer idPelanggan) {
        orderService.deleteOrdersByPelanggan(idPelanggan);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pelanggan/{idPelanggan}")
    public List<Order> getOrdersByPelanggan(@PathVariable Integer idPelanggan) {
        return orderService.getOrdersByPelanggan(idPelanggan);
    }
}
