import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final int requestLimit;
    private final long timeIntervalMillis;
    private final HttpClient httpClient;
    private final Gson gson;
    private final Queue<Long> requestTimestamps;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeIntervalMillis = timeUnit.toMillis(1);
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.requestTimestamps = new LinkedList<>();
    }

    public synchronized String createDocument(Document document, String signature) throws IOException, InterruptedException {
        while (true) {
            long currentTime = Instant.now().toEpochMilli();

            if (requestTimestamps.size() < requestLimit) {
                requestTimestamps.add(currentTime);
                break;
            }

            long oldestRequestTime = requestTimestamps.peek();
            if (currentTime - oldestRequestTime > timeIntervalMillis) {
                requestTimestamps.poll();
                requestTimestamps.add(currentTime);
                break;
            } else {
                long waitTime = timeIntervalMillis - (currentTime - oldestRequestTime);
                wait(waitTime);
            }
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(document)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public static class Document {
        @SerializedName("description")
        private Description description;
        @SerializedName("doc_id")
        private String docId;
        @SerializedName("doc_status")
        private String docStatus;
        @SerializedName("doc_type")
        private String docType;
        @SerializedName("importRequest")
        private boolean importRequest;
        @SerializedName("owner_inn")
        private String ownerInn;
        @SerializedName("participant_inn")
        private String participantInn;
        @SerializedName("producer_inn")
        private String producerInn;
        @SerializedName("production_date")
        private String productionDate;
        @SerializedName("production_type")
        private String productionType;
        @SerializedName("products")
        private Product[] products;
        @SerializedName("reg_date")
        private String regDate;
        @SerializedName("reg_number")
        private String regNumber;

    }

    public static class Description {
        @SerializedName("participantInn")
        private String participantInn;

    }

    public static class Product {
        @SerializedName("certificate_document")
        private String certificateDocument;
        @SerializedName("certificate_document_date")
        private String certificateDocumentDate;
        @SerializedName("certificate_document_number")
        private String certificateDocumentNumber;
        @SerializedName("owner_inn")
        private String ownerInn;
        @SerializedName("producer_inn")
        private String producerInn;
        @SerializedName("production_date")
        private String productionDate;
        @SerializedName("tnved_code")
        private String tnvedCode;
        @SerializedName("uit_code")
        private String uitCode;
        @SerializedName("uitu_code")
        private String uituCode;

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 5);

        Document document = new Document();

        String signature = "signature";
        String response = api.createDocument(document, signature);
        //System.out.println(response);
    }
}


