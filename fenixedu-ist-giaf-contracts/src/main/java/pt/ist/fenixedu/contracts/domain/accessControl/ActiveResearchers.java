/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Contracts.
 *
 * FenixEdu IST GIAF Contracts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Contracts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Contracts.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.contracts.domain.accessControl;

import org.fenixedu.bennu.core.annotation.GroupOperator;

@GroupOperator("activeResearchers")
public class ActiveResearchers extends SapBackedGroup {

	private static final long serialVersionUID = -6648971466827719165L;

	private static final String[] SAP_GROUP = new String[] { " Investigadores" };

	@Override
	protected String presentationNameLable() {
		return "label.name.ActiveResearchersGroup";
	}

	@Override
	protected String[] sapGroups() {
		return SAP_GROUP;
	}

}
