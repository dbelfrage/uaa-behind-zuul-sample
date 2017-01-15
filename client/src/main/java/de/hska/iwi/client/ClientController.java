package de.hska.iwi.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientController {

    @Autowired
    OAuth2RestOperations restTemplate;

    @RequestMapping("/dummy")
    public String dummy() {
        return restTemplate.getForObject("http://api-gateway:8765/dummy", String.class);
//        return restTemplate.getForObject("http://localhost:8765/dummy", String.class);
    }

    @RequestMapping("/dummysecret")
    public String dummysecret() {
        return restTemplate.getForObject("http://api-gateway:8765/dummy/secret", String.class);
//        return restTemplate.getForObject("http://localhost:8765/dummysecret", String.class);
    }
}

