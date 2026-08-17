# Capstone Authorization Server

This is the authorization server for the MD282 capstone, ready to run. It is the
Lab 4.8 auth server (single BFF client, RP-initiated logout, custom token claims)
with the capstone user list in place. Drop it in at the start of the capstone, run
it, and it issues the tokens the BFF and the BankService expect.

## Running it

Open the project in IntelliJ and run `AuthServerApplication`, or from the command
line with a local Maven:

```
mvn spring-boot:run
```

(If you want the `./mvnw` wrapper in the project, generate it once with
`mvn -N wrapper:wrapper`.)

It starts on port **9000**. Confirm it is up:

```
http://127.0.0.1:9000/.well-known/openid-configuration
```

The `issuer` field in that document must read `http://127.0.0.1:9000`. If it reads
`localhost`, the resource server's JWT validation will fail with an issuer
mismatch.

## Confirming it works

The project includes `authserver-tests.http`, runnable from IntelliJ's HTTP client.

Requests 1 to 3 run directly and self-verify: the discovery document (and that the
issuer is `127.0.0.1:9000`), the JWKS endpoint publishing a signing key, and the
authorize endpoint redirecting an unauthenticated request to the login page.

Requests 4 to 6 exercise a real login. Because this is a login server, that needs
one browser step: open the authorize URL in the file, log in as `487-978493` /
`password`, and copy the `code` value from the address bar (the browser will try to
reach the BFF at `localhost:8080` and fail to load, which is fine; the code is in
the URL). Paste it into request 4 and run 4, 5, and 6 to exchange the code for
tokens, call userinfo, and refresh. A closing note in the file shows how to decode
the access token to confirm the capstone claims (`sub` as the customer_number,
`roles`, and the narrowed `scope`).

## What changed from the labs

Everything structural carries over from Labs 2.1, 4.6, 4.7, and 4.8 unchanged: the
two filter chains, the `BankUser` custom `UserDetails`, the token customizer, the
`bank-client-bff` registration with its login and post-logout redirect URIs, and the
RSA signing key. The capstone-specific changes are confined to the user list:

- **Two roles only.** `account_holder` and `teller`. The `auditor` role, its user,
  and its scope set are removed.
- **Customers log in with their `customer_number`.** The login name and the `sub`
  claim are both the customer_number (for example `487-978493`), so the BankService
  can resolve ownership by matching `sub` to `customers.customer_number`. This
  replaces the lab's separate `C001`-style subject id.
- **Users are aligned to the seed data.** The two account holders are the two
  seeded customers; the teller is not in the database, as bank staff exist only
  here.

The registered client is just `bank-client-bff`. The old `bank-spa` and
`bank-service` clients are gone, exactly as in Lab 4.8. (If you attempt the secured
external-payment stretch, you would re-add a client-credentials client for the
BankService-to-payment call, like the old `bank-service`.)

## Demo identities

All three share the password `password`.

| Login (also the `sub`) | Role | Name | Notes |
|------------------------|------|------|-------|
| `487-978493` | account_holder | Alice Customer | the seeded Alice, three accounts incl. one inactive |
| `500-100200` | account_holder | Bob Customer | the seeded Bob, one account |
| `teller1` | teller | Teller One | bank staff, no accounts |

## What the token carries

A user access token includes:

- `sub`: the customer_number or the staff username
- `preferred_username`: the login name (same value)
- `name`: the full name
- `roles`: a one-element array, `["account_holder"]` or `["teller"]`
- `scope`: the role's allowed scopes intersected with what the client requested,
  always keeping `openid` and `profile`
- `iss`: `http://127.0.0.1:9000`

## What the resource server (BankService) must do

Two settings on the BankService side make this work, and both are easy to get
wrong:

1. **Issuer URI.** In the BankService `application.yml`:

   ```yaml
   spring:
     security:
       oauth2:
         resourceserver:
           jwt:
             issuer-uri: http://127.0.0.1:9000
   ```

   It must be `127.0.0.1`, matching the `iss` claim above. A `localhost` value here
   is the most common cause of a resource server returning 401 on every call.

2. **Map the `roles` claim to authorities.** The capstone authorizes with
   `hasRole('account_holder')` and `hasRole('teller')`, so the BankService must turn
   the token's `roles` claim into `ROLE_` authorities with a
   `JwtAuthenticationConverter`. Without this, the roles are invisible to
   `@PreAuthorize` and every role check fails. The converter reads `roles` and
   prefixes each with `ROLE_`.

## A note on hosts

The auth server uses `127.0.0.1`; the BFF and the SPA use `localhost` for
themselves. That mix is intentional and matches the labs: the browser treats the two
as different cookie origins, so the auth server is pinned to one of them
consistently. The BFF's own URLs stay on `localhost:8080` and `localhost:5173`,
while everything that validates a token from this server points at
`http://127.0.0.1:9000`.
