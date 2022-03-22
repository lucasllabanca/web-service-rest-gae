package br.com.messenger.gae_service.util;

public class CheckProperty {

    public static boolean isLong(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
}
