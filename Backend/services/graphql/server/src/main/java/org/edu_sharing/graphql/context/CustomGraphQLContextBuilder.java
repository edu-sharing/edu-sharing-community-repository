package org.edu_sharing.graphql.context;

import graphql.kickstart.execution.context.GraphQLKickstartContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.Session;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CustomGraphQLContextBuilder implements GraphQLServletContextBuilder {

    private final DataLoaderRegistryFactory dataLoaderRegistryFactory;


    @Override
    public GraphQLKickstartContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return GraphQLKickstartContext.of(
                dataLoaderRegistryFactory.create(),
                Map.of(
                        HttpServletRequest.class, httpServletRequest,
                        HttpServletResponse.class, httpServletResponse));
    }

    @Override
    public GraphQLKickstartContext build(Session session, HandshakeRequest handshakeRequest) {
        return null;
    }

    @Override
    public GraphQLKickstartContext build() {
        return null;
    }
}
