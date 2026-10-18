package com.group15.lab1.dto;

/**
 * 用户资料数据传输对象
 */
public class UserProfileDto {

    private String email;
    private String avatarUrl;
    private String fullName;
    private String phone;

    public UserProfileDto() {
    }

    public UserProfileDto(String email, String avatarUrl, String fullName, String phone) {
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.fullName = fullName;
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
