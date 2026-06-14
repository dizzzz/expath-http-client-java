/****************************************************************************/
/*  File:       JavaHttpConnection.java                                     */
/*  Author:     EXPath contributors                                         */
/*  Date:       2024-01-01                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2009 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package org.expath.httpclient.impl;

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MutableRequest;
import net.jcip.annotations.NotThreadSafe;
import org.expath.httpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * An implementation of an HTTP connection using the native Java 9+ HttpClient.
 * Uses Methanol for digest authentication support.
 *
 * <p>Note: HTTP/1.0 is not supported by the Java HttpClient; requests will use HTTP/1.1.
 * A warning is logged when HTTP/1.0 is requested.</p>
 */
@NotThreadSafe
public class JavaHttpConnection
        implements HttpConnection
{
    public JavaHttpConnection(URI uri)
    {
        myUri = uri;
        myMethod = null;
        myWithContent = false;
        myRequestHeaders = new ArrayList<>();
        myFollowRedirect = true;
        myTimeout = null;
        myGzip = false;
        myChunked = true;
        myPreemptiveAuthentication = false;
        myResponseStatus = -1;
        myResponseMessage = null;
        myResponseHeaders = null;
        myResponseStream = null;
    }

    @Override
    public void connect(final HttpRequestBody body, final HttpCredentials cred)
            throws HttpClientException
    {
        if (myMethod == null) {
            throw new HttpClientException(HttpClientError.HC001, "setRequestMethod has not been called before");
        }

        try {
            // Build the HttpClient
            final HttpClient client = buildClient(cred);

            // Build the request body publisher
            final HttpRequest.BodyPublisher bodyPublisher = buildBodyPublisher(body);

            // Build the request
            final MutableRequest request = MutableRequest.create(myUri)
                    .method(myMethod, bodyPublisher);

            // Add headers
            for (final String[] header : myRequestHeaders) {
                request.header(header[0], header[1]);
            }

            // Add Authorization header for preemptive basic auth (digest is handled by Methanol interceptor)
            if (myPreemptiveAuthentication && cred != null && "BASIC".equalsIgnoreCase(cred.getMethod())) {
                final String credentials = cred.getUser() + ":" + cred.getPwd();
                final String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.ISO_8859_1));
                request.header("Authorization", "Basic " + encoded);
            }

            // Set timeout per-request if configured
            if (myTimeout != null) {
                request.timeout(Duration.ofSeconds(myTimeout));
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("METHOD: " + myMethod);
                LOG.debug("URI: " + myUri);
                for (final String[] h : myRequestHeaders) {
                    LOG.debug("REQ HEADER: " + h[0] + ": " + h[1]);
                }
            }

            // Execute the request
            final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            myResponseStatus = response.statusCode();
            myResponseMessage = getReasonPhrase(response.statusCode());
            myResponseHeaders = response.headers();
            myResponseStream = response.body();

            if (LOG.isDebugEnabled()) {
                LOG.debug("RESP STATUS: " + myResponseStatus);
                response.headers().map().forEach((name, values) ->
                        values.forEach(v -> LOG.debug("RESP HEADER: " + name + ": " + v)));
            }
        }
        catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new HttpClientException(HttpClientError.HC001, "HTTP request interrupted", ex);
        }
        catch (final IOException ex) {
            throw new HttpClientException(HttpClientError.HC001, "Error executing the HTTP method: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void disconnect() throws HttpClientException
    {
        if (myResponseStream != null) {
            try {
                myResponseStream.close();
            }
            catch (final IOException ex) {
                throw new HttpClientException(HttpClientError.HC001, "Error closing response stream: " + ex.getMessage(), ex);
            }
            finally {
                myResponseStream = null;
            }
        }
    }

    @Override
    public void setHttpVersion(final String ver) throws HttpClientException
    {
        if (HttpConstants.HTTP_1_0.equals(ver)) {
            LOG.warn("HTTP/1.0 is not supported by the Java HttpClient; using HTTP/1.1 instead.");
        }
        else if (!HttpConstants.HTTP_1_1.equals(ver)) {
            throw new HttpClientException(HttpClientError.HC005, "Internal error, unknown HTTP version: '" + ver + "'");
        }
        // HTTP/1.1 is the default; nothing to configure
    }

    @Override
    public void setRequestHeaders(final HeaderSet headers) throws HttpClientException
    {
        for (final org.apache.hc.core5.http.Header h : headers) {
            myRequestHeaders.add(new String[]{ h.getName(), h.getValue() });
        }
    }

    @Override
    public void setRequestMethod(final String method, final boolean with_content) throws HttpClientException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Request method: " + method + " (" + with_content + ")");
        }
        if (!checkMethodName(method)) {
            throw new HttpClientException(HttpClientError.HC005, "Invalid HTTP method name [" + method + "]");
        }
        myMethod = method.toUpperCase();
        myWithContent = with_content;
    }

    @Override
    public void setFollowRedirect(final boolean follow)
    {
        myFollowRedirect = follow;
    }

    @Override
    public void setTimeout(final int seconds)
    {
        myTimeout = seconds;
    }

    @Override
    public void setGzip(final boolean gzip)
    {
        myGzip = gzip;
    }

    @Override
    public void setChunked(final boolean chunked)
    {
        myChunked = chunked;
    }

    @Override
    public void setPreemptiveAuthentication(final boolean preemptiveAuthentication)
    {
        myPreemptiveAuthentication = preemptiveAuthentication;
    }

    @Override
    public int getResponseStatus() throws HttpClientException
    {
        return myResponseStatus;
    }

    @Override
    public String getResponseMessage() throws HttpClientException
    {
        return myResponseMessage;
    }

    @Override
    public HeaderSet getResponseHeaders() throws HttpClientException
    {
        final HeaderSet result = new HeaderSet();
        if (myResponseHeaders != null) {
            myResponseHeaders.map().forEach((name, values) ->
                    values.forEach(value -> result.add(name, value)));
        }
        return result;
    }

    @Override
    public InputStream getResponseStream() throws HttpClientException
    {
        return myResponseStream;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Build the Java HttpClient (via Methanol for digest auth support).
     */
    private HttpClient buildClient(final HttpCredentials cred) throws HttpClientException
    {
        final Methanol.Builder builder = Methanol.newBuilder();

        // Proxy: use JVM defaults
        builder.proxy(ProxySelector.getDefault());

        // Redirect policy
        builder.followRedirects(myFollowRedirect
                ? HttpClient.Redirect.NORMAL
                : HttpClient.Redirect.NEVER);

        // Connect timeout
        if (myTimeout != null) {
            builder.connectTimeout(Duration.ofSeconds(myTimeout));
        }

        // Cookie manager (shared, session-scoped)
        builder.cookieHandler(COOKIE_MANAGER);

        // SSL: use system default (SNI is natively supported in Java 9+)
        try {
            builder.sslContext(SSLContext.getDefault());
        }
        catch (final Exception ex) {
            throw new HttpClientException(HttpClientError.HC001, "Error obtaining default SSL context: " + ex.getMessage(), ex);
        }

        // Credentials / authentication
        if (cred != null) {
            if ("DIGEST".equalsIgnoreCase(cred.getMethod())) {
                // Methanol provides digest authentication via its authenticator
                builder.authenticator(new java.net.Authenticator() {
                    @Override
                    protected java.net.PasswordAuthentication getPasswordAuthentication() {
                        return new java.net.PasswordAuthentication(
                                cred.getUser(), cred.getPwd().toCharArray());
                    }
                });
            }
            else {
                // BASIC: handled via preemptive header or standard challenge-response
                builder.authenticator(new java.net.Authenticator() {
                    @Override
                    protected java.net.PasswordAuthentication getPasswordAuthentication() {
                        return new java.net.PasswordAuthentication(
                                cred.getUser(), cred.getPwd().toCharArray());
                    }
                });
            }
        }

        return builder.build();
    }

    /**
     * Build the request body publisher from the {@link HttpRequestBody}.
     */
    private HttpRequest.BodyPublisher buildBodyPublisher(final HttpRequestBody body)
            throws HttpClientException
    {
        if (body == null || !myWithContent) {
            return HttpRequest.BodyPublishers.noBody();
        }

        try {
            if (myChunked && !myGzip) {
                // Stream directly using chunked transfer encoding
                final PipedOutputStream pipedOut = new PipedOutputStream();
                final PipedInputStream pipedIn = new PipedInputStream(pipedOut, PIPE_BUFFER_SIZE);

                final Thread writerThread = new Thread(() -> {
                    try (final OutputStream out = pipedOut) {
                        body.serialize(out);
                    }
                    catch (final Exception ex) {
                        LOG.error("Error serializing request body", ex);
                    }
                }, "JavaHttpConnection-body-writer");
                writerThread.setDaemon(true);
                writerThread.start();

                return HttpRequest.BodyPublishers.ofInputStream(() -> pipedIn);
            }
            else {
                // Buffer into memory (needed for gzip, or when not chunked)
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                if (myGzip) {
                    try (final GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
                        body.serialize(gzip);
                    }
                    // Add Content-Encoding header
                    myRequestHeaders.add(new String[]{ "Content-Encoding", "gzip" });
                }
                else {
                    body.serialize(buffer);
                }
                final byte[] bytes = buffer.toByteArray();
                return HttpRequest.BodyPublishers.ofByteArray(bytes);
            }
        }
        catch (final IOException ex) {
            throw new HttpClientException(HttpClientError.HC001, "Error building request body: " + ex.getMessage(), ex);
        }
    }

    /**
     * Returns the standard HTTP reason phrase for a given status code.
     * Java's HttpClient does not expose the reason phrase, so we derive it from the status code.
     */
    private static String getReasonPhrase(final int statusCode)
    {
        switch (statusCode) {
            case 100: return "Continue";
            case 101: return "Switching Protocols";
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 203: return "Non-Authoritative Information";
            case 204: return "No Content";
            case 205: return "Reset Content";
            case 206: return "Partial Content";
            case 300: return "Multiple Choices";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 303: return "See Other";
            case 304: return "Not Modified";
            case 305: return "Use Proxy";
            case 307: return "Temporary Redirect";
            case 308: return "Permanent Redirect";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 407: return "Proxy Authentication Required";
            case 408: return "Request Timeout";
            case 409: return "Conflict";
            case 410: return "Gone";
            case 411: return "Length Required";
            case 412: return "Precondition Failed";
            case 413: return "Content Too Large";
            case 414: return "URI Too Long";
            case 415: return "Unsupported Media Type";
            case 416: return "Range Not Satisfiable";
            case 417: return "Expectation Failed";
            case 426: return "Upgrade Required";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "HTTP Version Not Supported";
            default:  return "";
        }
    }

    /**
     * Check the method name matches HTTP/1.1 token production rules.
     */
    private boolean checkMethodName(final String method)
    {
        if (method == null || method.isEmpty()) {
            return false;
        }
        for (final char c : method.toCharArray()) {
            if (c > 127 || !METHOD_CHARS[c]) {
                return false;
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Static initializers
    // -----------------------------------------------------------------------

    private static final boolean[] METHOD_CHARS = new boolean[128];
    static {
        final String excl = "()<>@,;:\\\"/[]?={}";
        for (char c = 0; c < 128; ++c) {
            if (c < 33 || c == 127) {
                METHOD_CHARS[c] = false;
            }
            else if (excl.indexOf(c) == -1) {
                METHOD_CHARS[c] = true;
            }
            else {
                METHOD_CHARS[c] = false;
            }
        }
    }

    /** Shared cookie manager (session-scoped, in-memory). */
    private static final CookieManager COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    /** Buffer size for piped streams used in chunked streaming. */
    private static final int PIPE_BUFFER_SIZE = 64 * 1024;

    private static final Logger LOG = LoggerFactory.getLogger(JavaHttpConnection.class);

    // -----------------------------------------------------------------------
    // Instance fields
    // -----------------------------------------------------------------------

    /** The target URI. */
    private final URI myUri;
    /** The HTTP method name (uppercased). */
    private String myMethod;
    /** Whether the method carries a request body. */
    private boolean myWithContent;
    /** Accumulated request headers as [name, value] pairs. */
    private final List<String[]> myRequestHeaders;
    /** Follow HTTP redirects? */
    private boolean myFollowRedirect;
    /** Connect/request timeout in seconds, or null for default. */
    private Integer myTimeout;
    /** Whether to gzip-compress the request body. */
    private boolean myGzip;
    /** Whether to use chunked transfer encoding. */
    private boolean myChunked;
    /** Whether to send credentials preemptively (without waiting for 401). */
    private boolean myPreemptiveAuthentication;

    /** Response status code. */
    private int myResponseStatus;
    /** Response reason phrase (not available in Java HttpClient; always empty). */
    private String myResponseMessage;
    /** Response headers. */
    private HttpHeaders myResponseHeaders;
    /** Response body stream. */
    private InputStream myResponseStream;
}


/* ------------------------------------------------------------------------ */
/*  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS COMMENT.               */
/*                                                                          */
/*  The contents of this file are subject to the Mozilla Public License     */
/*  Version 1.0 (the "License"); you may not use this file except in        */
/*  compliance with the License. You may obtain a copy of the License at    */
/*  http://www.mozilla.org/MPL/.                                            */
/*                                                                          */
/*  Software distributed under the License is distributed on an "AS IS"     */
/*  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See    */
/*  the License for the specific language governing rights and limitations  */
/*  under the License.                                                      */
/*                                                                          */
/*  The Original Code is: all this file.                                    */
/*                                                                          */
/*  The Initial Developer of the Original Code is Florent Georges.          */
/*                                                                          */
/*  Contributor(s): none.                                                   */
/* ------------------------------------------------------------------------ */
