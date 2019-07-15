/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Delegates.
 *
 * FenixEdu IST Delegates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Delegates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Delegates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.api;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.oauth.annotation.OAuthEndpoint;
import org.joda.time.DateTime;

import pt.ist.fenixedu.delegates.domain.student.CycleDelegate;
import pt.ist.fenixedu.delegates.domain.student.DegreeDelegate;
import pt.ist.fenixedu.delegates.domain.student.Delegate;
import pt.ist.fenixedu.delegates.domain.student.YearDelegate;
import pt.ist.fenixedu.delegates.domain.util.email.DelegateSender;
import pt.ist.fenixframework.Atomic;

import com.google.gson.JsonObject;

@Path("/fenix/v1")
public class FenixDelegates {
    public static class FenixDelegate {
        public String delegateId;

        public Integer curricularYear;

        public String cycleType;

        public FenixDelegate() {
        }

        public FenixDelegate(String delegateId, Integer curricularYear, String cycleType) {
            this.delegateId = delegateId;
            this.curricularYear = curricularYear;
            this.cycleType = cycleType;
        }

        public FenixDelegate(Delegate delegate) {
            this.delegateId = delegate.getUser().getUsername();
            this.curricularYear = delegate.getCurricularYear() != null ? delegate.getCurricularYear().getYear() : null;
            this.cycleType = delegate.getCycleType() != null ? delegate.getCycleType().name() : null;
        }

        public String getDelegateId() {
            return delegateId;
        }

        public void setDelegateId(String delegateId) {
            this.delegateId = delegateId;
        }

        public Integer getCurricularYear() {
            return curricularYear;
        }

        public void setCurricularYear(Integer curricularYear) {
            this.curricularYear = curricularYear;
        }

        public String getCycleType() {
            return cycleType;
        }

        public void setCycleType(String cycleType) {
            this.cycleType = cycleType;
        }
    }

    public final static String DELEGATE_MANAGEMENT_SCOPE = "DELEGATE_MANAGEMENT";

    public final static String JSON_UTF8 = "application/json; charset=utf-8";

    @GET
    @Produces(JSON_UTF8)
    @Path("degrees/{degreeSigla}/delegates")
    public Set<FenixDelegate> delegates(@PathParam("degreeSigla") String degreeSigla,
            @QueryParam("curricularYear") Integer curricularYear, @QueryParam("cycleType") String cycleType) {
        Degree degree = Degree.readBySigla(degreeSigla);
        if (degree == null) {
            throw newApplicationError(Status.NOT_FOUND, "resource_not_found", "No degree found for acronym " + degreeSigla);
        }
        CurricularYear year = curricularYear != null ? CurricularYear.readByYear(curricularYear) : null;
        CycleType cycle = cycleType != null ? CycleType.valueOf(cycleType) : null;
        Set<FenixDelegate> result = new HashSet<>();
        for (Delegate delegate : degree.getDelegateSet()) {
            if (delegate.getInterval().containsNow()) {
                if (year != null && delegate.getCurricularYear() != year) {
                    continue;
                }
                if (cycle != null && delegate.getCycleType() != cycle) {
                    continue;
                }
                result.add(new FenixDelegate(delegate));
            }
        }
        return result;
    }

    @PUT
    @Consumes(JSON_UTF8)
    @Produces(JSON_UTF8)
    @Path("degrees/{degreeSigla}/delegates")
    @OAuthEndpoint(DELEGATE_MANAGEMENT_SCOPE)
    public FenixDelegate assign(@PathParam("degreeSigla") String degreeSigla, FenixDelegate bean) {
        if (!Group.dynamic("pedagogicalCouncil").isMember(Authenticate.getUser())) {
            throw newApplicationError(Status.UNAUTHORIZED, "unauthorized", "User is not authorized to access this resource");
        }
        Degree degree = Degree.readBySigla(degreeSigla);
        if (degree == null) {
            throw newApplicationError(Status.NOT_FOUND, "resource_not_found", "No degree found for acronym " + degreeSigla);
        }
        CurricularYear curricularYear =
                bean.getCurricularYear() != null ? CurricularYear.readByYear(bean.getCurricularYear()) : null;
        CycleType cycleType = bean.getCycleType() != null ? CycleType.valueOf(bean.getCycleType()) : null;

        User delegateUser = User.findByUsername(bean.getDelegateId());
        if (delegateUser == null) {
            throw newApplicationError(Status.NOT_FOUND, "resource_not_found",
                    "No user found with username " + bean.getDelegateId());
        }

        serv(degree, curricularYear, cycleType, delegateUser);
        return bean;
    }

    @Atomic
    private void serv(Degree degree, CurricularYear curricularYear, CycleType cycleType, User delegateUser) {
        degree.getDelegateSet().stream()
                .filter(d -> d.getInterval().containsNow())
                .filter(d -> Objects.equals(d.getCurricularYear(), curricularYear))
                .filter(d -> Objects.equals(d.getCycleType(), cycleType)).forEach(d -> d.setEnd(new DateTime()));
        Delegate newDelegate = null;
        if (cycleType != null) {
            newDelegate = new CycleDelegate(delegateUser, degree, cycleType);
        }
        if (curricularYear != null) {
            newDelegate = new YearDelegate(delegateUser, degree, curricularYear);
        }
        if (newDelegate == null) {
            newDelegate = new DegreeDelegate(delegateUser, degree);
        }
        newDelegate.setStart(new DateTime());
        newDelegate.setEnd(ExecutionYear.readCurrentExecutionYear().getEndDateYearMonthDay().toDateTimeAtMidnight().plusYears(1));
        newDelegate.setSender(new DelegateSender(newDelegate));
    }

    private WebApplicationException newApplicationError(Status status, String error, String description) {
        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("error", error);
        errorObject.addProperty("description", description);
        return new WebApplicationException(Response.status(status).entity(errorObject.toString()).build());
    }
}