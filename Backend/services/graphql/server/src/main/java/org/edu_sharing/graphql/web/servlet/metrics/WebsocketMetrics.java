package org.edu_sharing.graphql.web.servlet.metrics;

import graphql.kickstart.servlet.GraphQLWebsocketServlet;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

public class WebsocketMetrics {

  public WebsocketMetrics(MeterRegistry meterRegistry, GraphQLWebsocketServlet websocketServlet) {
    Gauge.builder(
            "graphql.websocket.sessions",
            websocketServlet,
            GraphQLWebsocketServlet::getSessionCount)
        .description("Active websocket sessions available for subscriptions")
        .register(meterRegistry);
    Gauge.builder(
            "graphql.websocket.subscriptions",
            websocketServlet,
            GraphQLWebsocketServlet::getSubscriptionCount)
        .description("Active websocket subscriptions")
        .register(meterRegistry);
  }
}
