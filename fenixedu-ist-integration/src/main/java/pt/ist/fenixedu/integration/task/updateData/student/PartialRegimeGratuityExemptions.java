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
package pt.ist.fenixedu.integration.task.updateData.student;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.Installment;
import org.fenixedu.academic.domain.accounting.PaymentPlan;
import org.fenixedu.academic.domain.accounting.PostingRule;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEventWithPaymentPlan;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityExemptionJustificationType;
import org.fenixedu.academic.domain.accounting.events.gratuity.PercentageGratuityExemption;
import org.fenixedu.academic.domain.accounting.installments.PartialRegimeInstallment;
import org.fenixedu.academic.domain.accounting.paymentPlans.FullGratuityPaymentPlanForPartialRegime;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@Task(englishTitle = "PartialRegimeGratuityExemptions", readOnly = true)
public class PartialRegimeGratuityExemptions extends CronTask {

    final static YearMonthDay DISPATCH_DATE = new YearMonthDay(2015, 12, 01);
    static String JUSTIFICATION_TEXT = "Aplicação da fórmula de cálculo de propina em tempo parcial ";
    static int exemptionsCreated = 0;
    final static int DECIMAL_PRECISION = 6;

    @Override
    public void runTask() throws Exception {
        exemptionsCreated = 0;
        int processedStudents = 0;
        ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        JUSTIFICATION_TEXT = JUSTIFICATION_TEXT + currentExecutionYear.getName();
        for (AnnualEvent event : currentExecutionYear.getAnnualEventsSet()) {
            if (event instanceof GratuityEventWithPaymentPlan) {
                GratuityEventWithPaymentPlan gratuityEvent = (GratuityEventWithPaymentPlan) event;
                if (gratuityEvent.getGratuityPaymentPlan() instanceof FullGratuityPaymentPlanForPartialRegime
                        && !gratuityEvent.isCancelled()) {
                    processedStudents++;
                    taskLog("-------- Amount values for %s --------\n", event.getPerson().getUsername());
                    try {
                        Money amountToPay =
                                calculateOriginalDebtAmount(gratuityEvent, gratuityEvent.getWhenOccured().plusSeconds(1), false);
                        Money newAmountToPay = calculateAmountToPay(gratuityEvent, gratuityEvent.getGratuityPaymentPlan());
                        taskLog("Valor antigo: %s - Valor novo: %s\n", amountToPay.getAmountAsString(),
                                newAmountToPay.getAmountAsString());
                        //since the exemption is given in % and the system only counts up to 2 decimal cases round up, there maybe values off by 1 cent
                        if (amountToPay.greaterThan(newAmountToPay.add(new BigDecimal(0.01)))) {
                            createExemption(gratuityEvent, amountToPay, newAmountToPay);
                        }
                    } catch (Exception e) {
                        taskLog("Exception processing event: %s\n", gratuityEvent.getExternalId());
                        taskLog("Error: %s\n", e.getMessage());
                    }
                }
            }
        }
        taskLog("\nStudents processed: %s\n", processedStudents);
        taskLog("Exemptions created: %s\n", exemptionsCreated);
    }

    private Money calculateOriginalDebtAmount(final Event event, final DateTime when, final boolean applyDiscount) {
        try {
            final Method method = PostingRule.class.getDeclaredMethod("doCalculationForAmountToPay", Event.class, DateTime.class,
                    boolean.class);
            method.setAccessible(true);
            return (Money) method.invoke(event.getPostingRule(), event, when, applyDiscount);
        } catch (final NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new Error(e);
        }
    }

    // Creates the exemption to adjust the value to pay, if an exemption already exists and was created by this script
    // has to be deleted, calculated the new clean value that the student is suppose to pay and created a new exemption with new total value to exempt
    @Atomic(mode = TxMode.WRITE)
    private void createExemption(GratuityEventWithPaymentPlan event, Money amountToPay, Money newAmountToPay) {
        if (event.getExemptionsSet().isEmpty()) {
            BigDecimal percentage = BigDecimal.ONE.subtract(
                    newAmountToPay.getAmount().divide(amountToPay.getAmount(), DECIMAL_PRECISION, RoundingMode.HALF_UP));
            new PercentageGratuityExemption((GratuityEventWithPaymentPlan) event,
                    GratuityExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION, JUSTIFICATION_TEXT, DISPATCH_DATE,
                    percentage);
            exemptionsCreated++;
            taskLog("Created an exemption for %s\n", event.getPerson().getUsername());
        } else {
            event.getExemptionsSet().stream().filter(this::isAutomaticExemption).forEach(ex -> {
                taskLog("Deleted exemption from %s\n", event.getPerson().getUsername());
                ex.delete();
            });
            Money cleanNewAmountToPay = calculateAmountToPay(event, event.getGratuityPaymentPlan());
            BigDecimal percentage = BigDecimal.ONE.subtract(
                    cleanNewAmountToPay.getAmount().divide(amountToPay.getAmount(), DECIMAL_PRECISION, RoundingMode.HALF_UP));
            new PercentageGratuityExemption((GratuityEventWithPaymentPlan) event,
                    GratuityExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION, JUSTIFICATION_TEXT, DISPATCH_DATE,
                    percentage);
            exemptionsCreated++;
            taskLog("Created a new exemption for %s\n", event.getPerson().getUsername());
        }
    }

