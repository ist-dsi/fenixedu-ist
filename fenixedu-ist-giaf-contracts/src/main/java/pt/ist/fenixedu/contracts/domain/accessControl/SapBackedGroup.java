package pt.ist.fenixedu.contracts.domain.accessControl;

import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.GroupStrategy;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;

import com.google.common.collect.Iterables;

import pt.ist.sap.group.integration.domain.SapGroup;
import pt.ist.sap.group.integration.domain.SapWrapper;

public abstract class SapBackedGroup extends GroupStrategy {

	private static final long serialVersionUID = -2985536595609345377L;

	@Override
	public String getPresentationName() {
		return BundleUtil.getString(Bundle.GROUP, presentationNameLable());
	}

	protected abstract String presentationNameLable();

	@Override
	public Stream<User> getMembers() {
		final SapGroup sapGroup = new SapGroup();
		Iterable<String> result = Collections.emptySet();
		for (final String institution : SapWrapper.institutions) {
			final String institutionCode = SapWrapper.institutionCode.apply(institution);
			for (String sapGroupName : sapGroups()) {
				sapGroup.setGroup(institutionCode + sapGroupName);
				result = Iterables.concat(result, sapGroup.list());
			}
		}
		return StreamSupport.stream(result.spliterator(), false).map(username -> User.findByUsername(username));
	}

	protected abstract String[] sapGroups();

	@Override
	public Stream<User> getMembers(final DateTime when) {
		return getMembers();
	}

	@Override
	public boolean isMember(final User user) {
        final SapGroup sapGroup = new SapGroup();
        if (user != null && user.getPerson() != null) {
            for (final String institution : SapWrapper.institutions) {
                final String institutionCode = SapWrapper.institutionCode.apply(institution);
                for (String sapGroupName : sapGroups()) {
                    sapGroup.setGroup(institutionCode + sapGroupName);
                    if (sapGroup.isMember(user.getUsername())) {
                        return true;
                    }
                }
            }
        }
        return false;
	}

	@Override
	public boolean isMember(final User user, final DateTime when) {
		return isMember(user);
	}

}
