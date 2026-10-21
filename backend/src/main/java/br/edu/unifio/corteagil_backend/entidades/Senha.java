package br.edu.unifio.corteagil_backend.entidades;

import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Senha {
  @Id private String id;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false, unique = true)
  private String label;

  private String service;
  private String origin;
  private String status;
  private long createdAt;

  @Column(name = "queue_order")
  private long order;

  @Column(name = "return_count")
  private int returns;

  private Long calledAt;
  private Long deadline;
  private Long startedAt;
  private Long finishedAt;

  @ElementCollection
  @CollectionTable(name = "ticket_notifications")
  @OrderColumn(name = "notice_order")
  private List<Aviso> notifications = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "ticket_notified")
  private Set<String> notified = new HashSet<>();
}
