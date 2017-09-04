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
package pt.ist.fenixedu.integration.task.updateData.enrolment;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.util.email.Message;
import org.fenixedu.academic.domain.util.email.Recipient;
import org.fenixedu.academic.domain.util.email.Sender;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.FenixFramework;

public class SendEmailToLowPerformanceStudents extends CustomTask {

    private final String SUBJECT = "Baixo rendimento académico";
    private final String BODY = "Caro aluno do TÉCNICO,\n" + 
            "\n" + 
            "Apesar de não constar da lista de prescrições em 2017/18, verificou-se \n" + 
            "que o seu rendimento académico tem sido claramente abaixo do esperado. \n" + 
            "Sabemos que vários são os motivos que podem ter condicionado o seu \n" + 
            "desempenho académico ao longo dos últimos anos. Provavelmente já terá \n" + 
            "tentado inverter esta situação, o Núcleo de Desenvolvimento Académico \n" + 
            "(NDA/GATu) disponibiliza-se a traçar consigo um plano específico e \n" + 
            "individualizado para melhorar o seu rendimento académico.\n" + 
            "\n" + 
            "Por forma a evitar a sua prescrição nos próximos anos é aconselhado a:\n" + 
            "\n" + 
            "·contactar o NDA/GATu para:\n" + 
            "\n" + 
            "   perceber as vantagens ou esclarecer dúvidas caso pretenda alterar a sua \n" + 
            "inscrição em 2017/18 para o regime de “tempo parcial”. Para mais \n" + 
            "informações sobre o Regime de Tempo Parcial consulte o Guia Académico em \n" + 
            "https://tecnico.ulisboa.pt/pt/recursos/documentos-importantes/\n" + 
            "\n" + 
            "   esclarecer qualquer questão que tenha relativa à Lei das Prescrições e \n" + 
            "às condições de exceção que evitaram a sua prescrição. Para mais \n" + 
            "informações sobre a Lei das Prescrições consulte a parte 2 do Guia \n" + 
            "Académico em https://tecnico.ulisboa.pt/pt/recursos/documentos-importantes/\n" + 
            "\n" + 
            "·Informar-se sobre o Workshop “Para Prescrever a Prescrição”, que \n" + 
            "decorrerá no mês de setembro.\n" + 
            "\n" + 
            "GATu: Há mais de 10 anos ao lado dos alunos a contribuir para a melhoria \n" + 
            "do rendimento académico!\n" + 
            "\n" + 
            "Com os melhores cumprimentos e votos de um bom ano escolar de 2017/18,\n" + 
            "\n" + 
            "Professora Fátima Montemor\n" + 
            "\n" + 
            "Conselho de Gestão do Instituto Superior Técnico,\n" + 
            "\n" + 
            "Assuntos Académicos";

    @Override
    public void runTask() throws Exception {
        User user = User.findByUsername("ist24616");
        Authenticate.mock(user);

        final String filename = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/lowPerformers.txt";
        Files.readAllLines(new File(filename).toPath())
            .forEach(line -> {
                Student student = Student.readStudentByNumber(Integer.valueOf(line));
                if (student == null) {
                    taskLog("Can't find student -> " + line);
                } else {
                    createEmail(student.getPerson());
                }
            });

        taskLog("Done.");
    }

    private Sender getConcelhoDeGestaoSender() {
        return FenixFramework.getDomainObject("4196183080395");
    }

    private void createEmail(final Person students) {
        final Sender sender = getConcelhoDeGestaoSender();
        final Set<Recipient> tos = new HashSet<Recipient>();
        tos.add(new Recipient(Group.users(students.getUser())));

        final Set<String> bccs = new HashSet<String>();
        bccs.add("marta.graca@ist.utl.pt");

        new Message(sender, null, tos, SUBJECT, BODY, bccs);
    }

}