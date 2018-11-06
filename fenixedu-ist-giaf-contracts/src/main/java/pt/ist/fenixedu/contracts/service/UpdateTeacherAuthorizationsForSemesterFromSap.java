package pt.ist.fenixedu.contracts.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Department;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Teacher;
import org.fenixedu.academic.domain.TeacherAuthorization;
import org.fenixedu.academic.domain.TeacherCategory;
import org.fenixedu.academic.domain.organizationalStructure.AccountabilityTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.SapSdkConfiguration;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.PeriodType;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.contracts.domain.organizationalStructure.EmployeeContract;
import pt.ist.sap.client.SapStaff;
import pt.ist.sap.client.SapStructure;
import pt.ist.sap.group.integration.domain.Colaborator;
import pt.ist.sap.group.integration.domain.ColaboratorSituation;

public class UpdateTeacherAuthorizationsForSemesterFromSap {

	private static final int minimumDaysForActivity = 90;
	private Map<String, TeacherCategory> categoryMap = new HashMap<String, TeacherCategory>();

	public String updateTeacherAuthorization(ExecutionSemester executionSemester) {
		StringBuilder output = new StringBuilder();
		int countNew = 0;
		int countRevoked = 0;
		int countEdited = 0;
		Interval semesterInterval = executionSemester.getAcademicInterval().toInterval();

		final SapStaff sapStaff = new SapStaff();
		final SapStructure sapStructure = new SapStructure();

		Map<User, Department> userDepartment = new HashMap<User, Department>();
		Map<User, Set<ColaboratorSituation>> colaboratorSituationsMap = new HashMap<User, Set<ColaboratorSituation>>();
			final JsonObject params = new JsonObject();
		params.addProperty("institution", SapSdkConfiguration.getConfiguration().sapServiceInstitutionCode());

			sapStaff.listPersonProfessionalInformation(params).forEach(e -> {
				final ColaboratorSituation colaboratorSituation = new ColaboratorSituation(e.getAsJsonObject());
				final User user = User.findByUsername(colaboratorSituation.username().toLowerCase());
				if (user == null) {
					output.append("\nError: No valid user found for " + colaboratorSituation.username());
				} else {

					Set<ColaboratorSituation> colaboratorSituations = colaboratorSituationsMap.get(user);
					if (colaboratorSituations == null) {
						colaboratorSituations = new HashSet<ColaboratorSituation>();
					}
					colaboratorSituations.add(colaboratorSituation);
					colaboratorSituationsMap.put(user, colaboratorSituations);
				}
			});

		sapStructure.listPeople(params).forEach(e -> {
			final Colaborator colaborator = new Colaborator(e.getAsJsonObject());
			final User user = User
					.findByUsername(SapSdkConfiguration.usernameProvider().toUsername(colaborator.sapId()));
			if (user != null && !Strings.isNullOrEmpty(colaborator.costCenter())) {
				try {
					Unit unit = Unit.readByCostCenterCode(Integer.parseInt(colaborator.costCenter().substring(2)));
					if (unit != null) {
						userDepartment.put(user, getEmployeeDepartmentUnit(unit));
		}
				} catch (NumberFormatException ex) {
					output.append("\nError: Invalid CC: " + colaborator.costCenter());
				}
			}
		});

		Set<TeacherAuthorization> processedAuthorizations = new HashSet<TeacherAuthorization>();

		for (User user : colaboratorSituationsMap.keySet()) {
			Person person = user.getPerson();
			Department department = userDepartment.get(user);// getDominantDepartment(person, executionSemester);
			TeacherAuthorization teacherAuthorization = null;
			if (department != null) {
				SortedSet<ColaboratorSituation> validColaboratorSituations = getValidPersonContractSituations(
						colaboratorSituationsMap, user, semesterInterval);
				int activeDays = validColaboratorSituations.stream().mapToInt(s -> getActiveDays(s, semesterInterval))
						.sum();
				if (activeDays >= minimumDaysForActivity) {
					ColaboratorSituation colaboratorSituation = getDominantColaboratorSituation(
							validColaboratorSituations, semesterInterval);
					if (colaboratorSituation != null) {
						Teacher teacher = person.getTeacher();
						if (person.getTeacher() == null) {
							teacher = new Teacher(person);
						}
						TeacherCategory teacherCategory = getTeacherCategory(colaboratorSituation.categoryName(),
								colaboratorSituation.categoryTypeName());
						if (teacherCategory != null) {
							Double lessonHours = getWeeklyLessonHours(colaboratorSituation, semesterInterval,
									teacherCategory);
							TeacherAuthorization existing = teacher
									.getTeacherAuthorization(executionSemester.getAcademicInterval()).orElse(null);
							if (existing != null) {
								if (existing.getDepartment().equals(department) && existing.isContracted()
										&& existing.getLessonHours().equals(lessonHours)
										&& existing.getTeacherCategory().equals(teacherCategory)) {
									teacherAuthorization = existing;
								} else {
									countEdited++;
									existing.revoke();
								}
							} else {
								countNew++;
							}
							if (teacherAuthorization == null) {
								teacherAuthorization = TeacherAuthorization.createOrUpdate(teacher, department,
										executionSemester, teacherCategory, true, lessonHours);
							}
							processedAuthorizations.add(teacherAuthorization);
						}

					}
				}
			}
		}

		Set<TeacherAuthorization> authorizations2Revoke = executionSemester.getTeacherAuthorizationStream()
				.filter(teacherAuthorization -> teacherAuthorization.isContracted()
						&& !processedAuthorizations.contains(teacherAuthorization))
				.collect(Collectors.toSet());

		for (TeacherAuthorization teacherAuthorization : authorizations2Revoke) {
			teacherAuthorization.revoke();
			countRevoked++;

		}

		output.append("\n" + countNew + " authorizations created for semester " + executionSemester.getQualifiedName());
		output.append(
				"\n" + countEdited + " authorizations edited for semester " + executionSemester.getQualifiedName());
		output.append(
				"\n" + countRevoked + " authorizations revoked for semester " + executionSemester.getQualifiedName());
		return output.toString();
	}

