/****************************************************************************/
/*  File:       LoggerHelper.java                                           */
/*  Author:     F. Georges - fgeorges.org                                   */
/*  Date:       2009-08-04                                                  */
/*  Tags:                                                                   */
/*      Copyright (c) 2009 Florent Georges (see end of file.)               */
/* ------------------------------------------------------------------------ */


package org.expath.httpclient.impl;

import org.slf4j.Logger;
import org.expath.httpclient.HttpHeader;
import java.net.HttpCookie;

/**
 * Helper to log HTTP-specific stuff.
 *
 * @author Florent Georges
 */
public class LoggerHelper
{
    public static void logCookies(Logger log, String prompt, Iterable<HttpCookie> cookies)
    {
        if ( log.isDebugEnabled() ) {
            if ( cookies == null ) {
                log.debug(prompt + ": null");
                return;
            }
            for ( HttpCookie c : cookies ) {
                log.debug(prompt + ": " + c.getName() + ": " + c.getValue());
            }
        }
    }

    public static void logHeaders(Logger log, String prompt, HttpHeader[] headers)
    {
        if ( log.isDebugEnabled() ) {
            if ( headers == null ) {
                log.debug(prompt + ": null");
                return;
            }
            for ( HttpHeader h : headers ) {
                log.debug(prompt + ": " + h.getName() + ": " + h.getValue());
            }
        }
    }

    public static void logHeaderDetails(Logger log, String prompt, Iterable<HttpHeader> headers)
    {
        if ( log.isDebugEnabled() ) {
            if ( headers == null ) {
                log.debug(prompt + ": null");
                return;
            }
            for ( HttpHeader h : headers ) {
                log.debug(prompt + " - HEADER: " + h.getName() + ": " + h.getValue());
            }
        }
    }
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
