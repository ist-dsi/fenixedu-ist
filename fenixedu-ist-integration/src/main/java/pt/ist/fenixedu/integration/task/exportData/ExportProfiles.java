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
package pt.ist.fenixedu.integration.task.exportData;

import static org.fenixedu.commons.stream.StreamUtils.toJsonArray;

import java.io.FileOutputStream;
import java.util.Optional;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.joda.time.LocalDate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.contracts.domain.LegacyRoleUtils;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

@Task(englishTitle = "Export user profiles to shared json file with other applications.")
public class ExportProfiles extends CronTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        final String profiles = Bennu.getInstance().getUserSet().stream()
                .parallel()
                .map(u -> toJsonObject(u))
                .filter(j -> j != null)
                .collect(toJsonArray()).toString();
        final byte[] bytes = profiles.getBytes();
        try (final FileOutputStream fos = new FileOutputStream("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/profiles.json")) {
            fos.write(bytes);
        }
    }

    private JsonObject toJsonObject(final User user) {
        try {
            return FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<JsonObject>() {

                @Override
                public JsonObject call() {
                    final UserProfile up = user.getProfile();
                    if (up == null) {
                        return null;
                    }
                    final JsonObject object = new JsonObject();
                    object.addProperty("username", up.getUser().getUsername());
                    object.addProperty("givenNames", up.getGivenNames());
                    object.addProperty("familyNames", up.getFamilyNames());
                    object.addProperty("displayName", up.getDisplayName());
                    object.addProperty("email", up.getEmail());
                    object.add("userAliases", aliasesFor(up));
                    Optional<LocalDate> expiration = up.getUser().getExpiration();
                    object.addProperty("expiration", expiration.map(Object::toString).orElse(null));
                    object.add("roles", LegacyRoleUtils.mainRoleKeys(up.getUser()).stream().map(JsonPrimitive::new).collect(toJsonArray()));
                    return object;
                }
            }, new AtomicInstance(TxMode.READ, false));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private JsonArray aliasesFor(final UserProfile up) {
    	final User user = up.getUser();
    	final Person person = user.getPerson();
    	final JsonArray result = new JsonArray();
    	result.add(new JsonPrimitive(user.getUsername()));
    	if (person != null) {
    		if (person.getEmployee() != null) {
    			result.add(new JsonPrimitive(person.getEmployee().getEmployeeNumber()));
    		}
    		if (person.getStudent() != null) {
    			result.add(new JsonPrimitive(person.getStudent().getNumber()));
    		}
    	}
    	return result;
    }

}
