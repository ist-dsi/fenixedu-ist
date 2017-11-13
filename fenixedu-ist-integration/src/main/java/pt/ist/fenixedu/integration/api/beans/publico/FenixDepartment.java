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
package pt.ist.fenixedu.integration.api.beans.publico;

import java.util.List;

public class FenixDepartment {
        private String name, acronym;
        private List<FenixDepartmentMember> members;

    public FenixDepartment(String name, String acronym, List<FenixDepartmentMember> members) {
        this.name = name;
        this.acronym = acronym;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public List<FenixDepartmentMember> getMembers() {
        return members;
    }

    public void setMembers(List<FenixDepartmentMember> members) {
        this.members = members;
    }

    public static class FenixDepartmentMember {
        private String istId, name, email, role, category, area, photo;

        public FenixDepartmentMember(String istId, String name, String email, String photo) {
            this.istId = istId;
            this.name = name;
            this.email = email;
            this.photo = photo;
        }

        public FenixDepartmentMember(String istId, String name, String email, String role, String category, String area, String photo) {
            this.istId = istId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.category = category;
            this.area = area;
            this.photo = photo;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getArea() {
            return area;
        }

        public void setArea(String area) {
            this.area = area;
        }

        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getIstId() {
            return istId;
        }

        public void setIstId(String istId) {
            this.istId = istId;
        }
    }
}
