package pt.ist.fenixedu.bullet.domain;

import java.util.Objects;
import java.util.stream.Stream;

public enum BulletObjectTag {

    //TODO refactor? Seems like too much responsibility already
    ZONE("Zona", "Zonas", BulletZone.class, BulletObjectType.GENERAL),
    BUILDING("Edificio", "Edificios", BulletBuilding.class, BulletObjectType.GENERAL),
    LEVEL("Piso", "Pisos", BulletLevel.class, BulletObjectType.GENERAL),
    ROOM("Sala", "Salas", BulletRoom.class, BulletObjectType.ENTITY),
    ROOM_FULL_PATH("Sala", "Salas", BulletRoom.class, BulletObjectType.ENTITY),
    CHARACTERISTIC("Caracteristica", "Caracteristicas", BulletCharacteristic.class, BulletObjectType.GENERAL),
    AREA("AreaCientifica", "AreasCientificas", BulletArea.class, BulletObjectType.GENERAL),
    TYPOLOGY("Tipologia", "Tipologias", BulletTypology.class, BulletObjectType.GENERAL),
    DEGREE("Curso", "Cursos", BulletDegree.class, BulletObjectType.CURRICULUM),
    CURRICULAR_PLAN("PlanoCurricular", "PlanosCurriculares", BulletPlan.class, BulletObjectType.CURRICULUM),
    COURSE("Disciplina", "Disciplinas", BulletCourse.class, BulletObjectType.CURRICULUM),
    LOAD("CargaSemanal", "CargasSemanais", BulletLoad.class, BulletObjectType.SCHEDULE),
    CLASS("Turma", "Turmas", BulletClass.class, BulletObjectType.ENTITY),
    //NEWCLASS("Turma", "Turmas", NewBulletClass.class, BulletObjectType.ENTITY),
    
    NAME("Nome"),
    DESCRIPTION("Descricao"),
    ACRONYM("Sigla"),
    CODE("Codigo"),
    DEGREE_CODE("CodigoCurso"),
    MANDATORY_COURSE_CODE("CodigoDisciplinaObrigatorio"),
    OPTIONAL_COURSE_CODE("CodigoDisciplinaOpcional"),
    IMPORTANCE("Importancia"),
    CAPACITY("Capacidade"),
    EXAM_CAPACITY("CapacidadeExame"),
    ACCEPTANCE_MARGIN("MargemAceitacao"),
    COURSE_CODE("CodigoDisciplina"),
    LOAD_NAME("NomeCargaSemanal"),
    TYPE_NAME("NomeTipologia"),
    SLOTS("NumSlots"),
    REPEATS("Repeticao"),
    SHIFTS("NumTurnos"),
    WEEKS("Semanas"),
    PLAN_CODE("CodigoPlanoCurricular"),
    NUMBER_STUDENTS("NumeroAlunos"),
    MAX_LIMIT("LimiteMaximo"),
    CONSECUTIVE_LIMIT("LimiteConsecutivo"),
    ECTS("ECTS");

    private String unit, group = null;
    private Class<? extends BulletObject> entity = null;
    private BulletObjectType type = null;

    BulletObjectTag(String unit) {
        this.unit = unit;
    }

    BulletObjectTag(String unit, String group, Class<? extends BulletObject> entity, BulletObjectType type) {
        this.unit = unit;
        this.group = group;
        this.entity = entity;
        this.type = type;
    }

    public static BulletObjectTag of(Class<? extends BulletObject> target) {
        return Stream.of(BulletObjectTag.values()).filter(t -> target.equals(t.getEntityClass())).findFirst().orElse(null);
    }

    public static Stream<? extends Class<? extends BulletObject>> entities() {
        return Stream.of(BulletObjectTag.values()).map(t -> t.entity).filter(Objects::nonNull);
    }

    public static Stream<Class<? extends BulletObject>> entities(BulletObjectType type) {
        return Stream.of(BulletObjectTag.values()).filter(t -> type.equals(t.type)).map(BulletObjectTag::getEntityClass);
    }


    public String unit() {
        return unit;
    }

    public String group() {
        return group;
    }

    public Class<? extends BulletObject> entity() {
        return entity;
    }

    public BulletObjectType type() {
        return type;
    }

    public Class<? extends BulletObject> getEntityClass() {
        return entity;
    }

}
