/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Core.
 *
 * FenixEdu Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Core.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.delegates.ui;

import pt.ist.fenixedu.delegates.domain.student.Delegate;

public class DelegateBean {

    String name;
    String username;
    String email;
    String delegateTitle;
    String picture;

    public DelegateBean(Delegate delegate) {
        this.name = delegate.getUser().getPerson().getName();
        this.username = delegate.getUser().getUsername();
        this.email = delegate.getUser().getPerson().getEmailForSendingEmails();
        this.delegateTitle = delegate.getTitle();
        this.picture = delegate.getUser().getProfile().getAvatarUrl();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDelegateTitle() {
        return delegateTitle;
    }

    public void setDelegateTitle(String delegateType) {
        this.delegateTitle = delegateType;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

}