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
package org.jasig.ssp.factory;

import java.util.Collection;
import java.util.Set;

import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.PersonSpecialServiceGroup;
import org.jasig.ssp.model.reference.SpecialServiceGroup;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.transferobject.PersonSpecialServiceGroupTO;
import org.jasig.ssp.transferobject.reference.ReferenceLiteTO;

public interface PersonSpecialServiceGroupTOFactory extends
		TOFactory<PersonSpecialServiceGroupTO, PersonSpecialServiceGroup> {

	PersonSpecialServiceGroup fromLite(
			final ReferenceLiteTO<SpecialServiceGroup> lite,
			final Person person) throws ObjectNotFoundException;

	Set<PersonSpecialServiceGroup> fromLites(
			final Collection<ReferenceLiteTO<SpecialServiceGroup>> lites,
			final Person person) throws ObjectNotFoundException;

	void updateSetFromLites(
			final Set<PersonSpecialServiceGroup> updateSet,
			final Collection<ReferenceLiteTO<SpecialServiceGroup>> lites,
			final Person person) throws ObjectNotFoundException;
}