package pt.ist.fenixedu.bullet.domain;

import java.util.LinkedHashMap;

import com.google.gson.JsonObject;

public abstract class BulletObject {

    //TODO add key method dependent on slots + a key tag and wrap objects to get their keys?
    //TODO it's useless work but also clearer/cleaner

    public LinkedHashMap<String, String> slots;

    public LinkedHashMap<String, String> slots(final DumpContext context) {
        if (slots == null) {
            slots = new LinkedHashMap<>();
            populateSlots(context, slots);
        }
        return slots;
    }

    protected abstract void populateSlots(final DumpContext context, LinkedHashMap<String, String> slots);

    public String tag() {
        return BulletObjectTag.of(this.getClass()).unit();
    }

    public JsonObject toJson(final DumpContext context) {
        JsonObject obj = new JsonObject();
        slots(context).forEach(obj::addProperty);
        return obj;
    }

}
