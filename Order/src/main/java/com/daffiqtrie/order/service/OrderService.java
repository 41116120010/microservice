package com.daffiqtrie.order.service;

import java.util.List;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Service;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.client.RestTemplate;

import com.daffiqtrie.order.model.Order;
import com.daffiqtrie.order.repository.OrderRepository;
import com.daffiqtrie.order.vo.Produk;
import com.daffiqtrie.order.vo.ResponseTemplate;
import com.netflix.discovery.converters.Auto;

@Service
public class OrderService {
    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Queue myQueue;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(myQueue.getName(), message);
        System.out.println("Message sent: " + message);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        sendMessage("Order created: " + "ID: " + savedOrder.getId() + " ID Produk: " + savedOrder.getIdProduk()
                + " Jumlah: " + savedOrder.getJumlah() + " Harga Satuan: " + savedOrder.getHarga() + " Total Harga: "
                + savedOrder.getTotal() + " ID Pelanggan: " + savedOrder.getIdPelanggan());
        return savedOrder;
    }

    public Order updateOrder(Order order) {
        sendMessage("Order updated: " + "ID: " + order.getId() + " ID Produk: " + order.getIdProduk() + " Jumlah: "
                + order.getJumlah() + "Harga Satuan: " + order.getHarga() + " Total Harga: " + order.getTotal()
                + " ID Pelanggan: " + order.getIdPelanggan());
        return orderRepository.save(order);
    }

    public void deleteOrder(Integer id) {
        sendMessage("Order deleted: " + id);
        orderRepository.deleteById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteOrdersByPelanggan(Integer idPelanggan) {
        orderRepository.deleteByIdPelanggan(idPelanggan);
    }

    public List<Order> getOrdersByPelanggan(Integer idPelanggan) {
        return orderRepository.findByIdPelanggan(idPelanggan);
    }

    public ResponseTemplate getOrderWithProduk(Integer id) {
        Order order = getOrderById(id);
        List<ServiceInstance> instances = discoveryClient.getInstances("PRODUK");
        if (instances.isEmpty()) {
            throw new RuntimeException("PRODUK service is not available");
        }
        ServiceInstance serviceInstance = instances.get(0);
        Produk produk = restTemplate.getForObject(
                serviceInstance.getUri() + "/api/produk/" + order.getIdProduk(),
                Produk.class);
        ResponseTemplate vo = new ResponseTemplate();
        vo.setOrder(order);
        vo.setProduk(produk);
        return vo;
    }
}
