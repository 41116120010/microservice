package com.daffiqtrie.produser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProduserController {
    @Autowired
    private ProduserService produserService;

    @GetMapping("/send")
    public String sendMessage(@RequestParam String message) {
        produserService.sendMessage(message);
        return "Message sent: " + message;
    }
}
