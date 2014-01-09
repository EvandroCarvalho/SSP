/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.ssp.web.api.reference;

import org.jasig.ssp.factory.TOFactory;
import org.jasig.ssp.factory.reference.SapStatusTOFactory;
import org.jasig.ssp.model.reference.SapStatus;
import org.jasig.ssp.service.AuditableCrudService;
import org.jasig.ssp.service.reference.SapStatusService;
import org.jasig.ssp.transferobject.reference.SapStatusTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/1/reference/sapstatus")
public class SapStatusController
		extends
		AbstractAuditableReferenceController<SapStatus, SapStatusTO> {

	@Autowired
	protected transient SapStatusService service;

	@Override
	protected AuditableCrudService<SapStatus> getService() {
		return service;
	}

	@Autowired
	protected transient SapStatusTOFactory factory;

	@Override
	protected TOFactory<SapStatusTO, SapStatus> getFactory() {
		return factory;
	}

	protected SapStatusController() {
		super(SapStatus.class, SapStatusTO.class);
	}

	private static final Logger LOGGER = LoggerFactory
			.getLogger(EthnicityController.class);

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}
	
	
}
