package io.mcdxai.harness.universal.mcp;

import org.apache.catalina.loader.WebappClassLoaderBase;

/**
 * Classloader for embedded Tomcat that skips webapp leak-detection cleanup.
 * The default WebappClassLoaderBase tries to deregister JDBC drivers, detect
 * ThreadLocal leaks, etc. — checks that fail under Fabric's Knot classloader
 * and have no relevance to an embedded MCP server.
 */
public class EmbeddedWebappClassLoader extends WebappClassLoaderBase {

    public EmbeddedWebappClassLoader() {
        super();
    }

    public EmbeddedWebappClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public EmbeddedWebappClassLoader copyWithoutTransformers() {
        EmbeddedWebappClassLoader copy = new EmbeddedWebappClassLoader(getParent());
        copy.setDelegate(getDelegate());
        return copy;
    }

    @Override
    protected void clearReferences() {
    }
}
