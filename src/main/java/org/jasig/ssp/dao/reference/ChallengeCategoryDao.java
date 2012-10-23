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
package org.jasig.ssp.dao.reference;

import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.jasig.ssp.dao.AuditableCrudDao;
import org.jasig.ssp.model.reference.ChallengeCategory;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.springframework.stereotype.Repository;

/**
 * Data access class for the ChallengeCategory reference entity.
 */
@Repository
public class ChallengeCategoryDao extends
		AbstractReferenceAuditableCrudDao<ChallengeCategory>
		implements AuditableCrudDao<ChallengeCategory> {

	public ChallengeCategoryDao() {
		super(ChallengeCategory.class);
	}

	public PagingWrapper<ChallengeCategory> getAllForCategory(
			final UUID categoryId,
			final SortingAndPaging sAndP) {
		final Criteria query = createCriteria()
				.add(Restrictions.eq("category.id", categoryId));
		return processCriteriaWithStatusSortingAndPaging(query, sAndP);
	}

	public PagingWrapper<ChallengeCategory> getAllForChallengeAndCategory(
			final UUID challengeId, final UUID categoryId,
			final SortingAndPaging sAndP) {
		final Criteria query = createCriteria()
				.add(Restrictions.eq("category.id", categoryId))
				.add(Restrictions.eq("challenge.id", challengeId));
		return processCriteriaWithStatusSortingAndPaging(query, sAndP);
	}
}