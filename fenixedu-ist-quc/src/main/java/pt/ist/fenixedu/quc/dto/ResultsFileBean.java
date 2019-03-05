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
package pt.ist.fenixedu.quc.dto;

import java.io.InputStream;
import java.io.Serializable;

import org.joda.time.DateTime;

import pt.ist.fenixedu.quc.domain.ResultsImportationFile;
import pt.ist.fenixedu.quc.domain.ResultsImportationProcess;
import pt.ist.fenixframework.Atomic;

public class ResultsFileBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private transient InputStream inputStream;
    private DateTime resultsDate;
    private boolean newResults;

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setResultsDate(DateTime resultsDate) {
        this.resultsDate = resultsDate;
    }

    public DateTime getResultsDate() {
        return resultsDate;
    }

    public void setNewResults(boolean newResults) {
        this.newResults = newResults;
    }

    public boolean getNewResults() {
        return newResults;
    }

    @Atomic(mode = Atomic.TxMode.WRITE)
    public void createImportationProcess(byte[] content) {
        ResultsImportationFile resultsImportationFile = new ResultsImportationFile("filename", content);
        new ResultsImportationProcess(getResultsDate(), resultsImportationFile, getNewResults());
    }
}
