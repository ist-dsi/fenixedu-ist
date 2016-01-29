/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Report Templates.
 *
 * FenixEdu IST Report Templates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Report Templates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Report Templates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenix.jasper.reports;

import net.sf.jasperreports.engine.fill.JRBaseFiller;
import net.sf.jasperreports.engine.fill.JRFillSubreport;
import net.sf.jasperreports.engine.fill.JRSubreportRunner;
import net.sf.jasperreports.engine.fill.JRSubreportRunnerFactory;

public class JRTxThreadSubreportRunnerFactory implements JRSubreportRunnerFactory {

    @Override
    public JRSubreportRunner createSubreportRunner(JRFillSubreport fillSubreport, JRBaseFiller subreportFiller) {
        return new JRTxThreadSubreportRunner(fillSubreport, subreportFiller);
    }

}
