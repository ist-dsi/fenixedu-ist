package org.fenixedu.ulisboa.integration.sas.dto;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

public class ActiveStudentBean implements Serializable {

    private static final long serialVersionUID = -9131181899894720285L;

    String name;
    String gender;
    String cardNumber;
    String mifare;
    String cardIssueDate;
    String isTemporaryCard;
    String identificationNumber;
    String fiscalCountryCode;
    String fiscalIdentificationNumber;
    String dateOfBirth;
    String studentCode;
    String degreeCode;
    String currentExecutionYear;
    String previousExecutionYear;
    String enroledECTTotal;
    String enroledECTTotalInPreviousYear;
    String approvedECTTotalInPreviousYear;
    String originCountryCode;
    String originCountry;
    String dateOfRegistration;
    String oficialDegreeCode;
    String curricularYear;
    String regime;
    // Due to shared degrees, a student may be frequenting a school, but the payment is performed in another school
    Boolean isPayingSchool;

    public Boolean getIsPayingSchool() {
        return isPayingSchool != null ? isPayingSchool : false;
    }

    public void setIsPayingSchool(Boolean isPayingSchool) {
        this.isPayingSchool = isPayingSchool;
    }

    public String getCurrentExecutionYear() {
        return currentExecutionYear != null ? currentExecutionYear : "";
    }

    public void setCurrentExecutionYear(String currentExecutionYear) {
        this.currentExecutionYear = currentExecutionYear;
    }

    public String getPreviousExecutionYear() {
        return previousExecutionYear != null ? previousExecutionYear : "";
    }

    public void setPreviousExecutionYear(String previousExecutionYear) {
        this.previousExecutionYear = previousExecutionYear;
    }

    public String getCurricularYear() {
        return curricularYear != null ? curricularYear : "";
    }

    public void setCurricularYear(String curricularYear) {
        this.curricularYear = curricularYear;
    }

    public String getRegime() {
        return regime != null ? regime : "";
    }

    public void setRegime(String regime) {
        this.regime = regime;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender != null ? gender : "";
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMifare() {
        return mifare != null ? mifare : "";
    }

    public void setMifare(String mifare) {
        this.mifare = mifare;
    }

    public String getIdentificationNumber() {
        return identificationNumber != null ? identificationNumber : "";
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public String getFiscalCountryCode() {
        return fiscalCountryCode;
    }
    
    public void setFiscalCountryCode(String fiscalCountryCode) {
        this.fiscalCountryCode = fiscalCountryCode;
    }

    public String getFiscalIdentificationNumber() {
        //Students without a fiscal identification number should use the default 999999990
        return !StringUtils.isEmpty(fiscalIdentificationNumber) ? fiscalIdentificationNumber : "999999990";
    }

    public void setFiscalIdentificationNumber(String fiscalIdentificationNumber) {
        this.fiscalIdentificationNumber = fiscalIdentificationNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth != null ? dateOfBirth : dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getStudentCode() {
        return studentCode != null ? studentCode : "";
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getDegreeCode() {
        return degreeCode != null ? degreeCode : "";
    }

    public void setDegreeCode(String degreeCode) {
        this.degreeCode = degreeCode;
    }

    public String getEnroledECTTotal() {
        return enroledECTTotal != null ? enroledECTTotal : "";
    }

    public void setEnroledECTTotal(String enroledECTTotal) {
        this.enroledECTTotal = enroledECTTotal;
    }

    public String getEnroledECTTotalInPreviousYear() {
        return enroledECTTotalInPreviousYear != null ? enroledECTTotalInPreviousYear : "";
    }

    public void setEnroledECTTotalInPreviousYear(String enroledECTTotalInPreviousYear) {
        this.enroledECTTotalInPreviousYear = enroledECTTotalInPreviousYear;
    }

    public String getApprovedECTTotalInPreviousYear() {
        return approvedECTTotalInPreviousYear != null ? approvedECTTotalInPreviousYear : "";
    }

    public void setApprovedECTTotalInPreviousYear(String approvedECTTotalInPreviousYear) {
        this.approvedECTTotalInPreviousYear = approvedECTTotalInPreviousYear;
    }
    
    public String getOriginCountryCode() {
        return originCountryCode;
    }
    
    public void setOriginCountryCode(String originCountryCode) {
        this.originCountryCode = originCountryCode;
    }

    public String getOriginCountry() {
        return originCountry != null ? originCountry : "";
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getDateOfRegistration() {
        return dateOfRegistration != null ? dateOfRegistration : "";
    }

    public void setDateOfRegistration(String dateOfRegistration) {
        this.dateOfRegistration = dateOfRegistration;
    }

    public String getOficialDegreeCode() {
        return oficialDegreeCode != null ? oficialDegreeCode : "";
    }

    public void setOficialDegreeCode(String oficialDegreeCode) {
        this.oficialDegreeCode = oficialDegreeCode;
    }

    public String getCardIssueDate() {
        return cardIssueDate != null ? cardIssueDate : "";
    }

    public void setCardIssueDate(String cardIssueDate) {
        this.cardIssueDate = cardIssueDate;
    }

    public String getIsTemporaryCard() {
        return isTemporaryCard != null ? isTemporaryCard : "";
    }

    public void setIsTemporaryCard(String isTemporaryCard) {
        this.isTemporaryCard = isTemporaryCard;
    }

    public String getCardNumber() {
        return cardNumber != null ? cardNumber : "";
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}