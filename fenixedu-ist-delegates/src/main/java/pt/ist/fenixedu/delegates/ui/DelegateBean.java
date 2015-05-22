/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Delegates.
 *
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

import static org.fenixedu.bennu.FenixEduDelegatesConfiguration.BUNDLE;

import java.util.Comparator;
import java.util.Objects;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import pt.ist.fenixedu.delegates.domain.student.Delegate;

public class DelegateBean {

    public static Comparator<DelegateBean> COMPARATOR_BY_DEGREE_FUNTION_AND_INTERVAL = Comparator
            .<DelegateBean, Degree> comparing(b -> b.getDegree(), Degree.COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_ID.reversed())
            .thenComparing(b -> b.getCurricularYear(), Comparator.nullsLast(CurricularYear.CURRICULAR_YEAR_COMPARATORY_BY_YEAR))
            .thenComparing(b -> b.getDelegate().getStart()).thenComparing(b -> b.getDelegate().getExternalId());

    String name;
    String username;
    String email;
    String delegateTitle;
    String picture;
    CurricularYear curricularYear;
    CycleType cycleType;
    Degree degree;
    Delegate delegate;

    public DelegateBean() {

    }

    public DelegateBean(Delegate delegate) {
        this.name = delegate.getUser().getPerson().getName();
        this.username = delegate.getUser().getUsername();
        this.email = delegate.getUser().getPerson().getEmailForSendingEmails();
        this.delegateTitle = delegate.getTitle();
        this.picture = delegate.getUser().getProfile().getAvatarUrl();
        this.curricularYear = delegate.getCurricularYear();
        this.delegate = delegate;
        this.degree = delegate.getDegree();
        this.cycleType = delegate.getCycleType();
    }

    public DelegateBean(CycleType cycleType, CurricularYear curricularYear, Degree degree) {
        this.curricularYear = curricularYear;
        this.cycleType = cycleType;
        this.degree = degree;
    }

    public String getName() {
        return name;
    }

    private String optCurricularYearString(CurricularYear cy) {
        if (cy == null) {
            return "-";
        }
        return cy.toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDelegateOID() {
        if (delegate != null) {
            return delegate.getExternalId();
        }
        return "";
    }

    public String getInterval() {
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("dd/MM/yyyy");
        // Printing the date
        String theInterval = dtfOut.print(delegate.getStart());
        theInterval += " - ";
        theInterval += dtfOut.print(delegate.getEnd());
        return theInterval;
    }

    @Override
    public boolean equals(Object o) {
        DelegateBean obj = (DelegateBean) o;
        if (obj.getCurricularYear() != getCurricularYear()) {
            return false;
        }
        if (obj.getDegree() != getDegree()) {
            return false;
        }
        if (obj.getCycleType() != getCycleType()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(curricularYear) + Objects.hashCode(cycleType) + Objects.hashCode(degree);
    }

    public String getDelegateTitle() {
        if (delegateTitle != null) {
            return delegateTitle;
        }
        String delegate = BundleUtil.getString(BUNDLE, "delegate");
        String of = BundleUtil.getString(BUNDLE, "delegate.of");
        String year = BundleUtil.getString(BUNDLE, "delegate.year");
        if (cycleType != null) {
            return delegate + " " + of + " " + cycleType.getDescription();
        }
        if (curricularYear != null) {
            return delegate + " " + of + " " + curricularYear.getYear() + " " + year;
        }
        return delegate + " " + of + " " + degree.getDegreeType().getName().getContent();
    }

    public void setDelegateTitle(String delegateType) {
        this.delegateTitle = delegateType;
    }

    public String getPicture() {
        return picture;
    }

    public Degree getDegree() {
        return degree;
    }

    public void setDegree(Degree degree) {
        this.degree = degree;
    }

    public CurricularYear getCurricularYear() {
        return curricularYear;
    }

    public void setCurricularYear(CurricularYear curricularYear) {
        this.curricularYear = curricularYear;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public CycleType getCycleType() {
        return cycleType;
    }

    public void setCycleType(CycleType cycleType) {
        this.cycleType = cycleType;
    }

    public Delegate getDelegate() {
        return delegate;
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

}