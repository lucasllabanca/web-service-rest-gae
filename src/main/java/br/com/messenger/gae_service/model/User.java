package br.com.messenger.gae_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class User implements Serializable, UserDetails {

    private Long id;
    private String email;
    private String password;
    private String fcmRegId;
    private Date lastLogin;
    private Date lastFcmRegister;
    private Date lastUpdate;
    private String role;
    private String cpf;
    private Long salesProviderUserId;
    private Long crmProviderUserId;
    private boolean enabled;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> setAuths = new ArrayList<>();
        setAuths.add(new SimpleGrantedAuthority(role));
        return setAuths;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFcmRegId() {
        return fcmRegId;
    }

    public void setFcmRegId(String fcmRegId) {
        this.fcmRegId = fcmRegId;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Date getLastFcmRegister() {
        return lastFcmRegister;
    }

    public void setLastFcmRegister(Date lastFCMRegister) {
        this.lastFcmRegister = lastFCMRegister;
    }

    public Date getLastUpdate() { return lastUpdate; }

    public void setLastUpdate(Date lastUpdate) { this.lastUpdate = lastUpdate; }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public Long getSalesProviderUserId() {
        return salesProviderUserId;
    }

    public void setSalesProviderUserId(Long salesProviderUserId) {
        this.salesProviderUserId = salesProviderUserId;
    }

    public Long getCrmProviderUserId() {
        return crmProviderUserId;
    }

    public void setCrmProviderUserId(Long crmProviderUserId) {
        this.crmProviderUserId = crmProviderUserId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
