/*
 * Copyright 2013 Andrej Petras <andrej@ajka-andrej.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.edu_sharing.alfresco.monitoring;

/**
 * The application.
 *
 * @author Andrej Petras <andrej@ajka-andrej.com>
 */
public class Application {

    /**
     * The application id.
     */
    private String id;
    /**
     * The application name.
     */
    private String name;
    /**
     * The host.
     */
    private String host;

    /**
     * Gets the host.
     *
     * @return the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the host.
     *
     * @param host the host.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Gets the application id.
     *
     * @return the application id.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the application id.
     *
     * @param id the application id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the application name.
     *
     * @return the application name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the application name.
     *
     * @param name the application name.
     */
    public void setName(String name) {
        this.name = name;
    }
}
