/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Parking.
 *
 * FenixEdu IST Parking is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Parking is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Parking.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.parking.ui.struts.action;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.util.DateFormatUtil;
import org.fenixedu.academic.util.FileUtils;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.fenixedu.bennu.struts.portal.StrutsFunctionality;
import org.fenixedu.commons.spreadsheet.ExcelStyle;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import com.google.common.io.Files;
import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Table;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.parking.domain.ParkingGroup;
import pt.ist.fenixedu.parking.domain.ParkingParty;
import pt.ist.fenixedu.parking.domain.Vehicle;

@StrutsFunctionality(app = ParkingManagerApp.class, path = "export-parking-data", titleKey = "link.mergeFiles")
@Mapping(module = "parkingManager", path = "/exportParkingDB", input = "/exportParkingDB.do?method=prepareExportFile",
        formBean = "exportFile")
@Forwards(@Forward(name = "exportFile", path = "/parkingManager/exportFile.jsp"))
public class ExportParkingDataToAccessDatabaseDA extends FenixDispatchAction {

    @EntryPoint
    public ActionForward prepareExportFile(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) {

        request.setAttribute("openFileBean", new OpenFileBean());

        return mapping.findForward("exportFile");
    }

    public ActionForward mergeFilesAndExportToExcel(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        OpenFileBean openFileBean = getRenderedObject();
        if (openFileBean != null) {
            if (openFileBean.getInputStream() == null) {
                ActionMessages actionMessages = new ActionMessages();
                actionMessages.add("file", new ActionMessage("error.no.file"));
                saveMessages(request, actionMessages);
                RenderUtils.invalidateViewState();
                return prepareExportFile(mapping, actionForm, request, response);
            }
            File dbFile = FileUtils.copyToTemporaryFile(openFileBean.getInputStream());
            Database database = Database.open(dbFile, Boolean.FALSE, Boolean.TRUE);
            Table table = database.getTable("XML");

            List<ParkingParty> parkingParties = getValidParkingParties();
            final HSSFWorkbook workbook = new HSSFWorkbook();
            final ExcelStyle excelStyle = new ExcelStyle(workbook);

            final Spreadsheet parkingBDSpreadsheet = new Spreadsheet("BD Fénix Parque");
            setHeaders(parkingBDSpreadsheet);

            for (Map<String, Object> row = table.getNextRow(); row != null; row = table.getNextRow()) {
                Long cardNumber = new Long((String) row.get("Card"));
                ParkingParty parkingParty = getParkingPartyCardNumber(cardNumber, parkingParties);
                if (parkingParty != null) {
                    addRow(parkingBDSpreadsheet, parkingParty, row);
                }
            }

            for (ParkingParty parkingParty : parkingParties) {
                addNewRow(parkingBDSpreadsheet, parkingParty);
            }

            response.setContentType("text/plain");
            response.setHeader("Content-disposition", "attachment; filename=parkingDB_merge.xls");
            final ServletOutputStream writer = response.getOutputStream();
            parkingBDSpreadsheet.exportToXLSSheet(workbook, (HSSFCellStyle) excelStyle.getHeaderStyle(), (HSSFCellStyle) excelStyle.getStringStyle());
            workbook.write(writer);
            writer.flush();

            response.flushBuffer();
        }
        return null;
    }

