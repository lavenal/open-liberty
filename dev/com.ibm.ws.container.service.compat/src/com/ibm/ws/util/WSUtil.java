/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class WSUtil {

    @SuppressWarnings("unchecked")
    public static String resolveURI(String uriToResolve) { // rewrote method to improve perf (251221)

        //Not much to resolve in this case
        if (uriToResolve == null) {
            return null;
        }

        int qindex = Integer.MAX_VALUE;
        String qstring = null;
        boolean parsedQuery = false;

        // begin 154268: part 1
        // just to be safe.. convert "\" to "/"
        if (uriToResolve.indexOf('\\') != -1) {
            parsedQuery = true;
            int tmpindex = -1;
            if ((tmpindex = uriToResolve.indexOf('?')) != -1) { // need to remove query string first to avoid altering query params.
                qindex = tmpindex;
                qstring = uriToResolve.substring(qindex);
                uriToResolve = uriToResolve.substring(0, qindex);
                if (uriToResolve == null || uriToResolve.equals("")) { // no uri to resolve; return queryString untouched.
                    return qstring;
                }
            }
            uriToResolve = uriToResolve.replace('\\', '/');
        }
        // end 154268

        // URI contains no metacharacters
        int tmpIndex = -1;
        int testIndex = -1;
        int resolveIndex = Integer.MAX_VALUE; // hold onto index of lowest match --
        // later used to compare against
        // queryString index.
        // 258806: Reduction of one indexOf call in standard uri case
        if ((testIndex = uriToResolve.indexOf("/.")) != -1) {
            if ((tmpIndex = uriToResolve.indexOf("/../", testIndex)) != -1) {
                resolveIndex = tmpIndex;
            }
            if ((tmpIndex = uriToResolve.indexOf("/./", testIndex)) != -1) {
                resolveIndex = resolveIndex < tmpIndex ? resolveIndex : tmpIndex;
            }
        }
        if ((tmpIndex = uriToResolve.indexOf("//")) != -1) {
            resolveIndex = resolveIndex < tmpIndex ? resolveIndex : tmpIndex;
        }

        if (!parsedQuery) {
            if (resolveIndex == Integer.MAX_VALUE) { // no special characters.
                return uriToResolve;
            }
            if ((qindex = uriToResolve.indexOf('?')) != -1) { // need to check for query string.
                qstring = uriToResolve.substring(qindex);
                uriToResolve = uriToResolve.substring(0, qindex);
                if (uriToResolve == null || uriToResolve.equals("")) { // no uri to resolve; return queryString untouched.
                    return qstring;
                }
                if (qindex <= resolveIndex) { // true if query string appears prior to special metacharacters
                    // needing to be resolved.
                    return uriToResolve + qstring;
                }
            }
        } else {
            if (qindex <= resolveIndex) { // true if query string appears prior to special metacharacters needing
                // to be resolved.
                if (qstring == null) {
                    return uriToResolve;
                } else {
                    return uriToResolve + qstring;
                }
            }
        }

        StringTokenizer uriParser = new StringTokenizer(uriToResolve, "/", false);
        String currentElement = null;
        ArrayList uriElements = new ArrayList();

        while (uriParser.hasMoreTokens() == true) {
            currentElement = uriParser.nextToken();

            if ((currentElement == null) || (currentElement.length() < 1)) {
                continue;
            }

            // Attempt to remove the last saved URI element from the list
            if (currentElement.equals("..") == true) {
                if (uriElements.size() < 1) {
                    // URI is outside the current context
                    throw new java.lang.IllegalArgumentException("The specified URI, " + uriToResolve
                                                                 + ", is invalid because it contains more references to parent directories (\"..\") than is possible.");
                }

                uriElements.remove(uriElements.size() - 1);

                continue;
            }

            // Ignore, don't add to list of elements
            if (currentElement.equals(".") == true) {
                continue;
            }

            // Keep track of this URI element
            uriElements.add(currentElement);
        }

        // Build a String from the remaining elements of the passed URI

        StringBuffer resolvedURI = new StringBuffer();
        int elementCount = uriElements.size();

        for (int elementIndex = 0; elementIndex < elementCount; ++elementIndex) {
            resolvedURI.append("/" + (String) uriElements.get(elementIndex));
        }

        if (qstring == null) {
            return resolvedURI.toString();
        } else {
            return resolvedURI.toString() + qstring;
        }

    }

    public static void main(String args[]) {

        String question = "/hello\\\\there\\////dog?queryString=5";
        System.out.println(question + ": resolvedURI = [" + WSUtil.resolveURI(question) + "]");
        String question2 = "/hello/../dog/";
        System.out.println(question2 + ": resolvedURI = [" + WSUtil.resolveURI(question2) + "]");
        String question3 = "/hello/./dog/";
        System.out.println(question3 + ": resolvedURI = [" + WSUtil.resolveURI(question3) + "]");
        String question4 = "/hello//dog/";
        System.out.println(question4 + ": resolvedURI = [" + WSUtil.resolveURI(question4) + "]");
        String question5 = "/hello//dog/?hello=/../5";
        System.out.println(question5 + ": resolvedURI = [" + WSUtil.resolveURI(question5) + "]");
        String question6 = "/hello/dog/?hello=/../5";
        System.out.println(question6 + ": resolvedURI = [" + WSUtil.resolveURI(question6) + "]");
        String question7 = "?hello=/../5";
        System.out.println(question7 + ": resolvedURI = [" + WSUtil.resolveURI(question7) + "]");
        String question8 = "todd?hello=\\string";
        System.out.println(question8 + ": resolvedURI = [" + WSUtil.resolveURI(question8) + "]");
        String question9 = "?";
        System.out.println(question9 + ": resolvedURI = [" + WSUtil.resolveURI(question9) + "]");
        String question10 = "/servlet/snoop";
        System.out.println(question10 + ": resolvedURI = [" + WSUtil.resolveURI(question10) + "]");
        String question11 = "\\servlet\\snoop";
        System.out.println(question11 + ": resolvedURI = [" + WSUtil.resolveURI(question11) + "]");
        String question12 = "\\";
        System.out.println(question12 + ": resolvedURI = [" + WSUtil.resolveURI(question12) + "]");

    }

}