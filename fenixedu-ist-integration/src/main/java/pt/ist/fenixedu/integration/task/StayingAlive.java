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
package pt.ist.fenixedu.integration.task;

import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import pt.ist.fenixedu.integration.FenixEduIstIntegrationConfiguration;
import pt.ist.fenixframework.Atomic;

import java.io.File;
import java.io.FileWriter;

@Task(englishTitle = "Scheduler watchdog", readOnly = true)
public class StayingAlive extends CronTask {
    
    @Override
    protected Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }
    
    @Override
    public void runTask() throws Exception {
        String pathname = FenixEduIstIntegrationConfiguration.getConfiguration().getSchedulerWatchdogFilePath();
        File watchdog = new File(pathname);
        watchdog.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(watchdog);
        writer.write(String.valueOf(System.currentTimeMillis() / 1000L));
        writer.close();
    
    }


}
