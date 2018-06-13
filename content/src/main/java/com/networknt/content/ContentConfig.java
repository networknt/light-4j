package com.networknt.content;

/**
 * Created by Ricardo Pina Arellano on 13/06/18.
 */
public class ContentConfig {
  boolean enabled;

  String contentType;

  String description;

  public ContentConfig() { }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
