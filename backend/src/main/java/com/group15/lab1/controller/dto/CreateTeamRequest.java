package com.group15.lab1.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建团队请求 DTO。
 */
public class CreateTeamRequest {

    @NotBlank(message = "团队名称不能为空")
    @Size(min = 1, max = 50, message = "团队名称长度应在 1-50 之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9_\\-\\s]+$", message = "团队名称只能包含中文、字母、数字、空格、下划线或短横线")
    private String name;

    public CreateTeamRequest() {
    }

    public CreateTeamRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