    private void addRow(final Spreadsheet parkingBDSpreadsheet, ParkingParty parkingParty, Map<String, Object> accessTableRow)
            throws FenixServiceException {
        long thisInstant = Calendar.getInstance().getTimeInMillis();
        DateTime dateTime = new DateTime(thisInstant);
        final Row row = parkingBDSpreadsheet.addRow();
        Person person = null;
        if (parkingParty.getParty().isPerson()) {
            person = (Person) parkingParty.getParty();
        }

        row.setCell(((Short) accessTableRow.get("Garage")).toString());
        row.setCell(parkingParty.getCardNumber().toString()); // cardNumber
        row.setCell(((Short) accessTableRow.get("Type")).toString());
        row.setCell(getBooleanString(accessTableRow.get("Access"))); // if the card is active or not
        row.setCell(convertParkingGroupToAccessDB(parkingParty.getParkingGroup())); // accessGroup
        row.setCell(((Short) accessTableRow.get("Fee")).toString());
        row.setCell(((Short) accessTableRow.get("SAC")).toString());
        row.setCell(DateFormatUtil.format("dd/MM/yyyy HH:mm:ss", (Date) accessTableRow.get("AlterDate")));
        row.setCell(DateFormatUtil.format("dd/MM/yyyy HH:mm:ss", (Date) accessTableRow.get("CreatedDate")));
        row.setCell(dateTime.toString("dd/MM/yyyy HH:mm:ss")); // editedDate
        row.setCell(person != null ? getName(person.getNickname()) : getName(parkingParty.getParty().getName())); // name
        row.setCell((String) accessTableRow.get("Address"));
        String vehicle1PlateNumber = "";
        String vehicle2PlateNumber = "";
        int counter = 1;
        for (Vehicle vehicle : parkingParty.getVehiclesSet()) {
            if (counter == 1) {
                vehicle1PlateNumber = vehicle.getPlateNumber();
            } else if (counter == 2) {
                vehicle2PlateNumber = vehicle.getPlateNumber();
            } else {
                break;
            }
            counter++;
        }
        row.setCell(vehicle1PlateNumber); // license
        row.setCell(vehicle2PlateNumber); // licenseAlt

        row.setCell(person != null && person.getWorkPhone() != null ? person.getWorkPhone() : ""); // registration
        row.setCell(person != null && person.getDefaultMobilePhoneNumber() != null ? person.getDefaultMobilePhoneNumber() : ""); // registrationAlt
        row.setCell((String) accessTableRow.get("ClientRef"));
        row.setCell((String) accessTableRow.get("Comment"));
        row.setCell(((Integer) accessTableRow.get("Price")).toString());

        String endValidityDate =
                parkingParty.getCardEndDate() == null ? "" : parkingParty.getCardEndDate().toString("dd/MM/yyyy HH:mm:ss");
        row.setCell(endValidityDate);
        row.setCell(DateFormatUtil.format("dd/MM/yyyy HH:mm:ss", (Date) accessTableRow.get("LastUsedDate")));
        row.setCell(getBooleanString(accessTableRow.get("Invoice")));
        row.setCell(parkingParty.getCardEndDate() != null ? "FALSE" : "TRUE"); // if true, start and end validity dates are ignored
        row.setCell(getBooleanString(accessTableRow.get("Present")));
        row.setCell(getBooleanString(accessTableRow.get("PayDirect")));
        row.setCell(getBooleanString(accessTableRow.get("APBCorrect"))); // if it's already in the park
        String startValidityDate =
                parkingParty.getCardStartDate() == null ? "" : parkingParty.getCardStartDate().toString("dd/MM/yyyy HH:mm:ss");
        row.setCell(startValidityDate);
        row.setCell(getBooleanString(accessTableRow.get("NoFee")));
    }

