<%--

    Licensed to Apereo under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Apereo licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License.  You may obtain a
    copy of the License at the following location:

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

--%>
<%@ page import="javax.servlet.jsp.JspException" %><%@page isErrorPage="true" contentType="application/json" %><%@ page import="org.slf4j.Logger,org.slf4j.LoggerFactory" %><%! static final Logger LOGGER = LoggerFactory.getLogger("400.jsp"); %><% System.out.println("Exception caught in view layer THIS IS THE EXCEPTION: " + pageContext); System.out.println(pageContext.getException()); LOGGER.error("Exception caught in view layer THIS IS THE EXCEPTION: " + pageContext); LOGGER.error(pageContext.getException()); %>{"success":false, "uri":"${pageContext.errorData.requestURI}", "http status code":"${pageContext.errorData.statusCode}", "exception":"${pageContext.errorData.throwable}"}