Migrating from Fenix 3 to FenixEdu Academic 4 and FenixEdu IST Vigilancies 4

Run the following SQL Statements:

```sql
alter table `VIGILANCY` add `OID_ATTENDED_WRITTEN_EVALUATION` bigint unsigned;
update VIGILANCY set OID_ATTENDED_WRITTEN_EVALUATION = OID_WRITTEN_EVALUATION where STATUS = 'ATTENDED';
```