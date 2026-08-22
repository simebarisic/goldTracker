package com.simebarisic.goldtracker.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "run_log")
public class RunLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    // 'running' | 'ok' | 'partial' | 'error'
    @Column(nullable = false)
    private String status = "running";

    @Column(name = "pages_checked")
    private Integer pagesChecked = 0;

    @Column(name = "products_found")
    private Integer productsFound = 0;

    @Column(name = "changes_found")
    private Integer changesFound = 0;

    @Column(name = "error_message")
    private String errorMessage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getPagesChecked() { return pagesChecked; }
    public void setPagesChecked(Integer pagesChecked) { this.pagesChecked = pagesChecked; }

    public Integer getProductsFound() { return productsFound; }
    public void setProductsFound(Integer productsFound) { this.productsFound = productsFound; }

    public Integer getChangesFound() { return changesFound; }
    public void setChangesFound(Integer changesFound) { this.changesFound = changesFound; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
