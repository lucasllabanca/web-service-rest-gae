package br.com.messenger.gae_service.repository;

import br.com.messenger.gae_service.exception.UserAlreadyExistsException;
import br.com.messenger.gae_service.exception.UserNotFoundException;
import br.com.messenger.gae_service.model.User;
import com.google.appengine.api.datastore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import java.util.*;
import java.util.logging.Logger;

@Repository
public class UserRepository {

    private static final String ADMIN_EMAIL = "admin@admin.com.br";
    private static final Logger log = Logger.getLogger(UserRepository.class.getName());

    private PasswordEncoder passwordEncoder;
    private DatastoreService datastoreService;

    @Autowired
    public UserRepository(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.datastoreService = DatastoreServiceFactory.getDatastoreService();
    }

    private static final String USER_KIND = "users";
    private static final String USER_KEY = "userKey";

    private static final String PROPERTY_ID = "userId";
    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_PASSWORD = "password";
    private static final String PROPERTY_FCM_REG_ID = "fcmRegId";
    private static final String PROPERTY_LAST_LOGIN = "lastLogin";
    private static final String PROPERTY_LAST_FCM_REGISTER = "lastFcmRegister";
    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_CPF = "cpf";
    private static final String PROPERTY_SALES_PROVIDER_USER_ID = "salesProviderUserId";
    private static final String PROPERTY_CRM_PROVIDER_USER_ID = "crmProviderUserId";
    private static final String PROPERTY_ENABLED = "enabled";

    @PostConstruct
    public void init() {

        User adminUser;
        Optional<User> optUser = this.getByEmail(ADMIN_EMAIL);

        try {
            if (optUser.isPresent()) {
                adminUser = optUser.get();
                if (!adminUser.getRole().equals("ROLE_ADMIN")){
                    adminUser.setRole("ROLE_ADMIN");
                    this.updateUser(adminUser, ADMIN_EMAIL);
                }
            } else {
                adminUser = new User();
                adminUser.setRole("ROLE_ADMIN");
                adminUser.setEnabled(true);
                adminUser.setPassword("admin");
                adminUser.setEmail(ADMIN_EMAIL);
                this.saveUser(adminUser);
            }
        } catch (UserAlreadyExistsException | UserNotFoundException e) {
            log.severe("Falha ao criar ou alterar usuário ADMIN");
            log.severe(e.getMessage());
        }
    }

    public void updateUserLogin(User user) {

        boolean canUseCache = true;
        boolean saveOnCache = true;

        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            Cache cache = cacheFactory.createCache(Collections.emptyMap());

            if (cache.containsKey(user.getEmail())) {
                Date lastLogin = (Date) cache.get(user.getEmail());
                if ((Calendar.getInstance().getTime().getTime() - lastLogin.getTime()) < 30000) {
                    saveOnCache = false;
                }
            }

            if (saveOnCache) {
                cache.put(user.getEmail(), (Date) Calendar.getInstance().getTime());
                canUseCache = false;
            }
        } catch (CacheException e) {
            canUseCache = false;
        }

