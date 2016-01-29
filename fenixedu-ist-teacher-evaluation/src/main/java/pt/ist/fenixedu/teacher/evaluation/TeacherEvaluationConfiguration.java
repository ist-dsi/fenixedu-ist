/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Teacher Evaluation.
 *
 * FenixEdu IST Teacher Evaluation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Teacher Evaluation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Teacher Evaluation.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.teacher.evaluation;

import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;
import org.fenixedu.commons.configuration.ConfigurationManager;
import org.fenixedu.commons.configuration.ConfigurationProperty;

public class TeacherEvaluationConfiguration {

    @ConfigurationManager(description = "Teacher Credits Configuration")
    public interface ConfigurationProperties {

        @ConfigurationProperty(key = "lastSemesterForCredits")
        public String getLastSemesterForCredits();

        @ConfigurationProperty(key = "lastYearForCredits", defaultValue = "2010/2011")
        public String getLastYearForCredits();

        @ConfigurationProperty(key = "startSemesterForCredits", defaultValue = "2")
        public String getStartSemesterForCredits();

        @ConfigurationProperty(key = "startYearForCredits", defaultValue = "2002/2003")
        public String getStartYearForCredits();

    }

    public static ConfigurationProperties getConfiguration() {
        return ConfigurationInvocationHandler.getConfiguration(ConfigurationProperties.class);
    }

}
