package pt.ist.fenixedu.integration.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.phd.*;
import org.fenixedu.academic.domain.phd.thesis.PhdThesisProcess;
import org.fenixedu.academic.domain.thesis.Thesis;
import org.fenixedu.academic.domain.thesis.ThesisEvaluationParticipant;
import org.fenixedu.academic.domain.thesis.ThesisFile;
import org.fenixedu.bennu.FenixEduIstIntegrationConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

@Path("/_internal/scholar/theses")
public class FenixScholarThesesApi {

    private final String FENIX_PDF = "fenix.pdf";

    private String calculateFosFromPhdProgram(String phdProgram) {
        switch (phdProgram) {
            case "Engenharia de Materiais": return "materials-engineering";
            case "Engenharia e Gestão": return "mechanical-engineering";
            case "Engenharia e Políticas Públicas": return "other-engineering-and-technologies";
            case "Restauro e Gestão Fluviais": return "earth-and-related-environmental-sciences";
            case "Engenharia Computacional": return "computer-and-information-sciences";
            case "Bioengenharia": return "other-engineering-and-technologies";
            case "Materiais e Processamento Avançados": return "materials-engineering";
            case "Engenharia de Sistemas": return "other-engineering-and-technologies";
            case "Engenharia Mecânica": return "mechanical-engineering";
            case "Sistemas de Transportes": return "other-engineering-and-technologies";
            case "Engenharia Electrotécnica e de Computadores": return "electrical-engineering-electronic-engineering-information-engineering";
            case "Engenharia do Ambiente": return "environmental-engineering";
            case "Engenharia de Petróleos": return "chemical-engineering";
            case "Química": return "chemical-sciences";
            case "Engenharia Química": return "chemical-engineering";
            case "Biotecnologia e Biociências": return "biological-sciences";
            case "Engenharia de Minas": return "other-engineering-and-technologies";
            case "Planeamento Regional e Urbano": return "earth-and-related-environmental-sciences";
            case "Engenharia e Gestão Industrial": return "mechanical-engineering";
            case "Segurança de Informação": return "computer-and-information-sciences";
            case "Engenharia Física": return "physical-sciences";
            case "Engenharia Informática e de Computadores": return "computer-and-information-sciences";
            case "Engenharia Física Tecnológica": return "physical-sciences";
            case "Georrecursos": return "earth-and-related-environmental-sciences";
            case "Engenharia Metalúrgica e de Materiais": return "materials-engineering";
            case "Engenharia Aeroespacial": return "mechanical-engineering";
            case "Física": return "physical-sciences";
            case "Matemática": return "mathematics";
            case "Engenharia do Território": return "other-engineering-and-technologies";
            case "Engenharia da Refinação, Petroquímica e Química": return "chemical-engineering";
            case "Mudança Tecnológica e Empreendedorismo": return "economics-and-business";
            case "Engenharia Naval e Oceânica": return "other-engineering-and-technologies";
            case "Ciências de Engenharia": return "other-engineering-and-technologies";
            case "Engenharia Biomédica": return "medical-engineering";
            case "Engenharia Civil": return "civil-engineering";
            case "Arquitectura": return "civil-engineering";
            case "Estatística e Processos Estocásticos": return "mathematics";
            case "Transportes": return "other-engineering-and-technologies";
            case "Líderes para Indústrias Tecnológicas": return "economics-and-business";
            case "Alterações Climáticas e Políticas de Desenvolvimento Sustentável": return "earth-and-related-environmental-sciences";
            case "Sistemas  Sustentáveis de Energia": return "other-engineering-and-technologies";
        }
        return null;
    }

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
        String token = authorization != null ? authorization.substring(7):"";
        if(FenixEduIstIntegrationConfiguration.getConfiguration().scholarThesesToken().equals(token)) {
            JsonArray infos = new JsonArray();

            for (PhdIndividualProgramProcessNumber phdProcessNumber : Bennu.getInstance().getPhdIndividualProcessNumbersSet()) {
                PhdIndividualProgramProcess phdProcess = phdProcessNumber.getProcess();
                if (phdProcess.isConcluded()) {
                    final PhdThesisProcess process = phdProcess.getThesisProcess();
                    PhdProgramProcessDocument document = process.getFinalThesisDocument();
                    if(document != null) {
                        JsonObject phdInfo = new JsonObject();
                        // [ 'description', 'tid']
                        phdInfo.addProperty("id", phdProcess.getExternalId());
                        JsonObject author = new JsonObject();
                        author.addProperty("name", phdProcess.getPerson().getName());
                        author.addProperty("userId", phdProcess.getPerson().getUsername());
                        phdInfo.add("author", author);
                        phdInfo.addProperty("title", phdProcess.getThesisTitle());
                        JsonArray advisors = new JsonArray();
                        for(PhdParticipant participant: process.getIndividualProgramProcess().getGuidingsSet()) {
                            JsonObject advisor = new JsonObject();
                            advisor.addProperty("name", participant.getName());
                            if(participant instanceof InternalPhdParticipant) {
                                InternalPhdParticipant internalParticipant = (InternalPhdParticipant) participant;
                                advisor.addProperty("userId", internalParticipant.getPerson().getUsername());
                            }
                            advisors.add(advisor);
                        }
                        phdInfo.addProperty("subjectFos", phdProcess.getPhdProgram().getName().getContent());
                        phdInfo.add("advisors", advisors);
                        // No info about language, defaults to english
                        String language = "en";
                        phdInfo.addProperty("language", language);
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

                        if(phdProcess.getPhdProgram() != null && phdProcess.getPhdProgram().getName() != null) {
                            phdInfo.addProperty("subjectFos", calculateFosFromPhdProgram(phdProcess.getPhdProgram().getName().getContent()));
                        }
                        JsonObject dateIssued = new JsonObject();
                        dateIssued.addProperty("year",  phdProcess.getConclusionDate().year().getAsShortText());
                        dateIssued.addProperty("month",  phdProcess.getConclusionDate().monthOfYear().getAsShortText());
                        phdInfo.add("dateIssued", dateIssued);
                        if(process.getDiscussionDate() != null) {
                            JsonObject dateSubmission = new JsonObject();
                            dateSubmission.addProperty("year",  process.getDiscussionDate().year().getAsShortText());
                            dateSubmission.addProperty("month",  process.getDiscussionDate().monthOfYear().getAsShortText());
                            phdInfo.add("dateSubmission", dateSubmission);
                        }

                        phdInfo.addProperty("fileId", document.getExternalId());
                        phdInfo.addProperty("type", "phdthesis");
                        infos.add(phdInfo);
                    }
                }

            }

            for (Thesis t : Bennu.getInstance().getThesesSet()) {
                if (t.isEvaluated()) {
                    JsonObject mscInfo = new JsonObject();
                    // Missing 'tid'
                    JsonArray advisors = new JsonArray();
                    for(ThesisEvaluationParticipant participant: t.getOrientation()) {
                        JsonObject advisor = new JsonObject();
                        advisor.addProperty("name", participant.getName());
                        if(participant.getPerson() != null) {
                            advisor.addProperty("userId", participant.getPerson().getUsername());
                        }
                        advisors.add(advisor);
                    }
                    mscInfo.add("advisors", advisors);
                    String language;
                    if(t.getLanguage() == null) {
                        language = "en";
                    } else if(t.getLanguage().toLanguageTag().equals("en-GB")) {
                        language = "en";
                    } else if(t.getLanguage().toLanguageTag().equals("pt-PT")) {
                        language = "pt";
                    } else {
                        language = "en";
                    }
                    mscInfo.addProperty("language", language);
                    mscInfo.addProperty("id", t.getExternalId());

                    JsonObject author = new JsonObject();
                    author.addProperty("name", t.getStudent().getPerson().getName());
                    author.addProperty("userId", t.getStudent().getPerson().getUsername());
                    mscInfo.add("author", author);
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

                    mscInfo.addProperty("fileId", t.getDissertation().getExternalId());
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
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
	}

    @GET
    @Path("{id}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response get(@HeaderParam("Authorization") String authorization, @PathParam("id") String id) throws IOException {
        String token = authorization != null ? authorization.substring(7):"";
        if(FenixEduIstIntegrationConfiguration.getConfiguration().scholarThesesToken().equals(token)) {
            DomainObject object = FenixFramework.getDomainObject(id);
            String fileName;
            InputStream stream;
            if(object instanceof ThesisFile) {
                ThesisFile thesisFile = (ThesisFile) object;
                fileName = thesisFile.getDisplayName();
                stream = CoreConfiguration.getConfiguration().developmentMode() ? devPdf() : thesisFile.getStream();
            } else if(object instanceof PhdProgramProcessDocument) {
                PhdProgramProcessDocument thesisFile = (PhdProgramProcessDocument) object;
                fileName = thesisFile.getDisplayName();
                stream = CoreConfiguration.getConfiguration().developmentMode() ? devPdf() : thesisFile.getStream();
            } else {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();
        }
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }


    private InputStream devPdf() {
        return this.getClass().getClassLoader().getResourceAsStream(FENIX_PDF);
    }


}
