package pt.ist.fenixedu.integration.task;

import java.io.File;
import java.nio.file.Files;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.sap.client.SapFinantialClient;

public class SendJsonToSap extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final File file = new File("/home/rcro/Documents/fenix/sap/dataToSend.json");
        String fileContent = new String(Files.readAllBytes(file.toPath()));
        JsonObject data = new JsonParser().parse(fileContent).getAsJsonObject();

        JsonObject result = SapFinantialClient.comunicate(data);

        JsonArray documents = result.getAsJsonArray("documents");

        taskLog("#####################");
        documents.forEach(item -> {
            JsonObject json = (JsonObject) item;
            taskLog(json.toString());
        });

        JsonArray customers = result.getAsJsonArray("customers");

        taskLog("#####################");
        customers.forEach(item -> {
            JsonObject json = (JsonObject) item;
            taskLog(json.toString());
        });
    }
}
