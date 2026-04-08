package com.daffiqtrie.produser;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProduserService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Queue myQueue;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(myQueue.getName(), message);
        System.out.println("Message sent: " + message);
    }
}
