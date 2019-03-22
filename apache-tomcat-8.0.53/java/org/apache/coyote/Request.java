/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.coyote;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ReadListener;

import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.buf.UDecoder;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.Parameters;
import org.apache.tomcat.util.http.ServerCookies;
import org.apache.tomcat.util.res.StringManager;

/**
 * Request这个类可谓tomcat的一大核心，几乎所有connector和容器都要用到它
 * <p>
 * Request类实现了对底层http字节流的封装，因为http本质上是从网络过来的一串字节流，并且从逻辑上根据http协议，分成了头和体，其中头部又有很多字段（包括MIME字段）。而Request的作用就是把这些字节封装成对应的字段，并且达到处理效率的最优
 * <p>
 * 因此，Request里面大部分方法是字段的get方法（set方法不多，因为大部分字段是不可改变的），此外还有提供给容器使用的方法，如recycle、inputbuffer等等。但最关键的是，Request是如何提高处理效率的
 * <p>
 * 对于底层的、和字节流打交道的DO（data object），性能瓶颈在于对内存的使用上（因为字节都是放在一块块的内存中），如果能有效的使用内存，就能有效地提高DO的性能。
 * <p>
 * 如果让我们来实现这个Request类，估计大部分人第一反应就是用String来表示每个http头字段，然而String的效率之低下是绝对无法胜任服务器的性能要求的
 * <p>
 * Request的注释告诉我们，它的大部分字段是“GC free”的，即很少、甚至不会被垃圾回收。杜绝了java中最大的一个性能瓶颈，Request自然性能得到大幅提升
 * <p>
 * 此外，其字段的一些耗时操作都会延迟到用户代码一级，也就是说，tomcat内部在使用Request时，都会尽量保证它的字段处于原始的字节状态（而不是图方便到处使用String），直到用户代码（也就是我们写的servlet）需要时才进行转换，如果用不到（其实http请求的大部分字段在我们编程时都用不到），就不作转换。这样又进一步挖掘出更多的性能潜力，其思想和“延迟加载”的设计模式如出一辙。
 * <p>
 * 当然，tomcat的程序员也是人也喜欢偷懒，谁都不乐意直接操纵字节数组，那样出错的风险也大。因此，tomcat的org.apache.tomcat.util包定义了许多底层的工具类，用于操作和维护字节数组。Request的字段们的类型为MessageBytes就是其中的一种
 * <p>
 * 而关于Response，原理和Request类似，但是Response简单了很多，最明显的是里面的字段不像Request那样为效率绞尽脑汁，而是直接用了String，也许是Response对整体效率影响不大，亦或者当前版本的tomcat还未对其进行改造
 * This is a low-level, efficient representation of a server request. Most
 * fields are GC-free（即很少、甚至不会被垃圾回收）, expensive operations are delayed until the  user code
 * needs the information.
 * <p>
 * Processing is delegated to modules, using a hook mechanism.
 * <p>
 * This class is not intended for user code - it is used internally by tomcat
 * for processing the request in the most efficient way. Users ( servlets ) can
 * access the information using a facade, which provides the high-level view
 * of the request.
 * <p>
 * Tomcat defines a number of attributes:
 * <ul>
 * <li>"org.apache.tomcat.request" - allows access to the low-level
 * request object in trusted applications
 * </ul>
 *
 * @author James Duncan Davidson [duncan@eng.sun.com]
 * @author James Todd [gonzo@eng.sun.com]
 * @author Jason Hunter [jch@eng.sun.com]
 * @author Harish Prabandham
 * @author Alex Cruikshank [alex@epitonic.com]
 * @author Hans Bergsten [hans@gefionsoftware.com]
 * @author Costin Manolache
 * @author Remy Maucherat
 */
public final class Request {

    private static final StringManager sm =
            StringManager.getManager(Constants.Package);

    // Expected maximum typica number of cookies per request.
    private static final int INITIAL_COOKIE_SIZE = 4;

