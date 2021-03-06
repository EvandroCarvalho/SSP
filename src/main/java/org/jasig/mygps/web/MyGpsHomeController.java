/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.mygps.web;


import org.jasig.ssp.model.SubjectAndBody;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.web.api.AbstractBaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/1/mygps/home")
public class MyGpsHomeController extends AbstractBaseController {

    @Autowired
    private transient ConfigService configService;

    @Autowired
    private transient MessageTemplateService messageTemplateService;


    private static final Logger LOGGER = LoggerFactory
            .getLogger(MyGpsMessageController.class);

    @PreAuthorize("hasAnyRole('ROLE_MY_GPS_TOOL', 'ROLE_ANONYMOUS')")
    @RequestMapping(value = "/tools", method = RequestMethod.GET)
    public @ResponseBody
    String getMyGPSTools() throws ObjectNotFoundException {
        // Don't fall back to default b/c we want to allow admins to specify an empty list
        return configService.getByNameNull("mygps_visible_tools");
    }

    @PreAuthorize("hasAnyRole('ROLE_MY_GPS_TOOL', 'ROLE_ANONYMOUS')")
    @RequestMapping(value = "/appname", method = RequestMethod.GET)
    public @ResponseBody
    String getAppName() throws ObjectNotFoundException {

        return configService.getByNameNullOrDefaultValue("app_title");
    }

    @PreAuthorize("hasAnyRole('ROLE_MY_GPS_TOOL', 'ROLE_ANONYMOUS')")
    @RequestMapping(value = "/welcome", method = RequestMethod.GET)
    public @ResponseBody
    String getWelcomeMessage() throws ObjectNotFoundException {

        final SubjectAndBody welcomeMessage = messageTemplateService.createMyGPSWelcomeMessage();

        if ( welcomeMessage != null ) {
            return welcomeMessage.getSubject() + " \n " + welcomeMessage.getBody();
        } else {
            return null;
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}

