package pt.ist.fenixedu.bullet.domain;

public enum BulletObjectType {

    GENERAL("gerais"), ENTITY("entidades"), CURRICULUM("curriculo"), SCHEDULE("aulas");

    private String title;

    BulletObjectType(String title) {
        this.title = title;
    }

    public String title() {
        return this.title;
    }

}
