package org.edu_sharing.graphql.context;

import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.DefaultGraphQLServletContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoaderRegistry;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CustomGraphQLContextBuilder implements GraphQLServletContextBuilder {

    private final DataLoaderRegistryFactory dataLoaderRegistryFactory;


    @Override
    public GraphQLContext build(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        DefaultGraphQLServletContext context = DefaultGraphQLServletContext.createServletContext()
                .with(httpServletRequest)
                .with(httpServletResponse)
                .with(dataLoaderRegistryFactory.create()) // Creates a data loader registry per call
                .build();

        return context;
    }

    @Override
    public GraphQLContext build(Session session, HandshakeRequest handshakeRequest) {
        return null;
    }

    @Override
    public GraphQLContext build() {
        return null;
    }
}
