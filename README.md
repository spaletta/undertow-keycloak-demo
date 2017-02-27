# keycloak-undertow demo

This project demonstrates how to bootstrap an Undertow server with the keycloak-undertow-adapter for bearer-token authentication.

To use it, you only need to place a `keycloak.json` file in the current directory. You can obtain this file from the _Installation_ tab after configuring the _client_ required for this server in the Keycloak admin console. The file is read only at runtime.

## Note on using with Let’s Encrypt or otherwise untrusted CA certificate

Your Resource Server (i.e., this software) will need to connect to your Authorization Server (i.e., keycloak) over TLS.

This only works when the Authorization Server certificate is trusted by the Java VM. At the time of writing, Oracle JRE does not have the Let’s Encrypt Root in its trust store.

You can create a trust store with a custom Root CA added by following these steps:

1. Copy the JRE trust store file `cacerts` from the location of the JRE to the current directory.
2. For the Let’s Encrypt Root, obtain `isrgrootx1.der` from https://letsencrypt.org. For another Root, substitute accordingly.
3. Run `keytool -trustcacerts -keystore cacerts -storepass changeit -importcert -alias isrgrootx1 -file isrgrootx1.der` and confirm the action.
4. When running this code, add JVM options to use the updated copy of the trust store: `-Djavax.net.ssl.trustStore=cacerts -Djavax.net.ssl.trustStorePassword=changeit`.
