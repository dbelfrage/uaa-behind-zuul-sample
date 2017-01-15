package de.hska.iwi.client;

import de.hska.iwi.client.config.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientController {

    private static final Logger LOG = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    SecurityConfig config;

//    @Autowired
    private OAuth2RestOperations restTemplate;

    @RequestMapping(value = "/doLogin", method = RequestMethod.POST)
    public String login(@RequestBody MultiValueMap<String, String> formData) {
        LOG.info("FORM DATA: " + formData);
        this.restTemplate = config.restTemplate(
                formData.getFirst("username"),
                formData.getFirst("password"));
        return "bla redirect here";
    }

    @RequestMapping("/dummy")
    public String dummy() {
        return restTemplate.getForObject("http://api-gateway:8765/dummy", String.class);
//        return restTemplate.getForObject("http://localhost:8765/dummy", String.class);
    }

    @RequestMapping("/dummy/secret")
    public String dummysecret() {
        return restTemplate.getForObject("http://api-gateway:8765/dummy/secret", String.class);
//        return restTemplate.getForObject("http://localhost:8765/dummysecret", String.class);
    }
}

