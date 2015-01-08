Migrating from Fenix 3 to FenixEdu Academic 4 and FenixEdu IST Delegates

Before the migration run [ExportDelegates.java](https://gist.github.com/cfscosta/dd352ccfe1b78b009514) and keep the output file.

If you want to keep the results from old QUC inquiries, run the following SQL Statements after you execute the SQL statements from fenixedu academic's guide:

```sql
alter table INQUIRY_ANSWER change OID_DELEGATE OLD_OID_DELEGATE bigint(20) unsigned;
```

Run the following script to import the Delegates information: [ImportDelegates.java](https://gist.github.com/cfscosta/5c93fd230d3fef1687fb).

The import process generates an output file that you should execute if you want to keep the results from the old QUC inquiries.