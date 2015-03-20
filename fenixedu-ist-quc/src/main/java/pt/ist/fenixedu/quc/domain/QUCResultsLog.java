/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.quc.domain;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;

public class QUCResultsLog extends QUCResultsLog_Base {

    public QUCResultsLog() {
        super();
    }

    public QUCResultsLog(Degree degree, ExecutionYear executionYear, String description) {
        super();
        if (getDegree() == null) {
            setDegree(degree);
        }
        if (getExecutionYear() == null) {
            setExecutionYear(executionYear);
        }
        setDescription(description);
    }

    public static QUCResultsLog createQUCResultsLog(Degree degree, ExecutionYear executionYear, String description) {
        return new QUCResultsLog(degree, executionYear, description);
    }

    public static QUCResultsLog createLog(Degree degree, ExecutionYear executionYear, String bundle, String key, String... args) {
        final String label = generateLabelDescription(bundle, key, args);
        return createQUCResultsLog(degree, executionYear, label);
    }

    @Override
    public DegreeLogTypes getDegreeLogType() {
        return DegreeLogTypes.QUC_RESULTS;
    }

}