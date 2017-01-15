package de.hska.iwi.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Adelheid Knodel
 */
// Enable OAuth2 Single Sign On
//    an OAuth2 client authentication filter is added to the Spring http security chain. 
//    It can be used to authenticate users and request access code for resource access.
@Configuration
@EnableOAuth2Client
public class SecurityConfig {

    @Value("${security.oauth2.client.accessTokenUri}")
    private String tokenUrl;

    @Value("${security.oauth2.client.userAuthorizationUri}")
    private String authorizeUrl;

    @Value("${security.oauth2.client.clientId}")
    private String cliendId;

    @Value("${security.oauth2.client.clientSecret}")
    private String clientSecret;


    // TODO does Spring support me this out of the box?
    // Cuz example is from an older version 1.2.1
//    @Bean
    private OAuth2ProtectedResourceDetails resource(String username, String password) {

        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();

        List<String> scopes = new ArrayList<>(2);
        scopes.add("openid");
        resource.setAccessTokenUri(tokenUrl);
        resource.setClientId(cliendId);
        resource.setClientSecret(clientSecret);
        resource.setGrantType("password");
        resource.setScope(scopes);

        resource.setUsername(username);
        resource.setPassword(password);

        return resource;
    }

//    @Bean
    public OAuth2RestOperations restTemplate(String username, String password) {
        AccessTokenRequest atr = new DefaultAccessTokenRequest();
        return new OAuth2RestTemplate(resource(username, password), new DefaultOAuth2ClientContext(atr));
    }
}