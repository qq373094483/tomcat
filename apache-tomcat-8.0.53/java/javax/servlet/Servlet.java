/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.servlet;

import java.io.IOException;

/**
 * Defines methods that all servlets must implement.
 * <p>
 * <p>
 * A servlet is a small Java program that runs within a Web server. Servlets
 * receive and respond to requests from Web clients, usually across HTTP, the
 * HyperText Transfer Protocol.
 * <p>
 * <p>
 * To implement this interface, you can write a generic servlet that extends
 * <code>javax.servlet.GenericServlet</code> or an HTTP servlet that extends
 * <code>javax.servlet.http.HttpServlet</code>.
 * <p>
 * <p>
 * This interface defines methods to initialize a servlet, to service requests,
 * and to remove a servlet from the server. These are known as life-cycle
 * methods and are called in the following sequence:
 * <ol>
 * <li>The servlet is constructed, then initialized with the <code>init</code>
 * method.
 * <li>Any calls from clients to the <code>service</code> method are handled.
 * <li>The servlet is taken out of service, then destroyed with the
 * <code>destroy</code> method, then garbage collected and finalized.
 * </ol>
 * <p>
 * <p>
 * In addition to the life-cycle methods, this interface provides the
 * <code>getServletConfig</code> method, which the servlet can use to get any
 * startup information, and the <code>getServletInfo</code> method, which allows
 * the servlet to return basic information about itself, such as author,
 * version, and copyright.
 * servlet的生命周期：
 * <p>
 * 1.加载和实例化
 * <p>
 * 　　Servlet容器负责加载和实例化Servlet。当Servlet容器启动时，或者在容器检测到需要这个Servlet来响应第一个请求时，创建Servlet实例。当Servlet容器
 * <p>
 * 启动后，它必须要知道所需的Servlet类在什么位置，Servlet容器可以从本地文件系统、远程文件系统或者其他的网络服务中通过类加载器加载Servlet类，
 * <p>
 * 成功加载后，容器创建Servlet的实例。因为容器是通过Java的反射API来创建Servlet实例，调用的是Servlet的默认构造方法（即不带参数的构造方法），所
 * <p>
 * 以我们在编写Servlet类的时候，不应该提供带参数的构造方法。
 * <p>
 * <p>
 * <p>
 * 2.初始化
 * <p>
 * 　　在Servlet实例化之后，容器将调用Servlet的init()方法初始化这个对象。初始化的目的是为了让Servlet对象在处理客户端请求前完成一些初始化的工作，
 * <p>
 * 如建立数据库的连接，获取配置信息等。对于每一个Servlet实例，init()方法只被调用一次。在初始化期间，Servlet实例可以使用容器为它准备的
 * <p>
 * ServletConfig对象从Web应用程序的配置信息（在web.xml中配置）中获取初始化的参数信息。在初始化期间，如果发生错误，Servlet实例可以抛出
 * <p>
 * ServletException异常或者UnavailableException异常来通知容器。ServletException异常用于指明一般的初始化失败，例如没有找到初始化参数；而
 * <p>
 * UnavailableException异常用于通知容器该Servlet实例不可用。例如，数据库服务器没有启动，数据库连接无法建立，Servlet就可以抛出
 * <p>
 * UnavailableException异常向容器指出它暂时或永久不可用。
 * <p>
 * <p>
 * <p>
 * I.如何配置Servlet的初始化参数？
 * <p>
 * 在web.xml中该Servlet的定义标记中，比如：
 * <p>
 * <servlet>
 * <servlet-name>TimeServlet</servlet-name>
 * <servlet-class>com.allanlxf.servlet.basic.TimeServlet</servlet-class>
 * <init-param>
 * <param-name>user</param-name>
 * <param-value>username</param-value>
 * </init-param>
 * <init-param>
 * <param-name>blog</param-name>
 * <param-value>http://。。。</param-value>
 * </init-param>
 * </servlet>
 * <p>
 * 配置了两个初始化参数user和blog它们的值分别为username和http://。。。， 这样以后要修改用户名和博客的地址不需要修改Servlet代码，只需修改配置文件即可。
 * <p>
 * II.如何读取Servlet的初始化参数？
 * <p>
 * ServletConfig中定义了如下的方法用来读取初始化参数的信息：
 * <p>
 * public String getInitParameter(String name)
 * <p>
 * 参数：初始化参数的名称。
 * 返回：初始化参数的值，如果没有配置，返回null。
 * <p>
 * III.init(ServletConfig)方法执行次数
 * <p>
 * 在Servlet的生命周期中，该方法执行一次。
 * <p>
 * IV.init(ServletConfig)方法与线程
 * <p>
 * 该方法执行在单线程的环境下，因此开发者不用考虑线程安全的问题。
 * <p>
 * V.init(ServletConfig)方法与异常
 * <p>
 * 该方法在执行过程中可以抛出ServletException来通知Web服务器Servlet实例初始化失败。一旦ServletException抛出，Web服务器不会将客户端请求交给该Servlet实例来处理，而是报告初始化失败异常信息给客户端，该Servlet实例将被从内存中销毁。如果在来新的请求，Web服务器会创建新的Servlet实例，并执行新实例的初始化操作
 * <p>
 * <p>
 * <p>
 * <p>
 * <p>
 * 3.请求处理
 * <p>
 * 　　Servlet容器调用Servlet的service()方法对请求进行处理。要注意的是，在service()方法调用之前，init()方法必须成功执行。在service()方法中，
 * <p>
 * Servlet实例通过ServletRequest对象得到客户端的相关信息和请求信息，在对请求进行处理后，调用ServletResponse对象的方法设置响应信息。在service
 * <p>
 * ()方法执行期间，如果发生错误，Servlet实例可以抛出ServletException异常或者UnavailableException异常。如果UnavailableException异常指示了该实
 * <p>
 * 例永久不可用，Servlet容器将调用实例的destroy()方法，释放该实例。此后对该实例的任何请求，都将收到容器发送的HTTP 404（请求的资源不可用）响应
 * <p>
 * 。如果UnavailableException异常指示了该实例暂时不可用，那么在暂时不可用的时间段内，对该实例的任何请求，都将收到容器发送的HTTP 503（服务器暂
 * <p>
 * 时忙，不能处理请求）响应。
 * <p>
 * <p>
 * <p>
 * I. service()方法的职责
 * <p>
 * service()方法为Servlet的核心方法，客户端的业务逻辑应该在该方法内执行，典型的服务方法的开发流程为：
 * <p>
 * 解析客户端请求-〉执行业务逻辑-〉输出响应页面到客户端
 * <p>
 * II.service()方法与线程
 * <p>
 * 为了提高效率，Servlet规范要求一个Servlet实例必须能够同时服务于多个客户端请求，即service()方法运行在多线程的环境下，Servlet开发者必须保证该方法的线程安全性。
 * <p>
 * III.service()方法与异常
 * <p>
 * service()方法在执行的过程中可以抛出ServletException和IOException。其中ServletException可以在处理客户端请求的过程中抛出，比如请求的资源不可用、数据库不可用等。一旦该异常抛出，容器必须回收请求对象，并报告客户端该异常信息。IOException表示输入输出的错误，编程者不必关心该异常，直接由容器报告给客户端即可。
 * <p>
 * 编程注意事项说明：
 * <p>
 * 1) 当Server Thread线程执行Servlet实例的init()方法时，所有的Client Service Thread线程都不能执行该实例的service()方法，更没有线程能够执行该实例的destroy()方法，因此Servlet的init()方法是工作在单线程的环境下，开发者不必考虑任何线程安全的问题。
 * <p>
 * 2) 当服务器接收到来自客户端的多个请求时，服务器会在单独的Client Service Thread线程中执行Servlet实例的service()方法服务于每个客户端。此时会有多个线程同时执行同一个Servlet实例的service()方法，因此必须考虑线程安全的问题。
 * <p>
 * 3) 请大家注意，虽然service()方法运行在多线程的环境下，并不一定要同步该方法。而是要看这个方法在执行过程中访问的资源类型及对资源的访问方式。分析如下：
 * <p>
 * i. 如果service()方法没有访问Servlet的成员变量也没有访问全局的资源比如静态变量、文件、数据库连接等，而是只使用了当前线程自己的资源，比如非指向全局资源的临时变量、request和response对象等。该方法本身就是线程安全的，不必进行任何的同步控制。
 * <p>
 * ii. 如果service()方法访问了Servlet的成员变量，但是对该变量的操作是只读操作，该方法本身就是线程安全的，不必进行任何的同步控制。
 * <p>
 * iii. 如果service()方法访问了Servlet的成员变量，并且对该变量的操作既有读又有写，通常需要加上同步控制语句。
 * <p>
 * iv. 如果service()方法访问了全局的静态变量，如果同一时刻系统中也可能有其它线程访问该静态变量，如果既有读也有写的操作，通常需要加上同步控制语句。
 * <p>
 * v. 如果service()方法访问了全局的资源，比如文件、数据库连接等，通常需要加上同步控制语句。
 * <p>
 * <p>
 * 4.服务终止
 * <p>
 * 　　当容器检测到一个Servlet实例应该从服务中被移除的时候，容器就会调用实例的destroy()方法，以便让该实例可以释放它所使用的资源，保存数据到持久存
 * <p>
 * 储设备中。当需要释放内存或者容器关闭时，容器就会调用Servlet实例的destroy()方法。在destroy()方法调用之后，容器会释放这个Servlet实例，该实例
 * <p>
 * 随后会被Java的垃圾收集器所回收。如果再次需要这个Servlet处理请求，Servlet容器会创建一个新的Servlet实例。
 * <p>
 * 　　在整个Servlet的生命周期过程中，创建Servlet实例、调用实例的init()和destroy()方法都只进行一次，当初始化完成后，Servlet容器会将该实例保存在内存中，通过调用它的service()方法，为接收到的请求服务。
 *
 * @see GenericServlet
 * @see javax.servlet.http.HttpServlet
 */
