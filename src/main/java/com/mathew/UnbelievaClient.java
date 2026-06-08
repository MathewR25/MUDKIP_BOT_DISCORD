package com.mathew;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UnbelievaClient {
    private final String token;
    private final String serverId;
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public UnbelievaClient(String token, String serverId) {
        this.token = token;
        this.serverId = serverId;
        this.client = new OkHttpClient();
    }

    public long getBankBalance(String userId) {
        try {
            String url = "https://unbelievaboat.com/api/v1/guilds/" + serverId + "/users/" + userId;
            
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", token)
                    .header("Accept", "application/json")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    if (body.contains("\"bank\":")) {
                        String sub = body.substring(body.indexOf("\"bank\":") + 7);
                        String val = sub.split("[,}]")[0].trim();
                        val = val.replace("\"", "");
                        return (long) Double.parseDouble(val);
                    }
                } else {
                    System.out.println("⚠️ [Mudkip API] Error balance. Código: " + response.code());
                }
            }
        } catch (Exception e) {
            System.out.println("❌ [Mudkip API] Error al obtener saldo de " + userId + ": " + e.getMessage());
        }
        return 0;
    }

    public okhttp3.OkHttpClient getClient() {
        return this.client;
    }

    public boolean modificarSaldo(String userId, long cantidad, String motivo) {
        try {
            String url = "https://unbelievaboat.com/api/v1/guilds/" + serverId + "/users/" + userId;
            
            String signoCantidad = (cantidad > 0) ? "+" + cantidad : String.valueOf(cantidad);

            String jsonStr = "{\n" +
                    "  \"bank\": \"" + signoCantidad + "\",\n" +
                    "  \"reason\": \"" + motivo + "\"\n" +
                    "}";

            RequestBody body = RequestBody.create(jsonStr, JSON);
            
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", token)
                    .header("Accept", "application/json")
                    .patch(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return true;
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Sin respuesta de texto";
                    System.out.println("⚠️ [Mudkip API] UnbelievaBoat rechazó el PATCH. Código: " + response.code() + " | Detalles: " + errorBody);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ [Mudkip API] Error al modificar saldo de " + userId + ": " + e.getMessage());
        }
        return false;
    }
}

