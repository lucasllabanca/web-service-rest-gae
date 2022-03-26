package br.com.messenger.gae_service.controller;

import br.com.messenger.gae_service.model.Order;
import br.com.messenger.gae_service.model.PriceUpdate;
import br.com.messenger.gae_service.model.ProductOfInterest;
import br.com.messenger.gae_service.model.User;
import br.com.messenger.gae_service.repository.ProductOfInterestRepository;
import br.com.messenger.gae_service.repository.UserRepository;
import com.google.appengine.api.datastore.Entity;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/messages")
public class MessageController {
    private static final Logger log = Logger.getLogger(MessageController.class.getName());

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductOfInterestRepository productOfInterestRepository;

    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setDatabaseUrl("https://web-service-rest-gae.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp inicializado com sucesso!");
        } catch (IOException e) {
            log.info("Falha ao configurar FirebaseApp");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/price-update")
    public ResponseEntity<String> sendPriceUpdateMessage(@RequestBody PriceUpdate priceUpdate) {

        String validateMsg = validatePriceUpdate(priceUpdate);

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        List<ProductOfInterest> productsOfInterest = productOfInterestRepository.getProductsOfInterestBySalesProviderProductIdAndMinPriceAlert(priceUpdate.getProductId(), priceUpdate.getNewProductPrice());

        if (productsOfInterest.size() > 0) {

            List<String> responseList = new ArrayList<>();

            for (ProductOfInterest productOfInterest : productsOfInterest) {

                Optional<User> optUser = userRepository.getByCpf(productOfInterest.getCpf());

                if (optUser.isPresent()) {
                    User user = optUser.get();

                    if (user.getFcmRegId() != null) {

                        try {
                            String notification = getPriceUpdateNotification(user, productOfInterest, priceUpdate);

                            Message message = Message.builder()
                                    .putData("salesMessage", notification)
                                    .setToken(user.getFcmRegId())
                                    .build();

                            String response = FirebaseMessaging.getInstance().send(message);

                            log.info("Notificação enviada: " + notification);
                            log.info("Resposta do Firebase Cloud Messaging: " + response);

                            responseList.add("STATUS: Notificação enviada. MOTIVO: Usuário com cpf: " + user.getCpf() + " possui o produto de interesse com Preço mínimo para alerta de R$" + productOfInterest.getMinPriceAlert());
                        } catch (FirebaseMessagingException e) {
                            responseList.add("STATUS: Notificação não enviada. MOTIVO: Falha ao enviar notificação: " + e.getMessage());
                        }
                    } else {
                        responseList.add("STATUS: Notificação não enviada. MOTIVO: Usuário com cpf: " + productOfInterest.getCpf() + " e produtos de interesse a notificar não possui fcmRegId cadastrado.");
                    }
                } else {
                    responseList.add("STATUS: Notificação não enviada. MOTIVO: Usuário com cpf: " + productOfInterest.getCpf() + " e produtos de interesse a notificar não encontrado na base pelo cpf.");
                }
            }
            return new ResponseEntity<>(getPriceUpdateResponse(responseList), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Nenhum produto de interesse com salesProviderProductId '" + priceUpdate.getProductId() + "' e minPriceAlert menor ou igual a 'R$" + priceUpdate.getNewProductPrice() + "' foi encontrado para notificar.", HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/order-status")
    public ResponseEntity<String> sendOrderMessage(@RequestBody Order order) {

        String validateMsg = validateOrder(order, null);

        if (!validateMsg.isEmpty())
            return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

        Optional<User> optUser = userRepository.getByCpf(order.getCpf());

        if (optUser.isPresent()) {

            User user = optUser.get();

            validateMsg = validateOrder(order, user);

            if (!validateOrder(order, user).isEmpty())
                return new ResponseEntity<>(validateMsg, HttpStatus.BAD_REQUEST);

            if (user.getFcmRegId() != null) {
                try {
                    Message message = Message.builder()
                            .putData("salesMessage", getOrderNotification(user, order))
                            .setToken(user.getFcmRegId())
                            .build();

                    String response = FirebaseMessaging.getInstance().send(message);

                    log.info("Notificação enviada para o usuário com cpf: " + user.getCpf() + " - com a messagem: " + order.getNotification());
                    log.info("Resposta do Firebase Cloud Messaging: " + response);

                    return new ResponseEntity<>("Notificação enviada com sucesso para o usuário com cpf: " + user.getCpf(), HttpStatus.OK);
                } catch (FirebaseMessagingException e) {
                    return new ResponseEntity<>("Falha ao enviar notificação: " + e.getMessage(), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>("Usuário com cpf: " + user.getCpf() + " - não registrado no FCM", HttpStatus.PRECONDITION_FAILED);
            }
        } else {
            return new ResponseEntity<>("Usuário com cpf: " + order.getCpf() + " - não encontrado", HttpStatus.NOT_FOUND);
        }
    }

    private String getPriceUpdateResponse(List<String> responseList) {
        StringBuilder response = new StringBuilder();

        for (String responseItem : responseList) {
            response.append(responseItem);
            response.append(System.getProperty("line.separator"));
        }

        return response.toString();
    }

    private String getPriceUpdateNotification(User user, ProductOfInterest productOfInterest, PriceUpdate priceUpdate) {
        StringBuilder notification = new StringBuilder();
        notification.append("Olá usuário: " + user.getEmail());
        notification.append(System.getProperty("line.separator"));
        notification.append("CPF: " + user.getCpf());
        notification.append(System.getProperty("line.separator"));
        notification.append("Seu produto de interesse com salesProviderProductId: " + productOfInterest.getSalesProviderProductId() + " possui a seguinte atualização:");
        notification.append(System.getProperty("line.separator"));
        notification.append("Novo preço do produto: R$" + priceUpdate.getNewProductPrice());
        notification.append(System.getProperty("line.separator"));
        notification.append("Preço mínimo para alerta: R$" + productOfInterest.getMinPriceAlert());
        return notification.toString();
    }

    private String getOrderNotification(User user, Order order) {
        StringBuilder notification = new StringBuilder();
        notification.append("Olá usuário: " + user.getEmail());
        notification.append(System.getProperty("line.separator"));
        notification.append("CPF: " + user.getCpf());
        notification.append(System.getProperty("line.separator"));
        notification.append("ID provedor de vendas: " + user.getSalesProviderUserId());
        notification.append(System.getProperty("line.separator"));
        notification.append("Seu pedido de código " + order.getOrderId() + " possui a seguinte atualização:");
        notification.append(System.getProperty("line.separator"));
        notification.append(order.getNotification());
        notification.append(System.getProperty("line.separator"));
        notification.append("Novo status do pedido: " + order.getNewOrderStatus());
        return notification.toString();
    }

    private String validateOrder(Order order, @Nullable User user) {
        if (user == null) {
            if (order.getCpf() == null || order.getCpf().trim().isEmpty())
                return "Está faltando a propriedade 'cpf' no modelo.";

            if (order.getOrderId() == null || order.getOrderId() == 0)
                return "Está faltando a propriedade 'orderId' no modelo.";

            if (order.getSalesProviderUserId() == null || order.getSalesProviderUserId() == 0)
                return "Está faltando a propriedade 'salesProviderUserId' no modelo.";

            if (order.getNotification() == null || order.getNotification().trim().isEmpty())
                return "Está faltando a propriedade 'notification' no modelo.";

            if (order.getNewOrderStatus() == null || order.getNewOrderStatus().trim().isEmpty())
                return "Está faltando a propriedade 'newOrderStatus' no modelo.";
        } else {
            if (order.getSalesProviderUserId() != user.getSalesProviderUserId())
                return "ID do provedor de vendas do usuário passado na requisição não coincide com o ID do provedor de vendas registrado na base para o usuário.";
        }
        return "";
    }

    private String validatePriceUpdate(PriceUpdate priceUpdate) {

        if (priceUpdate.getProductId() == null || priceUpdate.getProductId() <= 0)
            return "A propriedade 'productId' do modelo não pode ser nula e nem menor ou igual a 0.";

        if ((Double)priceUpdate.getNewProductPrice() == null || priceUpdate.getNewProductPrice() <= 0)
            return "A propriedade 'newProductPrice' do modelo não pode ser nula e nem menor ou igual a 0.";

        return "";
    }
}
