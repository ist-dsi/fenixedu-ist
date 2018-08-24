/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.domain.accounting.events.export;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.DfaGratuityEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.accounting.events.gratuity.StandaloneEnrolmentGratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.accounting.paymentCodes.EventPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.PaymentCodePool;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.groups.Group;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.integration.util.sibs.SibsOutgoingPaymentFile;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class SIBSOutgoingPaymentFile extends SIBSOutgoingPaymentFile_Base {
    private static final Comparator<SIBSOutgoingPaymentFile> SUCCESSFUL_SENT_DATE_TIME_COMPARATOR =
            Comparator.comparing(SIBSOutgoingPaymentFile_Base::getSuccessfulSentDate);

    public SIBSOutgoingPaymentFile(DateTime lastSuccessfulSentDateTime, Person person) {
        super();
        setExecutionYear(ExecutionYear.readCurrentExecutionYear());

        try {
            StringBuilder errorsBuilder = new StringBuilder();
            byte[] paymentFileContents = createPaymentFile(lastSuccessfulSentDateTime, person, errorsBuilder).getBytes("ASCII");
            setErrors(errorsBuilder.toString());
            init(outgoingFilename(), outgoingFilename(), paymentFileContents, Group.managers().or(AcademicAuthorizationGroup.get(AcademicOperationType.CREATE_SIBS_PAYMENTS_REPORT)));
        } catch (UnsupportedEncodingException e) {
            throw new DomainException(e.getMessage(), e);
        }
    }

    @Override
    protected List<Class> getAcceptedEventClasses() {
        return Arrays.asList(new Class[] { AdministrativeOfficeFeeAndInsuranceEvent.class, GratuityEventWithPaymentPlan.class,
                DfaGratuityEvent.class, InsuranceEvent.class, ResidenceEvent.class, StandaloneEnrolmentGratuityEvent.class });
    }

    private static class SetupPaymentCodePool extends Thread {
        private Person person;

        @Atomic(mode = Atomic.TxMode.WRITE)
        @Override public void run() {
            final PaymentCodePool paymentCodePool = PaymentCodePool.getInstance();
            paymentCodePool.enforceMinSize(person.getUser(), new LocalDate().withDayOfMonth(1));
            paymentCodePool.refreshPaymentCodes(new LocalDate().withDayOfMonth(1));
        }

        public SetupPaymentCodePool(Person person) {
            this.person = person;
            setName(this.getClass().getSimpleName());
        }
    }

    private static class ExportPaymentCodesFromPool extends Thread {

        private final SibsOutgoingPaymentFile sibsOutgoingPaymentFile;

        public ExportPaymentCodesFromPool(SibsOutgoingPaymentFile sibsOutgoingPaymentFile) {
            this.sibsOutgoingPaymentFile = sibsOutgoingPaymentFile;
        }

        @Atomic(mode = Atomic.TxMode.READ)
        @Override
        public void run() {
            PaymentCodePool.getInstance().getPaymentCodeSet().stream()
                    .filter(EventPaymentCode::isNew)
                    .forEach(eventPaymentCode -> {
                        sibsOutgoingPaymentFile.addLine(eventPaymentCode.getCode(), eventPaymentCode.getMinAmount(), eventPaymentCode.getMaxAmount(), eventPaymentCode.getStartDate(), eventPaymentCode.getEndDate());
                    });
        }
    }

    protected String createPaymentFile(DateTime lastSuccessfulSentDateTime, Person person, StringBuilder errorsBuilder) {
        final SibsOutgoingPaymentFile sibsOutgoingPaymentFile =
                new SibsOutgoingPaymentFile(SOURCE_INSTITUTION_ID, DESTINATION_INSTITUTION_ID, ENTITY_CODE,
                        lastSuccessfulSentDateTime);

        try {
            Thread thread = new SetupPaymentCodePool(person);
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            appendToErrors(errorsBuilder, "", e);
        }

        try {
            Thread thread = new ExportPaymentCodesFromPool(sibsOutgoingPaymentFile);
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            appendToErrors(errorsBuilder, "", e);
        }

        this.setPrintedPaymentCodes(sibsOutgoingPaymentFile.getAssociatedPaymentCodes());
        return sibsOutgoingPaymentFile.render();
    }

    private String outgoingFilename() {
        return String.format("SIBS-%s.txt", new DateTime().toString("dd-MM-yyyy_H_m_s"));
    }

    public static List<SIBSOutgoingPaymentFile> readSuccessfulSentPaymentFiles() {
        return readGeneratedPaymentFiles().stream().filter(s -> s.getSuccessfulSentDate() != null).collect(Collectors.toList());
    }

    public static SIBSOutgoingPaymentFile readLastSuccessfulSentPaymentFile() {
        List<SIBSOutgoingPaymentFile> files = readSuccessfulSentPaymentFiles();

        if (files.isEmpty()) {
            return null;
        }

        files.sort(Collections.reverseOrder(SUCCESSFUL_SENT_DATE_TIME_COMPARATOR));
        return files.iterator().next();
    }

    public static List<SIBSOutgoingPaymentFile> readGeneratedPaymentFiles() {
        return new ArrayList<>(ExecutionYear.readCurrentExecutionYear().getSIBSOutgoingPaymentFilesSet());
    }

    @Atomic
    public void markAsSuccessfulSent(DateTime dateTime) {
        setSuccessfulSentDate(dateTime);
    }

}
