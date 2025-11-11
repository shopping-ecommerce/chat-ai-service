package iuh.fit.se.dto;


import java.util.List;


public class ProductSearchPayload {
    public String type = "product_list";
    public String message; // optional: mô tả ngắn
    public List<Item> items;


    public static class Item {
        public String id;
        public String name;
        public Double price; // VND
        public Double discount; // percent
        public String url; // "/product/{id}"
        public String imageUrl; // ảnh đại diện
    }
}