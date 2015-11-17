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
package pt.ist.fenixedu.quc.domain.exceptions;

import org.fenixedu.bennu.core.domain.exceptions.DomainException;

public class FenixEduQucDomainException extends DomainException {

    private static final long serialVersionUID = 4379796158079580297L;

    protected static final String BUNDLE = "resources.FenixEduQucResources";

    protected FenixEduQucDomainException(String key, String... args) {
        super(BUNDLE, key, args);
    }

    protected FenixEduQucDomainException(Throwable cause, String key, String... args) {
        super(cause, BUNDLE, key, args);
    }

    public static FenixEduQucDomainException inquiriesNotAnswered() {
        return new FenixEduQucDomainException("message.student.cannotEnroll.inquiriesNotAnswered");
    }

    public static FenixEduQucDomainException questionAnwserDuplicateCreation() {
        return new FenixEduQucDomainException("error.inquiry.questionAnswer.duplicateCreation");
    }
}
