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

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Date;
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
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.PastAdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.candidacy.IndividualCandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.dfa.DFACandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.ExternalScholarshipGratuityContributionEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.phd.debts.PhdEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.StringNormalizer;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static final String FCT_NIF = "503904040";

    public static boolean validate(final ErrorLogConsumer consumer,
            final AccountingTransactionDetail detail) {
        final AccountingTransaction transaction = detail.getTransaction();
        final Event event = transaction.getEvent();
        if (!validate(consumer, event)) {
            return false;
        }
        try {
            transaction.getAmountWithAdjustment().getAmount();
        } catch (final DomainException ex) {
            logError(consumer, "Unable to Determine Amount", event, null, "", null, null, null, null, event);
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
                    logError(consumer, "Zero Value For Transaction", event, null, "", null, null, null, null, event);
                    return false;
                } else {
                    // consumer.accept(detail, "Ignore Adjustment Transaction", detail.getExternalId());
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean validate(final ErrorLogConsumer consumer, final Event event) {
        if (event.isCancelled()) {
            return false;
        }
        if (event instanceof ExternalScholarshipGratuityContributionEvent) {
            return false;
        }
        if (event instanceof ResidenceEvent) {
            return false;
        }
        if (!event.getParty().isPerson()) {
            if (isToIgnoreNIF(event.getParty())) {
                return false;
            }
        }
        final String eventDescription;
        try {
            eventDescription = event.getDescription().toString();
        } catch (final NullPointerException ex) {
            logError(consumer, "No Description Available", event, null, "", null, null, null, null, event);
            return false;
        }
        final Money originalAmountToPay;
        try {
            originalAmountToPay = event.getOriginalAmountToPay();
        } catch (final DomainException ex) {
            logError(consumer, "Unable to Determine Amount: " + ex.getMessage(), event, null, "", null, null, null, null, event);
            return false;
        } catch (final NullPointerException ex) {
            logError(consumer, "Unable to Determine Amount: " + ex.getMessage(), event, null, "", null, null, null, null, event);
            return false;
        }

        final Party party = event.getParty();
        final Country country = party.getCountry();

        final SimpleImmutableEntry<String, String> articleCode = SapEvent.mapToProduct(event, eventDescription, false, false);
        if (articleCode == null) {
            if (eventDescription.indexOf("Pagamento da resid") >= 0) {
                logError(consumer, "No Article Code - Residence", event, null, "", null, null, null, null, event);
            }
            return false;
        }

        final String ssn = ClientMap.uVATNumberFor(party);
        if ("PT999999990".equals(ssn) && originalAmountToPay.greaterThan(new Money(100))) {
            logError(consumer, "No Valid VAT Number", event, getUserIdentifier(party), ssn, country, party, null, null, event);
            return false;
        }

        if (!"PT999999990".equals(ssn)) {
            final PhysicalAddress address = toAddress(party, ssn.substring(0, 2));
            if (address == null) {
                logError(consumer, "No Address", event, getUserIdentifier(party), "", country, party, address, null, event);
                return false;
            }
            final Country countryOfAddress = address.getCountryOfResidence();
            if (countryOfAddress == null) {
//            logError(consumer, "No Valid Country for Address", event, getUserIdentifier(party), "", country, party, address,
//                    countryOfAddress, event);
//            return false;
            } else if ("PT".equals(countryOfAddress.getCode()) /* || "PT".equals(country.getCode()) */) {
                if (!isValidPostCode(hackAreaCodePT(address.getAreaCode(), countryOfAddress))) {
                    logError(consumer, "No Valid Post Code For Address", event, getUserIdentifier(party), "", country, party,
                            address, countryOfAddress, event);
                    return false;
                }
            }
        }

        final BigDecimal amount = originalAmountToPay.getAmount();
        //final BigDecimal amount = event.getOriginalAmountToPay().getAmount();
        if (amount.signum() <= 0) {
            if (event.isCancelled()) {
                // consumer.accept(detail, "Canceled Transaction", detail.getExternalId());
                return false;
            } else {
                //consumer.accept(t, "Zero Value For Transaction", event.getExternalId());
                return false;
            }
        }
        return true;
    }

    private static boolean isToIgnoreNIF(Party party) {
        return FCT_NIF.equalsIgnoreCase(party.getSocialSecurityNumber())
                || ("PT" + FCT_NIF).equals(party.getSocialSecurityNumber());
    }

    public static String getUserIdentifier(Party party) {
        if (party.isPerson()) {
            return ((Person) party).getUsername();
        } else {
            return party.getSocialSecurityNumber();
        }
    }

    public static String splitPath(final String id) {
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < id.length() - 1; i++, i++) {
            b.append(id.charAt(i));
            b.append(id.charAt(i + 1));
            b.append(File.separatorChar);
        }
        return b.toString();
    }

    public static PhysicalAddress toAddress(final Party party, final String countryCode) {
        return party.getPartyContactsSet().stream()
            .filter(pc -> pc instanceof PhysicalAddress)
            .map(pc -> (PhysicalAddress) pc)
            .sorted((a1, a2) -> {
                boolean d1 = a1.getDefaultContact();
                boolean d2 = a2.getDefaultContact();
                boolean ac1 = a1.getActive();
                boolean ac2 = a2.getActive();
                    // some addresses don't have an associated country and that is relevant for the postal codes
                Country c1 = a1.getCountryOfResidence();
                Country c2 = a2.getCountryOfResidence();
                    return (c1 != null && c2 == null) ? -1 : 
                        (
                          (c2 != null && c1 == null) ? 1 :
                          (
                              (ac1 && !ac2) ? -1 : (ac2 && !ac1) ? 1 : 
                              (
                                  (d1 && !d2) ? -1 : (d2 && !d1) ? 1 :
                                  a1.getExternalId().compareTo(a2.getExternalId())
                              )
                          )
                        );
            })
            .findFirst().orElse(null);
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

    private static void logError(final ErrorLogConsumer consumer, final String error, final Event event, final String user, final String vat, final Country country,
            final Party party, final PhysicalAddress address, final Country countryOfAddress, final Event e) {

        if (consumer == null) {
            return;
        }

        BigDecimal amount;
        DebtCycleType cycleType;
        
        try {
            amount = e.getOriginalAmountToPay().getAmount();
            cycleType = cycleType(e);
        } catch (Throwable t) {
            amount = null;
            cycleType = null;
        }

        consumer.accept(
                event.getExternalId(),
                user,
                party == null ? "" : party.getName(),
                amount == null ? "" : amount.toPlainString(),
                cycleType == null ? "" : cycleType.getDescription(),
                error,
                vat,
                "",
                country == null ? "" : country.getCode(),
                vat,
                address == null ? "" : address.getAddress(),
                "",
                address == null ? "" : address.getAreaCode(),
                countryOfAddress == null ? "" : countryOfAddress.getCode(),
                "",
                "",
                "");
    }

    public static DebtCycleType cycleType(final Event e) {
        final ExecutionYear debtYear = Utils.executionYearOf(e);
        final DebtCycleType type = Utils.cycleTypeFor(e, debtYear);
        if (type != null) {
            return type;
        }
        final DebtCycleType cycleType = Utils.cycleTypeFor(e, ExecutionYear.readCurrentExecutionYear());
        return cycleType == null ? Utils.cycleTypeFor(e, ExecutionYear.readCurrentExecutionYear().getPreviousExecutionYear()) : cycleType;
    }

    private static boolean isValidPostCode(final String postalCode) {
        if (postalCode != null) {
            final String v = postalCode.trim();
            return v.length() == 8 && v.charAt(4) == '-' && CharMatcher.DIGIT.matchesAllOf(v.substring(0, 4))
                    && CharMatcher.DIGIT.matchesAllOf(v.substring(5));
        }
        return false;
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

    public static String limitFormat(final int maxSize, String in) {
        if (in == null) {
            return "";
        }
        final String out = StringNormalizer.normalizePreservingCapitalizedLetters(in);
        return out.length() > maxSize ? out.substring(0, maxSize) : out;
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
                    LOGGER.debug("Failed write of file: % - Fail count: %s", path.toString(), c);
                }
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public static Date getDueDate(final Event event) {
        final DateTime dueDate;
        if (event instanceof GratuityEventWithPaymentPlan) {
            final GratuityEventWithPaymentPlan gratuityEventWithPaymentPlan = (GratuityEventWithPaymentPlan) event;
            dueDate = findLastDueDate(gratuityEventWithPaymentPlan);
        } else if (event instanceof PhdGratuityEvent) {
            final PhdGratuityEvent phdGratuityEvent = (PhdGratuityEvent) event;
            dueDate = phdGratuityEvent.getLimitDateToPay();
        } else if (event instanceof PastAdministrativeOfficeFeeAndInsuranceEvent) {
            dueDate = getDueDateByPaymentCodes(event);
        } else if (event instanceof AdministrativeOfficeFeeAndInsuranceEvent) {
            final AdministrativeOfficeFeeAndInsuranceEvent insuranceEvent = (AdministrativeOfficeFeeAndInsuranceEvent) event;
            final YearMonthDay ymd = insuranceEvent.getAdministrativeOfficeFeePaymentLimitDate();
            dueDate = ymd != null ? ymd.plusDays(1).toDateTimeAtMidnight() : getDueDateByPaymentCodes(event);
        } else {
            dueDate = getDueDateByPaymentCodes(event);
        }
        return dueDate.toDate();
    }

    private static DateTime getDueDateByPaymentCodes(final Event event) {
        final YearMonthDay ymd = event.getPaymentCodesSet().stream().map(pc -> pc.getEndDate()).max(YearMonthDay::compareTo).orElse(null);
        return ymd != null ? ymd.plusDays(1).toDateTimeAtMidnight() : event.getWhenOccured();
    }

    private static DateTime findLastDueDate(final GratuityEventWithPaymentPlan event) {
        return event.getInstallments().stream().map(i -> i.getEndDate().toDateTimeAtMidnight()).max(DateTime::compareTo).orElse(null);
    }

}