        if (!canUseCache) {
            user.setLastLogin(Calendar.getInstance().getTime());
            try {
                this.updateUser(user, user.getEmail());
            } catch (UserAlreadyExistsException | UserNotFoundException e) {
                log.severe("Falha ao atualizar último login do usuário");
            }
        }
    }

    public Optional<User> getByEmail (String email) {
        log.info("Get user by email: " + email);

        Entity userEntity = getUserEntityByEmail(email);

        if (userEntity != null) {
            return Optional.of(entityToUser(userEntity));
        } else {
            return Optional.empty();
        }
    }

    public Optional<User> getByCpf (String cpf) {
        log.info("Get user by CPF: " + cpf);

        Entity userEntity = getUserEntityByCpf(cpf);

        if (userEntity != null) {
            return Optional.of(entityToUser(userEntity));
        } else {
            return Optional.empty();
        }
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        Query query = new Query(USER_KIND).addSort(PROPERTY_EMAIL, Query.SortDirection.ASCENDING);

        List<Entity> userEntities = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());

        for (Entity userEntity : userEntities) {
            User user = entityToUser(userEntity);
            users.add(user);
        }

        return users;
    }

    public User deleteUser(String email) throws UserNotFoundException {

        Entity userEntity = getUserEntityByEmail(email);

        if (userEntity != null) {
            datastoreService.delete(userEntity.getKey());
            return entityToUser(userEntity);
        } else {
            throw new UserNotFoundException("Usuário " + email + " não encontrado");
        }
    }

    public User saveUser(User user) throws UserAlreadyExistsException {

        boolean emailExists = checkIfEmailExists(user);
        boolean cpfExists = checkIfCpfExists(user);

        if (!emailExists && !cpfExists) {
            Key userKey = KeyFactory.createKey(USER_KIND, USER_KEY);
            Entity userEntity = new Entity(USER_KIND, userKey);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userToEntity(user, userEntity, true);
            datastoreService.put(userEntity);
            user.setId(userEntity.getKey().getId());
            return user;
        } else {
            if (emailExists && cpfExists)
                throw new UserAlreadyExistsException("Usuário(s) com e-mail: " + user.getEmail() + " e CPF: " + user.getCpf() + " já existe(m)");
            else if (emailExists)
                throw new UserAlreadyExistsException("Usuário com e-mail: " + user.getEmail() + " já existe");
            else
                throw new UserAlreadyExistsException("Usuário com CPF: " + user.getEmail() + " já existe");
        }
    }

    public User updateUser(User user, String email) throws UserAlreadyExistsException, UserNotFoundException {

        if (!checkIfEmailExists(user)) {

            Entity userEntity = getUserEntityByEmail(email);

            if (userEntity != null) {
                userToEntity(user, userEntity, false);
                datastoreService.put(userEntity);
                user.setId(userEntity.getKey().getId());
                return user;
            } else {
                throw new UserNotFoundException("Usuário " + email + " não encontrado");
            }

        } else {
            throw new UserAlreadyExistsException("Usuário" + user.getEmail() + " já existe");
        }
    }

    private boolean checkIfEmailExists (User user) {

        Entity userEntity = getUserEntityByEmail(user.getEmail());

        if (userEntity == null) {
            return false;
        } else {
            if (user.getId() == null) {
                return true;
            } else {
                return userEntity.getKey().getId() != user.getId();
            }
        }
    }

    private boolean checkIfCpfExists (User user) {

        Entity userEntity = getUserEntityByCpf(user.getCpf());

        if (userEntity == null) {
            return false;
        } else {
            if (user.getId() == null) {
                return true;
            } else {
                return userEntity.getKey().getId() != user.getId();
            }
        }
    }

    private Entity getUserEntityByEmail(String email) {
        Query.Filter filter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, email);
        Query query = new Query(USER_KIND).setFilter(filter);
        return datastoreService.prepare(query).asSingleEntity();
    }

    private Entity getUserEntityByCpf(String cpf) {
        Query.Filter filter = new Query.FilterPredicate(PROPERTY_CPF, Query.FilterOperator.EQUAL, cpf);
        Query query = new Query(USER_KIND).setFilter(filter);
        return datastoreService.prepare(query).asSingleEntity();
    }

    private void userToEntity (User user, Entity userEntity, boolean updatePassword) {
        userEntity.setProperty(PROPERTY_ID, user.getId());
        userEntity.setProperty(PROPERTY_EMAIL, user.getEmail());
        userEntity.setProperty(PROPERTY_FCM_REG_ID, user.getFcmRegId());
        userEntity.setProperty(PROPERTY_LAST_LOGIN, user.getLastLogin());
        userEntity.setProperty(PROPERTY_LAST_FCM_REGISTER, user.getLastFcmRegister());
        userEntity.setProperty(PROPERTY_ROLE, user.getRole());
        userEntity.setProperty(PROPERTY_CPF, user.getCpf());
        userEntity.setProperty(PROPERTY_SALES_PROVIDER_USER_ID, user.getSalesProviderUserId());
        userEntity.setProperty(PROPERTY_CRM_PROVIDER_USER_ID, user.getCrmProviderUserId());
        userEntity.setProperty(PROPERTY_ENABLED, user.isEnabled());

        if (updatePassword)
            userEntity.setProperty(PROPERTY_PASSWORD, user.getPassword());
    }

    private User entityToUser (Entity userEntity) {
        User user = new User();
        user.setId(userEntity.getKey().getId());
        user.setEmail((String) userEntity.getProperty(PROPERTY_EMAIL));
        user.setPassword((String) userEntity.getProperty(PROPERTY_PASSWORD));
        user.setFcmRegId((String) userEntity.getProperty(PROPERTY_FCM_REG_ID));
        user.setLastLogin((Date) userEntity.getProperty(PROPERTY_LAST_LOGIN));
        user.setLastFcmRegister((Date) userEntity.getProperty(PROPERTY_LAST_FCM_REGISTER));
        user.setRole((String) userEntity.getProperty(PROPERTY_ROLE));
        user.setCpf((String) userEntity.getProperty(PROPERTY_CPF));
        user.setSalesProviderUserId(Long.parseLong((String) userEntity.getProperty(PROPERTY_SALES_PROVIDER_USER_ID)));
        user.setCrmProviderUserId(Long.parseLong((String) userEntity.getProperty(PROPERTY_CRM_PROVIDER_USER_ID)));
        user.setEnabled((Boolean) userEntity.getProperty(PROPERTY_ENABLED));
        return user;
    }
}
