Please run the following SQL statements **after** the *fenixedu-academic* migration.

```sql
alter table `MASTER_DEGREE_PROOF_VERSION_EXTERNAL_JURY` change `
OID_EXTERNAL_CONTRACT` `OID_ACCOUNTABILITY` bigint unsigned;

alter table `MASTER_DEGREE_THESIS_DATA_VERSION_EXTERNAL_ASSISTENT_GUIDER` change `OID_EXTERNAL_CONTRACT` `OID_ACCOUNTABILITY` bigint unsigned;

alter table `MASTER_DEGREE_THESIS_DATA_VERSION_EXTERNAL_GUIDER` change `OID_EXTERNAL_CONTRACT` `OID_ACCOUNTABILITY` bigint unsigned;
```