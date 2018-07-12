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
package pt.ist.fenixedu.integration.ui.struts.action.research.researchUnit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.groups.Group;

import pt.ist.fenixedu.integration.domain.UnitFile;
import pt.ist.fenixedu.integration.domain.UnitFileTag;

public class UnitFileBean implements Serializable {

    private final UnitFile file;

    private String name;

    private String description;

    private List<Group> groups;

    private String tags;

    protected UnitFileBean() {
        this.file = null;
        groups = new ArrayList<>();
    }

    public UnitFileBean(UnitFile file) {
        this.file = file;
        this.name = file.getDisplayName();
        this.description = file.getDescription();
        setGroup(file.getPermittedGroup());
        setupTags(file.getUnitFileTagsSet());
    }

    private void setupTags(Collection<UnitFileTag> unitFileTags) {
        String tags = "";
        int i = unitFileTags.size();
        for (UnitFileTag tag : unitFileTags) {
            tags += tag.getName();
            if (--i > 0) {
                tags += " ";
            }
        }
        setTags(tags);
    }

    public UnitFile getFile() {
        return file;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Group getGroup() {
        return groups.stream().reduce(Group.nobody(), (g1, g2) -> g1.or(g2));
    }

    public void setGroup(Group group) {
        String[] groupsExpressions = group.getExpression().split("[ ][|][ ]");
        this.groups = Arrays.stream(groupsExpressions).map(s -> Group.parse(s)).collect(Collectors.toList());
    }

//    public List<Group> getGroups() {
//        if (group instanceof UnionGroup) {
//            return new ArrayList<>(((UnionGroup) group).getChildren());
//        }
//        return Collections.singletonList(group);
//    }
//
//    public void setGroups(List<Group> groups) {
//        group = UnionGroup.of(groups.stream());
//    }

    public Unit getUnit() {
        return getFile().getUnit();
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<Group> getGroups() {
        return this.groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }
}
