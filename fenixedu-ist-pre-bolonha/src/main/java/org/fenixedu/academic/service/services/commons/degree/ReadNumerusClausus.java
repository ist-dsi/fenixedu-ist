/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Pre Bolonha.
 *
 * FenixEdu IST Pre Bolonha is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Pre Bolonha is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Pre Bolonha.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.academic.service.services.commons.degree;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.service.services.exceptions.NonExistingServiceException;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

/**
 * @author Nuno Nunes (nmsn@rnl.ist.utl.pt) Joana Mota (jccm@rnl.ist.utl.pt)
 * 
 *         modified by:
 * @author <a href="mailto:amam@mega.ist.utl.pt">Amin Amirali</a>
 * @author <a href="mailto:frnp@mega.ist.utl.pt">Francisco Paulo</a>
 */
public class ReadNumerusClausus {

    @Atomic
    public static Integer run(String degreeCurricularPlanID) throws NonExistingServiceException {

        DegreeCurricularPlan degreeCurricularPlan = null;

        degreeCurricularPlan = FenixFramework.getDomainObject(degreeCurricularPlanID);

        if (degreeCurricularPlan == null) {
            throw new NonExistingServiceException();
        }

        return degreeCurricularPlan.getNumerusClausus();
    }

}