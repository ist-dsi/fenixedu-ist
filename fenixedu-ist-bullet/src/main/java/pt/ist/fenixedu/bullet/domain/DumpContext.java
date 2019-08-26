package pt.ist.fenixedu.bullet.domain;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.common.util.XML;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularSemester;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.spreadsheet.SpreadsheetBuilder;
import org.fenixedu.commons.spreadsheet.WorkbookExportFormat;
import org.joda.time.DateTime;

import com.google.common.base.Throwables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

public class DumpContext {

    public ExecutionSemester baseSemester;
    public DateTime lessonsStart;
    public Set<CurricularSemester> semesters;
    public Set<ExecutionCourse> executions;
    public Set<CurricularCourse> curriculars;
    public Set<DegreeCurricularPlan> plans;
    public Set<Degree> degrees;
    public int courseImportance = 1, acceptanceMarginPercent = 10, classHoursLimit = 8, consecutiveClassHoursLimit = 5;
    public BigDecimal slotsPerHour = new BigDecimal(2), minClassSlots = slotsPerHour, maxClassSlots = slotsPerHour.multiply(new BigDecimal(consecutiveClassHoursLimit));
    private Map<String, Set<? extends BulletObject>> collected;

    public Set<BulletZone> zones;

    public DumpContext(final ExecutionSemester executionSemester) {
        baseSemester = executionSemester;
        lessonsStart = baseSemester.getLessonsPeriod().getPeriodInterval().getStart();
        semesters = Bennu.getInstance().getCurricularSemestersSet().stream()
                .filter(cs -> baseSemester.getSemester().equals(cs.getSemester())).collect(Collectors.toSet());
        executions = Bennu.getInstance().getExecutionCoursesSet().stream()
                .filter(ec -> baseSemester.equals(ec.getExecutionPeriod()))
                .collect(Collectors.toSet());
        curriculars = executions.stream()
                .flatMap(ec -> ec.getAssociatedCurricularCoursesSet().stream())
                .filter(cc -> cc.isActive(baseSemester)).collect(Collectors.toSet());
        plans = curriculars.stream()
                .map(CurricularCourse::getDegreeCurricularPlan).collect(Collectors.toSet());
        degrees = plans.stream()
                .map(DegreeCurricularPlan::getDegree).collect(Collectors.toSet());
        collected = new HashMap<>();
    }

    <T extends BulletObject> Set<T> all(final Class<T> clazz) {
        return (Set) collected.computeIfAbsent(clazz.getCanonicalName(), k -> reflectAll(clazz));
    }

    private <T extends BulletObject> Set<T> reflectAll(final Class<T> clazz) {
        try {
            return ((Stream<T>) clazz.getMethod("all", DumpContext.class).invoke(null, this)).collect(Collectors.toSet());
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public JsonObject toJson() {
        final JsonObject result = new JsonObject();

        Stream.of(BulletObjectTag.values())
            .filter(tag -> tag.entity() != null)
            .forEach(tag -> {
                result.add(tag.group(), all(tag.entity()).stream().map(bo -> bo.toJson(this)).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
            });

        return result;
    }

    public byte[] toXlsx(final BulletObjectType type) {
        final SpreadsheetBuilder builder = new SpreadsheetBuilder();
        
        Stream.of(BulletObjectTag.values())
            .filter(tag -> tag.entity() != null)
            .forEach(tag -> {
                builder.addSheet(tag.group(), new BulletSheetDataBuilder(this).bulletSheetData(all(tag.entity())));
            });

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            builder.build(WorkbookExportFormat.EXCEL, os);
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
        return os.toByteArray();
    }

    /*
    public String toXMLString() {
        try
        {

            System.out.println("oi\n" + this);
            final JAXBContext jaxbContext = JAXBContext.newInstance(DumpContext.class);
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.w3.org/2001/XMLSchema-instance");
            final StringWriter sw = new StringWriter();

            marshaller.marshal(this, sw);
            String xmlString = sw.toString();

            return xmlString;

        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public byte[] toXML() {
        try
        {
            DocumentBuilderFactory docBFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBFactory.newDocumentBuilder();
            Document XMLdoc = docBuilder.newDocument();
            Element mainRootElement = XMLdoc.createElementNS("https://www.w3.org/2000/xmlns/","Schedule");
            XMLdoc.appendChild(mainRootElement);
            Element eventsRoot = XMLdoc.createElement("Events");
            mainRootElement.appendChild(eventsRoot);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(XMLdoc);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(bos);
            transformer.transform(source, result);
            byte []array = bos.toByteArray();

            return array;

        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

     */
}
