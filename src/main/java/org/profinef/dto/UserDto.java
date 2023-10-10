package org.profinef.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class UserDto {

    @Id
    int id;
    @Column(name = "role_id")
    int roleId;
    @Column
    String login;
    @Column
    String password;

    public UserDto() {
    }

    public UserDto(int id, int roleId, String login, String password) {
        this.id = id;
        this.roleId = roleId;
        this.login = login;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", login='" + login + '\'' +
                '}';
    }
}
