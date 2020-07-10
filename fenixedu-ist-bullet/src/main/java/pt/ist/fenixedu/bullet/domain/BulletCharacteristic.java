package pt.ist.fenixedu.bullet.domain;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.spaces.domain.Space;

public class BulletCharacteristic extends BulletObject {
    String characteristic;

    private static Map<Pattern, String> patterns = new HashMap<>();
    //TODO refactor pattern map? Though for encapsulation it's good that it is in BC class

    static {
        patterns.put(Pattern.compile("(?i)gabinete"), "Gabinete");
        patterns.put(Pattern.compile("(?i)lab"), "Laboratório");
        patterns.put(Pattern.compile("(?i)aula"), "Sala de Aula");
        patterns.put(Pattern.compile("(?i)lti|estudo"), "Sala de Estudo");
        patterns.put(Pattern.compile("(?i)doutorand"), "Sala de Doutorandos");
        patterns.put(Pattern.compile("(?i)professor"), "Sala de Professores");
        patterns.put(Pattern.compile("(?i)bolseir"), "Sala de Bolseiros");
        patterns.put(Pattern.compile("(?i)ecumenico"), "Espaço Ecuménico");
        patterns.put(Pattern.compile("(?i)anfiteatro"), "Anfiteatro");
        patterns.put(Pattern.compile("(?i)secretar"), "Secretariado");
        patterns.put(Pattern.compile("(?i)espaco\\s+24h"), "Espaço 24H");
        patterns.put(Pattern.compile("(?i)reunioes"), "Sala de Reuniões");
        patterns.put(Pattern.compile("(?i)convivio"), "Sala de Convívio");
        patterns.put(Pattern.compile("(?i)servicos"), "Serviços");
        patterns.put(Pattern.compile("(?i)projector"), "Projector");
        patterns.put(Pattern.compile("(?i)apoio"), "Sala de Apoio");
        patterns.put(Pattern.compile("(?i)silencio"), "Sala de Silêncio");
        patterns.put(Pattern.compile("(?i)investig"), "Sala de Investigação");
        patterns.put(Pattern.compile("(?i)lti|computador|workstation"), "Sala de Computadores");
        patterns.put(Pattern.compile("(?i)biblio"), "Biblioteca");
        patterns.put(Pattern.compile("(?i)subdivisao"), "Subdivisão de Sala");
        patterns.put(Pattern.compile("(?i)exame"), "Sala de Exames");
        patterns.put(Pattern.compile("(?i)antecamara"), "Antecâmara");
    }

    private BulletCharacteristic(String characteristic) {
        this.characteristic = characteristic;
    }

    public static Stream<BulletCharacteristic> all(final DumpContext context) {
        /*XXX filtering the characteristics based on whether they are present in our collection of rooms
         * because some characteristics represent undesirable/erroneous rooms (Antecâmara, Secretariado...)
         * Once sure these rooms are not included then simply return the remaining map values */
        Set<String> present = new HashSet<>();
//        Set<String> remaining = new HashSet<>(patterns.values());
        Iterator<BulletRoom> rooms = context.all(BulletRoom.class).iterator();
        while (/*!remaining.isEmpty() || */ rooms.hasNext()) {
            Set<String> c = getCharacteristics(rooms.next().space).collect(Collectors.toSet());
  //          remaining.removeAll(c);
            present.addAll(c);
        }
        return present.stream().map(BulletCharacteristic::new);
    }

    @Override
    public void populateSlots(final DumpContext context, final LinkedHashMap<String, String> slots) {
        slots.put(BulletObjectTag.NAME.unit(), characteristic);
    }

    public static Stream<String> getCharacteristics(Space space) {
        String[] data = {
                space.getName(),
                space.<String>getMetadata("description").orElse(null),
                space.<String>getMetadata("observations").orElse(null),
                space.getClassification().getName().getContent(),
                space.<Integer>getMetadata("examCapacity").orElse(0) > 0 ? "exame" : null
        };
        final String blob = Normalizer.normalize(Stream.of(data).filter(Objects::nonNull)
                .collect(Collectors.joining("|")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return patterns.keySet().stream().filter(p -> p.matcher(blob).find()).map(patterns::get);
    }

}