    // ----------------------------------------------------------- Constructors

    public Request() {
        parameters.setQuery(queryMB);
        parameters.setURLDecoder(urlDecoder);
    }


    // ----------------------------------------------------- Instance Variables

    private int serverPort = -1;
    private final MessageBytes serverNameMB = MessageBytes.newInstance();

    private int remotePort;
    private int localPort;

    private final MessageBytes schemeMB = MessageBytes.newInstance();

    private final MessageBytes methodMB = MessageBytes.newInstance();
    private final MessageBytes uriMB = MessageBytes.newInstance();
    private final MessageBytes decodedUriMB = MessageBytes.newInstance();
    private final MessageBytes queryMB = MessageBytes.newInstance();
    private final MessageBytes protoMB = MessageBytes.newInstance();

    // remote address/host
    private final MessageBytes remoteAddrMB = MessageBytes.newInstance();
    private final MessageBytes localNameMB = MessageBytes.newInstance();
    private final MessageBytes remoteHostMB = MessageBytes.newInstance();
    private final MessageBytes localAddrMB = MessageBytes.newInstance();

    private final MimeHeaders headers = new MimeHeaders();

    private final MessageBytes instanceId = MessageBytes.newInstance();

    /**
     * Notes.
     */
    private final Object notes[] = new Object[Constants.MAX_NOTES];


    /**
     * Associated input buffer.
     */
    private InputBuffer inputBuffer = null;


    /**
     * URL decoder.
     */
    private final UDecoder urlDecoder = new UDecoder();


    /**
     * HTTP specific fields. (remove them ?)
     */
    private long contentLength = -1;
    private MessageBytes contentTypeMB = null;
    private String charEncoding = null;

    private final ServerCookies serverCookies = new ServerCookies(INITIAL_COOKIE_SIZE);
    private final Parameters parameters = new Parameters();

    private final MessageBytes remoteUser = MessageBytes.newInstance();
    private boolean remoteUserNeedsAuthorization = false;
    private final MessageBytes authType = MessageBytes.newInstance();
    private final HashMap<String, Object> attributes = new HashMap<>();

    private Response response;
    private volatile ActionHook hook;

    private long bytesRead = 0;
    // Time of the request - useful to avoid repeated calls to System.currentTime
    private long startTime = -1;
    private int available = 0;

    private final RequestInfo reqProcessorMX = new RequestInfo(this);


    volatile ReadListener listener;

    public ReadListener getReadListener() {
        return listener;
    }

    public void setReadListener(ReadListener listener) {
        if (listener == null) {
            throw new NullPointerException(
                    sm.getString("request.nullReadListener"));
        }
        if (getReadListener() != null) {
            throw new IllegalStateException(
                    sm.getString("request.readListenerSet"));
        }
        // Note: This class is not used for HTTP upgrade so only need to test
        //       for async
        AtomicBoolean result = new AtomicBoolean(false);
        action(ActionCode.ASYNC_IS_ASYNC, result);
        if (!result.get()) {
            throw new IllegalStateException(
                    sm.getString("request.notAsync"));
        }

        this.listener = listener;
    }

    private final AtomicBoolean allDataReadEventSent = new AtomicBoolean(false);

    public boolean sendAllDataReadEvent() {
        return allDataReadEventSent.compareAndSet(false, true);
    }


    // ------------------------------------------------------------- Properties

    /**
     * Get the instance id (or JVM route). Currently Ajp is sending it with each
     * request. In future this should be fixed, and sent only once ( or
     * 'negotiated' at config time so both tomcat and apache share the same name.
     *
     * @return the instance id
     */
    public MessageBytes instanceId() {
        return instanceId;
    }


    public MimeHeaders getMimeHeaders() {
        return headers;
    }


    public UDecoder getURLDecoder() {
        return urlDecoder;
    }

    // -------------------- Request data --------------------