    private void addNewRow(final Spreadsheet parkingBDSpreadsheet, ParkingParty parkingParty) throws FenixServiceException {
        DateTime dateTime = new DateTime();
        final Row row = parkingBDSpreadsheet.addRow();
        Person person = null;
        if (parkingParty.getParty().isPerson()) {
            person = (Person) parkingParty.getParty();
        }
        row.setCell("0"); // garage
        row.setCell(parkingParty.getCardNumber().toString()); // cardNumber
        row.setCell("3"); // type Mirafe
        row.setCell("TRUE"); // access (if the card is active or not)
        row.setCell(convertParkingGroupToAccessDB(parkingParty.getParkingGroup()));
        row.setCell("1"); // fee
        row.setCell("0"); // SAC
        row.setCell("1/1/2000"); // alterDate
        row.setCell(dateTime.toString("dd/MM/yyyy HH:mm:ss")); // createdDate
        row.setCell(dateTime.toString("dd/MM/yyyy HH:mm:ss")); // editedDate
        row.setCell(person != null ? getName(person.getNickname()) : getName(parkingParty.getParty().getName())); // name
        row.setCell(""); // address
        String vehicle1PlateNumber = "";
        String vehicle2PlateNumber = "";
        int counter = 1;
        for (Vehicle vehicle : parkingParty.getVehiclesSet()) {
            if (counter == 1) {
                vehicle1PlateNumber = vehicle.getPlateNumber();
            } else if (counter == 2) {
                vehicle2PlateNumber = vehicle.getPlateNumber();
            } else {
                break;
            }
            counter++;
        }
        row.setCell(vehicle1PlateNumber); // license
        row.setCell(vehicle2PlateNumber); // licenseAlt
        row.setCell(person != null && person.getWorkPhone() != null ? getString(person.getWorkPhone(), 19) : ""); // registration
        row.setCell(person != null && person.getDefaultMobilePhoneNumber() != null ? getString(
                person.getDefaultMobilePhoneNumber(), 19) : ""); // registrationAlt
        row.setCell(""); // clientRef
        row.setCell(""); // comment
        row.setCell("0"); // price
        String endValidityDate =
                parkingParty.getCardEndDate() == null ? "" : parkingParty.getCardEndDate().toString("dd/MM/yyyy HH:mm:ss");
        row.setCell(endValidityDate);
        row.setCell("1/1/2000"); // lastUsedDate
        row.setCell("FALSE"); // invoice
        row.setCell(parkingParty.getCardStartDate() != null ? "FALSE" : "TRUE"); // if true, start and end validity dates are ignored
        row.setCell("FALSE"); // present
        row.setCell("FALSE"); // payDirect
        row.setCell("FALSE"); // apbCorrect (if it's already in the park)
        String startValidityDate =
                parkingParty.getCardStartDate() == null ? "" : parkingParty.getCardStartDate().toString("dd/MM/yyyy HH:mm:ss");
        row.setCell(startValidityDate);
        row.setCell("TRUE"); // noFee
    }

    private String getName(String name) {
        if (name.length() > 59) { // max size of the other parking application DB
            StringBuilder resultName = new StringBuilder();
            resultName = new StringBuilder();
            String[] names = name.split("\\p{Space}+");
            for (int iter = 1; iter < names.length - 1; iter++) {
                if (names[iter].length() > 5) {
                    names[iter] = names[iter].substring(0, 5) + ".";
                }
            }
            for (String name2 : names) {
                resultName.append(name2).append(" ");
            }
            if (resultName.length() > 59) {
                resultName = new StringBuilder(names[0]).append(" ").append(names[names.length - 1]);
            }
            return resultName.toString().trim();
        } else {
            return name;
        }

    }

    private void setHeaders(final Spreadsheet spreadsheet) {
        spreadsheet.setHeader("Garage");
        spreadsheet.setHeader("Card");
        spreadsheet.setHeader("Type");
        spreadsheet.setHeader("Access");
        spreadsheet.setHeader("AccessGroup");
        spreadsheet.setHeader("Fee");
        spreadsheet.setHeader("SAC");
        spreadsheet.setHeader("AlterDate");
        spreadsheet.setHeader("CreatedDate");
        spreadsheet.setHeader("EditedDate");
        spreadsheet.setHeader("Name");
        spreadsheet.setHeader("Address");
        spreadsheet.setHeader("License");
        spreadsheet.setHeader("LicenseAlt");
        spreadsheet.setHeader("Registration");
        spreadsheet.setHeader("RegistrationAlt");
        spreadsheet.setHeader("ClientRef");
        spreadsheet.setHeader("Comment");
        spreadsheet.setHeader("Price");
        spreadsheet.setHeader("EndValidityDate");
        spreadsheet.setHeader("LastUsedDate");
        spreadsheet.setHeader("Invoice");
        spreadsheet.setHeader("Unlimited");
        spreadsheet.setHeader("Present");
        spreadsheet.setHeader("PayDirect");
        spreadsheet.setHeader("APBCorrect");
        spreadsheet.setHeader("StartValidityDate");
        spreadsheet.setHeader("NoFee");
    }

