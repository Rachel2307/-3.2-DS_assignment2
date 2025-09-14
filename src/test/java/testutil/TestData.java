package testutil;

public final class TestData {
    private TestData() {}

    public static String adelaideJson() {
        return "{\n" +
                "  \"id\":\"IDS60901\",\n" +
                "  \"name\":\"Adelaide (West Terrace)\",\n" +
                "  \"state\":\"SA\",\n" +
                "  \"time_zone\":\"CST\",\n" +
                "  \"lat\":-34.9,\n" +
                "  \"lon\":138.6,\n" +
                "  \"local_date_time\":\"15/04:00pm\",\n" +
                "  \"local_date_time_full\":\"20230715160000\",\n" +
                "  \"air_temp\":13.0,\n" +
                "  \"apparent_t\":9.5,\n" +
                "  \"cloud\":\"Partly cloudy\",\n" +
                "  \"dewpt\":5.7,\n" +
                "  \"press\":1023.9,\n" +
                "  \"rel_hum\":60,\n" +
                "  \"wind_dir\":\"S\",\n" +
                "  \"wind_spd_kmh\":15,\n" +
                "  \"wind_spd_kt\":8\n" +
                "}";
    }

    public static String sydneyJson() {
        return "{\n" +
                "  \"id\":\"IDS60902\",\n" +
                "  \"name\":\"Sydney (Observatory Hill)\",\n" +
                "  \"state\":\"NSW\",\n" +
                "  \"time_zone\":\"AEST\",\n" +
                "  \"lat\":-33.86,\n" +
                "  \"lon\":151.21,\n" +
                "  \"local_date_time\":\"15/04:00pm\",\n" +
                "  \"local_date_time_full\":\"20230715160000\",\n" +
                "  \"air_temp\":18.0,\n" +
                "  \"apparent_t\":17.0,\n" +
                "  \"cloud\":\"Clear\",\n" +
                "  \"dewpt\":10.0,\n" +
                "  \"press\":1018.0,\n" +
                "  \"rel_hum\":55,\n" +
                "  \"wind_dir\":\"NE\",\n" +
                "  \"wind_spd_kmh\":20,\n" +
                "  \"wind_spd_kt\":11\n" +
                "}";
    }

    public static String malformedJson() { return "{ \"id\": \"BAD\", "; }

    public static String emptyBody() { return ""; }
}