    public MessageBytes scheme() {
        return schemeMB;
    }

    public MessageBytes method() {
        return methodMB;
    }

    public MessageBytes requestURI() {
        return uriMB;
    }

    public MessageBytes decodedURI() {
        return decodedUriMB;
    }

    public MessageBytes queryString() {
        return queryMB;
    }

    public MessageBytes protocol() {
        return protoMB;
    }

    /**
     * Return the buffer holding the server name, if
     * any. Use isNull() to check if there is no value
     * set.
     * This is the "virtual host", derived from the
     * Host: header.
     */
    public MessageBytes serverName() {
        return serverNameMB;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public MessageBytes remoteAddr() {
        return remoteAddrMB;
    }

    public MessageBytes remoteHost() {
        return remoteHostMB;
    }

    public MessageBytes localName() {
        return localNameMB;
    }

    public MessageBytes localAddr() {
        return localAddrMB;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int port) {
        this.remotePort = port;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int port) {
        this.localPort = port;
    }

    // -------------------- encoding/type --------------------


    /**
     * Get the character encoding used for this request.
     */
    public String getCharacterEncoding() {

        if (charEncoding != null) {
            return charEncoding;
        }

        charEncoding = getCharsetFromContentType(getContentType());
        return charEncoding;

    }


    public void setCharacterEncoding(String enc) {
        this.charEncoding = enc;
    }


    public void setContentLength(long len) {
        this.contentLength = len;
    }


    public int getContentLength() {
        long length = getContentLengthLong();

        if (length < Integer.MAX_VALUE) {
            return (int) length;
        }
        return -1;
    }

    public long getContentLengthLong() {
        if (contentLength > -1) {
            return contentLength;
        }

        MessageBytes clB = headers.getUniqueValue("content-length");
        contentLength = (clB == null || clB.isNull()) ? -1 : clB.getLong();

        return contentLength;
    }

    public String getContentType() {
        contentType();
        if ((contentTypeMB == null) || contentTypeMB.isNull()) {
            return null;
        }
        return contentTypeMB.toString();
    }


    public void setContentType(String type) {
        contentTypeMB.setString(type);
    }


    public MessageBytes contentType() {
        if (contentTypeMB == null) {
            contentTypeMB = headers.getValue("content-type");
        }
        return contentTypeMB;
    }


    public void setContentType(MessageBytes mb) {
        contentTypeMB = mb;
    }


    public String getHeader(String name) {
        return headers.getHeader(name);
    }

    // -------------------- Associated response --------------------

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
        response.setRequest(this);
    }

    protected void setHook(ActionHook hook) {
        this.hook = hook;
    }

    public void action(ActionCode actionCode, Object param) {
        if (hook != null) {
            if (param == null) {
                hook.action(actionCode, this);
            } else {
                hook.action(actionCode, param);
            }
        }
    }


    // -------------------- Cookies --------------------

    public ServerCookies getCookies() {
        return serverCookies;
    }


    // -------------------- Parameters --------------------

    public Parameters getParameters() {
        return parameters;
    }


    // -------------------- Other attributes --------------------
    // We can use notes for most - need to discuss what is of general interest

    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    public HashMap<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public MessageBytes getRemoteUser() {
        return remoteUser;
    }

    public boolean getRemoteUserNeedsAuthorization() {
        return remoteUserNeedsAuthorization;
    }

    public void setRemoteUserNeedsAuthorization(boolean remoteUserNeedsAuthorization) {
        this.remoteUserNeedsAuthorization = remoteUserNeedsAuthorization;
    }

