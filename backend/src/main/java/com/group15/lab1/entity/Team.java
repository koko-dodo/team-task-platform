package com.group15.lab1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * 团队实体。
 * 
 * 团队由某个用户创建，创建者自动成为 Owner。
 */
@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String status;

    /**
     * 团队拥有者（Owner）。关联到 users 表。
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private Set<TeamMember> teamMembers = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDate updatedAt;    

    public Team() {
    }

    public Team(String name, User owner) {
        this.name = name;
        this.owner = owner;
        this.status = TeamStatus.ACTIVE.name();
    }

    @PrePersist
    public void onCreate() {
        LocalDate now = LocalDate.now();
        if (status == null || status.isBlank()) {
            status = TeamStatus.ACTIVE.name();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDate.now();
    }

    // ========== Getters & Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = TeamStatus.normalize(status);
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDate updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<TeamMember> getTeamMembers() {
        return teamMembers;
    }
 
    public void setTeamMembers(Set<TeamMember> teamMembers) {
        this.teamMembers = teamMembers;
    }

    // 添加成员
    public void addMember(User user) {
        if (user == null || isOwner(user) || isMember(user)) {
            return;
        }
        teamMembers.add(new TeamMember(this, user, TeamRole.Member));
    }
    
    // 移除成员
    public void removeMember(User user) {
        if (user == null || isOwner(user)) {
            return;
        }

        Iterator<TeamMember> iterator = teamMembers.iterator();
        while (iterator.hasNext()) {
            TeamMember member = iterator.next();
            if (memberMatchesUser(member, user)) {
                iterator.remove();
                member.setTeam(null);
                break;
            }
        }
    }
    
    // 检查用户是否是成员
    public boolean isMember(User user) {
        if (user == null) {
            return false;
        }
        if (isOwner(user)) {
            return true;
        }
        return teamMembers.stream().anyMatch(member -> memberMatchesUser(member, user));
    }
    
    // 检查用户是否是团队所有者
    public boolean isOwner(User user) {
        return user != null && this.owner != null && Objects.equals(this.owner.getId(), user.getId());
    }

    private boolean memberMatchesUser(TeamMember member, User user) {
        return member != null
                && member.getUser() != null
                && Objects.equals(member.getUser().getId(), user.getId());
    }
}