	private SortedSet<ColaboratorSituation> getValidPersonContractSituations(
			Map<User, Set<ColaboratorSituation>> situationsMap, User user, Interval semesterInterval) {
		SortedSet<ColaboratorSituation> validPersonContractSituations = new TreeSet<ColaboratorSituation>(
				new Comparator<ColaboratorSituation>() {
					@Override
					public int compare(ColaboratorSituation c1, ColaboratorSituation c2) {
						int compare = c1.beginDate().compareTo(c2.beginDate());
						return compare == 0 ? (c1.equals(c2) ? 0 : 1) : compare;
					}
				});
		validPersonContractSituations.addAll(situationsMap.get(user).stream().filter(cs -> {
			return cs.inExercise() && !cs.endSituation() && isValidAndOverlaps(cs, semesterInterval)
					&& isValidCategoryAndCategoryType(cs.categoryName(), cs.categoryTypeName());
		}).filter(Objects::nonNull).collect(Collectors.toSet()));
		return validPersonContractSituations;
	}

	private boolean isValidCategoryAndCategoryType(String categoryName, String categoryTypeName) {
		return getTeacherCategory(categoryName, categoryTypeName) != null;
	}

	private TeacherCategory getTeacherCategory(String name, String categoryType) {
		if (categoryMap.isEmpty()) {
			categoryMap.put("Docentes" + "Prof. Catedrático",
					TeacherCategory.findByCode("professor-catedratico").orElse(null));
			categoryMap.put("Docentes" + "Prof Cated Convidado",
					TeacherCategory.findByCode("professor-catedratico-convidado").orElse(null));
			categoryMap.put("Docentes" + "Prof Cated visitante",
					TeacherCategory.findByCode("professor-catedratico-visitante").orElse(null));
			categoryMap.put("Docentes" + "Professor Associado",
					TeacherCategory.findByCode("professor-associado").orElse(null));
			categoryMap.put("Docentes" + "Prof Assoc Convidado",
					TeacherCategory.findByCode("professor-associado-convidado").orElse(null));
			categoryMap.put("Docentes" + "Prof Ass Convi Agreg",
					TeacherCategory.findByCode("professor-associado-convidado").orElse(null));
			categoryMap.put("Docentes" + "Prof Assoc C\\ Agreg",
					TeacherCategory.findByCode("professor-associado").orElse(null));
			categoryMap.put("Docentes" + "Professor Auxiliar",
					TeacherCategory.findByCode("professor-auxiliar").orElse(null));
			categoryMap.put("Docentes" + "Prof Auxiliar Convid",
					TeacherCategory.findByCode("prof-auxiliar-convidado").orElse(null));

			categoryMap.put("Docentes" + "Prof Auxiliar Agreg.",
					TeacherCategory.findByCode("professor-auxiliar").orElse(null));
			categoryMap.put("Docentes" + "Assistente Convidado",
					TeacherCategory.findByCode("assistente-convidado").orElse(null));

			categoryMap.put("Investigadores" + "Professor Auxiliar",
					TeacherCategory.findByCode("professor-auxiliar").orElse(null));
			categoryMap.put("Investigadores" + "Prof Auxiliar Convid",
					TeacherCategory.findByCode("prof-auxiliar-convidado").orElse(null));

			categoryMap.put("Investigadores" + "Investig Coordenador",
					TeacherCategory.findByCode("investigador-coordenador").orElse(null));
            categoryMap.put("Investigadores" + "Invest Coord Convid",
                    TeacherCategory.findByCode("investigador-coordenador-convidado").orElse(null));
			categoryMap.put("Investigadores" + "Investigador Princip",
					TeacherCategory.findByCode("investigador-principal").orElse(null));
			categoryMap.put("Investigadores" + "Investig Auxiliar",
					TeacherCategory.findByCode("investigador-auxiliar").orElse(null));
			categoryMap.put("Investigadores" + "Investig Aux Convid",
					TeacherCategory.findByCode("investigador-auxiliar-convidado").orElse(null));

			categoryMap.put("Bolseiros" + "Assistente Convidado",
					TeacherCategory.findByCode("assistente-convidado").orElse(null));
			categoryMap.put("Bolseiros" + "Prof Auxiliar Convid",
					TeacherCategory.findByCode("prof-auxiliar-convidado").orElse(null));
			categoryMap.put("Bols. Investigação" + "Investig Auxiliar",
					TeacherCategory.findByCode("investigador-auxiliar").orElse(null));

		}
		return categoryMap.get(categoryType + name);
	}