    public MessageBytes getAuthType() {
        return authType;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public boolean isFinished() {
        AtomicBoolean result = new AtomicBoolean(false);
        action(ActionCode.REQUEST_BODY_FULLY_READ, result);
        return result.get();
    }

    public boolean getSupportsRelativeRedirects() {
        if (protocol().equals("") || protocol().equals("HTTP/1.0")) {
            return false;
        }
        return true;
    }


    // -------------------- Input Buffer --------------------

    public InputBuffer getInputBuffer() {
        return inputBuffer;
    }


    public void setInputBuffer(InputBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
    }


    /**
     * Read data from the input buffer and put it into a byte chunk.
     * <p>
     * The buffer is owned by the protocol implementation - it will be reused on the next read.
     * The Adapter must either process the data in place or copy it to a separate buffer if it needs
     * to hold it. In most cases this is done during byte-&gt;char conversions or via InputStream. Unlike
     * InputStream, this interface allows the app to process data in place, without copy.
     */
    public int doRead(ByteChunk chunk)
            throws IOException {
        int n = inputBuffer.doRead(chunk, this);
        if (n > 0) {
            bytesRead += n;
        }
        return n;
    }


    // -------------------- debug --------------------

    @Override
    public String toString() {
        return "R( " + requestURI().toString() + ")";
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    // -------------------- Per-Request "notes" --------------------


    /**
     * Used to store private data. Thread data could be used instead - but
     * if you have the req, getting/setting a note is just a array access, may
     * be faster than ThreadLocal for very frequent operations.
     * <p>
     * Example use:
     * Jk:
     * HandlerRequest.HOSTBUFFER = 10 CharChunk, buffer for Host decoding
     * WorkerEnv: SSL_CERT_NOTE=16 - MessageBytes containing the cert
     * <p>
     * Catalina CoyoteAdapter:
     * ADAPTER_NOTES = 1 - stores the HttpServletRequest object ( req/res)
     * <p>
     * To avoid conflicts, note in the range 0 - 8 are reserved for the
     * servlet container ( catalina connector, etc ), and values in 9 - 16
     * for connector use.
     * <p>
     * 17-31 range is not allocated or used.
     */
    public final void setNote(int pos, Object value) {
        notes[pos] = value;
    }


    public final Object getNote(int pos) {
        return notes[pos];
    }


    // -------------------- Recycling --------------------


    public void recycle() {
        bytesRead = 0;

        contentLength = -1;
        contentTypeMB = null;
        charEncoding = null;
        headers.recycle();
        serverNameMB.recycle();
        serverPort = -1;
        localNameMB.recycle();
        localPort = -1;
        remotePort = -1;
        available = 0;

        serverCookies.recycle();
        parameters.recycle();

        uriMB.recycle();
        decodedUriMB.recycle();
        queryMB.recycle();
        methodMB.recycle();
        protoMB.recycle();

        schemeMB.recycle();

        instanceId.recycle();
        remoteUser.recycle();
        remoteUserNeedsAuthorization = false;
        authType.recycle();
        attributes.clear();

        listener = null;
        allDataReadEventSent.set(false);

        startTime = -1;
    }

    // -------------------- Info  --------------------
    public void updateCounters() {
        reqProcessorMX.updateCounters();
    }

    public RequestInfo getRequestProcessor() {
        return reqProcessorMX;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public boolean isProcessing() {
        return reqProcessorMX.getStage() == org.apache.coyote.Constants.STAGE_SERVICE;
    }

    /**
     * Parse the character encoding from the specified content type header.
     * If the content type is null, or there is no explicit character encoding,
     * <code>null</code> is returned.
     *
     * @param contentType a content type header
     */
    private static String getCharsetFromContentType(String contentType) {

        if (contentType == null) {
            return (null);
        }
        int start = contentType.indexOf("charset=");
        if (start < 0) {
            return (null);
        }
        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end >= 0) {
            encoding = encoding.substring(0, end);
        }
        encoding = encoding.trim();
        if ((encoding.length() > 2) && (encoding.startsWith("\""))
                && (encoding.endsWith("\""))) {
            encoding = encoding.substring(1, encoding.length() - 1);
        }
        return (encoding.trim());

    }

}