public interface Servlet {

    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being placed into service.
     * <p>
     * <p>
     * The servlet container calls the <code>init</code> method exactly once
     * after instantiating the servlet. The <code>init</code> method must
     * complete successfully before the servlet can receive any requests.
     * <p>
     * <p>
     * The servlet container cannot place the servlet into service if the
     * <code>init</code> method
     * <ol>
     * <li>Throws a <code>ServletException</code>
     * <li>Does not return within a time period defined by the Web server
     * </ol>
     *
     * @param config a <code>ServletConfig</code> object containing the servlet's
     *               configuration and initialization parameters
     * @throws ServletException if an exception has occurred that interferes with the
     *                          servlet's normal operation
     * @see UnavailableException
     * @see #getServletConfig
     */
    public void init(ServletConfig config) throws ServletException;

    /**
     * Returns a {@link ServletConfig} object, which contains initialization and
     * startup parameters for this servlet. The <code>ServletConfig</code>
     * object returned is the one passed to the <code>init</code> method.
     * <p>
     * <p>
     * Implementations of this interface are responsible for storing the
     * <code>ServletConfig</code> object so that this method can return it. The
     * {@link GenericServlet} class, which implements this interface, already
     * does this.
     *
     * @return the <code>ServletConfig</code> object that initializes this
     * servlet
     * @see #init
     */
    public ServletConfig getServletConfig();

