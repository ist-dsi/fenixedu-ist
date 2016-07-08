/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST GIAF Invoices.
 *
 * FenixEdu IST GIAF Invoices is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST GIAF Invoices is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST GIAF Invoices.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.giaf.invoices;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.PostingRule;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.candidacy.IndividualCandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.dfa.DFACandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.candidacyProcess.CandidacyProcess;
import org.fenixedu.academic.domain.candidacyProcess.IndividualCandidacy;
import org.fenixedu.academic.domain.candidacyProcess.IndividualCandidacyProcess;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.phd.candidacy.PhdProgramCandidacyEvent;
import org.fenixedu.academic.domain.phd.candidacy.PhdProgramCandidacyProcess;
import org.fenixedu.academic.domain.phd.debts.PhdEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.gson.JsonObject;

import pt.ist.fenixframework.DomainObject;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static boolean validate(final ErrorConsumer<AccountingTransactionDetail> consumer,
            final AccountingTransactionDetail detail) {
        final AccountingTransaction transaction = detail.getTransaction();
        final Event event = transaction.getEvent();
        if (!validateEvent(consumer, detail, event)) {
            return false;
        }
        try {
            transaction.getAmountWithAdjustment().getAmount();
        } catch (final DomainException ex) {
            consumer.accept(detail, "Unable to Determine Amount", ex.getMessage());
            return false;
        }

        final BigDecimal amount = transaction.getAmountWithAdjustment().getAmount();
        final AccountingTransaction adjustedTransaction = transaction.getAdjustedTransaction();
        if (adjustedTransaction != null && adjustedTransaction.getAmountWithAdjustment().getAmount().signum() <= 0) {
            // consumer.accept(detail, "Ignore Adjusting Transaction", detail.getExternalId());
            return false;
        }
        if (amount.signum() <= 0) {
            if (event.isCancelled()) {
                // consumer.accept(detail, "Canceled Transaction", detail.getExternalId());
                return false;
            } else {
                if (transaction.getAdjustmentTransactionsSet().isEmpty()) {
                    consumer.accept(detail, "Zero Value For Transaction", detail.getExternalId());
                    return false;
                } else {
                    // consumer.accept(detail, "Ignore Adjustment Transaction", detail.getExternalId());
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean validate(final ErrorConsumer<Event> consumer, final Event event) {
        return validateEvent(consumer, event, event);
    }

    private static <T> boolean validateEvent(final ErrorConsumer<T> consumer, final T t, final Event event) {
        final String eventDescription;
        try {
            eventDescription = event.getDescription().toString();
        } catch (final NullPointerException ex) {
            consumer.accept(t, "No Description Available", ex.getMessage());
            return false;
        }
        try {
            event.getOriginalAmountToPay();
        } catch (final DomainException ex) {
            consumer.accept(t, "Unable to Determine Amount", ex.getMessage());
            return false;
        } catch (final NullPointerException ex) {
            consumer.accept(t, "Unable to Determine Amount", ex.getMessage());
            return false;
        }
        final String articleCode = Utils.mapToArticleCode(event, eventDescription);
        if (articleCode == null) {
            if (eventDescription.indexOf("Pagamento da resid") != 0) {
                consumer.accept(t, "No Article Code", eventDescription);
            }
            return false;
        }

        final Person person = event.getPerson();
        final Country country = person.getCountry();
        if (event.getParty().isPerson()) {
            if (country == null) {
                consumer.accept(t, "No Country", person.getUsername());
                return false;
            }
            final PhysicalAddress address = Utils.toAddress(person);
            if (address == null) {
                consumer.accept(t, "No Address", person.getUsername());
                return false;
            }
            final Country countryOfAddress = address.getCountryOfResidence();
            if (countryOfAddress == null) {
                consumer.accept(t, "No Valid Country for Address", person.getUsername());
                return false;
            } else if ("PT".equals(countryOfAddress.getCode()) /* || "PT".equals(country.getCode()) */) {
                if (!Utils.isValidPostCode(Utils.hackAreaCodePT(address.getAreaCode(), countryOfAddress))) {
                    consumer.accept(t, "No Valid Post Code For Address For", person.getUsername());
                    return false;
                }
            }

            final String vat = Utils.toVatNumber(person);
            if (vat == null) {
                consumer.accept(t, "No VAT Number", person.getUsername());
                return false;
            }
            if ("PT".equals(country.getCode())) {
                if (!Utils.isVatValidForPT(vat)) {
                    consumer.accept(t, "No a Valid PT VAT Number", vat);
                    return false;
                }
            }
        } else {
            consumer.accept(t, "Not a person", event.getParty().toString());
            return false;
        }

        final BigDecimal amount = calculateTotalDebtValue(event).getAmount();
        //final BigDecimal amount = event.getOriginalAmountToPay().getAmount();
        if (amount.signum() <= 0) {
            if (event.isCancelled()) {
                // consumer.accept(detail, "Canceled Transaction", detail.getExternalId());
                return false;
            } else {
                consumer.accept(t, "Zero Value For Transaction", event.getExternalId());
                return false;
            }
        }
        return true;
    }

    public static JsonObject toJson(final Person person) {
        final String clientCode = toClientCode(person);

        final String vat = toVatNumber(person);
        final String vatCountry = countryForVat(vat, person);

        final PhysicalAddress address = toAddress(person);
        final String street = limitFormat(60, address.getAddress()).replace('\t', ' ');
        final String locality = limitFormat(35, address.getAreaOfAreaCode());
        final String postCode = hackAreaCode(address.getAreaCode(), address.getCountryOfResidence(), person);
        final String country = address.getCountryOfResidence().getCode();
        final String name = limitFormat(50, getDisplayName(person));

        final JsonObject jo = new JsonObject();
        jo.addProperty("id", clientCode);
        jo.addProperty("name", limitFormat(60, name));
        jo.addProperty("type", "S");
        jo.addProperty("countryOfVatNumber", vatCountry);
        jo.addProperty("vatNumber", vat);
        jo.addProperty("address", street);
        jo.addProperty("locality", locality);
        jo.addProperty("postCode", postCode);
        jo.addProperty("countryOfAddress", country);
        jo.addProperty("phone", "");
        jo.addProperty("fax", "");
        jo.addProperty("email", "");
        jo.addProperty("ban", "");
        jo.addProperty("iban", "");
        jo.addProperty("swift", "");
        jo.addProperty("paymentMethod", "CH");

        return jo;
    }

    public static Money calculateTotalDebtValue(final Event event) {
        final DateTime when = event.getWhenOccured().plusSeconds(1);
        final PostingRule rule = event.getPostingRule();
        return call(rule, event, when, false);
    }

    private static Money call(final PostingRule rule, final Event event, final DateTime when, final boolean applyDiscount) {
        try {
            final Method method =
                    PostingRule.class
                            .getDeclaredMethod("doCalculationForAmountToPay", Event.class, DateTime.class, boolean.class);
            method.setAccessible(true);
            return (Money) method.invoke(rule, event, when, applyDiscount);
        } catch (final NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new Error(e);
        }
    }

    final static ErrorConsumer<AccountingTransactionDetail> VOID_CONSUMER = new ErrorConsumer<AccountingTransactionDetail>() {
        @Override
        public void accept(AccountingTransactionDetail t, String erro, String arg) {
        }
    };

    public static Money calculateAmountPayed(final Event event, final DateTime threshold) {
        return event.getAccountingTransactionsSet().stream().filter(t -> t.getWhenRegistered().isBefore(threshold))
                .filter(t -> validate(VOID_CONSUMER, t.getTransactionDetail())).map(t -> t.getAmountWithAdjustment())
                .reduce(Money.ZERO, Money::add);
    }


    public static byte[] toBytes(final Spreadsheet sheet) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            sheet.exportToXLSSheet(bos);
        } catch (final IOException e) {
            throw new Error(e);
        }
        return bos.toByteArray();
    }

    public static String valueOf(final AccountingTransactionDetail detail) {
        try {
            return detail.getTransaction().getAmountWithAdjustment().getAmount().toString();
        } catch (final DomainException ex) {
            return "?";
        }
    }

    public static ExecutionYear executionYearOf(final Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear.readByDateTime(event
                .getWhenOccured());
    }

    private static DebtCycleType getCycleType(Collection<CycleType> cycleTypes) {
        if (cycleTypes.size() > 1) {
            return DebtCycleType.INTEGRATED_MASTER;
        } else if (cycleTypes.size() > 0) {
            return DebtCycleType.valueOf(cycleTypes.iterator().next());
        }
        return null;
    }

    public static DebtCycleType cycleTypeFor(Event event, ExecutionYear executionYear) {
        if (event instanceof PhdEvent || event.isFctScholarshipPhdGratuityContribuitionEvent()) {
            return DebtCycleType.THIRD_CYCLE;
        }
        if (event.getParty().isPerson()) {
            if (event.isGratuity()) {
                final GratuityEvent gratuityEvent = (GratuityEvent) event;
                final StudentCurricularPlan scp = gratuityEvent.getStudentCurricularPlan();
                if (scp != null) {
                    Collection<CycleType> cycleTypes = scp.getDegree().getCycleTypes();
                    DebtCycleType cycleType = getCycleType(cycleTypes);
                    if (cycleType != null) {
                        return cycleType;
                    }
                }
            }
            Student student = event.getPerson().getStudent();
            if (student != null) {
                for (Registration registration : student.getRegistrationsSet()) {
                    StudentCurricularPlan scp = registration.getStudentCurricularPlan(executionYear);
                    if (scp == null) {
                        StudentCurricularPlan lastStudentCurricularPlan = registration.getLastStudentCurricularPlan();
                        if (lastStudentCurricularPlan != null
                                && lastStudentCurricularPlan.getStartExecutionYear() == executionYear) {
                            scp = lastStudentCurricularPlan;
                        }
                    }
                    if (scp != null) {
                        Collection<CycleType> cycleTypes = registration.getDegree().getCycleTypes();
                        DebtCycleType cycleType = getCycleType(cycleTypes);
                        if (cycleType != null) {
                            return cycleType;
                        }
                    }
                }
            } else if (event.isIndividualCandidacyEvent()) {
                IndividualCandidacyEvent candidacyEvent = (IndividualCandidacyEvent) event;
                Set<DebtCycleType> cycleTypes = new HashSet<DebtCycleType>();
                for (Degree degree : candidacyEvent.getIndividualCandidacy().getAllDegrees()) {
                    DebtCycleType cycleType = getCycleType(degree.getCycleTypes());
                    if (cycleType != null) {
                        cycleTypes.add(cycleType);
                    }
                }
                if (cycleTypes.size() == 1) {
                    return cycleTypes.iterator().next();
                }
            }
            if (!event.getPerson().getPhdIndividualProgramProcessesSet().isEmpty()) {
                return DebtCycleType.THIRD_CYCLE;
            }
        }
        return null;
    }

    public static String mapToArticleCode(final Event event, final String eventDescription) {
        if (event.isGratuity()) {
            final GratuityEvent gratuityEvent = (GratuityEvent) event;
            final StudentCurricularPlan scp = gratuityEvent.getStudentCurricularPlan();
            final Degree degree = scp.getDegree();
            if (scp.getRegistration().getRegistrationProtocol().isAlien()) {
                return "FEINTERN";// PROPINAS INTERNACIONAL
            }
            if (degree.isFirstCycle() && degree.isSecondCycle()) {
                return "FEMESTIN";// 724114 PROPINAS MESTRADO INTEGRADO
            }
            if (degree.isFirstCycle()) {
                return "FE1CICLO";// 724111 PROPINAS 1 CICLO
            }
            if (degree.isSecondCycle()) {
                return "FE2CICLO";// 724112 PROPINAS 2 CICLO
            }
            if (degree.isThirdCycle()) {
                return "FE3CICLO";// 724113 PROPINAS 3 CICLO
            }
            return "FEOUTPRO";// 724116 PROPINAS - OUTROS
        }
        if (event instanceof PhdGratuityEvent) {
            return "FE3CICLO";// 724113 PROPINAS 3 CICLO
        }
        if (event.isResidenceEvent()) {
            return null;
        }
        if (event.isFctScholarshipPhdGratuityContribuitionEvent()) {
            return null;
        }
        if (event.isAcademicServiceRequestEvent()) {
            if (eventDescription.indexOf(" Reingresso") >= 0) {
                return "FETAXAOUT";// 72419 OUTRAS TAXAS
            }
            return "FEEMOL";// 7246 EMOLUMENTOS
        }
        if (event.isDfaRegistrationEvent()) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        if (event.isIndividualCandidacyEvent()) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        if (event.isEnrolmentOutOfPeriod()) {
            return "FETAXAOUT";// 72419 OUTRAS TAXAS
        }
        if (event instanceof AdministrativeOfficeFeeAndInsuranceEvent) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        if (event instanceof InsuranceEvent) {
            return "FESEGESC";// 72415 SEGURO ESCOLAR
        }
        if (event.isSpecializationDegreeRegistrationEvent()) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        if (event instanceof ImprovementOfApprovedEnrolmentEvent) {
            return "FETAXAMN";// 72414 TAXAS DE MELHORIAS DE NOTAS
        }
        if (event instanceof DFACandidacyEvent) {
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA"
        }
        if (event instanceof SpecialSeasonEnrolmentEvent) {
            return "FETAXAEX";// 72413 TAXAS  DE EXAMES
        }
        if (event.isPhdEvent()) {
            if (eventDescription.indexOf("Taxa de Inscri") >= 0) {
                return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
            }
            if (eventDescription.indexOf("Requerimento de provas") >= 0) {
                return "FETAXAEX";// 72413 TAXAS  DE EXAMES
            }
            return "FETAXAMAT";// 72412 TAXAS DE MATRICULA
        }
        throw new Error("not.supported: " + event.getExternalId());
    }

    private static PhysicalAddress toAddress(final Person person) {
        PhysicalAddress address = person.getDefaultPhysicalAddress();
        if (address == null) {
            for (final PartyContact contact : person.getPartyContactsSet()) {
                if (contact instanceof PhysicalAddress) {
                    address = (PhysicalAddress) contact;
                    break;
                }
            }
        }
        return address;
    }

    private static boolean isValidPostCode(final String postalCode) {
        if (postalCode != null) {
            final String v = postalCode.trim();
            return v.length() == 8 && v.charAt(4) == '-' && CharMatcher.DIGIT.matchesAllOf(v.substring(0, 4))
                    && CharMatcher.DIGIT.matchesAllOf(v.substring(5));
        }
        return false;
    }

    private static String toVatNumber(final Person person) {
        final Country country = person.getCountry();
        final String ssn = person.getSocialSecurityNumber();
        final String vat = toVatNumber(ssn);
        if (vat != null && isVatValidForPT(vat)) {
            return vat;
        }
        if (country != null && "PT".equals(country.getCode())) {
            return null;
        }
        final User user = person.getUser();
        return user == null ? makeUpSomeRandomNumber(person) : user.getUsername();
    }

    private static String makeUpSomeRandomNumber(final Person person) {
        final String id = person.getExternalId();
        return "FE" + id.substring(id.length() - 10, id.length());
    }

    private static String toVatNumber(final String ssn) {
        return ssn == null ? null : ssn.startsWith("PT") ? ssn.substring(2) : ssn;
    }

    private static boolean isVatValidForPT(final String vat) {
        if (vat.length() != 9) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            if (!Character.isDigit(vat.charAt(i))) {
                return false;
            }
        }
        if (Integer.parseInt(vat) <= 0) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            final int c = Character.getNumericValue(vat.charAt(i));
            sum += c * (9 - i);
        }
        final int controleDigit = Character.getNumericValue(vat.charAt(8));
        final int remainder = sum % 11;
        int digit = 11 - remainder;
        return digit > 9 ? controleDigit == 0 : digit == controleDigit;
    }

    public static String toClientCode(final Person person) {
        final User user = person.getUser();
        return user == null ? makeUpSomeRandomNumber(person) : user.getUsername();
    }

    public static String toPaymentDocumentNumber(final AccountingTransactionDetail detail) {
        return detail instanceof SibsTransactionDetail ? ((SibsTransactionDetail) detail).getSibsCode() : "";
    }

    public static String limitFormat(final int maxSize, String in) {
        if (in == null) {
            return "";
        }
        final String out = StringNormalizer.normalizeAndRemoveAccents(in).toUpperCase();
        return out.length() > maxSize ? out.substring(0, maxSize) : out;
    }

    public static String countryForVat(final String vat, Person person) {
        return isVatValidForPT(vat) ? "PT" : person.getCountry().getCode();
    }

    private static String hackAreaCode(final String areaCode, final Country countryOfResidence, final Person person) {
        return countryOfResidence != null && !"PT".equals(countryOfResidence.getCode()) ? "0" : hackAreaCodePT(areaCode,
                countryOfResidence);
    }

    private static String hackAreaCodePT(final String areaCode, final Country countryOfResidence) {
        if (countryOfResidence != null && "PT".equals(countryOfResidence.getCode())) {
            if (areaCode == null || areaCode.isEmpty()) {
                return "0000-000";
            }
            if (areaCode.length() == 4) {
                return areaCode + "-001";
            }
        }
        return areaCode;
    }

    private static String getDisplayName(final Person person) {
        final User user = person.getUser();
        final UserProfile profile = user == null ? null : user.getProfile();
        final String displayName = profile == null ? null : profile.getDisplayName();
        return displayName == null ? person.getName() : displayName;
    }

    public static String idFor(final DomainObject object) {
        return object.getExternalId();
    }

    public static String idForDiscount(final Event event) {
        final String id = idFor(event);
        return "E" + id;
    }

    public static void writeFileWithoutFailuer(final Path path, final byte[] content, final boolean append) {
        for (int c = 0;; c++) {
            try {
                if (append) {
                    Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
                } else {
                    Files.write(path, content);
                }
                return;
            } catch (final Throwable e) {
                if (c > 0 && c % 5 == 0) {
                    LOGGER.debug("Failed write of invoice file: % - Fail count: %s", path.toString(), c);
                }
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e1) {
                }
            }
        }
    }

}
