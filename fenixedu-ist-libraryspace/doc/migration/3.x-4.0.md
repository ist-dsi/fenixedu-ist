Migrating from Fenix 3 to FenixEdu Academic 4 and FenixEdu Library Space 4

Run the following SQL Statements:

```sql
update FF$DOMAIN_CLASS_INFO set DOMAIN_CLASS_NAME = 'pt.ist.fenixedu.libraryattendance.space.SpaceAttendances' where DOMAIN_CLASS_NAME = 'org.fenixedu.academic.domain.space.SpaceAttendances';
```
