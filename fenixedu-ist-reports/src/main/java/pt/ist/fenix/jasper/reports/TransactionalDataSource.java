/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Report Templates.
 *
 * FenixEdu IST Report Templates is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Report Templates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Report Templates.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenix.jasper.reports;

import java.util.Collection;
import java.util.concurrent.Callable;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class TransactionalDataSource implements JRDataSource {

    private final Atomic atomic = new AtomicInstance(TxMode.READ, true);
    private final JRBeanCollectionDataSource wrap;

    public TransactionalDataSource(Collection beanCollection) {
        wrap = new JRBeanCollectionDataSource(beanCollection);
    }

    @Override
    public boolean next() throws JRException {
        return wrap.next();
    }

    private class FieldCallable implements Callable<Void> {

        final JRField field;
        Object result;

        private FieldCallable(final JRField field) {
            this.field = field;
        }

        @Override
        public Void call() throws Exception {
            result = wrap.getFieldValue(field);
            return null;
        }

    }

    @Override
    public Object getFieldValue(final JRField field) throws JRException {
        try {
            final FieldCallable callable = new FieldCallable(field);
            FenixFramework.getTransactionManager().withTransaction(callable, atomic);
            return callable.result;
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

}
