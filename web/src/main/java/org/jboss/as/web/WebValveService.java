/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.web;

import org.apache.catalina.Valve;
import org.apache.tomcat.util.IntrospectionUtils;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service creating and registering a global valve.
 *
 * @author Jean-Frederic Clere
 */
public class WebValveService implements Service<Valve> {

    private final String classname;
    private final String module;
    private ModelNode params;

    private volatile String filePath;
    private volatile String fileRelativeTo;

    private final InjectedValue<PathManager> pathManagerInjector = new InjectedValue<PathManager>();
    private final InjectedValue<WebServer> webServer = new InjectedValue<WebServer>();
    private Valve valve;

    private PathManager.Callback.Handle callbackHandle;

    public WebValveService(String name, String classname, String module) {
        this.classname = classname;
        this.module = module;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        String filename = pathManagerInjector.getValue().resolveRelativePathEntry(filePath, fileRelativeTo);
        Valve valve = null;
        try {
            valve = WebValve.createValve(filename, classname, this.getClass().getClassLoader());
        } catch (Exception e) {
            throw new StartException(e);
        }
        /* Process parameters */
        for (final Property param : params.asPropertyList()) {
            IntrospectionUtils.setProperty(valve, param.getName(), param.getValue().asString());
        }
        webServer.getValue().addValve(valve);
        this.valve = valve;
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
        final Valve valve = this.valve;
        this.valve = null;
        final WebServer server = webServer.getValue();
        server.removeValve(valve);
    }

    /** {@inheritDoc} */
    public synchronized Valve getValue() throws IllegalStateException {
        final Valve valve = this.valve;
        if(valve == null) {
            throw new IllegalStateException();
        }
        return valve;
    }

    public InjectedValue<PathManager> getPathManagerInjector() {
        return pathManagerInjector;
    }

    public InjectedValue<WebServer> getWebServer() {
        return webServer;
    }

    void setFilePaths(String path, String relativeTo) {
        this.filePath = path;
        this.fileRelativeTo = relativeTo;
    }

    public ModelNode getParam() {
        return params;
    }

    public void setParam(ModelNode param) {
        this.params = param;
    }
}
