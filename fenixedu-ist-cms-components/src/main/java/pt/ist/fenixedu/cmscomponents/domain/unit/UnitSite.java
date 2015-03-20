/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.domain.unit;

import java.util.Optional;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.DomainObject;

public class UnitSite extends UnitSite_Base {

    public UnitSite(Unit unit) {
        super();
        setUnit(unit);
        setBennu(Bennu.getInstance());
    }

    @Override
    public LocalizedString getName() {
        return Optional.ofNullable(super.getName()).orElse(getUnit().getNameI18n().toLocalizedString());
    }

    @Override
    public LocalizedString getDescription() {
        return Optional.ofNullable(super.getDescription()).orElse(getName());
    }

    @Override
    public DomainObject getObject() {
        return getUnit();
    }

    @Override
    public void delete() {
        this.setUnit(null);
        this.setBennu(null);
        super.delete();
    }
}
