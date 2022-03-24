package br.com.messenger.gae_service.model;

import java.io.Serializable;

public class Order implements Serializable {

    private Long orderId;
    private Long salesProviderUserId;
    private String cpf;
    private String notification;
    private String newOrderStatus;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long id) {
        this.orderId = id;
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
