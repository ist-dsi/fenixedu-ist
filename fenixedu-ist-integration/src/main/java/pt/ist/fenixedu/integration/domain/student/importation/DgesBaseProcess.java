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
package pt.ist.fenixedu.integration.domain.student.importation;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fenixedu.academic.domain.EntryPhase;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;

/**
 * 
 * @author naat
 * 
 */
public abstract class DgesBaseProcess extends DgesBaseProcess_Base {

    protected static final String ALAMEDA_UNIVERSITY = "A";
    protected static final String TAGUS_UNIVERSITY = "T";

    protected DgesBaseProcess() {
        super();
    }

    protected void init(final ExecutionYear executionYear, final EntryPhase entryPhase) {
        String[] args = new String[0];
        if (executionYear == null) {
            throw new DomainException("error.DgesBaseProcess.execution.year.is.null", args);
        }
        String[] args1 = new String[0];
        if (entryPhase == null) {
            throw new DomainException("error.DgesBaseProcess.entry.phase.is.null", args1);
        }

        setExecutionYear(executionYear);
        setEntryPhase(entryPhase);
    }

    protected List<DegreeCandidateDTO> parseDgesFile(byte[] contents, String university, EntryPhase entryPhase) {

        final List<DegreeCandidateDTO> result = new ArrayList<DegreeCandidateDTO>();
        String[] lines = readContent(contents);
        for (String dataLine : lines) {
            DegreeCandidateDTO dto = new DegreeCandidateDTO();
            if (dto.fillWithFileLineData(dataLine)) {
                result.add(dto);
            }
        }
        setConstantFields(university, entryPhase, result);
        return result;

    }

    private void setConstantFields(String university, EntryPhase entryPhase, final Collection<DegreeCandidateDTO> result) {
        for (final DegreeCandidateDTO degreeCandidateDTO : result) {
            degreeCandidateDTO.setIstUniversity(university);
            degreeCandidateDTO.setEntryPhase(entryPhase);
        }
    }

    public static String[] readContent(byte[] contents) {
        try {
            String fileContents = new String(contents, "UTF-8");
            return fileContents.split("\n");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
