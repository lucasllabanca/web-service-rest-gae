package br.com.messenger.gae_service.exception;

public class ProductOfInterestNotFoundException extends Exception {

    private String message;

    public ProductOfInterestNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
