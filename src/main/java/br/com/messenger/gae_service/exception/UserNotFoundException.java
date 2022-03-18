package br.com.messenger.gae_service.exception;

public class UserNotFoundException extends Exception {

    private String message;

    public UserNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
