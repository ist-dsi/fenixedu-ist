Migrating from Fenix 3 to FenixEdu Academic 4 and FenixEdu IST Delegates

Before the migration run [ExportDelegates.java](https://gist.github.com/cfscosta/dd352ccfe1b78b009514) and keep the output file.

After you execute the SQL statements from fenixedu academic's guide you should run the following SQL to drop the old delegates:

```sql
delete from SENDER where ((OID >> 32) & 0xFFFF) in (select DOMAIN_CLASS_ID from FF$DOMAIN_CLASS_INFO where DOMAIN_CLASS_NAME in (
    'org.fenixedu.academic.domain.util.email.PersonFunctionSender'
));

delete from PERSISTENT_GROUP where ((OID >> 32) & 0xFFFF) in (select DOMAIN_CLASS_ID from FF$DOMAIN_CLASS_INFO where DOMAIN_CLASS_NAME in (
    'org.fenixedu.academic.domain.accessControl.PersistentDelegatesGroup',
    'org.fenixedu.academic.domain.accessControl.PersistentDelegateStudentsGroup'
));
```

If you want to keep the results from old QUC inquiries, execute the following statement:

```sql
alter table INQUIRY_ANSWER change OID_DELEGATE OLD_OID_DELEGATE bigint(20) unsigned;
```

Run the following script to import the Delegates information: [ImportDelegates.java](https://gist.github.com/cfscosta/5c93fd230d3fef1687fb).

The import process generates an output file that you should execute if you want to keep the results from the old QUC inquiries.
