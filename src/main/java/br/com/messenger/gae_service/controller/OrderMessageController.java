package br.com.messenger.gae_service.controller;

import br.com.messenger.gae_service.model.Order;
import br.com.messenger.gae_service.model.User;
import br.com.messenger.gae_service.repository.UserRepository;
import br.com.messenger.gae_service.util.Operation;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/orders")
public class OrderMessageController {
    private static final Logger log = Logger.getLogger(OrderMessageController.class.getName());

    @Autowired
    UserRepository userRepository;

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
    @PostMapping(path = "/message")
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

                    log.info("Notificação enviada para o usuário com cpf: " + user.getCpf() + " - com a messagem: " +order.getNotification());
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
}
