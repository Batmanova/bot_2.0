import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OpenWeatherMapJsonParser {
    private final static String API_CALL_TEMPLATE1 = "https://api.openweathermap.org/data/2.5/forecast?q=";
    private final static String API_KEY_TEMPLATE1 = "&units=metric&APPID=941ffdc39a7187f720686d1608004005";
    private final static String API_CALL_TEMPLATE2 = "https://api.openweathermap.org/data/2.5/weather?q=";
    private final static String API_KEY_TEMPLATE2 = "&units=metric&appid=941ffdc39a7187f720686d1608004005";
    private final static String USER_AGENT = "Mozilla/5.0";
    private final static DateTimeFormatter INPUT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final static DateTimeFormatter OUTPUT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM-dd HH:mm", Locale.US);

    public String getReadyForecast(String city) {
        String result;
        try {
            String jsonRawData = downloadJsonRawData(city, 1);
            String currentWeather = downloadJsonRawData(city, 2);
            List<String> linesOfForecast = convertRawDataToList(jsonRawData);
            result = String.format("%s:%sCurrent weather: %s%s%s", city, System.lineSeparator(), parseCurrentWeatherData(currentWeather), System.lineSeparator(), parseForecastDataFromList(linesOfForecast));
        } catch (IllegalArgumentException e) {
            return String.format("Can't find \"%s\" city", city);
        } catch (Exception e) {
            e.printStackTrace();
            return "The service is not available, please try later";
        }
        return result;
    }


    private static String downloadJsonRawData(String city, int type) throws Exception {
        String urlString;
        if (type == 1) {
            urlString = API_CALL_TEMPLATE1 + city + API_KEY_TEMPLATE1;
        } else if (type == 2) {
            urlString = API_CALL_TEMPLATE2 + city + API_KEY_TEMPLATE2;
        } else {
            throw new Exception();
        }
        URL urlObject = new URL(urlString); //создаём объект который будет содержать ссылку
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection(); ///создаём соединение
        connection.setRequestMethod("GET"); //выбираем тип запроса GET
        connection.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = connection.getResponseCode(); //проверяем по коду ответа, нормально ли обработался запрос
        if (responseCode == 404) {
            throw new IllegalArgumentException();
        }
        //считываем json в строку
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }


    private static List<String> convertRawDataToList(String data) throws Exception {
        List<String> weatherList = new ArrayList<>();
        JsonNode arrNode = new ObjectMapper().readTree(data).get("list"); //JsonNode - это один из узлов в древовидной иерархии, от которого идут ветви
        //если это действительно массив узлов то добавляем каждый узел
        if (arrNode.isArray()) {
            int i = 0;
            for (final JsonNode objNode : arrNode) {
                if (i < 8) {
                    weatherList.add(objNode.toString());
                    i++;
                }
            }
        }
        return weatherList;
    }

    private static String parseCurrentWeatherData(String currentWeather) throws Exception {
        String result = "";
        ObjectMapper objectMapper = new ObjectMapper(); //объект Jackson, который выполняет сериализацию
        try {
            JsonNode mainNode = objectMapper.readTree(currentWeather).get("main");  //для получения температуры
            JsonNode weatherNode = objectMapper.readTree(currentWeather).get("weather").get(0);
            result = formatCurrentWeatherData(weatherNode.get("main").toString(), mainNode.get("temp").asDouble()); //objNode.get("main") из узла weather
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String parseForecastDataFromList(List<String> weatherList) throws Exception {
        final StringBuffer sb = new StringBuffer();
        ObjectMapper objectMapper = new ObjectMapper(); //объект Jackson, который выполняет сериализацию
        for (String line : weatherList) {
            try {
                JsonNode mainNode = objectMapper.readTree(line).get("main");  //для получения температуры
                JsonNode weatherArrNode = objectMapper.readTree(line).get("weather");
                for (final JsonNode objNode : weatherArrNode) {
                    String dateTime = objectMapper.readTree(line).get("dt_txt").toString();
                    sb.append(formatForecastData(dateTime, objNode.get("main").toString(), mainNode.get("temp").asDouble())); //objNode.get("main") из узла weather
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    private static String formatForecastData(String dateTime, String description, double temperature) throws Exception {
        LocalDateTime forecastDateTime = LocalDateTime.parse(dateTime.replaceAll("\"", ""), INPUT_DATE_TIME_FORMAT);
        String formattedDateTime = forecastDateTime.format(OUTPUT_DATE_TIME_FORMAT);
        String formattedTemperature;
        long roundedTemperature = Math.round(temperature);
        if (roundedTemperature > 0) {
            formattedTemperature = "+" + String.valueOf(Math.round(temperature));
        } else {
            formattedTemperature = String.valueOf(Math.round(temperature));
        }
        String formattedDescription = description.replaceAll("\"", "");
        return String.format("%s   %s %s%s", formattedDateTime, formattedTemperature, formattedDescription, System.lineSeparator());
    }
    private static String formatCurrentWeatherData(String description, double temperature) throws Exception {
        String formattedTemperature;
        long roundedTemperature = Math.round(temperature);
        if (roundedTemperature > 0) {
            formattedTemperature = "+" + String.valueOf(Math.round(temperature));
        } else {
            formattedTemperature = String.valueOf(Math.round(temperature));
        }
        String formattedDescription = description.replaceAll("\"", "");
        return String.format("%s %s%s", formattedTemperature, formattedDescription, System.lineSeparator());
    }
}