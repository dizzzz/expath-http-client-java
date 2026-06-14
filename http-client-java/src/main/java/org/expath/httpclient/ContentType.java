/****************************************************************************/
/*  File:       ContentType.java                                            */
/*  Author:     F. Georges - fgeorges.org                                   */
/*  Date:       2009-02-22                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2009 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package org.expath.httpclient;

import javax.annotation.Nullable;

/**
 * Represent a Content-Type header.
 * <p>
 * Provide the ability to get the boundary param in case of a multipart
 * content type on the one hand, and the ability to get only the MIME type
 * string without any param on the other hand.
 *
 * @author Florent Georges
 */
public class ContentType {

    public ContentType(final String type, final String charset, final String boundary) {
        this.myType = type;
        this.myCharset = charset;
        this.myBoundary = boundary;
    }

    public static @Nullable ContentType parse(@Nullable final HttpHeader header, @Nullable final String overrideType, @Nullable final String defaultCharset) throws HttpClientException {
        final String type;
        final String charset;
        final String boundary;

        if (overrideType != null) {
            // get the internet media type from the override
            type = extractMediaTypeFromContentType(overrideType);

            // does the override contain a charset?
            if (overrideType.indexOf("charset=") > -1) {
                // get the charset from the override
                charset = overrideType.replaceFirst(".+charset=([^;\\s]+).*", "$1");
            } else {
                // get the charset from the header or the default
                if (header == null || !"Content-Type".equalsIgnoreCase(header.getName())) {
                    throw new HttpClientException(HttpClientError.HC001, "Header is not content type");
                }
                charset = getParam(header.getValue(), "charset", defaultCharset);
            }

            // does the override contain a boundary?
            if (overrideType.indexOf("boundary=") > -1) {
                boundary = overrideType.replaceFirst(".+boundary=([^;\\s]+).*", "$1");
            } else {
                // get the boundary from the header or null
                if (header == null || !"Content-Type".equalsIgnoreCase(header.getName())) {
                    throw new HttpClientException(HttpClientError.HC001, "Header is not content type");
                }
                boundary = getParam(header.getValue(), "boundary", null);
            }

        } else {
            // get the internet media type from the header
            if (header == null) {
                return null;
            }

            if (!"Content-Type".equalsIgnoreCase(header.getName())) {
                throw new HttpClientException(HttpClientError.HC001, "Header is not Content-Type");
            }

            type = extractMediaTypeFromContentType(header.getValue());
            charset = getParam(header.getValue(), "charset", defaultCharset);
            boundary = getParam(header.getValue(), "boundary", null);
        }

        return new ContentType(type, charset, boundary);
    }

    /**
     * Extract a named parameter value from a header value string such as
     * "text/html; charset=utf-8; boundary=something".
     * Returns {@code defaultValue} when the parameter is absent.
     */
    private static @Nullable String getParam(final String headerValue, final String paramName, @Nullable final String defaultValue) {
        if (headerValue == null) {
            return defaultValue;
        }
        for (final String part : headerValue.split(";")) {
            final String trimmed = part.trim();
            final String prefix = paramName + "=";
            if (trimmed.toLowerCase().startsWith(prefix.toLowerCase())) {
                String value = trimmed.substring(prefix.length()).trim();
                // strip surrounding quotes if present
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return defaultValue;
    }

    private static String extractMediaTypeFromContentType(final String contentType) {
        final int idxParamSeparator = contentType.indexOf(';');
        if (idxParamSeparator > -1) {
            return contentType.substring(0, idxParamSeparator);
        } else {
            return contentType;
        }
    }

    @Nullable
    public String getType() {
        return myType;
    }

    @Nullable
    public String getCharset() {
        return myCharset;
    }

    @Nullable
    public String getBoundary() {
        return myBoundary;
    }

    @Nullable
    public String getValue() {
        final StringBuilder builder = new StringBuilder(myType);
        if (myCharset != null) {
            builder.append("; charset=").append(myCharset);
        }
        if (myBoundary != null) {
            builder.append("; boundary=").append(myCharset);
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return getValue();
    }

    private final String myType;
    private final String myCharset;
    private final String myBoundary;
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
