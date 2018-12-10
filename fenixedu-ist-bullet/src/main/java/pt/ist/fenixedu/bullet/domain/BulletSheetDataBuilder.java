package pt.ist.fenixedu.bullet.domain;

import org.fenixedu.commons.spreadsheet.SheetData;

public class BulletSheetDataBuilder {

    final DumpContext context;

    public BulletSheetDataBuilder(final DumpContext context) {
        this.context = context;
    }
    
    public class BulletSheetData<T extends BulletObject> extends SheetData<T> {
        
        public BulletSheetData(final Iterable<T> bulletEntities) {
            super(bulletEntities);
        }
        
        @Override
        protected void makeLine(BulletObject entity) {
            entity.slots(context).forEach(this::addCell);
        }
        
    }

    public <T extends BulletObject> BulletSheetData<T> bulletSheetData(final Iterable<T> bulletEntities) {
        return new BulletSheetData<>(bulletEntities);
    }

}