	private boolean isValidAndOverlaps(final ColaboratorSituation colaboratorSituation, final Interval interval) {
		LocalDate beginDate = getJsonDate(colaboratorSituation.beginDate());
		Interval situationInterval = null;
		if (!Strings.isNullOrEmpty(colaboratorSituation.endDate())) {
			LocalDate endDate = getJsonDate(colaboratorSituation.endDate());
			if (endDate.isBefore(beginDate)) {
				return false;
			}
			situationInterval = new Interval(beginDate.toDateTimeAtStartOfDay(),
					endDate.plusDays(1).toDateTimeAtStartOfDay());
		}
		return beginDate != null && ((situationInterval != null && situationInterval.overlaps(interval))
				|| (situationInterval == null && !beginDate.isAfter(interval.getEnd().toLocalDate())));
	}

	private LocalDate getJsonDate(String dateString) {
		return Strings.isNullOrEmpty(dateString) ? null : LocalDate.parse(dateString, ISODateTimeFormat.date());
	}

	private ColaboratorSituation getDominantColaboratorSituation(
			SortedSet<ColaboratorSituation> validColaboratorSituations, Interval semesterInterval) {
		for (ColaboratorSituation colaboratorSituation : validColaboratorSituations) {
			int activeDays = getActiveDays(colaboratorSituation, semesterInterval);
			if (activeDays > minimumDaysForActivity) {
				return colaboratorSituation;
			}
		}
		return validColaboratorSituations.first();
	}

	private int getActiveDays(ColaboratorSituation colaboratorSituation, Interval semesterInterval) {
		LocalDate situationBeginDate = getJsonDate(colaboratorSituation.beginDate());
		LocalDate beginDate = situationBeginDate.isBefore(semesterInterval.getStart().toLocalDate())
				? semesterInterval.getStart().toLocalDate()
				: situationBeginDate;
		LocalDate situationEndDate = getJsonDate(colaboratorSituation.endDate());
		LocalDate endDate = situationEndDate == null
				|| situationEndDate.isAfter(semesterInterval.getEnd().toLocalDate())
						? semesterInterval.getEnd().toLocalDate()
						: situationEndDate;

		int activeDays = new Interval(beginDate.toDateTimeAtStartOfDay(), endDate.toDateTimeAtStartOfDay())
				.toPeriod(PeriodType.days()).getDays() + 1;
		return activeDays;
	}

