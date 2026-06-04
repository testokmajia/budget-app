package com.techmanage.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    public Role() {}

    public Role(String code, String name, String description) {
        this.code = code != null ? code.trim() : null;
        this.name = name != null ? name.trim() : null;
        this.description = description != null ? description.trim() : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code != null ? code.trim() : null; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name.trim() : null; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description.trim() : null; }
}
