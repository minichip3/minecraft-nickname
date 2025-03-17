package net.minichip.minecraftnickname;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

public class MojangAPI {
    public static boolean doesPlayerExist(String playerName) {
        try {
            @SuppressWarnings("deprecation")
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(connection.getInputStream());
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.has("id");  // UUID가 있으면 존재하는 플레이어
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // getUUID 메서드 추가: 플레이어 이름으로 Mojang API를 호출하여 UUID 문자열을 반환
    public static String getUUID(String playerName) {
        try {
            @SuppressWarnings("deprecation")
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(connection.getInputStream());
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String trimmedUuid = jsonResponse.getString("id");
                if (trimmedUuid != null && trimmedUuid.length() == 32) {
                    return trimmedUuid.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5");
                } else {
                    return trimmedUuid;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}