	private Department getDominantDepartment(Person person, ExecutionSemester semester) {
		SortedSet<EmployeeContract> contracts = new TreeSet<EmployeeContract>(new Comparator<EmployeeContract>() {
			@Override
			public int compare(EmployeeContract ec1, EmployeeContract ec2) {
				int compare = ec1.getBeginDate().compareTo(ec2.getBeginDate());
				return compare == 0 ? ec1.getExternalId().compareTo(ec2.getExternalId()) : compare;
			}
		});
		Interval semesterInterval = semester.getAcademicInterval().toInterval();
		contracts.addAll(((Collection<EmployeeContract>) person
				.getParentAccountabilities(AccountabilityTypeEnum.WORKING_CONTRACT, EmployeeContract.class))
						.stream()
						.filter(ec -> ec.belongsToPeriod(semesterInterval.getStart().toYearMonthDay(),
								semesterInterval.getEnd().toYearMonthDay()))
						.filter(Objects::nonNull).collect(Collectors.toSet()));

		Department firstDepartmentUnit = null;
		for (EmployeeContract employeeContract : contracts) {
			Department employeeDepartmentUnit = getEmployeeDepartmentUnit(employeeContract.getUnit());
			if (employeeDepartmentUnit != null) {
				Interval contractInterval = new Interval(
						employeeContract.getBeginDate().toLocalDate().toDateTimeAtStartOfDay(),
						employeeContract.getEndDate() == null ? new DateTime(Long.MAX_VALUE)
								: employeeContract.getEndDate().toLocalDate().toDateTimeAtStartOfDay().plusMillis(1));
				Interval overlap = semesterInterval.overlap(contractInterval);
				if (overlap != null) {
					int days = overlap.toPeriod(PeriodType.days()).getDays() + 1;
					if (days > minimumDaysForActivity) {
						return employeeDepartmentUnit;
					}
					if (firstDepartmentUnit == null) {
						firstDepartmentUnit = employeeDepartmentUnit;
					}
				}
			}
		}
		return firstDepartmentUnit;
	}

	private Department getEmployeeDepartmentUnit(Unit unit) {
		Collection<Unit> parentUnits = unit.getParentUnits();
		if (unitDepartment(unit)) {
			return ((DepartmentUnit) unit).getDepartment();
		} else if (!parentUnits.isEmpty()) {
			for (Unit parentUnit : parentUnits) {
				if (unitDepartment(parentUnit)) {
					return ((DepartmentUnit) parentUnit).getDepartment();
				} else if (parentUnit.hasAnyParentUnits()) {
					Department department = getEmployeeDepartmentUnit(parentUnit);
					if (department != null) {
						return department;
					}
				}
			}
		}
		return null;
	}

	private boolean unitDepartment(Unit unit) {
		return unit.isDepartmentUnit() && ((DepartmentUnit) unit).getDepartment() != null;
	}

	public Double getWeeklyLessonHours(ColaboratorSituation colaboratorSituation, Interval interval,
			TeacherCategory teacherCategory) {
		if (colaboratorSituation.endSituation() || (colaboratorSituation.serviceExemption())// && !hasMandatoryCredits())
				|| !colaboratorSituation.inExercise()) {
			return Double.valueOf(0);
		}

		if (teacherCategory != null) {
			if (Strings.isNullOrEmpty(colaboratorSituation.fulltimeequivalent())) {
				if (isTeacherMonitorCategory(colaboratorSituation, teacherCategory)) {
					return Double.valueOf(4);
				} else if (isTeacherInvitedCategory(colaboratorSituation, teacherCategory)) {
					return Double.valueOf(12);
				} else {
					return Double.valueOf(9);
				}
			}
			BigDecimal fullTimeEquivalent = new BigDecimal(colaboratorSituation.fulltimeequivalent());
			Boolean exclusive = fullTimeEquivalent.equals(new BigDecimal(100));

			if (fullTimeEquivalent != null) {
				if (fullTimeEquivalent.compareTo(BigDecimal.ONE) >= 0) {
					if (isTeacherMonitorCategory(colaboratorSituation, teacherCategory)) {
						return Double.valueOf(4);
					} else if (isTeacherInvitedCategory(colaboratorSituation, teacherCategory)) {
						return exclusive ? Double.valueOf(18) : Double.valueOf(12);
					} else {
						return Double.valueOf(9);
					}
				} else {
					return fullTimeEquivalent.multiply(new BigDecimal(12)).doubleValue();
				}
			}
		}
		return Double.valueOf(12);
	}

	protected boolean isTeacherCategoryType(ColaboratorSituation colaboratorSituation) {
		return colaboratorSituation.categoryTypeName().equals("Docentes");
	}

	private boolean isTeacherMonitorCategory(ColaboratorSituation colaboratorSituation,
			TeacherCategory teacherCategory) {
		return isTeacherCategoryType(colaboratorSituation)
				&& teacherCategory.getName().anyMatch(name -> name.matches("(?i).*Monitor.*"));
	}

	private boolean isTeacherInvitedCategory(ColaboratorSituation colaboratorSituation,
			TeacherCategory teacherCategory) {
		return isTeacherCategoryType(colaboratorSituation)
				&& !isTeacherMonitorCategory(colaboratorSituation, teacherCategory)
				&& (teacherCategory.getName().anyMatch(name -> name.matches("(?i).*Convidado.*")
						|| name.matches("(?i).*Equip.*") || name.matches("(?i).*Colaborador.*")));
	}
}
