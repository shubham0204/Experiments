/*
 * Copyright 2025 Shubham Panchal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

@Guide(description = "An e-commerce product with various attributes")
public class ECommerceProduct {
    @NotNull
    @Guide(description = "The unique identifier or SKU of the product")
    public String sku;

    @NotNull
    @Guide(description = "The name or title of the product")
    public String name;

    @Guide(description = "A short description of the product")
    public String description;

    @NotNull
    @Positive
    @Guide(description = "The price of the product in its local currency")
    public double price;

    @Guide(description = "The currency code for the price, e.g. 'USD', 'INR'")
    public String currency;

    @Guide(description = "The brand or manufacturer of the product")
    public String brand;

    @Guide(description = "The main category of the product, e.g. 'Electronics', 'Clothing'")
    public String category;

    @Guide(description = "A list of tags or keywords for search optimization")
    public List<String> tags;

    @PositiveOrZero
    @Guide(description = "The number of items available in stock")
    public int stockQuantity;

    @Guide(description = "Whether the product is currently available for purchase")
    public boolean available;

    @Guide(description = "The average customer rating for the product, from 0.0 to 5.0")
    public double averageRating;

    @Guide(description = "The number of reviews the product has received")
    public int reviewCount;

    @Guide(description = "A list of image URLs for the product")
    public List<String> imageUrls;

    @Guide(description = "The weight of the product in kilograms")
    public double weightKg;

    @Guide(description = "The dimensions of the product in centimeters, formatted as LxWxH")
    public String dimensionsCm;

    @Guide(description = "The country where the product is manufactured")
    public String countryOfOrigin;

    @Override
    public String toString() {
        return "ECommerceProduct{" +
                "sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", brand='" + brand + '\'' +
                ", category='" + category + '\'' +
                ", tags=" + tags +
                ", stockQuantity=" + stockQuantity +
                ", available=" + available +
                ", averageRating=" + averageRating +
                ", reviewCount=" + reviewCount +
                ", imageUrls=" + imageUrls +
                ", weightKg=" + weightKg +
                ", dimensionsCm='" + dimensionsCm + '\'' +
                ", countryOfOrigin='" + countryOfOrigin + '\'' +
                '}';
    }
}
