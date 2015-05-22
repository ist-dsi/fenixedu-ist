/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.domain.serviceRequests;

import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.IProgramConclusionRequest;
import org.fenixedu.academic.domain.serviceRequests.RegistrationAcademicServiceRequest;
import org.joda.time.LocalDate;

public class IstRegistryCodeGenerator extends IstRegistryCodeGenerator_Base {
    public IstRegistryCodeGenerator() {
        super();
    }

    protected Integer getNextNumber(CycleType cycle) {
        switch (cycle) {
        case FIRST_CYCLE:
            super.setCurrentFirstCycle((super.getCurrentFirstCycle() != null ? super.getCurrentFirstCycle() : 0) + 1);
            return super.getCurrentFirstCycle();
        case SECOND_CYCLE:
            super.setCurrentSecondCycle((super.getCurrentSecondCycle() != null ? super.getCurrentSecondCycle() : 0) + 1);
            return super.getCurrentSecondCycle();
        case THIRD_CYCLE:
            super.setCurrentThirdCycle((super.getCurrentThirdCycle() != null ? super.getCurrentThirdCycle() : 0) + 1);
            return super.getCurrentThirdCycle();
        default:
            throw new DomainException("error.InstitutionRegistryCodeGenerator.unsupportedCycle");
        }
    }

    protected CycleType getCycle(AcademicServiceRequest request) {
        if (request.isRequestForPhd()) {
            return CycleType.THIRD_CYCLE;
        } else if (request.isRequestForRegistration()) {
            RegistrationAcademicServiceRequest registrationRequest = (RegistrationAcademicServiceRequest) request;
            if (registrationRequest.getDegreeType().isPreBolonhaDegree()) {
                return CycleType.FIRST_CYCLE;
            } else if (registrationRequest.getDegreeType().isPreBolonhaMasterDegree()) {
                return CycleType.SECOND_CYCLE;
            } else {
                throw new DomainException("error.registryCode.unableToGuessCycleTypeToGenerateCode");
            }
        }

        throw new DomainException("error.registryCode.request.neither.is.phd.nor.registration.request");
    }

    @Override
    public String getCode(AcademicServiceRequest request) {
        if (request instanceof IProgramConclusionRequest) {

            String type = null;
            CycleType cycle = ((IProgramConclusionRequest) request).getRequestedCycle();
            if (cycle == null) {
                cycle = getCycle(request);
            }

            switch (cycle) {
            case FIRST_CYCLE:
                type = "L";
                break;
            case SECOND_CYCLE:
                type = "M";
                break;
            case THIRD_CYCLE:
                type = "D";
                break;
            default:
                type = "";
            }
            return getNextNumber(cycle) + "/ISTC" + type + "/" + new LocalDate().toString("yy");
        }
        return super.getCode(request);
    }
}
