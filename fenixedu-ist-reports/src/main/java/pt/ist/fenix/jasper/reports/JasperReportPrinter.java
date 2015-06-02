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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.print.attribute.PrintRequestAttributeSet;

import net.sf.jasperreports.engine.JRAbstractExporter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.FontKey;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;
import net.sf.jasperreports.engine.export.PdfFont;
import net.sf.jasperreports.engine.fill.JRSubreportRunnerFactory;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRProperties;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.util.report.ReportPrinter;
import org.fenixedu.academic.util.report.ReportsUtils;

import com.lowagie.text.pdf.BaseFont;

public class JasperReportPrinter implements ReportPrinter {

    private final Map<String, JasperReport> reportsMap = new ConcurrentHashMap<String, JasperReport>();

    private final Properties properties = new Properties();

    public JasperReportPrinter() {
        JRProperties.setProperty(JRSubreportRunnerFactory.SUBREPORT_RUNNER_FACTORY,
                JRTxThreadSubreportRunnerFactory.class.getName());
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("reports.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load properties files.", e);
        }
    }

    @Override
    public ReportResult printReports(ReportDescription... reports) throws Exception {
        final List<JasperPrint> partials = new ArrayList<JasperPrint>();

        for (final ReportDescription report : reports) {
            JasperPrint jasperPrint = createJasperPrint(report.getKey(), report.getParameters(), report.getDataSource());

            if (jasperPrint == null) {
                throw new NullPointerException();
            } else {
                // HACK!
                if (report.getKey().equals("org.fenixedu.academic.report.thesis.StudentThesisIdentificationDocument")) {
                    jasperPrint = processStudentThesisIdentificationDocument(jasperPrint);
                }
            }

            partials.add(jasperPrint);
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        export(new JRPdfExporter(), partials, baos, (PrintRequestAttributeSet) null);
        return new ReportResult(baos.toByteArray(), "application/pdf", "pdf");
    }

    @Override
    public ReportResult printReport(String key, Map<String, Object> parameters, Collection<?> dataSource) throws Exception {
        final JasperPrint jasperPrint = createJasperPrint(key, parameters, dataSource);

        if (jasperPrint != null) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            export(new JRPdfExporter(), Collections.singletonList(jasperPrint), baos, (PrintRequestAttributeSet) null);
            return new ReportResult(baos.toByteArray(), "application/pdf", "pdf");
        }

        return null;
    }

    private static final String[][] FIELD_LINE_MAP = { { "textField-title", "13", "line-title-2" },
            { "textField-subtitle", "13", "line-subtitle-2" } };

    private static final float LINE_DISTANCE = 11.5f;

    public JasperPrint processStudentThesisIdentificationDocument(JasperPrint jasperPrint) {
        Map<String, JRPrintElement> map = getElementsMap(jasperPrint);

        for (String[] element2 : FIELD_LINE_MAP) {
            String elementKey = element2[0];
            Integer height = new Integer(element2[1]);

            JRPrintElement element = map.get(elementKey);

            if (element == null) {
                continue;
            }

            if (element.getHeight() > height.intValue()) {
                // height increased
                for (int j = 2; j < element2.length; j++) {
                    JRPrintElement line = map.get(element2[j]);

                    if (line != null) {
                        line.setY(element.getY() + ((int) (j * LINE_DISTANCE)));
                    }
                }
            } else {
                // height is the same
                for (int j = 2; j < element2.length; j++) {
                    JRPrintElement line = map.get(element2[j]);

                    if (line != null) {
                        removeElement(jasperPrint, line);
                    }
                }
            }
        }

        return jasperPrint;
    }

    private Map<String, JRPrintElement> getElementsMap(JasperPrint jasperPrint) {
        Map<String, JRPrintElement> map = new HashMap<String, JRPrintElement>();

        Iterator pages = jasperPrint.getPages().iterator();
        while (pages.hasNext()) {
            JRPrintPage page = (JRPrintPage) pages.next();

            Iterator elements = page.getElements().iterator();
            while (elements.hasNext()) {
                JRPrintElement element = (JRPrintElement) elements.next();

                map.put(element.getKey(), element);
            }
        }

        return map;
    }

    private void removeElement(JasperPrint jasperPrint, JRPrintElement target) {
        Iterator pages = jasperPrint.getPages().iterator();
        while (pages.hasNext()) {
            JRPrintPage page = (JRPrintPage) pages.next();

            Iterator elements = page.getElements().iterator();
            while (elements.hasNext()) {
                JRPrintElement element = (JRPrintElement) elements.next();

                if (element == target) {
                    elements.remove();
                }
            }
        }
    }

    private void export(final JRAbstractExporter exporter, final List<JasperPrint> prints, final ByteArrayOutputStream stream,
            final PrintRequestAttributeSet printRequestAttributeSet) throws JRException {
        exporter.setParameter(JRExporterParameter.FONT_MAP, createFontMap());

        if (prints.size() == 1) {
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, prints.iterator().next());
        } else {
            exporter.setParameter(JRExporterParameter.JASPER_PRINT_LIST, prints);
        }

        if (stream != null) {
            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, stream);
        }

        if (printRequestAttributeSet != null) {
            exporter.setParameter(JRPrintServiceExporterParameter.PRINT_REQUEST_ATTRIBUTE_SET, printRequestAttributeSet);
        }

        exporter.exportReport();
        return;
    }

    private Map<FontKey, PdfFont> createFontMap() {
        final Map<FontKey, PdfFont> result = new HashMap<FontKey, PdfFont>(4);

        addFont(result, "Quadraat-Regular", "QUAD____.ttf", BaseFont.CP1252);

        addFont(result, "Quadraat-Bold", "QUADBD__.ttf", BaseFont.CP1252);

        addFont(result, "Quadraat-Italic", "QUADI___.ttf", BaseFont.CP1252);

        addFont(result, "Quadraat-BoldItalic", "QUADBDI_.ttf", BaseFont.CP1252);

        addFont(result, "Arial", "Arial.ttf", BaseFont.CP1252);

        addFont(result, "Arial Black", "Arial_Black.ttf", BaseFont.CP1252);

        addFont(result, "Lucida Handwriting", "LucidaHandwriting.ttf", BaseFont.CP1252);

        addFont(result, "Garamond", "AdobeGaramondPro.ttf", BaseFont.CP1252);

        addFont(result, "Garamond Bold", "AdobeGaramondBold.ttf", BaseFont.CP1252);

        addFont(result, "Arial Unicode MS", "arialuni.ttf", BaseFont.IDENTITY_H);

        addFont(result, "DejaVu Serif Bold", "DejaVuSerif-Bold.ttf", BaseFont.IDENTITY_H);

        addFont(result, "Times New Roman", "tnr.ttf", BaseFont.CP1252);

        return result;
    }

    private void addFont(final Map<FontKey, PdfFont> result, final String fontName, final String pdfFontName,
            final String baseFont) {
        final URL url = ReportsUtils.class.getClassLoader().getResource("fonts/" + pdfFontName);
        result.put(new FontKey(fontName, false, false), new PdfFont(url.toExternalForm(), baseFont, true));
    }

    private JasperPrint createJasperPrint(final String key, final Map parameters, Collection dataSource) throws JRException {
        JasperReport report = reportsMap.get(key);

        if (report == null) {
            final String reportFileName = properties.getProperty(key);
            if (reportFileName != null) {
                report = (JasperReport) JRLoader.loadObject(ReportsUtils.class.getResourceAsStream(reportFileName));
                reportsMap.put(key, report);
            }
        }

        if (report != null) {
            if (dataSource == null || dataSource.isEmpty()) {
                // dummy, engine seems to work not very well with empty data
                // sources
                dataSource = Collections.singletonList(StringUtils.EMPTY);
            }

            return JasperFillManager.fillReport(report, parameters, new TransactionalDataSource(dataSource));
        }

        return null;
    }

}
