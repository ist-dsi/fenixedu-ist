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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.AcademicAuthorizationGroup;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.PaymentCode;
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.DfaGratuityEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.accounting.events.gratuity.StandaloneEnrolmentGratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.accounting.paymentCodes.AccountingEventPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.IndividualCandidacyPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.rectorate.RectoratePaymentCode;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.Group;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

import com.google.common.collect.Sets;

import pt.ist.fenixedu.integration.domain.student.importation.DgesStudentImportationProcess;
import pt.ist.fenixedu.integration.util.sibs.SibsOutgoingPaymentFile;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SIBSOutgoingPaymentFile extends SIBSOutgoingPaymentFile_Base {
    private static final Comparator<SIBSOutgoingPaymentFile> SUCCESSFUL_SENT_DATE_TIME_COMPARATOR =
            new Comparator<SIBSOutgoingPaymentFile>() {

                @Override
                public int compare(SIBSOutgoingPaymentFile o1, SIBSOutgoingPaymentFile o2) {
                    return o1.getSuccessfulSentDate().compareTo(o2.getSuccessfulSentDate());
                }
            };

    private static final Comparator<SIBSOutgoingPaymentFile> CREATION_DATE_TIME_COMPARATOR =
            new Comparator<SIBSOutgoingPaymentFile>() {

                @Override
                public int compare(SIBSOutgoingPaymentFile o1, SIBSOutgoingPaymentFile o2) {
                    if (o1.getCreationDate() == null && o2.getCreationDate() == null) {
                        return o1.getExternalId().compareTo(o2.getExternalId());
                    } else if (o1.getCreationDate() == null) {
                        return -1;
                    } else if (o2.getCreationDate() == null) {
                        return 1;
                    } else {
                        return o1.getCreationDate().compareTo(o2.getCreationDate());
                    }
                }
            };

    public SIBSOutgoingPaymentFile(DateTime lastSuccessfulSentDateTime) {
        super();
        setExecutionYear(subjectExecutionYear());

        try {
            StringBuilder errorsBuilder = new StringBuilder();
            byte[] paymentFileContents = createPaymentFile(lastSuccessfulSentDateTime, errorsBuilder).getBytes("ASCII");
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

    protected String createPaymentFile(DateTime lastSuccessfulSentDateTime, StringBuilder errorsBuilder) {
        final ExecutionYear executionYear = subjectExecutionYear();
        final SibsOutgoingPaymentFile sibsOutgoingPaymentFile =
                new SibsOutgoingPaymentFile(SOURCE_INSTITUTION_ID, DESTINATION_INSTITUTION_ID, ENTITY_CODE,
                        lastSuccessfulSentDateTime);

        for (final Entry<Person, List<Event>> entry : getNotPayedEventsGroupedByPerson(executionYear, errorsBuilder).entrySet()) {
            for (final Event event : entry.getValue()) {
                addCalculatedPaymentCodesFromEvent(sibsOutgoingPaymentFile, event, errorsBuilder);
            }
        }

        exportDgesStudentCandidacyPaymentCodes(sibsOutgoingPaymentFile, errorsBuilder);

        try {
            final ExportThingy exportThingy = new ExportThingy(sibsOutgoingPaymentFile, errorsBuilder);
            exportThingy.start();
            exportThingy.join();
        } catch (Throwable e) {
            appendToErrors(errorsBuilder, "", e);
        }

        try {
            final ExportAnotherThingy exportThingy = new ExportAnotherThingy(sibsOutgoingPaymentFile, errorsBuilder);
            exportThingy.start();
            exportThingy.join();
        } catch (Throwable e) {
            appendToErrors(errorsBuilder, "", e);
        }

        try {
            final SpecialSeasonEnrolmentEventStuff stuff = new SpecialSeasonEnrolmentEventStuff(sibsOutgoingPaymentFile, errorsBuilder);
            stuff.start();
            stuff.join();
        } catch (Throwable e) {
            appendToErrors(errorsBuilder, "", e);
        }

        this.setPrintedPaymentCodes(sibsOutgoingPaymentFile.getAssociatedPaymentCodes());
        invalidateOldPaymentCodes(sibsOutgoingPaymentFile, errorsBuilder);

        return sibsOutgoingPaymentFile.render();
    }

    private void invalidateOldPaymentCodes(SibsOutgoingPaymentFile sibsOutgoingPaymentFile, StringBuilder errorsBuilder) {
        SIBSOutgoingPaymentFile previous = readPreviousOfLastGeneratedPaymentFile();
        PrintedPaymentCodes currentSet = this.getPrintedPaymentCodes();
        PrintedPaymentCodes previousSet = previous == null ? null : previous.getPrintedPaymentCodes();

        if (previousSet != null && previousSet.getPaymentCodes() != null) {
            Collection<String> oldPaymentCodes = Sets.difference(previousSet.getPaymentCodes(), currentSet.getPaymentCodes());

            for (String oldCode : oldPaymentCodes) {
                sibsOutgoingPaymentFile.addLine(oldCode, new Money("0.01"), new Money("0.01"), new DateTime().minusDays(5)
                        .toYearMonthDay(), new DateTime().minusDays(5).toYearMonthDay());
            }
        }

    }

    private void exportDgesStudentCandidacyPaymentCodes(SibsOutgoingPaymentFile sibsOutgoingPaymentFile,
            StringBuilder errorsBuilder) {
        try {
            CalculateStudentCandidacyPaymentCodes workThread =
                    new CalculateStudentCandidacyPaymentCodes(sibsOutgoingPaymentFile, errorsBuilder);
            workThread.start();
            workThread.join();
        } catch (Throwable e) {
            appendToErrors(errorsBuilder, "", e);
        }

    }

    protected void exportIndividualCandidacyPaymentCodes(SibsOutgoingPaymentFile sibsFile, StringBuilder errorsBuilder) {
        Set<? extends PaymentCode> individualCandidacyPaymentCodeList = Bennu.getInstance().getPaymentCodesSet();

        LocalDate date = new LocalDate();

        for (PaymentCode paymentCode : individualCandidacyPaymentCodeList) {

            if (!(paymentCode instanceof IndividualCandidacyPaymentCode)) {
                continue;
            }

            if (!paymentCode.getStartDate().isAfter(date) && !paymentCode.getEndDate().isBefore(date) && paymentCode.isNew()) {
                addPaymentCode(sibsFile, paymentCode, errorsBuilder);
            }
        }
    }

    protected void exportRectoratePaymentCodes(SibsOutgoingPaymentFile sibsFile, StringBuilder errorsBuilder) {
        List<RectoratePaymentCode> allRectoratePaymentCodes = RectoratePaymentCode.getAllRectoratePaymentCodes();

        final LocalDate now = new LocalDate();

        for (RectoratePaymentCode rectoratePaymentCode : allRectoratePaymentCodes) {
            if (rectoratePaymentCode.getEndDate().isAfter(now)) {
                addPaymentCode(sibsFile, rectoratePaymentCode, errorsBuilder);
            }
        }

    }

    protected void exportSpecialSeasonEnrolmentCodes(SibsOutgoingPaymentFile sibsFile, StringBuilder errorsBuilder) {
        final LocalDate now = new LocalDate();

        Bennu.getInstance().getStudentsSet().stream()

        .map(Student::getPerson)

        .filter(Objects::nonNull)

        .flatMap(p -> p.getEventsSet().stream())

        .filter(e -> e instanceof SpecialSeasonEnrolmentEvent)

        .filter(Event::isOpen)

        .flatMap(e -> e.getAllPaymentCodes().stream())

        .filter(pc -> pc.getEndDate().isAfter(now))

        .forEach(pc -> addPaymentCode(sibsFile, pc, errorsBuilder));
    }

    protected void addPaymentCode(final SibsOutgoingPaymentFile file, final PaymentCode paymentCode, StringBuilder errorsBuilder) {
        try {
            file.addAssociatedPaymentCode(paymentCode);
            file.addLine(paymentCode.getCode(), paymentCode.getMinAmount(), paymentCode.getMaxAmount(),
                    paymentCode.getStartDate(), paymentCode.getEndDate());
        } catch (Throwable e) {
            appendToErrors(errorsBuilder, paymentCode.getExternalId(), e);
        }
    }

    protected void addCalculatedPaymentCodesFromEvent(final SibsOutgoingPaymentFile file, final Event event,
            StringBuilder errorsBuilder) {
        try {
            CalculatePaymentCodes thread = new CalculatePaymentCodes(event.getExternalId(), errorsBuilder, file);
            thread.start();
            thread.join();
        } catch (Throwable e) {
            appendToErrors(errorsBuilder, event.getExternalId(), e);
        }

    }

    private static ExecutionYear subjectExecutionYear() {
        return ExecutionYear.readCurrentExecutionYear();
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

        Collections.sort(files, Collections.reverseOrder(SUCCESSFUL_SENT_DATE_TIME_COMPARATOR));
        return files.iterator().next();
    }

    public static SIBSOutgoingPaymentFile readLastGeneratedPaymentFile() {
        List<SIBSOutgoingPaymentFile> files = readGeneratedPaymentFiles();
        Collections.sort(files, Collections.reverseOrder(CREATION_DATE_TIME_COMPARATOR));

        return files.iterator().next();
    }

    public static SIBSOutgoingPaymentFile readPreviousOfLastGeneratedPaymentFile() {
        List<SIBSOutgoingPaymentFile> files = readGeneratedPaymentFiles();
        Collections.sort(files, Collections.reverseOrder(CREATION_DATE_TIME_COMPARATOR));

        if (files.size() <= 1) {
            return null;
        }

        return files.get(1);
    }

    public static List<SIBSOutgoingPaymentFile> readGeneratedPaymentFiles() {
        return new ArrayList<SIBSOutgoingPaymentFile>(subjectExecutionYear().getSIBSOutgoingPaymentFilesSet());
    }

    @Atomic
    public void markAsSuccessfulSent(DateTime dateTime) {
        setSuccessfulSentDate(dateTime);
    }

    private class CalculatePaymentCodes extends Thread {
        private final String eventExternalId;
        private final StringBuilder errorsBuilder;
        private final SibsOutgoingPaymentFile sibsFile;

        public CalculatePaymentCodes(String eventExternalId, StringBuilder errorsBuilder, SibsOutgoingPaymentFile sibsFile) {
            this.eventExternalId = eventExternalId;
            this.errorsBuilder = errorsBuilder;
            this.sibsFile = sibsFile;
        }

        @Override
        @Atomic(mode = TxMode.READ)
        public void run() {
            try {
                txDo();
            } catch (Throwable e) {
                appendToErrors(errorsBuilder, eventExternalId, e);
            }
        }

        @Atomic
        private void txDo() {
            Event event = FenixFramework.getDomainObject(eventExternalId);

            for (final AccountingEventPaymentCode paymentCode : event.calculatePaymentCodes()) {
                this.sibsFile.addAssociatedPaymentCode(paymentCode);
                sibsFile.addLine(paymentCode.getCode(), paymentCode.getMinAmount(), paymentCode.getMaxAmount(),
                        paymentCode.getStartDate(), paymentCode.getEndDate());
            }
        }
    }

    private class ExportThingy extends Thread {

        final SibsOutgoingPaymentFile sibsOutgoingPaymentFile;
        final StringBuilder errorsBuilder;

        public ExportThingy(final SibsOutgoingPaymentFile sibsOutgoingPaymentFile, final StringBuilder errorsBuilder) {
            this.sibsOutgoingPaymentFile = sibsOutgoingPaymentFile;
            this.errorsBuilder = errorsBuilder;
        }

        @Override
        @Atomic(mode = TxMode.READ)
        public void run() {
            txDo();
        }

        private void txDo() {
            exportIndividualCandidacyPaymentCodes(sibsOutgoingPaymentFile, errorsBuilder);
        }
    }

    private class ExportAnotherThingy extends Thread {

        final SibsOutgoingPaymentFile sibsOutgoingPaymentFile;
        final StringBuilder errorsBuilder;

        public ExportAnotherThingy(final SibsOutgoingPaymentFile sibsOutgoingPaymentFile, final StringBuilder errorsBuilder) {
            this.sibsOutgoingPaymentFile = sibsOutgoingPaymentFile;
            this.errorsBuilder = errorsBuilder;
        }

        @Override
        @Atomic(mode = TxMode.READ)
        public void run() {
            txDo();
        }

        private void txDo() {
            exportRectoratePaymentCodes(sibsOutgoingPaymentFile, errorsBuilder);
        }

    }

    private class SpecialSeasonEnrolmentEventStuff extends Thread {

        final SibsOutgoingPaymentFile sibsOutgoingPaymentFile;
        final StringBuilder errorsBuilder;

        public SpecialSeasonEnrolmentEventStuff(final SibsOutgoingPaymentFile sibsOutgoingPaymentFile, final StringBuilder errorsBuilder) {
            this.sibsOutgoingPaymentFile = sibsOutgoingPaymentFile;
            this.errorsBuilder = errorsBuilder;
        }

        @Override
        @Atomic(mode = TxMode.READ)
        public void run() {
            txDo();
        }

        private void txDo() {
            exportSpecialSeasonEnrolmentCodes(sibsOutgoingPaymentFile, errorsBuilder);
        }

    }

    private class CalculateStudentCandidacyPaymentCodes extends Thread {
        final SibsOutgoingPaymentFile file;
        final StringBuilder errorsBuilder;

        CalculateStudentCandidacyPaymentCodes(final SibsOutgoingPaymentFile file, final StringBuilder errorsBuilder) {
            this.file = file;
            this.errorsBuilder = errorsBuilder;
        }

        @Override
        @Atomic(mode = TxMode.READ)
        public void run() {
            txDo();
        }

        private void txDo() {
            List<DgesStudentImportationProcess> processList =
                    DgesStudentImportationProcess.readDoneJobs(ExecutionYear.readCurrentExecutionYear());

            for (DgesStudentImportationProcess process : processList) {
                for (StudentCandidacy studentCandidacy : process.getStudentCandidacySet()) {
                    for (PaymentCode paymentCode : studentCandidacy.getAvailablePaymentCodesSet()) {
                        try {
                            if (paymentCode.isCancelled()) {
                                continue;
                            }

                            if (paymentCode.isInvalid()) {
                                continue;
                            }

                            if (!paymentCode.getEndDate().isAfter(new YearMonthDay())) {
                                continue;
                            }

                            if (((AccountingEventPaymentCode) paymentCode).getAccountingEvent() != null) {
                                continue;
                            }

                            this.file.addAssociatedPaymentCode(paymentCode);

                            this.file.addLine(paymentCode.getCode(), paymentCode.getMinAmount(), paymentCode.getMaxAmount(),
                                    paymentCode.getStartDate(), paymentCode.getEndDate());
                        } catch (Throwable e) {
                            appendToErrors(errorsBuilder, paymentCode.getExternalId(), e);
                        }
                    }
                }
            }
        }
    }

}
