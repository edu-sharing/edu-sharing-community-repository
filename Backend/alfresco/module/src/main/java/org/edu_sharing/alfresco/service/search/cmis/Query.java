package org.edu_sharing.alfresco.service.search.cmis;

import lombok.*;

import java.util.*;

public final class Query {
  public static Selection select(String... properties) {
    return select(Arrays.stream(properties).map(Property::new).toArray(Property[]::new));
  }

  public static Selection select(Property... properties) {
    return new Selection(properties);
  }

  public static Selection selectAll() {
    return new Selection();
  }
}




