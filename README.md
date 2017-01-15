# UAA (`AuthorizationServer`) load balanced behind API-GATEWAY (Edge service `Zuul`)

## Disclamer

**This project is *Proof of concept* (aka `PoC`)**, please before using in production review security concerns among other things. (See https://github.com/kakawait/uaa-behind-zuul-sample/issues/6)

## Change Log

see [CHANGELOG.md](CHANGELOG.md)

## Overview

Quick&dirty sample to expose how to configure `AuthorizationServer` (*UAA*) behind `Zuul`

This way to do may not work for all kind of configuration (I do not test without `JWT` and `prefer-token-info: true`)

## Usage
### Browser
Please deploy every services using [*docker way*](#docker) or [*maven way*](#maven), then simply load `http://localhost:8765/dummy` on your favorite browser.

There are 2 users with user/password 
1. `user/password` with Role `USER`
2. `admin/admin` with Role `ADMIN`

**NOTES:**
- The resource `http://localhost:8765/dummy` is available for both user but `http://localhost:8765/dummy/secret` is only accessible for user having the `ADMIN` role.
- The grant type `authorization_code` is used in this case.

### Command line
1. Obtain the access token and refresh token via one of the 3 equal methods
  - `curl -v --insecure -H "Authorization: Basic $(echo -n 'acme:acmesecret' | base64)" http://localhost:8765/uaa/oauth/token -d grant_type=password -d username=user -d password=password`
  - `curl -v --insecure -u acme:acmesecret http://localhost:8765/uaa/oauth/token -d grant_type=password -d username=user -d password=password`
  - `curl -v --insecure http://acme:acmesecret@localhost:8765/uaa/oauth/token -d grant_type=password -d username=user -d password=password`
  
2. Save the obtained access token in the `ACCESS_TOKEN` env var:
`export ACCESS_TOKEN=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE0ODQ0ODExM...`

3. Access the protected resources:
  - `curl -v -H 'Content-type: application/json' -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8765/dummy/`
  - For `ADMIN` only: `curl -v -H 'Content-type: application/json' -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8765/dummy/secret`. Otherwise you will get a `404`.

**NOTES:**
- The resource `http://localhost:8765/dummy` is available for both user but `http://localhost:8765/dummy/secret` is only accessible for user having the `ADMIN` role.
- The grant type `passowrd` is used in this case.
- You can also create a new user via `curl 'http://localhost:8765/user/register' -i -X POST -H 'Content-Type: application/json' -d '{"username":"myNewUser", "password":"p44rd","role":"ADMIN"}'`. 
ATM it's store in a Map internally, so a restart of the `user-service` will delete the user again. 

### Docker

Start building docker images for every services, simply run following command on root directory

```shell
mvn clean package -Pdocker
```

Launch services using `docker-compose` and remove old images of containers

```shell
docker-compose up -d --remove-orphans
```

### Maven

On each service folder run following command:

```sh
mvn spring-boot:run
```

## Goals

1. Avoid any absolute/hardcoded urls for `security.oauth2.client.accessTokenUri` & `security.oauth2.client.userAuthorizationUri` in order to improve portability!
2. `AuthorizationServer` distribution for HA
3. Do not expose `AuthorizationServer`, like other *service* `AuthorizationServer` will be behind `Zuul`

![network](network.png)

Where `localhost:8765` is `Zuul`, as you can see `AuthorizationServer` is not leaked outside! Only `Zuul` is targeted.

**ATTENTION** for **2.** you should manage yourself shared storage backend (unlike following sample)! Using database or something els.

## Keys points of the sample

### [`Zuul`] Custom `OAuth2ClientContextFilter`

I had to override [`OAuth2ClientContextFilter`](https://github.com/spring-projects/spring-security-oauth/blob/master/spring-security-oauth2/src/main/java/org/springframework/security/oauth2/client/filter/OAuth2ClientContextFilter.java) to support `URI` and not only `URL` (see `DynamicOauth2ClientContextFilter`).
Indeed `URL` does not support path like `/uaa/oauth/authorize`.

Why adding path support on `OAuth2ClientContextFilter`?

Because I want to use path on `security.oauth2.client.userAuthorizationUri` in order to redirect user to `Zuul` itself.

On this case I can't use `http://localhost:${server.port}/uaa/oauth/authorize` because `security.oauth2.client.userAuthorizationUri` is use on web redirection (header `Location: `).

Flow will look like following

```
Browser                             Zuul                               UAA                               USER     
   │        /dummy                   │                                  │                                  │
   ├────────────────────────────────>│                                  │                                  │
   │  Location:http://ZUUL/login     │                                  │                                  │
   │<┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┤                                  │                                  │
   │        /login                   │                                  │                                  │
   ├────────────────────────────────>│                                  │                                  │
   │  Location:/uaa/oauth/authorize  │                                  │                                  │
   │<┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┤                                  │                                  │
   │     /uaa/oauth/authorize        │                                  │                                  │
   ├────────────────────────────────>│                                  │                                  │
   │                                 │      /uaa/oauth/authorize        │                                  │
   │                                 ├┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄>│                                  │
   │                                 │                                  ├──┐                               |
   │                                 │                                  │  │ Not authorize                 │
   │                                 │                                  │<─┘                               │
   │                                 │                                  │                                  │
   │                                 │  Location:http://ZUUL/uaa/login  │                                  │
   │                                 │<┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┤                                  |
   │                                 │                                  │                                  │
   │ Location:http://ZUUL/uaa/login  │                                  │                                  │
   │<┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┤                                  │                                  │
   │       /uaa/login                │                                  │                                  │
   ├────────────────────────────────>│                                  │                                  │
   │                                 │            /uaa/login            │                                  │
   │                                 ├┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄>│                                  │
   │                                 │           LOGIN FORM             │                                  │
   │                                 │<┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┤                                  |
   │           LOGIN FORM            │                                  │                                  │
   │<────────────────────────────────┤                                  │                                  │
   │ {"user":"xx", "password":"yy"}  |                                  │                                  │
   │-───────────────────────────────>|                                  │                                  │
   │                                 |                                  │                                  │
   │                                 |┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄>│                                  |
   │                                 |                                  │       /user/{username}           │
   │                                 |                                  │┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄>│
   │                                 |                                  │                                  │
   │                                 |                                  │                                  │
```

Take attention on second redirection, location is using path (not at browser level).

### [`Zuul`] `localhost` trick for `security.oauth2.client.accessTokenUri`

Unlike `security.oauth2.client.userAuthorizationUri`, `security.oauth2.client.accessTokenUri` is not used a browser level for redirection but used by `RestTemplate`.
However `RestTemplate` does not support `path`, it must be an absolute url.

So to fool the system I'm using loopback trick, by doing that `Zuul` will call itself and forward itself request to `AuthorizationServer`

**ATTENTION** this is a trick we must find something better (https://github.com/spring-projects/spring-security-oauth/issues/671)

### [`Zuul`] Clear `sensitiveHeaders` lists for `AuthorizationServer` route

Since `Brixton.RC1`, `Zuul` filters some headers (http://cloud.spring.io/spring-cloud-static/spring-cloud.html#_cookies_and_sensitive_headers).

By default it filters:

- `Cookie`
- `Set-Cookie`
- `Authorization`

But we need that `AuthorizationServer` could create cookies so we must clear list

```
zuul:
  routes:
    uaa-service:
      sensitiveHeaders:
      path: /uaa/**
      stripPrefix: false
```

**TODO** Check if `zuul.routes.uaa-service.sensitiveHeaders: Authorization` could work?

### [`Zuul`] Disable `XSRF` at gateway level for `AuthorizationServer`

`AuthorizationServer` has it own `XSRF` protection so we must disable at `Zuul` level

```
private RequestMatcher csrfRequestMatcher() {
    return new RequestMatcher() {
        // Always allow the HTTP GET method
        private final Pattern allowedMethods = Pattern.compile("^(GET|HEAD|OPTIONS|TRACE)$");

        // Disable CSFR protection on the following urls:
        private final AntPathRequestMatcher[] requestMatchers = {
                                  new AntPathRequestMatcher("/uaa/**"),
                                  new AntPathRequestMatcher("/user/register") 
        };

        @Override
        public boolean matches(HttpServletRequest request) {
            if (allowedMethods.matcher(request.getMethod()).matches()) {
                return false;
            }

            for (AntPathRequestMatcher matcher : requestMatchers) {
                if (matcher.matches(request)) {
                    return false;
                }
            }
            return true;
        }
    };
}
```

### [`Zuul`] Authorize request to `AuthorizationServer` and `User-Service` for registration

Ok should I really need to explain why?

```
http.authorizeRequests().antMatchers("/uaa/**", "/login", "/user/register").permitAll()
```

**ATTENTION** do not use `"/uaa/**"` authorize only necessary API (I was to lazy)

### [`UAA`] Deploy `AuthorizationServer` on isolated `context-path`

`Zuul` and `AuthorizationServer` have to manage their own session! So both have to write two `JSESSIONID` cookies.

You must isolate `AuthorizationServer` on other context-path `server.context-path = /uaa` to avoid any cookies collision.

**ALTERNATIVE** we can check if `server.session.cookie.path` or `server.session.cookie.name` is not sufficient, I did not test it.

### [`UAA`] Enable `server.use-forward-headers`

Does not work without. I will not explain why, please look about `X-Forwarded-*` headers for more information.

