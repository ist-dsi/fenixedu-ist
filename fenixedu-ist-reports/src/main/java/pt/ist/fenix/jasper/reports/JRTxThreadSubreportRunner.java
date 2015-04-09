package pt.ist.fenix.jasper.reports;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.fill.JRBaseFiller;
import net.sf.jasperreports.engine.fill.JRFillSubreport;
import net.sf.jasperreports.engine.fill.JRSubreportRunResult;
import net.sf.jasperreports.engine.fill.JRSubreportRunnable;
import net.sf.jasperreports.engine.fill.JRSubreportRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import pt.ist.fenixframework.FenixFramework;

public class JRTxThreadSubreportRunner extends JRSubreportRunnable implements JRSubreportRunner {

    private static final Log log = LogFactory.getLog(JRTxThreadSubreportRunner.class);

    private final JRBaseFiller subreportFiller;

    private Thread fillThread;

    public JRTxThreadSubreportRunner(JRFillSubreport fillSubreport, JRBaseFiller subreportFiller) {
        super(fillSubreport);
        this.subreportFiller = subreportFiller;
    }

    @Override
    public boolean isFilling() {
        return fillThread != null;
    }

    @Override
    public JRSubreportRunResult start() {
        fillThread =
                new Thread(() -> FenixFramework.atomic(this), subreportFiller.getJasperReport().getName() + " subreport filler");

        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": starting thread " + fillThread);
        }

        fillThread.start();

        return waitResult();
    }

    @Override
    public JRSubreportRunResult resume() {
        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": notifying to continue");
        }

        //notifing the subreport fill thread that it can continue on the next page
        subreportFiller.notifyAll();

        return waitResult();
    }

    protected JRSubreportRunResult waitResult() {
        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": waiting for fill result");
        }

        try {
            // waiting for the subreport fill thread to fill the current page
            subreportFiller.wait(); // FIXME maybe this is useless since you cannot enter 
                                    // the synchornized bloc if the subreport filler hasn't 
                                    // finished the page and passed to the wait state.
        } catch (InterruptedException e) {
            if (log.isErrorEnabled()) {
                log.error("Fill " + subreportFiller + ": exception", e);
            }

            throw new JRRuntimeException("Error encountered while waiting on the report filling thread.", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": notified of fill result");
        }

        return runResult();
    }

    @Override
    public void reset() {
        fillThread = null;
    }

    @Override
    public void cancel() throws JRException {
        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": notifying to continue on cancel");
        }

        // notifying the subreport filling thread that it can continue.
        // it will stop anyway when trying to fill the current band
        subreportFiller.notifyAll();

        if (isRunning()) {
            if (log.isDebugEnabled()) {
                log.debug("Fill " + subreportFiller + ": still running, waiting");
            }

            try {
                //waits until the master filler notifies it that can continue with the next page
                subreportFiller.wait();
            } catch (InterruptedException e) {
                if (log.isErrorEnabled()) {
                    log.error("Fill " + subreportFiller + ": exception", e);
                }

                throw new JRException("Error encountered while waiting on the subreport filling thread.", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("Fill " + subreportFiller + ": wait ended");
            }
        }
    }

    @Override
    public void suspend() throws JRException {
        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": notifying on suspend");
        }

        //signals to the master filler that is has finished the page
        subreportFiller.notifyAll();

        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": waiting to continue");
        }

        try {
            //waits until the master filler notifies it that can continue with the next page
            subreportFiller.wait();
        } catch (InterruptedException e) {
            if (log.isErrorEnabled()) {
                log.error("Fill " + subreportFiller + ": exception", e);
            }

            throw new JRException("Error encountered while waiting on the subreport filling thread.", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": notified to continue");
        }
    }

    @Override
    public void run() {
        super.run();

        if (log.isDebugEnabled()) {
            log.debug("Fill " + subreportFiller + ": notifying of completion");
        }

        synchronized (subreportFiller) {
            //main filler notified that the subreport has finished
            subreportFiller.notifyAll();
        }

/*
        if (error != null)
        {
            synchronized (subreportFiller)
            {
                //if an exception occured then we should notify the main filler that we have finished the subreport
                subreportFiller.notifyAll();
            }
        }
        */
    }
}
