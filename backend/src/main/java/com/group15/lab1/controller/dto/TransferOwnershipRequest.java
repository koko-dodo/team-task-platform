package com.group15.lab1.controller.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 转让团队所有权请求 DTO。
 */
public class TransferOwnershipRequest {

    @NotNull(message = "新 Owner 用户 ID 不能为空")
    private Long newOwnerId;

    public Long getNewOwnerId() {
        return newOwnerId;
    }

    public void setNewOwnerId(Long newOwnerId) {
        this.newOwnerId = newOwnerId;
    }
}
