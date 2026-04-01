package com.daffiqtrie.order.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import com.daffiqtrie.order.vo.ResponseTemplate;

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
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/produk/{id}")
    public ResponseEntity<ResponseTemplate> getOrderWithProduk(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(orderService.getOrderWithProduk(id));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not available")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Integer id, @RequestBody Order order) {
        try {
            orderService.getOrderById(id);
            order.setId(id);
            return ResponseEntity.ok(orderService.updateOrder(order));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
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
