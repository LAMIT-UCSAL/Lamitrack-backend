package br.com.lamit.scrapersympla;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class TesteSympla {

    public static void main(String[] args) {

        try {
            String url = "https://www.sympla.com.br/evento/churrascarva-2026/3521412";

            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();

            Element nextData = document.selectFirst("script#__NEXT_DATA__");

            if (nextData != null) {

                // Pega o JSON que está dentro do __NEXT_DATA__
                String json = nextData.html();

                ObjectMapper mapper = new ObjectMapper();

                // Transforma o JSON em algo navegável
                JsonNode root = mapper.readTree(json);

                // Caminho até os dados do evento
                JsonNode event = root
                        .path("props")
                        .path("pageProps")
                        .path("hydrationData")
                        .path("eventHydration")
                        .path("event");

                System.out.println("Nome: " + event.path("name").asText());
                System.out.println("ID: " + event.path("id").asText());
                System.out.println("Data início: " + event.path("startDate").asText());
                System.out.println("Data fim: " + event.path("endDate").asText());
                System.out.println("Descrição: " + event.path("strippedDetail").asText());
                System.out.println("Categoria: " +
                        event.path("eventsCategory").path("name").asText());
                System.out.println("Organizador: " +
                        event.path("eventsHost").path("name").asText());
                System.out.println("URL: " + event.path("newUrl").asText());

            } else {
                System.out.println("Não encontrou o __NEXT_DATA__.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}