    private boolean isAutomaticExemption(Exemption exemption) {
        String reason = Normalizer.normalize(exemption.getReason(), Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String justification =
                Normalizer.normalize(JUSTIFICATION_TEXT, Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return reason.contains(justification.substring(0, justification.length() - 10)); //to remove the execution year, in the older version is YY/YY and in this one is YYYY/YYYY
    }

    private Money calculateAmountToPay(GratuityEventWithPaymentPlan gratuityEvent, PaymentPlan paymentPlan) {
        final Money totalAmountToPay = doCalculationForAmountToPay(gratuityEvent, true, paymentPlan);

        if (totalAmountToPay == null) {
            return Money.ZERO;
        }
        return totalAmountToPay;
    }

    private Money calculateTotalAmount(final Event event, final BigDecimal discountPercentage, PaymentPlan paymentPlan) {
        Money result = Money.ZERO;
        for (final Money amount : calculateInstallmentTotalAmounts(event, discountPercentage, paymentPlan).values()) {
            result = result.add(amount);
        }
        return result;
    }

    private Map<Installment, Money> calculateInstallmentTotalAmounts(final Event event, final BigDecimal discountPercentage,
            PaymentPlan paymentPlan) {
        final Map<Installment, Money> result = new HashMap<Installment, Money>();
        for (final Installment installment : paymentPlan.getInstallmentsSortedByEndDate()) {
            result.put(installment, calculateBaseAmount(event, (PartialRegimeInstallment) installment));
        }
        return result;
    }

    //we want to calculate the value without the exemptions that were created by the script
    private Money doCalculationForAmountToPay(Event event, boolean applyDiscount, PaymentPlan paymentPlan) {
        final BigDecimal discountPercentage = BigDecimal.ZERO;
        return calculateTotalAmount(event, discountPercentage, paymentPlan);
    }

    /**
     * Formula: 0.2 x Amount x (1 + EnroledEcts / ectsForAmount)
     */
    //installment
    private Money calculateBaseAmount(Event event, PartialRegimeInstallment installment) {
        final BigDecimal enroledEcts = getEnroledEcts((GratuityEvent) event, installment);
        if (enroledEcts.compareTo(BigDecimal.ZERO) == 0) {
            return Money.ZERO;
        }
        final BigDecimal proporcionToPay = enroledEcts.divide(installment.getEctsForAmount());
        // TODO: Fix Money limitation of scale = 2 to prevent this type of
        // coding
        final BigDecimal amount = installment.getAmount().getAmount().setScale(10);
        return new Money(amount.multiply(new BigDecimal("0.2")).multiply(BigDecimal.ONE.add(proporcionToPay)));
    }

    private BigDecimal getEnroledEcts(GratuityEvent gratuityEvent, PartialRegimeInstallment installment) {
        BigDecimal total = BigDecimal.ZERO;
        for (final Enrolment enrolment : collectEnrolments(gratuityEvent, installment)) {
            total = total.add(enrolment.getEctsCreditsForCurriculum());
        }
        return total;
    }

    private Set<Enrolment> collectEnrolments(GratuityEvent gratuityEvent, PartialRegimeInstallment installment) {
        final Set<Enrolment> result = new HashSet<Enrolment>();
        for (final ExecutionSemester executionSemester : installment.getExecutionSemestersSet()) {
            for (final CycleCurriculumGroup cycleCurriculumGroup : gratuityEvent.getStudentCurricularPlan()
                    .getCycleCurriculumGroups()) {
                for (final Enrolment enrolment : cycleCurriculumGroup.getEnrolmentsBy(executionSemester)) {
                    result.add(enrolment);
                }
            }
        }
        return result;
    }
}
