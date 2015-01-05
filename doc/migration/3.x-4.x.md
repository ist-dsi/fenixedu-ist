Migrating from Fenix 3 to FenixEdu Academic 4 and FenixEdu Tutorships 4

Run the following SQL Statements:

```sql
update FF$DOMAIN_CLASS_INFO set DOMAIN_CLASS_NAME = replace(DOMAIN_CLASS_NAME, 'org.fenixedu.academic.domain', 'pt.ist.fenixedu.tutorship.domain') where DOMAIN_CLASS_NAME like '%Tutor%';
update FF$DOMAIN_CLASS_INFO set DOMAIN_CLASS_NAME = 'pt.ist.fenixedu.tutorship.domain.StudentHighPerformanceQueueJob' where DOMAIN_CLASS_NAME = 'org.fenixedu.academic.domain.StudentHighPerformanceQueueJob';
```
