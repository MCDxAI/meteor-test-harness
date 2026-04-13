package com.mcdxai.meteortestharness.mcp;

import org.apache.catalina.loader.WebappClassLoaderBase;

/**
 * Classloader for embedded Tomcat that skips all webapp leak-detection cleanup.
 * <p>
 * The default {@link WebappClassLoaderBase} tries to deregister JDBC drivers,
 * detect ThreadLocal leaks, and clear RMI targets during shutdown.  These checks
 * assume a standard WAR deployment and fail inside Fabric's Knot classloader
 * (e.g. {@code JdbcLeakPrevention.class} is not resolvable as a resource).
 * <p>
 * None of that cleanup is relevant for an embedded, single-servlet MCP server,
 * so we override {@link #clearReferences()} as a no-op.
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
        // No-op: skip JDBC, ThreadLocal, RMI, and other leak detection
        // that does not apply in an embedded Fabric mod environment.
    }
}
