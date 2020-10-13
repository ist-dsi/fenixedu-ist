package pt.ist.fenixedu.domain;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class SapRoot extends SapRoot_Base {
    
    private SapRoot() {
        super();
        setBennu(Bennu.getInstance());
        super.setSapDocumentNumber(Long.valueOf(0));
        setAllowCommunication(true);
    }
    
    public static SapRoot getInstance() {
        if (Bennu.getInstance().getSapRoot() == null) {
            initialize();
        }
        return Bennu.getInstance().getSapRoot();
    }

    @Atomic(mode = TxMode.WRITE)
    private static void initialize() {
        if (Bennu.getInstance().getSapRoot() == null) {
            new SapRoot();
        }
    }

    @Override
    public void setSapDocumentNumber(Long docNumber) {
        throw new DomainException("error.domain.sap.not.possible.toSetNumber");
    }

    /**
     * Increments the document number and returns it
     * 
     * @return
     */
    public Long getAndSetNextDocumentNumber() {
        Long sapDocumentNumber = getSapDocumentNumber();
        super.setSapDocumentNumber(sapDocumentNumber + 1);
        return getSapDocumentNumber();
    }

    public boolean yearIsOpen(final int year) {
        return getOpenYear() != null && getOpenYear().intValue() == year;
    }

}
