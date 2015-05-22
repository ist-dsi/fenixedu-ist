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
package pt.ist.fenixedu.tutorship.ui.Action.pedagogicalCouncil.studentLowPerformance;

import java.io.Serializable;
import java.math.BigDecimal;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.Student;

public class StudentLowPerformanceBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private Student student;
    private BigDecimal sumEcts;
    private Degree degree;
    private int numberOfEntriesStudentInSecretary;
    private String email;
    private String regime;
    private String registrationStart;
    private IngressionType ingressionType;

    public StudentLowPerformanceBean(Student student, BigDecimal sumEcts, Degree degree, int numberOfEntriesStudentInSecretary,
            String email, String regime, String registrationStart, IngressionType ingressionType) {
        super();
        this.student = student;
        this.sumEcts = sumEcts;
        this.degree = degree;
        this.numberOfEntriesStudentInSecretary = numberOfEntriesStudentInSecretary;
        this.email = email;
        this.regime = regime;
        this.registrationStart = registrationStart;
        this.ingressionType = ingressionType;
    }

    public void setSumEcts(BigDecimal sumEcts) {
        this.sumEcts = sumEcts;
    }

    public BigDecimal getSumEcts() {
        return sumEcts;
    }

    public void setDegree(Degree degree) {
        this.degree = degree;
    }

    public Degree getDegree() {
        return degree;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Student getStudent() {
        return student;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public String getRegime() {
        return regime;
    }

    public void setRegime(String regime) {
        this.regime = regime;
    }

    public int getNumberOfEntriesStudentInSecretary() {
        return numberOfEntriesStudentInSecretary;
    }

    public void setNumberOfEntriesStudentInSecretary(int numberOfEntries) {
        this.numberOfEntriesStudentInSecretary = numberOfEntries;
    }

    public String getRegistrationStart() {
        return registrationStart;
    }

    public void setRegistrationStart(String registrationStart) {
        this.registrationStart = registrationStart;
    }

    public IngressionType getIngressionType() {
        return ingressionType;
    }

    public void setIngressionType(IngressionType ingressionType) {
        this.ingressionType = ingressionType;
    }
}