package org.axonframework.common.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * Factory for creating wrappers around a Connection, allowing one to override the behavior of the {@link
 * java.sql.Connection#close()} method.
 *
 * @author Allard Buijze
 * @since 2.2
 */
public abstract class ConnectionWrapperFactory {

    private ConnectionWrapperFactory() {
    }

    /**
     * Wrap the given <code>connection</code>, creating a Proxy with an additional <code>wrapperInterface</code>
     * (implemented by given <code>wrapperHandler</code>). Calls to the close method are forwarded to the given
     * <code>closeHandler</code>.
     * <p/>
     * Note that all invocations on methods declared on the <code>wrapperInterface</code> (including equals, hashCode)
     * are forwarded to the <code>wrapperHandler</code>.
     *
     * @param connection       The connection to wrap
     * @param wrapperInterface The additional interface to implement
     * @param wrapperHandler   The implementation for the additional interface
     * @param closeHandler     The handler to redirect close invocations to
     * @param <I>              The type of additional interface for the wrapper to implement
     * @return a wrapped Connection
     */
    public static <I> Connection wrap(final Connection connection, final Class<I> wrapperInterface,
                                      final I wrapperHandler,
                                      final ConnectionCloseHandler closeHandler) {
        return (Connection) Proxy.newProxyInstance(wrapperInterface.getClassLoader(),
                                                   new Class[]{Connection.class, wrapperInterface},
                                                   new InvocationHandler() {
                                                       @Override
                                                       public Object invoke(Object proxy, Method method, Object[] args)
                                                               throws Throwable {
                                                           if ("equals".equals(method.getName()) && args != null
                                                                   && args.length == 1) {
                                                               return proxy == args[0];
                                                           } else if ("hashCode".equals(
                                                                   method.getName()) && isEmpty(args)) {
                                                               return connection.hashCode();
                                                           } else if (method.getDeclaringClass().isAssignableFrom(
                                                                   wrapperInterface)) {
                                                               return method.invoke(wrapperHandler, args);
                                                           } else if ("close".equals(method.getName())
                                                                   && isEmpty(args)) {
                                                               closeHandler.close(connection);
                                                               return null;
                                                           } else {
                                                               return method.invoke(connection, args);
                                                           }
                                                       }
                                                   }
        );
    }

    /**
     * Wrap the given <code>connection</code>, creating a Proxy with an additional <code>wrapperInterface</code>
     * (implemented by given <code>wrapperHandler</code>). Calls to the close method are forwarded to the given
     * <code>closeHandler</code>.
     *
     * @param connection   The connection to wrap
     * @param closeHandler The handler to redirect close invocations to
     * @return a wrapped Connection
     */
    public static Connection wrap(final Connection connection, final ConnectionCloseHandler closeHandler) {
        return (Connection) Proxy.newProxyInstance(closeHandler.getClass().getClassLoader(),
                                                   new Class[]{Connection.class},
                                                   new InvocationHandler() {
                                                       @Override
                                                       public Object invoke(Object proxy, Method method, Object[] args)
                                                               throws Throwable {
                                                           if ("equals".equals(method.getName()) && args != null
                                                                   && args.length == 1) {
                                                               return proxy == args[0];
                                                           } else if ("hashCode".equals(
                                                                   method.getName()) && isEmpty(args)) {
                                                               return connection.hashCode();
                                                           } else if ("close".equals(method.getName())
                                                                   && isEmpty(args)) {
                                                               closeHandler.close(connection);
                                                               return null;
                                                           } else {
                                                               return method.invoke(connection, args);
                                                           }
                                                       }
                                                   }
        );
    }

    private static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Interface defining an operation to close the wrapped connection
     */
    public interface ConnectionCloseHandler {

        /**
         * Close the given <code>connection</code>, which was wrapped by the ConnectionWrapperFactory.
         *
         * @param connection the wrapped connection to close
         */
        void close(Connection connection);
    }

    /**
     * Implementation of ConnectionCloseHandler that does nothing.
     */
    public static class NoOpCloseHandler implements ConnectionCloseHandler {

        @Override
        public void close(Connection connection) {
        }
    }
}