package pt.ist.fenixedu.integration.task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityType;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.CountryUnit;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import pt.ist.fenixedu.contracts.persistenceTierOracle.DbConnector.ResultSetConsumer;
import pt.ist.fenixedu.contracts.persistenceTierOracle.GiafDbConnector;

@Task(englishTitle = "Import GIAF clients as potential external creditors.")
public class ImportCreditorsFromGiaf extends CronTask {

    @Override
    public void runTask() throws Exception {
        final DateTime now = DateTime.now();
        final YearMonthDay today = new YearMonthDay(now.getYear(), now.getMonthOfYear(), now.getDayOfMonth());
        final Spreadsheet sheet = new Spreadsheet("Clients");

        final Set<Party> creditors = Bennu.getInstance().getExternalScholarshipProviderSet();
        final Set<String> updatedCreditors = new HashSet<>();

        new GiafDbConnector().executeQuery(new ResultSetConsumer() {

            @Override
            public String query() {
                return "SELECT GIDENTGER.num_fis, GIDENTGER.nom_ent, GIDENTGER.nom_ent_abv, "
                        + "GIDPAISES.ISO_3166 FROM GIDCLI, GIDENTGER, GIDPAISES where GIDCLI.cli_cod_ent = GIDENTGER.cod_ent AND "
                        + "GIDENTGER.cod_pai = GIDPAISES.pais_cod_pai;";
            }

            @Override
            public void accept(final ResultSet resultSet) throws SQLException {

                final String number = get(resultSet, 1);
                final String name = get(resultSet, 2);
                final String acronym = get(resultSet, 3);
                final String country = get(resultSet, 4);
                Optional<Party> match =
                        creditors.stream().filter(party -> party.getSocialSecurityNumber().equals(number)).findAny();
                if (!match.isPresent()) {
                    match = Bennu.getInstance().getPartysSet().stream()
                            .filter(party -> party.getSocialSecurityNumber().equals(number)).findAny();
                    if (match.isPresent()) {
                        Bennu.getInstance().addExternalScholarshipProvider(match.get());
                    } else {
                        Unit externalUnit = Unit.createNewUnit(new LocalizedString(I18N.getLocale(), name), null, null, acronym, today, null,
                                CountryUnit.getCountryUnitByCountry(Country.readCountryByNationality(country)),
                                AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE), null, null, null,
                                false, null);
                        externalUnit.setSocialSecurityNumber(number);
                        Bennu.getInstance().addExternalScholarshipProvider(externalUnit);
                    }
                }
                updatedCreditors.add(number);
            }

            private String get(final ResultSet resultSet, final int i) throws SQLException {
                final String string = resultSet.getString(i);
                return string == null || string.length() == 0 ? " " : string.replace('\n', ' ').replace('\t', ' ');
            }

        });

        creditors.stream()
                .filter(party -> updatedCreditors.stream().noneMatch(number -> number.equals(party.getSocialSecurityNumber())))
                .forEach(party -> party.setRootDomainObjectExternalScholarshipProvider(null));
    }

}