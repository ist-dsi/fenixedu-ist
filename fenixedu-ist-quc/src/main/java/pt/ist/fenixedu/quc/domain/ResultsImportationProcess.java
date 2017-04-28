package pt.ist.fenixedu.quc.domain;

import java.io.InputStreamReader;

import org.fenixedu.academic.domain.QueueJobResult;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.joda.time.DateTime;

import com.google.common.io.CharStreams;

import pt.ist.fenixedu.quc.domain.exceptions.FenixEduQucDomainException;
import pt.ist.fenixframework.FenixFramework;

public class ResultsImportationProcess extends ResultsImportationProcess_Base {
    
    public static final String QUC_RESULTS_IMPORTED = "quc.results.created";
    
    public ResultsImportationProcess(DateTime resultsDate, ResultsImportationFile importationFile, boolean newResults) {
        super();
        checkParameters(resultsDate, importationFile);
        setNewResults(newResults);
        setResultsDate(resultsDate);
        setResultsImportationFile(importationFile);
    }

    private void checkParameters(DateTime resultsDate, ResultsImportationFile importationFile) {
        if (resultsDate == null) {
            throw FenixEduQucDomainException.importationResultDateEmpty();
        }
        if (importationFile == null) {
            throw FenixEduQucDomainException.importationResultFileEmpty();
        }
    }

    @Override
    public QueueJobResult execute() throws Exception {
        String stringResults = CharStreams.toString(new InputStreamReader(getResultsImportationFile().getStream()));
        if (getNewResults()) {
            InquiryResult.importResults(stringResults, getResultsDate());
        } else {
            InquiryResult.updateRows(stringResults, getResultsDate());
        }
        Signal.emit(QUC_RESULTS_IMPORTED,new DomainObjectEvent<>(null));
        
        return new QueueJobResult();
    }
}
