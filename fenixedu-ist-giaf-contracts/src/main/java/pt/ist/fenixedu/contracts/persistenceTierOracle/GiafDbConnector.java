package pt.ist.fenixedu.contracts.persistenceTierOracle;


public class GiafDbConnector extends DbConnector {

    private static GiafDbConnector instance = null;

    public static GiafDbConnector getInstance() {
        if (instance == null) {
            synchronized (GiafDbConnector.class) {
                if (instance == null) {
                    instance = new GiafDbConnector();
                }
            }
        }
        return instance;
    }

    @Override
    protected String dbProtocol() {
        return "jdbc:oracle:thin:@";
    }

    @Override
    protected String dbAlias() {
        return FenixIstGiafContractsConfiguration.getConfiguration().dbGiafAlias();
    }

    @Override
    protected String dbUser() {
        return FenixIstGiafContractsConfiguration.getConfiguration().dbGiafUser();
    }

    @Override
    protected String dbPass() {
        return FenixIstGiafContractsConfiguration.getConfiguration().dbGiafPass();
    }

}
