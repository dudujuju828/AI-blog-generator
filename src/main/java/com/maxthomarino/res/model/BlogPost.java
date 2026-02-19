package com.maxthomarino.res.model;

import java.util.List;

public record BlogPost(String title, String date, String description, List<String> tags, String content, String slug) {
}
