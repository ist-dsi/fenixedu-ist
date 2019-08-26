package pt.ist.fenixedu.bullet.domain;

import com.google.common.base.Throwables;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.util.DiaSemana;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.YearMonthDay;
import org.joda.time.DateTimeConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.commons.lang.StringUtils;
import pt.ist.fenixframework.Atomic;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class BulletXmlConverter {
    public ExecutionSemester baseSemester;
    public Set<ExecutionCourse> executionCourses;

    public BulletXmlConverter(final ExecutionSemester executionSemester) {
        baseSemester = executionSemester;
        executionCourses = executionSemester.getAssociatedExecutionCoursesSet();
    }

    public byte[] toXML() {
        try
        {
            DocumentBuilderFactory docBFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBFactory.newDocumentBuilder();
            Document XMLDoc = docBuilder.newDocument();
            Element mainRootElement = XMLDoc.createElementNS("https://www.w3.org/2000/xmlns/","Schedule");
            XMLDoc.appendChild(mainRootElement);
            Element eventsRoot = XMLDoc.createElement("Events");
            mainRootElement.appendChild(eventsRoot);

            for (ExecutionCourse course : executionCourses) {
                writeShifts(XMLDoc, course, eventsRoot);
            }
            
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(XMLDoc);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(bos);
            transformer.transform(source, result);
            byte []array = bos.toByteArray();

            return array;

        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void writeShifts(Document XMLDoc, ExecutionCourse executionCourse, Element eventsRoot) {
        Set<Shift> courseShifts = executionCourse.getAssociatedShifts();

        for (Shift shift : courseShifts) {
            for(Lesson lesson : shift.getLessonsOrderedByWeekDayAndStartTime()) {
                writeLesson(XMLDoc, shift, lesson, eventsRoot);
            }
        }
    }

    private void writeLesson(Document XMLDoc, Shift shift, Lesson lesson, Element eventsRoot) {
        Element writtenLesson = XMLDoc.createElement("Event");
        writtenLesson.appendChild(createXMLNode(XMLDoc, "Name", shift.getNome() + " * " + lesson.prettyPrint()));
        writtenLesson.appendChild(createXMLNode(XMLDoc, "BTT_Id", generateID(lesson)));
        writtenLesson.appendChild(createXMLNode(XMLDoc, "SectionName", shift.getNome()));
        writtenLesson.appendChild(XMLDoc.createElement("SectionConnector"));
        Integer capacity = 0;
        if(lesson.getSala() != null) {
            capacity = lesson.getSala().getAllocatableCapacity();
        }
        writtenLesson.appendChild(createXMLNode(XMLDoc, "NumberStudents", capacity.toString()));
        writtenLesson.appendChild(createXMLNode(XMLDoc, "StartTime", lesson.getBeginHourMinuteSecond().toString()));
        writtenLesson.appendChild(createXMLNode(XMLDoc, "EndTime", lesson.getEndHourMinuteSecond().toString()));
        writtenLesson.appendChild(createXMLNode(XMLDoc, "Day", convertWeekDay(lesson.getDiaSemana())));
        writtenLesson.appendChild(createModuleNode(XMLDoc, lesson.getExecutionCourse()));
        writtenLesson.appendChild(createClassroomsNode(XMLDoc, lesson));
        writtenLesson.appendChild(createTeachersNode(XMLDoc, shift));
        writtenLesson.appendChild(createWeeksNode(XMLDoc, lesson));
        writtenLesson.appendChild(createTypologiesNode(XMLDoc, shift));
        writtenLesson.appendChild(createStudentGroupsNode(XMLDoc, shift));
        eventsRoot.appendChild(writtenLesson);
    }

    private Node createXMLNode(Document XMLDoc, String elementName, String elementValue) {
        Element node = XMLDoc.createElement(elementName);
        node.appendChild(XMLDoc.createTextNode(elementValue));
        return node;
    }

    private String generateID(Lesson lesson){
        return Integer.toString(BttObject.getBttId(lesson));
    }

    private String convertWeekDay(DiaSemana weekDay) {
        int result = weekDay.getDiaSemana() - 2;
        if(result == -1) {
            result = 6;
        }
        return Integer.toString(result);
    }

    private Node createModuleNode(Document XMLDoc, ExecutionCourse executionCourse) {
        Node moduleNode = XMLDoc.createElement("Module");
        moduleNode.appendChild(createXMLNode(XMLDoc, "Name", executionCourse.getName()));
        moduleNode.appendChild(createXMLNode(XMLDoc, "Acronym", executionCourse.getPrettyAcronym()));
        moduleNode.appendChild(createXMLNode(XMLDoc, "Code", executionCourse.getExternalId()));
        return moduleNode;
    }

    private Node createClassroomsNode(Document XMLDoc, Lesson lesson) {
        Node classroomsNode = XMLDoc.createElement("Classrooms");
        Node classroomNode = XMLDoc.createElement("Classroom");
        Node buildingNode = XMLDoc.createElement("Building");
        String roomName = "", buildingName = "";

        if(lesson.getSala() != null) {
            roomName = new BulletRoom(lesson.getSala()).roomFullPath();
            buildingName = roomName;
            /*Set<Space> buildings = lesson.getSala().getPath().stream().filter(space -> isBuilding(space)).collect(Collectors.toSet());
            if(buildings != null) {
                Iterator<Space> iterator = buildings.iterator();
                if(iterator.hasNext()) {
                    buildingName = iterator.next().getName();
                }
            }*/
        }
        buildingNode.appendChild(createXMLNode(XMLDoc, "Name", buildingName));
        classroomNode.appendChild(createXMLNode(XMLDoc, "Name", roomName));
        classroomNode.appendChild(buildingNode);
        classroomsNode.appendChild(classroomNode);
        return classroomsNode;
    }


    private boolean isBuilding(final Space space) {
        try {
            final String name = space.getClassification().getName().getContent();
            return "Building".equals(name) || "Edifício".equals(name);
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    private Node createTeachersNode(Document XMLDoc, Shift shift) {
        Node teachersNode = XMLDoc.createElement("Teachers");
        /*Set<Professorship> professorshipSet = shift.getExecutionCourse().getProfessorshipsSet();
        for (Professorship professorship : professorshipSet){
            String name = professorship.getTeacher().getPerson().getName();
            String code = professorship.getTeacher().getExternalId();
            String acronym = professorship.getTeacher().getPerson().getUsername();
            Node teacher = XMLDoc.createElement("Teacher");
            teacher.appendChild(createXMLNode(XMLDoc, "Name", name));
            teacher.appendChild(createXMLNode(XMLDoc, "Acronym", acronym));
            teacher.appendChild(createXMLNode(XMLDoc, "Code", code));
            teachersNode.appendChild(teacher);
        }*/
        return teachersNode;
    }

    private String getInitials(String name) {
        String[] names = name.split(" ");
        String initials = "";
        for (String item : names) {
            if(!item.trim().equals("")) {
                initials += item.trim().charAt(0);
            }
        }
        //remove accents
        initials = Normalizer.normalize(initials.toUpperCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");;
        return initials;
    }

    private Node createWeeksNode(Document XMLDoc, Lesson lesson) {
        Node weeksNode = XMLDoc.createElement("Weeks");
        List<String> weeks = new ArrayList<>();
        lesson.getAllLessonIntervals().stream()
                .map(interval -> interval.getStart().toLocalDate().withDayOfWeek(DateTimeConstants.MONDAY).toString())
                .sorted()
                .forEachOrdered(weeks::add);
        for(String week : weeks) {
            Node weekNode = XMLDoc.createElement("Week");
            weekNode.appendChild(createXMLNode(XMLDoc,"Date", week));
            weeksNode.appendChild(weekNode);
        }
        return weeksNode;
    }

    private Node createTypologiesNode(Document XMLDoc, Shift shift) {
        Node typologiesNode = XMLDoc.createElement("Typologies");
        for(String typology : shift.getSortedTypes().stream().map(x -> x.getSiglaTipoAula()).collect(Collectors.toList())) {
            Node typologyNode = XMLDoc.createElement("Typology");
            typologyNode.appendChild(createXMLNode(XMLDoc,"Name", typology));
            typologiesNode.appendChild(typologyNode);
        }
        return typologiesNode;
    }

    private Node createStudentGroupsNode(Document XMLDoc, Shift shift) {
        Node studentGroupsNode = XMLDoc.createElement("StudentGroups");
        for(SchoolClass schoolClass : shift.getAssociatedClassesSet()) {
            Node studentGroupNode = XMLDoc.createElement("StudentGroup");
            studentGroupNode.appendChild(createXMLNode(XMLDoc,"Name", schoolClass.getNome()));
            DegreeCurricularPlan curricularPlan = schoolClass.getExecutionDegree().getDegreeCurricularPlan();
            studentGroupNode.appendChild(createCurricularPlanNode(XMLDoc, curricularPlan, schoolClass.getAnoCurricular()));
            studentGroupsNode.appendChild(studentGroupNode);
        }
        return studentGroupsNode;
    }

    private Node createCurricularPlanNode(Document XMLDoc, DegreeCurricularPlan curricularPlan, Integer year) {
        Node curricularPlanNode = XMLDoc.createElement("CurricularPlan");
        curricularPlanNode.appendChild(createXMLNode(XMLDoc, "Name", curricularPlan.getPresentationName()));
        curricularPlanNode.appendChild(createXMLNode(XMLDoc, "Code", curricularPlan.getExternalId()));
        curricularPlanNode.appendChild(createXMLNode(XMLDoc, "Year", year.toString()));
        Node courseNode = XMLDoc.createElement("Course");
        courseNode.appendChild(createXMLNode(XMLDoc, "Name", curricularPlan.getDegree().getPresentationName()));
        courseNode.appendChild(createXMLNode(XMLDoc, "Acronym", curricularPlan.getDegree().getCode()));
        courseNode.appendChild(createXMLNode(XMLDoc, "Code", curricularPlan.getDegree().getExternalId()));
        curricularPlanNode.appendChild(courseNode);
        return curricularPlanNode;
    }
}
