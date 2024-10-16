package org.edu_sharing.spring.scope.refresh;

/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <a href="https://github.com/spring-cloud/spring-cloud-commons">Source by spring cloud</a>
 */

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RefreshScopeRefreshedEvent extends ApplicationEvent {

    /**
     * Default name for the refresh scope refreshed event.
     */
    public static final String DEFAULT_NAME = "__refreshAll__";

    private String name;

    public RefreshScopeRefreshedEvent() {
        this(DEFAULT_NAME);
    }

    public RefreshScopeRefreshedEvent(String name) {
        super(name);
        this.name = name;
    }

}