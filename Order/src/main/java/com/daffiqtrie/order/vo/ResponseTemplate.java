package com.daffiqtrie.order.vo;

import com.daffiqtrie.order.model.Order;
import lombok.Data;

@Data
public class ResponseTemplate {
    private Order order;
    private Produk produk;
}
