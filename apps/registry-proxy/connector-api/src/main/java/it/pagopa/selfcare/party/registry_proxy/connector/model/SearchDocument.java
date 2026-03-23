package it.pagopa.selfcare.party.registry_proxy.connector.model;

import java.util.Date;
import java.util.List;

public class SearchDocument {
  private String id;
  private String title;
  private String content;
  private String category;
  private List<String> tags;
  private Date timestamp;

  // Costruttore default
  public SearchDocument() {}

  // Getters e Setters
  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }

  public List<String> getTags() { return tags; }
  public void setTags(List<String> tags) { this.tags = tags; }

  public Date getTimestamp() { return timestamp; }

  public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

}

