/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Tutorship.
 *
 * FenixEdu IST Tutorship is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Tutorship is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Tutorship.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.tutorship.domain;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;

public class ProgramTutoredParticipationLog extends ProgramTutoredParticipationLog_Base {

    public ProgramTutoredParticipationLog() {
        super();
    }

    public ProgramTutoredParticipationLog(Degree degree, ExecutionYear executionYear, String description) {
        super();
        if (getDegree() == null) {
            setDegree(degree);
        }
        if (getExecutionYear() == null) {
            setExecutionYear(executionYear);
        }
        setDescription(description);
    }

    public static ProgramTutoredParticipationLog createProgramTutoredParticipationLog(Degree degree, ExecutionYear executionYear,
            String description) {
        return new ProgramTutoredParticipationLog(degree, executionYear, description);
    }

    public static ProgramTutoredParticipationLog createLog(Degree degree, ExecutionYear executionYear, String bundle, String key,
            String... args) {
        final String label = generateLabelDescription(bundle, key, args);
        return createProgramTutoredParticipationLog(degree, executionYear, label);
    }

    @Override
    public DegreeLogTypes getDegreeLogType() {
        return DegreeLogTypes.PROGRAM_TUTORED_PARTICIPATION;
    }

}