    public ActionForward mergeFilesAndExport(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        OpenFileBean openFileBean = getRenderedObject();
        if (openFileBean != null) {
            if (openFileBean.getInputStream() == null) {
                ActionMessages actionMessages = new ActionMessages();
                actionMessages.add("file", new ActionMessage("error.no.file"));
                saveMessages(request, actionMessages);
                RenderUtils.invalidateViewState();
                return prepareExportFile(mapping, actionForm, request, response);
            }
            File dbFile = FileUtils.copyToTemporaryFile(openFileBean.getInputStream());
            Database database = Database.open(dbFile, Boolean.FALSE, Boolean.TRUE);
            Table xmlTable = database.getTable("XML");
            Table errors = database.getTable("Paste Errors");

            File temp = FileUtils.createTemporaryFile();
            Database db = Database.create(temp, Boolean.TRUE);
            List<Column> columns = new ArrayList<Column>();
            for (Column column : xmlTable.getColumns()) {
                Column newColumn = new Column();
                newColumn.setName(column.getName());
                newColumn.setType(column.getType());
                if (column.getType().equals(DataType.BOOLEAN)) {
                    newColumn.setLength((short) 0);
                } else {
                    newColumn.setLength(column.getLength());
                }
                columns.add(newColumn);
            }
            db.createTable("XML", columns);
            columns.clear();
            for (Column column : errors.getColumns()) {
                Column newColumn = new Column();
                newColumn.setName(column.getName());
                newColumn.setType(column.getType());
                newColumn.setLength(column.getLength());
                columns.add(newColumn);
            }
            db.createTable("Paste Errors", columns);

            Table xml = db.getTable("XML");
            List<ParkingParty> parkingParties = getValidParkingParties();
            List<Object[]> rows = new ArrayList<Object[]>();
            for (Map<String, Object> row = xmlTable.getNextRow(); row != null; row = xmlTable.getNextRow()) {
                Long cardNumber = new Long((String) row.get("Card"));
                ParkingParty parkingParty = getParkingPartyCardNumber(cardNumber, parkingParties);
                if (parkingParty != null) {
                    rows.add(updateRow(parkingParty, row));
                }
            }
            xml.addRows(rows);

            for (ParkingParty parkingParty : parkingParties) {
                Object[] newRow = new Object[28];
                fillInRow(parkingParty, newRow);
                xml.addRow(newRow);
            }

            Table newErrors = db.getTable("Paste Errors");
            rows = new ArrayList<Object[]>();
            for (Map<String, Object> row = errors.getNextRow(); row != null; row = errors.getNextRow()) {
                rows.add(new Object[] { row.get("Field0") });
            }
            newErrors.addRows(rows);

            database.close();

            response.setContentType("application/vnd.ms-access");
            response.setHeader("Content-disposition", "attachment; filename=Cartões_XML.mdb");
            final ServletOutputStream writer = response.getOutputStream();
            writer.write(Files.toByteArray(temp));
            writer.flush();
            writer.close();

            response.flushBuffer();
        }
        return null;
    }

    public ActionForward export(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        InputStream inputStream =
                ExportParkingDataToAccessDatabaseDA.class.getClassLoader().getResourceAsStream("templates/Cartoes_XML.mdb");

        if (inputStream != null) {
            File temp = FileUtils.copyToTemporaryFile(inputStream);
            Database db = Database.open(temp, Boolean.FALSE, Boolean.TRUE);

            Table xml = db.getTable("XML");
            List<ParkingParty> parkingParties = getValidParkingParties();
            for (ParkingParty parkingParty : parkingParties) {
                Object[] newRow = new Object[28];
                fillInRow(parkingParty, newRow);
                xml.addRow(newRow);
            }

            response.setContentType("application/vnd.ms-access");
            response.setHeader("Content-disposition", "attachment; filename=Cartões_XML.mdb");
            final ServletOutputStream writer = response.getOutputStream();
            writer.write(Files.toByteArray(temp));
            writer.flush();
            writer.close();

            response.flushBuffer();
            inputStream.close();
        }
        return null;
    }

