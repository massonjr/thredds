/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.httpservices;

import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;

import java.io.*;
import java.net.*;
import java.util.*;

import static org.apache.http.auth.AuthScope.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

abstract public class HTTPUtil
{

    //////////////////////////////////////////////////
    // Interceptors

    static abstract public class InterceptCommon
    {
        protected HttpContext context = null;
        protected List<Header> headers = new ArrayList<Header>();
        protected HttpRequest request = null;
        protected HttpResponse response = null;
        protected boolean printheaders = false;

        public InterceptCommon setPrint(boolean tf)
        {
            this.printheaders = tf;
            return this;
        }

        public void
        clear()
        {
            context = null;
            headers.clear();
            request = null;
            response = null;
        }

        synchronized public HttpResponse getRequest()
        {
            return this.response;
        }

        synchronized public HttpResponse getResponse()
        {
            return this.response;
        }

        synchronized public HttpContext getContext()
        {
            return this.context;
        }

        synchronized public List<Header> getHeaders(String key)
        {
            List<Header> keyh = new ArrayList<Header>();
            for(Header h : this.headers) {
                if(h.getName().equalsIgnoreCase(key.trim()))
                    keyh.add(h);
            }
            return keyh;
        }

        synchronized public List<Header> getHeaders()
        {
            return this.headers;
        }

        public void
        printHeaders()
        {
            if(this.request != null) {
                Header[] hdrs = this.request.getAllHeaders();
                if(hdrs == null) hdrs = new Header[0];
                System.err.println("Request Headers:");
                for(Header h : hdrs)
                    System.err.println(h.toString());
            }
            if(this.response != null) {
                Header[] hdrs = this.response.getAllHeaders();
                if(hdrs == null) hdrs = new Header[0];
                System.err.println("Response Headers:");
                for(Header h : hdrs)
                    System.err.println(h.toString());
            }
            System.err.flush();
        }
    }

    static public class InterceptResponse extends InterceptCommon
        implements HttpResponseInterceptor
    {
        synchronized public void
        process(HttpResponse response, HttpContext context)
            throws HttpException, IOException
        {
            this.response = response;
            this.context = context;
            if(this.printheaders)
                printHeaders();
            else if(this.response != null) {
                Header[] hdrs = this.response.getAllHeaders();
                for(int i = 0;i < hdrs.length;i++)
                    headers.add(hdrs[i]);
            }
        }
    }

    static public class InterceptRequest extends InterceptCommon
        implements HttpRequestInterceptor
    {
        synchronized public void
        process(HttpRequest request, HttpContext context)
            throws HttpException, IOException
        {
            this.request = request;
            this.context = context;
            if(this.printheaders)
                printHeaders();
            else if(this.request != null) {
                Header[] hdrs = this.request.getAllHeaders();
                for(int i = 0;i < hdrs.length;i++)
                    headers.add(hdrs[i]);
            }
        }
    }

    /**
     * Temporary hack to remove Content-Encoding: XXX-Endian headers
     */
    static public class ContentEncodingInterceptor extends InterceptCommon
        implements HttpResponseInterceptor
    {
        synchronized public void
        process(HttpResponse response, HttpContext context)
            throws HttpException, IOException
        {
            if(response == null) return;
            Header[] hdrs = response.getAllHeaders();
            if(hdrs == null) return;
            boolean modified = false;
            for(int i=0;i < hdrs.length;i++) {
                Header h = hdrs[i];
                if(!h.getName().equalsIgnoreCase("content-encoding")) continue;
                String value = h.getValue();
                if(value.trim().toLowerCase().endsWith("-endian")) {
                    hdrs[i] = new BasicHeader("X-Content-Encoding",value);
                    modified = true;
                }
            }
            if(modified)
                response.setHeaders(hdrs);
            // Similarly, suppress encoding for Entity
            HttpEntity entity= response.getEntity();
            Header ceheader = entity.getContentEncoding();
            if (ceheader != null) {
                String value = ceheader.getValue();
                if(value.trim().toLowerCase().endsWith("-endian")) {
                    int x = 0;//entity.setContentEncoding(new BasicHeader("Content-Encoding","Identity"));
                }
            }
        }
    }


    //////////////////////////////////////////////////
    // Misc.

    static public byte[]
    readbinaryfile(InputStream stream)
        throws IOException
    {
        // Extract the stream into a bytebuffer
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] tmp = new byte[1 << 16];
        for(;;) {
            int cnt;
            cnt = stream.read(tmp);
            if(cnt <= 0) break;
            bytes.write(tmp, 0, cnt);
        }
        return bytes.toByteArray();
    }

    static public String getCanonicalURL(String legalurl)
    {
        if(legalurl == null) return null;
        int index = legalurl.indexOf('?');
        if(index >= 0) legalurl = legalurl.substring(0, index);
        // remove any trailing extensuion
        //index = legalurl.lastIndexOf('.');
        //if(index >= 0) legalurl = legalurl.substring(0,index);
        return canonicalpath(legalurl);
    }

    /**
     * Convert path to use '/' consistently and
     * to remove any trailing '/'
     *
     * @param path convert this path
     * @return canonicalized version
     */
    static public String canonicalpath(String path)
    {
        if(path == null) return null;
        path = path.replace('\\', '/');
        if(path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        return path;
    }

    static public URL
    removeprincipal(URL u)
    {
        // Must be a simpler way
        try {
            return new URI(u.getProtocol(), null, u.getHost(), u.getPort(),
                u.getPath(), u.getQuery(), u.getRef()).toURL();
        } catch (URISyntaxException | MalformedURLException ex) {
            HTTPSession.log.error("Malformed url: "+u.toString());
            return null;
        }
    }

    static public String makerealm(URL url)
    {
        return makerealm(url.getHost(), url.getPort());
    }

    static public String makerealm(AuthScope scope)
    {
        return makerealm(scope.getHost(), scope.getPort());
    }

    static public String makerealm(String host, int port)
    {
        if(host == null) host = ANY_HOST;
        if(host == ANY_HOST)
            return ANY_REALM;
        String sport = (port <= 0 || port == ANY_PORT) ? "" : String.format("%d", port);
        return host + ":" + sport;
    }

    /**
     * Convert a uri string to an instance of java.net.URI.
     * The critical thing is that this procedure can handle backslash
     * escaped uris as well as %xx escaped uris.
     *
     * @param u  the uri to convert
     * @return The URI corresponding to u.
     * @throws URISyntaxException
     */
    static public URI
    parseToURI(final String u)
            throws URISyntaxException
    {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while(i < u.length()) {
            char c = u.charAt(i++);
            if(c == '\\') {
                if(i + 1 == u.length())
                    throw new URISyntaxException(u, "Trailing '\' at end of url");
                buf.append("%5c");
                c = u.charAt(i++);
                buf.append(String.format("%%%02x", (int) c));
            } else
                buf.append(c);
        }
        return new URI(buf.toString());
    }


}
