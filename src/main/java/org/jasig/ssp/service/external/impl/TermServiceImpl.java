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
package org.jasig.ssp.service.external.impl;

import org.jasig.ssp.dao.external.TermDao;
import org.jasig.ssp.model.external.Term;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.external.TermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;

/**
 * Term service implementation
 * 
 * @author jon.adams
 */
@Service
@Transactional
public class TermServiceImpl extends AbstractExternalReferenceDataService<Term> implements TermService {

	@Autowired
	transient private TermDao dao;

	@Override
	protected TermDao getDao() {
		return dao;
	}

	protected void setDao(final TermDao dao) {
		this.dao = dao;
	}

	@Override
	public Term getCurrentTerm() throws ObjectNotFoundException {
		return getDao().getCurrentTerm();
	}

	@Override
	public Term getNextTerm() throws ObjectNotFoundException {
		return getDao().getNextTerm(getCurrentTerm().getEndDate());
	}

	@Override
	public List<Term> getCurrentAndFutureTerms() throws ObjectNotFoundException {
		return getDao().getCurrentAndFutureTerms();
	}

	@Override
	public List<Term> facetSearch(String tag, String programCode) {
		return dao.facetSearch(tag,programCode);
	}
	 
	@Override
	public List<Term> getTermsWithRegistrationWindowOpenIfAny() {
		return dao.getTermsWithRegistrationWindowOpenIfAny();
	}
	
	@Override
	public List<Term> getTermsByCodes(List<String> codes){
		return dao.getTermsByCodes(codes);
	}

	@Override
	public List<Term> getTermsByDateRange(final Date dateFrom, final Date dateTo) {
		return dao.getTermsByEndDateRange(dateFrom, dateTo);
	}

	@Override
	public List<String> getTermCodesByDateRange(final Date startDate, final Date endDate) {
		return dao.getTermCodesByStartDateEndDateRange(startDate, endDate);
	}
}