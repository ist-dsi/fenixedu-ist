Giaf Integration Module for Generating Financial Documents
===

This module provides mechanisms for generating invoices and receipts in a
remote GIAF installation. 


##Configuration

To configure this module set an appropriate value to the following property 
in your applications configuration.properties file: 

```
pt.indra.mygiaf.invoice.dir
```

This is where this module will store references to objects in the GIAF 
system as well as the resulting documents.


#Using the Client

This module provides a simple API for generating and retrieving documents 
from a GIAF installation in the following class:

```
pt.ist.fenixedu.giaf.invoices.GiafInvoice
```

To create an invoice simply call the createInvoice method with either an 
event or a transaction. To obtain the resulting document simply call the 
invoiceStream method.
