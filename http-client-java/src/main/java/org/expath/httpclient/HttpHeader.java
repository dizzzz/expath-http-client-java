/****************************************************************************/
/*  File:       HttpHeader.java                                             */
/*  Author:     EXPath contributors                                         */
/*  Date:       2024                                                        */
/*  Tags:                                                                   */
/*      Copyright (c) 2024 EXPath contributors (see end of file.)           */
/* ------------------------------------------------------------------------ */


package org.expath.httpclient;

/**
 * A simple HTTP header name/value pair, independent of any HTTP client library.
 *
 * @author EXPath contributors
 */
public class HttpHeader {

    private final String myName;
    private final String myValue;

    public HttpHeader(final String name, final String value) {
        this.myName = name;
        this.myValue = value;
    }

    public String getName() {
        return myName;
    }

    public String getValue() {
        return myValue;
    }

    @Override
    public String toString() {
        return myName + ": " + myValue;
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
/*  The Initial Developer of the Original Code is EXPath contributors.      */
/*                                                                          */
/*  Contributor(s): none.                                                   */
/* ------------------------------------------------------------------------ */
