package io.nein.demo;

import io.undertow.Undertow;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.undertow.UndertowAuthenticationMechanism;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Arrays;

public class Starter
{
    private static final Logger LOGGER = LoggerFactory.getLogger( Starter.class );

    private static IdentityManager identityManager = new IdentityManager()
    {
        @Override
        public Account verify( Account account )
        {
            return account;
        }

        @Override
        public Account verify( String id,
                               Credential credential )
        {
            throw new IllegalStateException( "Should never be called in Keycloak flow" );
        }

        @Override
        public Account verify( Credential credential )
        {
            throw new IllegalStateException( "Should never be called in Keycloak flow" );
        }
    };

    public static void main( final String[] args )
    {
        LOGGER.trace( "ENTRY" );

        try
            {
            final AdapterConfig adapterConfig =
                    KeycloakDeploymentBuilder.loadAdapterConfig(
                            new FileInputStream( "keycloak.json" ) );

            final KeycloakDeployment keycloakDeployment =
                    KeycloakDeploymentBuilder.build( adapterConfig );

            final AdapterDeploymentContext adapterDeploymentContext =
                    new AdapterDeploymentContext( keycloakDeployment );

            final UndertowAuthenticationMechanism keycloakAuthMech =
                    new UndertowAuthenticationMechanism( adapterDeploymentContext,
                                                         new UndertowUserSessionManagement(),
                                                         new NodesRegistrationManagement(),
                                                         443, "" );

            /* Handlers will be invoked in reverse order of declaration. */

            /* Innermost handler; causes the AuthenticationMechanism(s) to be invoked. */
            final AuthenticationCallHandler authenticationCallHandler =
                    new AuthenticationCallHandler( Starter::respond );

            /* Handler to answer question “is authentication required”. Always requires
            authentication. */
            final AuthenticationConstraintHandler authenticationConstraintHandler =
                    new AuthenticationConstraintHandler( authenticationCallHandler );

            /* Handler to install AuthenticationMechanism(s). */
            final AuthenticationMechanismsHandler authenticationMechanismsHandler =
                    new AuthenticationMechanismsHandler( authenticationConstraintHandler,
                                                         Arrays.asList( keycloakAuthMech ) );

            /* Outermost handler: installs SecurityContext. */
            final SecurityInitialHandler securityInitialHandler =
                    new SecurityInitialHandler( AuthenticationMode.PRO_ACTIVE, identityManager,
                                                authenticationMechanismsHandler );

            final Undertow server = Undertow.builder()
                                            .addHttpListener( 8080, "localhost" )
                                            .setHandler( securityInitialHandler )
                                            .build();
            server.start();
            }
        catch ( Exception e )
            {
            LOGGER.error( "Bootstrap failed!", e );
            }

        LOGGER.trace( "RETURN" );
    }

    private static void respond( final HttpServerExchange exchange )
    {
        LOGGER.trace( "ENTRY" );

        final String response = exchange.getSecurityContext().getAuthenticatedAccount()
                                        .getPrincipal().getName();

        exchange.getResponseHeaders().put( Headers.CONTENT_TYPE, "test/plain" );
        exchange.getResponseSender().send( response );

        LOGGER.trace( "RETURN" );
    }

}
