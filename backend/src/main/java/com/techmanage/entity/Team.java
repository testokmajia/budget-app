package com.techmanage.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String department;

    @Column(length = 50)
    private String leader;

    @Column(columnDefinition = "TEXT")
    private String members;

    @Column(columnDefinition = "TEXT")
    private String systems;

    @Column(name = "system_owners", columnDefinition = "TEXT")
    private String systemOwners;

    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }
    public String getMembers() { return members; }
    public void setMembers(String members) { this.members = members; }
    public String getSystems() { return systems; }
    public void setSystems(String systems) { this.systems = systems; }
    public String getSystemOwners() { return systemOwners; }
    public void setSystemOwners(String systemOwners) { this.systemOwners = systemOwners; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
