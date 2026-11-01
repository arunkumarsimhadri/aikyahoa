package com.aikya.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "title_te", length = 250)
    private String titleTe;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_te", columnDefinition = "TEXT")
    private String contentTe;

    @Column(name = "category", nullable = false, length = 64)
    private String category = "GENERAL";

    @Column(name = "priority", nullable = false, length = 16)
    private String priority = "NORMAL";

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "published_by_user_id")
    private Long publishedByUserId;

    @Column(name = "published_by_name", length = 150)
    private String publishedByName;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    public Notice() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleTe() {
        return titleTe;
    }

    public void setTitleTe(String titleTe) {
        this.titleTe = titleTe;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentTe() {
        return contentTe;
    }

    public void setContentTe(String contentTe) {
        this.contentTe = contentTe;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public Long getPublishedByUserId() {
        return publishedByUserId;
    }

    public void setPublishedByUserId(Long publishedByUserId) {
        this.publishedByUserId = publishedByUserId;
    }

    public String getPublishedByName() {
        return publishedByName;
    }

    public void setPublishedByName(String publishedByName) {
        this.publishedByName = publishedByName;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