    private Object[] updateRow(ParkingParty parkingParty, Map<String, Object> accessTableRow) throws FenixServiceException {
        long thisInstant = Calendar.getInstance().getTimeInMillis();
        DateTime dateTime = new DateTime(thisInstant);
        Person person = null;
        if (parkingParty.getParty().isPerson()) {
            person = (Person) parkingParty.getParty();
        }

        Object[] newRow = new Object[28];
        newRow[0] = accessTableRow.get("Garage");
        newRow[1] = parkingParty.getCardNumber().toString();
        newRow[2] = accessTableRow.get("Type");
        newRow[3] = accessTableRow.get("Access");
        newRow[4] = Integer.valueOf(convertParkingGroupToAccessDB(parkingParty.getParkingGroup())).intValue();
        newRow[5] = accessTableRow.get("Fee");
        newRow[6] = accessTableRow.get("SAC");
        newRow[7] = accessTableRow.get("AlterDate");
        newRow[8] = dateTime.toDate();
        newRow[9] = dateTime.toDate();
        newRow[10] = person != null ? getName(person.getNickname()) : getName(parkingParty.getParty().getName());
        newRow[11] = accessTableRow.get("Address");
        String vehicle1PlateNumber = "";
        String vehicle2PlateNumber = "";
        int counter = 1;
        for (Vehicle vehicle : parkingParty.getVehiclesSet()) {
            if (counter == 1) {
                vehicle1PlateNumber = vehicle.getPlateNumber();
            } else if (counter == 2) {
                vehicle2PlateNumber = vehicle.getPlateNumber();
            } else {
                break;
            }
            counter++;
        }
        newRow[12] = vehicle1PlateNumber;
        newRow[13] = vehicle2PlateNumber;
        newRow[14] = person != null && person.getWorkPhone() != null ? getString(person.getWorkPhone(), 19) : "";
        newRow[15] =
                person != null && person.getDefaultMobilePhoneNumber() != null ? getString(person.getDefaultMobilePhoneNumber(),
                        19) : "";
        newRow[16] = accessTableRow.get("ClientRef");
        newRow[17] = accessTableRow.get("Comment");
        newRow[18] = accessTableRow.get("Price");
        newRow[19] = parkingParty.getCardEndDate() == null ? null : parkingParty.getCardEndDate().toDate();
        newRow[20] = accessTableRow.get("LastUsedDate");
        newRow[21] = accessTableRow.get("Invoice");
        newRow[22] = parkingParty.getCardStartDate() != null ? Boolean.FALSE : Boolean.TRUE;
        newRow[23] = accessTableRow.get("Present");
        newRow[24] = accessTableRow.get("PayDirect");
        newRow[25] = accessTableRow.get("APBCorrect");
        newRow[26] = accessTableRow.get("NoFee");
        newRow[27] = parkingParty.getCardStartDate() == null ? null : parkingParty.getCardStartDate().toDate();
        return newRow;
    }

