package io.shubham0204;

public class Main {

    private static final String ROLE_NL_PARSER = "You are an expert at parsing data from a paragraph and transforming it to structured JSON";

    public static void main(String[] args) {
        var apiHost = "localhost";
        var apiPort = 8080;
        var session = new GuidedLanguageModelClient(ROLE_NL_PARSER, apiHost, apiPort);

        var query = SampleQueries.ECOMMERCE_PRODUCT;

        ECommerceProduct product = session.respond(query, ECommerceProduct.class);
        System.out.println(product);
    }
}