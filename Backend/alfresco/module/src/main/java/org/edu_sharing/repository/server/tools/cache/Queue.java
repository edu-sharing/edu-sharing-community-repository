package org.edu_sharing.repository.server.tools.cache;

import javax.annotation.Nonnull;

public interface Queue<T> {

    T poll();

    boolean offer(@Nonnull T e);

    int size();


}