    private void fillInRow(ParkingParty parkingParty, Object[] newRow) throws Exception {
        DateTime dateTime = new DateTime();
        Person person = null;
        if (parkingParty.getParty().isPerson()) {
            person = (Person) parkingParty.getParty();
        }

        newRow[0] = 0;
        newRow[1] = parkingParty.getCardNumber().toString();
        newRow[2] = 3;
        newRow[3] = Boolean.TRUE;
        newRow[4] =
                parkingParty.getParkingGroup() != null ? Integer.valueOf(
                        convertParkingGroupToAccessDB(parkingParty.getParkingGroup())).intValue() : "";
        newRow[5] = 1;
        newRow[6] = 0;
        newRow[7] = new YearMonthDay(2000, 1, 1).toDateTimeAtMidnight().toDate();
        newRow[8] = dateTime.toDate();
        newRow[9] = dateTime.toDate();
        newRow[10] = person != null ? getName(person.getNickname()) : getName(parkingParty.getParty().getName());
        newRow[11] = "";
        String vehicle1PlateNumber = "";
        String vehicle2PlateNumber = "";
        int counter = 1;
        for (Vehicle vehicle : parkingParty.getVehiclesSet()) {
            if (counter == 1) {
                vehicle1PlateNumber = vehicle.getPlateNumber();
            } else if (counter == 2) {
                vehicle2PlateNumber = vehicle.getPlateNumber();
            } else {
                break;
            }
            counter++;
        }
        newRow[12] = vehicle1PlateNumber;
        newRow[13] = vehicle2PlateNumber;
        newRow[14] = person != null && person.getWorkPhone() != null ? getString(person.getWorkPhone(), 19) : "";
        newRow[15] =
                person != null && person.getDefaultMobilePhoneNumber() != null ? getString(person.getDefaultMobilePhoneNumber(),
                        19) : "";
        newRow[16] = "";
        newRow[17] = "";
        newRow[18] = 0;
        newRow[19] = parkingParty.getCardEndDate() == null ? null : parkingParty.getCardEndDate().toDate();
        newRow[20] = new YearMonthDay(2000, 1, 1).toDateTimeAtMidnight().toDate();
        newRow[21] = Boolean.FALSE;
        newRow[22] = parkingParty.getCardStartDate() != null ? Boolean.FALSE : Boolean.TRUE;
        newRow[23] = Boolean.FALSE;
        newRow[24] = Boolean.FALSE;
        newRow[25] = Boolean.FALSE;
        newRow[26] = Boolean.TRUE;
        newRow[27] = parkingParty.getCardStartDate() == null ? null : parkingParty.getCardStartDate().toDate();
    }

    private String convertParkingGroupToAccessDB(ParkingGroup parkingGroup) throws FenixServiceException {
        if (parkingGroup.getGroupName().equalsIgnoreCase("Docentes")) {
            return "1";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Não Docentes")) {
            return "2";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Especiais")) {
            return "3";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Bolseiros")) {
            return "4";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Investigadores")) {
            return "5";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("3º ciclo")) {
            return "6";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("2º ciclo")) {
            return "7";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("IPSFL")) {
            return "8";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Jubilados")) {
            return "9";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Limitados")) {
            return "10";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Limitado1")) {
            return "11";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Limitado2")) {
            return "12";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Limitado3")) {
            return "13";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Limitado4")) {
            return "14";
        } else if (parkingGroup.getGroupName().equalsIgnoreCase("Limitado5")) {
            return "15";
        }
        throw new FenixServiceException();
    }

    private List<ParkingParty> getValidParkingParties() {
        List<ParkingParty> parkingParties = new ArrayList<ParkingParty>();
        for (ParkingParty parkingParty : ParkingParty.getAll()) {
            if (parkingParty.getCardNumber() != null) {
                parkingParties.add(parkingParty);
            }
        }
        return parkingParties;
    }

    private ParkingParty getParkingPartyCardNumber(Long cardNumber, List<ParkingParty> parkingParties) {
        for (ParkingParty parkingParty : parkingParties) {
            if (parkingParty.getCardNumber().equals(cardNumber)) {
                parkingParties.remove(parkingParty);
                return parkingParty;
            }
        }
        return null;
    }

    private String getBooleanString(Object object) {
        Boolean booleanNumber = (Boolean) object;
        if (booleanNumber) {
            return "TRUE";
        } else {
            return "FALSE";
        }
    }

    private String getString(String string, int maxSize) {
        if (string.length() > maxSize) {
            return string.substring(0, maxSize - 1);
        } else {
            return string;
        }
    }

}