    /**
     * Called by the servlet container to allow the servlet to respond to a
     * request.
     * <p>
     * <p>
     * This method is only called after the servlet's <code>init()</code> method
     * has completed successfully.
     * <p>
     * <p>
     * The status code of the response always should be set for a servlet that
     * throws or sends an error.
     * <p>
     * <p>
     * <p>
     * Servlets typically run inside multithreaded servlet containers that can
     * handle multiple requests concurrently. Developers must be aware to
     * synchronize access to any shared resources such as files, network
     * connections, and as well as the servlet's class and instance variables.
     * More information on multithreaded programming in Java is available in <a
     * href
     * ="http://java.sun.com/Series/Tutorial/java/threads/multithreaded.html">
     * the Java tutorial on multi-threaded programming</a>.
     *
     * @param req the <code>ServletRequest</code> object that contains the
     *            client's request
     * @param res the <code>ServletResponse</code> object that contains the
     *            servlet's response
     * @throws ServletException if an exception occurs that interferes with the servlet's
     *                          normal operation
     * @throws IOException      if an input or output exception occurs
     */
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException;

    /**
     * Returns information about the servlet, such as author, version, and
     * copyright.
     * <p>
     * <p>
     * The string that this method returns should be plain text and not markup
     * of any kind (such as HTML, XML, etc.).
     *
     * @return a <code>String</code> containing servlet information
     */
    public String getServletInfo();

    /**
     * Called by the servlet container to indicate to a servlet that the servlet
     * is being taken out of service. This method is only called once all
     * threads within the servlet's <code>service</code> method have exited or
     * after a timeout period has passed. After the servlet container calls this
     * method, it will not call the <code>service</code> method again on this
     * servlet.
     * <p>
     * <p>
     * This method gives the servlet an opportunity to clean up any resources
     * that are being held (for example, memory, file handles, threads) and make
     * sure that any persistent state is synchronized with the servlet's current
     * state in memory.
     */
    public void destroy();
}
