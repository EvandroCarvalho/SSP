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
package org.jasig.ssp.dao.external;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.jasig.ssp.model.external.ExternalStudentFinancialAidAwardTerm;
import org.jasig.ssp.model.external.ExternalStudentTranscriptTerm;
import org.jasig.ssp.transferobject.reports.EntityStudentCountByCoachTO;
import org.jasig.ssp.util.hibernate.NamespacedAliasToBeanResultTransformer;
import org.springframework.stereotype.Repository;

@Repository
public class ExternalStudentFinancialAidAwardTermDao extends
		AbstractExternalDataDao<ExternalStudentFinancialAidAwardTerm> {

	protected ExternalStudentFinancialAidAwardTermDao() {
		super(ExternalStudentFinancialAidAwardTerm.class);
	}
	
	@SuppressWarnings("unchecked")
	public List<ExternalStudentFinancialAidAwardTerm> gettStudentFinancialAidAwardsBySchoolIdTermCode(String schoolId, String termCode){
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("schoolId", schoolId));
		criteria.add(Restrictions.eq("termCode", termCode));
		return (List<ExternalStudentFinancialAidAwardTerm>)criteria.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<ExternalStudentFinancialAidAwardTerm> getStudentFinancialAidAwardsBySchoolId(String schoolId){
		Query criteria = createHqlQuery("SELECT faAward.schoolId as faAward_schoolId, " +
				"faAward.accepted as faAward_accepted, " +
				"faAward.termCode as faAward_termCode " +
				"FROM ExternalStudentFinancialAidAwardTerm as faAward, Term as faAwardTerm " +
				"WHERE faAward.schoolId = :schoolId AND faAward.termCode = faAwardTerm.code " +
				"ORDER BY faAwardTerm.startDate DESC");
		criteria.setParameter("schoolId", schoolId);
		criteria.setResultTransformer(
				new NamespacedAliasToBeanResultTransformer(
						ExternalStudentFinancialAidAwardTerm.class, "faAward_"));
		return (List<ExternalStudentFinancialAidAwardTerm>)criteria.list();

	}
	
}
