package br.com.messenger.gae_service.model;

import java.io.Serializable;

public class Order implements Serializable {

    private Long id;
    private Long salesProviderUserId;
    private String cpf;
    private String notification;
    private String newOrderStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSalesProviderUserId() {
        return salesProviderUserId;
    }

    public void setSalesProviderUserId(Long salesProviderUserId) {
        this.salesProviderUserId = salesProviderUserId;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public String getNewOrderStatus() {
        return newOrderStatus;
    }

    public void setNewOrderStatus(String newOrderStatus) {
        this.newOrderStatus = newOrderStatus;
    }
}
