package com.tim07.domain.DTO.registrationDTO;

/**
 * Created by PC-PC on 19-Apr-17.
 */
public class UserOrManagerRegistrationDTO {
    private String name;
    private String lastname;
    private String username;
    private String password;
    private String password2;
    private String email;

    public UserOrManagerRegistrationDTO() { }

    public UserOrManagerRegistrationDTO(String name, String lastname, String username, String password, String password2, String email) {
        this.name = name;
        this.lastname = lastname;
        this.username = username;
        this.password = password;
        this.password2 = password2;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
