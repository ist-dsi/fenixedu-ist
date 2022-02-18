package pt.ist.fenixedu.integration.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.phd.*;
import org.fenixedu.academic.domain.phd.thesis.PhdThesisProcess;
import org.fenixedu.academic.domain.thesis.Thesis;
import org.fenixedu.bennu.FenixEduIstIntegrationConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.io.servlet.FileDownloadServlet;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;


@Path("/_internal/scholar/theses")
public class FenixScholarThesesApi {

    private String calculateFosFromDepartament(String departmentName) {
        switch (departmentName) {
            case "Departamento de Engenharia Civil, Arquitectura e Georrecursos (DECivil)": return "civil-engineering";
            case "Departamento de Física (DF)": return "physical-sciences";
            case "Departamento de Engenharia Informática (DEI)": return "computer-and-information-sciences";
            case "Departamento de Engenharia e Gestão (DEG)": return "mechanical-engineering";
            case "Departamento de Engenharia de Materiais (DEMAT)": return "materials-engineering";
            case "Departamento de Engenharia de Minas e Georrecursos (DEMG)": return "environmental-engineering";
            case "Departamento de Engenharia Mecânica (DEM)": return "mechanical-engineering";
            case "Departamento de Engenharia Electrotécnica e de Computadores (DEEC)": return "electrical-engineering-electronic-engineering-information-engineering";
            case "Departamento de Matemática (DM)": return "mathematics";
            case "Departamento de Engenharia e Ciências Nucleares (DECN)": return "physical-sciences";
            case "Secção Autónoma de Engenharia Naval (SAEN)": return "other-engineering-and-technologies";
            case "Departamento de Bioengenharia (DBE)": return "other-engineering-and-technologies";
            case "Departamento de Engenharia Química (DEQ)": return "chemical-engineering";
        }
        return null;
    }

    @GET
    @Produces(FenixAPIv1.JSON_UTF8)
    public String list(@HeaderParam("Authorization") String authorization) {
        String token = authorization.substring(7);
        if(FenixEduIstIntegrationConfiguration.getConfiguration().scholarThesesToken().equals(token)) {
            JsonArray infos = new JsonArray();

            for (PhdIndividualProgramProcessNumber phdProcessNumber : Bennu.getInstance().getPhdIndividualProcessNumbersSet()) {
                PhdIndividualProgramProcess phdProcess = phdProcessNumber.getProcess();
                if (phdProcess.isConcluded()) {
                    final PhdThesisProcess process = phdProcess.getThesisProcess();
                    PhdProgramProcessDocument document = process.getFinalThesisDocument();
                    if(document != null) {
                        JsonObject phdInfo = new JsonObject();
                        phdInfo.addProperty("id", phdProcess.getExternalId());
                        phdInfo.addProperty("author", phdProcess.getPerson().getUsername());
                        phdInfo.addProperty("title", phdProcess.getThesisTitle());

                        JsonArray schools = new JsonArray();
                        switch (phdProcess.getCollaborationType()) {
                            case NONE:
                            case WITH_SUPERVISION:
                            case ERASMUS_MUNDUS:
                            case OTHER:
                                schools.add(new JsonPrimitive(Unit.getInstitutionName().getContent()));
                                break;
                            default:
                                schools.add(new JsonPrimitive(Unit.getInstitutionName().getContent()));
                                schools.add(new JsonPrimitive(phdProcess.getCollaborationType().getLocalizedName()));
                        }
                        phdInfo.add("schools", schools);

                        // phdInfo.addProperty("language", language);
                        JsonObject dateIssued = new JsonObject();
                        dateIssued.addProperty("year",  phdProcess.getConclusionDate().year().getAsShortText());
                        dateIssued.addProperty("month",  phdProcess.getConclusionDate().monthOfYear().getAsShortText());
                        phdInfo.add("dateIssued", dateIssued);
                        JsonObject dateSubmission = new JsonObject();
                        phdInfo.add("dateSubmission", dateSubmission);
                        phdInfo.addProperty("url", FileDownloadServlet.getDownloadUrl(document));
                        phdInfo.addProperty("type", "phdthesis");
                        infos.add(phdInfo);
                    }
                }

            }

            for (Thesis t : Bennu.getInstance().getThesesSet()) {
                if (t.isEvaluated()) {
                    JsonObject mscInfo = new JsonObject();
                    // ['advisors', 'description', 'tid', 'subjectFos', 'language']
                    JsonArray advisors = new JsonArray();
                    for(Person advisor :t.getOrientationPersons()){
                        advisors.add(advisor.getUsername());
                    }
                    mscInfo.add("advisors", advisors);
                    String language = t.getLanguage() != null ? t.getLanguage().toLanguageTag(): "REDO";
                    mscInfo.addProperty("language", language);
                    mscInfo.addProperty("id", t.getExternalId());
                    mscInfo.addProperty("author", t.getStudent().getPerson().getUsername());
                    if(t.getDepartmentName() != null) {
                        mscInfo.addProperty("subjectFos", calculateFosFromDepartament(t.getDepartmentName()));
                    }


                    String title = t.getFinalFullTitle().getContent(org.fenixedu.academic.util.LocaleUtils.EN);
                    if (title == null) {
                        title = t.getFinalFullTitle().getContent(org.fenixedu.academic.util.LocaleUtils.PT);
                    }
                    mscInfo.addProperty("title", title);
                    String abstractText = t.getThesisAbstract().getContent((org.fenixedu.academic.util.LocaleUtils.EN));
                    if (abstractText == null) {
                        abstractText = t.getThesisAbstract().getContent(org.fenixedu.academic.util.LocaleUtils.PT);
                    }
                    mscInfo.addProperty("description", abstractText);

                    JsonObject dateIssued = new JsonObject();
                    dateIssued.addProperty("year", t.getDiscussed().year().getAsShortText());
                    dateIssued.addProperty("month", t.getDiscussed().monthOfYear().getAsShortText());
                    mscInfo.add("dateIssued", dateIssued);
                    JsonObject dateSubmission = new JsonObject();
                    dateSubmission.addProperty("year", t.getSubmission().year().getAsShortText());
                    dateSubmission.addProperty("month", t.getSubmission().monthOfYear().getAsShortText());
                    mscInfo.add("dateSubmission", dateSubmission);

                    JsonArray schools = new JsonArray();
                    schools.add(new JsonPrimitive(Unit.getInstitutionName().getContent()));
                    mscInfo.add("schools", schools);

                    mscInfo.addProperty("url", FileDownloadServlet.getDownloadUrl(t.getDissertation()));
                    mscInfo.addProperty("type", "mastersthesis");
                    JsonArray conditions = new JsonArray();
                    t.getGeneralConditions().stream().forEach(tc -> {
                        conditions.add(tc.getKey());
                    });
                    mscInfo.add("conditions", conditions);
                    infos.add(mscInfo);
                }
            }
            return infos.toString();
        }
        return "no";
	}
}
