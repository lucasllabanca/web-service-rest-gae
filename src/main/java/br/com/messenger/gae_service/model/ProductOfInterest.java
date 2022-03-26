package br.com.messenger.gae_service.model;

public class ProductOfInterest {

    private Long productOfInterestId;
    private String cpf;
    private Long salesProviderUserId;
    private Long salesProviderProductId;
    private double minPriceAlert;

    public Long getProductOfInterestId() {
        return productOfInterestId;
    }

    public void setProductOfInterestId(Long productOfInterestId) {
        this.productOfInterestId = productOfInterestId;
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

    public Long getSalesProviderProductId() {
        return salesProviderProductId;
    }

    public void setSalesProviderProductId(Long salesProviderProductId) {
        this.salesProviderProductId = salesProviderProductId;
    }

    public double getMinPriceAlert() {
        return minPriceAlert;
    }

    public void setMinPriceAlert(double minPriceAlert) {
        this.minPriceAlert = minPriceAlert;
    }
}
